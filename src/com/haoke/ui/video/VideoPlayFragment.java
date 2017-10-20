package com.haoke.ui.video;

import java.util.ArrayList;

import haoke.ui.util.OnHKTouchListener;
import haoke.ui.util.TOUCH_ACTION;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup; 
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amd.media.MediaInterfaceUtil;
import com.haoke.bean.FileNode;
import com.haoke.constant.MediaUtil;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.data.AllMediaList;
import com.haoke.data.OperateListener;
import com.haoke.define.MediaDef.PlayState;
import com.haoke.mediaservice.R;
import com.haoke.util.DebugLog;
import com.haoke.video.VideoSurfaceView;
import com.haoke.window.HKWindowManager;

public class VideoPlayFragment extends Fragment implements OnHKTouchListener, View.OnClickListener,
        OperateListener, OnTouchListener, OnGestureListener {
    private Context mContext;
    private View mRootView;
    private RelativeLayout mVideoLayout; // 视频布局框
    private VideoSurfaceView mVideoView;
    private View mForbiddenView;
    private View mCtrlBar;
    private ImageView mPlayImageView;
    private ImageView mCollectView;
    private TextView mTitleTextView;
    private VideoPlayTimeSeekBar mTimeSeekBar;
    private Handler mActivityHandler;
    private  GestureDetector mGestureDetector;
    
    private FileNode mFileNode;
    
    public void setFileNode(FileNode fileNode) {
        mFileNode = fileNode;
        if (mRootView != null) {
            refreshViewAndPlay();
        }
    }
    
    private void refreshViewAndPlay() {
        mTitleTextView.setText(mFileNode.getFileName());
        mCollectView.setImageResource(mFileNode.getCollect() == 1 ?
                R.drawable.media_collect : R.drawable.media_uncollect);
        if (mFileNode.isSame(Video_IF.getInstance().getPlayItem())) {
            Video_IF.getInstance().setPlayState(PlayState.PLAY);
        } else {
            Video_IF.getInstance().play(mFileNode);
        }
        checkSpeedAndRefreshView();
    }

    public void updateCtrlBar(int playState) {
        if (mPlayImageView != null && mTimeSeekBar != null) {
            mPlayImageView.setImageResource(playState == PlayState.PLAY ?
                    R.drawable.image_pause_icon_selector : R.drawable.image_play_icon_selector);
            mTimeSeekBar.updateCurTime();
        }
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
    }
    
    @Override
    public void onAttach(Activity activity) {
        if (activity instanceof Video_Activity_Main) {
            mActivityHandler = ((Video_Activity_Main) activity).getHandler();
        }
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mContext = getActivity();
        HKWindowManager.hideWallpaper(getActivity());
        
        mRootView = inflater.inflate(R.layout.video_play_fragment, null);
        mVideoLayout = (RelativeLayout) mRootView.findViewById(R.id.video_play_layout);
        mGestureDetector = new GestureDetector(this);
        mVideoLayout.setOnTouchListener(this);
        mVideoView = new VideoSurfaceView(mContext);
        mCtrlBar = mRootView.findViewById(R.id.video_play_ctrlbar);
        mCtrlBar.findViewById(R.id.video_ctrlbar_list).setOnClickListener(this);
        mCtrlBar.findViewById(R.id.video_ctrlbar_pre).setOnClickListener(this);
        mCtrlBar.findViewById(R.id.video_ctrlbar_fastpre).setOnClickListener(this);
        mCtrlBar.findViewById(R.id.video_ctrlbar_fastnext).setOnClickListener(this);
        mCtrlBar.findViewById(R.id.video_ctrlbar_next).setOnClickListener(this);
        mPlayImageView = (ImageView) mCtrlBar.findViewById(R.id.video_ctrlbar_pp);
        mPlayImageView.setOnClickListener(this);
        mTimeSeekBar = (VideoPlayTimeSeekBar) mRootView.findViewById(R.id.video_play_time_seekbar);
        mTimeSeekBar.setHKTouchListener(this);
        mCollectView = (ImageView) mRootView.findViewById(R.id.collect_video);
        mCollectView.setOnClickListener(this);
        mTitleTextView = (TextView) mRootView.findViewById(R.id.title_video);
        mForbiddenView = mRootView.findViewById(R.id.video_play_forbidden);
        mForbiddenView.setOnTouchListener(this);
        
        return mRootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        updateTimeBar();
        Video_IF.getInstance().setVideoView(mVideoView);
        if (mCtrlBar.getVisibility() == View.VISIBLE) {
            startHideTimer();
        }
        if (mFileNode != null) {
            refreshViewAndPlay();
        }
    }
    
    @Override
    public void onResume() {
        mHandler.sendEmptyMessageDelayed(UPDATE_VIEWS, 1000);
        if (mFileNode != null && mCollectView != null) {
            mCollectView.setVisibility(mFileNode.isFromCollectTable() ? View.GONE : View.VISIBLE);
            mCollectView.setImageResource(mFileNode.getCollect() == 1 ? R.drawable.media_collect : R.drawable.media_uncollect);
            mTitleTextView.setText(mFileNode.getFileName());
        }
        HKWindowManager.fullScreen(getActivity(), true);
        getActivity().getWindow().getDecorView()
            .setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        
        if (savePlayState) { // 如果在onPause的时候有保存这个状态。
            savePlayState = false;
            if (mFileNode != null && mFileNode.isSame(Video_IF.getInstance().getPlayItem())) {
                Video_IF.getInstance().setPlayState(PlayState.PLAY);
            }
        }
        super.onResume();
    }
    
    private boolean savePlayState = false;

    @Override
    public void onPause() {
        HKWindowManager.fullScreen(getActivity(), false);
        if (mActivityHandler != null) {
            mActivityHandler.sendEmptyMessage(Video_Activity_Main.SHOW_BOTTOM);
        }
        if (Video_IF.getInstance().getPlayState() == PlayState.PLAY) {
            savePlayState = true;
            Video_IF.getInstance().setPlayState(PlayState.PAUSE);
            updateCtrlBar(PlayState.PAUSE);
            
            mHandler.removeMessages(HIDE_CTRL);
            mCtrlBar.setVisibility(View.VISIBLE);
            mTimeSeekBar.setVisibility(View.VISIBLE);
            mCollectView.setVisibility(mFileNode.isFromCollectTable() ? View.GONE : View.VISIBLE);
            mTitleTextView.setVisibility(View.VISIBLE);
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        mVideoLayout.removeAllViews();
        super.onStop();
    }
    
    public void updateVideoLayout() {
    	checkSpeedAndRefreshView();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        HKWindowManager.showWallpaper(getActivity());
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
            updateCtrlBar(Video_IF.getInstance().getPlayState());
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
                            AllMediaList.instance(mContext).collectMediaFile(mFileNode, VideoPlayFragment.this);
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
                    mCollectView.setImageResource(R.drawable.media_collect);
                }
            } else {
                Toast.makeText(mContext, "收藏视频异常", Toast.LENGTH_SHORT).show();
            }
        } else if (operateValue == OperateListener.OPERATE_UNCOLLECT) {
            if (resultCode == OperateListener.OPERATE_SUCEESS) {
                if (progress == 100) {
                    mCollectView.setImageResource(R.drawable.media_uncollect);
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
                mCollectView.setVisibility(mFileNode.isFromCollectTable() ? View.GONE : View.VISIBLE);
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
    private static final int UPDATE_VIEWS = 2;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case HIDE_CTRL:
                slaverShow(false);
                break;
            case UPDATE_VIEWS:
                updateCtrlBar(Video_IF.getInstance().getPlayState());
                startHideTimer();
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
    
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (mCtrlBar.getVisibility() != View.VISIBLE) {
            slaverShow(true);
        }
        mTimeSeekBar.onStartTrackingTouch(mTimeSeekBar.getSeekBar());
        SeekBar seekBar = mTimeSeekBar.getSeekBar();
        int position = seekBar.getProgress();
        seekBar.setProgress(position - ((int)distanceX));
        mTimeSeekBar.checkScroll();
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        mTimeSeekBar.onStopTrackingTouch(mTimeSeekBar.getSeekBar());
        return true;
    }
    
    private void checkSpeedAndRefreshView() {
        if (mRootView == null) {
            return;
        }
        mVideoLayout.removeAllViews();
        mVideoLayout.addView(mVideoView);
        boolean showVideoFlag = true;
        // TODO 
        boolean limitFlag = Video_IF.limitToPlayVideoWhenDrive();
        DebugLog.d("Yearlay", " checkSpeedAndRefreshView limitFlag: " + limitFlag);
        mForbiddenView.setVisibility(showVideoFlag ? View.GONE : View.VISIBLE);
    }

}
