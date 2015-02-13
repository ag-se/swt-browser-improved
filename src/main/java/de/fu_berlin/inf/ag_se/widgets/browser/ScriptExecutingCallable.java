package de.fu_berlin.inf.ag_se.widgets.browser;

import de.fu_berlin.inf.ag_se.utils.IConverter;
import de.fu_berlin.inf.ag_se.utils.StringUtils;
import de.fu_berlin.inf.ag_se.widgets.browser.exception.BrowserDisposedException;
import de.fu_berlin.inf.ag_se.widgets.browser.exception.ScriptExecutionException;
import de.fu_berlin.inf.ag_se.widgets.browser.threading.NoCheckedExceptionCallable;
import de.fu_berlin.inf.ag_se.widgets.browser.threading.labeling.ThreadLabelingCallable;
import org.apache.log4j.Logger;

class ScriptExecutingCallable<DEST> extends ThreadLabelingCallable<DEST> {

    private static final Logger LOGGER = Logger.getLogger(ScriptExecutingCallable.class);
    private final String script;

    ScriptExecutingCallable(final InternalBrowserWrapper browser, final IConverter<Object, DEST> converter,
                                   final String script) {
        super(Browser.class, "Running " + StringUtils.shorten(script), new NoCheckedExceptionCallable<DEST>() {
            /**
             * @throws BrowserDisposedException
             * @throws ScriptExecutionException
             */
            @Override
            public DEST call()  {
                if (browser == null || browser.isDisposed()) {
                    throw new BrowserDisposedException();
                }

                browser.executeBeforeScriptExecutionScripts(script);

                Object returnValue = browser.evaluate(script);

                browser.executeAfterScriptExecutionScripts(returnValue);

                DEST rs = converter.convert(returnValue);
                LOGGER.debug("Returned " + rs);
                return rs;
            }
        });
        this.script = script;
    }

    String getScript() {
        return script;
    }
}
