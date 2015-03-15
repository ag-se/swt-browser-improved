package de.fu_berlin.inf.ag_se.browser;

import de.fu_berlin.inf.ag_se.browser.threading.NoCheckedExceptionCallable;
import org.apache.log4j.Logger;

import java.net.URI;
import java.util.concurrent.Semaphore;

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

        IBrowserFunction browserFunction = browser.createBrowserFunction(new IBrowserFunction(callbackFunctionName) {
            public Object function(Object[] arguments) {
                mutex.release();
                return null;
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
            browserFunction.dispose();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }
}
