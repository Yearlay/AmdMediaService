package com.amd.bt;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import com.amd.media.AudioFocus;
import com.amd.media.MediaInterfaceUtil;
import com.amd.media.AudioFocus.AudioFocusListener;
import com.amd.util.Source;
import com.haoke.data.AllMediaList;
import com.haoke.define.McuDef.McuFunc;
import com.haoke.btjar.main.BTDef.BTConnState;
import com.haoke.btjar.main.BTDef.BTFunc;
import com.haoke.constant.MediaUtil.PlayState;
import com.haoke.serviceif.BTService_Listener;
import com.haoke.serviceif.CarService_Listener;
import com.haoke.util.Media_IF;

public class BTMusicManager implements CarService_Listener,
		BTMusic_CarListener, BTService_Listener, BT_Listener,
		AudioFocusListener {

	private final String TAG = this.getClass().getSimpleName();
	private Service mParent = null;
	private BTMusic_IF mIF = null;
	private BT_IF mBTIF = null;
	private final int SOURCE_NULL = Source.NULL;
	private int mRecordPlayState = PlayState.STOP;
	
	private AudioManager mAudioManager;
	private ComponentName mComponentName;
	
	private AudioFocus mAudioFocus = null;
    public static boolean mPlay = false;

	public BTMusicManager(Service parent) {
		mParent = parent;
		
		mAudioFocus = new AudioFocus(parent);

		mIF = BTMusic_IF.getInstance();
		mIF.setContext(parent);
		mIF.registerModeCallBack(this); // 注册服务监听
		mIF.registerCarCallBack(this); // 注册服务监听
		mIF.bindCarService();

		mBTIF = BT_IF.getInstance();
		mBTIF.setContext(parent);
		mBTIF.registerModeCallBack(this); // 注册服务监听
		mBTIF.registerBTCallBack(this); // 注册服务监听
		mBTIF.bindBTService();

		mAudioFocus.registerListener(this); // 注册焦点监听
		
		mAudioManager = (AudioManager)mParent.getSystemService(Context.AUDIO_SERVICE);
		mComponentName = new ComponentName(mParent, BTMediaButtonReceiver.class); 
	}

	@Override
	public void onServiceConn() {
	}

	@Override
	public void onBTServiceConn() {
	}

	// 焦点变化通知
	@Override
	public void audioFocusChanged(int state) {
		boolean playing = mBTIF.music_isPlaying();
		int recordPlayState = getRecordPlayState();
		Log.v(TAG, "HMI------------audioFocusChanged state=" + state+"; playing="+playing+"; recordPlayState="+recordPlayState);
		switch (state) {
		case PlayState.PLAY:
			/*if (recordPlayState == PlayState.PAUSE) {
//				mBTIF.music_open();
//				mBTIF.music_play();
			} else if (recordPlayState == PlayState.STOP) {
//				mBTIF.music_open();
				mBTIF.music_play();
				setRecordPlayState(PlayState.PLAY);
			} else {
//				mBTIF.music_open();
				mBTIF.music_play();
				setRecordPlayState(PlayState.PLAY);
			}*/
			mBTIF.music_openEx();
			if (recordPlayState == PlayState.PLAY) {
			    if (MediaInterfaceUtil.mediaCannotPlayNoToast()) {
	            } else {
	                // 清除标志，避免原本是暂停，每次抢焦点都进行播放
	                setRecordPlayState(PlayState.STOP);
	                mBTIF.music_play();
	            }
			    mPlay = true;
			}
			mAudioManager.registerMediaButtonEventReceiver(mComponentName);
			break;

		case PlayState.PAUSE:
			//mBTIF.music_pause();
			mBTIF.music_close_pause();
            if (recordPlayState == PlayState.PLAY
                    && MediaInterfaceUtil.mediaCannotPlayNoToast()) {
            } else {
                if (Media_IF.getOnlyBtCallState()) {
                    setRecordPlayState(PlayState.STOP);
                } else {
                    setRecordPlayState(playing || mPlay ? PlayState.PLAY : PlayState.STOP);
                    if(mPlay){
                        mPlay = !mPlay;
                    }
                }
            }
			break;
			
		case PlayState.STOP:
			//MediaInterfaceUtil.resetMediaPlayStateRecord(SOURCE_BT);
			mAudioManager.unregisterMediaButtonEventReceiver(mComponentName);
			mBTIF.music_stop();
//			mBTIF.music_close();
			setRecordPlayState(PlayState.STOP);
			break;
		}
	}
	
	public AudioFocus getAudioFocus() {
	    return mAudioFocus;
	}
	
	public int getRecordPlayState() {
		return mRecordPlayState;
	}

	public void setRecordPlayState(int recordPlayState) {
		mRecordPlayState = recordPlayState;
	}

	@Override
	public void onCarDataChange(int mode, int func, int data) {
		Log.v(TAG, "onCarDataChange mode=" + mode + ", func=" + func + ", data=" + data);
		if (Source.isMcuMode(mode)) {
		    if (func == McuFunc.SOURCE) {
		        if (Source.isBTMusicSource(data)) {
		            //source change to BT, fix bug 20541
		            AllMediaList.notifyUpdateAppWidgetByBTMusic();
		        }
		    }
		}
	}
	
	@Override
	public void onBTDataChange(int mode, int func, int data) {
		if (Source.isBTMode(mode)) {
			switch (func) {
			case BTFunc.CONN_STATE:
				if (data == BTConnState.DISCONNECTED) {
					mBTIF.music_stop();
					if (Source.isBTMusicSource()) {
						mIF.setCurSource(SOURCE_NULL);
						AllMediaList.notifyUpdateAppWidgetByBTMusic();
					} else if (Source.isBTMusicSource(Media_IF.sLastSource)) {
						Media_IF.sLastSource = SOURCE_NULL;
						AllMediaList.notifyUpdateAppWidgetByBTMusic();
					}
				}
				break;
			case BTFunc.MUSIC_PLAY_STATE:
				// 手机端连接蓝牙后，在手机端操作放歌
//				int source = mIF.getCurSource();
//				if (mCurSource == SOURCE_NULL || source != BTSOURCE) {
//					if (mBTIF.music_isPlaying()) {
//						mIF.setCurSource(BTSOURCE);
//						mIF.requestAudioFocus(true);
//					}
//				}
				break;
			case BTFunc.MUSIC_ID3_UPDATE:
				break;
			}
		}
	}
}
