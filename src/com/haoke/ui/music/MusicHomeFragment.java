package com.haoke.ui.music;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewStub;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.amd.bt.BTMusic_IF;
import com.amd.bt.BT_IF;
import com.amd.bt.BT_Listener;
import com.amd.util.Source;
import com.haoke.btjar.main.BTDef.BTConnState;
import com.haoke.btjar.main.BTDef.BTFunc;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.constant.MediaUtil.MediaFunc;
import com.haoke.constant.MediaUtil.MediaState;
import com.haoke.constant.MediaUtil.PlayState;
import com.haoke.mediaservice.R;
import com.haoke.ui.media.Media_Activity_Main;
import com.haoke.ui.widget.CustomDialog;
import com.haoke.ui.widget.CustomDialog.DIALOG_TYPE;
import com.haoke.util.DebugLog;
import com.haoke.util.Media_IF;
import com.haoke.util.Media_Listener;

public class MusicHomeFragment extends FrameLayout implements Media_Listener, BT_Listener {
	private static final String TAG = "MusicHomeFragment";
	private Context mContext;
	private Media_IF mIF = null;
	private BT_IF mBTIF = null;
	private BTMusic_IF mBTMusicIF = null;
	private MusicHomeLayout mHomeLayout;
	private MusicPlayLayout mPlayLayout;
	private ViewStub mPlayLayoutStub;
	private CustomDialog mDialog;
	protected static boolean isShow = false;
	private boolean mBtConnected =false;
	private int mErrorCount = 0;
	
	public MusicHomeFragment(Context context) {
    	super(context);
	}
    
    public MusicHomeFragment(Context context, AttributeSet attrs) {
    	super(context, attrs);
    }
    
    public MusicHomeFragment(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		DebugLog.d(TAG, "MusicHomeFragment init");
	}
    
    @Override
    protected void onFinishInflate() {
    	super.onFinishInflate();
		mContext = getContext();
		DebugLog.d(TAG, "onFinishInflate mContext="+mContext);
		mIF = Media_IF.getInstance();
		mIF.initMedia();
		mBTIF = BT_IF.getInstance();
		mBTIF.bindBTService();
		mBTMusicIF = BTMusic_IF.getInstance();
		mBtConnected = mBTIF.isBtMusicConnected();
    	mDialog = new CustomDialog();
		mHomeLayout = (MusicHomeLayout) findViewById(R.id.music_home_layout);
		mPlayLayoutStub = (ViewStub) findViewById(R.id.music_play_layout_stub);
		mPlayLayout = null;
		mIF.registerLocalCallBack(this);
		mBTIF.registerModeCallBack(this);
    }

	enum ShowLayout {
		HOME_LAYOUT,
		AUDIO_PLAY_LAYOUT,
		BT_PLAY_LAYOUT,
	}
	private ShowLayout mShowLayout = ShowLayout.HOME_LAYOUT;
	
	private void changeShowLayout(ShowLayout showLayout) {
	    DebugLog.d(TAG, "changeShowLayout showLayout="+showLayout+"; mShowLayout="+mShowLayout+"; mPlayLayout="+mPlayLayout);
		mShowLayout = showLayout;
		if (mShowLayout == ShowLayout.HOME_LAYOUT) {
			mHomeLayout.setVisibility(View.VISIBLE);
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
				mPlayLayout.refreshSkin(false);
			}
		}
		setCurPlayViewState();
	}
	
	public void onNewIntent(int source, boolean autoPlay) {
		if (source == Media_Activity_Main.MODE_BTMUSIC) {
			if (autoPlay) {
				mBTIF.music_play();
			}
		}
	}
	
	public void onStart() {
	}
	
	public void onStop() {
    }

	public void onResume() {
	    DebugLog.d(TAG, "onResume mShowLayout="+mShowLayout+"; isShow="+isShow);
		isShow = true;
		if (mPlayLayout != null) {
			if (mPlayLayout.getVisibility() == View.VISIBLE) {
				mPlayLayout.onResume();
				if (!mPlayLayout.isWillShowState()) {
					goHome();
				}
			} else {
				mPlayLayout.onPause();
			}
		}
		if (mHomeLayout.getVisibility() == View.VISIBLE) {
			mHomeLayout.onResume();
		} else {
			mHomeLayout.onPause();
		}
		setCurPlayViewState();
	}

	public void onPause() {
	    DebugLog.d(TAG, "onPause");
		isShow = false;
		if (mPlayLayout!=null) {
			mPlayLayout.onPause();
		}
		mHomeLayout.onPause();
	}
	
	public void onDestroy() {
		mIF.unregisterLocalCallBack(this);
		mBTIF.unregisterModeCallBack(this);
	}
	
	public boolean isBTMusicPlayFragment() {
		return mShowLayout == ShowLayout.BT_PLAY_LAYOUT;
	}
	
    public boolean isAudioPlayFragment() {
        return mShowLayout == ShowLayout.AUDIO_PLAY_LAYOUT;
    }
    
    public boolean isMusicHomeFragment() {
        return mShowLayout == ShowLayout.HOME_LAYOUT;
    }
	
	public void refreshSkin(boolean loading) {
		mHomeLayout.refreshSkin(loading);
		if (mPlayLayout != null) {
			mPlayLayout.refreshSkin(loading);
		}
		if (!loading) {
	        setCurPlayViewState();
		}
	}
	
	private void setCurPlayViewState() {
		if (mContext instanceof com.haoke.ui.media.Media_Activity_Main) {
			((com.haoke.ui.media.Media_Activity_Main)mContext).setCurPlayViewState();
		}
	}
	
	public void replaceBtMusicFragment() {
		changeShowLayout(ShowLayout.BT_PLAY_LAYOUT);
	}
	
	public void checkErrorDialog(Handler handler, boolean showErrorDialog) {
	    if (showErrorDialog) {
	        Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (mDialog != null) {
                        mDialog.ShowDialog(mContext, DIALOG_TYPE.NONE_BTN, R.string.media_play_nosupport);
                    }
                }
            };
            if (handler != null) {
                handler.postDelayed(runnable, 100);
            } else {
                runnable.run();
            }
	    } else {
	        if (mDialog != null) {
	            mDialog.CloseDialogEx();
            }
	    }
//	    int state = Media_IF.getInstance().getMediaState();
//	    if (state == MediaState.PREPARING
//	            || state == MediaState.PREPARED) {
//	        if (mDialog != null) {
//	            mDialog.CloseDialog();
//	        }
//	    }
	}

	@Override public void setCurInterface(int data) {}
	@Override
	public void onDataChange(int mode, int func, int data1, int data2) {
	    DebugLog.d(TAG, "onDataChange mode="+mode+"; func="+func+"; data1="+data1+"; data2="+data2);
		if (mode == mIF.getMode()) {
			switch (func) {
			case MediaFunc.DEVICE_CHANGED://8 data1=deviceType, data2=isExist ? 1 : 0
			    //modify bug 21124 begin
			    mErrorCount = 0;
			    //modify bug 21124 begin
				deviceChanged(data1, data2);
				break;
			case MediaFunc.SCAN_STATE://1
				onScanStateChanged(data1);
				break;
			case MediaFunc.PREPARING:
				onPreparing();
				break;
			case MediaFunc.PREPARED:
			    //modify bug 21124 begin
			    mErrorCount = 0;
			    //modify bug 21124 begin
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
			case MediaFunc.MEDIA_SCAN_MODE:
			    scanModeChanged(data1);
			    break;
			case MediaFunc.COLLECT_FILE:
			    collectFileChanged(data1);
			    break;
			case MediaFunc.UNCOLLECT_FILE:
			    uncollectFileChanged(data1);
			    break;
			default:
				break;
			}
		}
	}
	
	private void showDeviceOutDialog() {
		if (mShowLayout == ShowLayout.AUDIO_PLAY_LAYOUT) {
			if (isShow) {
				mDialog.ShowDialog(mContext, DIALOG_TYPE.NONE_BTN, R.string.music_device_pullout_usb);
			}
			changeShowLayout(ShowLayout.HOME_LAYOUT);
		}
	}
	
	private void deviceChanged(int deviceType, int state) {
		boolean flag1 = (mIF.getPlayingDevice() == deviceType);
		boolean flag2 = false;
		if (mPlayLayout != null && mIF.getPlayingDevice() == DeviceType.COLLECT && mPlayLayout.getFileNode() != null) {
			if (mPlayLayout.getFileNode().getFromDeviceType() == deviceType) {
				flag2 = true;
			}
		}
		
		if (flag1 || flag2) {
			if (state == 0) { // 无设备
				showDeviceOutDialog();
			} 
		}
		mHomeLayout.deviceChanged(deviceType, state == 0);
		setCurPlayViewState();
	}
	
	private void onScanStateChanged(int data) {
	    if (data == 1) {
	        DebugLog.v(TAG, "onScanStateChanged() scanning!");
	    } else if (data == 8) {
	        DebugLog.v(TAG, "onScanStateChanged() scanning all over!");
	        postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Source.isAudioSource(Media_IF.getCurSource()) &&
                            mIF.getPlayState() != PlayState.STOP &&
                            mIF.getMediaState() == MediaState.PREPARED && isShow) {
                        changeShowLayout(ShowLayout.AUDIO_PLAY_LAYOUT);
                    }
                }
            }, 1000);
	    }
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
	    if (mContext != null && mContext instanceof com.haoke.ui.media.Media_Activity_Main) {
            boolean resumed = ((com.haoke.ui.media.Media_Activity_Main)mContext).getActResumed();
            if (!resumed) {
                DebugLog.e(TAG, "onError but activity is not resumed! return!");
                return;
            }
        }
	    //modify bug 21124 begin
	    mErrorCount++;
	    //modify bug 21124 end
		mDialog.ShowDialog(mContext, DIALOG_TYPE.NONE_BTN, R.string.media_play_nosupport);
		if (mPlayLayout != null) {
			mPlayLayout.onError();
			//modify bug 21124 begin
			int totalSize = mPlayLayout.getTotalSize();
			DebugLog.v(TAG, "totalSize ="+ totalSize+",mErrorCount ="+ mErrorCount);
			if (totalSize <= mErrorCount/* || mErrorCount >= 5*/) {
			    mErrorCount = 0;
	            goHome();
	        }
			//modify bug 21124 end
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
		if (state == PlayState.STOP) {
			goHome();
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
	
	private void scanModeChanged(int data) {
	    if (mPlayLayout != null && mPlayLayout.getVisibility() == View.VISIBLE) {
            mPlayLayout.updateScanMode(data);
        }
	}
	
	private void collectFileChanged(int data) {
	    if (mPlayLayout != null && mPlayLayout.getVisibility() == View.VISIBLE) {
            mPlayLayout.collectFileChanged(data);
        }
	}

    private void uncollectFileChanged(int data) {
        if (mPlayLayout != null && mPlayLayout.getVisibility() == View.VISIBLE) {
            mPlayLayout.uncollectFileChanged(data);
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
		int curSource = Media_IF.getCurSource();
		// 更新界面状态
		if (Source.isAudioSource(curSource) && playState != PlayState.STOP) {
			changeShowLayout(ShowLayout.AUDIO_PLAY_LAYOUT);
//		} else if (Source.isBTMusicSource(curSource) && btPlaying) {
			//modify bug 20830 begin
        } else if (Source.isBTMusicSource(curSource)) {
		    //modify bug 20830 end
			changeShowLayout(ShowLayout.BT_PLAY_LAYOUT);
		} else {
		    DebugLog.e(TAG, "goPlay no song playing!");
			if (toast) {
				Toast.makeText(mContext, R.string.no_song_is_playing, Toast.LENGTH_SHORT).show();
			}
			if (noPlayGoHome) {
				goHome();
			}
		}
	}

	@Override
	public void onBTDataChange(int mode, int func, int data) {
	    DebugLog.d(TAG, "onBTDataChange mode="+mode+"; func="+func+"; data="+data);
		if (Source.isBTMode(mode)) {
			switch (func) {
			case BTFunc.CONN_STATE://101
				//onBTStateChange(data);
			    mHomeLayout.setBTConnectedState(data);
				break;
				
			//case BTFunc.MUSIC_A2DP_STATE:  //404
			case BTFunc.MUSIC_AVRCP_STATE:  //405
			    onBTStateChange(data);
			    break;
			    
			case BTFunc.TALK_AUDIO://202
				break;
				
			case BTFunc.MUSIC_PLAY_STATE: //400
				setCurPlayViewState();
				if (mPlayLayout != null) {
					mPlayLayout.updateCtrlBar();
					mPlayLayout.refreshFromViewPagerMaybePlayBT(isShow, true);
				}
				break;
			case BTFunc.MUSIC_ID3_UPDATE://401
				setCurPlayViewState();
				if (mPlayLayout != null) {
					mPlayLayout.updateId3Info();
					mPlayLayout.refreshFromViewPagerMaybePlayBT(isShow, true);
				}
				break;
			}
		}
	}

	private void onBTStateChange(int data) {//蓝牙连接状态
	    DebugLog.d(TAG, "onBTStateChange data="+data);
		mHomeLayout.setBTConnectedState(data);
		if (data == BTConnState.DISCONNECTED) {
			if (mShowLayout == ShowLayout.BT_PLAY_LAYOUT) {
				changeShowLayout(ShowLayout.HOME_LAYOUT);
			}
			if (mBtConnected) {
				showBTDialog();
			}
			mBtConnected = false;
		} else {
			if (data == BTConnState.CONNECTED) {
				mBtConnected = true;
			}
			if (!Source.isBTMusicSource()) {
				
			} else if (data == BTConnState.CONNECTED) {
				if (mShowLayout != ShowLayout.BT_PLAY_LAYOUT) {
					changeShowLayout(ShowLayout.BT_PLAY_LAYOUT);
				}
			}
		}
	}
	
	private void showBTDialog() {
		CustomDialog dialog = new CustomDialog();
		dialog.ShowDialog(mContext, DIALOG_TYPE.NONE_BTN, R.string.btmusic_device_bt_error);
	}
}
