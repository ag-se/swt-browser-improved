package de.fu_berlin.inf.ag_se.widgets.browser;

import de.fu_berlin.inf.ag_se.utils.*;
import de.fu_berlin.inf.ag_se.widgets.browser.exception.*;
import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class BrowserStatusManager {

    private static Logger LOGGER = Logger.getLogger(BrowserStatusManager.class);

    /**
     * Relevant statuses a browser can have in terms of script execution.
     */
    public static enum BrowserStatus {
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

    public BrowserStatusManager() {
        this.browserStatus = BrowserStatus.INITIALIZING;
    }

    /**
     * Sets the browser status. This information is necessary for the correct script execution.
     *
     * @param browserStatus
     * @throws UnexpectedBrowserStateException
     * @throws NullPointerException            if argument is null
     */
    public void setBrowserStatus(BrowserStatus browserStatus) {
        if (browserStatus == null) {
            throw new NullPointerException();
        }

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
                if (Arrays.asList(BrowserStatus.TIMEDOUT, BrowserStatus.DISPOSED)
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
                throw new UnexpectedBrowserStateException("Cannot switch from "
                        + this.browserStatus + " to " + browserStatus);
            case TIMEDOUT:
                throw new UnexpectedBrowserStateException("Cannot switch from "
                        + this.browserStatus + " to " + browserStatus);
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
                delayedScriptsWorker.submit(new NoCheckedExceptionCallable<Void>() {
                    @Override
                    public Void call() {
                        if (!delayedScriptsWorker.isShutdown()) {
                            delayedScriptsWorker.shutdown();
                        }
                        return null;
                    }
                });

                delayedScriptsWorker.start();
                delayedScriptsWorker.finish();
                break;
            default:
        }
    }

    public BrowserStatus getBrowserStatus() {
        return browserStatus;
    }

    public <DEST> Future<DEST> createFuture(final ScriptExecutingCallable<DEST> scriptRunner) {
        final String script = scriptRunner.getScript();
        switch (browserStatus) {
            case INITIALIZING:
                return delayedScriptsWorker.submit(new ExecuteWhenLoaded<DEST>(scriptRunner, script));
            case LOADING:
                return delayedScriptsWorker.submit(new ExecuteWhenLoaded<DEST>(scriptRunner, script));
            case LOADED:
                return ExecUtils.nonUIAsyncExec(new ExecuteImmediately<DEST>(scriptRunner));
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

    protected Boolean queryLoadingStatus(String uri) {
        switch (getBrowserStatus()) {
            case LOADED:
                LOGGER.debug("Successfully loaded " + uri);
                break;
            case TIMEDOUT:
                LOGGER.warn("Aborted loading " + uri + " due to timeout");
                break;
            case DISPOSED:
                LOGGER.info("Aborted loading " + uri + " due to disposal");
                break;
            default:
                throw new RuntimeException("Implementation error");
        }

        return getBrowserStatus() == BrowserStatusManager.BrowserStatus.LOADED;
    }

    protected boolean isLoading() {
        return getBrowserStatus() == BrowserStatusManager.BrowserStatus.LOADING;
    }

    private class ExecuteWhenLoaded<DEST> implements Callable<DEST> {
        private final ScriptExecutingCallable<DEST> scriptRunner;
        private final String script;

        public ExecuteWhenLoaded(ScriptExecutingCallable<DEST> scriptRunner, String script) {
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
                    return syncExecScript(scriptRunner);
                case TIMEDOUT:
                    throw new ScriptExecutionException(script, new BrowserTimeoutException());
                case DISPOSED:
                    throw new ScriptExecutionException(script, new BrowserDisposedException());
                default:
                    throw new ScriptExecutionException(script, new UnexpectedBrowserStateException(browserStatus.toString()));
            }
        }
    }

    private class ExecuteImmediately<DEST> implements NoCheckedExceptionCallable<DEST> {
        private final ScriptExecutingCallable<DEST> scriptRunner;

        public ExecuteImmediately(ScriptExecutingCallable<DEST> scriptRunner) {
            this.scriptRunner = scriptRunner;
        }

        /**
         * @throws ScriptExecutionException
         * @throws JavaScriptException
         */
        @Override
        public DEST call() {
            return syncExecScript(scriptRunner);
        }
    }

    /**
     * @throws ScriptExecutionException
     * @throws JavaScriptException
     */
    private <DEST> DEST syncExecScript(ScriptExecutingCallable<DEST> scriptRunner) {
        return SwtUiThreadExecutor.syncExec(scriptRunner);
    }
}
