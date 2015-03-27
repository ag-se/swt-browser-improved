package de.fu_berlin.inf.ag_se.browser;

import com.google.common.util.concurrent.Futures;
import de.fu_berlin.inf.ag_se.browser.exception.BrowserDisposedException;
import de.fu_berlin.inf.ag_se.browser.exception.BrowserTimeoutException;
import de.fu_berlin.inf.ag_se.browser.exception.ScriptExecutionException;
import de.fu_berlin.inf.ag_se.browser.exception.UnexpectedBrowserStateException;
import de.fu_berlin.inf.ag_se.browser.utils.DelayedScriptRunner;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.concurrent.Future;

class BrowserStatusManager {

    private static Logger LOGGER = Logger.getLogger(BrowserStatusManager.class);

    /**
     * Relevant statuses a browser can have in terms of script execution.
     */
    static enum BrowserStatus {
        /**
         * The browser is initializing, meaning no resource had been loaded, yet.
         */
        INITIALIZING,

        /**
         * The browser is currently loading a resource.
         */
        LOADING,

        /**
         * The browser has successfully loaded a resource.
         */
        LOADED,

        /**
         * The browser timed out.
         */
        TIMEDOUT,

        /**
         * The browser is currently disposing or already disposed.
         */
        DISPOSED
    }

    private BrowserStatus browserStatus;

    private final DelayedScriptRunner delayedScriptsWorker;

    BrowserStatusManager() {
        this.browserStatus = BrowserStatus.INITIALIZING;
        delayedScriptsWorker = new DelayedScriptRunner();
    }

    /**
     * Sets the browser status. This information is necessary for the correct script execution.
     *
     * @param browserStatus
     * @throws UnexpectedBrowserStateException
     * @throws NullPointerException            if argument is null
     */
    synchronized void setBrowserStatus(BrowserStatus browserStatus) {
        if (this.browserStatus == browserStatus) {
            return;
        }

        checkStateTransition(browserStatus);

        this.browserStatus = browserStatus;

        callScriptWorker();
    }

    // throw exception on invalid new status
    private void checkStateTransition(BrowserStatus browserStatus) {
        switch (this.browserStatus) {
            case INITIALIZING:
                if (Arrays.asList(BrowserStatus.TIMEDOUT, BrowserStatus.LOADED)
                          .contains(browserStatus)) {
                    throw new UnexpectedBrowserStateException("Cannot switch from "
                            + this.browserStatus + " to " + browserStatus);
                }
                break;
            case LOADING:
                if (browserStatus == BrowserStatus.INITIALIZING) {
                    throw new UnexpectedBrowserStateException("Cannot switch from "
                            + this.browserStatus + " to " + browserStatus);
                }
                break;
            case LOADED:
                if (browserStatus == BrowserStatus.TIMEDOUT) {
                    return;
                }
                if (browserStatus != BrowserStatus.DISPOSED) {
                    throw new UnexpectedBrowserStateException("Cannot switch from "
                            + this.browserStatus + " to " + browserStatus);
                }
                break;
            case TIMEDOUT:
                if (browserStatus != BrowserStatus.DISPOSED) {
                    throw new UnexpectedBrowserStateException("Cannot switch from "
                            + this.browserStatus + " to " + browserStatus);
                }
                break;
            case DISPOSED:
                throw new UnexpectedBrowserStateException("Cannot switch from "
                        + this.browserStatus + " to " + browserStatus);
            default:
                throw new UnexpectedBrowserStateException("Cannot switch from "
                        + this.browserStatus + " to " + browserStatus);
        }
    }

    private void callScriptWorker() {
        switch (this.browserStatus) {
            case LOADING:
                delayedScriptsWorker.flush();
                break;
            case LOADED:
                delayedScriptsWorker.start();
                break;
            case TIMEDOUT:
                break;
            case DISPOSED:
                delayedScriptsWorker.stop();
                break;
            default:
        }
    }

    synchronized BrowserStatus getBrowserStatus() {
        return browserStatus;
    }

    synchronized <DEST> Future<DEST> createFuture(final ScriptExecutingCallable<DEST> scriptRunner) {
        final String script = scriptRunner.getScript();
        switch (browserStatus) {
            case INITIALIZING:
                return delayedScriptsWorker.submit(scriptRunner);
            case LOADING:
                return delayedScriptsWorker.submit(scriptRunner);
            case LOADED:
                return delayedScriptsWorker.submit(scriptRunner);
            case TIMEDOUT:
                return Futures.immediateFailedFuture(new ScriptExecutionException(script, new BrowserTimeoutException()));
            case DISPOSED:
                return Futures.immediateFailedFuture(new BrowserDisposedException());
            default:
                return Futures.immediateFailedFuture(new ScriptExecutionException(script,
                                new UnexpectedBrowserStateException(browserStatus.toString())));
        }
    }

    Boolean wasLoadingSuccessful(String uri) {
        switch (getBrowserStatus()) {
            case LOADED:
                LOGGER.debug("Successfully loaded " + uri);
                return true;
            case TIMEDOUT:
                LOGGER.warn("Aborted loading " + uri + " due to timeout");
                return false;
            case DISPOSED:
                LOGGER.info("Aborted loading " + uri + " due to disposal");
                return false;
            default:
                throw new RuntimeException("Implementation error");
        }
    }

    boolean isLoading() {
        return getBrowserStatus() == BrowserStatusManager.BrowserStatus.LOADING;
    }

    boolean isLoadingCompleted() {
        return getBrowserStatus() == BrowserStatus.LOADED;
    }
}
