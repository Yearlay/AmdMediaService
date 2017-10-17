package com.haoke.ui.image;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.haoke.bean.FileNode;
import com.haoke.bean.StorageBean;
import com.haoke.constant.MediaUtil;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.constant.VRConstant.VRIntent;
import com.haoke.data.AllMediaList;
import com.haoke.data.LoadListener;
import com.haoke.data.ModeSwitch;
import com.haoke.data.PlayStateSharedPreferences;
import com.haoke.define.MediaDef.PlayState;
import com.haoke.mediaservice.R;
import com.haoke.ui.media.MediaSearchActivity;
import com.haoke.util.DebugLog;

public class Image_Activity_Main extends FragmentActivity implements
        OnClickListener, LoadListener, OnCheckedChangeListener {
    
    private final String TAG = "PhotoApp";
    private int mLayoutProps = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

    public FragmentManager mFragmentManager;
    private PlayStateSharedPreferences mPlayPreferences;
    
    private PhotoListFragment mListFragment = null;
    private PhotoPlayFragment mPlayFragment = null;
    private PhotoDetailFragment mDetailsFragment = null;
    
    private FrameLayout mLayout = null;
    
    private RadioGroup mRadioGroup;
    private ImageButton mSearchButton;
    private View mEditView;
    private TextView mSelectAllView;
    private TextView mCopyTextView;
    
    private ArrayList<FileNode> mImageList = new ArrayList<FileNode>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_activity_main);
        
        AllMediaList.instance(getApplicationContext()).registerLoadListener(this);
        mPlayPreferences = PlayStateSharedPreferences.instance(getApplicationContext());
        
        getWindow().getDecorView().setSystemUiVisibility(mLayoutProps);

        mFragmentManager = this.getSupportFragmentManager();
        mListFragment = new PhotoListFragment();
        mPlayFragment = new PhotoPlayFragment();
        mDetailsFragment = new PhotoDetailFragment();
        mLayout = (FrameLayout) findViewById(R.id.image_fragment);
        
        mRadioGroup = (RadioGroup) findViewById(R.id.image_tab_group);
        mRadioGroup.setOnCheckedChangeListener(this);
        mSearchButton = (ImageButton) findViewById(R.id.image_search_button);
        mSearchButton.setOnClickListener(this);
        
        mEditView = findViewById(R.id.list_edit_view);
        findViewById(R.id.edit_delete).setOnClickListener(this);
        findViewById(R.id.edit_cancel).setOnClickListener(this);
        mSelectAllView = (TextView) mEditView.findViewById(R.id.edit_all);
        mSelectAllView.setOnClickListener(this);
        mCopyTextView = (TextView) mEditView.findViewById(R.id.copy_to_local);
        mCopyTextView.setOnClickListener(this);
        
        registerReceiver(mOperateAppReceiver, new IntentFilter(VRIntent.ACTION_OPERATE_IMAGE));
    }
    
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if ("MediaSearchActivity".equals(intent.getStringExtra("isfrom"))) {
            String filePath = intent.getStringExtra("filepath");
            int deviceType = MediaUtil.getDeviceType(filePath);
            int position = 0;
            updateDevice(deviceType);
            for (int index = 0; index < mImageList.size(); index++) {
                if (filePath.equals(mImageList.get(index).getFilePath())) {
                    position = index;
                    break;
                }
            }
            mPlayPreferences.saveImageDeviceType(deviceType);
            mPlayPreferences.saveImageShowFragment(SWITCH_TO_PLAY_FRAGMENT);
            mPlayPreferences.saveImageCurrentPosition(position);
            mPlayFragment.setPlayState(PlayState.PAUSE);
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
        if (!storageBean.isMounted()) {
            onChangeFragment(SWITCH_TO_LIST_FRAGMENT);
        } else {
            if (storageBean.isId3ParseCompleted()) {
                onChangeFragment(mPlayPreferences.getImageShowFragment());
            } else {
                onChangeFragment(SWITCH_TO_LIST_FRAGMENT);
            }
        }

        mImageList.clear();
        mImageList.addAll(AllMediaList.instance(getApplicationContext())
                .getMediaList(deviceType, FileType.IMAGE));
        mListFragment.updataList(mImageList, storageBean);
        mPlayFragment.updataList(mImageList, deviceType);
        if (mDetailsFragment.getFileNode() == null) {
            int position = mPlayPreferences.getImageCurrentPosition();
            position = position < 0 ? 0 : position;
            position = position >= mImageList.size() ? mImageList.size() - 1 : position; 
            if (position < mImageList.size() && position >= 0) {
                mDetailsFragment.setFileNode(mImageList.get(position));
            }
        }
        if (mImageList.size() == 0 && mListFragment.isEditMode()) {
            cancelEdit();
        }
    }
    
    @Override
    public void onStart() {
        super.onStart();
        updateDevice(mPlayPreferences.getImageDeviceType());
    }

    @Override
    protected void onResume() {
        AllMediaList.notifyAllLabelChange(getApplicationContext(), R.string.pub_image);
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AllMediaList.instance(getApplicationContext()).unRegisterLoadListener(this);
        unregisterReceiver(mOperateAppReceiver);
    }

    // Fragment替换
    private void replaceFragment(Fragment fragment) {
        if (fragment == null)
            return;
        
        try {
            FragmentTransaction transaction = mFragmentManager.beginTransaction();
            if (getCurFragment() != fragment) {
                transaction.replace(R.id.image_fragment, fragment);
            }
            transaction.commitAllowingStateLoss();

        } catch (Exception e) {
        }
        setFragmentParams(fragment);
    }

    // 获取当前Fragment
    private Fragment getCurFragment() {
        Fragment fragment = mFragmentManager.findFragmentById(R.id.image_fragment);
        return fragment;
    }
    
    private void setFragmentParams(Fragment fragment) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        if (fragment == mPlayFragment) {
            params.width = LayoutParams.MATCH_PARENT;
            params.height = LayoutParams.MATCH_PARENT;
            if (fragment == mListFragment) {
                params.topMargin = (int) getResources().getDimension(R.dimen.pub_statusbar_height);
            }
        } else {
            params.width = LayoutParams.MATCH_PARENT;
            params.height = 550;
            params.topMargin = (int) getResources().getDimension(R.dimen.pub_statusbar_height);
        }
        if (mLayout != null) {
            mLayout.setLayoutParams(params);
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
        if (deviceType == getCurrentDeviceType() && fileType == FileType.IMAGE) {
            DebugLog.d(TAG, "onLoadCompleted deviceType: " + deviceType + " && fileType: " + fileType);
            updateDevice(deviceType);
        }
    }

    @Override
    public void onScanStateChange(StorageBean storageBean) {
        // 处理磁盘状态 和 扫描状态发生改变的状态： 主要是更新UI的显示效果。
        if (storageBean.getDeviceType() == getCurrentDeviceType()) {
            updateDevice(getCurrentDeviceType());
            onChangeFragment(SWITCH_TO_LIST_FRAGMENT);
        }
    }
    
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.image_search_button) {
            Intent intent = new Intent(getApplicationContext(), MediaSearchActivity.class);
            intent.putExtra(MediaSearchActivity.INTENT_KEY_FILE_TYPE, FileType.IMAGE);
            startActivity(intent);
        } else if (v.getId() == R.id.edit_delete) {
            mListFragment.deleteSelected(mRadioGroup.getCheckedRadioButtonId() == R.id.image_device_collect);
        } else if (v.getId() == R.id.edit_cancel) {
            cancelEdit();
        } else if (v.getId() == R.id.edit_all) {
            if (AllMediaList.checkSelected(this, mImageList)) {
                mListFragment.unSelectAll();
                mSelectAllView.setText(R.string.music_choose_all);
            } else {
                mListFragment.selectAll();
                mSelectAllView.setText(R.string.music_choose_remove);
            }
        } else if (v.getId() == R.id.copy_to_local) {
            mListFragment.copySelected();
        }
    }
    
    private void cancelEdit() {
        mListFragment.cancelEdit();
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
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Fragment curFragment = getCurFragment();
            if (curFragment == mDetailsFragment || curFragment == mPlayFragment) {
                onChangeFragment(SWITCH_TO_LIST_FRAGMENT);
                return true;
            }
            if (mListFragment.isEditMode()) {
                mListFragment.cancelEdit();
                mEditView.setVisibility(View.GONE);
                mRadioGroup.setVisibility(View.VISIBLE);
                mSearchButton.setVisibility(View.VISIBLE);
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    public Handler getHandler() {
        return mHandler;
    }
    
    public static final int SWITCH_TO_LIST_FRAGMENT = 0;
    public static final int SWITCH_TO_PLAY_FRAGMENT = 1;
    public static final int SWITCH_TO_DETAIL_FRAGMENT = 2;
    public static final int CLICK_LIST_ITEM = 3;
    public static final int LONG_CLICK_LIST_ITEM = 4;
    public static final int SHOW_BOTTOM = 5;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case SWITCH_TO_LIST_FRAGMENT:
            case SWITCH_TO_PLAY_FRAGMENT:
            case SWITCH_TO_DETAIL_FRAGMENT:
                onChangeFragment(msg.what);
                break;
            case CLICK_LIST_ITEM:
                if (mListFragment.isEditMode()) {
                    mListFragment.selectItem(msg.arg1);
                    if (AllMediaList.checkSelected(Image_Activity_Main.this, mImageList)) {
                        mSelectAllView.setText(R.string.music_choose_remove);
                    } else {
                        mSelectAllView.setText(R.string.music_choose_all);
                    }
                } else {
                    onChangeFragment(SWITCH_TO_PLAY_FRAGMENT);
                    mPlayFragment.setCurrentPosition(msg.arg1);
                }
                break;
            case LONG_CLICK_LIST_ITEM:
                // onChangeFragment(SWITCH_TO_DETAIL_FRAGMENT);
                // mDetailsFragment.setFileNode(mListFragment.getFileNode(msg.arg1));
                if (!mListFragment.isEditMode()) {
                    mEditView.setVisibility(View.VISIBLE);
                    if (mRadioGroup.getCheckedRadioButtonId() == R.id.image_device_usb1 || 
                            mRadioGroup.getCheckedRadioButtonId() == R.id.image_device_usb2) {
                        mCopyTextView.setVisibility(View.VISIBLE);
                    } else {
                        mCopyTextView.setVisibility(View.GONE);
                    }
                    mRadioGroup.setVisibility(View.INVISIBLE);
                    mSearchButton.setVisibility(View.INVISIBLE);
                    mListFragment.beginEdit();
                }
                break;
            case SHOW_BOTTOM:
                getWindow().getDecorView()
                        .setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
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
        if (index == SWITCH_TO_PLAY_FRAGMENT) {
            replaceFragment(mPlayFragment);
        } else if (index == SWITCH_TO_DETAIL_FRAGMENT) {
            replaceFragment(mDetailsFragment);
        } else {
            replaceFragment(mListFragment);
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
                    mPlayFragment.setPlayState(PlayState.PLAY);
                    break;
                case VRIntent.PAUSE_IMAGE:
                    mPlayFragment.setPlayState(PlayState.PAUSE);
                    break;
                case VRIntent.PRE_IMAGE:
                    if (mPlayPreferences.getImageShowFragment() != SWITCH_TO_PLAY_FRAGMENT) {
                        onChangeFragment(SWITCH_TO_PLAY_FRAGMENT);
                    }
                    mPlayFragment.preImage();
                    break;
                case VRIntent.NEXT_IMAGE:
                    if (mPlayPreferences.getImageShowFragment() != SWITCH_TO_PLAY_FRAGMENT) {
                        onChangeFragment(SWITCH_TO_PLAY_FRAGMENT);
                    }
                    mPlayFragment.nextImage();
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
                mPlayFragment.setPlayState(PlayState.PLAY);
                if (mPlayPreferences.getImageShowFragment() == SWITCH_TO_PLAY_FRAGMENT) {
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                mPlayFragment.setPlayState(PlayState.PAUSE);
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
        return (fragmentIndex == SWITCH_TO_PLAY_FRAGMENT) && PhotoPlayFragment.mPlayState == PlayState.PLAY;
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
