package de.fu_berlin.inf.ag_se.utils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This {@link java.util.concurrent.Future} implementation addresses a possible deadlock that can
 * occur if an UI thread call triggers a new thread that itself needs an UI thread call.
 * It throws an exception to inform about the programming error.
 * <p>
 *
 * @param <V>
 * @author bkahlert
 */
public class UIThreadSafeFuture<V> implements Future<V> {
    private final Future<V> future;

    public UIThreadSafeFuture(Future<V> future) {
        Assert.isNotNull(future);
        this.future = future;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return this.future.cancel(mayInterruptIfRunning);
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        if (ExecUtils.isUIThread() && !this.isDone()) {
            throw new ExecutionException(
                    "Waiting is not allowed from the UI thread. Should the calculation include UI thread code, this could lead to a deadlock.\nWait in another thread or check isDone() before calling get().",
                    null);
        }
        return this.future.get();
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
        if (ExecUtils.isUIThread() && !this.isDone()) {
            throw new ExecutionException(
                    "Waiting is not allowed from the UI thread. Should the calculation include UI thread code, this could lead to a deadlock.\nWait in another thread or check isDone() before calling get(...).",
                    null);
        }
        return this.future.get(timeout, unit);
    }

    @Override
    public boolean isCancelled() {
        return this.future.isCancelled();
    }

    @Override
    public boolean isDone() {
        return this.future.isDone();
    }

    @Override
    public String toString() {
        String detail;
        try {
            if (this.isDone()) {
                detail = "value = " + this.get();
            } else {
                detail = "still computing";
            }
        } catch (Exception e) {
            detail = "exception = " + e;
        }
        return UIThreadSafeFuture.class.getSimpleName() + "(" + detail
                + ")";
    }
}
