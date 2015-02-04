package de.fu_berlin.inf.ag_se.utils.thread_labeling;

public class ThreadLabelingUtils {

    public static String createThreadLabel(Class<?> clazz, String purpose) {
        return createThreadLabel("", clazz, purpose);
    }

    public static String createThreadLabel(String prefix, Class<?> clazz,
                                           String purpose) {
        return prefix + clazz.getSimpleName() + " :: " + purpose;
    }

}
