package de.fu_berlin.inf.ag_se.browser.threading;

public interface UIThreadExecutor {
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
    <V> V syncExec(NoCheckedExceptionCallable<V> callable);

    /**
     * Synchronously executes the given {@link Runnable} in the SWT UI thread. Checks if the caller is already in the SWT UI thread and if
     * so runs the
     * runnable directly in order to avoid deadlocks. This method can be called from any thread.
     *
     * @param runnable the runnable to execute
     * @throws RuntimeException if a runtime exception occurs while executing the callable it is re-thrown
     * @UIThread
     * @NonUIThread
     */
    void syncExec(Runnable runnable);

    /**
     * Checks if the current thread is an SWT UI thread.
     *
     * @return true if it is, false otherwise
     */
    boolean isUIThread();

    void checkNotUIThread();
}
