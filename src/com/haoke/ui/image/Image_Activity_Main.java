package com.haoke.ui.image;

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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import com.amd.util.AmdConfig;
import com.amd.util.SkinManager;
import com.amd.util.SkinManager.SkinListener;
import com.haoke.bean.FileNode;
import com.haoke.bean.StorageBean;
import com.haoke.constant.MediaUtil;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.constant.MediaUtil.PlayState;
import com.haoke.constant.VRConstant.VRIntent;
import com.haoke.data.AllMediaList;
import com.haoke.data.LoadListener;
import com.haoke.data.PlayStateSharedPreferences;
import com.haoke.mediaservice.R;
import com.haoke.service.MediaService;
import com.haoke.ui.media.MediaSearchActivity;
import com.haoke.ui.widget.CustomDialog;
import com.haoke.ui.widget.CustomDialog.DIALOG_TYPE;
import com.haoke.util.DebugLog;
import com.haoke.window.HKWindowManager;

public class Image_Activity_Main extends Activity implements
        OnClickListener, LoadListener, OnCheckedChangeListener {
    
    private int mLayoutProps = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
    public static final String TAG = "ImageActivity";

    private PlayStateSharedPreferences mPlayPreferences;
    
    private PhotoListLayout mListLayout = null;
    private PhotoPlayLayout mPlayLayout = null;
    
    private RadioGroup mRadioGroup;
    private ImageButton mSearchButton;
    private View mEditView;
    private TextView mSelectAllView;
    private TextView mDeleteView;
    private TextView mCancelView;
    private TextView mCopyTextView;
    private String mFilePathFromSearch;
    private boolean isShow;
    
    private ArrayList<FileNode> mImageList = new ArrayList<FileNode>();
    private BroadcastReceiver mPowerRreceiver;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	Log.e(TAG,"--------image onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_activity_main);
        AllMediaList.launcherTocheckAllStorageScanState(this);
        AllMediaList.instance(getApplicationContext()).registerLoadListener(this);
        mPlayPreferences = PlayStateSharedPreferences.instance(getApplicationContext());
        
        getWindow().getDecorView().setSystemUiVisibility(mLayoutProps);

        mListLayout = (PhotoListLayout) findViewById(R.id.image_list_layout);
        mListLayout.setActivityHandler(mHandler, this);
        mPlayLayout = (PhotoPlayLayout) findViewById(R.id.image_play_home);
        mPlayLayout.setActivityHandler(mHandler, this);
        
        mRadioGroup = (RadioGroup) findViewById(R.id.image_tab_group);
        mSearchButton = (ImageButton) findViewById(R.id.image_search_button);
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
        if (AmdConfig.ENABLE_LOCAl_SKIN_MANAGE) {
            findViewById(R.id.textview_skinmanager).setOnClickListener(this);
        }
        skinManager = SkinManager.instance(getApplicationContext());
        localRadioButton = (RadioButton) mRadioGroup.findViewById(R.id.image_device_flash);
        usb1RadioButton = (RadioButton) mRadioGroup.findViewById(R.id.image_device_usb1);
        usb2RadioButton = (RadioButton) mRadioGroup.findViewById(R.id.image_device_usb2);
        collectRadioButton = (RadioButton) mRadioGroup.findViewById(R.id.image_device_collect);
        
        registerReceiver(mOperateAppReceiver, new IntentFilter(VRIntent.ACTION_OPERATE_IMAGE));

        initIntent(getIntent());
        
      //modify bug 20762 begin
        IntentFilter intentFilter=new IntentFilter();
        intentFilter.addAction("power_off");
        intentFilter.addAction("power_on");
        mPowerRreceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                DebugLog.d(TAG, "action =" + action);
                if (action.equals("power_off")) {
                    mPlayLayout.setPlayState(PlayState.PAUSE);
                } else if (action.equals("power_on")) {
                    int fragmentIndex = PlayStateSharedPreferences.instance(context).getImageShowFragment();
                    if (fragmentIndex == SWITCH_TO_PLAY_FRAGMENT) {
                        mPlayLayout.setPlayState(PlayState.PLAY);
                    }
                }
            }
        };
        registerReceiver(mPowerRreceiver, intentFilter);
      //modify bug 20762 end
    }
    
    private void initIntent(Intent intent){
    	Log.e(TAG, "-------- initIntent: " + intent.toString());
        if("MediaSearchActivity".equals(intent.getStringExtra("isfrom"))){ //search入口
        	Log.e(TAG, "onNewIntent MediaSearchActivity");
        	mFilePathFromSearch = intent.getStringExtra("filepath");
        } else if(intent != null && intent.getIntExtra(MediaService.KEY_COMMAND_FROM, 100) == MediaService.VALUE_FROM_VR_APP) { // VR打开图片
        	Log.e(TAG, "initIntent VALUE_FROM_VR_APP");
        	ArrayList<FileNode> imageList = null;
        	int deviceType;
        	imageList = AllMediaList.instance(getApplicationContext()).getMediaList(DeviceType.FLASH, FileType.IMAGE);
        	deviceType = DeviceType.FLASH;
			if (imageList.size() == 0) {
				imageList = AllMediaList.instance(getApplicationContext()).getMediaList(DeviceType.USB1, FileType.IMAGE);
				deviceType = DeviceType.USB1;
				
				if (imageList.size() == 0) {
					imageList = AllMediaList.instance(getApplicationContext()).getMediaList(DeviceType.USB2, FileType.IMAGE);
					deviceType = DeviceType.USB2;
				}
			} 
			if(imageList.size() > 0){
				Log.e(TAG,"initIntent deviceType: " + deviceType);
				updateDevice(deviceType, mListLayout.getPhotoListSize() == 0);
                mPlayLayout.setPlayState(PlayState.PLAY);
                mPlayLayout.setCurrentPosition(0);;
                onChangeFragment(SWITCH_TO_PLAY_FRAGMENT);
			} else {
				Log.e(TAG, "initIntent device have no images!!!!");
				updateDevice(DeviceType.FLASH, false);
				onChangeFragment(SWITCH_TO_LIST_FRAGMENT);
			}
			
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.e(TAG, "--------image onNewIntent");
        initIntent(intent);
    }

    public void updateDevice(final int deviceType, boolean forceUpdate) {
        DebugLog.d(TAG,"updateDevice: " + deviceType);
        int oldDeviceType = mPlayPreferences.getImageDeviceType();
        if (oldDeviceType != deviceType) {
            mPlayPreferences.saveImageDeviceType(deviceType);
        }
        updateRadioGroup(deviceType); // 更新RadioGroup的状态。

        StorageBean storageBean = AllMediaList.instance(getApplicationContext()).getStoragBean(deviceType);
        if (!storageBean.isMounted() || !storageBean.isId3ParseCompleted()) {
            onChangeFragment(SWITCH_TO_LIST_FRAGMENT);
            forceUpdate = true; // 如果未扫描完成或者未解析完成，强制更新。
        }
        if (deviceType == oldDeviceType && !forceUpdate) {
            // 如果deviceType相等，且不强制更新，什么也不做。
        } else {
            mImageList.clear();
            mImageList.addAll(AllMediaList.instance(getApplicationContext())
                    .getMediaList(deviceType, FileType.IMAGE));
            mListLayout.updataList(mImageList, storageBean);
            mPlayLayout.updateList(mImageList, deviceType);
        }
        // refreshView函数中有更新加载框和是否显示无媒体的逻辑。
        mListLayout.refreshView(storageBean);
        if (mImageList.size() == 0 && mListLayout.isEditMode()) {
            cancelEdit();
        }
    }


    @Override
    protected void onResume() {
    	Log.e(TAG,"--------image onResume");
        isShow = true;
        AllMediaList.notifyAllLabelChange(getApplicationContext(), R.string.pub_image);
        if (mFilePathFromSearch != null) {
            int deviceType = MediaUtil.getDeviceType(mFilePathFromSearch);
            int position = 0;
            mPlayPreferences.saveImageDeviceType(deviceType);
            updateDevice(deviceType, mListLayout.getPhotoListSize() == 0);
            for (int index = 0; index < mImageList.size(); index++) {
                if (mFilePathFromSearch.equals(mImageList.get(index).getFilePath())) {
                    position = index;
                    break;
                }
            }
            mPlayLayout.setPlayState(PlayState.PLAY);
            mPlayLayout.setCurrentPosition(position);
            onChangeFragment(SWITCH_TO_PLAY_FRAGMENT);
            mFilePathFromSearch = null;
        } else {
            updateDevice(mPlayPreferences.getImageDeviceType(), mListLayout.getPhotoListSize() == 0);
            mPlayLayout.setPlayState(mPlayLayout.mRecordPlayState);
        }
        mRadioGroup.setOnCheckedChangeListener(this);
        
        refreshSkin(true);
        refreshSkin(false);
        SkinManager.registerSkin(mSkinListener);
        super.onResume();
    }
    
    private SkinManager skinManager;
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
        if(loading || mTextColorStateList == null){//preload
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
        
        if(!loading){//update UI
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
    	Log.e(TAG,"--------image onPause");
    	mPlayLayout.mRecordPlayState = mPlayLayout.mPlayState;
        isShow = false;
        mListLayout.dismissDialog();
        mRadioGroup.setOnCheckedChangeListener(null);
        mPlayLayout.setPlayState(PlayState.PAUSE);
        SkinManager.unregisterSkin(mSkinListener);
        super.onPause();
    }

    @Override
    public void onDestroy() {
    	Log.e(TAG,"--------image onDestroy");
        super.onDestroy();
        AllMediaList.instance(getApplicationContext()).unRegisterLoadListener(this);
        unregisterReceiver(mOperateAppReceiver);
        //modify bug 20762 begin
        unregisterReceiver(mPowerRreceiver);
        //modify bug 20762 begin
    }

    private void updateRadioGroup(final int deviceType) {
        int checkId = R.id.image_device_flash;
        if (deviceType == DeviceType.USB1) {
            checkId = R.id.image_device_usb1;
        } else if (deviceType == DeviceType.USB2) {
            checkId = R.id.image_device_usb2;
        } else if (deviceType == DeviceType.COLLECT) {
            checkId = R.id.image_device_collect;
        }
        if (mRadioGroup.getCheckedRadioButtonId() != checkId) {
            DebugLog.e(TAG,"set mRadioGroup");
            mRadioGroup.check(checkId);
        }
    }

    private int getCurrentDeviceType() {
        int deviceType = DeviceType.FLASH;
        if (mRadioGroup.getCheckedRadioButtonId() == R.id.image_device_usb1) {
            deviceType = DeviceType.USB1;
        } else if (mRadioGroup.getCheckedRadioButtonId() == R.id.image_device_usb2) {
            deviceType = DeviceType.USB2;
        } else if (mRadioGroup.getCheckedRadioButtonId() == R.id.image_device_collect) {
            deviceType = DeviceType.COLLECT;
        }
        return deviceType;
    }

    @Override
    public void onLoadCompleted(int deviceType, int fileType) {
        // 处理数据加载完成的事件: 主要是处理数据。
    	Log.e(TAG,"-----onLoadCompleted deviceType: " + deviceType + " fileType: " + fileType);
        if (deviceType == getCurrentDeviceType() && fileType == FileType.IMAGE) {
            updateDevice(deviceType, true);
        }
    }

    @Override
    public void onScanStateChange(StorageBean storageBean) {
        // 处理磁盘状态 和 扫描状态发生改变的状态： 主要是更新UI的显示效果。
    	Log.e(TAG, "onScanStateChange storageBean: " + storageBean.toString());
        if (storageBean.getDeviceType() == getCurrentDeviceType()) {
            updateDevice(getCurrentDeviceType(), true);
            onChangeFragment(SWITCH_TO_LIST_FRAGMENT);
            if (!storageBean.isMounted()) {
                if (mListLayout != null) {
                    mListLayout.dismissDialog();
                }
                if (isShow) {
                    new CustomDialog().ShowDialog(Image_Activity_Main.this, DIALOG_TYPE.NONE_BTN,
                            R.string.music_device_pullout_usb);
                }
            }
        }
    }
    
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.image_search_button) {
            Intent intent = new Intent(getApplicationContext(), MediaSearchActivity.class);
            intent.putExtra(MediaSearchActivity.INTENT_KEY_FILE_TYPE, FileType.IMAGE);
            startActivity(intent);
        } else if (v.getId() == R.id.edit_delete) {
            mListLayout.deleteSelected(mRadioGroup.getCheckedRadioButtonId() == R.id.image_device_collect);
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
        } else if (v.getId() == R.id.textview_skinmanager) {
            clickCount++;
            if (clickCount % 5 == 0) {
                try {
                    Intent intent = new Intent();
                    String packageName = "com.archermind.skin";
                    String className = "com.archermind.skin.SkinActivity";
                    intent.setClassName(packageName, className);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    int clickCount;
    
    private void cancelEdit() {
        mListLayout.cancelEdit();
        mEditView.setVisibility(View.GONE);
        mRadioGroup.setVisibility(View.VISIBLE);
        mSearchButton.setVisibility(View.VISIBLE);
    }
    
    private void touchEvent(int deviceType) {
        //mPlayPreferences.saveImageDeviceType(deviceType);
        if (mPlayPreferences.getImageShowFragment() != SWITCH_TO_LIST_FRAGMENT) {
            onChangeFragment(SWITCH_TO_LIST_FRAGMENT);
        }
        updateDevice(deviceType, mListLayout.getPhotoListSize() == 0);
    }
    
    @Override
    public void onBackPressed() {
    	Log.e(TAG,"--------image onBackPressed");
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

    public boolean isAllSelect(ArrayList<FileNode> list){
    	if(list.size() <= 0){
    		return false;
    	}
        ArrayList<FileNode> selectList = new ArrayList<FileNode>();
        for (FileNode fileNode : list) {
            if (fileNode.isSelected()) {
                selectList.add(fileNode);
            }
        }
        if(selectList.size() > 0){
        	return true;
        }
        return false;
    }
    
    public Handler getHandler() {
        return mHandler;
    }

	private boolean isAllSelect(){
		if(mImageList.size() <= 0){
			return false;
		}
		for(FileNode filenode : mImageList){
			if(!filenode.isSelected()){
				return false;
			}
		}
		
		return true;
	}
    
    public static final int SWITCH_TO_LIST_FRAGMENT = 0;
    public static final int SWITCH_TO_PLAY_FRAGMENT = 1;
    public static final int CLICK_LIST_ITEM = 3;
    public static final int LONG_CLICK_LIST_ITEM = 4;
    public static final int SHOW_BOTTOM = 5;
    public static final int HIDE_BOTTOM = 6;
    public static final int DISMISS_COPY_DIALOG = 7;
    public static final int CANCEL_EDIT = 1002;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case SWITCH_TO_LIST_FRAGMENT:
            case SWITCH_TO_PLAY_FRAGMENT:
                onChangeFragment(msg.what);
                break;
            case CLICK_LIST_ITEM:
                if (mListLayout.isEditMode()) {
                    mListLayout.selectItem(msg.arg1);
                    if (isAllSelect()) {
                        mSelectAllView.setText(R.string.music_choose_remove);
                    } else {
                        mSelectAllView.setText(R.string.music_choose_all);
                    }
                } else {
                    mPlayLayout.setPlayState(PlayState.PLAY);
                    mPlayLayout.setCurrentPosition(msg.arg1);
                    onChangeFragment(SWITCH_TO_PLAY_FRAGMENT);
                }
                break;
            case LONG_CLICK_LIST_ITEM:
                if (!mListLayout.isEditMode()) {
                    mEditView.setVisibility(View.VISIBLE);
                    if (mRadioGroup.getCheckedRadioButtonId() == R.id.image_device_usb1 || 
                            mRadioGroup.getCheckedRadioButtonId() == R.id.image_device_usb2) {
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
            case SHOW_BOTTOM:
                getWindow().getDecorView()
                        .setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                getWindow().getDecorView().setSystemUiVisibility(mLayoutProps);
                break;
            case HIDE_BOTTOM:
                getWindow().getDecorView()
                .setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                getWindow().getDecorView().setSystemUiVisibility(mLayoutProps);
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
			case CANCEL_EDIT:
				if (mListLayout.isEditMode()) {
					cancelEdit();
				}
				break;
            default:
                break;
            }
            super.handleMessage(msg);
        }
    };
    
    private void onChangeFragment(int index) {
        mPlayPreferences.saveImageShowFragment(index);
        //Thread.dumpStack();
        Log.e(TAG,"onChangeFragment: " + index);
        if (index == SWITCH_TO_PLAY_FRAGMENT) {
            mListLayout.setVisibility(View.INVISIBLE);
            mPlayLayout.setVisibility(View.VISIBLE);
            mPlayLayout.onResume();
            HKWindowManager.hideWallpaper(this);
            HKWindowManager.fullScreen(this, true);
            getWindow().getDecorView()
                    .setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        } else {
            mListLayout.setVisibility(View.VISIBLE);
            mPlayLayout.setVisibility(View.INVISIBLE);
            mPlayLayout.onPause();
            HKWindowManager.showWallpaper(this);
            HKWindowManager.fullScreen(this, false);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            getWindow().getDecorView().setSystemUiVisibility(mLayoutProps);
        }
    }
    
    private BroadcastReceiver mOperateAppReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && VRIntent.ACTION_OPERATE_IMAGE.equals(intent.getAction())) {
                switch (intent.getIntExtra(VRIntent.KEY_IMAGE, 0)) {
                case VRIntent.FINISH_IMAGE:
                    Image_Activity_Main.this.finish();
                    break;
                case VRIntent.PLAY_IMAGE:
                    if (mPlayPreferences.getImageShowFragment() != SWITCH_TO_PLAY_FRAGMENT) {
                        onChangeFragment(SWITCH_TO_PLAY_FRAGMENT);
                    }
                    mPlayLayout.setPlayState(PlayState.PLAY);
                    break;
                case VRIntent.PAUSE_IMAGE:
                    mPlayLayout.setPlayState(PlayState.PAUSE);
                    break;
                case VRIntent.PRE_IMAGE:
                    if (mPlayPreferences.getImageShowFragment() != SWITCH_TO_PLAY_FRAGMENT) {
                        onChangeFragment(SWITCH_TO_PLAY_FRAGMENT);
                    }
                    mPlayLayout.preImage();
                    break;
                case VRIntent.NEXT_IMAGE:
                    if (mPlayPreferences.getImageShowFragment() != SWITCH_TO_PLAY_FRAGMENT) {
                        onChangeFragment(SWITCH_TO_PLAY_FRAGMENT);
                    }
                    mPlayLayout.nextImage();
                    break;
                default:
                    break;
                }
            }
        }
    };

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                mPlayLayout.setPlayState(PlayState.PLAY);
                if (mPlayPreferences.getImageShowFragment() == SWITCH_TO_PLAY_FRAGMENT) {
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                mPlayLayout.setPlayState(PlayState.PAUSE);
                if (mPlayPreferences.getImageShowFragment() == SWITCH_TO_PLAY_FRAGMENT) {
                    return true;
                }
                break;
            default:
                break;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    public static boolean isPlayImage(Context context) {
        int fragmentIndex = PlayStateSharedPreferences.instance(context).getImageShowFragment();
        return (fragmentIndex == SWITCH_TO_PLAY_FRAGMENT) && PhotoPlayLayout.mPlayState == PlayState.PLAY;
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
    	Log.e(TAG,"onCheckedChanged");
        View view = group.findViewById(checkedId);
        switch (view.getId()) {
        case R.id.image_device_flash:
            touchEvent(DeviceType.FLASH);
            break;
        case R.id.image_device_usb1:
            touchEvent(DeviceType.USB1);
            break;
        case R.id.image_device_usb2:
            touchEvent(DeviceType.USB2);
            break;
        case R.id.image_device_collect:
            touchEvent(DeviceType.COLLECT);
            break;
        default:
            break;
        }
    }

    private SkinListener mSkinListener = new SkinListener(new Handler()) {
        @Override
        public void loadingSkinData() {
            refreshSkin(true);
        }

        @Override
        public void refreshViewBySkin() {
            refreshSkin(false);
        };
    };
}
