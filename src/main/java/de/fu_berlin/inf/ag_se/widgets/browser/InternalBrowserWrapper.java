package de.fu_berlin.inf.ag_se.widgets.browser;

import de.fu_berlin.inf.ag_se.utils.*;
import de.fu_berlin.inf.ag_se.widgets.browser.BrowserStatusManager.BrowserStatus;
import de.fu_berlin.inf.ag_se.widgets.browser.exception.BrowserDisposedException;
import de.fu_berlin.inf.ag_se.widgets.browser.exception.JavaScriptException;
import de.fu_berlin.inf.ag_se.widgets.browser.exception.ScriptExecutionException;
import de.fu_berlin.inf.ag_se.widgets.browser.exception.UnexpectedBrowserStateException;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.JavaScriptExceptionListener;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * This is an internal wrapper class around the {@link org.eclipse.swt.browser.Browser}.
 * It enhances its core functionality and is used by {@link de.fu_berlin.inf.ag_se.widgets.browser.Browser}
 * to provide more specific methods to the users.
 */
public class InternalBrowserWrapper {

    private static Logger LOGGER = Logger.getLogger(InternalBrowserWrapper.class);

    private Browser browser;

    private BrowserStatusManager browserStatusManager;

    private boolean settingUri = false;

    private boolean allowLocationChange = false;

    private final Object monitor = new Object();

    private Rectangle cachedContentBounds = null;

    private List<Runnable> beforeLoading = new ArrayList<Runnable>();

    private List<Runnable> afterLoading = new ArrayList<Runnable>();

    private List<Runnable> beforeCompletion = new ArrayList<Runnable>();

    private List<Function<String>> beforeScripts = new ArrayList<Function<String>>();

    private List<Function<Object>> afterScripts = new ArrayList<Function<Object>>();

    private final List<JavaScriptExceptionListener> javaScriptExceptionListeners = Collections
            .synchronizedList(new ArrayList<JavaScriptExceptionListener>());

    InternalBrowserWrapper(Composite parent) {
        browser = new Browser(parent, SWT.NONE);
        browser.setVisible(false);

        browserStatusManager = new BrowserStatusManager();

        final JavaScriptExceptionListener javaScriptExceptionListener = new JavaScriptExceptionListener() {
            @Override
            public void thrown(JavaScriptException javaScriptException) {
                fireJavaScriptExceptionThrown(javaScriptException);
            }
        };

        // throws exception that arise from calls within the browser,
        // meaning code that has not been invoked by Java but by JavaScript
        createBrowserFunction("__error_callback", new IBrowserFunction() {
            @Override
            public Object function(Object[] arguments) {
                JavaScriptException javaScriptException = JavaScriptException
                        .parseJavaScriptException(arguments);
                LOGGER.error(javaScriptException);
                javaScriptExceptionListener.thrown(javaScriptException);
                return false;
            }
        });

        browser.addLocationListener(new LocationAdapter() {
            @Override
            public void changing(LocationEvent event) {
                if (!settingUri) {
                    event.doit = allowLocationChange || getBrowserStatus() == BrowserStatus.LOADING;
                }
            }
        });

        browser.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                synchronized (monitor) {
                    if (getBrowserStatus() == BrowserStatus.LOADING) {
                        setBrowserStatus(BrowserStatus.DISPOSED);
                    }
                    monitor.notifyAll();
                }
            }
        });
    }

    /**
     * @throws BrowserDisposedException if the browser is disposed
     */
    Future<Boolean> open(final String uri, final Integer timeout,
                         final String pageLoadCheckExpression) {
        if (browser.isDisposed()) {
            throw new BrowserDisposedException();
        }

        setBrowserStatus(BrowserStatus.LOADING);
        activateExceptionHandling();

        browser.addProgressListener(new ProgressAdapter() {
            @Override
            public void completed(ProgressEvent event) {
                waitAndComplete(pageLoadCheckExpression);
            }
        });

        return ExecUtils.nonUIAsyncExec(Browser.class, "Opening " + uri,
                new NoCheckedExceptionCallable<Boolean>() {
                    @Override
                    public Boolean call() {
                        // stops waiting after timeout
                        Future<Void> timeoutMonitor = null;
                        if (timeout != null && timeout > 0) {
                            timeoutMonitor = ExecUtils.nonUIAsyncExec(Browser.class,
                                    "Timeout Watcher for " + uri, new Runnable() {
                                        @Override
                                        public void run() {
                                            executeTimeoutCallback();
                                        }
                                    }, timeout);
                        } else {
                            LOGGER.warn("timeout must be greater or equal 0. Ignoring timeout.");
                        }

                        for (Runnable runnable : beforeLoading) {
                            ExecUtils.nonUISyncExec(runnable);
                        }

                        SwtUiThreadExecutor.syncExec(new Runnable() {
                            @Override
                            public void run() {
                                settingUri = true;
                                if (!browser.isDisposed()) {
                                    browser.setUrl(uri);
                                }
                                settingUri = false;
                            }
                        });

                        for (Runnable runnable : afterLoading) {
                            ExecUtils.nonUISyncExec(runnable);
                        }

                        synchronized (monitor) {
                            LOGGER.debug("Waiting for " + uri + " to be loaded (Thread: "
                                    + Thread.currentThread() + "; status: " + getBrowserStatus() + ")");
                            while (browserStatusManager.isLoading()) {
                                try {
                                    monitor.wait();
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                                // notified by progresslistener or by
                                // timeout
                            }

                            if (timeoutMonitor != null) {
                                timeoutMonitor.cancel(true);
                            }

                            return browserStatusManager.queryLoadingStatus(uri);
                        }
                    }
                });
    }

    private void executeTimeoutCallback() {
        synchronized (monitor) {
            if (getBrowserStatus() != BrowserStatus.LOADED) {
                setBrowserStatus(BrowserStatus.TIMEDOUT);
            }
            monitor.notifyAll();
        }
    }

    /**
     * This method waits for the {@link Browser} to complete loading.
     * It has been observed that the {@link ProgressListener#completed(ProgressEvent)} fires to early.
     * This method uses JavaScript to reliably detect the completed state.
     *
     * @param pageLoadCheckExpression the Javascript expression to used for checking the loading state
     */
    private void waitAndComplete(String pageLoadCheckExpression) {
        if (browser == null || browser.isDisposed()) {
            return;
        }

        if (getBrowserStatus() != BrowserStatus.LOADING) {
            if (!Arrays.asList(BrowserStatus.TIMEDOUT, BrowserStatus.DISPOSED)
                       .contains(getBrowserStatus())) {
                //TODO State Error: LOADED
                LOGGER.error("State Error: " + getBrowserStatus());
            }
            return;
        }

        String condition = "document.readyState == 'complete'" + (
                pageLoadCheckExpression != null ?
                " && (" + pageLoadCheckExpression + ")" :
                "");
        String randomFunctionName = BrowserUtils.createRandomFunctionName();
        createBrowserFunction(randomFunctionName, new IBrowserFunction() {
            @Override
            public Object function(Object[] arguments) {
                complete();
                dispose();
                return null;
            }
        });
        String completedCheckScript = JavascriptString.createWaitForConditionJavascript(
                condition, randomFunctionName);

        try {
            runImmediately(completedCheckScript, IConverter.CONVERTER_VOID);
        } catch (RuntimeException e) {
            LOGGER.error("An error occurred while checking the page load state", e);
            synchronized (monitor) {
                monitor.notifyAll();
            }
        }
    }

    /**
     * @throws BrowserDisposedException if the browser is disposed
     */
    void waitForCondition(String condition) {
        if (browser == null || browser.isDisposed()) {
            throw new BrowserDisposedException();
        }

        //TODO Maybe we don't need the callback
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        String randomFunctionName = BrowserUtils.createRandomFunctionName();
        IBrowserFunction browserFunction = createBrowserFunction(randomFunctionName, new IBrowserFunction() {
            public Object function(Object[] arguments) {
                countDownLatch.countDown();
                return null;
            }
        });
        String checkScript = JavascriptString.createWaitForConditionJavascript(condition, randomFunctionName);

        runImmediately(checkScript, IConverter.CONVERTER_VOID);
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        browserFunction.dispose();
    }

    /**
     * This method is called by {@link #waitAndComplete(String)} and post processes the loaded page. <ol> <li>calls beforeCompletion</li>
     * <li>injects necessary scripts</li> <li>runs the scheduled user scripts</li> </ol>
     */
    private void complete() {
        final String uri = getUrl();
        for (Runnable runnable : beforeCompletion) {
            ExecUtils.nonUISyncExec(runnable);
        }

        ExecUtils.nonUISyncExec(Browser.class, "Progress Check for " + uri,
                new Runnable() {
                    @Override
                    public void run() {
                        SwtUiThreadExecutor.asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                if (!browser.isDisposed()) {
                                    browser.setVisible(true);
                                }
                            }
                        });

                        synchronized (monitor) {
                            if (!Arrays.asList(BrowserStatus.TIMEDOUT,
                                    BrowserStatus.DISPOSED).contains(getBrowserStatus())) {
                                setBrowserStatus(BrowserStatus.LOADED);
                            }
                            monitor.notifyAll();
                        }
                    }
                });
    }

    /**
     * Notifies all registered Javascript exception listeners in case a JavaScript error occurred.
     */
    private void activateExceptionHandling() {
        try {
            runImmediately(JavascriptString.getExceptionForwardingScript("__error_callback"), IConverter.CONVERTER_VOID);
        } catch (ScriptExecutionException e) {
            LOGGER.error("Error activating browser's exception handling. JavaScript exceptions are not detected!", e);
        }
    }

    Future<Boolean> inject(URI scriptURI) {
        return run(scriptURI, false);
    }

    Future<Boolean> run(File scriptFile) {
        Assert.isLegal(scriptFile.canRead());
        return run(scriptFile.toURI(), false);
    }

    Future<Boolean> run(final URI scriptURI) {
        return run(scriptURI, true);
    }

    private Future<Boolean> run(final URI scriptURI, final boolean removeAfterExecution) {
        Assert.isLegal(scriptURI != null);
        if ("file".equalsIgnoreCase(scriptURI.getScheme())) {
            File file = new File(scriptURI);
            if (removeAfterExecution) {
                LOGGER.warn("The script "
                        + scriptURI
                        + " is on the local file system. To circument security restrictions its content becomes directly executed and thus cannot be removed.");
            }

            try {
                return run(FileUtils.readFileToString(file), IConverter.CONVERTER_BOOLEAN);
            } catch (IOException e) {
                return new CompletedFuture<Boolean>(null, e);
            }
        } else {
            return ExecUtils.nonUIAsyncExec(Browser.class, "Script Runner for: " + scriptURI,
                    new CallbackFunctionCallable(this, scriptURI, removeAfterExecution));
        }
    }

    Future<Object> run(final String script) {
        return run(script, new IConverter<Object, Object>() {
            @Override
            public Object convert(Object object) {
                return object;
            }
        });
    }

    <DEST> Future<DEST> run(final String script,
                            final IConverter<Object, DEST> converter) {
        Assert.isLegal(converter != null);
        return browserStatusManager.createFuture(new ScriptExecutingCallable<DEST>(this, converter, script));
    }

    /**
     * @throws ScriptExecutionException if an exception occurs while executing the script
     */
    <DEST> DEST runImmediately(String script,
                               IConverter<Object, DEST> converter) {
        return SwtUiThreadExecutor.syncExec(new ScriptExecutingCallable<DEST>(this, converter, script));
    }

    /**
     * @throws ScriptExecutionException if an exception occurs while executing the script
     * @throws IOException              if an exception occurs while reading the passed file
     */
    void runContentsImmediately(File scriptFile) throws IOException {
        runImmediately(FileUtils.readFileToString(scriptFile), IConverter.CONVERTER_VOID);
    }

    /**
     * @throws ScriptExecutionException if an exception occurs while executing the script
     * @throws IOException              if an exception occurs while reading the passed file
     */
    void runContentsAsScriptTagImmediately(File scriptFile) throws IOException {
        runImmediately(JavascriptString.embedContentsIntoScriptTag(scriptFile), IConverter.CONVERTER_VOID);
    }

    void executeBeforeScript(Function<String> runnable) {
        beforeScripts.add(runnable);
    }

    void executeAfterScript(Function<Object> runnable) {
        afterScripts.add(runnable);
    }

    Future<Void> injectJsFile(File file) {
        return run(JavascriptString.createJsFileInjectionScript(file),
                IConverter.CONVERTER_VOID);
    }

    /**
     * @throws ScriptExecutionException if an exception occurs while executing the script
     */
    void injectJsFileImmediately(File file) {
        runImmediately(JavascriptString.createJsFileInjectionScript(file),
                IConverter.CONVERTER_VOID);
    }

    Future<Void> injectCssFile(URI uri) {
        return run(JavascriptString.createCssFileInjectionScript(uri),
                IConverter.CONVERTER_VOID);
    }

    /**
     * @throws ScriptExecutionException if an exception occurs while executing the script
     */
    void injectCssFileImmediately(URI uri) {
        runImmediately(JavascriptString.createCssFileInjectionScript(uri),
                IConverter.CONVERTER_VOID);
    }

    Future<Void> injectCss(String css) {
        return run(JavascriptString.createCssInjectionScript(css), IConverter.CONVERTER_VOID);
    }

    /**
     * @throws ScriptExecutionException if an exception occurs while executing the script
     */
    void injectCssImmediately(String css) {
        runImmediately(JavascriptString.createCssInjectionScript(css),
                IConverter.CONVERTER_VOID);
    }

    /**
     * Returns the state of the browser.
     *
     * @return a status enum value
     */
    BrowserStatus getBrowserStatus() {
        return browserStatusManager.getBrowserStatus();
    }

    void setBrowserStatus(BrowserStatus browserStatus) throws UnexpectedBrowserStateException {
        browserStatusManager.setBrowserStatus(browserStatus);
    }

    void setAllowLocationChange(boolean allowed) {
        this.allowLocationChange = allowed;
    }

    boolean isLoadingCompleted() {
        return getBrowserStatus() == BrowserStatus.LOADED;
    }

    boolean isDisposed() {
        return browser.isDisposed();
    }

    /**
     * Wrapper for {@link org.eclipse.swt.browser.Browser#evaluate(String)}.
     * Converts {@link org.eclipse.swt.SWTException}s into SWT independent exceptions.
     *
     * @param javaScript the Javascript string to be executed,
     *                   must not be null
     * @return the return value, if any, of executing the script
     *
     * @throws java.lang.IllegalArgumentException if the argument is null
     * @throws ScriptExecutionException           if an exception occurs while executing the script
     */
    Object evaluate(String javaScript) {
        try {
            String script = JavascriptString.getExceptionReturningScript(javaScript);
            Object returnValue = browser.evaluate(script);
            BrowserUtils.rethrowJavascriptException(script, returnValue);
            return returnValue;
        } catch (SWTException e) {
            //TODO perform SWT error conversion here
            throw new ScriptExecutionException(javaScript, e);
        }
    }

    /**
     * May be called from whatever thread.
     */
    String getUrl() {
        return SwtUiThreadExecutor.syncExec(new NoCheckedExceptionCallable<String>() {
            @Override
            public String call() {
                return browser.getUrl();
            }
        });
    }

    void addListener(int eventType, Listener listener) {
        // TODO evtl. erst ausführen, wenn alles wirklich geladen wurde, um
        // evtl. falsche Mauskoordinaten zu verhindern und so ein Fehlverhalten
        // im InformationControl vorzeugen
        browser.addListener(eventType, listener);
    }

    void layoutRoot() {
        Composite root = SWTUtils.getRoot(browser);
        LOGGER.debug("layout all");
        root.layout(true, true);
    }

    void setCachedContentBounds(Rectangle rectangle) {
        cachedContentBounds = rectangle;
    }

    Rectangle getCachedContentBounds() {
        return cachedContentBounds;
    }

    synchronized void fireJavaScriptExceptionThrown(JavaScriptException javaScriptException) {
        for (JavaScriptExceptionListener listener : javaScriptExceptionListeners) {
            listener.thrown(javaScriptException);
        }
    }

    void addJavaScriptExceptionListener(JavaScriptExceptionListener javaScriptExceptionListener) {
        javaScriptExceptionListeners.add(javaScriptExceptionListener);
    }

    void removeJavaScriptExceptionListener(JavaScriptExceptionListener javaScriptExceptionListener) {
        javaScriptExceptionListeners.remove(javaScriptExceptionListener);
    }

    void executeBeforeLoading(Runnable runnable) {
        beforeLoading.add(runnable);
    }

    void executeAfterLoading(Runnable runnable) {
        afterLoading.add(runnable);
    }

    void executeBeforeCompletion(Runnable runnable) {
        beforeCompletion.add(runnable);
    }

    /**
     * May be called from whatever thread.
     */
    IBrowserFunction createBrowserFunction(final String functionName,
                                           final IBrowserFunction function) {
        return SwtUiThreadExecutor.syncExec(new NoCheckedExceptionCallable<IBrowserFunction>() {
            @Override
            public IBrowserFunction call() {
                new BrowserFunction(browser, functionName) {
                    @Override
                    public Object function(Object[] arguments) {
                        return function.function(arguments);
                    }
                };
                return function;
            }
        });
    }

    void executeBeforeScriptExecutionScripts(final String script) {
        executeScriptList(beforeScripts, script);
    }

    void executeAfterScriptExecutionScripts(final Object returnValue) {
        executeScriptList(afterScripts, returnValue);
    }

    /**
     * Executes a list of Javascript scripts sequentially.
     * If an exceptions occurs the remaining scripts are executed nevertheless.
     *
     * @param scripts
     * @param input
     * @param <V>
     */
    private <V> void executeScriptList(List<Function<V>> scripts, final V input) {
        for (final Function<V> script : scripts) {
            try {
                SwtUiThreadExecutor.syncExec(new Runnable() {
                    @Override
                    public void run() {
                        script.run(input);
                    }
                });
            } catch (RuntimeException e) {
                LOGGER.error(e);
                // TODO only catch some Exceptions throw the rest?
            }
        }
    }

    /**
     * This method must not be called from the UI thread, as it is blocking.
     *
     * @throws IllegalStateException    if this method is called from the UI thread
     * @throws ScriptExecutionException if an exception occurs while executing the script
     */
    Object syncRun(String script) {
        if (ExecUtils.isUIThread())
            throw new IllegalStateException("This method must not be called from the UI thread.");

        Future<Object> res = run(script);
        try {
            return res.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.debug("Interrupted while waiting for the result. Returning null.");
            return null;
        } catch (ExecutionException e) {
            throw new ScriptExecutionException(script, e);
        }
    }
}
