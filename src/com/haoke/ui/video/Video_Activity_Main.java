package com.haoke.ui.video;

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
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.amd.media.MediaInterfaceUtil;
import com.haoke.bean.FileNode;
import com.haoke.bean.StorageBean;
import com.haoke.constant.MediaUtil;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.constant.VRConstant.VRIntent;
import com.haoke.data.AllMediaList;
import com.haoke.data.LoadListener;
import com.haoke.data.PlayStateSharedPreferences;
import com.haoke.define.MediaDef.MediaFunc;
import com.haoke.define.MediaDef.MediaState;
import com.haoke.define.MediaDef.PlayState;
import com.haoke.mediaservice.R;
import com.haoke.serviceif.CarService_Listener;
import com.haoke.ui.media.MediaSearchActivity;
import com.haoke.ui.widget.CustomDialog;
import com.haoke.ui.widget.CustomDialog.DIALOG_TYPE;
import com.haoke.util.Media_IF;
import com.haoke.util.Media_Listener;

public class Video_Activity_Main extends FragmentActivity implements
        CarService_Listener, Media_Listener, OnClickListener,
        LoadListener, OnCheckedChangeListener{

    private final String TAG = this.getClass().getSimpleName();
    private int mLayoutProps = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
    private Video_IF mIF;
    private FragmentManager mFragmentManager = null;
    private VideoListFragment mListFragment = null;
    private VideoPlayFragment mPlayFragment = null;
    private FrameLayout mLayout = null;
    
    private RadioGroup mRadioGroup;
    private ImageButton mSearchButton;
    private View mEditView;
    private TextView mSelectAllView;
    private TextView mCopyTextView;
    private boolean mPreFlag;

    public PlayStateSharedPreferences mPreferences;
    private ArrayList<FileNode> mVideoList = new ArrayList<FileNode>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "HMI------------onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_activity_main);
        
        AllMediaList.instance(getApplicationContext()).registerLoadListener(this);
        
        getWindow().getDecorView().setSystemUiVisibility(mLayoutProps);
        
        mIF = Video_IF.getInstance();
        mIF.registerCarCallBack(this); // 注册服务监听
        mIF.registerLocalCallBack(this); // 注册服务监听
        mIF.bindCarService();
        mIF.initMedia();
        mPreferences = PlayStateSharedPreferences.instance(getApplicationContext());
        
        mFragmentManager = this.getSupportFragmentManager();
        mListFragment = new VideoListFragment();
        mPlayFragment = new VideoPlayFragment();
        mLayout = (FrameLayout) findViewById(R.id.video_fragment);
        
        mRadioGroup = (RadioGroup) findViewById(R.id.video_tab_group);
        mRadioGroup.setOnCheckedChangeListener(this);
        mSearchButton = (ImageButton) findViewById(R.id.video_search_button);
        mSearchButton.setOnClickListener(this);
        
        mEditView = findViewById(R.id.list_edit_view);
        findViewById(R.id.edit_delete).setOnClickListener(this);
        findViewById(R.id.edit_cancel).setOnClickListener(this);
        mSelectAllView = (TextView) mEditView.findViewById(R.id.edit_all);
        mSelectAllView.setOnClickListener(this);
        mCopyTextView = (TextView) mEditView.findViewById(R.id.copy_to_local);
        mCopyTextView.setOnClickListener(this);

        Media_IF.getInstance().setVideoActivity(this);
        
        registerReceiver(mOperateAppReceiver, new IntentFilter(VRIntent.ACTION_OPERATE_VIDEO));
        
        mPreferences.saveVideoShowFragment(SWITCH_TO_LIST_FRAGMENT);
    }
    
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if ("MediaSearchActivity".equals(intent.getStringExtra("isfrom"))) {
            String filePath = intent.getStringExtra("filepath");
            int deviceType = MediaUtil.getDeviceType(filePath);
            int position = 0;
            updateDevice(deviceType);
            for (int index = 0; index < mVideoList.size(); index++) {
                if (filePath.equals(mVideoList.get(index).getFilePath())) {
                    position = index;
                    break;
                }
            }
            mPreferences.saveVideoDeviceType(deviceType);
            mPreferences.saveVideoShowFragment(SWITCH_TO_PLAY_FRAGMENT);
            mPreferences.saveVideoCurrentPosition(position);
            mPlayFragment.setFileNode(mVideoList.get(position));
        }
    }

    public void updateDevice(final int deviceType) {
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
                onChangeFragment(mPreferences.getVideoShowFragment());
            } else {
                onChangeFragment(SWITCH_TO_LIST_FRAGMENT);
            }
        }
        
        mVideoList.clear();
        mVideoList.addAll(AllMediaList.instance(getApplicationContext())
                .getMediaList(deviceType, FileType.VIDEO));
        mListFragment.updataList(mVideoList, storageBean);
        if (mVideoList.size() == 0 && mListFragment.isEditMode()) {
            cancelEdit();
        }
        if (getCurFragment() == mPlayFragment) {
            mPlayFragment.checkPlayFileNode(mVideoList);
        }
    }
    
    @Override
    public void onStart() {
        super.onStart();
        mIF.registerCarCallBack(this); // 注册服务监听
        mIF.registerLocalCallBack(this); // 注册服务监听
        mIF.setCurScanner(getCurrentDeviceType(), FileType.VIDEO);
        updateDevice(getCurrentDeviceType());
    }
    
    @Override
    protected void onResume() {
        AllMediaList.notifyAllLabelChange(getApplicationContext(), R.string.pub_video);
        super.onResume();
    }

    @Override
    public void onStop() {
        Log.v(TAG, "HMI------------onPause");
        super.onStop();
        mIF.unregisterCarCallBack(this); // 注销服务监听
        mIF.unregisterLocalCallBack(this); // 注销服务监听
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "HMI------------onDestroy");
        super.onDestroy();
        Media_IF.getInstance().setVideoActivity(null);
        AllMediaList.instance(getApplicationContext()).unRegisterLoadListener(this);
        unregisterReceiver(mOperateAppReceiver);
    }

    // Fragment替换
    private void replaceFragment(Fragment fragment) {
        if (fragment == null)
            return;

        try {
            FragmentTransaction transaction = mFragmentManager
                    .beginTransaction();
            if (getCurFragment() != fragment) {
                transaction.replace(R.id.video_fragment, fragment);
            }
            transaction.commitAllowingStateLoss();

        } catch (Exception e) {
        }
        setFragmentparams(fragment);
    }

    private Fragment getCurFragment() {
        Fragment fragment = mFragmentManager.findFragmentById(R.id.video_fragment);
        return fragment;
    }
    
    private void setFragmentparams(Fragment fragment) {
        RelativeLayout.LayoutParams videolParams = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        if (fragment == mPlayFragment) {
            videolParams.width = LayoutParams.MATCH_PARENT;
            videolParams.height = LayoutParams.MATCH_PARENT;
            if (fragment == mListFragment) {
                videolParams.topMargin = (int) getResources()
                        .getDimension(R.dimen.pub_statusbar_height);
            }
        } else {
            videolParams.width = LayoutParams.MATCH_PARENT;
            videolParams.height = 550;
            videolParams.topMargin = (int) getResources().getDimension(R.dimen.pub_statusbar_height);
        }
        if (mLayout != null) {
            mLayout.setLayoutParams(videolParams);
        }
    }

    // ------------------------------回调函数 start------------------------------
    // 车载服务重连回调
    @Override
    public void onServiceConn() {
        mIF.registerCarCallBack(this); // 注册服务监听
        mIF.registerLocalCallBack(this); // 注册服务监听
    }

    @Override public void setCurInterface(int data) {}

    @Override
    public void onDataChange(int mode, int func, int data1, int data2) {
        Log.v(TAG, "HMI------------onDataChange func= " + func+",mode="+mode);
        if (mode == mIF.getMode()) {
            switch (func) {
            case MediaFunc.DEVICE_CHANGED:
            case MediaFunc.SCAN_STATE:
            case MediaFunc.SCAN_ID3_SINGLE_OVER:
            case MediaFunc.SCAN_ID3_PART_OVER:
            case MediaFunc.SCAN_ID3_ALL_OVER:
            case MediaFunc.SCAN_THUMBNAIL_SINGLE_OVER:
            case MediaFunc.SCAN_THUMBNAIL_ALL_OVER:
                break;
            case MediaFunc.PREPARING:
                onPreparing();  // 到播放列表。
                break;
            case MediaFunc.PREPARED:
                onPrepared();
                break;
            case MediaFunc.COMPLETION:
                onCompletion();
                break;
            case MediaFunc.SEEK_COMPLETION:
                onSeekCompletion();
                break;
            case MediaFunc.ERROR:
                onError();
                break;
            case MediaFunc.PLAY_OVER:
                onPlayOver();
                break;
            case MediaFunc.PLAY_STATE:
                playStateChanged(data1);
                break;
            case MediaFunc.REPEAT_MODE:
                repeatModeChanged(data1);
                break;
            case MediaFunc.RANDOM_MODE:
                randomModeChanged(data1);
                break;
            }
        }
    }

    private void onPreparing() {
        onChangeFragment(SWITCH_TO_PLAY_FRAGMENT); // 回播放
    }

    private void onPrepared() {
        Log.v(TAG, "HMI------------onPrepared");
        // 快速切换曲时，在收到onPrepared()后，mediaState可能已经不是PREPARED状态，需要过滤不处理
        int mediaState = mIF.getMediaState();
        if (mediaState != MediaState.PREPARED) {
            return;
        }
        if (getCurFragment() == mPlayFragment) {
            mPlayFragment.updateCtrlBar(mIF.getPlayState());
            mPlayFragment.updateTimeBar();
        }
    }

    private void onCompletion() {}

    private void onSeekCompletion() {}

    private void onError() {
        CustomDialog mDialog = new CustomDialog();
        mDialog.ShowDialog(this, DIALOG_TYPE.NONE_BTN, R.string.media_play_nosupport);
        if (mPlayFragment != null) {
            if (mPreFlag) {
                playPre();
            } else {
                playNext();
            }
            mPlayFragment.updateVideoLayout();
        }
    }

    private void onPlayOver() {
        onChangeFragment(SWITCH_TO_LIST_FRAGMENT); // 回列表
    }

    private void playStateChanged(int state) {
        if (getCurFragment() == mPlayFragment) {
            mPlayFragment.updateCtrlBar(mIF.getPlayState());
        }
    }

    private void repeatModeChanged(int mode) {
        if (getCurFragment() == mPlayFragment) {
            mPlayFragment.updateCtrlBar(mIF.getPlayState());
        }
    }

    private void randomModeChanged(int mode) {
        if (getCurFragment() == mPlayFragment) {
            mPlayFragment.updateCtrlBar(mIF.getPlayState());
        }
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
    
    private void touchEvent(int deviceType) {
        mPreferences.saveVideoDeviceType(deviceType);
        if (mPreferences.getVideoShowFragment() != SWITCH_TO_LIST_FRAGMENT) {
            onChangeFragment(SWITCH_TO_LIST_FRAGMENT);
        }
        mIF.setCurScanner(deviceType, FileType.VIDEO);
        updateDevice(deviceType);
    }
    
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.video_search_button) {
            Intent intent = new Intent(getApplicationContext(), MediaSearchActivity.class);
            intent.putExtra(MediaSearchActivity.INTENT_KEY_FILE_TYPE, FileType.VIDEO);
            startActivity(intent);
        } else if (v.getId() == R.id.edit_delete) { // TODO
            mListFragment.deleteSelected(mRadioGroup.getCheckedRadioButtonId() == R.id.video_device_collect);
        } else if (v.getId() == R.id.edit_cancel) { // TODO
            cancelEdit();
        } else if (v.getId() == R.id.edit_all) { // TODO
            if (AllMediaList.checkSelected(this, mVideoList)) {
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

    @Override
    public void onLoadCompleted(int deviceType, int fileType) {
        // 处理数据加载完成的事件: 主要是处理数据。
        if (deviceType == getCurrentDeviceType() && fileType == FileType.VIDEO) {
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
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Fragment curFragment = getCurFragment();
            if (curFragment == mPlayFragment) {
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
    
    private void playVideo(int position) {
    	if (MediaInterfaceUtil.mediaCannotPlay()) {
    		return;
    	}
        FileNode fileNode = mVideoList.get(position);
        if (!fileNode.isSame(mIF.getPlayItem())) {
            mPreferences.saveVideoCurrentPosition(position);
        }
        onChangeFragment(SWITCH_TO_PLAY_FRAGMENT);
        mPlayFragment.setFileNode(mVideoList.get(position));
    }
    
    private BroadcastReceiver mOperateAppReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && VRIntent.ACTION_OPERATE_VIDEO.equals(intent.getAction())) {
                switch (intent.getIntExtra(VRIntent.KEY_VIDEO, 0)) {
                case VRIntent.FINISH_VIDEO:
                    Video_Activity_Main.this.finish();
                    break;
                case VRIntent.PLAY_VIDEO:
                    if (mPreferences.getVideoShowFragment() != SWITCH_TO_PLAY_FRAGMENT) {
                        onChangeFragment(SWITCH_TO_PLAY_FRAGMENT);
                    }
                    mIF.play(mPreferences.getVideoCurrentPosition());
                    mPlayFragment.updateCtrlBar(mIF.getPlayState());
                    break;
                case VRIntent.PAUSE_VIDEO:
                    mIF.setPlayState(PlayState.PAUSE);
                    mPlayFragment.updateCtrlBar(mIF.getPlayState());
                    break;
                case VRIntent.PRE_VIDEO:
                    if (mPreferences.getVideoShowFragment() != SWITCH_TO_PLAY_FRAGMENT) {
                        onChangeFragment(SWITCH_TO_PLAY_FRAGMENT);
                    }
                    playPre();
                    break;
                case VRIntent.NEXT_VIDEO:
                    if (mPreferences.getVideoShowFragment() != SWITCH_TO_PLAY_FRAGMENT) {
                        onChangeFragment(SWITCH_TO_PLAY_FRAGMENT);
                    }
                    playNext();
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
    
    public static final int SWITCH_TO_LIST_FRAGMENT = 0;
    public static final int SWITCH_TO_PLAY_FRAGMENT = 1;
    public static final int CLICK_LIST_ITEM = 2;
    public static final int LONG_CLICK_LIST_ITEM = 3;
    public static final int PLAY_PRE = 4;
    public static final int PLAY_NEXT = 5;
    public static final int SHOW_BOTTOM = 6;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case SWITCH_TO_LIST_FRAGMENT:
            case SWITCH_TO_PLAY_FRAGMENT:
                onChangeFragment(msg.what);
                break;
            case CLICK_LIST_ITEM:
                if (mListFragment.isEditMode()) {
                    mListFragment.selectItem(msg.arg1);
                    if (AllMediaList.checkSelected(Video_Activity_Main.this, mVideoList)) {
                        mSelectAllView.setText(R.string.music_choose_remove);
                    } else {
                        mSelectAllView.setText(R.string.music_choose_all);
                    }
                } else {
                    playVideo(msg.arg1);
                }
                break;
            case LONG_CLICK_LIST_ITEM:
                if (!mListFragment.isEditMode()) {
                    mEditView.setVisibility(View.VISIBLE);
                    if (mRadioGroup.getCheckedRadioButtonId() == R.id.video_device_usb1 || 
                            mRadioGroup.getCheckedRadioButtonId() == R.id.video_device_usb2) {
                        mCopyTextView.setVisibility(View.VISIBLE);
                    } else {
                        mCopyTextView.setVisibility(View.GONE);
                    }
                    mRadioGroup.setVisibility(View.INVISIBLE);
                    mSearchButton.setVisibility(View.INVISIBLE);
                    mSelectAllView.setText(R.string.music_choose_all);
                    mListFragment.beginEdit();
                }
                break;
            case PLAY_PRE:
                mPreFlag = true;
                playPre();
                break;
            case PLAY_NEXT:
                mPreFlag = false;
                playNext();
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
        mPreferences.saveVideoShowFragment(index);
        if (index == SWITCH_TO_PLAY_FRAGMENT) {
            replaceFragment(mPlayFragment);
        } else {
            replaceFragment(mListFragment);
        }
    }

    private void playPre() {
        int position = mPreferences.getVideoCurrentPosition();
        position--;
        position = (position < 0) ? mVideoList.size() - 1 : position;
        mPreferences.saveVideoCurrentPosition(position);
        mPlayFragment.updateVideoLayout();
        mPlayFragment.setFileNode(mVideoList.get(position));
        mIF.play(position);
    }

    private void playNext() {
        int position = mPreferences.getVideoCurrentPosition();
        position++;
        position = (position >= mVideoList.size()) ? 0 : position;
        mPreferences.saveVideoCurrentPosition(position);
        mPlayFragment.updateVideoLayout();
        mPlayFragment.setFileNode(mVideoList.get(position));
        mIF.play(position);
    }
}
