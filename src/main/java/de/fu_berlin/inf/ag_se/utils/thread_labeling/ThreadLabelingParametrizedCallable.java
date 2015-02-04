package de.fu_berlin.inf.ag_se.utils.thread_labeling;

import de.fu_berlin.inf.ag_se.utils.ParametrizedCallable;

public class ThreadLabelingParametrizedCallable<IN, DEST> extends AbstractThreadLabeler implements ParametrizedCallable<IN, DEST> {

    private final ParametrizedCallable<IN, DEST> callable;

    public ThreadLabelingParametrizedCallable(Class clazz, String purpose, ParametrizedCallable<IN, DEST> callable) {
        super(clazz, purpose);
        this.callable = callable;
    }

    @Override
    public DEST call(IN object) throws Exception {
        relabelThread();
        try {
            return callable.call(object);
        } finally {
            restoreLabel();
        }
    }
}
