package de.fu_berlin.inf.ag_se.browser.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;

public class DelayedScriptRunner {

    private final LinkedBlockingQueue<FutureTask<?>> queue;
    private final Thread runner;

    public DelayedScriptRunner() {
        this.queue = new LinkedBlockingQueue<FutureTask<?>>();
        this.runner = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        FutureTask<?> task = queue.take();
                        task.run();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }, "DelayedScriptRunner");
    }

    public void start() {
        runner.start();
    }

    public void stop() {
        runner.interrupt();
    }

    public synchronized <V> Future<V> submit(final Callable<V> callable) {
        FutureTask<V> task = new FutureTask<V>(callable);
        if (!queue.add(task)) {
            throw new RuntimeException("Capacity (" + queue.size()
                    + ") of " + this.getClass().getSimpleName() + " exceeded!");
        }
        return task;
    }

    /**
     * Removes all jobs from the queue. The currently job is still finished but the other jobs will never be executed.
     */
    public void flush() {
        queue.clear();
    }
}
