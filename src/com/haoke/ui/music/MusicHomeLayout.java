package com.haoke.ui.music;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amd.bt.BT_IF;
import com.haoke.bean.StorageBean;
import com.haoke.btjar.main.BTDef.BTConnState;
import com.haoke.data.AllMediaList;
import com.haoke.data.LoadListener;
import com.haoke.define.MediaDef.DeviceType;
import com.haoke.define.MediaDef.FileType;
import com.haoke.define.MediaDef.ScanState;
import com.haoke.mediaservice.R;
import com.haoke.ui.media.Media_Activity_Main;
import com.haoke.ui.widget.CustomDialog;
import com.haoke.ui.widget.CustomDialog.DIALOG_TYPE;
import com.haoke.ui.widget.CustomDialog.OnDialogListener;
import com.haoke.util.Media_IF;

public class MusicHomeLayout extends LinearLayout implements OnClickListener,
        OnTouchListener, LoadListener {
	private static final String TAG = "MusicHomeLayout";
	private TextView mCollectTextView = null;
	private TextView mLocalTextView = null;
	private TextView mHistoryTextView = null;
	private CustomDialog mRetryDialog = null;
	private View mLayoutUsb1;
	private View mLayoutUsb2;
	
	private int mSaveCount = 123;
	private int mLocalCount = 123;

    public MusicHomeLayout(Context context) {
    	super(context);
	}
    
    public MusicHomeLayout(Context context, AttributeSet attrs) {
    	super(context, attrs);
    }
    
    public MusicHomeLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Log.d(TAG, "onFinishInflate");
        mCollectTextView = (TextView) findViewById(R.id.music_save_count);
        mLocalTextView = (TextView) findViewById(R.id.music_local_count);
        mHistoryTextView = (TextView) findViewById(R.id.music_bt_device_connect);
        View layoutView = findViewById(R.id.music_layout_enshrine);
        layoutView.setOnClickListener(this);
        layoutView.setOnTouchListener(this);
        layoutView = findViewById(R.id.music_layout_local);
        layoutView.setOnClickListener(this);
        layoutView.setOnTouchListener(this);
        layoutView = findViewById(R.id.music_layout_scan_bt);
        layoutView.setOnClickListener(this);
        layoutView.setOnTouchListener(this);
        mLayoutUsb1 = findViewById(R.id.music_layout_scan_usb);
        mLayoutUsb1.setOnClickListener(this);
        mLayoutUsb1.setOnTouchListener(this);
        mLayoutUsb2 = findViewById(R.id.music_layout_scan_usb2);
        mLayoutUsb2.setOnClickListener(this);
        mLayoutUsb2.setOnTouchListener(this);
    };

    @Override
	protected void onAttachedToWindow() {
    	AllMediaList.instance(getContext()).registerLoadListener(this);
		super.onAttachedToWindow();
	}
    
    @Override
    public void setVisibility(int visibility) {
    	int getVisibility = getVisibility();
    	Log.d(TAG, "setVisibility getVisibility="+getVisibility+"; visibility="+visibility);
    	if (getVisibility() != View.VISIBLE && visibility == View.VISIBLE) {
    		refreshInterface();
    	}
    	super.setVisibility(visibility);
    }

	@Override
	protected void onDetachedFromWindow() {
		if (mRetryDialog != null) {
            mRetryDialog.CloseDialog();
        }
		AllMediaList.instance(getContext()).unRegisterLoadListener(this);
		super.onDetachedFromWindow();
	}
	
	public void onResume() {
		refreshInterface();
	}

    public void setBTConnectedState(int data) {
        mHistoryTextView.setText(data == BTConnState.CONNECTED ?
                R.string.bt_connect_success : R.string.music_scan_bt_free);
    }
    
    public void deviceChanged(int deviceType, boolean noDevice) {
        if (deviceType == DeviceType.USB1) {
        	if (noDevice) {
        		mLayoutUsb1.setBackgroundResource(R.drawable.music_back_ground_gray);
        	} else {
        		mLayoutUsb1.setBackgroundResource(R.drawable.music_back_ground);
        	}
        } else if (deviceType == DeviceType.USB2) {
        	if (noDevice) {
        		mLayoutUsb2.setBackgroundResource(R.drawable.music_back_ground_gray);
        	} else {
        		mLayoutUsb2.setBackgroundResource(R.drawable.music_back_ground);
        	}
        }
        mHandler.removeMessages(GET_COLLECT_SIZE);
        mHandler.sendEmptyMessageDelayed(GET_COLLECT_SIZE, 100);
    }

    @Override
    public void onClick(View v) {
    	int id = v.getId();
    	Log.d(TAG, "onClick id="+id);
        switch (id) {
        case R.id.music_layout_enshrine:
            startListActivity(DeviceType.COLLECT);
            break;
        case R.id.music_layout_local:
        	startListActivity(DeviceType.FLASH);
            //Intent carmusicIntent = getContext().getPackageManager().getLaunchIntentForPackage("cn.com.ecarx.xiaoka.carmusic");
            //getContext().startActivity(carmusicIntent);
            break;
        case R.id.music_layout_scan_bt:
            startBTFragment();
            break;
        case R.id.music_layout_scan_usb:
            startListActivity(DeviceType.USB1);
            break;
        case R.id.music_layout_scan_usb2:
            startListActivity(DeviceType.USB2);
            break;
        default:
            break;
        }
    }
    
    @Override
    public boolean onTouch(View v, MotionEvent event) {
    	int action = event.getActionMasked();
    	Drawable background = null;
    	int alpha = 255;
    	switch (action) {
		case MotionEvent.ACTION_DOWN:
			background = v.getBackground();
			alpha = 128;
			break;
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			background = v.getBackground();
			alpha = 255;
			break;
		}
    	if (background != null) {
        	background.setAlpha(alpha);
    	}
    	return false;
    }
    
    private void refreshInterface() {
        mHistoryTextView.setText(BT_IF.getInstance().getConnState() == BTConnState.DISCONNECTED ?
                R.string.music_scan_bt_free : R.string.bt_connect_success);
        deviceChanged(DeviceType.USB1, Media_IF.getInstance().getScanState(DeviceType.USB1) == ScanState.NO_DEVICE);
        deviceChanged(DeviceType.USB2, Media_IF.getInstance().getScanState(DeviceType.USB2) == ScanState.NO_DEVICE);
	}
    
    private void startBTFragment() {
        if (BT_IF.getInstance().getConnState() == BTConnState.DISCONNECTED) {
            showRetryDialog();
        } else {
            Activity activity = (Activity) getContext();
            if (activity instanceof Media_Activity_Main) {
                ((Media_Activity_Main)activity).replaceBtMusicFragment();
            }
        }
    }

    private void startListActivity(int type) {
        Media_IF.getInstance().setAudioDevice(type);
        Intent intent = new Intent();
        intent.setClass(getContext(), Music_Activity_List.class);
        getContext().startActivity(intent);
    }
    
    private void showRetryDialog() {
        mRetryDialog = new CustomDialog();
        mRetryDialog.ShowDialog(getContext(), DIALOG_TYPE.ONE_BTN, R.string.btmusic_device_disconnected);
        mRetryDialog.SetDialogListener(new OnDialogListener() {
            @Override public void OnDialogDismiss() {}
            @Override
            public void OnDialogEvent(int id) {
            	Intent in = new Intent();
            	in.setClassName("com.haoke.bdset", "com.haoke.bt.ui.BTDevicesActivity");
            	getContext().startActivity(in);
            }
        });
    }
    
    private static final int GET_COLLECT_SIZE = 1;
    private static final int UPDATE_COLLECT_SIZE = 2;
    private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Log.d(TAG, "mHandler handleMessage what="+msg.what);
			switch (msg.what) {
			case GET_COLLECT_SIZE:
				new Thread(new Runnable() {
					@Override
					public void run() {
						int collectSize = Media_IF.getInstance().getMediaListSize(DeviceType.COLLECT, FileType.AUDIO);
						int localSize = Media_IF.getInstance().getMediaListSize(DeviceType.FLASH, FileType.AUDIO);
						mHandler.obtainMessage(UPDATE_COLLECT_SIZE, collectSize, localSize).sendToTarget();
					}
				}).start();
				break;
			case UPDATE_COLLECT_SIZE:
				mSaveCount = msg.arg1;
				mLocalCount = msg.arg2;
	            mCollectTextView.setText(String.format(getResources().getString(R.string.music_shou), mSaveCount));
	            mLocalTextView.setText(String.format(getResources().getString(R.string.music_shou), mLocalCount));
				break;
			}
		}
    };

	@Override
	public void onLoadCompleted(int deviceType, int fileType) {
		if (deviceType == DeviceType.COLLECT && fileType == FileType.AUDIO) {
			mHandler.removeMessages(GET_COLLECT_SIZE);
	        mHandler.sendEmptyMessageDelayed(GET_COLLECT_SIZE, 100);
		}
	}

	@Override
	public void onScanStateChange(StorageBean storageBean) {}
}
