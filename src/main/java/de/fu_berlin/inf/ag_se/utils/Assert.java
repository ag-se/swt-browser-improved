package de.fu_berlin.inf.ag_se.utils;

public class Assert {

    public static void isLegal(boolean b) {
        if (!b) {
            throw new IllegalArgumentException();
        }
    }

    public static void isNotNull(Object o) {
        if (o == null) {
            throw new IllegalStateException("Assertion failed: null argument");
        }
    }

    public static void isTrue(boolean b) {
        if (!b) {
            throw new IllegalStateException("Assertion failed: argument is false");
        }
    }
}
