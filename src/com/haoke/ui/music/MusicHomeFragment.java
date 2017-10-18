package com.haoke.ui.music;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Toast;

import com.amd.bt.BTMusic_IF;
import com.amd.bt.BT_IF;
import com.amd.bt.BT_Listener;
import com.haoke.btjar.main.BTDef.BTConnState;
import com.haoke.btjar.main.BTDef.BTFunc;
import com.haoke.define.GlobalDef;
import com.haoke.define.ModeDef;
import com.haoke.define.MediaDef.MediaFunc;
import com.haoke.define.MediaDef.MediaState;
import com.haoke.define.MediaDef.PlayState;
import com.haoke.mediaservice.R;
import com.haoke.ui.media.Media_Activity_Main;
import com.haoke.ui.widget.CustomDialog;
import com.haoke.ui.widget.CustomDialog.DIALOG_TYPE;
import com.haoke.util.Media_IF;
import com.haoke.util.Media_Listener;

public class MusicHomeFragment extends Fragment implements Media_Listener, BT_Listener {
	private static final String TAG = "MusicHomeFragment";
	private Context mContext;
	private Media_IF mIF = null;
	private BT_IF mBTIF = null;
	private BTMusic_IF mBTMusicIF = null;
	private MusicHomeLayout mHomeLayout;
	private MusicPlayLayout mPlayLayout;
	private ViewStub mPlayLayoutStub;
	private CustomDialog mDialog;
	private boolean mRefreshLayout = false;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "onCreateView");
		mContext = getActivity();
		mIF = Media_IF.getInstance();
		mIF.initMedia();
		
		mBTIF = BT_IF.getInstance();
		mBTIF.bindBTService();
		mBTMusicIF = BTMusic_IF.getInstance();
		
		View rootView = inflater.inflate(R.layout.music_activity_home, container, false);
		mDialog = new CustomDialog();
		mHomeLayout = (MusicHomeLayout) rootView.findViewById(R.id.music_home_layout);
		mPlayLayoutStub = (ViewStub) rootView.findViewById(R.id.music_play_layout_stub);
		mPlayLayout = null;
		return rootView;
	}

	private void setMusicModeFragment() {
		String md = null;
		if (getActivity() != null && getActivity().getIntent() != null) {
			md = (String) getActivity().getIntent().getSerializableExtra("Mode_To_Music");
		}
		Log.d(TAG, "setMusicModeFragment md="+md);
		if ("btMusic_intent".equals(md)) {
			GlobalDef.currentsource = 4;
			changeShowLayout(ShowLayout.BT_PLAY_LAYOUT);
			getActivity().setIntent(null);
			return;
		}
		
		int playState = mIF.getPlayState();
		int source = mIF.getCurSource();
		// 更新界面状态
		if (source == ModeDef.AUDIO && playState != PlayState.STOP) {
			changeShowLayout(ShowLayout.AUDIO_PLAY_LAYOUT);
		} else if (source == ModeDef.BT && mBTIF.music_isPlaying()) {
			changeShowLayout(ShowLayout.BT_PLAY_LAYOUT);
		} else {
			changeShowLayout(ShowLayout.HOME_LAYOUT);
		}
	}
	
	enum ShowLayout {
		HOME_LAYOUT,
		AUDIO_PLAY_LAYOUT,
		BT_PLAY_LAYOUT,
	}
	private ShowLayout mShowLayout = ShowLayout.HOME_LAYOUT;
	
	private void changeShowLayout(ShowLayout showLayout) {
		Log.d(TAG, "changeShowLayout showLayout="+showLayout+"; mShowLayout="+mShowLayout+"; mPlayLayout="+mPlayLayout);
		mShowLayout = showLayout;
		mRefreshLayout = false;
		if (mHomeLayout == null) {
			mRefreshLayout = true;
		} else if (mShowLayout == ShowLayout.HOME_LAYOUT) {
			mHomeLayout.setVisibility(View.VISIBLE);
			updateSystemUILabel(ModeDef.AUDIO, false);
			if (mPlayLayout!=null) mPlayLayout.setVisibility(View.GONE);
		} else {
			mHomeLayout.setVisibility(View.GONE);
			if (mPlayLayout!=null) {
				mPlayLayout.setBTPlayMode(mShowLayout == ShowLayout.BT_PLAY_LAYOUT);
				mPlayLayout.setVisibility(View.VISIBLE);
			} else {
				mPlayLayout = (MusicPlayLayout) mPlayLayoutStub.inflate();
				mPlayLayout.setBTPlayMode(mShowLayout == ShowLayout.BT_PLAY_LAYOUT);
				mPlayLayout.setVisibility(View.VISIBLE);
			}
			updateSystemUILabel(mPlayLayout.isBTPlay() ? ModeDef.BT : ModeDef.AUDIO, false);
		}
		setCurPlayViewState();
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
		mIF.registerLocalCallBack(this);
		mBTIF.registerModeCallBack(this);
		//setMusicModeFragment();
		if (mRefreshLayout) {
			changeShowLayout(mShowLayout);
		}
		if (getUserVisibleHint()) {
			int source = ModeDef.AUDIO;
			if (mShowLayout == ShowLayout.BT_PLAY_LAYOUT) {
				source = ModeDef.BT;
			}
			updateSystemUILabel(source, true);
		}
		mHomeLayout.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.d(TAG, "onPause");
		mIF.unregisterLocalCallBack(this);
		mBTIF.unregisterModeCallBack(this);
		if (mPlayLayout!=null) {
			mPlayLayout.onPause();
		}
	}
	
	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);
		if (mPlayLayout!=null) {
			mPlayLayout.setUserVisibleHint(isVisibleToUser);
		}
		if (isVisibleToUser) {
			int source = ModeDef.AUDIO;
			if (mPlayLayout != null && mPlayLayout.getVisibility() == View.VISIBLE) {
				if (mPlayLayout.isBTPlay()) {
					source = ModeDef.BT;
				}
			}
			updateSystemUILabel(source, true);
		}
	}
	
	private void updateSystemUILabel(int curLabel, boolean force) {
		Activity activity = getActivity();
        if (activity != null && activity instanceof Media_Activity_Main) {
        	((Media_Activity_Main)activity).updateSystemUILabel(curLabel, force);
        }
	}

	private void stopBtMusic() {
		if (mBTIF.music_isPlaying()) {
			mBTIF.music_stop();
		}
	}
	
	public boolean isBTMusicPlayFragment() {
		return mShowLayout == ShowLayout.BT_PLAY_LAYOUT;
	}
	
	private void setCurPlayViewState() {
		boolean isAudioMusicPlayFragment = mShowLayout == ShowLayout.AUDIO_PLAY_LAYOUT;
		boolean isBtMusicPlayFragment = mShowLayout == ShowLayout.BT_PLAY_LAYOUT;
		boolean isHomeFragment = mShowLayout == ShowLayout.HOME_LAYOUT;
		int source = mIF.getCurSource();
		boolean isAudioMusicPlay = (source == ModeDef.AUDIO && mIF.getPlayState() == PlayState.PLAY);
		boolean isBTMusicPlay = (source == ModeDef.BT && mBTIF.music_isPlaying());
		Activity activity = getActivity();
		if (activity instanceof com.haoke.ui.media.Media_Activity_Main) {
			((com.haoke.ui.media.Media_Activity_Main)activity).setCurPlayViewState(
					isHomeFragment, isAudioMusicPlayFragment, isBtMusicPlayFragment,
					isAudioMusicPlay, isBTMusicPlay);
		}
	}
	
	public void replaceBtMusicFragment() {
		changeShowLayout(ShowLayout.BT_PLAY_LAYOUT);
	}

	@Override public void setCurInterface(int data) {}
	@Override
	public void onDataChange(int mode, int func, int data1, int data2) {
		Log.d(TAG, "onDataChange mode="+mode+"; func="+func+"; data1="+data1+"; data2="+data2);
		if (mode == mIF.getMode()) {
			switch (func) {
			case MediaFunc.DEVICE_CHANGED://8 data1=deviceType, data2=isExist ? 1 : 0
				deviceChanged(data1, data2);
				break;
			case MediaFunc.SCAN_STATE://1
				onDeviceOut();
				break;
			case MediaFunc.PREPARING:
				onPreparing();
				break;
			case MediaFunc.PREPARED:
				onPrepared(); // play-101
				break;
			case MediaFunc.ERROR://104
				onError();
				break;
			case MediaFunc.COMPLETION:
				onCompletion();
				break;
			case MediaFunc.PLAY_STATE:
				playStateChanged(data1); // play-106
				break;
			case MediaFunc.REPEAT_MODE:
				repeatModeChanged(data1); // play-107
				break;
			case MediaFunc.RANDOM_MODE:
				randomModeChanged(data1); // play-108
				break;
			default:
				break;
			}
		}
	}
	
	private void showDeviceOutDialog() {
		if (mShowLayout == ShowLayout.AUDIO_PLAY_LAYOUT || mIF.getPlayState() != PlayState.STOP) {
			mDialog.ShowDialog(mContext, DIALOG_TYPE.NONE_BTN, R.string.music_device_pullout_usb);
		}
	}
	
	private void deviceChanged(int deviceType, int state) {
		if (mIF.getPlayingDevice() == deviceType) {
			if (state == 0) { // 无设备
				showDeviceOutDialog();
				changeShowLayout(ShowLayout.HOME_LAYOUT);
			} 
		}
		mHomeLayout.deviceChanged(deviceType, state == 0);
	}
	
	private void onDeviceOut() {
		Log.v(TAG, "onDeviceOut() playState= " + mIF.getPlayState());
	}
	
	private void onPreparing() {
		if (mPlayLayout != null && mPlayLayout.getVisibility() == View.VISIBLE) {
			mPlayLayout.quickRefreshId3Info();
		}
	}

	private void onPrepared() {
		// 快速切换曲时，在收到onPrepared()后，mediaState可能已经不是PREPARED状态，需要过滤不处理
		int mediaState = mIF.getMediaState();
		if (mediaState != MediaState.PREPARED) {
			return;
		}
		if (mPlayLayout != null && mPlayLayout.getVisibility() == View.VISIBLE) {
			mPlayLayout.updateId3Info();
			mPlayLayout.updateCtrlBar();
			mPlayLayout.updateTimeBar();
		}
		setCurPlayViewState();
	}
	
	private void onError() {
		mDialog.ShowDialog(mContext, DIALOG_TYPE.NONE_BTN, R.string.media_play_nosupport);
		if (mPlayLayout != null) {
			mPlayLayout.onError();
		}
	}
	
	private void onCompletion() {
		if (mPlayLayout != null) {
			mPlayLayout.onCompletion();
		}
	}
	
	private void playStateChanged(int state) {
		if (mPlayLayout != null && mPlayLayout.getVisibility() == View.VISIBLE) {
			mPlayLayout.updateCtrlBar();
		}
		setCurPlayViewState();
	}

	private void repeatModeChanged(int mode) {
		if (mPlayLayout != null && mPlayLayout.getVisibility() == View.VISIBLE) {
			mPlayLayout.updateRepeatMode();
		}
	}

	private void randomModeChanged(int mode) {
		if (mPlayLayout != null && mPlayLayout.getVisibility() == View.VISIBLE) {
			mPlayLayout.updateRepeatMode();
		}
	}

	// 是否在播放界面
	public boolean isPlayFragment() {
		if (mContext == null) {
			return false;
		}
		return mPlayLayout != null && mPlayLayout.getVisibility() == View.VISIBLE;
	}
	
	// 返回主页
	public void goHome() {
		changeShowLayout(ShowLayout.HOME_LAYOUT);
	}
	
	// 跳转到播放界面
	public void goPlay(boolean toast, boolean noPlayGoHome) {
		int playState = mIF.getPlayState();
		boolean btPlaying = mBTIF.music_isPlaying();
		int curSource = mIF.getCurSource();
		// 更新界面状态
		if (curSource == ModeDef.AUDIO && playState != PlayState.STOP) {
			changeShowLayout(ShowLayout.AUDIO_PLAY_LAYOUT);
		} else if (curSource == ModeDef.BT && btPlaying) {
			changeShowLayout(ShowLayout.BT_PLAY_LAYOUT);
		} else {
			Log.e(TAG, "goPlay no song playing!");
			if (toast) {
				Toast.makeText(mContext, "没有歌曲在播放！", Toast.LENGTH_SHORT).show();
			}
			if (noPlayGoHome) {
				goHome();
			}
		}
	}

	@Override
	public void onBTDataChange(int mode, int func, int data) {
		Log.d(TAG, "onBTDataChange mode="+mode+"; func="+func+"; data="+data);
		if (mode == ModeDef.BT) {
			switch (func) {
			case BTFunc.CONN_STATE://101
				onBTStateChange(data);
				break;
				
			case BTFunc.TALK_AUDIO://202
				break;
				
			case BTFunc.MUSIC_PLAY_STATE: //400
				setCurPlayViewState();
				if (mPlayLayout != null) {
					mPlayLayout.updateCtrlBar();
					mPlayLayout.refreshFromViewPagerMaybePlayBT(getUserVisibleHint(), true);
				}
				break;
			case BTFunc.MUSIC_ID3_UPDATE://401
				setCurPlayViewState();
				if (mPlayLayout != null) {
					mPlayLayout.updateId3Info();
					mPlayLayout.refreshFromViewPagerMaybePlayBT(getUserVisibleHint(), true);
				}
				break;
			}
		}
	}

	private void onBTStateChange(int data) {//蓝牙连接状态
		Log.d(TAG, "onBTStateChange data="+data);
		mHomeLayout.setBTConnectedState(data);
		if (data == BTConnState.DISCONNECTED) {
			if (mShowLayout != ShowLayout.HOME_LAYOUT) {
				changeShowLayout(ShowLayout.HOME_LAYOUT);
			}
			showBTDialog();
		} else if (mIF.getCurSource() != ModeDef.BT) {
			
		} else if (data == BTConnState.CONNECTED) {
			if (mShowLayout != ShowLayout.BT_PLAY_LAYOUT) {
				changeShowLayout(ShowLayout.BT_PLAY_LAYOUT);
			}
		}
	}
	
	private void showBTDialog() {
		CustomDialog dialog = new CustomDialog();
		dialog.ShowDialog(mContext, DIALOG_TYPE.NONE_BTN, R.string.btmusic_device_bt_error);
	}
}
