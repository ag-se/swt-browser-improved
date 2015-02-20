package de.fu_berlin.inf.ag_se.browser;

import de.fu_berlin.inf.ag_se.browser.exception.BrowserDisposedException;
import de.fu_berlin.inf.ag_se.browser.exception.ScriptExecutionException;
import de.fu_berlin.inf.ag_se.browser.threading.NoCheckedExceptionCallable;
import de.fu_berlin.inf.ag_se.browser.threading.labeling.ThreadLabelingCallable;
import de.fu_berlin.inf.ag_se.browser.utils.IConverter;
import de.fu_berlin.inf.ag_se.browser.utils.StringUtils;
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

                Object returnValue = browser.evaluate(script);

                DEST rs = converter.convert(returnValue);
                LOGGER.debug(StringUtils.shorten(script) + " returned " + rs);
                return rs;
            }
        });
        this.script = script;
    }

    String getScript() {
        return script;
    }
}
