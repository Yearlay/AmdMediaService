package com.amd.radio;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.amd.util.Source;
import com.haoke.data.AllMediaList;
import com.haoke.define.RadioDef.RadioFunc;
import com.haoke.serviceif.CarService_Listener;
import com.haoke.util.DebugLog;

public class Radio_CarCallBack {

	private final String TAG = this.getClass().getSimpleName();
	private CallBackHandler mHandler = null; // 本地消息处理句柄
	private ArrayList<Radio_CarListener> mListenerList = new ArrayList<Radio_CarListener>();
	private ArrayList<CarService_Listener> mCarListenerList = new ArrayList<CarService_Listener>();

	public Radio_CarCallBack() {
		mHandler = new CallBackHandler();
	}

	protected void onServiceConn() {
		for (int i = 0; i < mCarListenerList.size(); i++) {
			CarService_Listener listener = mCarListenerList.get(i);
			if (listener == null)
				continue;

			listener.onServiceConn();
		}
		AllMediaList.notifyUpdateAppWidgetByRadio();//收音机绑定成功，需要通知桌面媒体框
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

	public void registerModeCallBack(Radio_CarListener listener) {
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

	public void unregisterModeCallBack(Radio_CarListener listener) {
		for (int i = 0; i < mListenerList.size(); i++) {
			if (mListenerList.get(i) == listener) {
				mListenerList.remove(i);
				break;
			}
		}
	}

	public void setInterface(int id) {
		for (int i = 0; i < mListenerList.size(); i++) {
			Radio_CarListener listener = mListenerList.get(i);
			if (listener == null)
				continue;

			listener.setRadioCurInterface(id);
		}
	}

	public void onDataChange(int mode, int func, int data) {
		if (mHandler == null) {
			return;
		}

		int curMode = Radio_IF.getInstance().getMode();
		if (mode == curMode || Source.isMcuMode(mode) || Source.isBTMode(mode)) {
			DebugLog.v(TAG, "HMI------------onDataChange mode=" + mode + ", func="
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
				Radio_CarListener listener = mListenerList.get(i);
				if (listener == null)
					continue;

				listener.onRadioCarDataChange(mode, func, data);
			}
			if (func == RadioFunc.FREQ // 表示频率发生改变。
	                || func == RadioFunc.STATE // 表示收音播放状态发生改变
	                || func == RadioFunc.ENABLE // 表示收音Enable状态发生改变
	                || func == RadioFunc.CUR_CH /* 表示当前台发生变化 */ ) {
	            AllMediaList.notifyUpdateAppWidgetByRadio();
	        }
		}
	}
}
