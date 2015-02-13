package de.fu_berlin.inf.ag_se.widgets.browser;

import de.fu_berlin.inf.ag_se.widgets.browser.threading.NoCheckedExceptionCallable;
import de.fu_berlin.inf.ag_se.widgets.browser.threading.SwtUiThreadExecutor;
import org.apache.log4j.Logger;

import java.net.URI;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

public class CallbackFunctionCallable implements NoCheckedExceptionCallable<Boolean> {

    private static final Logger LOGGER = Logger.getLogger(CallbackFunctionCallable.class);

    private final boolean removeAfterExecution;
    private final InternalBrowserWrapper browser;
    private final URI scriptURI;

    public CallbackFunctionCallable(InternalBrowserWrapper browser, URI scriptURI, boolean removeAfterExecution) {
        this.removeAfterExecution = removeAfterExecution;
        this.browser = browser;
        this.scriptURI = scriptURI;
    }

    @Override
    public Boolean call() {
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
