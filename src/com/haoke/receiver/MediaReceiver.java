package com.haoke.receiver;

import com.amd.media.UsbAutoPlay;
import com.haoke.constant.MediaUtil;
import com.haoke.constant.MediaUtil.ScanType;
import com.haoke.service.MediaService;
import com.haoke.util.DebugLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MediaReceiver extends BroadcastReceiver {
    private static final String TAG = "MediaReceiver";
    public static boolean isDynamicFlag = false;
    
    private static String getDataPath(Intent intent) {
        String datapath = intent.getDataString();
        if (datapath != null) {
            datapath = datapath.replace("file://", "");
        }
        if (datapath != null) {
            if (MediaUtil.DEVICE_PATH_FLASH_old.equals(datapath)) {
                datapath = MediaUtil.LOCAL_COPY_DIR; // 转换成 绝对路径下的 media目录。
            }
            if (MediaUtil.DEVICE_PATH_USB_1_old.equals(datapath)) {
                datapath = MediaUtil.DEVICE_PATH_USB_1; // 转换成USB1绝对路径。
            }
            if (MediaUtil.DEVICE_PATH_USB_2_old.equals(datapath)) {
                datapath = MediaUtil.DEVICE_PATH_USB_2; // 转换成USB2绝对路径。
            }
        }
        return datapath;
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        DebugLog.i(TAG, "onReceive isDynamicFlag : " + isDynamicFlag + "; action="+intent.getAction());
        if (!isDynamicFlag) {
            onReceiveEx(context, intent);
        }
    }
    
    public static boolean sUsb1Mounted = true;
    public static boolean sUsb2Mounted = true;
    public static void onReceiveEx(Context context, Intent intent) {
        String action = intent.getAction();
        DebugLog.i(TAG, "onReceiveEx isDynamicFlag : " + isDynamicFlag);
        String devicePath = getDataPath(intent);
        if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
            DebugLog.d(TAG, "MediaReceiver Intent.ACTION_MEDIA_MOUNTED: " + devicePath);
            if (MediaUtil.DEVICE_PATH_USB_1.equals(devicePath)) {
                sUsb1Mounted = true;
            } else if (MediaUtil.DEVICE_PATH_USB_2.equals(devicePath)) {
                sUsb2Mounted = true;
            }
            startFileService(context, ScanType.SCAN_STORAGE, devicePath);
        } else if (action.equals(Intent.ACTION_MEDIA_EJECT) || action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
            DebugLog.d(TAG, "MediaReceiver Eject or Unmounted: " + devicePath);
            if (MediaUtil.DEVICE_PATH_USB_1.equals(devicePath)) {
                UsbAutoPlay.resetUsbAutoPlay(true, false);
                if (!sUsb1Mounted) {
                    return;
                }
                sUsb1Mounted = false;
            } else if (MediaUtil.DEVICE_PATH_USB_2.equals(devicePath)) {
                UsbAutoPlay.resetUsbAutoPlay(false, true);
                if (!sUsb2Mounted) {
                    return;
                }
                sUsb2Mounted = false;
            }
            startFileService(context, ScanType.REMOVE_STORAGE, devicePath);
        }
    }
    
    public static void startFileService(Context context,
            int scanType, String storagePath) {
        if (storagePath != null) {
            Intent intents = new Intent(context, MediaService.class);
            intents.putExtra(MediaService.KEY_COMMAND_FROM, MediaService.VALUE_FROM_SCAN);
            intents.putExtra(ScanType.SCAN_TYPE_KEY, scanType);
            intents.putExtra(ScanType.SCAN_FILE_PATH, storagePath);
            context.startService(intents);
        }
    }
}
