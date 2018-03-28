package com.haoke.util;

import com.amd.util.AmdConfig;
import com.jsbd.util.LogUtil;

public class DebugLog {
    private static final boolean DEBUG = AmdConfig.ENABLE_DEBUG_LOG;

    public static void d(String tags, String str) {
        if (DEBUG) {
            android.util.Log.d(tags, str);
        } else {
            LogUtil.d(tags, str);
        }
    }
    
    public static void e(String tags, String str) {
        if (DEBUG) {
            android.util.Log.e(tags, str);
        } else {
            LogUtil.e(tags, str);
        }
    }
    
    public static void v(String tags, String str) {
        if (DEBUG) {
            android.util.Log.v(tags, str);
        } else {
            LogUtil.v(tags, str);
        }
    }
    
    public static void w(String tags, String str) {
        if (DEBUG) {
            android.util.Log.w(tags, str);
        } else {
            LogUtil.w(tags, str);
        }
    }
    
    public static void i(String tags, String str) {
        if (DEBUG) {
            android.util.Log.i(tags, str);
        } else {
            LogUtil.i(tags, str);
        }
    }
    
    public static void a(String className, String methodName, String discription) {
        if (DEBUG) {
            android.util.Log.i(className, methodName + ": " + discription);
        } else {
            LogUtil.i(className, methodName + ": " + discription);
        }
    }
}
