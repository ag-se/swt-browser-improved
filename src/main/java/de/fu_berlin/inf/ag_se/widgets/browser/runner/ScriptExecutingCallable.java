package de.fu_berlin.inf.ag_se.widgets.browser.runner;

import de.fu_berlin.inf.ag_se.utils.IConverter;
import de.fu_berlin.inf.ag_se.utils.StringUtils;
import de.fu_berlin.inf.ag_se.utils.thread_labeling.ThreadLabelingCallable;
import de.fu_berlin.inf.ag_se.widgets.browser.Browser;
import de.fu_berlin.inf.ag_se.widgets.browser.BrowserUtils;
import de.fu_berlin.inf.ag_se.widgets.browser.exception.JavaScriptException;
import de.fu_berlin.inf.ag_se.widgets.browser.exception.ScriptExecutionException;
import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;

import java.util.concurrent.Callable;

public class ScriptExecutingCallable<DEST> extends ThreadLabelingCallable<DEST> {

    private static final Logger LOGGER = Logger.getLogger(ScriptExecutingCallable.class);
    private final String script;

    public ScriptExecutingCallable(final Browser browser, final IConverter<Object, DEST> converter,
                                   final String script) {
        super(Browser.class, "Running " + StringUtils.shorten(script), new Callable<DEST>() {
            @Override
            public DEST call() throws Exception {
                if (browser == null || browser.isDisposed()) {
                    throw new ScriptExecutionException(script,
                            new SWTException(SWT.ERROR_WIDGET_DISPOSED));
                }

                try {
                    browser.scriptAboutToBeSentToBrowser(script);
                    Object returnValue = browser.evaluate(BrowserUtils.getExecutionReturningScript(script));

                    BrowserUtils.assertException(script, returnValue);

                    browser.scriptReturnValueReceived(returnValue);
                    DEST rs = converter.convert(returnValue);
                    LOGGER.info("Returned " + rs);
                    return rs;
                } catch (SWTException e) {
                    throw e;
                } catch (JavaScriptException e) {
                    LOGGER.error(e);
                    throw e;
                } catch (Exception e) {
                    LOGGER.error(e);
                    throw e;
                }
            }
        });
        this.script = script;
    }

    public String getScript() {
        return script;
    }
}
