package com.amd.util;

public class AmdConfig {
    /**
     * app的版本识别ID<p>
     * 1.会与Media_IF合并成一个TAG，方便log打印<p>
     * 2.会与ID3专辑图的暗码合并成Toast输出
     */
    public static final String APP_VERSION_DATE = "0314";
    /**
     * app的时间,会与ID3专辑图的暗码合并成Toast输出
     */
    public static final String APP_VERSION_TIME = "18:30";
    /**
     * 激活ID3专辑图的暗码
     */
    public static final boolean ENABLE_ID3_ALBUM_SECRET_CODE = false;
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
    
    /**
     * ImageLoader的缓存大小, 当前定义为12M。
     */
    public static final int CACHE_SIZE_OF_IMAGELOADER = 12;
    
    /**
     * 是否使用ImageLoader开关。
     */
    public static final boolean IMAGELOADER_OFF = false;
    /**
     * 支持的最大的图片文件，当前定义为32M。
     */
    public static final int MAX_SIZE_OF_IMAGE = 32;
    /**
     * 支持音乐、蓝牙音乐在音乐界面的onkeyup的媒体按键的处理
     */
    public static final boolean ENABLE_MEDIA_KEY_HANDLE_IN_ACTIVITY = true;
    /**
     * 是否有AP来控制收音的按键互斥逻辑处理，默认为true； false表示交由mcu来处理
     */
    public static final boolean ENABLE_RADIO_MUTEX_LOGIC = true;
    /**
     * 本地调试皮肤skinManager
     */
    public static final boolean ENABLE_LOCAl_SKIN_MANAGE = false;
    /**
     * 车速限制
     */
    public static final float CAR_SPEED_LIMIT = 30.0f;
    
}