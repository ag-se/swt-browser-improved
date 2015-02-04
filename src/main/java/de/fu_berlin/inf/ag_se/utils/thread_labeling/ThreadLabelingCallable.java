package de.fu_berlin.inf.ag_se.utils.thread_labeling;

import java.util.concurrent.Callable;

public class ThreadLabelingCallable<DEST> extends AbstractThreadLabeler implements Callable<DEST> {

    private final Callable<DEST> callable;

    public ThreadLabelingCallable(Class clazz, String purpose, Callable<DEST> callable) {
        super(clazz, purpose);
        this.callable = callable;
    }

    @Override
    public DEST call() throws Exception {
        relabelThread();
        try {
            return callable.call();
        } finally {
            restoreLabel();
        }
    }
}
