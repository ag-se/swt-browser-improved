package de.fu_berlin.inf.ag_se.utils;

import java.util.concurrent.Callable;

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

    public static void setThreadLabel(Class<?> clazz, String purpose) {
        Thread.currentThread().setName(createThreadLabel(clazz, purpose));
    }

    public static void setThreadLabel(String prefix, Class<?> clazz,
                                      String purpose) {
        Thread.currentThread().setName(
                createThreadLabel(prefix, clazz, purpose));
    }

    public static <I, O> ExecUtils.ParametrizedCallable<I, O> createThreadLabelingCode(
            final ExecUtils.ParametrizedCallable<I, O> parametrizedCallable,
            final Class<?> clazz, final String purpose) {
        return new ExecUtils.ParametrizedCallable<I, O>() {
            @Override
            public O call(I i) throws Exception {
                String oldName = Thread.currentThread().getName();
                setThreadLabel(oldName + " :: ", clazz, purpose);
                try {
                    return parametrizedCallable.call(i);
                } finally {
                    Thread.currentThread().setName(oldName);
                }
            }
        };
    }

    public static <V> Callable<V> createThreadLabelingCode(
            final Callable<V> callable, final Class<?> clazz,
            final String purpose) {
        return new Callable<V>() {
            @Override
            public V call() throws Exception {
                String oldName = Thread.currentThread().getName();
                setThreadLabel(oldName + " :: ", clazz, purpose);
                try {
                    return callable.call();
                } finally {
                    Thread.currentThread().setName(oldName);
                }
            }
        };
    }

    public static Runnable createThreadLabelingCode(final Runnable runnable,
                                                    final Class<?> clazz, final String purpose) {
        return new Runnable() {
            @Override
            public void run() {
                String oldName = Thread.currentThread().getName();
                setThreadLabel(oldName + " :: ", clazz, purpose);
                try {
                    runnable.run();
                } finally {
                    Thread.currentThread().setName(oldName);
                }
            }
        };
    }
}
