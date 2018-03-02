package com.amd.radio;

import com.amd.media.AmdMediaButtonReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RadioMediaButtonReceiver extends BroadcastReceiver {
    private static final String TAG = "RadioButtonReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("ajax", "onReceive RadioMediaButtonReceiver");
        AmdMediaButtonReceiver.onMediaButtonReceive(context, intent);
    }
}