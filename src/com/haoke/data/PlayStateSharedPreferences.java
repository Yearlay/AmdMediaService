
package com.haoke.data;

import com.haoke.bean.FileNode;
import com.haoke.constant.DBConfig;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.define.MediaDef.RepeatMode;
import com.haoke.ui.image.Image_Activity_Main;

import android.content.Context;
import android.content.SharedPreferences;

public class PlayStateSharedPreferences {
    public static final String SPLIT_STR = "##";
    private static final String LOGINFO_FILE_NAME = "play_state";
    private static final String PLAY_MODE_KEY = "play_mode";
    private static final String LAST_DEVICE_TYPR = "last_device_type";
    private static final String LAST_DEVICE_TYPE_VIDEO = "last_device_type_video";
    private static final String LAST_MUSIC_PLAY_STATE = "last_music_play_state";
    private static final String LAST_VIDEO_PLAY_STATE = "last_video_play_state";
    
    static PlayStateSharedPreferences mSharedPreferences;

    public Context mContext;

    public synchronized static PlayStateSharedPreferences instance(Context context) {
        if (mSharedPreferences == null) {
            mSharedPreferences = new PlayStateSharedPreferences(context);
        }
        return mSharedPreferences;
    }
    
    public PlayStateSharedPreferences(Context context) {
        mContext = context.getApplicationContext();
    }
    
    private SharedPreferences getPreferences() {
        return mContext.getSharedPreferences(LOGINFO_FILE_NAME, Context.MODE_PRIVATE);
    }

    public void savePlayTime(FileNode fileNode) {
        int deviceType = fileNode.getDeviceType();
        int fileType = fileNode.getFileType();
        String key = DBConfig.getTableName(deviceType, fileType);
        getPreferences().edit().putString(key, fileNode.getFilePath() + SPLIT_STR + fileNode.getPlayTime()).commit();
        if (fileType == FileType.AUDIO) {
            getPreferences().edit().putInt(LAST_DEVICE_TYPR, deviceType).commit();
        } else if (fileType == FileType.VIDEO) {
        	getPreferences().edit().putInt(LAST_DEVICE_TYPE_VIDEO, deviceType).commit();
        }
    }
    
    public void clearPlayTime(int deviceType, int fileType) {
        getPreferences().edit().remove(DBConfig.getTableName(deviceType, fileType)).commit();
    }
    
    public int getLastDeviceType() {
        return getPreferences().getInt(LAST_DEVICE_TYPR, DeviceType.NULL);
    }
    
    public int getLastDeviceTypeVideo() {
        return getPreferences().getInt(LAST_DEVICE_TYPE_VIDEO, DeviceType.NULL);
    }

    public String getPlayTime(int deviceType, int fileType) {
        String key = DBConfig.getTableName(deviceType, fileType);
        String valueStr = getPreferences().getString(key, "");
        return valueStr;
    }
    
    public void savePlayState(int fileType, boolean playing) {
        if (fileType == FileType.AUDIO) {
            getPreferences().edit().putBoolean(LAST_MUSIC_PLAY_STATE, playing).apply();
        } else if (fileType == FileType.VIDEO) {
            getPreferences().edit().putBoolean(LAST_VIDEO_PLAY_STATE, playing).apply();
        }
    }
    
    public boolean getPlayState(int fileType) {
        boolean playing = false;
        if (fileType == FileType.AUDIO) {
            playing = getPreferences().getBoolean(LAST_MUSIC_PLAY_STATE, false);
        } else if (fileType == FileType.VIDEO) {
            playing = getPreferences().getBoolean(LAST_VIDEO_PLAY_STATE, false);
        }
        return playing;
    }
    
    public void savePlayMode(int playMode) {
        getPreferences().edit().putInt(PLAY_MODE_KEY, playMode).commit();
    }
    
    public int getPlayMode() {
        return getPreferences().getInt(PLAY_MODE_KEY, RepeatMode.CIRCLE);
    }
    
    class ImageInfo {
        public static final String DEVICETYPE = "picture_devicetype";
        public static final String SHOWFRAGMENT = "picture_fragment";
        public static final String CURRENTPOSITION = "picture_position";
    }
    public void saveImageDeviceType(int deviceType) {
        getPreferences().edit().putInt(ImageInfo.DEVICETYPE, deviceType).commit();
    }
    public int getImageDeviceType() {
        return getPreferences().getInt(ImageInfo.DEVICETYPE, DeviceType.FLASH);
    }
    public void saveImageShowFragment(int fragmentId) {
        getPreferences().edit().putInt(ImageInfo.SHOWFRAGMENT, fragmentId).commit();
    }
    public int getImageShowFragment() {
        return getPreferences().getInt(ImageInfo.SHOWFRAGMENT, 0);
    }
    public void saveImageCurrentPosition(int currentPosition) {
        getPreferences().edit().putInt(ImageInfo.CURRENTPOSITION, currentPosition).commit();
    }
    public int getImageCurrentPosition() {
        return getPreferences().getInt(ImageInfo.CURRENTPOSITION, 0);
    }
    
    class VideoInfo {
    	public static final String DEVICETYPE = "video_devicetype";
        public static final String SHOWFRAGMENT = "video_fragment";
        public static final String CURRENTPOSITION = "video_position";
    }
    public void saveVideoDeviceType(int deviceType) {
        getPreferences().edit().putInt(VideoInfo.DEVICETYPE, deviceType).commit();
    }
    public int getVideoDeviceType() {
        return getPreferences().getInt(VideoInfo.DEVICETYPE, DeviceType.FLASH);
    }
    public void saveVideoShowFragment(int fragmentId) {
        getPreferences().edit().putInt(VideoInfo.SHOWFRAGMENT, fragmentId).commit();
    }
    public int getVideoShowFragment() {
        return getPreferences().getInt(VideoInfo.SHOWFRAGMENT, 0);
    }
    public void saveVideoCurrentPosition(int currentPosition) {
        getPreferences().edit().putInt(VideoInfo.CURRENTPOSITION, currentPosition).commit();
    }
    public int getVideoCurrentPosition() {
        return getPreferences().getInt(VideoInfo.CURRENTPOSITION, 0);
    }
    
    public static final String MODE_SWITCH_KEY = "mode_switch";
    public void saveSwitchMode(int currentMode) {
        getPreferences().edit().putInt(MODE_SWITCH_KEY, currentMode).commit();
    }
    public int getSwitchMode() {
        return getPreferences().getInt(MODE_SWITCH_KEY, 0);
    }
    
    public static final String MODE_MARK_KEY = "mode_mark";
    public void saveSwitchMode(boolean showMark) {
        getPreferences().edit().putBoolean(MODE_MARK_KEY, showMark).commit();
    }
    public boolean getModeMark() {
        return getPreferences().getBoolean(MODE_MARK_KEY, false);
    }
}
