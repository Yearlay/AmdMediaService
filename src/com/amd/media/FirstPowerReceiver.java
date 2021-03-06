package com.amd.media;

import com.amd.radio.Radio_IF;

import static com.haoke.constant.MediaUtil.RepeatMode.CIRCLE;

import com.haoke.util.DebugLog;
import com.haoke.util.Media_IF;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class FirstPowerReceiver extends BroadcastReceiver{
	private static final String TAG = "FirstPowerReceiver";
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		DebugLog.d(TAG, "onReceive action="+action);
		if ("com.haoke.action.firstpower".equals(action)) {
			clearAppDataFromBoot(context);
		}
	}
	
	private static boolean clearedFlag = false;
	/**
	 * 断B+起来，carmanager会发送该广播，此时应用需要清除一些记忆数据。
	 */
	public static void clearAppDataFromBoot(Context context) {
		if (!clearedFlag) {
			clearedFlag = true;
			//Radio_IF.getInstance().clearColloctFreq(context);
			Media_IF.getInstance().setRepeatMode(CIRCLE);
			Media_IF.resetSource(context);
		}
	}
}
