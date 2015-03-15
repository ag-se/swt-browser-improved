package de.fu_berlin.inf.ag_se.browser.threading;

import de.fu_berlin.inf.ag_se.browser.threading.labeling.ThreadLabelingCallable;
import de.fu_berlin.inf.ag_se.browser.threading.labeling.ThreadLabelingRunnable;
import de.fu_berlin.inf.ag_se.browser.threading.labeling.ThreadLabelingUtils;
import org.apache.log4j.Logger;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


public class UIThreadAwareExecutor extends ThreadPoolExecutor {

    private static final Logger LOGGER = Logger.getLogger(UIThreadAwareExecutor.class);

    private final UIThreadExecutor uiThreadExecutor;

    public UIThreadAwareExecutor(UIThreadExecutor uiThreadExecutor) {
        super(0, 5, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), createThreadFactory(UIThreadAwareExecutor.class, ""));
        this.uiThreadExecutor = uiThreadExecutor;
    }

    static class ThreadSafeUITask<V> implements RunnableFuture<V> {

        private final RunnableFuture<V> task;
        private final UIThreadExecutor uiThreadExecutor;

        public ThreadSafeUITask(RunnableFuture<V> task, UIThreadExecutor uiThreadExecutor) {
            this.task = task;
            this.uiThreadExecutor = uiThreadExecutor;
        }

        @Override
        public void run() {
            task.run();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return task.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return task.isCancelled();
        }

        @Override
        public boolean isDone() {
            return task.isDone();
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            assertNoUIThread();
            return task.get();
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            assertNoUIThread();
            return task.get(timeout, unit);
        }

        private void assertNoUIThread() {
            if (uiThreadExecutor.isUIThread() && !this.isDone()) {
                throw new RuntimeException(
                        "Waiting is not allowed from the UI thread. Should the calculation include UI thread code, this could lead to a deadlock.\nWait in another thread or check isDone() before calling get(...).",
                        null);
            }
        }
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new ThreadSafeUITask<T>(super.newTaskFor(runnable, value), uiThreadExecutor);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new ThreadSafeUITask<T>(super.newTaskFor(callable), uiThreadExecutor);
    }

    private static ThreadFactory createThreadFactory(Class<?> clazz, String purpose) {
        final String prefix = ThreadLabelingUtils.createThreadLabel("", clazz, purpose);
        return new ThreadFactory() {
            private final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();
            private AtomicInteger i = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = defaultThreadFactory.newThread(r);
                t.setName(prefix + " #" + this.i);
                i.getAndIncrement();
                return t;
            }
        };
    }

    public Future<?> nonUIAsyncExec(final Class<?> clazz,
                                        final String purpose, final Runnable runnable) {
        return submit(new ThreadLabelingRunnable(clazz, purpose, runnable), 0);
    }

    public <V> Future<V> nonUIAsyncExec(final Class<?> clazz, final String purpose, final NoCheckedExceptionCallable<V> callable) {
        return submit(new ThreadLabelingCallable<V>(clazz, purpose, callable));
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
       public <V> Future<V> asyncUIExec(final NoCheckedExceptionCallable<V> callable) {
           return submit(new NoCheckedExceptionCallable<V>() {
               @Override
               public V call() {
                   return uiThreadExecutor.syncExec(callable);
               }
           });
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
       public Future<?> asyncUIExec(final Runnable runnable) {
           return submit(new Runnable() {
               @Override
               public void run() {
                   uiThreadExecutor.syncExec(runnable);
               }
           });
       }
}
