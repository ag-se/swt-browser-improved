package de.fu_berlin.inf.ag_se.utils;

import org.apache.log4j.Logger;
import org.eclipse.swt.widgets.Display;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The methods in this class all execute stuff on the SWT UI thread
 */
public class SwtUiThreadExecutor {

    private static final Logger LOGGER = Logger.getLogger(SwtUiThreadExecutor.class);

    /**
     * Synchronously executes the given {@link NoCheckedExceptionCallable} in the SWT UI thread. Checks if the caller is already in the
     * SWT UI thread
     * and if so runs the callable directly in order to avoid deadlocks. This method can be called from any thread.
     *
     * @param callable the callable to execute
     * @return the value returned by the callable
     *
     * @throws RuntimeException if an exception occurs while executing the callable it is re-thrown
     * @UIThread
     * @NonUIThread
     */
    public static <V> V syncExec(final NoCheckedExceptionCallable<V> callable) {
        if (ExecUtils.isUIThread()) {
            return callable.call();
        }

        final AtomicReference<V> r = new AtomicReference<V>();
        final AtomicReference<RuntimeException> exception = new AtomicReference<RuntimeException>();
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    r.set(callable.call());
                } catch (RuntimeException e) {
                    exception.set(e);
                }
            }
        });
        if (exception.get() != null) {
            throw exception.get();
        }
        return r.get();
    }

    /**
     * Synchronously executes the given {@link Runnable} in the SWT UI thread. Checks if the caller is already in the SWT UI thread and if
     * so runs the
     * runnable directly in order to avoid deadlocks. This method can be called from any thread.
     *
     * @param runnable the runnable to execute
     * @throws java.lang.RuntimeException if a runtime exception occurs while executing the callable it is re-thrown
     * @UIThread
     * @NonUIThread
     */
    public static void syncExec(final Runnable runnable) throws RuntimeException {
        if (ExecUtils.isUIThread()) {
            runnable.run();
        } else {
            final AtomicReference<RuntimeException> exception = new AtomicReference<RuntimeException>();
            //TODO maybe catch SWTExceptions?
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    try {
                        runnable.run();
                    } catch (RuntimeException e) {
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
     * Asynchronously executes the given {@link NoCheckedExceptionCallable} in the SWT UI thread.
     * The return value is returned in the calling thread.
     *
     * @param callable the callable to execute
     * @return a future representing the result of the execution
     *
     * @UIThread <b>Warning: {@link java.util.concurrent.Future#get()} must not be called from the UI thread</b>
     * @NonUIThread
     */
    public static <V> Future<V> asyncExec(final NoCheckedExceptionCallable<V> callable) {
        return new UIThreadSafeFuture<V>(
                ExecUtils.EXECUTOR_SERVICE.submit(new NoCheckedExceptionCallable<V>() {
                    @Override
                    public V call() {
                        return syncExec(callable);
                    }
                }));
    }

    /**
     * Asynchronously executes the given {@link Runnable} in the SWT UI thread.
     *
     * @param runnable the runnable to execute
     * @return a future that can be used to check when the code has been executed
     *
     * @UIThread <b>Warning: {@link java.util.concurrent.Future#get()} must not be called from the UI thread</b>
     * @NonUIThread
     */
    public static Future<Void> asyncExec(final Runnable runnable) {
        return new UIThreadSafeFuture<Void>(
                ExecUtils.EXECUTOR_SERVICE
                        .submit(new NoCheckedExceptionCallable<Void>() {
                            @Override
                            public Void call() {
                                final AtomicReference<RuntimeException> exception = new AtomicReference<RuntimeException>();
                                Display.getDefault().asyncExec(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            runnable.run();
                                        } catch (RuntimeException e) {
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
}
