package com.scottbezek.util;

import android.os.Looper;

/**
 * Various Android-specific assertions.
 */
public class AndroidAssert {

    private AndroidAssert() {}

    public static void mainThreadOnly() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new AssertionError("Must be called on the UI thread");
        }
    }
}
