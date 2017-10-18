package com.haoke.ui.video;

import com.amd.media.AmdMediaButtonReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class VideoMediaButtonReceiver extends BroadcastReceiver {
    private static final String TAG = "BTMediaButtonReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
    	Log.d(TAG, "onReceive");
    	AmdMediaButtonReceiver.onMediaButtonReceive(context, intent);
    }
}