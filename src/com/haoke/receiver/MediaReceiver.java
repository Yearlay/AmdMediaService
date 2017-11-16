package com.haoke.receiver;

import com.haoke.constant.MediaUtil.ScanType;
import com.haoke.service.MediaService;
import com.haoke.util.DebugLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MediaReceiver extends BroadcastReceiver {
    public static boolean isDynamicFlag = false;
    
    private static String getDataPath(Intent intent) {
        String datapath = intent.getDataString();
        datapath = datapath.replace("file://", "");
        return datapath;
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!isDynamicFlag) {
            onReceiveEx(context, intent);
        }
    }
    
    public static void onReceiveEx(Context context, Intent intent) {
        DebugLog.i("Yearlay", "onReceiveEx isDynamicFlag : " + isDynamicFlag);
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
            DebugLog.d("Yearlay", "MediaReceiver Intent.ACTION_MEDIA_MOUNTED: " + getDataPath(intent));
            startFileService(context, ScanType.SCAN_STORAGE, getDataPath(intent));
        } else if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
            DebugLog.d("Yearlay", "MediaReceiver Intent.ACTION_MEDIA_EJECT: " + getDataPath(intent));
            startFileService(context, ScanType.REMOVE_STORAGE, getDataPath(intent));
        }
    }
    
    private static void startFileService(Context context,
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
