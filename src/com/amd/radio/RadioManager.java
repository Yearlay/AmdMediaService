package com.amd.radio;

import android.media.AudioManager;
import android.util.Log;

import com.amd.media.AudioFocus.AudioFocusListener;
import com.haoke.define.McuDef.McuFunc;
import com.haoke.define.MediaDef.PlayState;
import com.haoke.define.RadioDef.RadioFunc;
import com.haoke.define.ModeDef;
import com.haoke.mediaservice.R;
import com.haoke.service.RadioService;
import com.haoke.serviceif.CarService_Listener;
import com.amd.media.MediaInterfaceUtil;
import com.amd.radio.Radio_CarListener;
import com.amd.radio.Radio_IF;

public class RadioManager implements Radio_CarListener, CarService_Listener,
		AudioFocusListener {

	private static final String TAG = "RadioManager";
	private RadioService mParent = null;
	private Radio_IF mIF = null;
	private int mCurSource = ModeDef.NULL;
	private static boolean isScan5S = false;
    private static boolean isRescan = false;

	public RadioManager(RadioService parent) {
		mParent = parent;

		mIF = Radio_IF.getInstance();
		mIF.registerModeCallBack(this); // 注册服务监听
		mIF.registerCarCallBack(this); // 注册服务监听
		mIF.bindCarService();

		mParent.getAudioFocus().registerListener(this); // 注册焦点监听
	}

	// add by lyb 20170405
	// 注册接收器
	public void registerReceiver() {
		Log.v(TAG, "registerReceiver");
	}

	// 注销接收器
	public void unregisterReceiver() {
		Log.v(TAG, "unregisterReceiver");
	}
	
	@Override
	public void onServiceConn() {
		// TODO Auto-generated method stub
		Log.v(TAG, "HMI------------onServiceConn source=" + mIF.getCurSource());
		sourceChanged(mIF.getCurSource());
	}

	/**
	 * 切换播放源，参数为 {@link com.haoke.define.ModeDef} <p>
	 * eg. {@link com.haoke.define.ModeDef#VIDEO}
	 */
	public void sourceChanged(int source) {
		Log.v(TAG, "sourceChanged source=" + source);
		if (source == ModeDef.IMAGE) {
			return;
		}
		if (source == ModeDef.AUDIO || source == ModeDef.VIDEO || source == ModeDef.BT) {
			mCurSource = ModeDef.NULL;
//			if (mIF.isEnable()) {
//				Log.v(TAG, "sourceChanged setEnable false");
//				mIF.setEnable(false); // 暂停播放
//			}
		}
		if (source != ModeDef.AUDIO && source != ModeDef.VIDEO 
				&& source != ModeDef.IMAGE && source != ModeDef.BT) {
			mCurSource = source;
		}
	}
	
	private boolean mRecordRadioOn = false; // 用来记忆被抢焦点前的播放状态，便于恢复播放
	// 设置播放状态（被抢焦点前）
	public void setRecordRadioOnOff(boolean on) {
		mRecordRadioOn = on;
	}

	// 获取播放状态（被抢焦点前）
	public boolean getRecordRadioOn() {
		return mRecordRadioOn;
	}
	
	private boolean hasAudioFocus() {
		boolean ret = false;
		int audioFocusState = mParent.getAudioFocus().getFocusState();
		if (audioFocusState == AudioManager.AUDIOFOCUS_GAIN
				|| audioFocusState == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
			ret = true;

		return ret;
	}
	
	public boolean requestAudioFocus(boolean request) {
		boolean focus = hasAudioFocus();
		Log.d(TAG, "requestAudioFocus request="+request+"; focus="+focus);
		if (!focus) {
			return mParent.getAudioFocus().requestAudioFocus(request);
		}
		return true;
	}

	// 焦点变化通知
	@Override
	public void audioFocusChanged(int state) {
		boolean enable = mIF.isEnable();
		boolean radioOn = getRecordRadioOn();
		Log.v(TAG, "HMI------------audioFocusChanged state=" + state + "; enable=" + enable + "; radioOn="+radioOn);
		switch (state) {
		case PlayState.PLAY:
			if (radioOn) {
				setRecordRadioOnOff(false);
				mIF.setEnable(true);
			}
			break;
		case PlayState.PAUSE:
			if (!enable) {
				Log.v(TAG, "HMI------------audioFocusChanged STOP 1");
				return;
			}
			setRecordRadioOnOff(enable);
			mIF.setEnable(false);
			break;
		case PlayState.STOP:
			MediaInterfaceUtil.resetMediaPlayStateRecord(ModeDef.RADIO);
//			if (!enable) {
//				Log.v(TAG, "HMI------------audioFocusChanged STOP 2");
//				return;
//			}
			setRecordRadioOnOff(false);
			mIF.setEnable(false);
			break;
		}
	}

	@Override
	public void onCarDataChange(int mode, int func, int data) {
		if (mode == ModeDef.MCU) {
			Log.v(TAG, "onCarDataChange MCU func=" + func + ", data=" + data);
			switch (func) {
			case McuFunc.SOURCE:
				sourceChanged(data);
				break;
			}

		} else if (mode == ModeDef.RADIO) {
			switch (func) {
			case RadioFunc.STATE:
				isScanStateChange(data);
				break;
			}
		}
//		else if (mode == ModeDef.BT) { // 通话开始或结束，声音需要处理
//			Log.v(TAG, "onCarDataChange BT func=" + func + ", data=" + data);
//			Log.v(TAG, "onCarDataChange BT source=" + mIF.getCurSource());
//
//			if (func == BTFunc.CALL_STATE) {
//				if (mIF.getCurSource() == mIF.getMode()) { // 处于当前源
//					if (data == BTCallState.IDLE) { // 打完电话，需要再切下通道，避免没声音
//						Log.v(TAG, "onCarDataChange openAvio");
//						audioFocusChanged(PlayState.PLAY);
//					}
//				}
//			}
//		}
	}

	@Override
	public void setCurInterface(int data) {
		// TODO Auto-generated method stub

	}
	
	private void isScanStateChange(int data) {
    	//data为2表示SCAN[Scan5S]， 为3表示SEARCH[Rescan]
    	if (data == 2) {
    		isScan5S = true;
    	} else if (data == 3) {
    		isRescan = true;
    	} else if (data == 0) {
    		isRescan = false;
    		isScan5S = false;
    	}
    }
	
	//获取是否在扫描状态
	public boolean isRescanState() {
		return isRescan;
	}
	
	//获取是否在预览状态
	public boolean isScan5SState() {
		return isScan5S;
	}
}
