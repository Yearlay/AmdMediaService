package com.amd.util;

public class AmdConfig {
    /**
     * 插入U盘，自动播放歌曲，与 INSERT_USB_RECODRD_PLAY_MUSIC 冲突，不能同时为true
     */
    public static boolean INSERT_USB_AUTO_PLAY_MUSIC = true;
    
    /**
     * 播放U盘歌曲，拨出U盘后，再插入，继续歌曲播放，与 INSERT_USB_AUTO_PLAY_MUSIC 冲突，不能同时为true
     */
    public static boolean INSERT_USB_RECODRD_PLAY_MUSIC = false;
    
    public static boolean SCAN_OVER_LAUNCHER_PARSE_AUDIO_ID3_INFO = false;
}