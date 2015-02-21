package de.fu_berlin.inf.ag_se.browser;

import de.fu_berlin.inf.ag_se.browser.BrowserStatusManager.BrowserStatus;
import de.fu_berlin.inf.ag_se.browser.exception.BrowserDisposedException;
import de.fu_berlin.inf.ag_se.browser.exception.JavaScriptException;
import de.fu_berlin.inf.ag_se.browser.exception.ScriptExecutionException;
import de.fu_berlin.inf.ag_se.browser.functions.CallbackFunction;
import de.fu_berlin.inf.ag_se.browser.functions.Function;
import de.fu_berlin.inf.ag_se.browser.listener.JavaScriptExceptionListener;
import de.fu_berlin.inf.ag_se.browser.threading.CompletedFuture;
import de.fu_berlin.inf.ag_se.browser.threading.NoCheckedExceptionCallable;
import de.fu_berlin.inf.ag_se.browser.threading.UIThreadAwareScheduledThreadPoolExecutor;
import de.fu_berlin.inf.ag_se.browser.threading.UIThreadExecutor;
import de.fu_berlin.inf.ag_se.browser.utils.Assert;
import de.fu_berlin.inf.ag_se.browser.utils.IConverter;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

/**
 * This is an internal wrapper class around the {@link org.eclipse.swt.browser.Browser}.
 * It enhances its core functionality and is used by {@link Browser}
 * to provide more specific methods to the users.
 */
public class InternalBrowserWrapper<T extends IFrameworkBrowser> {

    private static Logger LOGGER = Logger.getLogger(InternalBrowserWrapper.class);

    protected final T browser;

    private BrowserStatusManager browserStatusManager;

    private final Object monitor = new Object();

    private Rectangle cachedContentBounds = null;

    private List<Callable<Object>> beforeLoading = new ArrayList<Callable<Object>>();

    private List<Callable<Object>> afterLoading = new ArrayList<Callable<Object>>();

    private List<Callable<Object>> afterCompletion = new ArrayList<Callable<Object>>();

    private List<Function<String>> beforeScripts = new ArrayList<Function<String>>();

    private List<Function<Object>> afterScripts = new ArrayList<Function<Object>>();

    private List<Runnable> runOnDisposalList = new ArrayList<Runnable>();

    private final List<JavaScriptExceptionListener> javaScriptExceptionListeners = Collections
            .synchronizedList(new ArrayList<JavaScriptExceptionListener>());

    private Future<?> timeoutMonitor;

    protected final UIThreadAwareScheduledThreadPoolExecutor executor;

    protected final UIThreadExecutor uiThreadExecutor;

    protected boolean allowLocationChange = false;

    protected boolean settingUri = false;

    protected InternalBrowserWrapper(T browser) {
        this.browser = browser;
        browser.setVisible(false);
        uiThreadExecutor = browser.getUIThreadExecutor();
        executor = new UIThreadAwareScheduledThreadPoolExecutor(uiThreadExecutor);
        browserStatusManager = new BrowserStatusManager(uiThreadExecutor);

        // throws exception that arise from calls within the browser,
        // meaning code that has not been invoked by Java but by JavaScript
        createBrowserFunction(new IBrowserFunction("__error_callback") {
            @Override
            public Object function(Object[] arguments) {
                JavaScriptException javaScriptException = JavaScriptException
                        .parseJavaScriptException(arguments);
                LOGGER.error(javaScriptException);
                fireJavaScriptExceptionThrown(javaScriptException);
                return false;
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

        browser.addProgressListener(new Runnable() {
            @Override
            public void run() {
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

                        uiThreadExecutor.syncExec(new Runnable() {
                            @Override
                            public void run() {
                                if (!browser.isDisposed()) {
                                    settingUri = true;
                                    browser.setUrl(uri);
                                    settingUri = false;
                                }
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
     * It has been observed that the ProgressListener#completed(ProgressEvent) fires to early.
     * This method uses JavaScript to reliably detect the completed state.
     *
     * @param pageLoadCheckExpression the Javascript expression to used for checking the loading state
     */
    private void waitAndComplete(String pageLoadCheckExpression) {
        if (browser.isDisposed()) {
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
        createBrowserFunction(new IBrowserFunction(randomFunctionName) {
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
     * This method is called by {@link #waitAndComplete(String)} and post processes the loaded page. <ol> <li>calls afterCompletion</li>
     * <li>injects necessary scripts</li> <li>runs the scheduled user scripts</li> </ol>
     */
    private void complete() {
        activateExceptionHandling();

        try {
            executor.invokeAll(afterCompletion);
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

    Future<Boolean> openWithCallback(URI uri, int timeout, String pageLoadCheckScript, CallbackFunction<Boolean, Boolean> callback) {
        return runWithCallback(open(uri.toString(), timeout, pageLoadCheckScript), callback);
    }

    boolean syncOpen(URI uri, int timeout, String pageLoadCheckScript) {
        checkNotUIThread();
        Future<Boolean> opened = open(uri.toString(), timeout, pageLoadCheckScript);
        try {
            return opened.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    /**
     * @throws BrowserDisposedException if the browser is disposed
     */
    void waitForCondition(String javaScriptExpression) {
        try {
            checkCondition(javaScriptExpression).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    Future<Void> checkCondition(final String javaScriptExpression) {
        return executor.submit(new NoCheckedExceptionCallable<Void>() {
            @Override
            public Void call() {
                if (browser.isDisposed()) {
                    throw new BrowserDisposedException();
                }

                //TODO Maybe we don't need the callback
                final CountDownLatch countDownLatch = new CountDownLatch(1);
                String randomFunctionName = BrowserUtils.createRandomFunctionName();
                IBrowserFunction browserFunction = createBrowserFunction(new IBrowserFunction(randomFunctionName) {
                    public Object function(Object[] arguments) {
                        countDownLatch.countDown();
                        return null;
                    }
                });
                String checkScript = JavascriptString.createWaitForConditionJavascript(javaScriptExpression, randomFunctionName);

                InternalBrowserWrapper.this.run(checkScript, IConverter.CONVERTER_VOID);
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                browserFunction.dispose();
                return null;
            }
        });
    }

    <DEST> Future<DEST> executeWhenConditionIsMet(String javaScriptExpression, CallbackFunction<Void, DEST> callback) {
        return runWithCallback(checkCondition(javaScriptExpression), callback);
    }

    Future<Boolean> injectJavascript(URI scriptURI) {
        return run(scriptURI, false);
    }

    Future<Boolean> injectJavascript(File file) {
        return run(JavascriptString.createJsFileInjectionScript(file),
                IConverter.CONVERTER_BOOLEAN);
    }

    boolean syncInjectJavascript(URI scriptURI) {
        try {
            return injectJavascript(scriptURI).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException e) {
            throw (RuntimeException) e.getCause();
        }
    }


    Future<Boolean> injectCss(URI uri) {
        return run(JavascriptString.createCssFileInjectionScript(uri),
                IConverter.CONVERTER_BOOLEAN);
    }

    boolean syncInjectCssURI(URI uri) {
        try {
            return injectCss(uri).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    Future<Boolean> injectCss(String css) {
        return run(JavascriptString.createCssInjectionScript(css), IConverter.CONVERTER_BOOLEAN);
    }

    <DEST> Future<DEST> injectCss(String css, CallbackFunction<Boolean, DEST> callback) {
        return runWithCallback(injectCss(css), callback);
    }

    boolean syncInjectCss(String css) {
        try {
            return injectCss(css).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException e) {
            throw (RuntimeException) e.getCause();
        }
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

    <T> Future<T> run(final String script, final CallbackFunction<Object, T> callback) {
        return runWithCallback(InternalBrowserWrapper.this.run(script), callback);
    }


    /**
     * This method must not be called from the UI thread, as it is blocking.
     *
     * @throws IllegalStateException    if this method is called from the UI thread
     * @throws ScriptExecutionException if an exception occurs while executing the script
     */
    Object syncRun(String script) {
        return syncRun(script, IConverter.CONVERTER_IDENT);
    }

    <DEST> Future<DEST> run(final String script,
                            final IConverter<Object, DEST> converter) {
        if (isLoadingCompleted()) {
            try {
                DEST dest = uiThreadExecutor.syncExec(new ScriptExecutingCallable<DEST>(this, converter, script));
                return new CompletedFuture<DEST>(dest, null);
            } catch (RuntimeException e) {
                return new CompletedFuture<DEST>(null, e);
            }
        }
        return browserStatusManager.createFuture(new ScriptExecutingCallable<DEST>(this, converter, script));
    }

    <DEST> DEST syncRun(String script, IConverter<Object, DEST> converter) {
        try {
            return run(script, converter).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    /**
     * @throws ScriptExecutionException if an exception occurs while executing the script
     */
    <DEST> DEST runImmediately(String script,
                               IConverter<Object, DEST> converter) {
        return uiThreadExecutor.syncExec(new ScriptExecutingCallable<DEST>(this, converter, script));
    }

    /**
     * @throws ScriptExecutionException if an exception occurs while executing the script
     * @throws IOException              if an exception occurs while reading the passed file
     */
    Future<Boolean> runContent(File scriptFile) throws IOException {
        return run(FileUtils.readFileToString(scriptFile), IConverter.CONVERTER_BOOLEAN);
    }

    /**
     * @throws ScriptExecutionException if an exception occurs while executing the script
     * @throws IOException              if an exception occurs while reading the passed file
     */
    Future<Boolean> runContentAsScriptTag(File scriptFile) throws IOException {
        return run(JavascriptString.embedContentsIntoScriptTag(scriptFile), IConverter.CONVERTER_BOOLEAN);
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
        executeBeforeScriptExecutionScripts(javaScript);

        String script = JavascriptString.getExceptionReturningScript(javaScript);
        Object returnValue = browser.evaluate(script);
        BrowserUtils.rethrowJavascriptException(script, returnValue);

        executeAfterScriptExecutionScripts(returnValue);

        return returnValue;
    }

    /**
     * May be called from whatever thread.
     */
    String getUrl() {
        return uiThreadExecutor.syncExec(new NoCheckedExceptionCallable<String>() {
            @Override
            public String call() {
                return browser.getUrl();
            }
        });
    }

    /**
     * Sets a {@link Function}
     * that is executed if when a script is about to be executed by the browser.
     *
     * @param function the runnable to be executed with the script as parameter
     * @throws NullPointerException if runnable is null
     */
    void executeBeforeScript(Function<String> function) {
        beforeScripts.add(function);
    }

    /**
     * Sets a {@link Function}
     * to get executed when a script finishes execution.
     *
     * @param function the runnable to be executed with the return value of the last script execution
     * @throws NullPointerException if runnable is null
     */
    void executeAfterScript(Function<Object> function) {
        afterScripts.add(function);
    }

    /**
     * Returns the state of the browser.
     *
     * @return a status enum value
     */
    private BrowserStatus getBrowserStatus() {
        return browserStatusManager.getBrowserStatus();
    }

    boolean isLoadingCompleted() {
        return browserStatusManager.isLoadingCompleted();
    }

    protected boolean isLoading() {
        return browserStatusManager.isLoading();
    }

    protected void fireIsDisposed() {
        for (Runnable runnable : runOnDisposalList) {
            executor.submit(runnable);
        }

        synchronized (monitor) {
            browserStatusManager.setBrowserStatus(BrowserStatus.DISPOSED);
            monitor.notifyAll();
        }
        executor.shutdownNow();
    }

    void setVisible(final boolean visible) {
        executor.asyncUIExec(new Runnable() {
            @Override
            public void run() {
                browser.setVisible(visible);
            }
        });
    }

    protected void setCachedContentBounds(Rectangle rectangle) {
        cachedContentBounds = rectangle;
    }

    protected Rectangle getCachedContentBounds() {
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

    /**
     * Set a runnable to the executed just before the
     * URI is set internally. This methods may be called
     * multiple times to queue more runnables to be executed.
     *
     * @param runnable the runnable to be executed
     * @throws NullPointerException if the runnable is null
     */
    @SuppressWarnings("UnusedDeclaration")
    void executeBeforeSettingURI(final Runnable runnable) {
        beforeLoading.add(Executors.callable(runnable));
    }

    /**
     * Set a runnable to the executed just after the
     * URI is set internally. This methods may be called
     * multiple times to queue more runnables to be executed.
     *
     * @param runnable the runnable to be executed
     * @throws NullPointerException if the runnable is null
     */
    @SuppressWarnings("UnusedDeclaration")
    void executeAfterSettingURI(Runnable runnable) {
        afterLoading.add(Executors.callable(runnable));
    }

    void executeAfterCompletion(Runnable runnable) {
        afterCompletion.add(Executors.callable(runnable));
    }

    /**
     * May be called from whatever thread.
     */
    IBrowserFunction createBrowserFunction(final IBrowserFunction function) {
        return uiThreadExecutor.syncExec(new NoCheckedExceptionCallable<IBrowserFunction>() {
            @Override
            public IBrowserFunction call() {
                return browser.createBrowserFunction(function);
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

    <V, T> Future<T> runWithCallback(final Future<V> future, final CallbackFunction<V, T> callback) {
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

    void runOnDisposal(Runnable runnable) {
        runOnDisposalList.add(runnable);
    }

    void setAllowLocationChange(boolean allowed) {
        this.allowLocationChange = allowed;
    }

    boolean isDisposed() {
        return browser.isDisposed();
    }

    void checkNotUIThread() {
        uiThreadExecutor.checkNotUIThread();
    }

    void syncExec(Runnable runnable) {
        uiThreadExecutor.syncExec(runnable);
    }

    void setSize(final int width, final int height) {
        executor.asyncUIExec(new Runnable() {
            @Override
            public void run() {
                browser.setSize(width, height);
            }
        });
    }
}
