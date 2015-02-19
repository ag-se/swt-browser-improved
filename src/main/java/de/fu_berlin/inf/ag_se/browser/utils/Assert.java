package de.fu_berlin.inf.ag_se.browser.utils;

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
}
