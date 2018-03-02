package com.amd.radio;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import com.amd.media.AudioFocus.AudioFocusListener;
import com.haoke.define.McuDef.McuFunc;
import com.haoke.constant.MediaUtil.PlayState;
import com.haoke.define.RadioDef.RadioFunc;
import com.haoke.serviceif.CarService_Listener;
import com.amd.media.AudioFocus;
import com.amd.radio.Radio_CarListener;
import com.amd.radio.Radio_IF;
import com.amd.util.Source;

public class RadioManager implements Radio_CarListener, CarService_Listener,
		AudioFocusListener {

	private static final String TAG = "RadioManager";
	private Radio_IF mIF = null;
	private AudioFocus mAudioFocus = null;
    private AudioManager mAudioManager;
    private ComponentName mComponentName;

	public RadioManager(Service parent) {
		
		mAudioFocus = new AudioFocus(parent);

		mIF = Radio_IF.getInstance();
		mIF.setContext(parent);
		mIF.registerModeCallBack(this); // 注册服务监听
		mIF.registerCarCallBack(this); // 注册服务监听
		mIF.bindCarService();

		mAudioFocus.registerListener(this); // 注册焦点监听
		
		Service mParent = parent;
		mAudioManager = (AudioManager)mParent.getSystemService(Context.AUDIO_SERVICE);
        mComponentName = new ComponentName(mParent, RadioMediaButtonReceiver.class);
	}

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
		Log.v(TAG, "HMI------------onServiceConn source=" + mIF.getCurSource());
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
		int audioFocusState = mAudioFocus.getFocusState();
		if (audioFocusState == AudioManager.AUDIOFOCUS_GAIN
				|| audioFocusState == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
			ret = true;

		return ret;
	}
	
	public boolean requestAudioFocus(boolean request) {
		boolean focus = hasAudioFocus();
		Log.d(TAG, "requestAudioFocus request="+request+"; focus="+focus);
		if (!focus) {
			return mAudioFocus.requestAudioFocus(request);
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
			if (mComponentName != null) {
                mAudioManager.registerMediaButtonEventReceiver(mComponentName);
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
			//MediaInterfaceUtil.resetMediaPlayStateRecord(Source.RADIO);
//			if (!enable) {
//				Log.v(TAG, "HMI------------audioFocusChanged STOP 2");
//				return;
//			}
		    if (mComponentName != null) {
                mAudioManager.unregisterMediaButtonEventReceiver(mComponentName);
            }
			setRecordRadioOnOff(false);
			mIF.setEnable(false);
			break;
		}
	}

	@Override
	public void onCarDataChange(int mode, int func, int data) {
		if (Source.isMcuMode(mode)) {
			Log.v(TAG, "onCarDataChange MCU func=" + func + ", data=" + data);
			switch (func) {
			case McuFunc.SOURCE:
				break;
			}

		} else if (Source.isRadioMode(mode)) {
			switch (func) {
			case RadioFunc.FREQ:
			    if(data>5000 && data%10 > 0){//纠错
			        mIF.setCurFreq(8750);
	            }
			    break;
			case RadioFunc.STATE:
				mIF.isScanStateChange(data);
				break;
			}
		}
	}

	@Override
	public void setCurInterface(int data) {

	}
}
