package com.haoke.ui.video;

import java.util.ArrayList;

import haoke.ui.util.OnHKTouchListener;
import haoke.ui.util.TOUCH_ACTION;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amd.media.MediaInterfaceUtil;
import com.amd.util.SkinManager;
import com.haoke.bean.FileNode;
import com.haoke.constant.MediaUtil;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.data.AllMediaList;
import com.haoke.data.OperateListener;
import com.haoke.data.PlayStateSharedPreferences;
import com.haoke.constant.MediaUtil.MediaState;
import com.haoke.constant.MediaUtil.PlayState;
import com.haoke.mediaservice.R;
import com.haoke.util.DebugLog;
import com.haoke.video.VideoSurfaceView;
import com.nforetek.bt.res.MsgOutline;

public class VideoPlayLayout extends RelativeLayout implements OnHKTouchListener, View.OnClickListener,
        OperateListener, OnTouchListener, OnGestureListener {
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
    private Handler mActivityHandler;
    private GestureDetector mGestureDetector;
    
    private boolean mNextPlay = true;
    private FileNode mFileNode;
    
    private SkinManager skinManager;
    
    public VideoPlayLayout(Context context) {
        super(context);
    }
    
    public VideoPlayLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public VideoPlayLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setFileNode(FileNode fileNode) {
    	if (fileNode == null) {
    		Log.e("luke","VideoPlayLayout setFileNode is null!!");
    		return;
    	}
        mFileNode = fileNode;
        mTitleTextView.setText(mFileNode.getFileName());
        updateCollectView();
        mVideoController.play(mFileNode);
        updateVideoLayout(true);
        if (mCtrlBar.getVisibility() == View.VISIBLE) {
            mHandler.removeMessages(HIDE_CTRL);
            mHandler.sendEmptyMessageDelayed(HIDE_CTRL, DELAY_TIME);
        }
    }
    
    public void setUnsupportViewShow(boolean showFlag) {
        if (mUnsupportView != null) {
            mUnsupportView.setVisibility(showFlag ? View.VISIBLE : View.GONE);
        }
        
        if(!showFlag) {
        	if(mNextPlay){
        		mVideoController.playNext();
        	}else {
        		mVideoController.playPre();
        	}
        }
    }
    
    public void updatePlayState(int playState) {
        mPlayImageView.setImageDrawable(skinManager.getDrawable(
        		!mVideoController.isPlayState() ?
                R.drawable.image_pause_icon_selector : R.drawable.image_play_icon_selector));
        mTimeSeekBar.updateCurTime();
    }
    
    public void updateTimeBar() {
        FileNode fileNode = mVideoController.getPlayFileNode();
        if (fileNode != null) {
            if (!fileNode.isSame(mFileNode)) {
                mFileNode = fileNode;
            }
            mTitleTextView.setText(fileNode.getFileName());
        }
        mTimeSeekBar.updateTimeInfo();
        updateCollectView();
    }
    
    public void setActivityHandler(Handler handler) {
        mActivityHandler = handler;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mContext = getContext();
        mVideoView = (MyVideoView) findViewById(R.id.video_play_layout);
        mVideoController = new VideoPlayController(mVideoView);
        
        mGestureDetector = new GestureDetector(this);
        
        
        mVideoView.setOnTouchListener(this);
        mVideoView.setOnPreparedListener(new OnPreparedListener() {
        	@Override
        	public void onPrepared(MediaPlayer mp) {
        		Log.e("luke","----onPrepared!!");
        		//videoView.setBackgroundColor(0xff000000);
        		
        		Video_Activity_Main.mErrorCount = 0;
        		FileNode temp =  mVideoController.getPlayFileNode();
        		try{
        			if(temp != null){
        				//mVideoController.play(temp);
        				//mVideoController.startRecordTimer();
        				Log.e("luke","------onPrepared getPlayTime: " + temp.getPlayTime());
        				//mVideoController.play(temp);
        				mVideoController.getVideoView().start();
        				mVideoController.setPosition(temp.getPlayTime());
        			}
        		
        		}catch (Exception e) {
        			Log.e("luke","--" + e.toString());
        		}
        		
        		updateTimeBar();
        		
        	}
        });
        
        mVideoView.setOnErrorListener(new OnErrorListener(){

			@Override
			public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
				// TODO Auto-generated method stub
				Log.e("luke","-----------setOnErrorListener");
		        if (getVisibility() == View.VISIBLE) {
		        	Log.e("luke","send error message!!!");
		            setUnsupportViewShow(true);
		            mActivityHandler.removeMessages(Video_Activity_Main.HIDE_UNSUPPORT_VIEW);
		            mActivityHandler.sendEmptyMessageDelayed(Video_Activity_Main.HIDE_UNSUPPORT_VIEW, 1000);
		        }
				return true;
			}
        	
        });
        
        mVideoView.setOnCompletionListener(new OnCompletionListener(){

			@Override
			public void onCompletion(MediaPlayer arg0) {
				// TODO Auto-generated method stub
				Log.e("luke","setOnCompletionListener");
				updateTimeBar();
				mVideoController.playNext();
			}
        	
        });
        
        
        //mVideoView = new VideoSurfaceView(mContext);
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
        mTimeSeekBar.setVideoController(mVideoController);
        mTimeSeekBar.setHKTouchListener(this);
        mCollectView = (ImageView) findViewById(R.id.collect_video);
        mCollectView.setOnClickListener(this);
        mTitleTextView = (TextView) findViewById(R.id.title_video);
        mForbiddenView = findViewById(R.id.video_play_forbidden);
        mForbiddenView.setOnTouchListener(this);
        mUnsupportView = findViewById(R.id.not_support_text);
        skinManager = SkinManager.instance(mContext);
    }
    
    public void refreshSkin() {
        mBackImageView.setImageDrawable(skinManager.getDrawable(R.drawable.image_back_icon_selector));
        mPreImageView.setImageDrawable(skinManager.getDrawable(R.drawable.image_pre_icon_selector));
        mFastPreImageView.setImageDrawable(skinManager.getDrawable(R.drawable.video_ctrl_fastpre_selector));
        mFastNextImageView.setImageDrawable(skinManager.getDrawable(R.drawable.video_ctrl_fastnext_selector));
        mNextImageView.setImageDrawable(skinManager.getDrawable(R.drawable.image_next_icon_selector));
        mTimeSeekBar.refreshSkin();
    }

    public void onResume() {
        //Video_IF.getInstance().setVideoShow(true);
    	Log.e("luke","------VideoPlayLayout onResume");
        if (mFileNode != null) {
            mTitleTextView.setText(mFileNode.getFileName());
        }
        updateCollectView();
        mVideoController.startRecordTimer();
        
        if (savePlayState) { // 如果在onPause的时候有保存这个状态。
            savePlayState = false;
            mHandler.sendEmptyMessageDelayed(DELAY_PLAY, 1000);

        }
        updateVideoLayout(true);
        if (!mVideoController.isPlayState()) {
            slaverShow(true);
        }
    }
    
    private boolean savePlayState = false;

    public void onPause() {
    	Log.e("luke","------VideoPlayLayout onPause");
        if (mContext == null) {
            return;
        }
        mHandler.removeMessages(DELAY_PLAY);
        mVideoController.stopRecordTimer();
        savePlayState = true;

        mVideoController.getVideoView().pause();
        mVideoController.getVideoView().setVisibility(View.INVISIBLE);
    }

    public void updateVideoLayout(boolean checkSpeed) {
    	Log.e("luke","----updateVideoLayout!!");
        mVideoController.getVideoView().setVisibility(View.VISIBLE);
        if (checkSpeed) {
            checkSpeedAndRefreshView(AllMediaList.sCarSpeed);
        }
    }

    @Override
    public void onClick(View view) {
        stopHideTimer();
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
            mVideoController.playPre();
            break;
        case R.id.video_ctrlbar_fastpre:  // 快退
            if (MediaInterfaceUtil.mediaCannotPlay()) {
                break;
            }
            showToast(true);
            break;
        case R.id.video_ctrlbar_pp:
            if (MediaInterfaceUtil.mediaCannotPlay()) {
                break;
            }
            //Video_IF.getInstance().changePlayState();
            boolean playing = mVideoController.isPlayState();
            Log.e("luke","-----onClick playing: " + playing);
            mPlayImageView.setImageDrawable(skinManager.getDrawable(
            		!playing ?
                    R.drawable.image_pause_icon_selector : R.drawable.image_play_icon_selector));
            if(playing){
            	mVideoController.getVideoView().pause();
            } else {
            	mVideoController.getVideoView().start();
            }
            mTimeSeekBar.updateCurTime();
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
            mVideoController.playNext();
            break;
        case R.id.collect_video:
            collectOrUncollect();
            break;
        }
        startHideTimer();
    }
    
    private void showToast(boolean isFastPre) {
        int oldPosition = mVideoController.getPosition();
        if (oldPosition >= mVideoController.getDuration() - 15 && !isFastPre) {
            Toast.makeText(mContext, R.string.video_fastnext_to_end_message, Toast.LENGTH_SHORT).show();
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
                    new AlertDialog.Builder(mContext).setTitle(R.string.collect_limit_dialog_title)
                    .setMessage(R.string.collect_limit_dialog_message)
                    .setPositiveButton(R.string.collect_limit_dialog_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AllMediaList.instance(mContext).deleteOldCollect(FileType.VIDEO);
                            AllMediaList.instance(mContext).collectMediaFile(mFileNode, VideoPlayLayout.this);
                        }
                    })
                    .setNegativeButton(R.string.collect_limit_dialog_cancel, null)
                    .show();
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

    @Override
    public void OnHKTouchEvent(View view, TOUCH_ACTION action) {
        if (action == TOUCH_ACTION.BTN_DOWN) {
            stopHideTimer();
        } else if (action == TOUCH_ACTION.BTN_UP) {
            startHideTimer();
        }
    }

    // 启动托盘隐藏计时器
    private void startHideTimer() {
        if (mVideoController.isPlayState()) {
            mHandler.removeMessages(HIDE_CTRL);
            mHandler.sendEmptyMessageDelayed(HIDE_CTRL, DELAY_TIME);
        }
    }

    // 停止托盘隐藏计时器
    private void stopHideTimer() {
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
                //Video_IF.getInstance().setPlayState(PlayState.PLAY);
                //updatePlayState(mVideoController.isPlayState());
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

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent e) {  // 用户轻触触摸屏，由1个MotionEvent ACTION_DOWN触发
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) { // 用户轻触触摸屏，尚未松开或拖动，由一个1个MotionEvent ACTION_DOWN触发
    }
    
    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) { // 用户（轻触触摸屏后）松开，由一个1个MotionEvent ACTION_UP触发
        slaverShow(mCtrlBar.getVisibility() != View.VISIBLE);
        return true;
    }
    
    private int modifyDistanceX(float distanceX) {
        int distance = 0;
        if (mFileNode != null) {
            int duration = mVideoController.getDuration();
            float temp = distanceX * duration / 3600.0f;
            if (((int ) Math.abs(temp)) == 0) {
                temp = (temp < 0 ? -1 : 1);
            }
            distance = (int)(temp);
        }
        return distance;
    }
    
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (mCtrlBar.getVisibility() != View.VISIBLE) {
            slaverShow(true);
        }
        mTimeSeekBar.onStartTrackingTouch(mTimeSeekBar.getSeekBar());
        SeekBar seekBar = mTimeSeekBar.getSeekBar();
        int position = seekBar.getProgress();
        seekBar.setProgress(position - modifyDistanceX(distanceX));
        mTimeSeekBar.checkScroll();
        mHandler.removeMessages(END_SCROLL);
        mHandler.sendEmptyMessageDelayed(END_SCROLL, 1500);
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        mTimeSeekBar.onStopTrackingTouch(mTimeSeekBar.getSeekBar());
        return true;
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

    private void updateCollectView() {
        if (mFileNode == null || mCollectView == null) {
            return;
        }
        boolean showFlag = !mFileNode.isFromCollectTable();
        if (showFlag) {
            mCollectView.setImageDrawable(skinManager.getDrawable(mFileNode.getCollect() == 1 ?
                    R.drawable.media_collect : R.drawable.media_uncollect));
        }
        if (mCtrlBar.getVisibility() == View.VISIBLE) {
            mCollectView.setVisibility(showFlag ? View.VISIBLE : View.GONE);
        }
    }

	public VideoPlayController getVideoController() {
		return mVideoController;
	}
	
	public void playDefault() {
		if (mFileNode == null) {
			// 取系统存储的默认的路径来进行播放。
			PlayStateSharedPreferences sPreferences = PlayStateSharedPreferences.instance();
			int deviceType = sPreferences.getLastDeviceTypeVideo();
			String videoInfo = sPreferences.getPlayTime(deviceType, FileType.VIDEO);
			String splitStr = PlayStateSharedPreferences.SPLIT_STR;
            String filePath = videoInfo.substring(0, videoInfo.indexOf(splitStr));
            String playTimeStr = videoInfo.substring(videoInfo.indexOf(splitStr) +
            		videoInfo.length(), videoInfo.length());
            int playTime = Integer.valueOf(playTimeStr);
            
            ArrayList<FileNode> videoList = AllMediaList.instance(mContext).getMediaList(deviceType, FileType.VIDEO);
            if (videoList.size() > 0 && !TextUtils.isEmpty(filePath)) {
            	for (FileNode fileNode : videoList) {
            		if (filePath.equals(fileNode.getFilePath())) {
            			mFileNode = fileNode;
            			mFileNode.setPlayTime(playTime);
            			break;
            		}
            	}
            	if (mFileNode == null) {
            		mFileNode = videoList.get(0);
            	}
            }
		}
		setFileNode(mFileNode);
	}
}
