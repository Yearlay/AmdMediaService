package com.haoke.ui.image;

import java.security.spec.MGF1ParameterSpec;
import java.util.ArrayList;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import com.archermind.skinlib.SkinManager;
import com.haoke.bean.FileNode;
import com.haoke.bean.StorageBean;
import com.haoke.constant.MediaUtil;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.constant.VRConstant.VRIntent;
import com.haoke.data.AllMediaList;
import com.haoke.data.LoadListener;
import com.haoke.data.PlayStateSharedPreferences;
import com.haoke.define.MediaDef.PlayState;
import com.haoke.mediaservice.R;
import com.haoke.ui.media.MediaSearchActivity;
import com.haoke.ui.widget.CustomDialog;
import com.haoke.ui.widget.CustomDialog.DIALOG_TYPE;
import com.haoke.window.HKWindowManager;

public class Image_Activity_Main extends Activity implements
        OnClickListener, LoadListener, OnCheckedChangeListener {
    
    private int mLayoutProps = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

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
    
    private ArrayList<FileNode> mImageList = new ArrayList<FileNode>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_activity_main);
        
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
        
        registerReceiver(mOperateAppReceiver, new IntentFilter(VRIntent.ACTION_OPERATE_IMAGE));
        
        checkFromSearchActivity(getIntent());
    }
    
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkFromSearchActivity(intent);
    }

    public void checkFromSearchActivity(Intent intent) {
        if ("MediaSearchActivity".equals(intent.getStringExtra("isfrom"))) {
            mFilePathFromSearch = intent.getStringExtra("filepath");
        }
    }
    
    public void updateDevice(final int deviceType) {
        int checkId = R.id.image_device_flash;
        if (deviceType == DeviceType.USB1) {
            checkId = R.id.image_device_usb1;
        } else if (deviceType == DeviceType.USB2) {
            checkId = R.id.image_device_usb2;
        } else if (deviceType == DeviceType.COLLECT) {
            checkId = R.id.image_device_collect;
        }
        if (mRadioGroup.getCheckedRadioButtonId() != checkId) {
            mRadioGroup.check(checkId);
        }

        StorageBean storageBean = AllMediaList.instance(getApplicationContext()).getStoragBean(deviceType);
        mImageList.clear();
        mImageList.addAll(AllMediaList.instance(getApplicationContext())
                .getMediaList(deviceType, FileType.IMAGE));
        mListLayout.updataList(mImageList, storageBean);
        mPlayLayout.updateList(mImageList, deviceType);
        if (mImageList.size() == 0 && mListLayout.isEditMode()) {
            cancelEdit();
        }
        
        if (!storageBean.isMounted() || !storageBean.isId3ParseCompleted()) {
            onChangeFragment(SWITCH_TO_LIST_FRAGMENT);
        }
    }

    @Override
    protected void onResume() {
        AllMediaList.notifyAllLabelChange(getApplicationContext(), R.string.pub_image);
        if (mFilePathFromSearch != null) {
            int deviceType = MediaUtil.getDeviceType(mFilePathFromSearch);
            int position = 0;
            mPlayPreferences.saveImageDeviceType(deviceType);
            updateDevice(deviceType);
            for (int index = 0; index < mImageList.size(); index++) {
                if (mFilePathFromSearch.equals(mImageList.get(index).getFilePath())) {
                    position = index;
                    break;
                }
            }
            mPlayLayout.setPlayState(PlayState.PAUSE);
            mPlayLayout.setCurrentPosition(position);
            onChangeFragment(SWITCH_TO_PLAY_FRAGMENT);
            mFilePathFromSearch = null;
        } else {
            updateDevice(mPlayPreferences.getImageDeviceType());
        }
        mRadioGroup.setOnCheckedChangeListener(this);
        
        refreshSkin();
        super.onResume();
    }
    
    private void refreshSkin() {
        SkinManager skinManager = SkinManager.instance(getApplicationContext());
        RadioButton localRadioButton = (RadioButton) mRadioGroup.findViewById(R.id.image_device_flash);
        localRadioButton.setTextColor(skinManager.getColorStateList(R.drawable.tab_textcolor_selector));
        localRadioButton.setBackgroundDrawable(skinManager.getStateListDrawable(R.drawable.tab_backgroud_selector));
        RadioButton usb1RadioButton = (RadioButton) mRadioGroup.findViewById(R.id.image_device_usb1);
        usb1RadioButton.setTextColor(skinManager.getColorStateList(R.drawable.tab_textcolor_selector));
        usb1RadioButton.setBackgroundDrawable(skinManager.getStateListDrawable(R.drawable.tab_backgroud_selector));
        RadioButton usb2RadioButton = (RadioButton) mRadioGroup.findViewById(R.id.image_device_usb2);
        usb2RadioButton.setTextColor(skinManager.getColorStateList(R.drawable.tab_textcolor_selector));
        usb2RadioButton.setBackgroundDrawable(skinManager.getStateListDrawable(R.drawable.tab_backgroud_selector));
        RadioButton collectRadioButton = (RadioButton) mRadioGroup.findViewById(R.id.image_device_collect);
        collectRadioButton.setTextColor(skinManager.getColorStateList(R.drawable.tab_textcolor_selector));
        collectRadioButton.setBackgroundDrawable(skinManager.getStateListDrawable(R.drawable.tab_backgroud_selector));
        mSearchButton.setImageDrawable(skinManager.getStateListDrawable(R.drawable.media_search_selector));
        mSelectAllView.setTextColor(skinManager.getColorStateList(R.drawable.text_color_selector));
        mDeleteView.setTextColor(skinManager.getColorStateList(R.drawable.text_color_selector));
        mCancelView.setTextColor(skinManager.getColorStateList(R.drawable.text_color_selector));
        mCopyTextView.setTextColor(skinManager.getColorStateList(R.drawable.text_color_selector));
        mListLayout.refreshSkin();
        mPlayLayout.refreshSkin();
    }

    @Override
    protected void onPause() {
        mListLayout.dismissDialog();
        mRadioGroup.setOnCheckedChangeListener(null);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AllMediaList.instance(getApplicationContext()).unRegisterLoadListener(this);
        unregisterReceiver(mOperateAppReceiver);
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
        if (deviceType == getCurrentDeviceType() && fileType == FileType.IMAGE) {
            updateDevice(deviceType);
        }
    }

    @Override
    public void onScanStateChange(StorageBean storageBean) {
        // 处理磁盘状态 和 扫描状态发生改变的状态： 主要是更新UI的显示效果。
        if (storageBean.getDeviceType() == getCurrentDeviceType()) {
            updateDevice(getCurrentDeviceType());
            onChangeFragment(SWITCH_TO_LIST_FRAGMENT);
            if (!storageBean.isMounted()) {
                if (mListLayout != null) {
                    mListLayout.dismissDialog();
                }
                new CustomDialog().ShowDialog(Image_Activity_Main.this, DIALOG_TYPE.NONE_BTN,
                        R.string.music_device_pullout_usb);
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
            if (AllMediaList.checkSelected(this, mImageList)) {
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
    
    private void touchEvent(int deviceType) {
        mPlayPreferences.saveImageDeviceType(deviceType);
        if (mPlayPreferences.getImageShowFragment() != SWITCH_TO_LIST_FRAGMENT) {
            onChangeFragment(SWITCH_TO_LIST_FRAGMENT);
        }
        updateDevice(deviceType);
    }
    
    @Override
    public void onBackPressed() {
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

    public Handler getHandler() {
        return mHandler;
    }
    
    public static final int SWITCH_TO_LIST_FRAGMENT = 0;
    public static final int SWITCH_TO_PLAY_FRAGMENT = 1;
    public static final int CLICK_LIST_ITEM = 3;
    public static final int LONG_CLICK_LIST_ITEM = 4;
    public static final int SHOW_BOTTOM = 5;
    public static final int HIDE_BOTTOM = 6;
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
                    if (AllMediaList.checkSelected(Image_Activity_Main.this, mImageList)) {
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
            default:
                break;
            }
            super.handleMessage(msg);
        }
    };
    
    private void onChangeFragment(int index) {
        mPlayPreferences.saveImageShowFragment(index);
        Thread.dumpStack();
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
}
