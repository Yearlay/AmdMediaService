package com.haoke.ui.music;

import java.util.ArrayList;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amd.media.MediaInterfaceUtil;
import com.haoke.bean.FileNode;
import com.haoke.constant.MediaUtil;
import com.haoke.constant.MediaUtil.CopyState;
import com.haoke.data.AllMediaList;
import com.haoke.data.ModeSwitch;
import com.haoke.define.GlobalDef;
import com.haoke.define.ModeDef;
import com.haoke.define.MediaDef.DeleteState;
import com.haoke.define.MediaDef.DeviceType;
import com.haoke.define.MediaDef.FileType;
import com.haoke.define.MediaDef.MediaFunc;
import com.haoke.define.MediaDef.MediaState;
import com.haoke.define.MediaDef.RepeatMode;
import com.haoke.define.MediaDef.ScanState;
import com.haoke.mediaservice.R;
import com.haoke.ui.widget.CustomDialog;
import com.haoke.ui.widget.CustomDialog.DIALOG_TYPE;
import com.haoke.ui.widget.CustomDialog.OnDialogListener;
import com.haoke.util.Media_IF;
import com.haoke.util.Media_Listener;

public class Music_Activity_List extends Activity implements Media_Listener, OnItemClickListener, OnDismissListener {

    private final String TAG = this.getClass().getSimpleName();
    private int mLayoutProps = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

    private Media_IF mIF;
    
    private CustomDialog mErrorDialog;
    private CustomDialog mDeleteDialog;
    private CustomDialog mDialog;
    private int mDeviceType = DeviceType.NULL;
    
    private View mLoadingLayout = null;
    private ImageView mLoadImageView;
    private TextView mLoadTextView;
    
    private View mNoDeviceLayout = null;
    private TextView mNodeviceTextView;
    
    private View mListLayout = null;
    private Music_Adapter_List mAdapter = null;
    private ListView mListView;
    private View mEmptyTip;
    private Music_List_Tab mListTab = null;
    
    private CustomDialog mProgressDialog;

    private int mType = 0;//当前模式：0 列表模式，1 编辑模式
    private boolean mPlayDefault = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, " ----- onCreate()");
        setContentView(R.layout.music_activity_list);
        getWindow().getDecorView().setSystemUiVisibility(mLayoutProps);
        
        mIF = Media_IF.getInstance();
        initView();
        
        mIF.initMedia();
        
        init();
        getMusicMode();
        resetDeviceType();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.i(TAG, " ----- onNewIntent() intent == null:" + (intent == null));
        this.setIntent(intent);
        init();
        getMusicMode();
        resetDeviceType();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        backToList();
    }
    
    private void init() {
        mDeviceType = mIF.getAudioDevice();
    }
    
    private void getMusicMode() {
        Intent intent = getIntent();
        if (intent == null) {
            Log.i(TAG, "getMusicMode musicMode=" + "intent == null");
            return ;
        }
        
        String musicMode = intent.getStringExtra("Mode_To_Music");
        
        if ("hddAudio_intent".equals(musicMode)) {
            mDeviceType = DeviceType.FLASH;
        } else if ("USB1_intent".equals(musicMode)) {
            mDeviceType = DeviceType.USB1;
        } else if ("USB2_intent".equals(musicMode)) {
            mDeviceType = DeviceType.USB2;
        }
        
        mPlayDefault = intent.getBooleanExtra("play_music", false);
    }
    
    private void resetDeviceType() {
        if (mType == 1) {
            for (int pos = 0; pos < mIF.getListTotal(); pos++) {
                if (mIF.isCurItemSelected(pos)) {
                    mIF.selectFile(pos, false);
                }
            }
            mType = 0;
            mAdapter.setListType(mType);
        }

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
        
        mAdapter.updateList();
        mListTab.updateEditTab();
        showListLayout();
    }
    
    private void initView() {
        mLoadingLayout = findViewById(R.id.music_list_layout_loading);
        mLoadImageView = (ImageView) mLoadingLayout.findViewById(R.id.media_loading_img);
        mLoadTextView = (TextView) mLoadingLayout.findViewById(R.id.media_text);
        
        mNoDeviceLayout = findViewById(R.id.music_list_layout_nodevice);
        mNodeviceTextView = (TextView) mNoDeviceLayout.findViewById(R.id.music_no_device_text);
        
        mListLayout = findViewById(R.id.music_activity_list);
        mEmptyTip = mListLayout.findViewById(R.id.music_list_empty);
        mListView = (ListView) mListLayout.findViewById(R.id.music_list_view);
        mListTab = (Music_List_Tab) mListLayout.findViewById(R.id.music_list_tab_id);
        mListTab.setOnTabClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "setOnTabClickListener() v.id = " + v.getId());
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
                    mType = 1;
                    mAdapter.setListType(mType);
                    mListTab.updateBtndate(false);
                    mAdapter.updateList();
                    mListTab.updateListTab();
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
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (getIntent() != null && "com.haoke.data.ModeSwitch".equals(getIntent().getAction())) {
            ModeSwitch.instance().setGoingFlag(false);
            //setIntent(null);
        }
        mIF.registerLocalCallBack(this); // 注册服务监听
        updateStatus();
        
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
        
        if (mPlayDefault) {
            if (mIF.isPlayState() && mIF.getPlayingDevice() == mDeviceType) {
            } else {
                mIF.playDefault(mDeviceType, FileType.AUDIO);
            }
            mPlayDefault = false;
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        mIF.unregisterLocalCallBack(this); // 注销服务监听
        if (mProgressDialog != null && mProgressDialog.getDialog() != null &&
                mProgressDialog.getDialog().isShowing()) {
            mProgressDialog.CloseDialog();
            Toast.makeText(this, R.string.file_operate_cancel, Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && 
                getIntent() != null && "com.haoke.data.ModeSwitch".equals(getIntent().getAction())) {
            MediaInterfaceUtil.launchLauncherActivity(this);
            setIntent(null);
            finish();
            return true;
        }
        return super.onKeyUp(keyCode, event);
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
            case MediaFunc.DELETE_FILE://7 歌曲删除状态
                updateDeleteState(data1, data2);
                break;
            case MediaUtil.MediaFuncEx.MEDIA_LIST_UPDATE: //列表有更新
                if (data1 == mDeviceType && data2 == FileType.AUDIO && mIF.getScanState()==ScanState.COMPLETED_ALL) {
                    Log.d(TAG, "onDataChange MEDIA_LIST_UPDATE data1="+data1+"; data2="+data2);
                    refreshList();
                }
                break;
            case MediaUtil.MediaFuncEx.MEDIA_COPY_FILE: //拷贝文件
                updateCopyState(data1, data2);
                break;
            }
        }
    }
    
    // 更新状态
    private void updateStatus() {
        int scanState = mIF.getScanState();
        Log.v(TAG, "HMI------------updateStatus scanState=" + scanState);
        if (scanState == ScanState.SCANNING || scanState == ScanState.IDLE) { // 扫描中
            showLoadingLayout();
        } else {//离开扫描页面，需要关闭定时器
            if (scanState == ScanState.NO_DEVICE
                    || scanState == ScanState.SCAN_ERROR) { // 无设备
                showNodeviceLayout();
            } else if (scanState == ScanState.COMPLETED_PART
                    || scanState == ScanState.COMPLETED_ALL) { // 扫描完成
                showListLayout();
                refreshList();
            }
        } 
    }
    
    private void deviceChanged(int deviceType, int state) {
        if (mIF.getMediaDevice() == deviceType) {
            if (state == 0) { // 无设备 - 拔出设备
                showNodeviceLayout();
            } else {
                mIF.browseDevice(deviceType, FileType.AUDIO);
            }
        } else if (mIF.getMediaDevice() == DeviceType.NULL) { // 当前没有浏览设备
            if (state == 1) {//插入设备
                mIF.browseDevice(deviceType, FileType.AUDIO);
            }
        }
    }

    private void scanStateChanged(int scanState) {
        updateStatus();
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
    
    private void selectAllItems(){
        if (isItemsSeleted()) {
            mIF.selectAll(false);
        } else {
            mIF.selectAll(true);
        }
        mListTab.updateBtndate(isItemsSeleted());
        mAdapter.updateList();
    }
    
    //返回音乐列表
    private void backToList() {
        for (int pos = 0; pos < mIF.getListTotal(); pos++) {
            if (mIF.isCurItemSelected(pos)) {
                mIF.selectFile(pos, false);
            }
        }
        mType = 0;
        mAdapter.setListType(mType);
        mAdapter.updateList();
        mListTab.updateEditTab();
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
                }
            });
        } else {
            mErrorDialog.ShowDialog(this, DIALOG_TYPE.ONE_BTN, R.string.music_delect_empty);
        }
    }
    
    private void updateCopyState(int data, int data2) {
        if (data == CopyState.COPYING) {
            if (data2 >= 0) {
                if (mProgressDialog != null) {
                    mProgressDialog.updateProgressValue(data2);
                }
            }
            if (data2 == 100) {
                if (mProgressDialog != null) {
                    mProgressDialog.CloseDialog();
                }
                for (int pos = 0; pos < mIF.getListTotal(); pos++) {
                    if (mIF.isCurItemSelected(pos)) {
                        mIF.selectFile(pos, false);
                    }
                }
                mAdapter.notifyDataSetChanged();
            }
        } else if (data == CopyState.SUCCESS) {
            mListTab.updateBtndate(false);
        } else if (data == CopyState.FAIL) {
            Toast.makeText(this, "拷贝音乐文件异常", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void copyItems() {
        final ArrayList<FileNode> audioList = AllMediaList.instance(getApplicationContext())
                .getMediaList(mDeviceType, FileType.AUDIO);
        if (mErrorDialog == null) {
            mErrorDialog = new CustomDialog();
        }
        if (AllMediaList.checkSelected(this, audioList)) {
            mErrorDialog.SetDialogListener(new OnDialogListener() {
                @Override
                public void OnDialogEvent(int id) {
                    switch (id) {
                    case R.id.pub_dialog_ok:
                    	if (FileNode.existSameNameFile(audioList)) {
                            Toast.makeText(Music_Activity_List.this, R.string.copy_file_error_of_same_name,
                                    Toast.LENGTH_SHORT).show();
                        }
                        doCopy(audioList);
                        break;
                    case R.id.pub_dialog_cancel:
                        break;
                    }
                }
                @Override
                public void OnDialogDismiss() {
                    mAdapter.notifyDataSetChanged();
                }
            });
            mErrorDialog.showCoverDialog(this, audioList);
        } else {
            mErrorDialog.ShowDialog(this, DIALOG_TYPE.ONE_BTN, R.string.music_copy_empty);
        }
    }
    
    private void doCopy(ArrayList<FileNode> audioList) {
        if (MediaUtil.checkAvailableSize(audioList)) {
            if (isItemsSeleted()) {
                if (mProgressDialog == null) {
                    mProgressDialog = new CustomDialog();
                }
                mProgressDialog.showProgressDialog(this, R.string.copy_audio_progress_title, this);
                mIF.copyStart();
            }
        } else {
            new CustomDialog().ShowDialog(this, DIALOG_TYPE.ONE_BTN, R.string.failed_check_available_size);
        }
    }
    
    public void updateListWithoutSelection() {
        int total = mIF.getListTotal();
        if (total <= 0) {
            try {
                mEmptyTip.setVisibility(View.VISIBLE);
                mListView.setVisibility(View.GONE);
                mListTab.setVisibility(View.GONE);
                backToList();
            } catch (Exception e) {
            }
            return;
        }
        mAdapter.updateList();
    }
    
    private boolean isVisibility(View view) {
        return view.getVisibility() == View.VISIBLE;
    }
    
    private void hideAllLayout() {
        mLoadingLayout.setVisibility(View.GONE);
        mNoDeviceLayout.setVisibility(View.GONE);
        mListLayout.setVisibility(View.GONE);
    }
    
    private void showNodeviceLayout() {
        String text;
        if (mDeviceType == DeviceType.USB2) {
            text = getString(R.string.no_device_usb_two);
        } else {
            text = getString(R.string.no_device_usb_one);
        }
        mNodeviceTextView.setText(text);
        hideAllLayout();
        mNoDeviceLayout.setVisibility(View.VISIBLE);
        backToList();
        if (mErrorDialog != null) {
            mErrorDialog.CloseDialog();
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
        hideAllLayout();
        mLoadingLayout.setVisibility(View.VISIBLE);
    }
    
    private void showListLayout() {
        hideAllLayout();
        mListLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        Log.v(TAG, "onItemClick mType="+mType+"; position="+position);
        if (mType == 0) {//扫描列表
            if (MediaInterfaceUtil.mediaCannotPlay()) {
                return;
            }
            finish();
            int index = mIF.getListItemIndex(position);
            Log.v(TAG, "HMI-----------index= " + index + ", PlayIndex= " + mIF.getPlayIndex()
            + ", repeat:" + mIF.getRepeatMode() + ", random:" + mIF.getRandomMode());
            if (mIF.getRepeatMode() == RepeatMode.OFF) {                    
                mIF.setRepeatMode(RepeatMode.CIRCLE);
            }
            if (index == mIF.getPlayIndex() 
                    && mIF.getPlayingDevice() == mIF.getMediaDevice()
                    && mIF.getPlayingFileType() == mIF.getMediaFileType()
                    && mIF.getCurSource() == ModeDef.AUDIO) {
                mIF.setInterface(1);//回播放界面
            } else {
                mIF.play(position);
            }
            Intent musicIntent = new Intent();
            musicIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            musicIntent.setClassName("com.haoke.mediaservice", "com.haoke.ui.media.Media_Activity_Main");
            musicIntent.putExtra("Mode_To_Music", "music_play_intent");
            startActivity(musicIntent);
        } else if (mType == 1){//编辑列表
            if (mIF.isCurItemSelected(position)) {
                mIF.selectFile(position, false);
            } else {
                mIF.selectFile(position, true);
            }
            
            mListTab.updateBtndate(isItemsSeleted());
            mAdapter.notifyDataSetChanged();
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
    
    private void refreshList() {
        int total = mIF.getListTotal();
        if (total <= 0) {
            mEmptyTip.setVisibility(View.VISIBLE);
            mListView.setVisibility(View.GONE);
            mListTab.setVisibility(View.GONE);
            backToList();
        } else {
            mEmptyTip.setVisibility(View.GONE);
            mListView.setVisibility(View.VISIBLE);
            mListTab.setVisibility(View.VISIBLE);
        }
        mAdapter.updateList();
        
        // 停止当前正在滚动的列表
//        mListView.dispatchTouchEvent(MotionEvent.obtain(
//                SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
//                MotionEvent.ACTION_CANCEL, 0, 0, 0));
        
        final Runnable checkSelection = new Runnable() {
            @Override
            public void run() {
                mListView.requestFocusFromTouch();
                if (mIF.getPlayingDevice() == mDeviceType && mIF.getPlayingFileType() == FileType.AUDIO) {
                    int focusNo = 0;
                    focusNo = mIF.getPlayPos();
                    if (focusNo < 0)
                        focusNo = 0;
                    mListView.setSelection(focusNo);
                } else {
                    mListView.setSelection(0);
                }
            }
        };

        checkSelection.run();
        mListView.postDelayed(checkSelection, 20);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        AllMediaList.instance(this).stopOperateThread();
    }
}
