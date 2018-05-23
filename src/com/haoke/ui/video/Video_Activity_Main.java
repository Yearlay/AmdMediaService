package com.haoke.ui.video;

import java.util.ArrayList;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import com.amd.media.MediaInterfaceUtil;
import com.amd.util.AmdConfig;
import com.amd.util.SkinManager;
import com.amd.util.SkinManager.SkinListener;
import com.haoke.bean.FileNode;
import com.haoke.bean.StorageBean;
import com.haoke.constant.MediaUtil;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.constant.VRConstant.VRIntent;
import com.haoke.data.AllMediaList;
import com.haoke.data.LoadListener;
import com.haoke.data.PlayStateSharedPreferences;
import com.haoke.define.CMSStatusDef.CMSStatusFuc;
import com.haoke.define.ModeDef;
import com.haoke.mediaservice.R;
import com.haoke.service.MediaService;
import com.haoke.ui.media.MediaSearchActivity;
import com.haoke.ui.widget.CustomDialog;
import com.haoke.ui.widget.CustomDialog.DIALOG_TYPE;
import com.haoke.util.DebugLog;
import com.haoke.util.Media_CarListener;
import com.haoke.util.Media_IF;
import com.haoke.window.HKWindowManager;

public class Video_Activity_Main extends Activity implements OnClickListener, LoadListener, OnCheckedChangeListener, Media_CarListener {

	private final String TAG = "Video_Activity_Main";
	private int mLayoutProps = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
	private VideoListLayout mListLayout;
	private VideoPlayLayout mPlayLayout;

	private RadioGroup mRadioGroup;
	private ImageButton mSearchButton;
	private View mEditView;
	private TextView mSelectAllView;
	private TextView mDeleteView;
	private TextView mCancelView;
	private TextView mCopyTextView;
	private boolean mPlaying;
	private boolean isFront;

	public PlayStateSharedPreferences mPreferences;
	private ArrayList<FileNode> mVideoList = new ArrayList<FileNode>();
	private int mCurPosition;

	public void updateCurPosition(int position) {
		mCurPosition = position;
		mCurPosition = (mCurPosition < 0) ? 0 : mCurPosition;
		mCurPosition = (mCurPosition >= mVideoList.size()) ? mVideoList.size() - 1 : mCurPosition;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		DebugLog.e("luke", "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.video_activity_main);
		AllMediaList.launcherTocheckAllStorageScanState(this);
		AllMediaList.instance(getApplicationContext()).registerLoadListener(this);

		getWindow().getDecorView().setSystemUiVisibility(mLayoutProps);

		Media_IF.getInstance().bindCarService();
		mPreferences = PlayStateSharedPreferences.instance(getApplicationContext());

		mListLayout = (VideoListLayout) findViewById(R.id.video_list_layout);
		mListLayout.setActivityHandler(mHandler);
		mPlayLayout = (VideoPlayLayout) findViewById(R.id.video_play_home);
		mPlayLayout.setActivityHandler(mHandler);

		mRadioGroup = (RadioGroup) findViewById(R.id.video_tab_group);
		mRadioGroup.setOnCheckedChangeListener(this);
		int deviceType = mPreferences.getVideoDeviceType();
		if (deviceType == DeviceType.COLLECT) {
			mRadioGroup.check(R.id.video_device_collect);
		} else if (deviceType == DeviceType.USB2) {
			mRadioGroup.check(R.id.video_device_usb2);
		} else if (deviceType == DeviceType.USB1) {
			mRadioGroup.check(R.id.video_device_usb1);
		} else {
			mRadioGroup.check(R.id.video_device_flash);
		}
		mSearchButton = (ImageButton) findViewById(R.id.video_search_button);
		mSearchButton.setOnClickListener(this);

		mEditView = findViewById(R.id.list_edit_view);
		mDeleteView = (TextView) findViewById(R.id.edit_delete);
		mDeleteView.setOnClickListener(this);
		mCancelView = (TextView) findViewById(R.id.edit_cancel);
		mCancelView.setOnClickListener(this);
		mSelectAllView = (TextView) mEditView.findViewById(R.id.edit_all);
		mSelectAllView.setOnClickListener(this);
		mCopyTextView = (TextView) mEditView.findViewById(R.id.copy_to_local);
		mCopyTextView.setOnClickListener(this);

		skinManager = SkinManager.instance(getApplicationContext());
		localRadioButton = (RadioButton) mRadioGroup.findViewById(R.id.video_device_flash);
		usb1RadioButton = (RadioButton) mRadioGroup.findViewById(R.id.video_device_usb1);
		usb2RadioButton = (RadioButton) mRadioGroup.findViewById(R.id.video_device_usb2);
		collectRadioButton = (RadioButton) mRadioGroup.findViewById(R.id.video_device_collect);
		
		registerReceiver(mOperateAppReceiver, new IntentFilter(VRIntent.ACTION_OPERATE_VIDEO));

		mPlaying = false;

		initIntent(getIntent());
	}

	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		initIntent(intent);
	}

	private void initIntent(Intent intent) { // 第三方叫醒播放入口
		DebugLog.e("luke", "onNewIntent: " + intent);
		if (intent != null && "MediaSearchActivity".equals(intent.getStringExtra("isfrom"))) { // search、开机播放
			String filePath = intent.getStringExtra("filepath");
			String flag = intent.getStringExtra("flag");
			if (flag != null && flag.equals("bootStartPlay")) {
				mPlayLayout.getVideoController().playDefaultVideo(true);
			}
			int deviceType = MediaUtil.getDeviceType(filePath);
			int position = 0;
			updateDevice(deviceType, false);
			for (int index = 0; index < mVideoList.size(); index++) {
				if (filePath.equals(mVideoList.get(index).getFilePath())) {
					position = index;
					break;
				}
			}
			mPreferences.saveVideoDeviceType(deviceType);
			mPlayLayout.setBeforePlaystate(true);
			updateCurPosition(position);
			FileNode fileNode = mVideoList.get(mCurPosition);
			mPlayLayout.setFileNode(fileNode);
			DebugLog.e("luke", "onNewInten  MediaSearchActivity deviceType: " + deviceType + "   , fileNode: " + fileNode);
			onChangeFragment(SWITCH_TO_PLAY_FRAGMENT);
		} else if (intent != null && intent.getIntExtra("isfrom", 100) == MediaService.VALUE_FROM_VR_APP) { // VR
			// VR play default file
			DebugLog.e("luke", "onNewInten VR");
			FileNode temp = mPlayLayout.playDefault();
			if (temp == null || temp.getFilePath().length() == 0) {
				updateDevice(DeviceType.FLASH, false);
			} else {
				updateDevice(temp.getDeviceType(), false);
				onChangeFragment(SWITCH_TO_PLAY_FRAGMENT);
			}
		} else if (intent != null && "modeSwitch".equals(intent.getStringExtra("isfrom"))) { // 没有视频文件入口
			DebugLog.e("luke", "onNewInten modeSwitch");
			int deviceType = intent.getIntExtra("deviceType", DeviceType.FLASH);
			updateDevice(deviceType, false);
		}
		DebugLog.e("luke", "BeforePlaystate: " + mPlayLayout.getBeforePlaystate());
	}

	public void updateDevice(final int deviceType, boolean toHead) {
		int checkId = R.id.video_device_flash;
		if (deviceType == DeviceType.USB1) {
			checkId = R.id.video_device_usb1;
		} else if (deviceType == DeviceType.USB2) {
			checkId = R.id.video_device_usb2;
		} else if (deviceType == DeviceType.COLLECT) {
			checkId = R.id.video_device_collect;
		}
		if (mRadioGroup.getCheckedRadioButtonId() != checkId) {
			mRadioGroup.check(checkId);
		}

		StorageBean storageBean = AllMediaList.instance(getApplicationContext()).getStoragBean(deviceType);
		if (!storageBean.isMounted()) {
			onChangeFragment(SWITCH_TO_LIST_FRAGMENT);
		} else {
			if (storageBean.isId3ParseCompleted()) {
				onChangeFragment(mPlaying ? SWITCH_TO_PLAY_FRAGMENT : SWITCH_TO_LIST_FRAGMENT);
			} else {
				onChangeFragment(SWITCH_TO_LIST_FRAGMENT);
			}
		}

		mVideoList.clear();
		mVideoList.addAll(AllMediaList.instance(getApplicationContext()).getMediaList(deviceType, FileType.VIDEO));
		mListLayout.updataList(mVideoList, storageBean, toHead);
		if (mVideoList.size() == 0 && mListLayout.isEditMode()) {
			cancelEdit();
		}
		if (mPlayLayout.getVisibility() == View.VISIBLE) {
			mPlayLayout.checkPlayFileNode(mVideoList);
		}
		DebugLog.i(TAG, "updateDevice deviceType: " + deviceType + " && video size: " + mVideoList.size());
	}

	@Override
	public void onStart() {
		super.onStart();
/*		if(AllMediaList.sCarSpeed == 0) {
			AllMediaList.sCarSpeed = Media_IF.getCarSpeed();
		}*/
		DebugLog.v(TAG, "HMI------------onStart sCarSpeed: " + AllMediaList.sCarSpeed);
		Media_IF.getInstance().registerModeCallBack(this);
		updateDevice(getCurrentDeviceType(), false);
	}

	@Override
	protected void onResume() {
		DebugLog.v(TAG, "HMI------------onResume");
		isFront = true;
		AllMediaList.notifyAllLabelChange(getApplicationContext(), R.string.pub_video);
		if (mPlaying) {
			onChangeFragment(SWITCH_TO_PLAY_FRAGMENT);
		} else {
			onChangeFragment(SWITCH_TO_LIST_FRAGMENT);
		}
		if (mPlayLayout.getVisibility() == View.VISIBLE) {
			mPlayLayout.onResume();
		}
		mPlayLayout.registerMediaButtonReceiver();
		refreshSkin(true);
        refreshSkin(false);
        SkinManager.registerSkin(mSkinListener);
		super.onResume();
	}

	SkinManager skinManager;
    private RadioButton localRadioButton;
    private RadioButton usb1RadioButton;
    private RadioButton usb2RadioButton;
    private RadioButton collectRadioButton;
    private ColorStateList mRadioLocalColorStateList;
    private Drawable mRadioLocalBgDrawable;
    private ColorStateList mRadioUSB1ColorStateList;
    private Drawable mRadioUSB1BgDrawable;
    private ColorStateList mRadioUSB2ColorStateList;
    private Drawable mRadioUSB2BgDrawable;
    private ColorStateList mRadioCollectColorStateList;
    private Drawable mRadioCollectBgDrawable;
    private Drawable mSearchButtonImageDrawable;
    private ColorStateList mTextColorStateList;
	
	private void refreshSkin(boolean loading) {
		if(loading || mTextColorStateList == null){
			mRadioLocalColorStateList = skinManager.getColorStateList(R.drawable.tab_textcolor_selector);
			mRadioLocalBgDrawable = skinManager.getDrawable(R.drawable.tab_backgroud_selector);
			mRadioUSB1ColorStateList = skinManager.getColorStateList(R.drawable.tab_textcolor_selector);
			mRadioUSB1BgDrawable = skinManager.getDrawable(R.drawable.tab_backgroud_selector);
			mRadioUSB2ColorStateList = skinManager.getColorStateList(R.drawable.tab_textcolor_selector);
			mRadioUSB2BgDrawable = skinManager.getDrawable(R.drawable.tab_backgroud_selector);
			mRadioCollectColorStateList = skinManager.getColorStateList(R.drawable.tab_textcolor_selector);
			mRadioCollectBgDrawable = skinManager.getDrawable(R.drawable.tab_backgroud_selector);
        	
        	mSearchButtonImageDrawable = skinManager.getDrawable(R.drawable.media_search_selector);
        	mTextColorStateList = skinManager.getColorStateList(R.drawable.text_color_selector);
		}
		
		if(!loading){
        	localRadioButton.setTextColor(mRadioLocalColorStateList);
        	localRadioButton.setBackgroundDrawable(mRadioLocalBgDrawable);
        	usb1RadioButton.setTextColor(mRadioUSB1ColorStateList);
        	usb1RadioButton.setBackgroundDrawable(mRadioUSB1BgDrawable);
        	usb2RadioButton.setTextColor(mRadioUSB2ColorStateList);
        	usb2RadioButton.setBackgroundDrawable(mRadioUSB2BgDrawable);
        	collectRadioButton.setTextColor(mRadioCollectColorStateList);
        	collectRadioButton.setBackgroundDrawable(mRadioCollectBgDrawable);
        	mSearchButton.setImageDrawable(mSearchButtonImageDrawable);
        	mSelectAllView.setTextColor(mTextColorStateList);
        	mDeleteView.setTextColor(mTextColorStateList);
        	mCancelView.setTextColor(mTextColorStateList);
        	mCopyTextView.setTextColor(mTextColorStateList);
		}
		mListLayout.refreshSkin(loading);
		mPlayLayout.refreshSkin(loading);
	}

	@Override
	protected void onPause() {
		super.onPause();
		isFront = false;
		DebugLog.v("luke", "HMI------------onPause BeforePlaystate: " + mPlayLayout.getBeforePlaystate());
		mRadioGroup.setVisibility(View.GONE);
		mSearchButton.setVisibility(View.GONE);
		if (mPlayLayout.getVisibility() == View.VISIBLE) {
			DebugLog.v(TAG, "HMI------------onPause mPlayLayout VISIBLE");
			mPlayLayout.onPause();

			if (!mPlayLayout.getVideoController().hasAudioFocus()) {
				DebugLog.v(TAG, "HMI------------onPause mPlayLayout not AudioFocus");
				mPlayLayout.setBeforePlaystate(mPlayLayout.getBeforePlaystate());
			} else {
				mPlayLayout.setBeforePlaystate(VideoPlayController.isVideoPlaying);
			}
		}
		DebugLog.v("luke", "HMI------------onPause BeforePlaystate: " + mPlayLayout.getBeforePlaystate());
		mListLayout.dismissDialog();
		SkinManager.unregisterSkin(mSkinListener);
		mPlayLayout.unRegisterMediaButtonReceiver();
		mListLayout.onPause();
	}

	@Override
	public void onStop() {
		DebugLog.v(TAG, "HMI------------onStop");
		super.onStop();
		Media_IF.getInstance().unregisterModeCallBack(this);
	}

	@Override
	public void onDestroy() {
		DebugLog.v(TAG, "HMI------------onDestroy");
		super.onDestroy();
		AllMediaList.instance(getApplicationContext()).unRegisterLoadListener(this);
		unregisterReceiver(mOperateAppReceiver);

	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		int index = ev.getActionIndex();
		if (index != 0) {
			return true;
		}
		try {
			return super.dispatchTouchEvent(ev);
		} catch (Exception e) {
		}
		return true;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		int index = ev.getActionIndex();
		if (index != 0) {
			return true;
		}
		return super.onTouchEvent(ev);
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		View view = group.findViewById(checkedId);
		switch (view.getId()) {
		case R.id.video_device_flash:
			touchEvent(DeviceType.FLASH);
			break;
		case R.id.video_device_usb1:
			touchEvent(DeviceType.USB1);
			break;
		case R.id.video_device_usb2:
			touchEvent(DeviceType.USB2);
			break;
		case R.id.video_device_collect:
			touchEvent(DeviceType.COLLECT);
			break;
		default:
			break;
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		DebugLog.e("luke", "onKeyUp!! keyCode: " + keyCode);
		if (MediaInterfaceUtil.isMediaKeyCode(keyCode) && mPlayLayout.getVideoController().hasAudioFocus()) {
		    mPlayLayout.mediaKeyHandle(getApplicationContext(), keyCode);
		    return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	private void touchEvent(int deviceType) {
		mPreferences.saveVideoDeviceType(deviceType);
		updateDevice(deviceType, true);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.video_search_button) {
			Intent intent = new Intent(getApplicationContext(), MediaSearchActivity.class);
			intent.putExtra(MediaSearchActivity.INTENT_KEY_FILE_TYPE, FileType.VIDEO);
			startActivity(intent);
		} else if (v.getId() == R.id.edit_delete) {
			mListLayout.deleteSelected(mRadioGroup.getCheckedRadioButtonId() == R.id.video_device_collect);
		} else if (v.getId() == R.id.edit_cancel) {
			cancelEdit();
		} else if (v.getId() == R.id.edit_all) {
			if (isAllSelect()) {
				mListLayout.unSelectAll();
				mSelectAllView.setText(R.string.music_choose_all);
			} else {
				mListLayout.selectAll();
				mSelectAllView.setText(R.string.music_choose_remove);
			}
		} else if (v.getId() == R.id.copy_to_local) {
			mListLayout.copySelected();
		}
	}

	private void cancelEdit() {
		mListLayout.cancelEdit();
		mEditView.setVisibility(View.GONE);
		mRadioGroup.setVisibility(View.VISIBLE);
		mSearchButton.setVisibility(View.VISIBLE);
	}

	@Override
	public void onLoadCompleted(int deviceType, int fileType) {
		// 处理数据加载完成的事件: 主要是处理数据。
		DebugLog.e(TAG, "onLoadCompleted deviceType: " + deviceType + "  ,fileType: " + fileType);
		if (deviceType == getCurrentDeviceType() && fileType == FileType.VIDEO /*&& mPlayLayout.getVisibility() != View.VISIBLE*/) {
			updateDevice(deviceType, false);
		}
	}

	@Override
	public void onScanStateChange(StorageBean storageBean) {
		// 处理磁盘状态 和 扫描状态发生改变的状态： 主要是更新UI的显示效果。
		DebugLog.e(TAG, "onScanStateChange storageBean: " + storageBean.toString());
		if (storageBean.getDeviceType() == getCurrentDeviceType()) {
			updateDevice(getCurrentDeviceType(), false);
			onChangeFragment(SWITCH_TO_LIST_FRAGMENT);
			if (!storageBean.isMounted()) {
				if (mListLayout != null) {
					mListLayout.dismissDialog();
				}
				if (isFront) {
					new CustomDialog().ShowDialog(Video_Activity_Main.this, DIALOG_TYPE.NONE_BTN, R.string.music_device_pullout_usb);
				}
			}
		} else if (getCurrentDeviceType() == DeviceType.COLLECT && mPlayLayout.getCurFileNode() != null) {
			if (mPlayLayout.getCurFileNode().getFromDeviceType() == storageBean.getDeviceType()) {
				if (!storageBean.isMounted()) {
					if (mListLayout != null) {
						mListLayout.dismissDialog();
					}
					if (isFront) {
						new CustomDialog().ShowDialog(Video_Activity_Main.this, DIALOG_TYPE.NONE_BTN, R.string.music_device_pullout_usb);
					}
				}
			}
		}
		if (storageBean.getState() == StorageBean.EJECT && mPlayLayout.getVisibility() == View.VISIBLE) {
		    mPlayLayout.checkRemoveStorageEvent(storageBean);
		}
	}

	private int getCurrentDeviceType() {
		int deviceType = DeviceType.FLASH;
		if (mRadioGroup.getCheckedRadioButtonId() == R.id.video_device_usb1) {
			deviceType = DeviceType.USB1;
		} else if (mRadioGroup.getCheckedRadioButtonId() == R.id.video_device_usb2) {
			deviceType = DeviceType.USB2;
		} else if (mRadioGroup.getCheckedRadioButtonId() == R.id.video_device_collect) {
			deviceType = DeviceType.COLLECT;
		}
		return deviceType;
	}

	@Override
	public void onBackPressed() {
	    DebugLog.v(TAG, "HMI-----------onBackPressed---");
		if (mPlayLayout.getVisibility() == View.VISIBLE) {
			onChangeFragment(SWITCH_TO_LIST_FRAGMENT);
		} else if (mListLayout.isEditMode()) {
			mListLayout.cancelEdit();
			mEditView.setVisibility(View.GONE);
			mRadioGroup.setVisibility(View.VISIBLE);
			mSearchButton.setVisibility(View.VISIBLE);
		} else {
			super.onBackPressed();
		}
	}

	private void playVideo(int position) {
		if (MediaInterfaceUtil.mediaCannotPlay()) {
			return;
		}
		mPlayLayout.setBeforePlaystate(true);
		mPlayLayout.mNextPlay = true;
		DebugLog.e(TAG, "playVideo setBeforePlaystate " + mPlayLayout.getBeforePlaystate());
		updateCurPosition(position);
		FileNode fileNode = mVideoList.get(mCurPosition);
		mPlayLayout.setFileNode(fileNode);
		onChangeFragment(SWITCH_TO_PLAY_FRAGMENT);
	}

	private BroadcastReceiver mOperateAppReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			DebugLog.e("luke", "mOperateAppReceiver onReceive");
			if (intent != null && VRIntent.ACTION_OPERATE_VIDEO.equals(intent.getAction())) {
				switch (intent.getIntExtra(VRIntent.KEY_VIDEO, 0)) {
				case VRIntent.FINISH_VIDEO:
					Video_Activity_Main.this.finish();
					break;
				case VRIntent.PLAY_VIDEO:
					/*
					 * if (!mPlaying) {
					 * onChangeFragment(SWITCH_TO_PLAY_FRAGMENT); }
					 * mPlayLayout.updatePlayState(false);
					 * mPlayLayout.playDefault();
					 */
					mPlayLayout.getVideoController().getControllerHandler().sendEmptyMessage(VideoPlayController.VR_PLAY_STATE);
					break;
				case VRIntent.PAUSE_VIDEO:
					// mPlayLayout.getVideoController().playOrPause(false);
					mPlayLayout.getVideoController().getControllerHandler().sendEmptyMessage(VideoPlayController.VR_PAUSE_STATE);
					break;
				case VRIntent.PRE_VIDEO:
					if (!mPlaying) {
						onChangeFragment(SWITCH_TO_PLAY_FRAGMENT);
					}
					mPlayLayout.mNextPlay = false;
					mPlayLayout.getVideoController().playPre(true);
					break;
				case VRIntent.NEXT_VIDEO:
					if (!mPlaying) {
						onChangeFragment(SWITCH_TO_PLAY_FRAGMENT);
					}
					mPlayLayout.mNextPlay = true;
					mPlayLayout.getVideoController().playNext(true);
					break;
				default:
					break;
				}

			}
		}
	};

	public Handler getHandler() {
		return mHandler;
	}
	
	private boolean isAllSelect(){
		if(mVideoList.size() <= 0){
			return false;
		}
		for(FileNode filenode : mVideoList){
			if(!filenode.isSelected()){
				return false;
			}
		}
		
		return true;
	}

	public static final int SWITCH_TO_LIST_FRAGMENT = 0;
	public static final int SWITCH_TO_PLAY_FRAGMENT = 1;
	public static final int CLICK_LIST_ITEM = 2;
	public static final int LONG_CLICK_LIST_ITEM = 3;
	public static final int HIDE_UNSUPPORT_VIEW = 6;
	public static final int DISMISS_COPY_DIALOG = 7;
	public static final int SHOW_FORBIDDEN_VIEW = 8;
	public static final int HIDE_FORBIDDEN_VIEW = 9;
	public static final int FORBIDDEN_VIEW_TEST = 1001;
	public static final int CANCEL_EDIT = 1002;
	public static int mErrorCount = 0;
	public static int speedTest = 0;
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case SWITCH_TO_LIST_FRAGMENT:
			case SWITCH_TO_PLAY_FRAGMENT:
				onChangeFragment(msg.what);
				break;
			case CLICK_LIST_ITEM:
				DebugLog.e("luke", "-----item click, playing!!!!");
				if (mListLayout.isEditMode()) {
					mListLayout.selectItem(msg.arg1);
					if (isAllSelect()) {
						mSelectAllView.setText(R.string.music_choose_remove);
					} else {
						mSelectAllView.setText(R.string.music_choose_all);
					}
				} else {
					playVideo(msg.arg1);
				}
				break;
			case LONG_CLICK_LIST_ITEM:
				if (!mListLayout.isEditMode()) {
					mEditView.setVisibility(View.VISIBLE);
					if (mRadioGroup.getCheckedRadioButtonId() == R.id.video_device_usb1 || mRadioGroup.getCheckedRadioButtonId() == R.id.video_device_usb2) {
						mCopyTextView.setVisibility(View.VISIBLE);
					} else {
						mCopyTextView.setVisibility(View.GONE);
					}
					mRadioGroup.setVisibility(View.INVISIBLE);
					mSearchButton.setVisibility(View.INVISIBLE);
					mSelectAllView.setText(R.string.music_choose_all);
					mListLayout.beginEdit();
				}
				break;
			case HIDE_UNSUPPORT_VIEW:
				mErrorCount++;
				boolean notPlayNext = (mErrorCount >= 5 || mVideoList.size() <= 1);
				DebugLog.d(TAG, "HIDE_UNSUPPORT_VIEW mErrorCount: " + mErrorCount + " && notPlayNext: " + notPlayNext);
				if (mPlayLayout != null) {
					mPlayLayout.hideUnsupportView(notPlayNext);
				}
				if (notPlayNext) {
					mErrorCount = 0;
					onChangeFragment(SWITCH_TO_LIST_FRAGMENT);
				}
				break;
			case DISMISS_COPY_DIALOG:
				if (mListLayout.isEditMode()) {
					if (isAllSelect()) {
						mSelectAllView.setText(R.string.music_choose_remove);
					} else {
						mSelectAllView.setText(R.string.music_choose_all);
					}
				}
				break;
			case SHOW_FORBIDDEN_VIEW:
				if (mPlayLayout != null && AllMediaList.sCarSpeed >= AmdConfig.CAR_SPEED_LIMIT) {
					mPlayLayout.showOrHideForbiddenView(true);
					
				}
				break;
			case HIDE_FORBIDDEN_VIEW:
				if (mPlayLayout != null && AllMediaList.sCarSpeed < AmdConfig.CAR_SPEED_LIMIT) {
					mPlayLayout.showOrHideForbiddenView(false);
					
				}
				break;
			case CANCEL_EDIT:
				if (mListLayout.isEditMode()) {
					cancelEdit();
				}
				break;
			case FORBIDDEN_VIEW_TEST:
				if (speedTest >= 100) {
					speedTest = 0;
				}
				DebugLog.e("luke", "FORBIDDEN_VIEW_TEST " + speedTest);
				onCarDataChange(ModeDef.CMS_STATUS, CMSStatusFuc.CAR_SPEED, speedTest);
				speedTest += 3;
				sendEmptyMessageDelayed(FORBIDDEN_VIEW_TEST, 1000);
				break;
			default:
				break;
			}
			super.handleMessage(msg);
		}
	};

	private void onChangeFragment(int index) {
		DebugLog.d(TAG, "onChangeFragment index: " + index);
		mPlaying = (index == SWITCH_TO_PLAY_FRAGMENT);
		if (index == SWITCH_TO_PLAY_FRAGMENT) {
			mPlayLayout.setVisibility(View.VISIBLE);
			mPlayLayout.onResume();
			mListLayout.setVisibility(View.GONE);

			
			HKWindowManager.hideWallpaper(this);
			HKWindowManager.fullScreen(this, true);
			getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
			 

		} else {
			mPlayLayout.onPause();
			mPlayLayout.setVisibility(View.GONE);
			mListLayout.setVisibility(View.VISIBLE);
			if (!mListLayout.isEditMode()) {
				mRadioGroup.setVisibility(View.VISIBLE);
				if (mSearchButton != null) {
					mSearchButton.setVisibility(View.VISIBLE);
				}
			}

			HKWindowManager.showWallpaper(this);
			HKWindowManager.fullScreen(this, false);
			getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
			getWindow().getDecorView().setSystemUiVisibility(mLayoutProps);

		}
	}

	@Override
	public void onCarDataChange(int mode, int func, int data) {
		if (mode == ModeDef.CMS_STATUS && func == CMSStatusFuc.CAR_SPEED) {
			float speed = data / 100.0f;
			AllMediaList.sCarSpeed = speed;
			DebugLog.d("Yearlay", "onUartDataChange 0D 00 2D current speed: " + speed);
			if (mPlayLayout.isShowForbiddenView()) { // 限制播放视频View，显示状态。
				if (speed < AmdConfig.CAR_SPEED_LIMIT) {
					mHandler.sendEmptyMessageDelayed(HIDE_FORBIDDEN_VIEW, 3000);
				} else {
					mHandler.removeMessages(HIDE_FORBIDDEN_VIEW);
				}
			} else { // 限制播放视频View，不显示状态。
				if (speed >= AmdConfig.CAR_SPEED_LIMIT) {
					mHandler.sendEmptyMessageDelayed(SHOW_FORBIDDEN_VIEW, 3000);
				} else {
					mHandler.removeMessages(SHOW_FORBIDDEN_VIEW);
				}
			}
		}
	}

	@Override
	public void onUartDataChange(int mode, int len, byte[] datas) {
	}

    private SkinListener mSkinListener = new SkinListener(new Handler()) {
        @Override
        public void loadingSkinData() {
        	DebugLog.d(TAG, "Load Resource!!!!!");
            refreshSkin(true);
        }

        @Override
        public void refreshViewBySkin() {
        	DebugLog.d(TAG, "Update UI!!!!");
            refreshSkin(false);
        };
    };
}
