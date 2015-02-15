package de.fu_berlin.inf.ag_se.widgets.browser;

import de.fu_berlin.inf.ag_se.utils.Assert;
import de.fu_berlin.inf.ag_se.utils.IConverter;
import de.fu_berlin.inf.ag_se.utils.SWTUtils;
import de.fu_berlin.inf.ag_se.widgets.browser.BrowserStatusManager.BrowserStatus;
import de.fu_berlin.inf.ag_se.widgets.browser.exception.BrowserDisposedException;
import de.fu_berlin.inf.ag_se.widgets.browser.exception.JavaScriptException;
import de.fu_berlin.inf.ag_se.widgets.browser.exception.ScriptExecutionException;
import de.fu_berlin.inf.ag_se.widgets.browser.functions.CallbackFunction;
import de.fu_berlin.inf.ag_se.widgets.browser.functions.Function;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.JavaScriptExceptionListener;
import de.fu_berlin.inf.ag_se.widgets.browser.threading.NoCheckedExceptionCallable;
import de.fu_berlin.inf.ag_se.widgets.browser.threading.SwtUiThreadExecutor;
import de.fu_berlin.inf.ag_se.widgets.browser.threading.UIThreadAwareScheduledThreadPoolExecutor;
import de.fu_berlin.inf.ag_se.widgets.browser.threading.futures.CompletedFuture;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

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

    private List<Callable<Object>> beforeLoading = new ArrayList<Callable<Object>>();

    private List<Callable<Object>> afterLoading = new ArrayList<Callable<Object>>();

    private List<Callable<Object>> beforeCompletion = new ArrayList<Callable<Object>>();

    private List<Function<String>> beforeScripts = new ArrayList<Function<String>>();

    private List<Function<Object>> afterScripts = new ArrayList<Function<Object>>();

    private final List<JavaScriptExceptionListener> javaScriptExceptionListeners = Collections
            .synchronizedList(new ArrayList<JavaScriptExceptionListener>());

    private Future<?> timeoutMonitor;

    private final UIThreadAwareScheduledThreadPoolExecutor executor;

    InternalBrowserWrapper(Composite parent) {
        browser = new Browser(parent, SWT.NONE);
        browser.setVisible(false);

        executor = new UIThreadAwareScheduledThreadPoolExecutor();

        browserStatusManager = new BrowserStatusManager();

        // throws exception that arise from calls within the browser,
        // meaning code that has not been invoked by Java but by JavaScript
        createBrowserFunction("__error_callback", new IBrowserFunction() {
            @Override
            public Object function(Object[] arguments) {
                JavaScriptException javaScriptException = JavaScriptException
                        .parseJavaScriptException(arguments);
                LOGGER.error(javaScriptException);
                fireJavaScriptExceptionThrown(javaScriptException);
                return false;
            }
        });

        browser.addLocationListener(new LocationAdapter() {
            @Override
            public void changing(LocationEvent event) {
                if (!settingUri) {
                    event.doit = allowLocationChange || browserStatusManager.isLoading();
                }
            }
        });

        browser.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                synchronized (monitor) {
                    browserStatusManager.setBrowserStatus(BrowserStatus.DISPOSED);
                    monitor.notifyAll();
                }
                executor.shutdownNow();
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

        browserStatusManager.setBrowserStatus(BrowserStatus.LOADING);

        browser.addProgressListener(new ProgressAdapter() {
            @Override
            public void completed(ProgressEvent event) {
                waitAndComplete(pageLoadCheckExpression);
            }
        });

        return executor.nonUIAsyncExec(Browser.class, "Opening " + uri,
                new NoCheckedExceptionCallable<Boolean>() {
                    @Override
                    public Boolean call() {
                        startTimeout(uri, timeout);

                        try {
                            executor.invokeAll(beforeLoading);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
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

                        try {
                            executor.invokeAll(afterLoading);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }

                        synchronized (monitor) {
                            LOGGER.debug("Waiting for " + uri + " to be loaded (Thread: " + Thread.currentThread());
                            while (browserStatusManager.isLoading()) {
                                try {
                                    // notified by progresslistener or by
                                    // timeout
                                    monitor.wait();
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }

                            cancelTimeout();

                            return browserStatusManager.wasLoadingSuccessful(uri);
                        }
                    }
                });
    }

    private void startTimeout(String uri, Integer timeout) {
        if (timeout == null || timeout <= 0) {
            timeoutMonitor = null;
            LOGGER.warn("timeout must be greater or equal 0. Ignoring timeout.");
        } else {
            timeoutMonitor = executor.nonUIAsyncExec(Browser.class,
                    "Timeout Watcher for " + uri, new Runnable() {
                        @Override
                        public void run() {
                            executeTimeoutCallback();
                        }
                    }, timeout);
        }
    }

    private void cancelTimeout() {
        if (timeoutMonitor != null) {
            timeoutMonitor.cancel(true);
        }
    }

    private void executeTimeoutCallback() {
        synchronized (monitor) {
            browserStatusManager.setBrowserStatus(BrowserStatus.TIMEDOUT);
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

        if (!browserStatusManager.isLoading()) {
            if (!Arrays.asList(BrowserStatus.TIMEDOUT, BrowserStatus.DISPOSED)
                       .contains(getBrowserStatus())) {
                //TODO state error loaded
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
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        complete();
                    }
                });
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
     * This method is called by {@link #waitAndComplete(String)} and post processes the loaded page. <ol> <li>calls beforeCompletion</li>
     * <li>injects necessary scripts</li> <li>runs the scheduled user scripts</li> </ol>
     */
    private void complete() {
        activateExceptionHandling();

        try {
            executor.invokeAll(beforeCompletion);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        setVisible(true);

        synchronized (monitor) {
            browserStatusManager.setBrowserStatus(BrowserStatus.LOADED);
            monitor.notifyAll();
        }
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

        run(checkScript, IConverter.CONVERTER_VOID);
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        browserFunction.dispose();
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
                        + " is on the local file system. To circumvent security restrictions its content becomes directly executed and thus cannot be removed.");
            }

            try {
                return run(FileUtils.readFileToString(file), IConverter.CONVERTER_BOOLEAN);
            } catch (IOException e) {
                return new CompletedFuture<Boolean>(null, e);
            }
        } else {
            return executor.nonUIAsyncExec(Browser.class, "Script Runner for: " + scriptURI,
                    new CallbackFunctionCallable(this, scriptURI, removeAfterExecution));
        }
    }

    Future<Object> run(final String script) {
        return run(script, IConverter.CONVERTER_IDENT);
    }

    <DEST> Future<DEST> run(final String script,
                            final IConverter<Object, DEST> converter) {
        if (isLoadingCompleted()) {
            try {
                DEST dest = SwtUiThreadExecutor.syncExec(new ScriptExecutingCallable<DEST>(this, converter, script));
                return new CompletedFuture<DEST>(dest, null);
            } catch (RuntimeException e) {
                return new CompletedFuture<DEST>(null, e);
            }
        }
        return browserStatusManager.createFuture(new ScriptExecutingCallable<DEST>(this, converter, script));
    }

    /**
     * @throws ScriptExecutionException if an exception occurs while executing the script
     */
    <DEST> DEST runImmediately(String script,
                               IConverter<Object, DEST> converter) {
        return SwtUiThreadExecutor.syncExec(new ScriptExecutingCallable<DEST>(this, converter, script));
    }

    Object runImmediately(String script) {
        return runImmediately(script, IConverter.CONVERTER_IDENT);
    }

    /**
     * @throws ScriptExecutionException if an exception occurs while executing the script
     * @throws IOException              if an exception occurs while reading the passed file
     */
    Future<Void> runContent(File scriptFile) throws IOException {
        return run(FileUtils.readFileToString(scriptFile), IConverter.CONVERTER_VOID);
    }

    /**
     * @throws ScriptExecutionException if an exception occurs while executing the script
     * @throws IOException              if an exception occurs while reading the passed file
     */
    Future<Void> runContentsAsScriptTag(File scriptFile) throws IOException {
        return run(JavascriptString.embedContentsIntoScriptTag(scriptFile), IConverter.CONVERTER_VOID);
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
    private BrowserStatus getBrowserStatus() {
        return browserStatusManager.getBrowserStatus();
    }

    void setAllowLocationChange(boolean allowed) {
        this.allowLocationChange = allowed;
    }

    boolean isLoadingCompleted() {
        return browserStatusManager.isLoadingCompleted();
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
            executeBeforeScriptExecutionScripts(javaScript);

            String script = JavascriptString.getExceptionReturningScript(javaScript);
            Object returnValue = browser.evaluate(script);
            BrowserUtils.rethrowJavascriptException(script, returnValue);

            executeAfterScriptExecutionScripts(returnValue);

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

    void setVisible(final boolean visible) {
        executor.asyncUIExec(new Runnable() {
            @Override
            public void run() {
                if (!browser.isDisposed()) {
                    browser.setVisible(visible);
                }
            }
        });
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

    void executeBeforeLoading(final Runnable runnable) {
        beforeLoading.add(Executors.callable(runnable));
    }

    void executeAfterLoading(Runnable runnable) {
        afterLoading.add(Executors.callable(runnable));
    }

    void executeBeforeCompletion(Runnable runnable) {
        beforeCompletion.add(Executors.callable(runnable));
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
        Collection<NoCheckedExceptionCallable<Object>> res = new ArrayList<NoCheckedExceptionCallable<Object>>();
        for (final Function<V> script : scripts) {
            res.add(new NoCheckedExceptionCallable<Object>() {
                @Override
                public Void call() {
                    script.run(input);
                    return null;
                }
            });
        }
        try {
            executor.invokeAll(res);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }

    /**
     * This method must not be called from the UI thread, as it is blocking.
     *
     * @throws IllegalStateException    if this method is called from the UI thread
     * @throws ScriptExecutionException if an exception occurs while executing the script
     */
    Object syncRun(String script) {
        if (SwtUiThreadExecutor.isUIThread())
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

    <T> Future<T> syncRun(final String script, final CallbackFunction<Object, T> callback) {
        return runWithCallback(InternalBrowserWrapper.this.run(script), callback);
    }

    public <V, T> Future<T> runWithCallback(final Future<V> future, final CallbackFunction<V, T> callback) {
        return executor.submit(new Callable<T>() {
            @Override
            public T call() throws InterruptedException {
                V returnValue = null;
                RuntimeException exception = null;
                try {
                    returnValue = future.get();
                } catch (ExecutionException e) {
                    exception = (RuntimeException) e.getCause();
                }
                return callback.apply(returnValue, exception);
            }
        });
    }
}
