package com.haoke.ui.video;

import com.archermind.skinlib.SkinManager;
import com.haoke.constant.MediaUtil;
import com.haoke.constant.MediaUtil.PlayState;
import com.haoke.mediaservice.R;
import com.haoke.util.Media_IF;

import haoke.ui.util.OnHKTouchListener;
import haoke.ui.util.TOUCH_ACTION;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class VideoPlayTimeSeekBar extends RelativeLayout implements OnSeekBarChangeListener {
    
    // ------------------------------外部接口 start------------------------------
    /**
     * 设置控件的触摸监听函数
     * @param OnHKTouchListener
     */
    public void setHKTouchListener(OnHKTouchListener listener) {
        mTouchedListener = listener;
    }

    /**
     * 更新播放总时间
     */
    public void updateTimeInfo() {
        if (mCanUpdate == false)
            return;
        
        int total = videoController.getDuration();
        mSeekBar.setMax(total);
		setTotalTime(total);
        updateCurTime();
    }
    
    /**
     * 更新当前播放时间
     */
    public void updateCurTime() {
        if (mCanUpdate == false)
            return;

        int time = videoController.getPosition();
        mSeekBar.setProgress(time);
		setCurrTime(time);
		mTimeHandler.removeMessages(MSG_UPDATE_TIME);
		if (videoController.isPlayState()) {
			mTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, 200);
		}
    }
    
    public void setVideoController(VideoPlayController vc){
    	videoController = vc;
    	
    }
    
    // ------------------------------外部接口 end------------------------------

    // 内部变量
    private static final String TAG = "VideoPlayTimeSeekBar";
    //private Video_IF mIF;
    private SeekBar mSeekBar;
    public SeekBar getSeekBar() {
		return mSeekBar;
	}

	private TextView mCurTimeTextView = null;
    private TextView mDurationTextView = null;
    private boolean mCanUpdate = true;
    private OnHKTouchListener mTouchedListener = null;
    private int mTextFormat = 2;
    
    private boolean isTracking;
    private View mTrackViews;
    private ImageView showIcon;
    private TextView showText;
    private TextView durationText;
    private SkinManager skinManager;
    private VideoPlayController videoController;
    
    private int lastProgress;

    public VideoPlayTimeSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        //mIF = Video_IF.getInstance();
    }
    
    public void showTrackView(boolean isFastPre, int position) {
        String positionStr = null;
        if (position > 3600) {
            positionStr = MediaUtil.TimeFormat_HMS(position);
        } else {
            positionStr = MediaUtil.TimeFormat(position);
        }
        Log.e("luke","----position: " + position + " , " + positionStr);
        showText.setText(positionStr);
        int duration = videoController.getDuration();
        String durationStr = null;
        if (duration > 3600) {
        	durationStr  = MediaUtil.TimeFormat_HMS(duration);
        } else {
        	durationStr = MediaUtil.TimeFormat(duration);
        }
        
        Log.e("luke","----duration: " + duration + " , " + durationStr);
        durationText.setText(" / " + durationStr);
        showIcon.setImageResource(isFastPre ? R.drawable.video_ctrl_fastpre : R.drawable.video_ctrl_fastnext);
        mSeekBar.setProgress(position);

        mTrackViews.setVisibility(View.VISIBLE);
        mTimeHandler.removeMessages(MSG_HIDE_TRACK_VIEW);
        mTimeHandler.sendEmptyMessageDelayed(MSG_HIDE_TRACK_VIEW, 1500);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initView();
        skinManager = SkinManager.instance(getContext());
    }
    
    // 初始化控件
    private void initView() {
        mSeekBar = (SeekBar) this.findViewById(R.id.media_seekbar);
        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (mTouchedListener != null) {
		    		int action = event.getAction();
		    		if (action == MotionEvent.ACTION_DOWN) {
		    			mTouchedListener.OnHKTouchEvent(v, TOUCH_ACTION.BTN_DOWN);
		    		} else if (action == MotionEvent.ACTION_CANCEL
		    				|| action == MotionEvent.ACTION_UP) {
		    			mTouchedListener.OnHKTouchEvent(v, TOUCH_ACTION.BTN_UP);
		    		}
		    	}
				return false;
			}
		});
        
        mCurTimeTextView = (TextView) this.findViewById(R.id.video_cur_time);
        mDurationTextView = (TextView) this.findViewById(R.id.video_duration_time);
        
        mTrackViews =  findViewById(R.id.track_view);
        showIcon = (ImageView) findViewById(R.id.toast_show_ico);
        showText = (TextView) findViewById(R.id.toast_show_time);
        durationText = (TextView) findViewById(R.id.toast_show_duration);
    }
    
    public void refreshSkin() {
        mSeekBar.setThumb(skinManager.getDrawable(R.drawable.video_seekbar_block));
        mSeekBar.setProgressDrawable(skinManager.getProgressDrawable(R.drawable.video_seekbar_progress));
        showText.setTextColor(skinManager.getColor(R.color.hk_custom_text_d));
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	if (mTouchedListener != null) {
    		int action = event.getAction();
    		if (action == MotionEvent.ACTION_DOWN) {
    			mTouchedListener.OnHKTouchEvent(this, TOUCH_ACTION.BTN_DOWN);
    		} else if (action == MotionEvent.ACTION_CANCEL
    				|| action == MotionEvent.ACTION_UP) {
    			mTouchedListener.OnHKTouchEvent(this, TOUCH_ACTION.BTN_UP);
    		}
    	}
    	return super.onTouchEvent(event);
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
	
	@Override
	public void setVisibility(int visibility) {
		super.setVisibility(visibility);
		mTimeHandler.removeMessages(MSG_UPDATE_TIME);
		if (visibility == View.VISIBLE) {
			if (videoController.isPlayState()) {
				mTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, 200);
			}
		}
	}
    
    private static final int MSG_UPDATE_TIME = 1;
	private static final int MSG_SEEKBAR_CHANGE = 2;
	private static final int MSG_HIDE_TRACK_VIEW = 3;
	private Handler mTimeHandler = new Handler() {
		public void handleMessage(Message msg) {
			int what = msg.what;
			Log.d(TAG, "mTimeHandler what=" + what);
			removeMessages(what);
			switch (msg.what) {
			case MSG_UPDATE_TIME:
				int time = videoController.getPosition();
		        mSeekBar.setProgress(time);
				setCurrTime(time);
				if (getVisibility() == View.VISIBLE) {
					sendEmptyMessageDelayed(MSG_UPDATE_TIME, 200);
				}
				break;
			case MSG_SEEKBAR_CHANGE:
				int progress = mSeekBar.getProgress();
				videoController.setPosition(progress);
				setCurrTime(progress);
				break;
			case MSG_HIDE_TRACK_VIEW:
				mTrackViews.setVisibility(View.GONE);
				break;
			case 4:
			}
		};
	};
	
	public void checkScroll() {
		mTimeHandler.removeMessages(MSG_SEEKBAR_CHANGE);
		mTimeHandler.sendEmptyMessageDelayed(MSG_SEEKBAR_CHANGE, 500);
	}
	
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		mTimeHandler.sendEmptyMessage(MSG_SEEKBAR_CHANGE);
		if (videoController.isPlayState()) {
			mTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
		}
		isTracking = false;
		mTrackViews.setVisibility(View.GONE);
	}
	
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		mTimeHandler.removeMessages(MSG_UPDATE_TIME);
		isTracking = true;
	}
	
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		setCurrTime(progress);
		lastProgress = progress;
	}
	
	private void setTotalTime(int total) {
		String string = "";
//		if (total > 3600) {
//			mTextFormat = 2;
//		} else {
//			mTextFormat = 1;
//		}
		if (mTextFormat == 1) {
			string = MediaUtil.TimeFormat(total);
        } else if (mTextFormat == 2) {
        	string = MediaUtil.TimeFormat_HMS(total);
        }
		Log.e("luke","setTotalTime: " + total + " , " + string);
		mDurationTextView.setText(string);
	}
	
	private void setCurrTime(int time) {
		String string = "";
		if (mTextFormat == 1) {
			string = MediaUtil.TimeFormat(time);
        } else if (mTextFormat == 2) {
        	string = MediaUtil.TimeFormat_HMS(time);
        }
		Log.e("luke","setCurrTime: " + time + " , " + string);
		mCurTimeTextView.setText(string);
		if (isTracking) {
			showTrackView(lastProgress > time, time);
		}
	}
}
