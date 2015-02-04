package de.fu_berlin.inf.ag_se.widgets.browser.runner;

import de.fu_berlin.inf.ag_se.utils.*;
import de.fu_berlin.inf.ag_se.widgets.browser.Browser;
import de.fu_berlin.inf.ag_se.widgets.browser.BrowserStatusManager;
import de.fu_berlin.inf.ag_se.widgets.browser.BrowserUtils;
import de.fu_berlin.inf.ag_se.widgets.browser.IBrowserFunction;
import de.fu_berlin.inf.ag_se.widgets.browser.exception.JavaScriptException;
import de.fu_berlin.inf.ag_se.widgets.browser.exception.UnexpectedBrowserStateException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import static de.fu_berlin.inf.ag_se.widgets.browser.BrowserStatusManager.BrowserStatus;

/**
 * This is the default implementation of {@link IBrowserScriptRunner}.
 *
 * @author bkahlert
 */
public class BrowserScriptRunner implements IBrowserScriptRunner {

    /**
     * Instances of this class can handle asynchronous {@link JavaScriptException}s, that are exceptions raised by the {@link
     * org.eclipse.swt.browser.Browser} itself and not provoked by Java invocations.
     *
     * @author bkahlert
     */
    public static interface JavaScriptExceptionListener {
        public void thrown(JavaScriptException javaScriptException);
    }

    private static final Logger LOGGER = Logger.getLogger(BrowserScriptRunner.class);

    private final Browser browser;
    private BrowserStatusManager browserStatusManager;

    public BrowserScriptRunner(Browser browser, final JavaScriptExceptionListener javaScriptExceptionListener) {
        Assert.isNotNull(browser);
        this.browser = browser;
        this.browserStatusManager = new BrowserStatusManager();

        // throws exception that arise from calls within the browser,
        // meaning code that has not been invoked by Java but by JavaScript
        browser.createBrowserFunction("__error_callback", new IBrowserFunction() {
            @Override
            public Object function(Object[] arguments) {
                JavaScriptException javaScriptException = BrowserUtils
                        .parseJavaScriptException(arguments);
                LOGGER.error(javaScriptException);
                if (javaScriptExceptionListener != null) {
                    javaScriptExceptionListener.thrown(javaScriptException);
                }
                return false;
            }
        });
    }

    public void setBrowserStatus(BrowserStatus browserStatus)
                throws UnexpectedBrowserStateException {
        browserStatusManager.setBrowserStatus(browserStatus);
    }

    /**
     * Returns the state of the browser.
     *
     * @return
     */
    public BrowserStatus getBrowserStatus() {
        return browserStatusManager.getBrowserStatus();
    }

    /**
     * Notifies all registered Javascript exception listeners in case a JavaScript error occurred.
     */
    public void activateExceptionHandling() {
        try {
            this.runImmediately(BrowserUtils
                            .getExceptionForwardingScript("__error_callback"),
                    IConverter.CONVERTER_VOID);
        } catch (Exception e) {
            LOGGER.error(
                    "Error activating browser's exception handling. JavaScript exceptions are not detected!",
                    e);
        }
    }

    @Override
    public Future<Boolean> inject(URI script) {
        return this.run(script, false);
    }

    @Override
    public Future<Boolean> run(final File script) {
        Assert.isLegal(script.canRead());
        try {
            return this.run(new URI("file://" + script.getAbsolutePath()));
        } catch (URISyntaxException e) {
            return new CompletedFuture<Boolean>(false, e);
        }
    }

    @Override
    public Future<Boolean> run(final URI script) {
        return this.run(script, true);
    }

    private Future<Boolean> run(final URI script,
                                final boolean removeAfterExecution) {
        Assert.isLegal(script != null);

        if ("file".equalsIgnoreCase(script.getScheme())) {
            File file = new File(script.toString()
                                       .substring("file://".length()));
            try {
                String scriptContent = FileUtils.readFileToString(file);
                Future<Boolean> rs = this.run(scriptContent,
                        new IConverter<Object, Boolean>() {
                            @Override
                            public Boolean convert(Object returnValue) {
                                return true;
                            }
                        });
                if (removeAfterExecution) {
                    LOGGER.warn("The script "
                            + script
                            + " is on the local file system. To circument security restrictions its content becomes directly executed and thus cannot be removed.");
                }
                return rs;
            } catch (IOException e) {
                return new CompletedFuture<Boolean>(null, e);
            }
        } else {
            return ExecUtils.nonUIAsyncExec(Browser.class,
                    "Script Runner for: " + script, new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            final String callbackFunctionName = BrowserUtils
                                    .createRandomFunctionName();

                            final Semaphore mutex = new Semaphore(0);
                            SwtUiThreadExecutor.syncExec(new Runnable() {
                                @Override
                                public void run() {
                                    final AtomicReference<IBrowserFunction> callback = new AtomicReference<IBrowserFunction>();
                                    callback.set(browser.createBrowserFunction(
                                            callbackFunctionName, new IBrowserFunction() {
                                        public Object function(Object[] arguments) {
                                            callback.get().dispose();
                                            mutex.release();
                                            return null;
                                        }
                                    }));
                                }
                            });

                            String js = "var h = document.getElementsByTagName(\"head\")[0]; var s = document.createElement(\"script\");s.type = \"text/javascript\";s.src = \""
                                    + script.toString()
                                    + "\"; s.onload=function(e){";
                            if (removeAfterExecution) {
                                js += "h.removeChild(s);";
                            }
                            js += callbackFunctionName + "();";
                            js += "};h.appendChild(s);";

                            // runs the scripts that ends by calling the
                            // callback
                            // ...
                            BrowserScriptRunner.this.run(js);
                            try {
                                // ... which destroys itself and releases this
                                // lock
                                mutex.acquire();
                            } catch (InterruptedException e) {
                                LOGGER.error(e);
                            }
                            return null;
                        }
                    });
        }
    }

    @Override
    public Future<Object> run(final String script) {
        return this.run(script, new IConverter<Object, Object>() {
            @Override
            public Object convert(Object object) {
                return object;
            }
        });
    }

    @Override
    public <DEST> Future<DEST> run(final String script,
                                   final IConverter<Object, DEST> converter) {
        Assert.isLegal(converter != null);
        final ScriptExecutingCallable<DEST> scriptRunner = new ScriptExecutingCallable<DEST>(browser, converter, script);

        return browserStatusManager.createFuture(scriptRunner);
    }

    @Override
    public <DEST> DEST runImmediately(String script, IConverter<Object, DEST> converter) throws Exception {
        return SwtUiThreadExecutor.syncExec(new ScriptExecutingCallable<DEST>(browser, converter, script));
    }

    @Override
    public void runContentsImmediately(File script) throws Exception {
        String scriptContent = FileUtils.readFileToString(script);
        this.runImmediately(scriptContent, IConverter.CONVERTER_VOID);
    }

    @Override
    public void runContentsAsScriptTagImmediately(File scriptFile)
            throws Exception {
        String scriptContent = FileUtils.readFileToString(scriptFile);
        String script = "var script=document.createElement(\"script\"); script.type=\"text/javascript\"; script.text=\""
                + StringEscapeUtils.escapeJavaScript(scriptContent)
                + "\"; document.getElementsByTagName(\"head\")[0].appendChild(script);";
        this.runImmediately(script, IConverter.CONVERTER_VOID);
    }

    @Override
    public void scriptAboutToBeSentToBrowser(String script) {
        return;
    }

    @Override
    public void scriptReturnValueReceived(Object returnValue) {
        return;
    }
}
