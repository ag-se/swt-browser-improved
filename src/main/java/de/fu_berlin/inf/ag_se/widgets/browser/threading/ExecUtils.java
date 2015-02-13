package de.fu_berlin.inf.ag_se.widgets.browser.threading;

import de.fu_berlin.inf.ag_se.utils.Assert;
import de.fu_berlin.inf.ag_se.widgets.browser.threading.futures.CompletedFuture;
import de.fu_berlin.inf.ag_se.widgets.browser.threading.futures.UIThreadSafeFuture;
import de.fu_berlin.inf.ag_se.widgets.browser.threading.labeling.ThreadLabelingCallable;
import de.fu_berlin.inf.ag_se.widgets.browser.threading.labeling.ThreadLabelingRunnable;
import de.fu_berlin.inf.ag_se.widgets.browser.threading.labeling.ThreadLabelingUtils;
import org.apache.log4j.Logger;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Display;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Offers functionality to run {@link Runnable}s and {@link java.util.concurrent.Callable} synchronously and asynchronously. In contrast to
 * the functionality provided by {@link org.eclipse.swt.widgets.Display} this util class allows also return values. <p> Code can be run in
 * the following way: <table> <tr> <th>&nbsp;</th> <th>UI thread</th> <th>Non-UI thread</th> </tr> <tr> <th>Sync</th> <td>{@link
 * SwtUiThreadExecutor#syncExec(java.util.concurrent.Callable)}<br> {@link SwtUiThreadExecutor#syncExec(Runnable)}</td> <td>{@link
 * #nonUISyncExec(java.util.concurrent.Callable)}<br> {@link #nonUISyncExec(Runnable)}</td> </tr> <tr> <th>Async</th> <td>{@link
 * SwtUiThreadExecutor#asyncExec(java.util.concurrent.Callable)}<br> {@link SwtUiThreadExecutor#asyncExec(Runnable)}</td> <td> {@link
 * #nonUIAsyncExec(java.util.concurrent.Callable)}<br> {@link #nonUIAsyncExec(Runnable)}</td> </tr> </table> <p> Synchronous non-UI thread
 * code is executed immediately. They still return a {@link java.util.concurrent.Future} instead of the value itself. Should the call have
 * been made from an UI thread the code must be executed in a separate thread. <p> Asynchronous non-UI thread naturally always returns a
 * {@link java.util.concurrent.Future}. <p> To allow asynchronous non-UI thread methods to work with a custom {@link
 * java.util.concurrent.ExecutorService}, just instantiate {@link ExecUtils}. This is especially practical if you want to limit the number
 * of maximum threads.
 *
 * @author bkahlert
 */
public class ExecUtils {

    private static final Logger LOGGER = Logger.getLogger(ExecUtils.class);

    public static ThreadFactory createThreadFactory(Class<?> clazz, String purpose) {
        final String prefix = ThreadLabelingUtils.createThreadLabel("", clazz, purpose);
        return new ThreadFactory() {
            private final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();
            private int i = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread t = this.defaultThreadFactory.newThread(r);
                t.setName(prefix + " #" + this.i);
                this.i++;
                return t;
            }
        };
    }

    static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool(createThreadFactory(ExecUtils.class, ""));

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

    /**
     * Runs the given {@link java.util.concurrent.Callable} immediately in a non-UI thread. If the caller already runs in such one the
     * {@link java.util.concurrent.Callable} is simply executed. Otherwise a new thread is started.
     *
     * @param callable
     * @return
     *
     * @UIThread <b>Warning: {@link java.util.concurrent.Future#get()} must not be called from the UI thread</b>
     * @NonUIThread
     */
    public static <V> Future<V> nonUISyncExec(Callable<V> callable) {
        if (ExecUtils.isUIThread()) {
            return new UIThreadSafeFuture<V>(EXECUTOR_SERVICE.submit(callable));
        } else {
            return new CompletedFuture<V>(callable);
        }
    }

    /**
     * Runs the given {@link Runnable} immediately in a non-UI thread. If the caller already runs in such one the {@link Runnable} is
     * simply
     * executed. Otherwise a new thread is started.
     *
     * @param runnable
     * @UIThread <b>Warning: {@link java.util.concurrent.Future#get()} must not be called from the UI thread</b>
     * @NonUIThread
     */
    public static Future<Void> nonUISyncExec(final Runnable runnable) {
        if (ExecUtils.isUIThread()) {
            return new UIThreadSafeFuture<Void>(EXECUTOR_SERVICE.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    runnable.run();
                    return null;
                }
            }));
        } else {
            return new CompletedFuture<Void>(runnable);
        }
    }

    /**
     * Runs the given {@link java.util.concurrent.Callable} immediately in a non-UI thread. If the caller already runs in such one the
     * {@link java.util.concurrent.Callable} is simply executed. Otherwise a new thread is started. <p> The given {@link Class} and purpose
     * are used to give the thread a reasonable name.
     *
     * @param clazz    that invokes this call
     * @param purpose  of the callable
     * @param callable
     * @return
     *
     * @UIThread <b>Warning: {@link java.util.concurrent.Future#get()} must not be called from the UI thread</b>
     * @NonUIThread
     */
    public static <V> Future<V> nonUISyncExec(final Class<?> clazz,
                                              final String purpose, NoCheckedExceptionCallable<V> callable) {
        return nonUISyncExec(new ThreadLabelingCallable<V>(clazz, purpose, callable));
    }

    /**
     * Runs the given {@link Runnable} immediately in a non-UI thread. If the caller already runs in such one the {@link Runnable} is
     * simply
     * executed. Otherwise a new thread is started. <p> The given {@link Class} and purpose are used to give the thread a reasonable name.
     *
     * @param clazz    that invokes this call
     * @param purpose  of the runnable
     * @param runnable
     * @return
     *
     * @UIThread <b>Warning: {@link java.util.concurrent.Future#get()} must not be called from the UI thread</b>
     * @NonUIThread
     */
    public static Future<?> nonUISyncExec(final Class<?> clazz,
                                          final String purpose, Runnable runnable) {
        return nonUISyncExec(new ThreadLabelingRunnable(clazz, purpose, runnable));
    }

    /**
     * Executes the given {@link java.util.concurrent.Callable} synchronously with a delay. <p> The return value is returned in the calling
     * thread.
     *
     * @param callable
     * @param delay
     * @return
     *
     * @UIThread <b>Warning: {@link java.util.concurrent.Future#get()} must not be called from the UI thread</b>
     * @NonUIThread
     */
    public static <V> Future<V> nonUISyncExec(final Callable<V> callable,
                                              final int delay) {
        return nonUIAsyncExec(callable, delay);
    }

    /**
     * Executes the given {@link Runnable} synchronously with a delay. <p> The return value is returned in the calling thread.
     *
     * @param runnable
     * @param delay
     * @return
     *
     * @UIThread <b>Warning: {@link java.util.concurrent.Future#get()} must not be called from the UI thread</b>
     * @NonUIThread
     */
    public static Future<Void> nonUISyncExec(final Runnable runnable,
                                             final int delay) {
        return nonUIAsyncExec(runnable, delay);
    }

    /**
     * Runs the given {@link java.util.concurrent.Callable} with a delay in a non-UI thread. If the caller already runs in such one the
     * {@link java.util.concurrent.Callable} is simply executed. Otherwise a new thread is started. <p> The given {@link Class} and purpose
     * are used to give the thread a reasonable name.
     *
     * @param clazz    that invokes this call
     * @param purpose  of the callable
     * @param callable
     * @param delay
     * @return
     *
     * @UIThread <b>Warning: {@link java.util.concurrent.Future#get()} must not be called from the UI thread</b>
     * @NonUIThread
     */
    public static <V> Future<V> nonUISyncExec(final Class<?> clazz,
                                              final String purpose, NoCheckedExceptionCallable<V> callable, int delay) {
        return nonUISyncExec(new ThreadLabelingCallable<V>(clazz, purpose, callable), delay);
    }

    /**
     * Runs the given {@link Runnable} with a delay in a non-UI thread. If the caller already runs in such one the {@link Runnable} is
     * simply executed. Otherwise a new thread is started. <p> The given {@link Class} and purpose are used to give the thread a reasonable
     * name.
     *
     * @param clazz    that invokes this call
     * @param purpose  of the runnable
     * @param runnable
     * @param delay
     * @return
     *
     * @UIThread <b>Warning: {@link java.util.concurrent.Future#get()} must not be called from the UI thread</b>
     * @NonUIThread
     */
    public static Future<?> nonUISyncExec(final Class<?> clazz,
                                          final String purpose, Runnable runnable, int delay) {
        return nonUISyncExec(new ThreadLabelingRunnable(clazz, purpose, runnable), delay);
    }

    /**
     * Executes the given {@link java.util.concurrent.Callable} asynchronously, meaning always in a new thread.
     *
     * @param callable
     * @return
     *
     * @UIThread <b>Warning: {@link java.util.concurrent.Future#get()} must not be called from the UI thread</b>
     * @NonUIThread
     */
    public static <V> Future<V> nonUIAsyncExec(final Callable<V> callable) {
        Assert.isNotNull(callable);
        return new UIThreadSafeFuture<V>(EXECUTOR_SERVICE.submit(callable));
    }

    /**
     * Executes the given {@link java.util.concurrent.Callable}s asynchronously, meaning always in a new thread.
     *
     * @param callables
     * @return
     *
     * @UIThread <b>Warning: {@link java.util.concurrent.Future#get()} must not be called from the UI thread</b>
     * @NonUIThread
     */
    public static <V> Future<List<V>> nonUIAsyncExec(
            @SuppressWarnings("unchecked") final Callable<V>... callables) {
        Assert.isLegal(callables != null && callables.length != 0);
        return nonUIAsyncExec(new Callable<List<V>>() {
            @Override
            public List<V> call() throws Exception {
                List<V> list = new ArrayList<V>(callables.length);
                List<Future<V>> futures = new ArrayList<Future<V>>(callables.length);
                for (Callable<V> callable : callables) {
                    futures.add(nonUIAsyncExec(callable));
                }
                for (Future<V> future : futures) {
                    list.add(future.get());
                }
                return list;
            }
        });
    }

    /**
     * Executes the given {@link Runnable} asynchronously, meaning always in a new thread. <p> The return value is returned in the calling
     * thread.
     *
     * @param runnable
     * @return
     *
     * @UIThread <b>Warning: {@link java.util.concurrent.Future#get()} must not be called from the UI thread</b>
     * @NonUIThread
     */
    public static Future<Void> nonUIAsyncExec(final Runnable runnable) {
        return new UIThreadSafeFuture<Void>(EXECUTOR_SERVICE.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                runnable.run();
                return null;
            }
        }));
    }

    public static <V> Future<V> nonUIAsyncExec(final Class<?> clazz, final String purpose, final NoCheckedExceptionCallable<V> callable) {
        return new UIThreadSafeFuture<V>(EXECUTOR_SERVICE.submit(new ThreadLabelingCallable<V>(clazz, purpose, callable)));
    }

    public static Future<Void> nonUIAsyncExec(final Class<?> clazz,
                                              final String purpose, final Runnable runnable) {
        return new UIThreadSafeFuture<Void>(
                EXECUTOR_SERVICE.submit(new ThreadLabelingCallable<Void>(clazz, purpose, new NoCheckedExceptionCallable<Void>() {
                    @Override
                    public Void call() {
                        return null;
                    }
                })));
    }

    /**
     * Executes the given {@link java.util.concurrent.Callable} asynchronously, meaning always in a new thread. <p> The return value is
     * returned in the calling thread.
     *
     * @param callable
     * @param delay
     * @return
     *
     * @UIThread <b>Warning: {@link java.util.concurrent.Future#get()} must not be called from the UI thread</b>
     * @NonUIThread
     */
    public static <V> Future<V> nonUIAsyncExec(final Callable<V> callable,
                                               final int delay) {
        return nonUIAsyncExec(new Callable<V>() {
            @Override
            public V call() throws Exception {
                synchronized (this) {
                    this.wait(delay);
                    return callable.call();
                }
            }
        });
    }

    /**
     * Executes the given {@link Runnable} asynchronously, meaning always in a new thread. <p> The return value is returned in the calling
     * thread.
     *
     * @param runnable
     * @param delay
     * @return
     *
     * @UIThread <b>Warning: {@link java.util.concurrent.Future#get()} must not be called from the UI thread</b>
     * @NonUIThread
     */
    public static Future<Void> nonUIAsyncExec(final Runnable runnable,
                                              final int delay) {
        return nonUIAsyncExec(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                synchronized (this) {
                    try {
                        this.wait(delay);
                        runnable.run();
                    } catch (InterruptedException e) {
                    }
                }
                return null;
            }
        });
    }

    public static <V> Future<V> nonUIAsyncExec(final Class<?> clazz,
                                               final String purpose, final NoCheckedExceptionCallable<V> callable, final int delay) {
        return nonUIAsyncExec(new Callable<V>() {
            @Override
            public V call() throws Exception {
                synchronized (this) {
                    this.wait(delay);
                    return new ThreadLabelingCallable<V>(clazz, purpose, callable).call();
                }
            }
        });
    }

    public static Future<Void> nonUIAsyncExec(final Class<?> clazz,
                                              final String purpose, final Runnable runnable, final int delay) {
        return nonUIAsyncExec(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                synchronized (this) {
                    try {
                        this.wait(delay);
                        new ThreadLabelingRunnable(clazz, purpose, runnable).run();
                    } catch (InterruptedException e) {
                    }
                }
                return null;
            }
        });
    }
}
