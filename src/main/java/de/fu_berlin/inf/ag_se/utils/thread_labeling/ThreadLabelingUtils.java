package de.fu_berlin.inf.ag_se.utils.thread_labeling;

public class ThreadLabelingUtils {

    private static ThreadLocal<String> threadLabelBackup = new ThreadLocal<String>() {
        @Override
        protected String initialValue() {
            return null;
        }
    };

    public static String backupThreadLabel() {
        String label = Thread.currentThread().getName();
        threadLabelBackup.set(label);
        return label;
    }

    public static void restoreThreadLabel() {
        String label = threadLabelBackup.get();
        if (label != null) {
            Thread.currentThread().setName(label);
        }
    }

    public static String createThreadLabel(Class<?> clazz, String purpose) {
        return createThreadLabel("", clazz, purpose);
    }

    public static String createThreadLabel(String prefix, Class<?> clazz,
                                           String purpose) {
        return prefix + clazz.getSimpleName() + " :: " + purpose;
    }

}
