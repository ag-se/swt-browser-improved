package de.fu_berlin.inf.ag_se.browser.threading.labeling;

import de.fu_berlin.inf.ag_se.browser.threading.NoCheckedExceptionCallable;

public class ThreadLabelingCallable<DEST> extends AbstractThreadLabeler implements NoCheckedExceptionCallable<DEST> {

    private final NoCheckedExceptionCallable<DEST> callable;

    public ThreadLabelingCallable(Class clazz, String purpose, NoCheckedExceptionCallable<DEST> callable) {
        super(clazz, purpose);
        this.callable = callable;
    }

    @Override
    public DEST call() {
        relabelThread();
        try {
            return callable.call();
        } finally {
            restoreLabel();
        }
    }
}
