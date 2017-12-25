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
    private static boolean sDonotDelay = true;
    private static boolean isBootInsertUsb1 = false;
    private static boolean isBootInsertUsb2 = false;
    
    public static void setServiceStartTime() {
        if (sServiceStartTime == -1) {
            sServiceStartTime = SystemClock.elapsedRealtime();
            isBootInsertUsb1 = MediaInterfaceUtil.isUsbOn(DeviceType.USB1);
            isBootInsertUsb2 = MediaInterfaceUtil.isUsbOn(DeviceType.USB2);
        }
    }
    
    public static void playDefaultMusic(int deviceType) {
        if (!AmdConfig.INSERT_USB_AUTO_PLAY_MUSIC) {
            return;
        }
        if (deviceType != DeviceType.USB1 && deviceType != DeviceType.USB2) {
            return;
        }
        if (!Media_IF.isBootSourceChanged()) {
            Log.d(TAG, "playDefaultMusic return! BootSource not changed!");
            return;
        }
        if (!Media_IF.isPowerOn() && !Media_IF.getScreenOn()) {
            Log.d(TAG, "playDefaultMusic return! PowerOff and ScreenOff!");
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
        Log.d(TAG, "playDefaultMusic deviceType="+deviceType+"; isBootInsertUsb1="+isBootInsertUsb1+"; isBootInsertUsb2="+isBootInsertUsb2);
        if (isBootInsertUsb1 && deviceType == DeviceType.USB1) {
            isBootInsertUsb1 = false;
            return;
        } else if (isBootInsertUsb2 && deviceType == DeviceType.USB2) {
            isBootInsertUsb2 = false;
            return;
        }
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
                if (Media_IF.getInstance().play(filePath)) {
                    Media_IF.setScreenOn();
                    if (!Media_IF.isCarReversing()) {
                        MediaInterfaceUtil.launchMusicPlayActivity(context);
                    }
                } else {
                    filePath = null;
                }
            } else {
                filePath = null;
            }
        } else {
            filePath = null;
        }
        if (filePath == null) {
            ArrayList<FileNode> lists = allMediaList.getMediaList(deviceType, FileType.AUDIO);
            if (lists.size() > 0) {
                if (Media_IF.getInstance().play(lists.get(0))) {
                    Media_IF.setScreenOn();
                    if (!Media_IF.isCarReversing()) {
                        MediaInterfaceUtil.launchMusicPlayActivity(context);
                    }
                }
            }
        }
        return;
    }
}