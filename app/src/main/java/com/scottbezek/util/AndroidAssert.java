package com.scottbezek.util;

import android.os.Looper;

/**
 * Various Android-specific assertions.
 */
public class AndroidAssert {

    private AndroidAssert() {}

    public static void assertSameLooper(Looper expectedLooper) {
        if (Looper.myLooper() != expectedLooper) {
            throw new AssertionError("Not called on required Looper: " + expectedLooper);
        }
    }

    public static void mainThread() {
        assertSameLooper(Looper.getMainLooper());
    }
}
