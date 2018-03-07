package com.haoke.ui.music;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.amd.bt.BTMusic_IF;
import com.amd.bt.BT_IF;
import com.amd.media.MediaInterfaceUtil;
import com.amd.util.Source;
import com.amd.util.SkinManager;
import com.haoke.bean.FileNode;
import com.haoke.btjar.main.BTDef.BTConnState;
import com.haoke.constant.MediaUtil;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.data.AllMediaList;
import com.haoke.data.ModeSwitch;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.constant.MediaUtil.OperateState;
import com.haoke.constant.MediaUtil.PlayState;
import com.haoke.constant.MediaUtil.RepeatMode;
import com.haoke.mediaservice.R;
import com.haoke.util.Media_IF;

public class MusicPlayLayout extends RelativeLayout implements OnClickListener {
	private static final String TAG = "MusicPlayLayout";
	private Music_Play_Id3 mId3;
	private SeekBar mTimeSeekBar;
	private TextView mCurrTime;
	private TextView mTotalTime;
	private ImageView mListImg = null;
	private ImageView mModeImg = null;
	private ImageView mSavedImg = null;
	private ImageView mScanImg = null;
	private View mPlayTimeLayout;
	
	private ImageView mBtnPP;
	private ImageView mBtnPre;
	private ImageView mBtnNext;
	
	private int mTextFormat = 1; //1为分秒，2为时分秒
	
	private Media_IF mIF = Media_IF.getInstance();
	
	private SkinManager sSkinManager;
	private Drawable mListImgDrawable;
	private Drawable mBtnPreDrawable;
	private Drawable mBtnNextDrawable;
	private Drawable mTimeSeekBarThumbDrawable;
	private Drawable mTimeSeekBarProgressDrawable;
	
	private FileNode mFileNode;
	public FileNode getFileNode() {
		return mFileNode;
	}
	
	public boolean isWillShowState() {
		if (isBTPlay) {
			if(BT_IF.getInstance().getConnState() != BTConnState.CONNECTED) {
				return false;
			}
		} else {
			if (getFileNode() == null || !getFileNode().isExist(getContext())) {
				return false;
			}
		}
		return true;
	}
	
	private boolean isBTPlay = false;
	
	public boolean isBTPlay() {
		return isBTPlay;
	}
	
	public void setBTPlayMode(boolean btModeFlag) {
		Log.d(TAG, "setBTPlayMode btModeFlag="+btModeFlag);
		if (btModeFlag != isBTPlay) {
	        isBTPlay = btModeFlag;
		    AllMediaList.notifyAllLabelChange(getContext(), 
	                isBTPlay ? R.string.pub_btmusic :R.string.pub_music);
		}
		mId3.setBTPlayMode(btModeFlag);
		if (isBTPlay) {
			ModeSwitch.instance().setCurrentMode(getContext(), true, ModeSwitch.MUSIC_BT_MODE);
		} else {
		    int deviceType = mIF.getPlayingDevice();
		    int fileType = mIF.getPlayingFileType();
		    if (deviceType != DeviceType.NULL &&
		            mIF.getMediaDevice() != deviceType &&
		            mIF.getPlayState() != PlayState.STOP) {
	            mIF.setCurScanner(deviceType, fileType);
		    }
		}
		updateId3Info();
		updateCtrlBar();
		updateTimeBar();
		updateRepeatMode();
		updateCollectIcon();
		mListImg.setVisibility(isBTPlay ? View.GONE : View.VISIBLE);
		mScanImg.setVisibility(isBTPlay ? View.GONE : View.VISIBLE);
		if (isBTPlay) {
			mTimeHandler.removeCallbacksAndMessages(null);
			exitScanMode();
		}
	}
	
	public MusicPlayLayout(Context context) {
		super(context);
	}
	
	public MusicPlayLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public MusicPlayLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Log.d(TAG, "onFinishInflate");
		
		mId3 = (Music_Play_Id3) findViewById(R.id.music_play_id3);
		mTimeSeekBar = (SeekBar) findViewById(R.id.music_play_time_seekbar);
		mCurrTime = (TextView) findViewById(R.id.music_play_time_current);
		mTotalTime = (TextView) findViewById(R.id.music_play_time_total);
		mTimeSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
		mPlayTimeLayout = findViewById(R.id.music_play_time);
		
		mListImg = (ImageView) findViewById(R.id.music_play_lists);
		mModeImg = (ImageView) findViewById(R.id.music_play_mode);
		mSavedImg = (ImageView) findViewById(R.id.music_save_status);
		mScanImg = (ImageView) findViewById(R.id.music_scan);
		mListImg.setOnClickListener(this);
		mModeImg.setOnClickListener(this);
		mSavedImg.setOnClickListener(this);
		mScanImg.setOnClickListener(this);
		
		mBtnPP = (ImageView) findViewById(R.id.media_ctrlbar_btn);
        mBtnPP.setOnClickListener(this);
        mBtnPre = (ImageView) this.findViewById(R.id.media_ctrlbar_pre);
        mBtnPre.setOnClickListener(this);
        mBtnNext = (ImageView) this.findViewById(R.id.media_ctrlbar_next);
        mBtnNext.setOnClickListener(this);
		
		sSkinManager = SkinManager.instance(getContext());
	}
	
	public void refreshSkin(boolean loading) {
		if (sSkinManager == null) {
			sSkinManager = SkinManager.instance(getContext());
		}
		if (loading || mListImgDrawable==null) {
	        mListImgDrawable = sSkinManager.getDrawable(R.drawable.all);
	        mBtnPreDrawable = sSkinManager.getDrawable(R.drawable.pre);
	        mBtnNextDrawable = sSkinManager.getDrawable(R.drawable.next);
	        mTimeSeekBarThumbDrawable = sSkinManager.getDrawable(R.drawable.media_seekbar_block_g11);
	        mTimeSeekBarProgressDrawable = sSkinManager.getProgressDrawable(R.drawable.play_time_seekbar_progress);
		}
		if (!loading) {
	        mListImg.setImageDrawable(mListImgDrawable);
	        mBtnPre.setImageDrawable(mBtnPreDrawable);
	        mBtnNext.setImageDrawable(mBtnNextDrawable);
	        updateCtrlBar(); // 更新播放按钮。
	        updateRepeatMode();
	        if (mIF.getScanMode()) {
	            mScanImg.setImageDrawable(sSkinManager.getDrawable(R.drawable.radio_scan_5s_normal));
	        } else {
	            mScanImg.setImageDrawable(sSkinManager.getDrawable(R.drawable.radio_scan_5s_pressed));
	        }
	        mId3.refreshSkin(sSkinManager);
	        mTimeSeekBar.setThumb(mTimeSeekBarThumbDrawable);
	        mTimeSeekBar.setProgressDrawable(mTimeSeekBarProgressDrawable);
		}
	}
	
	private void updateCollectIcon() {
		if (isBTPlay) {
			mSavedImg.setVisibility(View.GONE);
			return;
		}
		if (mIF.getPlayingDevice() == DeviceType.COLLECT) {
			mSavedImg.setVisibility(View.GONE);
		} else {
			mSavedImg.setVisibility(View.VISIBLE);
			boolean isCollected = false;
			int pos = mIF.getPlayPos();
			isCollected = mIF.isCollected(pos);
			if (isCollected) {
				mSavedImg.setImageDrawable(sSkinManager.getDrawable(R.drawable.media_collect));
			} else {
				mSavedImg.setImageDrawable(sSkinManager.getDrawable(R.drawable.media_uncollect));
			}	
		}
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
	}
	
	@Override
	protected void onDetachedFromWindow() {
		Log.d(TAG, "onDetachedFromWindow");
		mTimeHandler.removeCallbacksAndMessages(null);
		super.onDetachedFromWindow();
	}
	
    public void onPause() {
    	Log.d(TAG, "onPause isBTPlay="+isBTPlay);
    	exitScanMode();
    	mTimeHandler.removeCallbacksAndMessages(null);
    }
    
	public void onResume() {
		Log.d(TAG, "onResume isBTPlay="+isBTPlay);
		AllMediaList.notifyAllLabelChange(getContext(), 
				isBTPlay ? R.string.pub_btmusic :R.string.pub_music);
		updateCtrlBar();
		updateTimeBar();
		
		refreshFromViewPagerMaybePlayBT(true, true);
    	if (mIF == null || isBTPlay) {
    		return;
    	}
    	mTimeHandler.removeMessages(MSG_UPDATE_TIME);
    	if (getVisibility() == View.VISIBLE) {
    		if (mIF.getPlayState() == PlayState.PLAY) {
				mTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, 200);
			}
    	} else {
    		onPause();
    	}
    	if (mIF.getPlayItem() != null) {
    		mFileNode = mIF.getPlayItem();
    	}
    	//refreshSkin();
	}
	
	@Override
	public void setVisibility(int visibility) {
    	int getVisibility = getVisibility();
    	Log.d(TAG, "setVisibility getVisibility="+getVisibility+"; visibility="+visibility);
		super.setVisibility(visibility);
		if (getVisibility != visibility) {
			if (visibility != View.VISIBLE) {
				onPause();
			} else if (MusicHomeFragment.isShow) {
				onResume();
			}
		}
	}
	
	@Override
	public void onClick(View v) {
		int id = v.getId();
		Log.d(TAG, "onClick id="+id);
        if (MediaInterfaceUtil.isButtonClickTooFast()) {
            return;
        }
		switch (id) {
		case R.id.music_play_lists:
			startListActivity(mIF.getPlayingDevice());
			break;
			
		case R.id.music_play_mode:
			musicPlayMode();
			break;
			
		case R.id.music_save_status:
			final int pos = mIF.getPlayPos();
			boolean isCollect = mIF.isCollected(pos);
			if (isCollect) {
				mIF.deleteCollectedMusic(pos);
				mSavedImg.setImageDrawable(sSkinManager.getDrawable(R.drawable.media_uncollect));
			} else {
				if (AllMediaList.instance(getContext()).getCollectSize(FileType.AUDIO) < MediaUtil.COLLECT_COUNT_MAX) {
            		mIF.collectMusic(pos);
    				mSavedImg.setImageDrawable(sSkinManager.getDrawable(R.drawable.media_collect));
            	} else {
					new AlertDialog.Builder(getContext()).setTitle(R.string.collect_limit_dialog_title)
            		.setMessage(R.string.collect_limit_dialog_message)
            		.setPositiveButton(R.string.collect_limit_dialog_ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							AllMediaList.instance(getContext()).deleteOldCollect(FileType.AUDIO);
							mIF.collectMusic(pos);
							mSavedImg.setImageDrawable(sSkinManager.getDrawable(R.drawable.media_collect));
						}
					})
					.setNegativeButton(R.string.collect_limit_dialog_cancel, null)
					.show();
            	}
			}	
			break;
			
		case R.id.music_scan:
			boolean enable = mIF.getScanMode();
			if (enable) {
				exitScanMode();
			} else {
				enterScanMode();
			}
			break;

		case R.id.media_ctrlbar_btn:
			if (isBTPlay) {
				if (BT_IF.getInstance().music_isPlaying()) {
					BT_IF.getInstance().music_pause();
//					BT_IF.getInstance().setRecordPlayState(PlayState.PAUSE);
				} else {
					BT_IF.getInstance().music_play();
//					BT_IF.getInstance().setRecordPlayState(PlayState.PLAY);
				}
			} else {
				exitScanMode();
				mIF.changePlayState();
			}
			break;
		case R.id.media_ctrlbar_pre:
			if (isBTPlay) {
				BT_IF.getInstance().music_pre();
			} else {
				exitScanMode();
				mIF.playPre();
			}
			break;
		case R.id.media_ctrlbar_next:
			if (isBTPlay) {
				BT_IF.getInstance().music_next();
			} else {
				exitScanMode();
				mIF.playNext();
			}
			break;
		}
	}
	
	private void startListActivity(int type) {
		Intent intent = new Intent();
		mIF.setAudioDevice(type);
		intent.setClass(getContext(), Music_Activity_List.class);
        String value = null;
        if (type == DeviceType.COLLECT) {
            value = "COLLECT_intent";
        } else if (type == DeviceType.FLASH) {
            value = "hddAudio_intent";
        } else if (type == DeviceType.USB1) {
            value = "USB1_intent";
        } else if (type == DeviceType.USB2) {
            value = "USB2_intent";
        } else {
            return;
        }
        intent.putExtra("Mode_To_Music", value);
		getContext().startActivity(intent);
	}
	
	private void musicPlayMode() {
		int repeat = mIF.getRepeatMode();
	    if (repeat == RepeatMode.RANDOM || repeat == RepeatMode.OFF) {
	    	mIF.setRepeatMode(RepeatMode.ONE);
	    	mModeImg.setImageResource(R.drawable.music_play_single);
	    } else if (repeat == RepeatMode.ONE) {
	    	mIF.setRepeatMode(RepeatMode.CIRCLE);
	    	mModeImg.setImageResource(R.drawable.music_play_cycle);
	    } else if (repeat == RepeatMode.CIRCLE) {
	    	mIF.setRepeatMode(RepeatMode.RANDOM);
	    	mModeImg.setImageResource(R.drawable.music_play_nomal);
	    }
	}
	
	public void updateId3Info() {
		mId3.updateId3Info();
	}
	
	public void quickRefreshId3Info() {
		mId3.quickRefreshId3Info();
	}
	
	public void updateCtrlBar() {
		mTimeHandler.removeMessages(MSG_UPDATE_TIME);
		boolean playState = false;
		if (isBTPlay) {
			playState = BT_IF.getInstance().music_isPlaying();
		} else {
			playState = Media_IF.getInstance().getPlayState() == PlayState.PLAY;
			updateCollectIcon();
			if (playState) {
				mTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, 200);
			}
		}
		mBtnPP.setImageDrawable(sSkinManager.getDrawable(playState ? R.drawable.pause : R.drawable.play));
	}
	
	public void updateRepeatMode() {
		mModeImg.setVisibility(isBTPlay ? View.GONE : View.VISIBLE);
		if (isBTPlay) {
			return;
		}
		int repeat = mIF.getRepeatMode();
	    if (repeat == RepeatMode.RANDOM) {
	    	mModeImg.setImageDrawable(sSkinManager.getDrawable(R.drawable.music_play_nomal));
	    } else if (repeat == RepeatMode.ONE) {
	    	mModeImg.setImageDrawable(sSkinManager.getDrawable(R.drawable.music_play_single));
	    } else if (repeat == RepeatMode.CIRCLE) {
	    	mModeImg.setImageDrawable(sSkinManager.getDrawable(R.drawable.music_play_cycle));
	    }
	}
	
	public void updateTimeBar() {
		mPlayTimeLayout.setVisibility(isBTPlay ? View.GONE : View.VISIBLE);
		if (isBTPlay) {
			return;
		}
		int max = mIF.getDuration();
		int curr = mIF.getPosition();
		mTimeSeekBar.setMax(max);
		mTimeSeekBar.setProgress(curr);
		setTotalTime(max);
		setCurrTime(curr);
	}
	
	private static final int SCAN_CHANGE_TIME = 10;
	private static final int MSG_UPDATE_TIME = 1;
	private static final int MSG_SEEKBAR_CHANGE = 2;
	private static final int MSG_SCAN_MUSIC_CHANGE = 3;
	private static final int MSG_MAYBE_PLAY_BT = 4;
	private Handler mTimeHandler = new Handler() {
		public void handleMessage(Message msg) {
			int what = msg.what;
			Log.d(TAG, "mTimeHandler what="+what);
			removeMessages(what);
			switch (msg.what) {
			case MSG_UPDATE_TIME:
				mFileNode = mIF.getPlayItem();
				int curr = mIF.getPosition();
				Log.d(TAG, "mTimeHandler MSG_UPDATE_TIME curr="+curr);
				mTimeSeekBar.setProgress(curr);
				setCurrTime(curr);
				sendEmptyMessageDelayed(MSG_UPDATE_TIME, 200);
				if (mIF.getScanMode() && curr >= SCAN_CHANGE_TIME) {
					sendEmptyMessage(MSG_SCAN_MUSIC_CHANGE);
				}
				break;
			case MSG_SEEKBAR_CHANGE:
				int progress = mTimeSeekBar.getProgress();
				mIF.setPosition(progress);
				setCurrTime(progress);
				break;
			case MSG_SCAN_MUSIC_CHANGE:
				checkScanModeAndGoOn();
				break;
			case MSG_MAYBE_PLAY_BT:
				refreshFromViewPagerMaybePlayBTEx(msg.arg1 == 1 ? true : false);
				break;
			}
		};
	};
	
	private OnSeekBarChangeListener mSeekBarChangeListener = new OnSeekBarChangeListener() {
		
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			mTimeHandler.sendEmptyMessage(MSG_SEEKBAR_CHANGE);
			if (mIF.getPlayState() == PlayState.PLAY) {
				mTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
			}
		}
		
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			mTimeHandler.removeMessages(MSG_UPDATE_TIME);
			exitScanMode();
		}
		
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			setCurrTime(progress);
		}
	};
	
	private void setTotalTime(int total) {
		String string = "";
		if (total > 3600) {
			mTextFormat = 2;
		} else {
			mTextFormat = 1;
		}
		if (mTextFormat == 1) {
			string = TimeFormat(total);
        } else if (mTextFormat == 2) {
        	string = TimeFormat_HMS(total);
        }
		mTotalTime.setText(string);
	}
	
	private void setCurrTime(int time) {
		String string = "";
		if (mTextFormat == 1) {
			string = TimeFormat(time);
        } else if (mTextFormat == 2) {
        	string = TimeFormat_HMS(time);
        }
		mCurrTime.setText(string);
	}
	
    // 转化为时间格式（分秒）
    private String TimeFormat(int num) {
        String sTime = "";
        int minute = (int) (num / 60);
        int second = (int) (num % 60);
        String sMinute = ConvertToDoubleNum(minute);
        String sSecond = ConvertToDoubleNum(second);
        sTime = sMinute + ":" + sSecond;
        return sTime;
    }

    // 转化为时间格式（时分秒）
    private String TimeFormat_HMS(int num) {
        String sTime = "";
        int hour = (int) (num / 3600);
        int minute = (int) (num / 60) % 60;
        int second = (int) (num % 60);
        String sHour = ConvertToDoubleNum(hour);
        String sMinute = ConvertToDoubleNum(minute);
        String sSecond = ConvertToDoubleNum(second);
        sTime = sHour + ":" + sMinute + ":" + sSecond;
        return sTime;
    }
    
    // 将数字转化为两位字符串
    private String ConvertToDoubleNum(int num) {
        return (num < 10) ? "0" + num : num + "";
    }
    
    private int mScanStartPos = -1;
    private void enterScanMode() {
        if (MediaInterfaceUtil.mediaCannotPlay()) {
            return;
        }
    	if (!mIF.getScanMode()) {
        	mIF.setScanMode(true);
        	mScanStartPos = mIF.getPlayPos();
        	if (mIF.getPlayState() != PlayState.PLAY) {
        	    if (mIF.getPosition() > 10) {
        	        mTimeHandler.sendEmptyMessage(MSG_SCAN_MUSIC_CHANGE);
        	    } else {
                    mIF.setPlayState(PlayState.PLAY);
        	    }
        	}
        	mScanImg.setImageDrawable(sSkinManager.getDrawable(R.drawable.radio_scan_5s_normal));
    	}
    }
    
    private void exitScanMode() {
    	mScanImg.setImageDrawable(sSkinManager.getDrawable(R.drawable.radio_scan_5s_pressed));
    	mIF.setScanMode(false);
    	mTimeHandler.removeMessages(MSG_SCAN_MUSIC_CHANGE);
    	mScanStartPos = -1;
    }
    
    private void checkScanModeAndGoOn() {
    	if (mIF.getScanMode()) {
    		int pos = mIF.getPlayPos();
			int total = mIF.getMediaListSize(mIF.getPlayingDevice(), mIF.getPlayingFileType());
			pos ++;
			if (pos > total - 1) {
				pos = 0;
			}
			mIF.play(pos);
			if (pos == mScanStartPos) {
				exitScanMode();
			}
    	}
    }
    
    public void onError() {
    	checkScanModeAndGoOn();
    }
    
    public void onCompletion() {
    	checkScanModeAndGoOn();
    }
    
    public void updateScanMode(int data) {
        if (data == 0) {
            if (mScanStartPos != -1) {
                exitScanMode();
            }
        }
    }
    
    public void collectFileChanged(int data) {
        if (data == OperateState.SUCCESS) {
            updateCollectIcon();
        }
    }

    public void uncollectFileChanged(int data) {
        if (data == OperateState.SUCCESS) {
            updateCollectIcon();
        }
    }
    
    private void refreshFromViewPagerMaybePlayBTEx(boolean fragmentVisible) {
    	if (fragmentVisible && getVisibility() == View.VISIBLE) {
			if (isBTPlay) {
				BT_IF btIF = BT_IF.getInstance();
				int source = Media_IF.getCurSource();
				boolean btPlaying = btIF.music_isPlaying();
				Log.d(TAG, "refreshFromViewPagerMaybePlayBT source="+source+"; btPlaying="+btPlaying);
				if (!Source.isBTMusicSource(source)) {
					if (btPlaying) {
						btIF.music_play();
						setCurPlayViewState();
					}
				} else {
				    if (btPlaying) {
				        boolean hasFocus = BTMusic_IF.getInstance().hasAudioFocus();
				        boolean calling = Media_IF.getCallState();
				        if (!hasFocus && !calling) {
	                        Log.d(TAG, "refreshFromViewPagerMaybePlayBT hasFocus="+hasFocus+"; calling="+calling);
	                        BTMusic_IF.getInstance().requestAudioFocus(true);
				        }
				    }
				}
			}
		}
    }
    
    public void refreshFromViewPagerMaybePlayBT(boolean fragmentVisible, boolean post) {
    	mTimeHandler.removeMessages(MSG_MAYBE_PLAY_BT);
    	if (post) {
    		mTimeHandler.sendMessageDelayed(
        			mTimeHandler.obtainMessage(MSG_MAYBE_PLAY_BT, fragmentVisible?1:0, 0),
        			200);
    	} else {
    		refreshFromViewPagerMaybePlayBTEx(fragmentVisible);
    	}
	}
    
    private void setCurPlayViewState() {
        Context context = getContext();
        if (context instanceof com.haoke.ui.media.Media_Activity_Main) {
            ((com.haoke.ui.media.Media_Activity_Main)context).setCurPlayViewState();
        }
    }
	
}
