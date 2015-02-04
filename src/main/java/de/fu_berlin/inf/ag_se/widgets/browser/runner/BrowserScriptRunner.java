package de.fu_berlin.inf.ag_se.widgets.browser.runner;

import de.fu_berlin.inf.ag_se.utils.*;
import de.fu_berlin.inf.ag_se.widgets.browser.*;
import de.fu_berlin.inf.ag_se.widgets.browser.exception.JavaScriptException;
import de.fu_berlin.inf.ag_se.widgets.browser.exception.UnexpectedBrowserStateException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Future;

import static de.fu_berlin.inf.ag_se.widgets.browser.BrowserStatusManager.BrowserStatus;

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

    public void setBrowserStatus(BrowserStatus browserStatus) throws UnexpectedBrowserStateException {
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
            this.runImmediately(BrowserUtils.getExceptionForwardingScript("__error_callback"), IConverter.CONVERTER_VOID);
        } catch (Exception e) {
            LOGGER.error("Error activating browser's exception handling. JavaScript exceptions are not detected!", e);
        }
    }

    @Override
    public Future<Boolean> inject(URI script) {
        return run(script, false);
    }

    @Override
    public Future<Boolean> run(final File scriptFile) {
        Assert.isLegal(scriptFile.canRead());
        return run(scriptFile.toURI(), false);
    }

    @Override
    public Future<Boolean> run(final URI script) {
        return run(script, true);
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
                    new CallbackFunctionCallable(removeAfterExecution, browser, scriptURI));
        }
    }


    @Override
    public Future<Object> run(final String script) {
        return run(script, new IConverter<Object, Object>() {
            @Override
            public Object convert(Object object) {
                return object;
            }
        });
    }

    @Override
    public <DEST> Future<DEST> run(final String script, final IConverter<Object, DEST> converter) {
        Assert.isLegal(converter != null);
        return browserStatusManager.createFuture(new ScriptExecutingCallable<DEST>(browser, converter, script));
    }

    @Override
    public void runContentsImmediately(File scriptFile) throws Exception {
        this.runImmediately(FileUtils.readFileToString(scriptFile), IConverter.CONVERTER_VOID);
    }

    @Override
    public void runContentsAsScriptTagImmediately(File scriptFile) throws Exception {
        runImmediately(JavascriptString.embedContentsIntoScriptTag(scriptFile), IConverter.CONVERTER_VOID);
    }

    @Override
    public <DEST> DEST runImmediately(String script, IConverter<Object, DEST> converter) throws Exception {
        return SwtUiThreadExecutor.syncExec(new ScriptExecutingCallable<DEST>(browser, converter, script));
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
