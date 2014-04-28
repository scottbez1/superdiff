package com.scottbezek.util;

public class Assert {

    private Assert() {}

    public static void isNull(Object o) {
        if (o != null) {
            throw new AssertionError("Expected null: " + o);
        }
    }

    public static void notNull(Object o) {
        if (o == null) {
            throw new AssertionError("Expected non-null");
        }
    }

    public static void isFalse(boolean val) {
        if (val != false) {
            throw new AssertionError("Expected false");
        }
    }

    public static void isTrue(boolean val) {
        if (val != true) {
            throw new AssertionError("Expected true");
        }
    }

    public static RuntimeException fail(String message) {
        throw new AssertionError(message);
    }
}
