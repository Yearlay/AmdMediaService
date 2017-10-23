package com.amd.bt;

import android.content.Context;
import android.media.AudioManager;
import android.os.RemoteException;
import android.util.Log;

import com.haoke.aidl.ICarCallBack;
import com.haoke.btjar.main.BTDef.BTConnState;
import com.haoke.define.ModeDef;
import com.haoke.define.McuDef.KeyCode;
import com.haoke.define.McuDef.KeyState;
import com.haoke.define.McuDef.McuFunc;
import com.haoke.service.BTMusicService;
import com.haoke.serviceif.CarService_IF;
import com.haoke.serviceif.CarService_Listener;
import com.haoke.util.Media_IF;

public class BTMusic_IF extends CarService_IF {

	private final String TAG = this.getClass().getSimpleName();
	private static BTMusic_IF mSelf = null;
	private BTMusic_CarCallBack mCarCallBack = null;
	private boolean mServiceConn = false;

	public BTMusic_IF() {
		mMode = ModeDef.BTMUSIC;
		mCarCallBack = new BTMusic_CarCallBack();
		
		// 以下处理服务回调
		mICallBack = new ICarCallBack.Stub() {
			@Override
			public void onDataChange(int mode, int func, int data)
					throws RemoteException {
				if (mode == ModeDef.MCU && func == McuFunc.SOURCE) {
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
		Log.v(TAG, "HMI------------onServiceDisConn");
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
		mCarCallBack.onDataChange(ModeDef.MCU, McuFunc.SOURCE, source);
	}
	
	private boolean hasAudioFocus() {
		boolean ret = false;
		int audioFocusState = BTMusicService.getInstance().getAudioFocus().getFocusState();
		if (audioFocusState == AudioManager.AUDIOFOCUS_GAIN
				|| audioFocusState == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
			ret = true;

		return ret;
	}
	
	// 设置当前音频焦点
	public boolean requestAudioFocus(boolean request) {
		try {
			if (!hasAudioFocus()) {
				return BTMusicService.getInstance().getAudioFocus().requestAudioFocus(request);
			}
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return true;
	}
	
	// 设置当前源
	public boolean setCurSource(int source) {
		try {
			return Media_IF.setCurSource(source);
//			return mServiceIF.mcu_setCurSource(source);
        } catch (Exception e) {
    		Log.e(TAG, "HMI------------interface e="+e.getMessage());
        }	
		return false;
	}

	// 获取当前源
	public int getCurSource() {
		try {
			return Media_IF.getCurSource();
//			return mServiceIF.mcu_getCurSource();
        } catch (Exception e) {
    		Log.e(TAG, "HMI------------interface e="+e.getMessage());
        }	
		return ModeDef.NULL;
	}
	
	// 获取蓝牙连接状态
	public int getConnState() {
		try {
			return mServiceIF.bt_getConnState();
        } catch (Exception e) {
    		Log.e(TAG, "HMI------------interface e="+e.getMessage());
        }	
		return BTConnState.DISCONNECTED;
	}
	
	public void onBackPress() {
		try {
			Log.e(TAG, "HMI------------onBackPress mServiceIF=" + mServiceIF);
			byte[] data = new byte[2];
			data[0] = KeyState.PRESS_RELEASED;
			data[1] = (byte) KeyCode.HOME;
			mServiceIF.mcu_sendDataToMcu((byte) 0x0C, 0x01, data);
        } catch (Exception e) {
    		Log.e(TAG, "HMI------------interface e="+e.getMessage());
        }	
	}
}
