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
        boolean videoShow = Video_IF.getInstance().getVideoShow();
        Log.d(TAG, "onReceive videoShow="+videoShow);
        if (videoShow) {
            AmdMediaButtonReceiver.onMediaButtonReceive(context, intent);
        }
    }
}