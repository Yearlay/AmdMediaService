package com.haoke.util;

import com.jsbd.util.LogUtil;

public class DebugLog {

    public static void d(String tags, String str) {
        LogUtil.d(tags, str);
    }
    
    public static void e(String tags, String str) {
        LogUtil.e(tags, str);
    }
    
    public static void v(String tags, String str) {
        LogUtil.v(tags, str);
    }
    
    public static void w(String tags, String str) {
        LogUtil.w(tags, str);
    }
    
    public static void i(String tags, String str) {
        LogUtil.i(tags, str);
    }
}
