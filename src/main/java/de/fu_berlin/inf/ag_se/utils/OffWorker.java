package de.fu_berlin.inf.ag_se.utils;

import de.fu_berlin.inf.ag_se.utils.thread_labeling.ThreadLabelingCallable;
import de.fu_berlin.inf.ag_se.utils.thread_labeling.ThreadLabelingUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;

public class OffWorker {

    public static class StateException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public StateException(State oldState, State newState) {
            super("Cannot change from state " + oldState + " to " + newState);
        }
    }

    private static enum State {
        INIT, RUNNING, SHUTDOWN
    }

    private final LinkedBlockingQueue<FutureTask<?>> queue;
    private final Thread runner;
    private State state = State.INIT;

    public OffWorker(Class<?> owner, String purpose) {
        this.queue = new LinkedBlockingQueue<FutureTask<?>>();
        this.runner = new Thread(new Runnable() {
            @Override
            public void run() {
                loop:
                while (true) {
                    boolean isInterrupted = Thread.interrupted();
                    if (!isInterrupted) {
                        try {
                            FutureTask<?> task = queue.take();
                            task.run();
                        } catch (InterruptedException e) {
                            isInterrupted = true;
                        }
                    }
                    if (isInterrupted) {
                        switch (OffWorker.this.state) {
                            case INIT:
                                throw new RuntimeException("Implementation Error");
                            case RUNNING:
                                break;
                            case SHUTDOWN:
                                break loop;
                        }
                    }
                }
            }
        }, owner.getSimpleName() + " :: " + purpose + " :: "
                + OffWorker.class.getSimpleName());
    }

    public void start() {
        switch (state) {
            case INIT:
                state = State.RUNNING;
                runner.start();
                break;
            case RUNNING:
                throw new StateException(State.RUNNING, State.RUNNING);
            case SHUTDOWN:
                throw new StateException(State.SHUTDOWN, State.RUNNING);
        }
    }

    public void finish() {
        this.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                shutdown();
                return null;
            }
        });
    }

    public void shutdown() {
        switch (state) {
            case INIT:
                state = State.SHUTDOWN;
                break;
            case RUNNING:
                state = State.SHUTDOWN;
                runner.interrupt();
                break;
            case SHUTDOWN:
                state = State.SHUTDOWN;
                runner.interrupt();
        }
    }

    public boolean isShutdown() {
        return this.state == State.SHUTDOWN;
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
