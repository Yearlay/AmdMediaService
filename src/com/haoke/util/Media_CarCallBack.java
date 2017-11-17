package com.haoke.util;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.amd.util.Source;
import com.haoke.serviceif.CarService_Listener;

public class Media_CarCallBack {

	private final String TAG = this.getClass().getSimpleName();
	private CallBackHandler mHandler; // 本地消息处理句柄
	private UartCallBackHandler mUartCallBackHandler;
	private ArrayList<Media_CarListener> mListenerList = new ArrayList<Media_CarListener>();
	private ArrayList<CarService_Listener> mCarListenerList = new ArrayList<CarService_Listener>();

	public Media_CarCallBack() {
		mHandler = new CallBackHandler();
		mUartCallBackHandler = new UartCallBackHandler();
	}

	protected void onServiceConn() {
		for (int i = 0; i < mCarListenerList.size(); i++) {
			CarService_Listener listener = mCarListenerList.get(i);
			if (listener == null)
				continue;

			listener.onServiceConn();
		}
	}

	public void registerCarCallBack(CarService_Listener listener) {
		boolean found = false;
		for (int i = 0; i < mCarListenerList.size(); i++) {
			if (mCarListenerList.get(i) == listener) {
				found = true;
				break;
			}
		}
		if (!found)
			mCarListenerList.add(listener);
	}

	public void unregisterCarCallBack(CarService_Listener listener) {
		for (int i = 0; i < mCarListenerList.size(); i++) {
			if (mCarListenerList.get(i) == listener) {
				mCarListenerList.remove(i);
				break;
			}
		}
	}

	public void registerModeCallBack(Media_CarListener listener) {
		boolean found = false;
		for (int i = 0; i < mListenerList.size(); i++) {
			if (mListenerList.get(i) == listener) {
				found = true;
				break;
			}
		}
		if (!found)
			mListenerList.add(listener);
	}

	public void unregisterModeCallBack(Media_CarListener listener) {
		for (int i = 0; i < mListenerList.size(); i++) {
			if (mListenerList.get(i) == listener) {
				mListenerList.remove(i);
				break;
			}
		}
	}

	public void onDataChange(int mode, int func, int data) {
		if (mHandler == null) {
			return;
		}

		int curMode = Media_IF.getInstance().getMode();
		if (mode == curMode || Source.isMcuMode(mode)
		        || Source.isBTMode(mode) || Source.isEQMode(mode)) {
			Log.v(TAG, "HMI------------onDataChange mode=" + mode + ", func="
					+ func + ", data=" + data);
			Message message = mHandler.obtainMessage();
			message.what = mode; // 模式ID
			message.arg1 = func; // 功能ID
			message.arg2 = data; // 参数
			mHandler.sendMessage(message);
		}
	}

	// 本地消息处理类
	@SuppressLint("HandlerLeak")
	private class CallBackHandler extends Handler {
		public void handleMessage(Message msg) {
			int mode = msg.what;
			int func = msg.arg1;
			int data = msg.arg2;

			for (int i = 0; i < mListenerList.size(); i++) {
				Media_CarListener listener = mListenerList.get(i);
				if (listener == null)
					continue;
				listener.onCarDataChange(mode, func, data);
			}
		}
	}
	
	public void onUartDataChange(int mode, int len, byte[] datas) {
		if (mUartCallBackHandler == null) {
			return;
		}
		if (datas.length >0 && mode == com.haoke.define.ModeDef.MCU_UART) {
			Message message = mUartCallBackHandler.obtainMessage();
			message.what = mode; // 模式ID
			message.arg1 = len; // 功能ID
			Bundle bundle = new Bundle();
			bundle.putByteArray("uartbfer", datas);
			message.setData(bundle);
			mUartCallBackHandler.sendMessage(message);	
		}
	}
	
	@SuppressLint("HandlerLeak")
	private class UartCallBackHandler extends Handler {
		public void handleMessage(Message msg) {
			int mode = msg.what;
			int len = msg.arg1;
			byte[] buffer = msg.getData().getByteArray("uartbfer");
				for (int i = 0; i < mListenerList.size(); i++) {
					Media_CarListener listener = mListenerList.get(i);
					if (listener == null)
						continue;
					listener.onUartDataChange(mode, len, buffer);
				}
		}
	}
}
