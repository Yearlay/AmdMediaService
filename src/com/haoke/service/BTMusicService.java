/*
*describe:BTService是蓝牙后台管理服务
*author:林永彬
*date:2016.12.14
*/

package com.haoke.service;

import com.amd.bt.BTMusicManager;
import com.amd.bt.BTMusic_IF;
import com.amd.bt.BT_IF;
import com.amd.media.AudioFocus;
import com.haoke.define.GlobalDef;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class BTMusicService extends Service {

	private final String TAG = this.getClass().getSimpleName();
	private static BTMusicService mSelf = null;
	private BTMusicManager mBTMusicManager = null;
	private AudioFocus mAudioFocus = null;
	
	public static BTMusicService getInstance() {
		return mSelf;
	}
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		Log.v(TAG, "onCreate");	
		super.onCreate();
		mSelf = this;
		BTMusic_IF.getInstance().setContext(this);
		BT_IF.getInstance().setContext(this);
		
		mAudioFocus = new AudioFocus(this);
		mBTMusicManager = new BTMusicManager(this);
		
		// 发广播通知服务已经启动
		Intent intent = new Intent();
		intent.setAction(GlobalDef.BTMUSIC_SERVICE_ACTION_REBOOT);
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
	
	public BTMusicManager getBTManager() {
		return mBTMusicManager;
	}
	
	public AudioFocus getAudioFocus() {
		return mAudioFocus;
	}
}
