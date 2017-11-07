package com.haoke.receiver;

import com.haoke.constant.VRConstant.VRApp;
import com.haoke.constant.VRConstant.VRImage;
import com.haoke.constant.VRConstant.VRMusic;
import com.haoke.constant.VRConstant.VRRadio;
import com.haoke.constant.VRConstant.VRVideo;
import com.haoke.service.MediaService;
import com.haoke.util.DebugLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class VROperatorReceiver extends BroadcastReceiver {
	
	private final String TAG = "VROperatorReceiver";
	
	@Override
	public void onReceive(Context arg0, Intent intent) {
		String action = intent.getAction();
		DebugLog.d(TAG, "VROperatorReceiver onReceive intent=" + action);
		
		if (VRApp.ACTION_APP.equals(action)) {
			startServiceForVRApp(arg0, intent.getIntExtra(VRApp.KEY_FUCTION, 0),
					intent.getIntExtra(VRApp.KEY_OPERATOR, -1));
		} else if (VRMusic.ACTION_MUSIC_OPERATOR.equals(action)) {
			startServiceForVRMusic(arg0, intent.getIntExtra(VRMusic.KEY_COMMAND_CODE, 0),
					intent.getStringExtra(VRMusic.KEY_MUSIC_PATH));
		} else if (VRRadio.ACTION_RADIO_OPERATOR.equals(action)) {
			startServiceForVRRadio(arg0, intent.getIntExtra(VRRadio.KEY_COMMAND_CODE, 0), intent.getStringExtra(VRRadio.KEY_STATION_FREQ));
		} else if (VRImage.ACTION_IMAGE_OPERATOR.equals(action)) {
			startServiceForVRImage(arg0, intent.getIntExtra(VRImage.KEY_COMMAND_CODE, 0));
		} else if (VRVideo.ACTION_VIDEO_OPERATOR.equals(action)) {
			startServiceForVRVideo(arg0, intent.getIntExtra(VRVideo.KEY_COMMAND_CODE, 0));
		}
	}
	
	private void startServiceForVRApp(Context context, int function, int value) {
		DebugLog.d(TAG, " startServiceForVRApp function: " + function + " && value: " + value);
		if (function != 0 && value != -1 ) {
			Intent intents = new Intent(context, MediaService.class);
			intents.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intents.putExtra(MediaService.KEY_COMMAND_FROM, MediaService.VALUE_FROM_VR_APP);
			intents.putExtra(VRApp.KEY_FUCTION, function);
			intents.putExtra(VRApp.KEY_OPERATOR, value);
			context.startService(intents);
		}
    }
	
	private void startServiceForVRMusic(Context context, int commandCode, String filePath) {
		DebugLog.d(TAG, " startServiceForVRMusic commandCode: " + commandCode + " && filePath: " + filePath);
		if (commandCode != 0) {
			Intent intents = new Intent(context, MediaService.class);
			intents.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intents.putExtra(MediaService.KEY_COMMAND_FROM, MediaService.VALUE_FROM_VR_MUSIC);
			intents.putExtra(VRMusic.KEY_COMMAND_CODE, commandCode);
			intents.putExtra(VRMusic.KEY_MUSIC_PATH, filePath);
			context.startService(intents);
		}
	}
	
	public void startServiceForVRRadio(Context context, int commandCode, String station) {
		DebugLog.d(TAG, " startServiceForVRRadio commandCode: " + commandCode + "; station: " + station);
		if (commandCode != 0) {
			Intent intents = new Intent(context, MediaService.class);
			intents.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intents.putExtra(MediaService.KEY_COMMAND_FROM, MediaService.VALUE_FROM_VR_RADIO);
			intents.putExtra(VRRadio.KEY_COMMAND_CODE, commandCode);
			intents.putExtra(VRRadio.KEY_STATION_FREQ, station);
			context.startService(intents);
		}
	}
	
	public void startServiceForVRImage(Context context, int commandCode) {
		DebugLog.d(TAG, " startServiceForVRImage commandCode: " + commandCode);
		if (commandCode != 0) {
			Intent intents = new Intent(context, MediaService.class);
			intents.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intents.putExtra(MediaService.KEY_COMMAND_FROM, MediaService.VALUE_FROM_VR_IMAGE);
			intents.putExtra(VRImage.KEY_COMMAND_CODE, commandCode);
			context.startService(intents);
		}
	}
	
	public void startServiceForVRVideo(Context context, int commandCode) {
		DebugLog.d(TAG, " startServiceForVRVideo commandCode: " + commandCode);
		if (commandCode != 0) {
			Intent intents = new Intent(context, MediaService.class);
			intents.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intents.putExtra(MediaService.KEY_COMMAND_FROM, MediaService.VALUE_FROM_VR_VIDEO);
			intents.putExtra(VRVideo.KEY_COMMAND_CODE, commandCode);
			context.startService(intents);
		}
	}
}
