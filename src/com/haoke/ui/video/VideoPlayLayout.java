package com.haoke.ui.video;

import java.util.ArrayList;

import haoke.ui.util.OnHKTouchListener;
import haoke.ui.util.TOUCH_ACTION;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
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
import com.archermind.skinlib.SkinManager;
import com.haoke.bean.FileNode;
import com.haoke.constant.MediaUtil;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.data.AllMediaList;
import com.haoke.data.OperateListener;
import com.haoke.define.MediaDef.MediaState;
import com.haoke.define.MediaDef.PlayState;
import com.haoke.mediaservice.R;
import com.haoke.video.VideoSurfaceView;
import com.nforetek.bt.res.MsgOutline;

public class VideoPlayLayout extends RelativeLayout implements OnHKTouchListener, View.OnClickListener,
        OperateListener, OnTouchListener, OnGestureListener {
    private Context mContext;
    private RelativeLayout mVideoLayout; // 视频布局框
    private VideoSurfaceView mVideoView;
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
        mFileNode = fileNode;
        Video_IF.getInstance().setVideoView(mVideoView);
        mTitleTextView.setText(mFileNode.getFileName());
        updateCollectView();
        if (mFileNode.isSame(Video_IF.getInstance().getPlayItem()) &&
                Video_IF.getInstance().getMediaState() == MediaState.PREPARED) {
            Video_IF.getInstance().setPlayState(PlayState.PLAY);
        } else {
            Video_IF.getInstance().play(mFileNode);
        }
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
    }
    
    public void updatePlayState(int playState) {
        mPlayImageView.setImageDrawable(skinManager.getDrawable(
                playState == PlayState.PLAY ?
                R.drawable.image_pause_icon_selector : R.drawable.image_play_icon_selector));
        mTimeSeekBar.updateCurTime();
    }
    
    public void updateTimeBar() {
        FileNode fileNode = Video_IF.getInstance().getPlayItem();
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
        mVideoLayout = (RelativeLayout) findViewById(R.id.video_play_layout);
        mGestureDetector = new GestureDetector(this);
        mVideoLayout.setOnTouchListener(this);
        mVideoView = new VideoSurfaceView(mContext);
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
        Video_IF.getInstance().setVideoShow(true);
        if (mFileNode != null) {
            mTitleTextView.setText(mFileNode.getFileName());
        }
        updateCollectView();
        
        if (savePlayState) { // 如果在onPause的时候有保存这个状态。
            savePlayState = false;
            if (mFileNode != null && mFileNode.isSame(Video_IF.getInstance().getPlayItem())) {
                mHandler.sendEmptyMessageDelayed(DELAY_PLAY, 1000);
            }
        }
        updateVideoLayout(true);
        if (!Video_IF.getInstance().isPlayState()) {
            slaverShow(true);
        }
    }
    
    private boolean savePlayState = false;

    public void onPause() {
        Video_IF.getInstance().setVideoShow(false);
        if (mContext == null) {
            return;
        }
        mHandler.removeMessages(DELAY_PLAY);
        if (Video_IF.getInstance().getPlayState() == PlayState.PLAY) {
            savePlayState = true;
            Video_IF.getInstance().setPlayState(PlayState.PAUSE);
        }
        mVideoLayout.removeAllViews();
    }

    public void updateVideoLayout(boolean checkSpeed) {
        mVideoLayout.removeAllViews();
        mVideoLayout.addView(mVideoView);
        if (checkSpeed) {
            checkSpeedAndRefreshView(Video_IF.getCarSpeed());
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
            if (mActivityHandler != null) {
                mActivityHandler.sendEmptyMessage(Video_Activity_Main.PLAY_PRE);
            }
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
            Video_IF.getInstance().changePlayState();
            mPlayImageView.setImageDrawable(skinManager.getDrawable(
                    Video_IF.getInstance().getPlayState() == PlayState.PLAY ?
                    R.drawable.image_pause_icon_selector : R.drawable.image_play_icon_selector));
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
            if (mActivityHandler != null) {
                mActivityHandler.sendEmptyMessage(Video_Activity_Main.PLAY_NEXT);
            }
            break;
        case R.id.collect_video:
            collectOrUncollect();
            break;
        }
        startHideTimer();
    }
    
    private void showToast(boolean isFastPre) {
        int oldPosition = Video_IF.getInstance().getPosition();
        if (oldPosition >= Video_IF.getInstance().getDuration() - 15 && !isFastPre) {
            Toast.makeText(mContext, R.string.video_fastnext_to_end_message, Toast.LENGTH_SHORT).show();
            return;
        }
        int newPosition = oldPosition + (isFastPre ? -30 : 30);
        if (newPosition >= Video_IF.getInstance().getDuration()) {
            newPosition = Video_IF.getInstance().getDuration() - 3;
        }
        if (newPosition <= 0) {
            newPosition = 0;
        }
        Video_IF.getInstance().setPosition(newPosition);
        
        mTimeSeekBar.showTrackView(isFastPre, Video_IF.getInstance().getPosition());
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
        if (Video_IF.getInstance().getPlayState() == PlayState.PLAY) {
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
                Video_IF.getInstance().setPlayState(PlayState.PLAY);
                updatePlayState(Video_IF.getInstance().getPlayState());
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
            int duration = Video_IF.getInstance().getDuration();
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
    
    public void checkSpeedAndRefreshView(float speed) {
        boolean showForbiddenViewFlag = false;
        try {
            boolean sysLimitFlag = Video_IF.limitToPlayVideoWhenDrive();
            boolean speedLimitFlag = (speed >= 20.0f);
            showForbiddenViewFlag = (sysLimitFlag && speedLimitFlag);
        } catch (Exception e) {
            e.printStackTrace();
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
}
