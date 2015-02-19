package de.fu_berlin.inf.ag_se.browser.threading.labeling;

public abstract class AbstractThreadLabeler {

    private final Class clazz;
    private final String purpose;
    private String oldName;

    protected AbstractThreadLabeler(Class clazz, String purpose) {
        this.clazz = clazz;
        this.purpose = purpose;
    }

    protected String relabelThread() {
        oldName = Thread.currentThread().getName();
        String newName = createThreadLabel(oldName + " :: ");
        Thread.currentThread().setName(newName);
        return oldName;
    }

    protected void restoreLabel() {
        Thread.currentThread().setName(oldName);
    }

    private String createThreadLabel(String prefix) {
        return ThreadLabelingUtils.createThreadLabel(prefix, clazz, purpose);
    }
}
