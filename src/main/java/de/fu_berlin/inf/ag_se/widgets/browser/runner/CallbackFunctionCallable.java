package de.fu_berlin.inf.ag_se.widgets.browser.runner;

import de.fu_berlin.inf.ag_se.utils.SwtUiThreadExecutor;
import de.fu_berlin.inf.ag_se.widgets.browser.Browser;
import de.fu_berlin.inf.ag_se.widgets.browser.BrowserUtils;
import de.fu_berlin.inf.ag_se.widgets.browser.IBrowserFunction;
import de.fu_berlin.inf.ag_se.widgets.browser.JavascriptString;
import org.apache.log4j.Logger;

import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

public class CallbackFunctionCallable implements Callable<Boolean> {

    private static final Logger LOGGER = Logger.getLogger(CallbackFunctionCallable.class);

    private final boolean removeAfterExecution;
    private final Browser browser;
    private final URI scriptURI;

    public CallbackFunctionCallable(Browser browser, URI scriptURI, boolean removeAfterExecution) {
        this.removeAfterExecution = removeAfterExecution;
        this.browser = browser;
        this.scriptURI = scriptURI;
    }

    @Override
    public Boolean call() throws Exception {
        final Semaphore mutex = new Semaphore(0);
        final String callbackFunctionName = BrowserUtils.createRandomFunctionName();

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

        String js = JavascriptString.createJSForInjection(callbackFunctionName, scriptURI, removeAfterExecution);

        // runs the scripts that ends by calling the
        // callback
        // ...
        browser.run(js);
        try {
            // ... which destroys itself and releases this
            // lock
            mutex.acquire();
        } catch (InterruptedException e) {
            LOGGER.error(e);
        }
        //TODO don't return null
        return null;
    }
}
