package com.ss.Misty_Screen_Recoder_lite;

import android.util.Log;

/**
 * Custom logging utility that can be conditionally compiled
 * to remove debug logging from release builds
 */
public class LogUtils {
    
    // Set to false for release builds to remove all debug logging
    private static final boolean DEBUG = BuildConfig.DEBUG;
    
    private static final String TAG_PREFIX = "MistyRecorder";
    
    public static void d(String tag, String message) {
        if (DEBUG) {
            Log.d(TAG_PREFIX + ":" + tag, message);
        }
    }
    
    public static void i(String tag, String message) {
        if (DEBUG) {
            Log.i(TAG_PREFIX + ":" + tag, message);
        }
    }
    
    public static void w(String tag, String message) {
        if (DEBUG) {
            Log.w(TAG_PREFIX + ":" + tag, message);
        }
    }
    
    public static void e(String tag, String message) {
        if (DEBUG) {
            Log.e(TAG_PREFIX + ":" + tag, message);
        }
    }
    
    public static void e(String tag, String message, Throwable throwable) {
        if (DEBUG) {
            Log.e(TAG_PREFIX + ":" + tag, message, throwable);
        }
    }
    
    public static void v(String tag, String message) {
        if (DEBUG) {
            Log.v(TAG_PREFIX + ":" + tag, message);
        }
    }
    
    public static void wtf(String tag, String message) {
        if (DEBUG) {
            Log.wtf(TAG_PREFIX + ":" + tag, message);
        }
    }
    
    public static void wtf(String tag, String message, Throwable throwable) {
        if (DEBUG) {
            Log.wtf(TAG_PREFIX + ":" + tag, message, throwable);
        }
    }
} 