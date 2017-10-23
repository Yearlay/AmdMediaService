/*
*describe:RadioService是收音后台管理服务
*author:林永彬
*date:2016.09.21
*/

package com.haoke.service;

import com.amd.media.AudioFocus;
import com.haoke.define.GlobalDef;
import com.amd.radio.RadioManager;
import com.amd.radio.Radio_IF;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
public class RadioService extends Service {

	private final String TAG = this.getClass().getSimpleName();
	private static RadioService mSelf = null;
	private RadioManager mRadioManager = null;
	private AudioFocus mAudioFocus = null;
	
	public static RadioService getInstance() {
		return mSelf;
	}
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		Log.v(TAG, "onCreate");	
		super.onCreate();
		mSelf = this;
		Radio_IF.getInstance().setContext(this);
		
		mAudioFocus = new AudioFocus(this);
		mRadioManager = new RadioManager(this);
		mRadioManager.registerReceiver();
		
		// 发广播通知服务已经启动
		Intent intent = new Intent();
		intent.setAction(GlobalDef.RADIO_SERVICE_ACTION_REBOOT);
		this.sendBroadcast(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		Log.v(TAG, "onStartCommand");
		
		flags = Service.START_STICKY;
		//将服务拉到前台，避免被销毁
//		Notification notification = new Notification();
//		notification.flags = Notification.FLAG_ONGOING_EVENT;
//		notification.flags |= Notification.FLAG_NO_CLEAR;
//		notification.flags |= Notification.FLAG_FOREGROUND_SERVICE;
//		this.startForeground(startId, notification);
		
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		Log.v(TAG, "onDestroy");
		super.onDestroy();
		mRadioManager.unregisterReceiver();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		Log.v(TAG, "onBind");
		return null;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// TODO Auto-generated method stub
		Log.v(TAG, "onUnbind");
		return super.onUnbind(intent);
	}
	
	public RadioManager getRadioManager() {
		return mRadioManager;
	}
	
	public AudioFocus getAudioFocus() {
		return mAudioFocus;
	}
}
