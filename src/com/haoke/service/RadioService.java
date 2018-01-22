/*
*describe:RadioService是收音后台管理服务
*author:林永彬
*date:2016.09.21
*/

package com.haoke.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
public class RadioService extends Service {

	private static final String TAG = "RadioService";
	
	@Override
	public void onCreate() {
		Log.v(TAG, "onCreate");	
		super.onCreate();
		// 发广播通知服务已经启动
		//Intent intent = new Intent();
		//intent.setAction(GlobalDef.RADIO_SERVICE_ACTION_REBOOT);
		//this.sendBroadcast(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
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
		Log.v(TAG, "onDestroy");
		super.onDestroy();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.v(TAG, "onBind");
		return null;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.v(TAG, "onUnbind");
		return super.onUnbind(intent);
	}
	
}
