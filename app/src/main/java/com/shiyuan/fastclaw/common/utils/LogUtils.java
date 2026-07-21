package com.shiyuan.fastclaw.common.utils;

import android.util.Log;

import com.shiyuan.fastclaw.BuildConfig;

public class LogUtils {

    private static final String TAG = "DebugModel";
    private static final boolean isDebug = BuildConfig.DEBUG;

    public static void i(String str) {
        if (!isDebug) {
            return;
        }

        Log.i(TAG, str);
    }

    public static void d(String str) {
        if (!isDebug) {
            return;
        }
        Log.d(TAG, str);
    }

    public static void v(String str) {
        if (!isDebug) {
            return;
        }

        Log.v(TAG, str + getSuffix());
    }

    public static void e(String str) {
        if (!isDebug) {
            return;
        }

        Log.e(TAG, str + getSuffix());
    }

    public static void e(Exception e) {
        if (!isDebug) {
            return;
        }

        Log.e(TAG, "Error info will print at next line on tag v:" + getSuffix());
        e.printStackTrace();
    }

    public static String getSuffix() {
        StackTraceElement[] ss = Thread.currentThread().getStackTrace();

        String className = ss[4].getClassName();

        if (className.isEmpty()) {
            return "";
        }

        className = className.substring(className.lastIndexOf(".") + 1);

        return " - [" + className + ".java:" + ss[4].getLineNumber() + "]";
    }
}