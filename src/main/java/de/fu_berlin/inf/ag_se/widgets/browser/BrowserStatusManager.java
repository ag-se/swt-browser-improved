package de.fu_berlin.inf.ag_se.widgets.browser;

import de.fu_berlin.inf.ag_se.utils.Assert;
import de.fu_berlin.inf.ag_se.utils.CompletedFuture;
import de.fu_berlin.inf.ag_se.utils.OffWorker;
import de.fu_berlin.inf.ag_se.utils.SwtUiThreadExecutor;
import de.fu_berlin.inf.ag_se.widgets.browser.exception.BrowserTimeoutException;
import de.fu_berlin.inf.ag_se.widgets.browser.exception.ScriptExecutionException;
import de.fu_berlin.inf.ag_se.widgets.browser.exception.UnexpectedBrowserStateException;
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
     * @throws de.fu_berlin.inf.ag_se.widgets.browser.exception.UnexpectedBrowserStateException
     */
    public void setBrowserStatus(BrowserStatus browserStatus)
            throws UnexpectedBrowserStateException {
        Assert.isNotNull(browserStatus);
        if (this.browserStatus == browserStatus) {
            return;
        }

        // throw exception on invalid new status
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

        // apply new status
        this.browserStatus = browserStatus;
        switch (this.browserStatus) {
            case LOADING:

                break;
            case LOADED:
                delayedScriptsWorker.start();
                delayedScriptsWorker.finish();
                break;
            case TIMEDOUT:
            case DISPOSED:
                delayedScriptsWorker.submit(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
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
                try {
                    return new CompletedFuture<DEST>(SwtUiThreadExecutor.syncExec(scriptRunner), null);
                } catch (Exception e) {
                    return new CompletedFuture<DEST>(null, e);
                }
            case TIMEDOUT:
                return new CompletedFuture<DEST>(null,
                        new ScriptExecutionException(script, new BrowserTimeoutException()));
            case DISPOSED:
                return new CompletedFuture<DEST>(null,
                        new ScriptExecutionException(script, new SWTException(SWT.ERROR_WIDGET_DISPOSED)));
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

        @Override
        public DEST call() throws Exception {
            switch (browserStatus) {
                case LOADED:
                    return SwtUiThreadExecutor.syncExec(scriptRunner);
                case TIMEDOUT:
                    throw new ScriptExecutionException(script, new BrowserTimeoutException());
                case DISPOSED:
                    throw new ScriptExecutionException(script, new SWTException(SWT.ERROR_WIDGET_DISPOSED));
                default:
                    throw new ScriptExecutionException(script, new UnexpectedBrowserStateException(browserStatus.toString()));
            }
        }
    }
}
