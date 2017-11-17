package com.amd.bt;

import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import com.amd.media.MediaInterfaceUtil;
import com.amd.media.AudioFocus.AudioFocusListener;
import com.amd.util.Source;
import com.haoke.data.AllMediaList;
import com.haoke.btjar.main.BTDef.BTConnState;
import com.haoke.btjar.main.BTDef.BTFunc;
import com.haoke.define.McuDef.McuFunc;
import com.haoke.constant.MediaUtil.PlayState;
import com.haoke.service.BTMusicService;
import com.haoke.serviceif.BTService_Listener;
import com.haoke.serviceif.CarService_Listener;
import com.haoke.util.Media_IF;

public class BTMusicManager implements CarService_Listener,
		BTMusic_CarListener, BTService_Listener, BT_Listener,
		AudioFocusListener {

	private final String TAG = this.getClass().getSimpleName();
	private BTMusicService mParent = null;
	private BTMusic_IF mIF = null;
	private BT_IF mBTIF = null;
	private final int SOURCE_BT = Source.BT;
	private final int SOURCE_NULL = Source.NULL;
	private int mCurSource = Source.NULL;
	private int mRecordPlayState = PlayState.STOP;
	
	private AudioManager mAudioManager;
	private ComponentName mComponentName;

	public BTMusicManager(BTMusicService parent) {
		mParent = parent;

		mIF = BTMusic_IF.getInstance();
		mIF.registerModeCallBack(this); // 注册服务监听
		mIF.registerCarCallBack(this); // 注册服务监听
		mIF.bindCarService();

		mBTIF = BT_IF.getInstance();
		mBTIF.registerModeCallBack(this); // 注册服务监听
		mBTIF.registerBTCallBack(this); // 注册服务监听
		mBTIF.bindBTService();

		mParent.getAudioFocus().registerListener(this); // 注册焦点监听
		
		mAudioManager = (AudioManager)mParent.getSystemService(Context.AUDIO_SERVICE);
		mComponentName = new ComponentName(mParent, BTMediaButtonReceiver.class); 
	}

	@Override
	public void onServiceConn() {
		sourceChanged(mIF.getCurSource());
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
				// 清除标志，避免原本是暂停，每次抢焦点都进行播放
				setRecordPlayState(PlayState.STOP);
				mBTIF.music_play();
			}
			mAudioManager.registerMediaButtonEventReceiver(mComponentName);
			break;

		case PlayState.PAUSE:
			//mBTIF.music_pause();
			mBTIF.music_close_pause();
			setRecordPlayState(playing ? PlayState.PLAY : PlayState.STOP);
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
			switch (func) {
			case McuFunc.SOURCE:
				sourceChanged(data);
				break;
				
			case McuFunc.KEY://按钮处理
//				carKeyClick(data);
				break;
			}

		}
	}
	
	public void sourceChanged(int source) {
		Log.v(TAG, "HMI------------sourceChanged source=" + source);
		if (Source.isBTMusicSource(source)) {
//			mIF.requestAudioFocus(true);

			if (SOURCE_BT != mCurSource) {
				int state = mIF.getConnState();
				int state1 = mBTIF.getConnState();
				Log.v(TAG, "HMI------------onServiceConn openActivityByAction ACTION_NAME_BT_MUSIC state="+state+"; state1="+state1);
				/*String fragmentName = "btMusic_intent";
				if (fragmentName != null && state1 == BTConnState.CONNECTED) {
					Log.v(TAG, "openMediaActivity source=" + source);
					PackageManager packageManager = mParent.getPackageManager();
					Intent intent = packageManager.getLaunchIntentForPackage("com.haoke.mediaservice");
					Bundle bundle = new Bundle();
					bundle.clear();
					bundle.putString("Mode_To_Music", fragmentName);
					intent.putExtras(bundle);
					mParent.startActivity(intent);
				}*/
			}
		}
		mCurSource = source;
	}

	@Override
	public void onBTDataChange(int mode, int func, int data) {
		if (Source.isBTMode(mode)) {
			switch (func) {
			case BTFunc.CONN_STATE:
				if (data == BTConnState.DISCONNECTED) {
					mBTIF.music_stop();
					if (mIF.getCurSource() == SOURCE_BT) {
						mIF.setCurSource(SOURCE_NULL);
						AllMediaList.notifyUpdateAppWidgetByBTMusic();
					} else if (Media_IF.sLastSource == SOURCE_BT) {
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
