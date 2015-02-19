package de.fu_berlin.inf.ag_se.browser;

import de.fu_berlin.inf.ag_se.browser.exception.*;
import de.fu_berlin.inf.ag_se.browser.threading.CompletedFuture;
import de.fu_berlin.inf.ag_se.browser.threading.NoCheckedExceptionCallable;
import de.fu_berlin.inf.ag_se.browser.threading.UIThreadExecutor;
import de.fu_berlin.inf.ag_se.browser.utils.OffWorker;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

class BrowserStatusManager {

    private static Logger LOGGER = Logger.getLogger(BrowserStatusManager.class);
    private final UIThreadExecutor uiThreadExecutor;

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

    private final OffWorker delayedScriptsWorker = new OffWorker(this.getClass(), "Script Runner");

    BrowserStatusManager(UIThreadExecutor uiThreadExecutor) {
        this.uiThreadExecutor = uiThreadExecutor;
        this.browserStatus = BrowserStatus.INITIALIZING;
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

                break;
            case LOADED:
                delayedScriptsWorker.start();
                delayedScriptsWorker.finish();
                break;
            case TIMEDOUT:
            case DISPOSED:
                if (!delayedScriptsWorker.isShutdown()) {
                    delayedScriptsWorker.submit(new NoCheckedExceptionCallable<Void>() {
                        @Override
                        public Void call() {
                            delayedScriptsWorker.shutdown();
                            return null;
                        }
                    });

                    delayedScriptsWorker.start();
                    delayedScriptsWorker.finish();
                }
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
                return delayedScriptsWorker.submit(new ExecuteWhenLoaded<DEST>(scriptRunner, script));
            case LOADING:
                return delayedScriptsWorker.submit(new ExecuteWhenLoaded<DEST>(scriptRunner, script));
            case TIMEDOUT:
                return new CompletedFuture<DEST>(null,
                        new ScriptExecutionException(script, new BrowserTimeoutException()));
            case DISPOSED:
                return new CompletedFuture<DEST>(null, new BrowserDisposedException());
            default:
                return new CompletedFuture<DEST>(null,
                        new ScriptExecutionException(script,
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

    private class ExecuteWhenLoaded<DEST> implements Callable<DEST> {
        private final ScriptExecutingCallable<DEST> scriptRunner;
        private final String script;

        ExecuteWhenLoaded(ScriptExecutingCallable<DEST> scriptRunner, String script) {
            this.scriptRunner = scriptRunner;
            this.script = script;
        }

        /**
         * @throws ScriptExecutionException
         * @throws JavaScriptException
         */
        @Override
        public DEST call() {
            switch (browserStatus) {
                case LOADED:
                    return uiThreadExecutor.syncExec(scriptRunner);
                case TIMEDOUT:
                    throw new ScriptExecutionException(script, new BrowserTimeoutException());
                case DISPOSED:
                    throw new ScriptExecutionException(script, new BrowserDisposedException());
                default:
                    throw new ScriptExecutionException(script, new UnexpectedBrowserStateException(browserStatus.toString()));
            }
        }
    }
}
