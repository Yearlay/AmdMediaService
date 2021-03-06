package com.amd.bt;

import android.content.Context;
import android.media.AudioManager;
import android.os.RemoteException;
import com.amd.media.AudioFocus;
import com.amd.util.Source;
import com.haoke.aidl.ICarCallBack;
import com.haoke.define.McuDef.McuFunc;
import com.haoke.service.MediaService;
import com.haoke.serviceif.CarService_IF;
import com.haoke.serviceif.CarService_Listener;
import com.haoke.util.DebugLog;
import com.haoke.util.Media_IF;

public class BTMusic_IF extends CarService_IF {

	private final String TAG = this.getClass().getSimpleName();
	private static BTMusic_IF mSelf = null;
	private BTMusic_CarCallBack mCarCallBack = null;
	private boolean mServiceConn = false;

	public BTMusic_IF() {
		mMode = com.haoke.define.ModeDef.BTMUSIC;
		mCarCallBack = new BTMusic_CarCallBack();
		
		// 以下处理服务回调
		mICallBack = new ICarCallBack.Stub() {
			@Override
			public void onDataChange(int mode, int func, int data)
					throws RemoteException {
				if (Source.isMcuMode(mode) && func == McuFunc.SOURCE) {
				} else {
					mCarCallBack.onDataChange(mode, func, data);
				}
			}
		};
	}
	
	// 获取接口实例
	synchronized public static BTMusic_IF getInstance() {
		if (mSelf == null) {
			mSelf = new BTMusic_IF();
		}
		return mSelf;
	}

	// 设置上下文
	public void setContext(Context context) {
		mContext = context;
	}

	// 获取模式
	public int getMode() {
		return mMode;
	}
	
	// 服务已经绑定成功，需要刷新动作
	@Override
	protected void onServiceConn() {
		mCarCallBack.onServiceConn();
		mServiceConn = true;
	}
	
	@Override
	protected void onServiceDisConn() {
		super.onServiceDisConn();
		DebugLog.v(TAG, "HMI------------onServiceDisConn");
		mServiceConn = false;
	}
	
	public boolean isServiceConnected() {
		return mServiceConn;
	}

	// 注册车载服务回调（全局状态变化）
	public void registerCarCallBack(CarService_Listener listener) {
		mCarCallBack.registerCarCallBack(listener);
	}

	// 注销车载服务回调（全局状态变化）
	public void unregisterCarCallBack(CarService_Listener listener) {
		mCarCallBack.unregisterCarCallBack(listener);
	}
	
	// 注册车载服务回调（模块相关变化）
	public void registerModeCallBack(BTMusic_CarListener listener) {
		mCarCallBack.registerModeCallBack(listener);
	}

	// 注销车载服务回调（模块相关变化）
	public void unregisterModeCallBack(BTMusic_CarListener listener) {
		mCarCallBack.unregisterModeCallBack(listener);
	}
	
	//禁止UI层调用
	public void sendSouceChange(int source) {
		mCarCallBack.onDataChange(com.haoke.define.ModeDef.MCU, McuFunc.SOURCE, source);
	}
	
   public AudioFocus getAudioFocus() {
        return MediaService.getInstance().getBtMusicManager().getAudioFocus();
    }
	
	public boolean hasAudioFocus() {
		boolean ret = false;
		int audioFocusState = getAudioFocus().getFocusState();
		if (audioFocusState == AudioManager.AUDIOFOCUS_GAIN
				|| audioFocusState == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
			ret = true;

		return ret;
	}
	
	// 设置当前音频焦点
	public boolean requestAudioFocus(boolean request) {
		try {
			if (!hasAudioFocus()) {
				return getAudioFocus().requestAudioFocus(request);
			}
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return true;
	}
	
	// 设置当前源
	public boolean setCurSource(int source) {
	    return Media_IF.setCurSource(source);
	}

}
