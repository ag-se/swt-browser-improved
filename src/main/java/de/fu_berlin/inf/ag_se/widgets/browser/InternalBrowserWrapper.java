package de.fu_berlin.inf.ag_se.widgets.browser;

import de.fu_berlin.inf.ag_se.utils.*;
import de.fu_berlin.inf.ag_se.widgets.browser.exception.JavaScriptException;
import de.fu_berlin.inf.ag_se.widgets.browser.exception.UnexpectedBrowserStateException;
import de.fu_berlin.inf.ag_se.widgets.browser.runner.CallbackFunctionCallable;
import de.fu_berlin.inf.ag_se.widgets.browser.runner.JavaScriptExceptionListener;
import de.fu_berlin.inf.ag_se.widgets.browser.runner.ScriptExecutingCallable;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

public class InternalBrowserWrapper {

    private static Logger LOGGER = Logger.getLogger(InternalBrowserWrapper.class);

    private Browser browser;

    private BrowserStatusManager browserStatusManager;

    private boolean settingUri = false;

    private boolean allowLocationChange = false;

    private final Object monitor = new Object();

    Rectangle cachedContentBounds = null;

    List<Runnable> beforeLoading = new ArrayList<Runnable>();

    List<Runnable> afterLoading = new ArrayList<Runnable>();

    List<Runnable> beforeCompletion = new ArrayList<Runnable>();

    private final List<JavaScriptExceptionListener> javaScriptExceptionListeners = Collections
            .synchronizedList(new ArrayList<JavaScriptExceptionListener>());

    public InternalBrowserWrapper(Composite parent) {
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
                JavaScriptException javaScriptException = BrowserUtils
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
                    event.doit = allowLocationChange || getBrowserStatus() == BrowserStatusManager.BrowserStatus.LOADING;
                }
            }
        });

        browser.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                synchronized (monitor) {
                    if (getBrowserStatus() == BrowserStatusManager.BrowserStatus.LOADING) {
                        setBrowserStatus(BrowserStatusManager.BrowserStatus.DISPOSED);
                    }
                    monitor.notifyAll();
                }
            }
        });
    }

    public Future<Boolean> open(final String uri, final Integer timeout,
                                final String pageLoadCheckExpression) {
        if (browser.isDisposed()) {
            throw new SWTException(SWT.ERROR_WIDGET_DISPOSED);
        }

        setBrowserStatus(BrowserStatusManager.BrowserStatus.LOADING);
        activateExceptionHandling();

        browser.addProgressListener(new ProgressAdapter() {
            @Override
            public void completed(ProgressEvent event) {
                waitAndComplete(pageLoadCheckExpression);
            }
        });

        return ExecUtils.nonUIAsyncExec(Browser.class, "Opening " + uri,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        // stops waiting after timeout
                        Future<Void> timeoutMonitor = null;
                        if (timeout != null && timeout > 0) {
                            timeoutMonitor = ExecUtils.nonUIAsyncExec(Browser.class,
                                    "Timeout Watcher for " + uri, new Runnable() {
                                        @Override
                                        public void run() {
                                            synchronized (monitor) {
                                                if (getBrowserStatus() != BrowserStatusManager.BrowserStatus.LOADED) {
                                                    setBrowserStatus(BrowserStatusManager.BrowserStatus.TIMEDOUT);
                                                }
                                                monitor.notifyAll();
                                            }
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
                                monitor.wait();
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

    /**
     * This method waits for the {@link de.fu_berlin.inf.ag_se.widgets.browser.Browser} to complete loading. <p> It has been observed that
     * the {@link org.eclipse.swt.browser.ProgressListener#completed(org.eclipse.swt.browser.ProgressEvent)} fires to early. This method
     * uses JavaScript to reliably detect the completed state.
     *
     * @param pageLoadCheckExpression
     */
    private void waitAndComplete(String pageLoadCheckExpression) {
        if (browser == null || browser.isDisposed()) {
            return;
        }

        if (getBrowserStatus() != BrowserStatusManager.BrowserStatus.LOADING) {
            if (!Arrays.asList(BrowserStatusManager.BrowserStatus.TIMEDOUT, BrowserStatusManager.BrowserStatus.DISPOSED)
                       .contains(getBrowserStatus())) {
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
        } catch (Exception e) {
            LOGGER.error("An error occurred while checking the page load state", e);
            synchronized (monitor) {
                monitor.notifyAll();
            }
        }
    }

    public void waitForCondition(String condition) {
        if (browser == null || browser.isDisposed()) {
            return;
        }

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        String randomFunctionName = BrowserUtils.createRandomFunctionName();
        IBrowserFunction browserFunction = createBrowserFunction(randomFunctionName, new IBrowserFunction() {
            public Object function(Object[] arguments) {
                countDownLatch.countDown();
                return null;
            }
        });
        String checkScript = JavascriptString.createWaitForConditionJavascript(condition,
                randomFunctionName);

        try {
            runImmediately(checkScript, IConverter.CONVERTER_VOID);
            countDownLatch.await();
            browserFunction.dispose();
        } catch (Exception e) {
            LOGGER.error("An error occurred while checking the condition", e);
        }
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
                            if (!Arrays.asList(BrowserStatusManager.BrowserStatus.TIMEDOUT,
                                    BrowserStatusManager.BrowserStatus.DISPOSED).contains(getBrowserStatus())) {
                                setBrowserStatus(BrowserStatusManager.BrowserStatus.LOADED);
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
            runImmediately(BrowserUtils.getExceptionForwardingScript("__error_callback"), IConverter.CONVERTER_VOID);
        } catch (Exception e) {
            LOGGER.error("Error activating browser's exception handling. JavaScript exceptions are not detected!", e);
        }
    }

    public Future<Boolean> inject(URI scriptURI) {
        return run(scriptURI, false);
    }

    public Future<Boolean> run(File scriptFile) {
        Assert.isLegal(scriptFile.canRead());
        return run(scriptFile.toURI(), false);
    }

    public Future<Boolean> run(final URI scriptURI) {
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

    public Future<Object> run(final String script) {
        return run(script, new IConverter<Object, Object>() {
            @Override
            public Object convert(Object object) {
                return object;
            }
        });
    }

    public <DEST> Future<DEST> run(final String script,
                                   final IConverter<Object, DEST> converter) {
        Assert.isLegal(converter != null);
        return browserStatusManager.createFuture(new ScriptExecutingCallable<DEST>(this, converter, script));
    }

    public <DEST> DEST runImmediately(String script,
                                      IConverter<Object, DEST> converter) throws Exception {
        return SwtUiThreadExecutor.syncExec(new ScriptExecutingCallable<DEST>(this, converter, script));
    }

    public void runContentsImmediately(File scriptFile) throws Exception {
        runImmediately(FileUtils.readFileToString(scriptFile), IConverter.CONVERTER_VOID);
    }

    public void runContentsAsScriptTagImmediately(File scriptFile)
            throws Exception {
        runImmediately(JavascriptString.embedContentsIntoScriptTag(scriptFile), IConverter.CONVERTER_VOID);
    }

    public void scriptAboutToBeSentToBrowser(String script) {
        return;
    }

    public void scriptReturnValueReceived(Object returnValue) {
        return;
    }

    public Future<Void> injectJsFile(File file) {
        return run(JavascriptString.createJsFileInjectionScript(file),
                IConverter.CONVERTER_VOID);
    }

    public void injectJsFileImmediately(File file) throws Exception {
        runImmediately(JavascriptString.createJsFileInjectionScript(file),
                IConverter.CONVERTER_VOID);
    }

    public Future<Void> injectCssFile(URI uri) {
        return run(JavascriptString.createCssFileInjectionScript(uri),
                IConverter.CONVERTER_VOID);
    }

    public void injectCssFileImmediately(URI uri) throws Exception {
        runImmediately(JavascriptString.createCssFileInjectionScript(uri),
                IConverter.CONVERTER_VOID);
    }

    public Future<Void> injectCss(String css) {
        return run(JavascriptString.createCssInjectionScript(css), IConverter.CONVERTER_VOID);
    }

    public void injectCssImmediately(String css) throws Exception {
        runImmediately(JavascriptString.createCssInjectionScript(css),
                IConverter.CONVERTER_VOID);
    }

    /**
     * Returns the state of the browser.
     *
     * @return
     */
    public BrowserStatusManager.BrowserStatus getBrowserStatus() {
        return browserStatusManager.getBrowserStatus();
    }

    public void setBrowserStatus(BrowserStatusManager.BrowserStatus browserStatus) throws UnexpectedBrowserStateException {
        browserStatusManager.setBrowserStatus(browserStatus);
    }

    public void setAllowLocationChange(boolean allowed) {
        this.allowLocationChange = allowed;
    }

    public boolean isLoadingCompleted() {
        return getBrowserStatus() == BrowserStatusManager.BrowserStatus.LOADED;
    }

    public boolean isDisposed() {
        return browser.isDisposed();
    }

    public Object evaluate(String javaScript) {
        return browser.evaluate(javaScript);
    }

    public String getUrl() {
        return browser.getUrl();
    }

    public void addListener(int eventType, Listener listener) {
        // TODO evtl. erst ausführen, wenn alles wirklich geladen wurde, um
        // evtl. falsche Mauskoordinaten zu verhindern und so ein Fehlverhalten
        // im InformationControl vorzeugen
        browser.addListener(eventType, listener);
    }

    protected void layoutRoot() {
        Composite root = SWTUtils.getRoot(browser);
        LOGGER.debug("layout all");
        root.layout(true, true);
    }

    public void setCachedContentBounds(Rectangle rectangle) {
        cachedContentBounds = rectangle;
    }

    public Rectangle getCachedContentBounds() {
        return cachedContentBounds;
    }

    synchronized protected void fireJavaScriptExceptionThrown(
            JavaScriptException javaScriptException) {
        for (JavaScriptExceptionListener listener : javaScriptExceptionListeners) {
            listener.thrown(javaScriptException);
        }
    }

    public void addJavaScriptExceptionListener(
            JavaScriptExceptionListener javaScriptExceptionListener) {
        javaScriptExceptionListeners.add(javaScriptExceptionListener);
    }

    public void removeJavaScriptExceptionListener(
            JavaScriptExceptionListener javaScriptExceptionListener) {
        javaScriptExceptionListeners.remove(javaScriptExceptionListener);
    }

    public void executeBeforeLoading(Runnable runnable) {
        beforeLoading.add(runnable);
    }

    public void executeAfterLoading(Runnable runnable) {
        afterLoading.add(runnable);
    }

    public void executeBeforeCompletion(Runnable runnable) {
        beforeCompletion.add(runnable);
    }

    public IBrowserFunction createBrowserFunction(final String functionName,
                                                  final IBrowserFunction function) {
        try {
            return SwtUiThreadExecutor.syncExec(new Callable<IBrowserFunction>() {
                @Override
                public IBrowserFunction call() throws Exception {
                    new BrowserFunction(browser, functionName) {
                        @Override
                        public Object function(Object[] arguments) {
                            return function.function(arguments);
                        }
                    };
                    return function;
                }
            });
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        return null;
    }
}
