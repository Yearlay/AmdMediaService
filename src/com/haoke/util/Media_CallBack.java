package com.haoke.util;

import java.util.ArrayList;

import com.haoke.data.AllMediaList;
import com.haoke.define.ModeDef;
import com.haoke.define.MediaDef.MediaFunc;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class Media_CallBack {

	private final String TAG = this.getClass().getSimpleName();
	private CallBackHandler mHandler = null; // 本地消息处理句柄
	private ArrayList<Media_Listener> mListenerList = new ArrayList<Media_Listener>();
	private int mMediaMode;

	public Media_CallBack(int mode) {
		mMediaMode = mode;
		mHandler = new CallBackHandler();
	}

	public void registerMediaCallBack(Media_Listener listener) {
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

	public void unregisterMediaCallBack(Media_Listener listener) {
		for (int i = 0; i < mListenerList.size(); i++) {
			if (mListenerList.get(i) == listener) {
				mListenerList.remove(i);
				break;
			}
		}
	}

	public void setInterface(int id) {
		for (int i = 0; i < mListenerList.size(); i++) {
			Media_Listener listener = mListenerList.get(i);
			if (listener == null)
				continue;

			listener.setCurInterface(id);
		}
	}

	public class DataElement {
		public int mData1 = 0;
		public int mData2 = 0;

		public DataElement(int data1, int data2) {
			mData1 = data1;
			mData2 = data2;
		}
	}

	public void onDataChange(int mode, int func, int data1, int data2) {
		if (mHandler == null) {
			return;
		}

		if (mode == mMediaMode) {
			Log.v(TAG, "HMI------------onDataChange mode=" + mode + ", func="
					+ func + ", data1=" + data1 + ", data2=" + data2);
			Message message = mHandler.obtainMessage();
			message.what = mode; // 模式ID
			message.arg1 = func; // 功能ID
			message.obj = new DataElement(data1, data2); // 参数
			mHandler.sendMessage(message);
		}
	}

	// 本地消息处理类
	@SuppressLint("HandlerLeak")
	private class CallBackHandler extends Handler {
		public void handleMessage(Message msg) {
			int curMode = mMediaMode;
			int mode = msg.what;
			int func = msg.arg1;
			DataElement datas = (DataElement) msg.obj;

			if (mode == curMode) {
				for (int i = 0; i < mListenerList.size(); i++) {
					Media_Listener listener = mListenerList.get(i);
					if (listener == null)
						continue;
					listener.onDataChange(mode, func, datas.mData1,
							datas.mData2);
				}
				if (func == MediaFunc.SCAN_STATE ||
						func == MediaFunc.PREPARED ||
						func == MediaFunc.PLAY_STATE ||
						func == MediaFunc.PLAY_OVER) {
					if (mode == ModeDef.MEDIA) {
						AllMediaList.notifyUpdateAppWidget();
					}
				}
			}
		}
	}
}
