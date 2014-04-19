package com.scottbezek.util;

import android.os.SystemClock;
import android.util.Log;


public class StopWatch {

    private static final String TAG = StopWatch.class.getName();

    private final String mMessage;
    private final long mStartElapsedRealtimeMs;

    public static StopWatch start(String message) {
        return new StopWatch(message);
    }

    private StopWatch(String message) {
        mMessage = message;
        mStartElapsedRealtimeMs = SystemClock.elapsedRealtime();
    }

    public void stopAndLog() {
        long elapsed = SystemClock.elapsedRealtime() - mStartElapsedRealtimeMs;
        Log.d(TAG, "Time: " + mMessage + " -- " + elapsed + "ms");
    }

}
