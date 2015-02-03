package de.fu_berlin.inf.ag_se.utils;

import org.apache.log4j.Logger;
import org.eclipse.swt.widgets.Display;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The methods in this class all execute stuff on the SWT UI thread
 */
public class SwtUiThreadExecutor {

    private static final Logger LOGGER = Logger.getLogger(SwtUiThreadExecutor.class);

    /**
     * Executes the given {@link java.util.concurrent.Callable}. <p> Checks if the caller is already in the UI thread and if so runs the
     * runnable directly in order to avoid deadlocks.
     *
     * @param callable
     * @UIThread
     * @NonUIThread
     */
    public static <V> V syncExec(final Callable<V> callable) throws Exception {
        if (ExecUtils.isUIThread()) {
            return callable.call();
        }

        final AtomicReference<V> r = new AtomicReference<V>();
        final AtomicReference<Exception> exception = new AtomicReference<Exception>();
        final Semaphore mutex = new Semaphore(0);
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    r.set(callable.call());
                } catch (Exception e) {
                    exception.set(e);
                }
                mutex.release();
            }
        });
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            LOGGER.error(e);
        }
        if (exception.get() != null) {
            throw exception.get();
        }
        return r.get();
    }

    /**
     * Executes the given {@link Runnable}. <p> Checks if the caller is already in the UI thread and if so runs the runnable directly in
     * order to avoid deadlocks.
     *
     * @param runnable
     * @throws Exception
     * @UIThread
     * @NonUIThread
     */
    public static void syncExec(final Runnable runnable) throws Exception {
        if (ExecUtils.isUIThread()) {
            runnable.run();
        } else {
            final AtomicReference<Exception> exception = new AtomicReference<Exception>();
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    try {
                        runnable.run();
                    } catch (Exception e) {
                        exception.set(e);
                    }
                }
            });
            if (exception.get() != null) {
                throw exception.get();
            }
        }
    }

    /**
     * Executes the given {@link java.util.concurrent.Callable} asynchronously in the UI thread. <p> The return value is returned in the
     * calling thread.
     *
     * @param callable
     * @return
     * @UIThread <b>Warning: {@link java.util.concurrent.Future#get()} must not be called from the UI thread</b>
     * @NonUIThread must not be called from the UI thread
     */
    public static <V> Future<V> asyncExec(final Callable<V> callable) {
        return new UIThreadSafeFuture<V>(
                ExecUtils.EXECUTOR_SERVICE.submit(new Callable<V>() {
                    @Override
                    public V call() throws Exception {
                        return syncExec(callable);
                    }
                }));
    }

    /**
     * Executes the given {@link Runnable} asynchronously in the UI thread.
     *
     * @param runnable
     * @return can be used to check when the code has been executed
     * @UIThread <b>Warning: {@link java.util.concurrent.Future#get()} must not be called from the UI thread</b>
     * @NonUIThread must not be called from the UI thread
     */
    public static Future<Void> asyncExec(final Runnable runnable) {
        return new UIThreadSafeFuture<Void>(
                ExecUtils.EXECUTOR_SERVICE
                        .submit(new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                final AtomicReference<Exception> exception = new AtomicReference<Exception>();
                                Display.getDefault().asyncExec(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            runnable.run();
                                        } catch (Exception e) {
                                            exception.set(e);
                                        }
                                    }
                                });
                                if (exception.get() != null) {
                                    throw exception.get();
                                }
                                return null;
                            }
                        }));
    }

    /**
     * Executes the given {@link java.util.concurrent.Callable} with a delay and asynchronously in the UI thread.
     *
     * @param callable
     * @param delay
     * @return
     * @UIThread <b>Warning: {@link java.util.concurrent.Future#get()} must not be called from the UI thread</b>
     * @NonUIThread must not be called from the UI thread
     *
     * TODO implement using Display.timerExec
     */
    public static <V> Future<V> asyncExec(final Callable<V> callable,
                                          final long delay) {
        return new UIThreadSafeFuture<V>(ExecUtils.EXECUTOR_SERVICE.submit(new Callable<V>() {
            @Override
            public V call() throws Exception {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    LOGGER.error("Could not execute with a delay callable "
                            + callable);
                }

                return syncExec(callable);
            }
        }));
    }

    /**
     * Executes the given {@link Runnable} with a delay and asynchronously in the UI thread.
     *
     * @param runnable
     * @param delay
     * @UIThread <b>Warning: {@link java.util.concurrent.Future#get()} must not be called from the UI thread</b>
     * @NonUIThread TODO implement using Display.timerExec
     */
    public static Future<Void> asyncExec(final Runnable runnable,
                                         final long delay) {
        return new UIThreadSafeFuture<Void>(ExecUtils.EXECUTOR_SERVICE.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    LOGGER.error("Could not execute with a delay runnable "
                            + runnable);
                }

                syncExec(runnable);
                return null;
            }
        }));
    }
}
