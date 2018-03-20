package com.amd.bt;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.haoke.btjar.main.BTDef.BTFunc;
import com.haoke.data.AllMediaList;
import com.haoke.serviceif.BTService_Listener;
import com.haoke.util.DebugLog;

public class BT_CallBack {

	private final String TAG = this.getClass().getSimpleName();
	private CallBackHandler mHandler = null; // 本地消息处理句柄
	private ArrayList<BT_Listener> mListenerList = new ArrayList<BT_Listener>();
	private ArrayList<BTService_Listener> mBTListenerList = new ArrayList<BTService_Listener>();

	public BT_CallBack() {
		mHandler = new CallBackHandler();
	}

	protected void onServiceConn() {
		for (int i = 0; i < mBTListenerList.size(); i++) {
			BTService_Listener listener = mBTListenerList.get(i);
			if (listener == null)
				continue;

			listener.onBTServiceConn();
		}
	}

	public void registerBTCallBack(BTService_Listener listener) {
		boolean found = false;
		for (int i = 0; i < mBTListenerList.size(); i++) {
			if (mBTListenerList.get(i) == listener) {
				found = true;
				break;
			}
		}
		if (!found)
			mBTListenerList.add(listener);
	}

	public void unregisterBTCallBack(BTService_Listener listener) {
		for (int i = 0; i < mBTListenerList.size(); i++) {
			if (mBTListenerList.get(i) == listener) {
				mBTListenerList.remove(i);
				break;
			}
		}
	}

	public void registerModeCallBack(BT_Listener listener) {
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

	public void unregisterModeCallBack(BT_Listener listener) {
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

		DebugLog.v(TAG, "HMI------------onDataChange mode=" + mode + ", func="
				+ func + ", data=" + data);
		Message message = mHandler.obtainMessage();
		message.what = mode; // 模式ID
		message.arg1 = func; // 功能ID
		message.arg2 = data; // 参数
		mHandler.sendMessage(message);
	}

	// 本地消息处理类
	@SuppressLint("HandlerLeak")
	private class CallBackHandler extends Handler {
		public void handleMessage(Message msg) {
			int mode = msg.what;
			int func = msg.arg1;
			int data = msg.arg2;
			for (int i = 0; i < mListenerList.size(); i++) {
				BT_Listener listener = mListenerList.get(i);
				if (listener == null)
					continue;
				listener.onBTDataChange(mode, func, data);
			}
			if (func == BTFunc.MUSIC_PLAY_STATE ||
					func == BTFunc.MUSIC_ID3_UPDATE) {
				AllMediaList.notifyUpdateAppWidgetByBTMusic();
			}
		}
	}
}
