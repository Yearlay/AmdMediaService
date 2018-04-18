package com.haoke.ui.music;

import java.util.ArrayList;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amd.media.AmdMediaButtonReceiver;
import com.amd.media.MediaInterfaceUtil;
import com.amd.util.Source;
import com.amd.util.SkinManager;
import com.amd.util.SkinManager.SkinListener;
import com.haoke.bean.FileNode;
import com.haoke.constant.MediaUtil;
import com.haoke.constant.MediaUtil.CopyState;
import com.haoke.data.AllMediaList;
import com.haoke.data.ModeSwitch;
import com.haoke.define.GlobalDef;
import com.haoke.constant.MediaUtil.DeleteState;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.constant.MediaUtil.MediaFunc;
import com.haoke.constant.MediaUtil.MediaState;
import com.haoke.constant.MediaUtil.RepeatMode;
import com.haoke.constant.MediaUtil.ScanState;
import com.haoke.mediaservice.R;
import com.haoke.ui.widget.CopyDialog;
import com.haoke.ui.widget.CustomDialog;
import com.haoke.ui.widget.CustomDialog.DIALOG_TYPE;
import com.haoke.ui.widget.CustomDialog.OnDialogListener;
import com.haoke.util.DebugLog;
import com.haoke.util.Media_IF;
import com.haoke.util.Media_Listener;

public class Music_Activity_List extends Activity implements Media_Listener, OnItemClickListener,
        OnDismissListener, OnCancelListener {

    private final String TAG = this.getClass().getSimpleName();
    private int mLayoutProps = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

    private Media_IF mIF;
    
    private CustomDialog mErrorDialog;
    private CustomDialog mDeleteDialog;
    private CustomDialog mDialog;
    private int mDeviceType = DeviceType.NULL;
    
    private View mLoadingLayout = null;
    private ImageView mLoadAnimationView;
    private ImageView mLoadImageView;
    private TextView mLoadTextView;
    
    private View mTipLayout = null;
    private TextView mTipTextView;
    
    private View mListLayout = null;
    private Music_Adapter_List mAdapter = null;
    private ListView mListView;
    private Music_List_Tab mListTab = null;
    
    private CustomDialog mProgressDialog;

    private boolean mEditMode = false;//当前模式：false 列表模式，true 编辑模式
    private boolean mPlayDefault = false;
    private Handler mHandler = new Handler();
    
    private SkinManager skinManager;
    private Drawable mLoadAnimationViewDrawable;
    private boolean isShow = false;
    private CopyDialog mCopyDialog;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DebugLog.i(TAG, " ----- onCreate()");
        setContentView(R.layout.music_activity_list);
        getWindow().getDecorView().setSystemUiVisibility(mLayoutProps);
        
        mIF = Media_IF.getInstance();
        skinManager = SkinManager.instance(getApplicationContext());
        initView();
        
        mIF.initMedia();
        
        init();
        getMusicMode();
        resetDeviceType();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        DebugLog.i(TAG, " ----- onNewIntent() intent == null:" + (intent == null));
        this.setIntent(intent);
        init();
        getMusicMode();
        resetDeviceType();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        DebugLog.d(TAG, "onStop");
      //modify but 21127 begin
//        backToList();
      //modify but 21127 end
    }
    
    private void init() {
        mDeviceType = mIF.getAudioDevice();
    }
    
    private void getMusicMode() {
        Intent intent = getIntent();
        if (intent == null) {
            DebugLog.i(TAG, "getMusicMode musicMode=" + "intent == null");
            return ;
        }
        
        String musicMode = intent.getStringExtra("Mode_To_Music");
        
        if ("hddAudio_intent".equals(musicMode)) {
            mDeviceType = DeviceType.FLASH;
        } else if ("USB1_intent".equals(musicMode)) {
            mDeviceType = DeviceType.USB1;
        } else if ("USB2_intent".equals(musicMode)) {
            mDeviceType = DeviceType.USB2;
        } else if ("COLLECT_intent".equals(musicMode)) {
            mDeviceType = DeviceType.COLLECT;
        }
        
        mPlayDefault = intent.getBooleanExtra("play_music", false);
    }
    
    private void resetDeviceType() {
        exitEditMode();

        mListTab.setLeftName(mDeviceType);
        if (mDeviceType == DeviceType.COLLECT) {//19
            GlobalDef.currentsource = 5;
            mIF.setCurScanner(mDeviceType, FileType.AUDIO);
        } else if (mDeviceType == DeviceType.USB1) {//3
            GlobalDef.currentsource = 24;
            mIF.setCurScanner(mDeviceType, FileType.AUDIO);
        } else if (mDeviceType == DeviceType.USB2) {//4
            GlobalDef.currentsource = 25;
            mIF.setCurScanner(mDeviceType, FileType.AUDIO);
        } else if (mDeviceType == DeviceType.FLASH) {
            //GlobalDef.currentsource = 25;
            mIF.setCurScanner(mDeviceType, FileType.AUDIO);
        }
        
        mAdapter.updateDeviceType(mDeviceType, false);
        setCurPlaySelection(false);
        showListLayout();
        
        if (mErrorDialog != null) {
            mErrorDialog.CloseDialog();
            mErrorDialog = null;
        }
    }
    
    private void initView() {
        mLoadingLayout = findViewById(R.id.music_list_layout_loading);
        mLoadAnimationView = (ImageView) mLoadingLayout.findViewById(R.id.media_loading_imageview);
        mLoadImageView = (ImageView) mLoadingLayout.findViewById(R.id.media_loading_img);
        mLoadTextView = (TextView) mLoadingLayout.findViewById(R.id.media_text);
        
        mTipLayout = findViewById(R.id.music_list_layout_tip);
        mTipTextView = (TextView) mTipLayout.findViewById(R.id.music_tip_text);
        
        mListLayout = findViewById(R.id.music_activity_list);
        mListView = (ListView) mListLayout.findViewById(R.id.music_list_view);
        mListTab = (Music_List_Tab) mListLayout.findViewById(R.id.music_list_tab_id);
        mListTab.setOnTabClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DebugLog.i(TAG, "setOnTabClickListener() v.id = " + v.getId());
                switch (v.getId()) {
                case R.id.music_edit_all:
                    selectAllItems();
                    break;
                case R.id.music_edit_cancle:
                    backToList();
                    break;
                case R.id.music_edit_delect:
                    deleteItems();
                    break;
                case R.id.music_tab_list_id:
                    enterEditMode();
                    break;
                case R.id.copy_to_local:
                    copyItems();
                    break;
                }
            }
        });
        mAdapter = new Music_Adapter_List(this);
        mAdapter.setListView(mListView);
        mListView.setAdapter(mAdapter);
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mListView.setOnItemClickListener(this);
        mListView.setSelector(new ColorDrawable(Color.TRANSPARENT));
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        DebugLog.d(TAG, "onResume");
        isShow = true;
        if (getIntent() != null && "com.haoke.data.ModeSwitch".equals(getIntent().getAction())) {
            ModeSwitch.instance().setGoingFlag(false);
            //setIntent(null);
        }
        mIF.registerLocalCallBack(this); // 注册服务监听
        updateStatus(false, true);
        
        int labelRes = R.string.pub_media;
        int curSource = mDeviceType;
        if (curSource == DeviceType.COLLECT) {
            labelRes = R.string.music_my_save;
        } else if (curSource == DeviceType.USB1) {
            labelRes = R.string.music_usb1_label;
            ModeSwitch.instance().setCurrentMode(this, true, ModeSwitch.MUSIC_USB1_MODE);
        } else if (curSource == DeviceType.USB2) {
            labelRes = R.string.music_usb2_label;
            ModeSwitch.instance().setCurrentMode(this, true, ModeSwitch.MUSIC_USB2_MODE);
        } else if (curSource == DeviceType.FLASH) {
            labelRes = R.string.music_local;
            ModeSwitch.instance().setCurrentMode(this, true, ModeSwitch.MUSIC_LOCAL_MODE);
        }
        AllMediaList.notifyAllLabelChange(getApplicationContext(), labelRes);
        
        playDefault();
        refreshSkin(true);
        refreshSkin(false);
        SkinManager.registerSkin(mSkinListener);
        //getContentResolver().registerContentObserver(MediaInterfaceUtil.URI_SKIN, false, mContentObserver);
    }
    
    private void refreshSkin(boolean loading) {
        mListTab.refreshSkin(loading, skinManager);
        if (loading || mLoadAnimationViewDrawable==null) {
            mLoadAnimationViewDrawable = skinManager.getDrawable(R.drawable.media_loading_anim);
        }
        if (!loading) {
            mLoadAnimationView.setImageDrawable(mLoadAnimationViewDrawable);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        DebugLog.d(TAG, "onPause");
        isShow = false;
        mPlayDefault = false;
        mIF.unregisterLocalCallBack(this); // 注销服务监听
        if (mProgressDialog != null) {
            mProgressDialog.CloseDialog();
        }
        if (mCopyDialog != null) {
            mCopyDialog.closeCopyDialog();
        }
        SkinManager.unregisterSkin(mSkinListener);
        //getContentResolver().unregisterContentObserver(mContentObserver);
        //modify but 21127 begin
        backToList();
        //modify but 21127 end
    }
    
    @Override
    protected void onDestroy() {
        DebugLog.d(TAG, "onDestroy");
        mHandler.removeCallbacksAndMessages(null);
        mShowHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        DebugLog.d(TAG, "onKeyUp keyCode="+keyCode);
        if (AmdMediaButtonReceiver.onKeyUp(this, keyCode)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
    
    @Override
    public void onBackPressed() {
        DebugLog.d(TAG, "onBackPressed");
        if (getIntent() != null && "com.haoke.data.ModeSwitch".equals(getIntent().getAction())) {
            MediaInterfaceUtil.launchLauncherActivity(this);
            setIntent(null);
            finish();
        } else {
            super.onBackPressed();
        }
    }
    
    private void playDefault() {
        if (mPlayDefault) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!mPlayDefault) {
                        return;
                    }
                    if (mIF.isPlayState() && mIF.getPlayingDevice() == mDeviceType) {
                        mPlayDefault = false;
                    } else {
                        int state = mIF.getScanState(mDeviceType);
                        if (state == ScanState.SCANNING || state == ScanState.IDLE) {
                            DebugLog.d(TAG, "playDefault mDeviceType:" + mDeviceType + " is scanning!");
                        } else {
                            if(!mIF.playDefault(mDeviceType, FileType.AUDIO)) {
                                mIF.setAudioSourceAndRequestFocus(mDeviceType);
                            }
                            mPlayDefault = false;
                        }
                    }
                }
            }, 0);
        }
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int index = ev.getActionIndex();
        if (index != 0) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
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
    public void setCurInterface(int data) {
    }

    @Override
    public void onDataChange(int mode, int func, int data1, int data2) {
        if (mode == mIF.getMode()) {
            switch (func) {
            case MediaFunc.DEVICE_CHANGED://8 插拔USB
                deviceChanged(data1, data2);// data2：0 拔出设备，1 插入设备
                break;
            case MediaFunc.SCAN_STATE:
                scanStateChanged(data1);//1 扫描状态改变
                break;
            case MediaFunc.SCAN_ID3_SINGLE_OVER://2
                scanId3Over_Single(data1);
                break;
            case MediaFunc.SCAN_ID3_PART_OVER://3
                scanId3Over_Part();
                break;
            case MediaFunc.SCAN_ID3_ALL_OVER://4
                scanId3Over_All();
                break;
            case MediaFunc.PREPARED://101
                onPrepared();
                break;
            case MediaFunc.ERROR://104
                onError();
                break;
            case MediaFunc.PLAY_OVER://105
                onPlayOver();
                break;
            case MediaFunc.PLAY_STATE://106
                onPlayStateChange(data1, data2);
                break;
            case MediaFunc.DELETE_FILE://7 歌曲删除状态
                updateDeleteState(data1, data2);
                break;
            case MediaUtil.MediaFunc.MEDIA_LIST_UPDATE: //列表有更新
                if (data1 == mDeviceType && data2 == FileType.AUDIO && mIF.getScanState()==ScanState.COMPLETED_ALL) {
                    DebugLog.d(TAG, "onDataChange MEDIA_LIST_UPDATE data1="+data1+"; data2="+data2);
                    refreshList(true, false);
                    playDefault();
                }
                break;
            case MediaUtil.MediaFunc.MEDIA_COPY_FILE: //拷贝文件
                updateCopyState(data1, data2);
                break;
            }
        }
    }
    
    // 更新状态
    private void updateStatus(boolean postFlag, boolean smoothScroll) {
        int scanState = mIF.getScanState();
        DebugLog.v(TAG, "HMI------------updateStatus scanState=" + scanState);
        if (scanState == ScanState.SCANNING || scanState == ScanState.IDLE) { // 扫描中
            showLoadingLayout();
        } else {//离开扫描页面，需要关闭定时器
            if (scanState == ScanState.NO_MEDIA_STORAGE
                    || scanState == ScanState.SCAN_ERROR) { // 无设备
                showNodeviceLayout();
            } else if (scanState == ScanState.COMPLETED_ALL) { // 扫描完成
                showListLayout();
                refreshList(postFlag, smoothScroll);
            }
        } 
    }
    
    private void deviceChanged(int deviceType, int state) {
        if (mDeviceType == deviceType) {
            if (state == 0) { // 无设备 - 拔出设备
                showNodeviceLayout();
                // modify bug 21207 begin
                if (mCopyDialog != null) {
                    mCopyDialog.closeCopyDialog();
                }
                // modify bug 21207 end
                if (isShow) {
                    new CustomDialog().ShowDialog(this, DIALOG_TYPE.NONE_BTN,
                            R.string.music_device_pullout_usb);
                }
            } else {
                mIF.browseDevice(deviceType, FileType.AUDIO);
            }
        }
    }

    private void scanStateChanged(int scanState) {
        updateStatus(false, false);
    }

    private void scanId3Over_Single(int index) {
    }

    private void scanId3Over_Part() {
        if (isVisibility(mListLayout)) {
            updateListWithoutSelection();
        }
    }

    private void scanId3Over_All() {
        if (isVisibility(mListLayout)) {
            updateListWithoutSelection();
        }
    }

    private void onPrepared() {
        // 快速切换曲时，在收到onPrepared()后，mediaState可能已经不是PREPARED状态，需要过滤不处理
        if (mIF.getPlayingDevice() == mDeviceType) {
            int mediaState = mIF.getMediaState();
            if (mediaState != MediaState.PREPARED) {
                return;
            }
            if (isVisibility(mListLayout)) {
                updateListWithoutSelection();
                setCurPlaySelection(true);
            }
        }
    }

    private void onError() {
        if (mIF.getPlayingDevice() == mDeviceType) {
            if (isVisibility(mListLayout)) {
                updateListWithoutSelection();
            }
            if (mDialog == null) {
                mDialog = new CustomDialog();
            }
            mDialog.ShowDialog(this, DIALOG_TYPE.NONE_BTN, R.string.media_play_nosupport);
        }
    }

    private void onPlayOver() {
        if (isVisibility(mListLayout) && mIF.getPlayingDevice() == mDeviceType) {
            updateListWithoutSelection();
        }
    }
    
    private void onPlayStateChange(int data1, int data2) {
        if (isVisibility(mListLayout) && mIF.getPlayingDevice() == mDeviceType) {
            updateListWithoutSelection();
        }
    }
    
    private void selectAllItems(){
        if (isAllItemsSeleted()) {
            mIF.selectAll(false);
            mListTab.updateBtndate(false);
        } else {
            mIF.selectAll(true);
            mListTab.updateBtndate(true);
        }
        notifyDataSetChanged();
    }
    
    //返回音乐列表
    private void backToList() {
        exitEditMode();
    }
    
    private void enterEditMode() {
        mEditMode = true;
        mAdapter.setListType(mEditMode, true);
        mListTab.updateBtndate(false);
        mListTab.updateListTab();
    }
    
    private void exitEditMode() {
        if (mEditMode) {
            for (int pos = 0; pos < mIF.getListTotal(); pos++) {
                if (mIF.isCurItemSelected(pos)) {
                    mIF.selectFile(pos, false);
                }
            }
            mEditMode = false;
            mAdapter.setListType(mEditMode, true);
            mListTab.updateEditTab();
        }
    }
    
    private void updateDeleteState(int data, int data2) {
        if (mDeleteDialog == null) {
            mDeleteDialog = new CustomDialog();
        }
        if (data == DeleteState.DELETING) {
            if (data2 == -1) {
                mDeleteDialog.ShowDialog(this, DIALOG_TYPE.NONE_BTN, R.string.music_delect_wait);
            } else if (data2 == 100) {
                mDeleteDialog.CloseDialog();
            }
        } else if (data == DeleteState.SUCCESS) {
            mListTab.updateBtndate(false);
        } else if (data == DeleteState.FAIL) {
            new CustomDialog().ShowDialog(getApplicationContext(), DIALOG_TYPE.NONE_BTN,
                    R.string.music_delect_error);
        }
    }
    
    private void deleteItems() {
        if (mErrorDialog == null) {
            mErrorDialog = new CustomDialog();
        }
        if (isItemsSeleted()) {
            mErrorDialog.ShowDialog(this, DIALOG_TYPE.TWO_BTN_MSG, R.string.music_delect_ok);
            mErrorDialog.SetDialogListener(new OnDialogListener() {
                @Override
                public void OnDialogEvent(int id) {
                    switch (id) {
                    case R.id.pub_dialog_ok:
                        mIF.deleteStart();
                        break;
                    case R.id.pub_dialog_cancel:
                        break;
                    }
                }
                @Override
                public void OnDialogDismiss() {
                    AllMediaList.instance(Music_Activity_List.this).stopOperateThread();
                    notifyDataSetChanged();
                }
            });
        } else {
            mErrorDialog.ShowDialog(this, DIALOG_TYPE.ONE_BTN, R.string.music_delect_empty);
        }
    }
    
    private void updateCopyState(int data, int data2) {
        DebugLog.v(TAG, "updateCopyState  --> data = "+ data + ";data2 =" + data2);
        if (data == CopyState.COPYING) {
            if (data2 >= 0) {
                if (mCopyDialog != null) {
                    mCopyDialog.updateProgressValue(data2);
                }
            }
            if (data2 == 100) {
                if (mCopyDialog != null) {
                    mCopyDialog.closeCopyDialog();
                }
                for (int pos = 0; pos < mIF.getListTotal(); pos++) {
                    if (mIF.isCurItemSelected(pos)) {
                        mIF.selectFile(pos, false);
                    }
                }
                notifyDataSetChanged();
            }
        } else if (data == CopyState.SUCCESS) {
            mListTab.updateBtndate(false);
            backToList();
        } else if (data == CopyState.FAIL) {
            Toast.makeText(this, R.string.copy_music_file_exception, Toast.LENGTH_SHORT).show();
        }
    }
    
    private void copyItems() {
        final ArrayList<FileNode> audioList = AllMediaList.instance(getApplicationContext())
                .getMediaList(mDeviceType, FileType.AUDIO);
        DebugLog.v(TAG, "copyItems --> mDeviceType ="+ mDeviceType + "; audioList.size() =" + audioList.size());
        if (mCopyDialog != null) {
            mCopyDialog.closeCopyDialog();
        }
        mCopyDialog = new CopyDialog();
        if (AllMediaList.checkSelected(this, audioList)) {
            mCopyDialog.SetDialogListener(new CopyDialog.OnDialogListener() {
                @Override
                public void OnDialogEvent(int id) {
                    switch (id) {
                    case R.id.copy_ok:
                        if (FileNode.existSameNameFile(audioList)) {
                            Toast.makeText(Music_Activity_List.this, R.string.copy_file_error_of_same_name,
                                    Toast.LENGTH_SHORT).show();
                        }
                        mCopyDialog.updateProgressValue(0);
                        doCopy(audioList);
                        break;
                    case R.id.copy_cancel:
                        mCopyDialog.closeCopyDialog();
                        break;
                    }
                }
                @Override
                public void OnDialogDismiss() {
                    AllMediaList.instance(Music_Activity_List.this).stopOperateThread();
                    notifyDataSetChanged();
                }
            });
            mCopyDialog.showCopyDialog(this, audioList, this);
        } else {
            if (mErrorDialog == null) {
                mErrorDialog = new CustomDialog();
            }
            mErrorDialog.ShowDialog(this, DIALOG_TYPE.ONE_BTN, R.string.music_copy_empty);
        }
    }
    
    private void doCopy(ArrayList<FileNode> audioList) {
        if (MediaUtil.checkAvailableSize(audioList)) {
            if (isItemsSeleted()) {
                if (mCopyDialog == null) {
                    mCopyDialog = new CopyDialog();
                }
                mIF.copyStart();
            } else {
                mCopyDialog.closeCopyDialog();
            }
        } else {
            //modify bug 20966 begin
            mCopyDialog.closeCopyDialog();
            //modify bug 20966 end
            new CustomDialog().ShowDialog(this, DIALOG_TYPE.ONE_BTN, R.string.failed_check_available_size);
        }
    }
    
    private void updateListWithoutSelection() {
        int total = mIF.getListTotal();
        if (total <= 0) {
            showEmptyList(false);
            return;
        }
        mAdapter.resetLastPlayItem();
        notifyDataSetChanged();
    }
    
    private boolean isVisibility(View view) {
        return view.getVisibility() == View.VISIBLE;
    }
    
    private void showNodeviceLayout() {
        String text;
        if (mDeviceType == DeviceType.USB2) {
            text = getString(R.string.no_device_usb_two);
        } else {
            text = getString(R.string.no_device_usb_one);
        }
        mTipTextView.setText(text);
        mTipLayout.setVisibility(View.VISIBLE);
        mLoadingLayout.setVisibility(View.GONE);
        mListLayout.setVisibility(View.GONE);
        backToList();
        if (mErrorDialog != null) {
            mErrorDialog.CloseDialog();
            mErrorDialog = null;
        }
    }
    
    private Handler mShowHandler = new Handler();
    
    private void showEmptyList(boolean postFlag) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mTipLayout.setVisibility(View.VISIBLE);
                mTipTextView.setText(R.string.media_no_file);
                mListLayout.setVisibility(View.GONE);
                mLoadingLayout.setVisibility(View.GONE);
                backToList();
            }
        };
        mShowHandler.removeCallbacksAndMessages(null);
        if (postFlag) {
            mShowHandler.postDelayed(runnable, 500);
        } else {
            runnable.run();
        }
    }
    
    private void showLoadingLayout() {
        int drawableId;
        int textId;
        if (mDeviceType == DeviceType.FLASH) {//加载显示
            drawableId = R.drawable.music_loading_usb;
            textId = R.string.music_loading_flash;
        } else if (mDeviceType == DeviceType.USB1) {
            drawableId = R.drawable.music_loading_usb;
            textId = R.string.music_loading_usb1;
        } else {
            drawableId = R.drawable.music_loading_usb;
            textId = R.string.music_loading_usb2;
        }
        mLoadImageView.setImageResource(drawableId);
        mLoadTextView.setText(textId);
        mLoadingLayout.setVisibility(View.VISIBLE);
        mListLayout.setVisibility(View.GONE);
        mTipLayout.setVisibility(View.GONE);
    }
    
    private void showListLayout() {
        mShowHandler.removeCallbacksAndMessages(null);
        mListLayout.setVisibility(View.VISIBLE);
        mTipLayout.setVisibility(View.GONE);
        mLoadingLayout.setVisibility(View.GONE);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        DebugLog.v(TAG, "onItemClick mEditMode="+mEditMode+"; position="+position);
        if (!mEditMode) {//扫描列表
            if (MediaInterfaceUtil.mediaCannotPlay()) {
                return;
            }
            finish();
            int index = mIF.getListItemIndex(position);
            DebugLog.v(TAG, "HMI-----------index= " + index + ", PlayIndex= " + mIF.getPlayIndex()
            + ", repeat:" + mIF.getRepeatMode() + ", random:" + mIF.getRandomMode());
            if (mIF.getRepeatMode() == RepeatMode.OFF) {                    
                mIF.setRepeatMode(RepeatMode.CIRCLE);
            }
            if (index == mIF.getPlayIndex() 
                    && mIF.getPlayingDevice() == mDeviceType
                    && mIF.getPlayingFileType() == FileType.AUDIO
                    && Source.isAudioSource()
                    && mIF.isPlayState()) {
                mIF.setInterface(1);//回播放界面
            } else {
                boolean play = mIF.play(position);
                if (!play) {
                    DebugLog.e(TAG, "onItemClick position="+position+" play error!");
                    return;
                }
            }
            Intent musicIntent = new Intent();
            musicIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            musicIntent.setClassName("com.haoke.mediaservice", "com.haoke.ui.media.Media_Activity_Main");
            musicIntent.putExtra("Mode_To_Music", "music_play_intent");
            startActivity(musicIntent);
        } else if (mEditMode){//编辑列表
            if (mIF.isCurItemSelected(position)) {
                mIF.selectFile(position, false);
            } else {
                mIF.selectFile(position, true);
            }
            mListTab.updateBtndate(isAllItemsSeleted());
            notifyDataSetChanged();
        }
    }
    
    private boolean isItemsSeleted() {
        for (int pos = 0; pos < mIF.getListTotal(); pos++) {
            if (mIF.isCurItemSelected(pos)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isAllItemsSeleted() {
        boolean all = true;
        for (int pos = 0; pos < mIF.getListTotal(); pos++) {
            if (!mIF.isCurItemSelected(pos)) {
                all = false;
                break;
            }
        }
        return all;
    }
    
    private void refreshList(boolean postFlag, boolean smoothScroll) {
        int total = mIF.getListTotal();
        if (total <= 0) {
            showEmptyList(postFlag);
        } else {
            showListLayout();
        }
        mAdapter.updateDeviceType(mDeviceType, true);
        
        // 停止当前正在滚动的列表
//        mListView.dispatchTouchEvent(MotionEvent.obtain(
//                SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
//                MotionEvent.ACTION_CANCEL, 0, 0, 0));
        
        setCurPlaySelection(smoothScroll);
    }
    
    private void setCurPlaySelection(boolean smoothScroll) {
        if (mIF.getPlayingDevice() == mDeviceType && mIF.getPlayingFileType() == FileType.AUDIO) {
            int pos = mIF.getPlayPos();
            if (pos == -1) {                                           
                pos = mIF.getLastPlayItem(mDeviceType, FileType.AUDIO);
            }
            setSelection(pos, smoothScroll);
        } else {
//            if (mPlayDefault) {
//                int index = mIF.getPlayDefaultIndex(mDeviceType, FileType.AUDIO);
//                setSelection(index);
//            } else {
//                setSelection(0);
//            }
            //modify bug 20648 begin
            int index = mIF.getPlayDefaultIndex(mDeviceType, FileType.AUDIO);
            setSelection(index, smoothScroll);
            //modify bug 20648 end
        }
    }
    
    private void setSelection(int index, boolean smoothScroll) {
        if (smoothScroll) {
            if (index < 0) {
                index = 0;
            }
            mListView.requestFocusFromTouch();
            mListView.setSelection(index);
            mListView.smoothScrollToPosition(index);
        }
        notifyDataSetChanged();
    }
    
    private void notifyDataSetChanged() {
        mAdapter.notifyDataSetChangedEx();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        AllMediaList.instance(this).stopOperateThread();
    }
    
    private SkinListener mSkinListener = new SkinListener(new Handler()) {
        @Override
        public void loadingSkinData() {
            refreshSkin(true);
        }

        @Override
        public void refreshViewBySkin() {
            notifyDataSetChanged();
            refreshSkin(false);
        };
    };

    @Override
    public void onCancel(DialogInterface dialog) {
        if (mCopyDialog != null) {
            mCopyDialog.interruptCheckOperator();
        }
        AllMediaList.instance(this).stopOperateThread();
        exitEditMode();
    }
}
