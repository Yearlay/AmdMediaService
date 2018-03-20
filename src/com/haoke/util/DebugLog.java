package com.haoke.util;

import com.haoke.constant.DebugConstant;

import android.util.Log;

public class DebugLog {

    public static void d(String tags, String str) {
        if (!DebugConstant.DEBUG) return;
        DebugLog.d(tags, str);
    }
    
    public static void e(String tags, String str) {
        DebugLog.e(tags, str);
    }
    
    public static void v(String tags, String str) {
        if (!DebugConstant.DEBUG) return;
        Log.v(tags, str);
    }
    
    public static void w(String tags, String str) {
        if (!DebugConstant.DEBUG) return;
        Log.w(tags, str);
    }
    
    public static void i(String tags, String str) {
        if (!DebugConstant.DEBUG) return;
        Log.i(tags, str);
    }
}
