package com.amd.media;

import java.io.File;
import java.util.ArrayList;

import com.amd.util.AmdConfig;
import com.haoke.application.MediaApplication;
import com.haoke.bean.FileNode;
import com.haoke.bean.StorageBean;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.data.AllMediaList;
import com.haoke.util.Media_IF;

import android.content.Context;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

public class UsbAutoPlay {
    private static String TAG = "UsbAutoPlay";
    private static long sServiceStartTime = -1;
    private static final int DELAY_AUTO_PLAY = 90;
    private static boolean sDonotDelay = false;
    
    public static void setServiceStartTime() {
        if (sServiceStartTime == -1) {
            sServiceStartTime = SystemClock.elapsedRealtime();
        }
    }
    
    public static void playDefaultMusic(int deviceType) {
        if (!AmdConfig.INSERT_USB_AUTO_PLAY_MUSIC) {
            return;
        }
        if (deviceType != DeviceType.USB1 && deviceType != DeviceType.USB2) {
            return;
        }
        if (!sDonotDelay) {
            setServiceStartTime();
            long time = SystemClock.elapsedRealtime();
            long interval = Math.abs(time - sServiceStartTime) / 1000;
            Log.d(TAG, "playDefaultMusic time="+time+"; interval="+interval+"; sServiceStartTime="+sServiceStartTime);
            if (interval > DELAY_AUTO_PLAY) {
                sDonotDelay = true;
            } else {
                Log.d(TAG, "playDefaultMusic interval="+interval);
                return;
            }
        }
        Log.d(TAG, "playDefaultMusic deviceType="+deviceType);
        Media_IF mIF = Media_IF.getInstance();
        Context context = MediaApplication.getInstance();
        AllMediaList allMediaList = AllMediaList.instance(context);
        if (mIF.isPlayState() && mIF.getPlayingDevice() == deviceType) {
            Log.d(TAG, "playDefaultMusic Media_IF is playing!");
            return;
        }
        String filePath = allMediaList.getLastPlayPath(deviceType, FileType.AUDIO);
        if (!TextUtils.isEmpty(filePath)) {
            File file = new File(filePath);
            if (file.exists() && file.canRead()) {
                Media_IF.setScreenOn();
                Media_IF.getInstance().play(filePath);
                MediaInterfaceUtil.launchMusicPlayActivity(context);
            } else {
                filePath = null;
            }
        } else {
            filePath = null;
        }
        if (filePath == null) {
            ArrayList<FileNode> lists = allMediaList.getMediaList(deviceType, FileType.AUDIO);
            if (lists.size() > 0) {
                Media_IF.setScreenOn();
                Media_IF.getInstance().play(lists.get(0));
                MediaInterfaceUtil.launchMusicPlayActivity(context);
            }
        }
        return;
    }
}