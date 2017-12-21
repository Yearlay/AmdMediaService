package com.amd.util;

public class AmdConfig {
    /**
     * app的版本识别ID<p>
     * 1.会与Media_IF合并成一个TAG，方便log打印<p>
     * 2.会与ID3专辑图的暗码合并成Toast输出
     */
    public static final String APP_VERSION_DATE = "1220";
    /**
     * app的时间,会与ID3专辑图的暗码合并成Toast输出
     */
    public static final String APP_VERSION_TIME = "18:00";
    /**
     * 激活ID3专辑图的暗码
     */
    public static final boolean ENABLE_ID3_ALBUM_SECRET_CODE = true;
    /**
     * 插入U盘，自动播放歌曲，与 {@link INSERT_USB_RECODRD_PLAY_MUSIC} 冲突，不能同时为true
     */
    public static final boolean INSERT_USB_AUTO_PLAY_MUSIC = true;
    
    /**
     * 播放U盘歌曲，拨出U盘后，再插入，继续歌曲播放，与 {@link INSERT_USB_AUTO_PLAY_MUSIC} 冲突，不能同时为true
     */
    public static final boolean INSERT_USB_RECODRD_PLAY_MUSIC = false;
    
    /**
     * 当扫描完成后，启动音频的id3解析
     */
    public static final boolean SCAN_OVER_LAUNCHER_PARSE_AUDIO_ID3_INFO = true;
}