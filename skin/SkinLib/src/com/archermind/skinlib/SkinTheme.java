package com.archermind.skinlib;

import java.util.HashMap;

public class SkinTheme {
    public static final String SKIN_KEY_NAME = "bd_theme_color";
    
    public static final int SKIN_DEFAULT = 0;
    public static final int SKIN_ONE = 1;    // 红色主题。
    
    private static HashMap<String, String> sAppTagHashMap = new HashMap<String, String>();
    static {
        sAppTagHashMap.put("com.haoke.mediaservice", "mediaservice");
    }
    
    public static String getAppTag(String packageName) {
        return sAppTagHashMap.get(packageName);
    }
}
