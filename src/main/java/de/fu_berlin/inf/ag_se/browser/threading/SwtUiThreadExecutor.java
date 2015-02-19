package de.fu_berlin.inf.ag_se.browser.threading;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Display;

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
        if (isUIThread()) {
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
    public static void syncExec(final Runnable runnable) {
        if (isUIThread()) {
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
     * Checks if the current thread is an SWT UI thread.
     *
     * @return true if it is, false otherwise
     */
    public static boolean isUIThread() {
        try {
            return Display.getCurrent() != null;
        } catch (SWTException e) {
            return false;
        }
    }

    public static void checkNotUIThread() {
        if (isUIThread()) {
            throw new IllegalStateException("This method must not be called from the UI thread.");
        }
    }
}
