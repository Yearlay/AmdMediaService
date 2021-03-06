package com.amd.radio;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.util.Log;

import com.amd.media.AudioFocus.AudioFocusListener;
import com.haoke.define.McuDef.McuFunc;
import com.haoke.constant.MediaUtil.PlayState;
import com.haoke.define.RadioDef.RadioFunc;
import com.haoke.serviceif.CarService_Listener;
import com.haoke.util.DebugLog;
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
		
		mAudioManager = (AudioManager)parent.getSystemService(Context.AUDIO_SERVICE);
        mComponentName = new ComponentName(parent, RadioMediaButtonReceiver.class);
        
        IntentFilter intentFilter=new IntentFilter();
        intentFilter.addAction("com.jsbd.media.KeyCode.TRACKDN");
        intentFilter.addAction("com.jsbd.media.KeyCode.TRACKUP");
        BroadcastReceiver receiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                boolean isRadioSource = Source.isRadioSource();
                boolean hasFocus = hasAudioFocus();
                DebugLog.d(TAG, "action =" + action + ", isRadioSource ="
                        + isRadioSource + ", hasFocus =" + hasFocus);
                //判断由当前源时收音机  有焦点
                if(isRadioSource && hasFocus){
                    if (action.equals("com.jsbd.media.KeyCode.TRACKDN")) {
                        mIF.setNextSearch();
                    } else if (action.equals("com.jsbd.media.KeyCode.TRACKUP")) {
                        mIF.setPreSearch();
                    }
                }
            }
        };
        parent.registerReceiver(receiver, intentFilter);
	}

	// 注册接收器
	public void registerReceiver() {
		DebugLog.v(TAG, "registerReceiver");
	}

	// 注销接收器
	public void unregisterReceiver() {
		DebugLog.v(TAG, "unregisterReceiver");
	}
	
	@Override
	public void onServiceConn() {
		DebugLog.v(TAG, "HMI------------onServiceConn source=" + mIF.getCurSource());
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
	
	public boolean hasAudioFocus() {
		boolean ret = false;
		int audioFocusState = mAudioFocus.getFocusState();
		if (audioFocusState == AudioManager.AUDIOFOCUS_GAIN
				|| audioFocusState == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
			ret = true;

		return ret;
	}
	
	public boolean requestAudioFocus(boolean request) {
		boolean focus = hasAudioFocus();
		DebugLog.d(TAG, "requestAudioFocus request="+request+"; focus="+focus);
		if (!focus || !Source.isRadioSource()) {
//            if (getRecordRadioOn()) {
//                DebugLog.e(TAG, "requestAudioFocus reset RecordPlayState!");
//                setRecordRadioOnOff(false);
//            }
			return mAudioFocus.requestAudioFocus(request);
		}
		return true;
	}

	// 焦点变化通知
	@Override
	public void audioFocusChanged(int state) {
		boolean enable = mIF.isEnable();
		boolean radioOn = getRecordRadioOn();
		DebugLog.v(TAG, "HMI------------audioFocusChanged state=" + state + "; enable=" + enable + "; radioOn="+radioOn);
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
				DebugLog.v(TAG, "HMI------------audioFocusChanged STOP 1");
				return;
			}
			setRecordRadioOnOff(enable);
			mIF.setEnable(false);
			break;
		case PlayState.STOP:
			//MediaInterfaceUtil.resetMediaPlayStateRecord(Source.RADIO);
//			if (!enable) {
//				DebugLog.v(TAG, "HMI------------audioFocusChanged STOP 2");
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
	public void onRadioCarDataChange(int mode, int func, int data) {
		if (Source.isMcuMode(mode)) {
			DebugLog.v(TAG, "onCarDataChange MCU func=" + func + ", data=" + data);
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
	public void setRadioCurInterface(int data) {

	}
}
