package com.haoke.ui.video;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amd.media.MediaInterfaceUtil;
import com.amd.util.SkinManager;
import com.haoke.bean.FileNode;
import com.haoke.constant.MediaUtil;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.data.AllMediaList;
import com.haoke.data.OperateListener;
import com.haoke.data.PlayStateSharedPreferences;
import com.haoke.mediaservice.R;
import com.haoke.ui.image.Image_Activity_Main;
import com.haoke.util.DebugClock;
import com.haoke.util.DebugLog;
import com.haoke.util.Media_IF;

public class VideoPlayLayout extends RelativeLayout implements View.OnClickListener, OperateListener, OnTouchListener {
	private Context mContext;
	private VideoPlayController mVideoController; // 视频布局框
	private MyVideoView mVideoView;
	private View mForbiddenView;
	private View mCtrlBar;
	private ImageView mBackImageView;
	private ImageView mPreImageView;
	private ImageView mFastPreImageView;
	private ImageView mFastNextImageView;
	private ImageView mNextImageView;
	private ImageView mPlayImageView;
	private ImageView mCollectView;
	private TextView mTitleTextView;
	private VideoPlayTimeSeekBar mTimeSeekBar;
	private View mUnsupportView;
	private ImageView mLoading;
	private Handler mActivityHandler;

	public boolean mNextPlay = true;
	private FileNode mFileNode;
	private boolean mPlayStateBefore = false;

	private SkinManager skinManager;
	private Toast mToEndToast;

	public VideoPlayLayout(Context context) {
		super(context);
	}

	public VideoPlayLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public VideoPlayLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public ImageView getLoadingAnimation(){
		return mLoading;
	}
	public void setBeforePlaystate(boolean playing) {
		mPlayStateBefore = playing;
	}

	public boolean getBeforePlaystate() {
		return mPlayStateBefore;
	}

	public void setFileNode(FileNode fileNode) {
		if (fileNode == null) {
			Log.e("luke", "VideoPlayLayout setFileNode is null!!");
			return;
		}
		setCurFileNode(fileNode);
		mTitleTextView.setText(mFileNode.getFileName());
		updateCollectView();
		updateVideoLayout(true);
		slaverShow(true);
		setBeforePlaystate(true);
		Log.e("luke", "setFileNode setBeforePlaystate" + getBeforePlaystate());

		mVideoController.play(fileNode);

		if (mCtrlBar.getVisibility() == View.VISIBLE) {
			mHandler.removeMessages(HIDE_CTRL);
			mHandler.sendEmptyMessageDelayed(HIDE_CTRL, DELAY_TIME);
		}
	}

	public void setUnsupportViewShow(boolean showFlag) {
		if (mUnsupportView != null) {
			mUnsupportView.setVisibility(showFlag ? View.VISIBLE : View.GONE);
		}

		if (!showFlag) {
			if (mNextPlay) {
				mVideoController.playNext();
			} else {
				mVideoController.playPre();
			}
		}
	}

	public void updatePlayState(boolean playing) { // true: playicon, false:
													// pauseicon
		Log.e("luke", "updatePlayState playIcon " + playing);
		mPlayImageView.setImageDrawable(skinManager.getDrawable(!playing ? R.drawable.image_pause_icon_selector : R.drawable.image_play_icon_selector));
		mTimeSeekBar.updateCurTime();
	}

	public void setCurFileNode(FileNode filenode) {
		mFileNode = filenode;
	}

	public FileNode getCurFileNode() {
		return mFileNode;
	}

	public void updateTimeBar() {
		FileNode fileNode = mVideoController.getPlayFileNode();
		if (fileNode != null) {
			mTitleTextView.setText(fileNode.getFileName());
		}
		mTimeSeekBar.updateTimeInfo();
		updateCollectView();
	}

	public void setActivityHandler(Handler handler) {
		mActivityHandler = handler;
	}
	
	@Override
    protected void onAttachedToWindow() {
	    mVideoController.setRegisterListener(true);
        super.onAttachedToWindow();
    }
	
	@Override
    protected void onDetachedFromWindow() {
	    mVideoController.setRegisterListener(false);
        super.onDetachedFromWindow();
    }

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		mContext = getContext();
		mVideoView = (MyVideoView) findViewById(R.id.video_play_layout);
		mVideoController = new VideoPlayController(mVideoView);

		mLoading = (ImageView) findViewById(R.id.loading_image);

		mVideoView.setOnTouchListener(this);
		mVideoView.setOnPreparedListener(new OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {
				Log.e("luke", "----onPrepared!!");
				Video_Activity_Main.mErrorCount = 0;
				FileNode temp = mVideoController.getPlayFileNode();
				try {
					if (temp != null) {
						Log.e("luke", "------onPrepared filePlayTime: " + temp.getPlayTime());
						mVideoController.playOrPause(getBeforePlaystate());
						mVideoController.setPosition(temp.getPlayTime());
						if (getBeforePlaystate()) {
							startHideTimer();
						}
					}
				} catch (Exception e) {
					Log.e("luke", "--" + e.toString());
				}
				updateTimeBar();
				mLoading.setVisibility(View.GONE);

				// mActivityHandler.removeMessages(Video_Activity_Main.FORBIDDEN_VIEW_TEST);
				// mActivityHandler.sendEmptyMessageDelayed(Video_Activity_Main.FORBIDDEN_VIEW_TEST,
				// 5000);
			}
		});

		mVideoView.setOnErrorListener(new OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
				// TODO Auto-generated method stub
				Log.e("luke", "-----------setOnErrorListener");
				//if (getVisibility() == View.VISIBLE) {
					Log.e("luke", "send error message!!!");
					setUnsupportViewShow(true);
					mActivityHandler.removeMessages(Video_Activity_Main.HIDE_UNSUPPORT_VIEW);
					mActivityHandler.sendEmptyMessageDelayed(Video_Activity_Main.HIDE_UNSUPPORT_VIEW, 1000);
				//}
				mLoading.setVisibility(View.GONE);
				return true;
			}
		});

		mVideoView.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer arg0) {
				// VideoPlayController.isVideoPlaying = false;
				mNextPlay = true;
				setBeforePlaystate(VideoPlayController.isVideoPlaying);
				Log.e("luke", "OnCompletionListener setBeforePlaystate " + getBeforePlaystate());
				updateTimeBar();
				mVideoController.getPlayFileNode().setPlayTime(0);
				mVideoController.playNext();
			}
		});

		mCtrlBar = findViewById(R.id.video_play_ctrlbar);
		mBackImageView = (ImageView) mCtrlBar.findViewById(R.id.video_ctrlbar_list);
		mBackImageView.setOnClickListener(this);
		mPreImageView = (ImageView) mCtrlBar.findViewById(R.id.video_ctrlbar_pre);
		mPreImageView.setOnClickListener(this);
		mFastPreImageView = (ImageView) mCtrlBar.findViewById(R.id.video_ctrlbar_fastpre);
		mFastPreImageView.setOnClickListener(this);
		mFastNextImageView = (ImageView) mCtrlBar.findViewById(R.id.video_ctrlbar_fastnext);
		mFastNextImageView.setOnClickListener(this);
		mNextImageView = (ImageView) mCtrlBar.findViewById(R.id.video_ctrlbar_next);
		mNextImageView.setOnClickListener(this);
		mPlayImageView = (ImageView) mCtrlBar.findViewById(R.id.video_ctrlbar_pp);
		mPlayImageView.setOnClickListener(this);
		mTimeSeekBar = (VideoPlayTimeSeekBar) findViewById(R.id.video_play_time_seekbar);
		mTimeSeekBar.setVideoLayout(this);
		mCollectView = (ImageView) findViewById(R.id.collect_video);
		mCollectView.setOnClickListener(this);
		mTitleTextView = (TextView) findViewById(R.id.title_video);
		mForbiddenView = findViewById(R.id.video_play_forbidden);
		mForbiddenView.setOnTouchListener(this);
		mUnsupportView = findViewById(R.id.not_support_text);
		mVideoController.setVideoPlayLayout(this);

		initTouchSlop();
		skinManager = SkinManager.instance(mContext);
	}
	
	private boolean registerFlag = false;
	public void registerMediaButtonReceiver() {
	    if (!registerFlag) {
	        getContext().registerReceiver(mBroadcastReceiver, new IntentFilter(Intent.ACTION_MEDIA_BUTTON));
	        registerFlag = true;
	    }
	}
	
	public void unRegisterMediaButtonReceiver() {
	    if (registerFlag) {
	        getContext().unregisterReceiver(mBroadcastReceiver);
	        registerFlag = false;
	    }
	}

	// 系统按键处理
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d("luke", "Steering wheel control onReceive action=" + action);
			if (MediaInterfaceUtil.mediaCannotPlay()) {
				return;
			}
			if (Intent.ACTION_MEDIA_BUTTON.equals(action)) {
				KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
				if (event == null || event.getAction() != KeyEvent.ACTION_UP) {
					return;
				}
				int keycode = event.getKeyCode();
				Log.d("luke", "onReceive keycode=" + keycode);
				mediaKeyHandle(context, keycode);
			}
		}
	};

	public void mediaKeyHandle(Context context, int key) {
		switch (key) {
		case KeyEvent.KEYCODE_MEDIA_PLAY: // 126
			if (!Image_Activity_Main.isPlayImage(context) && mVideoController != null) {
				// play();
				if (mVideoController.getVideoView().getVisibility() == View.VISIBLE) {
					if (mVideoController.hasAudioFocus()) {
						mVideoController.playOrPause(true);
					} else {
						mVideoController.getControllerHandler().sendEmptyMessage(VideoPlayController.VR_PLAY_STATE);
					}
				}
			}
			break;
		case KeyEvent.KEYCODE_MEDIA_PAUSE: // 127
			if (!Image_Activity_Main.isPlayImage(context) && mVideoController != null) {
				// pause();
				// zanting
				if (mVideoController.getVideoView().getVisibility() == View.VISIBLE) {
					if (mVideoController.hasAudioFocus()) {
						mVideoController.playOrPause(false);
					} else {
						mVideoController.getControllerHandler().sendEmptyMessage(VideoPlayController.VR_PAUSE_STATE);
					}
				}
			}
			break;
		case KeyEvent.KEYCODE_MEDIA_PREVIOUS: // 88
			// prev();
			// 上一个
			if (mVideoController != null) {
				if (mVideoController.getVideoView().getVisibility() == View.VISIBLE) {
					mVideoController.playPre();
					mNextPlay = false;
				}
			}
			break;
		case KeyEvent.KEYCODE_MEDIA_NEXT: // 87
			// next();
			// 下一个
			if (mVideoController != null) {
				if (mVideoController.getVideoView().getVisibility() == View.VISIBLE) {
					mVideoController.playNext();
					mNextPlay = true;
				}
			}
			break;
		}
	}

	public void refreshSkin() {
		mBackImageView.setImageDrawable(skinManager.getDrawable(R.drawable.image_back_icon_selector));
		mPreImageView.setImageDrawable(skinManager.getDrawable(R.drawable.image_pre_icon_selector));
		mFastPreImageView.setImageDrawable(skinManager.getDrawable(R.drawable.video_ctrl_fastpre_selector));
		mFastNextImageView.setImageDrawable(skinManager.getDrawable(R.drawable.video_ctrl_fastnext_selector));
		mNextImageView.setImageDrawable(skinManager.getDrawable(R.drawable.image_next_icon_selector));
		mUnsupportView.setBackground(skinManager.getDrawable(R.drawable.pub_msgbox_bg1));
		updateCollectView();
		mTimeSeekBar.refreshSkin();
	}

	public void onResume() {
		Log.e("luke", "------VideoPlayLayout onResume " + getBeforePlaystate());
		if (mFileNode != null) {
			mTitleTextView.setText(mFileNode.getFileName());
		}
		updateCollectView();
		mVideoController.startRecordTimer();

		updateVideoLayout(true);
		if (!mVideoController.isVideoPlaying) {
			slaverShow(true);
		}

		if (mFileNode != null) {
			mVideoController.playOrPause(getBeforePlaystate());
			startHideTimer();
		}
	}

	public void onPause() {
		Log.e("luke", "------VideoPlayLayout onPause: " + mVideoController.isVideoPlaying);
		if (mContext == null) {
			return;
		}
		mVideoController.stopRecordTimer();
		mVideoController.getVideoView().setVisibility(View.INVISIBLE);

		if (mVideoController.isPlayState() == true) {
			mVideoController.playOrPause(false);
		}
	}

	public void updateVideoLayout(boolean checkSpeed) {
		DebugClock debugClock = new DebugClock();
		mVideoController.getVideoView().setVisibility(View.VISIBLE);
		debugClock.calculateTime("luke", "updateVideoLayout setVisibility");
		if (checkSpeed) {
			if (AllMediaList.sCarSpeed == 0) {
				AllMediaList.sCarSpeed = Media_IF.getCarSpeed();
			}
			checkSpeedAndRefreshView(AllMediaList.sCarSpeed);
		}
		debugClock.calculateTime("luke", "updateVideoLayout checkSpeedAndRefreshView");
	}

	@Override
	public void onClick(View view) {
		stopHideTimer();
		setBeforePlaystate(mVideoController.isVideoPlaying);
		switch (view.getId()) {
		case R.id.video_ctrlbar_list:
			if (mActivityHandler != null) {
				mActivityHandler.sendEmptyMessage(Video_Activity_Main.SWITCH_TO_LIST_FRAGMENT);
			}
			break;
		case R.id.video_ctrlbar_pre:
			if (MediaInterfaceUtil.mediaCannotPlay()) {
				break;
			}
			mNextPlay = false;
			setBeforePlaystate(true);
			mVideoController.playPre();
			updateCollectView();
			break;
		case R.id.video_ctrlbar_fastpre: // 快退
			if (MediaInterfaceUtil.mediaCannotPlay()) {
				break;
			}
			showToast(true);
			break;
		case R.id.video_ctrlbar_pp:
			if (MediaInterfaceUtil.mediaCannotPlay()) {
				break;
			}
			boolean playing = mVideoController.isPlayState();
			Log.e("luke", "-----onClick playing: " + playing);
			mVideoController.playOrPause(!playing);
			break;
		case R.id.video_ctrlbar_fastnext: // 快进
			if (MediaInterfaceUtil.mediaCannotPlay()) {
				break;
			}
			showToast(false);
			break;
		case R.id.video_ctrlbar_next:
			if (MediaInterfaceUtil.mediaCannotPlay()) {
				break;
			}
			mNextPlay = true;
			setBeforePlaystate(true);
			mVideoController.playNext();
			updateCollectView();
			break;
		case R.id.collect_video:
			collectOrUncollect();
			break;
		}
		startHideTimer();
		Log.e("luke", "onClick setBeforePlaystate " + getBeforePlaystate());
	}

	private void showToast(boolean isFastPre) {
		int oldPosition = mVideoController.getPosition();
		if (oldPosition >= mVideoController.getDuration() - 15 && !isFastPre) {
			if (mToEndToast == null) {
				mToEndToast = Toast.makeText(mContext, R.string.video_fastnext_to_end_message, Toast.LENGTH_SHORT);
			}
			mToEndToast.show();
			return;
		}
		int newPosition = oldPosition + (isFastPre ? -30 : 30);
		if (newPosition >= mVideoController.getDuration()) {
			newPosition = mVideoController.getDuration() - 3;
		}
		if (newPosition <= 0) {
			newPosition = 0;
		}
		mVideoController.setPosition(newPosition);

		mTimeSeekBar.showTrackView(isFastPre, mVideoController.getPosition());
	}

	private void collectOrUncollect() {
		if (mFileNode != null && !mFileNode.isFromCollectTable()) {
			if (mFileNode.getCollect() == 0) {
				// 收藏视频。
				if (AllMediaList.instance(mContext).getCollectSize(FileType.VIDEO) < MediaUtil.COLLECT_COUNT_MAX) {
					AllMediaList.instance(mContext).collectMediaFile(mFileNode, this);
				} else {
					new AlertDialog.Builder(mContext).setTitle(R.string.collect_limit_dialog_title).setMessage(R.string.collect_limit_dialog_message)
							.setPositiveButton(R.string.collect_limit_dialog_ok, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									AllMediaList.instance(mContext).deleteOldCollect(FileType.VIDEO);
									AllMediaList.instance(mContext).collectMediaFile(mFileNode, VideoPlayLayout.this);
								}
							}).setNegativeButton(R.string.collect_limit_dialog_cancel, null).show();
				}
			} else {
				// 取消收藏。
				AllMediaList.instance(mContext).uncollectMediaFile(mFileNode, this);
			}
		}
	}

	@Override
	public void onOperateCompleted(int operateValue, int progress, int resultCode) {
		if (operateValue == OperateListener.OPERATE_COLLECT) {
			if (resultCode == OperateListener.OPERATE_SUCEESS) {
				if (progress == 100) {
					mCollectView.setImageDrawable(skinManager.getDrawable(R.drawable.media_collect));
				}
			} else {
				Toast.makeText(mContext, "收藏视频异常", Toast.LENGTH_SHORT).show();
			}
		} else if (operateValue == OperateListener.OPERATE_UNCOLLECT) {
			if (resultCode == OperateListener.OPERATE_SUCEESS) {
				if (progress == 100) {
					mCollectView.setImageDrawable(skinManager.getDrawable(R.drawable.media_uncollect));
				}
			} else {
				Toast.makeText(mContext, "取消收藏视频异常", Toast.LENGTH_SHORT).show();
			}
		}
	}

	// 启动托盘隐藏计时器
	public void startHideTimer() {
		if (mVideoController.isPlayState()) {
			Log.e("luke", "startHideTimer");
			mHandler.removeMessages(HIDE_CTRL);
			mHandler.sendEmptyMessageDelayed(HIDE_CTRL, DELAY_TIME);
		}
	}

	// 停止托盘隐藏计时器
	public void stopHideTimer() {
		Log.e("luke", "stopHideTimer");
		mHandler.removeMessages(HIDE_CTRL);
	}

	// 托盘显示控制
	private void slaverShow(boolean visible) {
		if (visible) {
			if (mCtrlBar.getVisibility() != View.VISIBLE) {
				mCtrlBar.setVisibility(View.VISIBLE);
				mTimeSeekBar.setVisibility(View.VISIBLE);
				updateCollectView();
				mTitleTextView.setVisibility(View.VISIBLE);
				startHideTimer();
			}
		} else {
			mCtrlBar.setVisibility(View.GONE);
			mTimeSeekBar.setVisibility(View.GONE);
			mCollectView.setVisibility(View.GONE);
			mTitleTextView.setVisibility(View.GONE);
			stopHideTimer();
		}
	}

	private static final int DELAY_TIME = 5000;
	private static final int HIDE_CTRL = 1;
	private static final int DELAY_PLAY = 2;
	private static final int END_SCROLL = 3;
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HIDE_CTRL:
				slaverShow(false);
				break;
			case DELAY_PLAY:
				// Video_IF.getInstance().setPlayState(PlayState.PLAY);
				// updatePlayState(mVideoController.isPlayState());
				startHideTimer();
				break;
			case END_SCROLL:
				mTimeSeekBar.onStopTrackingTouch(mTimeSeekBar.getSeekBar());
				break;
			default:
				break;
			}
			super.handleMessage(msg);
		}
	};

	public void checkPlayFileNode(ArrayList<FileNode> dataList) {
		boolean isExistFlag = false;
		for (FileNode fileNode : dataList) {
			if (fileNode.isSame(mFileNode)) {
				isExistFlag = true;
				break;
			}
		}
		if (!isExistFlag && mActivityHandler != null) {
			mActivityHandler.sendEmptyMessage(Video_Activity_Main.SWITCH_TO_LIST_FRAGMENT);
		}
	}

	private float mLastFocusX;
	private float mLastFocusY;
	private float mDownFocusX;
	private float mDownFocusY;
	private boolean mAlwaysInTapRegion;
	private int mTouchSlopSquare;

	private void initTouchSlop() {
		int touchSlop;
		if (mContext == null) {
			// noinspection deprecation
			touchSlop = ViewConfiguration.getTouchSlop();
		} else {
			final ViewConfiguration configuration = ViewConfiguration.get(mContext);
			touchSlop = configuration.getScaledTouchSlop();
			mTouchSlopSquare = touchSlop * touchSlop;
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// Log.e("luke","onTouch " + event.toString());
		int eventaction = event.getAction();
		final boolean pointerUp = (eventaction & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_UP;
		final int skipIndex = pointerUp ? event.getActionIndex() : -1;

		// Determine focal point
		float sumX = 0, sumY = 0;
		final int count = event.getPointerCount();
		for (int i = 0; i < count; i++) {
			if (skipIndex == i)
				continue;
			sumX += event.getX(i);
			sumY += event.getY(i);
		}
		final int div = pointerUp ? count - 1 : count;
		final float focusX = sumX / div;
		final float focusY = sumY / div;

		switch (eventaction) {
		case MotionEvent.ACTION_DOWN:
			mDownFocusX = mLastFocusX = focusX;
			mDownFocusY = mLastFocusY = focusY;
			mAlwaysInTapRegion = true;
			break;
		case MotionEvent.ACTION_MOVE:
			final float scrollX = mLastFocusX - focusX;
			final float scrollY = mLastFocusY - focusY;

			if (mAlwaysInTapRegion) {
				final int deltaX = (int) (focusX - mDownFocusX);
				final int deltaY = (int) (focusY - mDownFocusY);
				int distance = (deltaX * deltaX) + (deltaY * deltaY);
				if (distance > mTouchSlopSquare) {
					// TODO onscroll
					doOnScroll(scrollX);
					mLastFocusX = focusX;
					mLastFocusY = focusY;
					mAlwaysInTapRegion = false;
				}

			} else if ((Math.abs(scrollX) >= 1) || (Math.abs(scrollY) >= 1)) {
				// TODO onscroll
				doOnScroll(scrollX);
				mLastFocusX = focusX;
				mLastFocusY = focusY;
			}

			break;
		case MotionEvent.ACTION_UP:
			if (mAlwaysInTapRegion) {
				// TODO onSingleTap
				doOnSingleTap();
			} else {
				mTimeSeekBar.onStopTrackingTouch(mTimeSeekBar.getSeekBar());
			}
			startHideTimer();
			break;

		case MotionEvent.ACTION_CANCEL:
			mAlwaysInTapRegion = false;
			break;
		}
		return true;
	}

	private void doOnScroll(float distanceX) {
		Log.e("luke", "onScroll");
		if (mCtrlBar.getVisibility() != View.VISIBLE) {
			slaverShow(true);
		}
		mTimeSeekBar.onStartTrackingTouch(mTimeSeekBar.getSeekBar());
		SeekBar seekBar = mTimeSeekBar.getSeekBar();
		int position = seekBar.getProgress();
		Log.e("luke", "onTouch position: " + position + "  ," + modifyDistanceX(distanceX) + "  ," + distanceX);
		seekBar.setProgress(position - modifyDistanceX(distanceX));
		mTimeSeekBar.checkScroll();
	}

	private void doOnSingleTap() {
		Log.e("luke", "onSingleTapUp");
		slaverShow(mCtrlBar.getVisibility() != View.VISIBLE);
	}

	private int modifyDistanceX(float distanceX) {
		int distance = 0;
		if (mFileNode != null) {
			int duration = mVideoController.getDuration();
			float temp = distanceX * duration / 3600.0f;
			if (((int) Math.abs(temp)) == 0) {
				temp = (temp < 0 ? -1 : 1);
			}
			distance = (int) (temp);
		}
		return distance;
	}

	public boolean isShowForbiddenView() {
		boolean showFlag = false;
		if (mForbiddenView != null) {
			showFlag = mForbiddenView.getVisibility() == View.VISIBLE;
		}
		return showFlag;
	}

	public void checkSpeedAndRefreshView(float speed) {
		boolean showForbiddenViewFlag = false;
		try {
			boolean sysLimitFlag = mVideoController.limitToPlayVideoWhenDrive();
			boolean speedLimitFlag = (speed >= 20.0f);
			showForbiddenViewFlag = (sysLimitFlag && speedLimitFlag);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (showForbiddenViewFlag) {
			DebugLog.d("Yearlay", "show Forbidden View... speed : " + speed);
		} else {
			DebugLog.d("Yearlay", "hide Forbidden View... speed : " + speed);
		}
		mForbiddenView.setVisibility(showForbiddenViewFlag ? View.VISIBLE : View.GONE);
	}

	public void showOrHideForbiddenView(boolean showForbiddenView) {
		boolean showForbiddenViewFlag = false;
		try {
			boolean sysLimitFlag = mVideoController.limitToPlayVideoWhenDrive();
			showForbiddenViewFlag = (sysLimitFlag && showForbiddenView);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (showForbiddenViewFlag) {
			DebugLog.d("Yearlay", "show Forbidden View... speed : " + AllMediaList.sCarSpeed);
		} else {
			DebugLog.d("Yearlay", "hide Forbidden View... speed : " + AllMediaList.sCarSpeed);
		}
		mForbiddenView.setVisibility(showForbiddenViewFlag ? View.VISIBLE : View.GONE);
	}

	private void updateCollectView() {
		if (mFileNode == null || mCollectView == null) {
			return;
		}
		boolean showFlag = !mFileNode.isFromCollectTable();
		if (showFlag) {
			mCollectView.setImageDrawable(skinManager.getDrawable(mFileNode.getCollect() == 1 ? R.drawable.media_collect : R.drawable.media_uncollect));
		}
		if (mCtrlBar.getVisibility() == View.VISIBLE) {
			mCollectView.setVisibility(showFlag ? View.VISIBLE : View.GONE);
		}
	}

	public VideoPlayController getVideoController() {
		return mVideoController;
	}

	public FileNode playDefault() {
		ArrayList<FileNode> videoList = null;
		if (mFileNode == null) {
			PlayStateSharedPreferences sPreferences = PlayStateSharedPreferences.instance();
			int deviceType = sPreferences.getLastDeviceTypeVideo();
			Log.e("luke","playDefault record deviceType： " + deviceType);
			
			if(deviceType == 0){//没有记忆文件，播放(本地>USB1>USB2)
				Log.e("luke", "playDefault record video file is not exsit!!!!!");
				videoList = AllMediaList.instance(mContext).getMediaList(DeviceType.FLASH, FileType.VIDEO);
				if (videoList.size() == 0) {
					videoList = AllMediaList.instance(mContext).getMediaList(DeviceType.USB1, FileType.VIDEO);
				} 
				if (videoList.size() == 0) {
					videoList = AllMediaList.instance(mContext).getMediaList(DeviceType.USB2, FileType.VIDEO);
				}
				if (videoList.size() > 0 ) {//播放(本地>USB1>USB2)中第一个文件
					mFileNode = videoList.get(0);
					Log.e("luke","playDefault deviceType: " + mFileNode.getDeviceType());
				} else {
					Log.e("luke","playDefault Device have no any video!!!!!");
					mFileNode = null;
				}
			} else { //存在记忆文件
				videoList = AllMediaList.instance(mContext).getMediaList(deviceType, FileType.VIDEO);
				if(videoList.size() > 0){
					String videoInfo = sPreferences.getPlayTime(deviceType, FileType.VIDEO);
					Log.e("luke","playDefault record video： " + videoInfo);
					String splitStr = PlayStateSharedPreferences.SPLIT_STR;
					String filePath = videoInfo.substring(0, videoInfo.indexOf(splitStr));
					String playTimeStr = videoInfo.substring(videoInfo.indexOf(splitStr) + 2, videoInfo.length());
					int playTime = Integer.valueOf(playTimeStr);
					
					if (!TextUtils.isEmpty(filePath)) { 
						for (FileNode fileNode : videoList) {
							if (filePath.equals(fileNode.getFilePath())) {
								mFileNode = fileNode;
								mFileNode.setPlayTime(playTime);
								break;
							}
						}
					}
					
					if(mFileNode == null){ //记录视频不存在，当前DeviceType第一个视频作为播放视频
						Log.e("luke", "playDefault record video no exsit!!!!");
						mFileNode = videoList.get(0);
					}
				} else {//记录deviceType中没有任何文件
					Log.e("luke", "playDefault record deviceType have no any video");
					videoList = AllMediaList.instance(mContext).getMediaList(DeviceType.FLASH, FileType.VIDEO);
					if (videoList.size() == 0) {
						videoList = AllMediaList.instance(mContext).getMediaList(DeviceType.USB1, FileType.VIDEO);
					} 
					if (videoList.size() == 0) {
						videoList = AllMediaList.instance(mContext).getMediaList(DeviceType.USB2, FileType.VIDEO);
					}
					if (videoList.size() > 0 ) { //播放(本地>USB1>USB2)中第一个文件
						mFileNode = videoList.get(0);
						Log.e("luke","playDefault deviceType: " + mFileNode.getDeviceType());
					} else {
						Log.e("luke","playDefault Device have no any video!!!!!");
						mFileNode = null;
					}
				}
			}
		} else {
			videoList = AllMediaList.instance(mContext).getMediaList(mFileNode.getDeviceType(), FileType.VIDEO);
			boolean fileNodeExsitFlag = false;
			if(videoList.size() > 0){
				for (FileNode fileNode : videoList) {
					if (mFileNode.isSamePathAndFrom(fileNode)) {
						fileNodeExsitFlag = true;
						break;
					}
				}
				if(fileNodeExsitFlag){ //
					Log.e("luke", "playDefault mFileNode is exsit!!!");
				} else {//之前视频文件不存在，播放第一个文件
					Log.e("luke", "playDefault mFileNode delete!!!");
					mFileNode = videoList.get(0);
				}
			} else {//当前filenode的devicetype没有文件
				Log.e("luke", "playDefault mFileNode deviceType have no any video");
				videoList = AllMediaList.instance(mContext).getMediaList(DeviceType.FLASH, FileType.VIDEO);
				if (videoList.size() == 0) {
					videoList = AllMediaList.instance(mContext).getMediaList(DeviceType.USB1, FileType.VIDEO);
				} 
				if (videoList.size() == 0) {
					videoList = AllMediaList.instance(mContext).getMediaList(DeviceType.USB2, FileType.VIDEO);
				}
				if (videoList.size() > 0 ) { //播放(本地>USB1>USB2)中第一个文件
					mFileNode = videoList.get(0);
					Log.e("luke","playDefault deviceType: " + mFileNode.getDeviceType());
				} else {
					Log.e("luke","playDefault Device have no any video!!!!!");
					mFileNode = null;
				}
			}
		}
		setBeforePlaystate(true);
		Log.e("luke", "playDefault setBeforePlaystate: " + getBeforePlaystate());
		setFileNode(mFileNode);
		mVideoController.playDefaultVideo(true);
		return mFileNode;
	}
}
