package de.fu_berlin.inf.ag_se.utils.thread_labeling;

public class ThreadLabelingRunnable extends  AbstractThreadLabeler implements Runnable {

    private final Runnable runnable;

    public ThreadLabelingRunnable(Class clazz, String purpose, Runnable runnable) {
        super(clazz, purpose);
        this.runnable = runnable;
    }

    @Override
    public void run() {
        relabelThread();
        try {
            runnable.run();
        } finally {
            restoreLabel();
        }
    }
}
