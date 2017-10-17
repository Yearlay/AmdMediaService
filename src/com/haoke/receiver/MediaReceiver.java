package com.haoke.receiver;

import com.haoke.constant.MediaUtil.ScanType;
import com.haoke.define.MediaDef;
import com.haoke.service.MediaService;
import com.haoke.util.DebugLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MediaReceiver extends BroadcastReceiver {
	
	private String getDataPath(Intent intent) {
        String datapath = intent.getDataString();
        datapath = datapath.replace("file://", "");
        return datapath;
    }
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            DebugLog.i("Yearlay", "MediaReceiver onReceive ACTION_BOOT_COMPLETED");
            startFileService(context, ScanType.SCAN_ALL, null);
		} else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
			DebugLog.d("Yearlay", "MediaReceiver Intent.ACTION_MEDIA_MOUNTED: " + getDataPath(intent));
			startFileService(context, ScanType.SCAN_STORAGE, getDataPath(intent));
		} else if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
			DebugLog.d("Yearlay", "MediaReceiver Intent.ACTION_MEDIA_EJECT: " + getDataPath(intent));
			startFileService(context, ScanType.REMOVE_STORAGE, getDataPath(intent));
		} else if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
            DebugLog.d("Yearlay", "Intent.ACTION_MEDIA_UNMOUNTED");
        } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_STARTED)) {
            DebugLog.d("Yearlay", "Intent.ACTION_MEDIA_SCANNER_STARTED");
        } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
            DebugLog.d("Yearlay", "Intent.ACTION_MEDIA_SCANNER_FINISHED");
        } else if (action.equals(Intent.ACTION_MEDIA_SHARED)) {
            DebugLog.d("Yearlay", "Intent.ACTION_MEDIA_SHARED");
        }
	}
	
	private void startFileService(Context context,
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
