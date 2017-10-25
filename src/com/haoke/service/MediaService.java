package com.haoke.service;

import com.amd.bt.BT_IF;
import com.amd.bt.BT_Listener;
import com.haoke.application.MediaApplication;
import com.haoke.bean.FileNode;
import com.haoke.btjar.main.BTDef.BTConnState;
import com.haoke.btjar.main.BTDef.BTFunc;
import com.haoke.constant.MediaUtil;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.constant.MediaUtil.ScanState;
import com.haoke.constant.MediaUtil.ScanType;
import com.haoke.constant.VRConstant.VRApp;
import com.haoke.constant.VRConstant.VRImage;
import com.haoke.constant.VRConstant.VRIntent;
import com.haoke.constant.VRConstant.VRMusic;
import com.haoke.constant.VRConstant.VRRadio;
import com.haoke.constant.VRConstant.VRVideo;
import com.haoke.data.AllMediaList;
import com.haoke.data.ModeSwitch;
import com.haoke.data.PlayStateSharedPreferences;
import com.haoke.define.GlobalDef;
import com.haoke.define.McuDef;
import com.haoke.define.EQDef.EQFunc;
import com.haoke.define.McuDef.McuFunc;
import com.haoke.define.McuDef.PowerState;
import com.haoke.define.MediaDef.DeviceType;
import com.haoke.define.MediaDef.MediaFunc;
import com.haoke.define.MediaDef.MediaState;
import com.haoke.define.MediaDef.PlayState;
import com.haoke.define.MediaDef.RepeatMode;
import com.haoke.define.ModeDef;
import com.haoke.scanner.MediaScanner;
import com.haoke.scanner.MediaScannerListner;
import com.haoke.ui.image.Image_Activity_Main;
import com.haoke.ui.video.Video_Activity_Main;
import com.haoke.ui.video.Video_IF;
import com.haoke.util.DebugLog;
import com.haoke.util.Media_CarListener;
import com.haoke.util.Media_IF;
import com.haoke.util.Media_Listener;
import com.amd.media.MediaInterfaceUtil;
import com.amd.radio.Radio_IF;
import com.jsbd.util.Meter_IF;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.Toast;

public class MediaService extends Service implements Media_CarListener, MediaScannerListner,
                Media_Listener, BT_Listener {
    public static final String KEY_COMMAND_FROM = "isfrom";
    public static final int VALUE_FROM_SCAN = 1;
    public static final int VALUE_FROM_VR_APP = 2;
    public static final int VALUE_FROM_VR_MUSIC = 3;
    public static final int VALUE_FROM_VR_RADIO = 4;
    public static final int VALUE_FROM_VR_IMAGE = 5;
    public static final int VALUE_FROM_VR_VIDEO = 6;
    
    private static final String TAG = "MediaService";
    private static MediaService mSelf = null;
    private boolean ret;
    
    private Media_IF mMediaIF = null;
    private BT_IF mBTIF = null;
    private Radio_IF mRadioIF = null;
    private MediaScanner mScanner;

    public static MediaService getInstance() {
        return mSelf;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSelf = this;
        mMediaIF = Media_IF.getInstance();
        mBTIF = BT_IF.getInstance();
        mRadioIF = Radio_IF.getInstance();
        mMediaIF.setContext(this);
        mMediaIF.bindCarService();
        mMediaIF.initMedia();

        // 发广播通知服务已经启动
        Intent intent = new Intent();
        intent.setAction(GlobalDef.MEDIA_SERVICE_ACTION_REBOOT);
        this.sendBroadcast(intent);

        mMediaIF.registerModeCallBack(this);
        
        mMediaIF.registerLocalCallBack(this);
        mBTIF.registerModeCallBack(this);
        
        AllMediaList.instance(getApplicationContext());
        
        checkLaunchFromBoot();
//        checkLaunchRadio(); // 开机的时候检查关机的时候，是否是Radio界面。
        
        mScanner = new MediaScanner(this, this);
        int pidID = android.os.Process.myPid();
        DebugLog.i("Yearlay", "MediaService pid: " + pidID);
        if (pidID > 2000) {
            mScanner.beginScanningAllStorage();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        flags = Service.START_STICKY;
        if (intent != null && !MediaInterfaceUtil.mediaCannotPlay()) {
            switch (intent.getIntExtra(KEY_COMMAND_FROM, 0)) {
            case VALUE_FROM_SCAN:
                scanOperate(intent);
                break;
            case VALUE_FROM_VR_APP:
                vrAppOperate(intent);
                break;
            case VALUE_FROM_VR_MUSIC:
                vrMusicOperate(intent);
                break;
            case VALUE_FROM_VR_RADIO:
                vrRadioOperate(intent);
                break;
            case VALUE_FROM_VR_IMAGE:
                vrImageOperate(intent);
                break;
            case VALUE_FROM_VR_VIDEO:
                vrVideoOperate(intent);
                break;
            default:
                break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void scanOperate(Intent intent) {
        switch (intent.getIntExtra(ScanType.SCAN_TYPE_KEY, ScanType.SCAN_ALL)) {
        case ScanType.SCAN_ALL: // 扫描所有已经挂载的磁盘。
            // mScanner.beginScanningAllStorage();
            break;
        case ScanType.SCAN_STORAGE: // 指定磁盘进行扫描。
            mScanner.beginScanningStorage(intent.getStringExtra(ScanType.SCAN_FILE_PATH));
            break;
        case ScanType.SCAN_DIRECTORY: // 目前不支持。
            break;
        case ScanType.SCAN_FILE: // 目前不支持。
            break;
        case ScanType.REMOVE_STORAGE: // 磁盘拔出的处理过程。
            String devicePath = intent.getStringExtra(ScanType.SCAN_FILE_PATH);
            mScanner.removeStorage(devicePath);
            // showEjectToast(devicePath);
            break;
        }
    }
    
    private void showEjectToast(String devicePath) {
        Toast toast = Toast.makeText(getApplicationContext(),
                MediaUtil.DEVICE_PATH_USB_1.equals(devicePath) ? "已移除USB设备1" : "已移除USB设备2", Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }
    
    private void vrAppOperate(Intent intent) {
        int function = intent.getIntExtra(VRApp.KEY_FUCTION, 0);
        if (function == VRApp.FUNCTION_OPEN || function == VRApp.FUNCTION_CLOSE) {
            boolean yesOperate = (function == VRApp.FUNCTION_OPEN);
            switch (intent.getIntExtra(VRApp.KEY_OPERATOR, -1)) {
            case VRApp.OPERATOR_MUSIC:
                operateMusic(yesOperate);
                break;
            case VRApp.OPERATOR_VIDEO:
                operateVideo(yesOperate);
                break;
            case VRApp.OPERATOR_IMAGE:
                operateImage(yesOperate);
                break;
            case VRApp.OPERATOR_BT:
                operateBT(yesOperate);
                break;
            case VRApp.OPERATOR_RADIO:
                operateRadio(yesOperate);
                break;
            case VRApp.OPERATOR_COLLECT:
                operateCollect(yesOperate);
                break;
            default:
                break;
            }
        }
    }
    
    private void operateMusic(boolean yesOperate) {
        // 音乐，播放当前歌曲，进入音乐播放界面。
        int playState = mMediaIF.getPlayState();
        DebugLog.d(TAG, "operateMusic yesOperate="+yesOperate+"; playState="+playState);
        if (yesOperate) {
            if (playState != PlayState.PLAY) {
                // Media_IF.setCurSource(ModeDef.AUDIO);
                mMediaIF.setPlayState(PlayState.PLAY);
            }
            launchMusicPlayActivity();
        } else {
            if (playState == PlayState.PLAY) {
                DebugLog.d(TAG, "operateMusic playState="+playState);
                mMediaIF.setPlayState(PlayState.PAUSE);
            }
            mMediaIF.setRecordPlayState(PlayState.PAUSE);
            sendBroadcast(new Intent(VRIntent.ACTION_FINISH_MUSIC_RADIO));
        }
    }
    
    private void operateVideo(boolean yesOperate) {
        DebugLog.d(TAG, "operateVideo");
        if (yesOperate) {
            Intent intent = new Intent(this, Video_Activity_Main.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            Video_IF.getInstance().setRecordPlayState(PlayState.PAUSE);
            Intent intent = new Intent(VRIntent.ACTION_OPERATE_VIDEO);
            intent.putExtra(VRIntent.KEY_VIDEO, VRIntent.FINISH_VIDEO);
            sendBroadcast(intent);
        }
    }
    
    private void operateImage(boolean yesOperate) {
        DebugLog.d(TAG, "operateImage");
        if (yesOperate) {
            Intent intent = new Intent(this, Image_Activity_Main.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            Intent intent = new Intent(VRIntent.ACTION_OPERATE_IMAGE);
            intent.putExtra(VRIntent.KEY_IMAGE, VRIntent.FINISH_IMAGE);
            sendBroadcast(intent);
        }
    }
    
    private void operateBT(boolean yesOperate) {
        if (BT_IF.getInstance().getConnState() != BTConnState.CONNECTED) {
            return;
        }
        // 蓝牙音乐，播放蓝牙音乐，进入蓝牙音乐界面。
        if (yesOperate) {
            launchSourceActivity(ModeSwitch.MUSIC_BT_MODE, true);
        } else {
            if (mBTIF.music_isPlaying()) {
                mBTIF.music_pause();
            }
            mBTIF.setRecordPlayState(PlayState.PAUSE);
            sendBroadcast(new Intent(VRIntent.ACTION_FINISH_MUSIC_RADIO));
        }
    }
    
    private void operateRadio(boolean yesOperate) {
        // 收音机，播放电台，进入收音机播放界面。
        if (yesOperate) {
            launchSourceActivity(ModeSwitch.RADIO_MODE, true);
        } else {
            if (mRadioIF.isEnable()) {
                mRadioIF.setEnable(false);
            }
            mRadioIF.setRecordRadioOnOff(false);
            sendBroadcast(new Intent(VRIntent.ACTION_FINISH_MUSIC_RADIO));
        }
    }
    
    private void operateCollect(boolean yesOperate) {
        // 收藏音乐，播放收藏歌曲，进入收藏音乐播放界面。
        int playState = mMediaIF.getPlayState();
        DebugLog.d(TAG, "operateCollect yesOperate="+yesOperate+"; playState="+playState);
        if (yesOperate) {
            int size = mMediaIF.getMediaListSize(DeviceType.COLLECT, FileType.AUDIO);
            if (size != 0) {
                if (mMediaIF.getPlayingDevice() != DeviceType.COLLECT
                        || mMediaIF.getPlayingFileType() != FileType.AUDIO) {
                    mMediaIF.setCurScanner(DeviceType.COLLECT, FileType.AUDIO);
                    mMediaIF.play(0);
                } else if (playState != PlayState.PLAY) {
                    mMediaIF.setPlayState(PlayState.PLAY);
                }
                launchMusicPlayActivity();
            }
        } else {
            if (playState == PlayState.PLAY
                    && mMediaIF.getPlayingDevice() == DeviceType.COLLECT
                    && mMediaIF.getPlayingFileType() == FileType.AUDIO) {
                mMediaIF.setPlayState(PlayState.PAUSE);
            }
            sendBroadcast(new Intent(VRIntent.ACTION_FINISH_MUSIC_RADIO));
        }
    }
    
    private void vrMusicOperate(Intent intent) {
        switch (intent.getIntExtra(VRMusic.KEY_COMMAND_CODE, 0)) {
        case VRMusic.COMMAND_SINGLE_MODE:
            commandSingleMode();
            break;
        case VRMusic.COMMAND_RANDOM_MODE:
            commandRandomMode();
            break;
        case VRMusic.COMMAND_CIRCLE_MODE:
            commandCircleMode();
            break;
        case VRMusic.COMMAND_COLLECT_MUSIC:
            commandCollectMode();
            break;
        case VRMusic.COMMAND_UNCOLLECT_MUSIC:
            commandUnCollectMode();
            break;
        case VRMusic.COMMAND_PLAY_MUSIC:
            commandPlayMusic(intent.getStringExtra(VRMusic.KEY_MUSIC_PATH));
            break;
        default:
            break;
        }
    }
    
    private void commandSingleMode() {
        // 设置单曲循环，并播放。
        int playState = mMediaIF.getPlayState();
        DebugLog.d(TAG, "commandSingleMode playState="+playState);
        mMediaIF.setRepeatMode(RepeatMode.ONE);
        if (playState != PlayState.PLAY) {
            mMediaIF.setPlayState(PlayState.PLAY);
        }
    }
    
    private void commandRandomMode() {
        // 设置随机循环，并播放。
        int playState = mMediaIF.getPlayState();
        DebugLog.d(TAG, "commandRandomMode playState="+playState);
        mMediaIF.setRepeatMode(RepeatMode.RANDOM);
        if (playState != PlayState.PLAY) {
            mMediaIF.setPlayState(PlayState.PLAY);
        }
    }
    
    private void commandCircleMode() {
        // 设置列表循环，并播放。
        int playState = mMediaIF.getPlayState();
        DebugLog.d(TAG, "commandCircleMode playState="+playState);
        mMediaIF.setRepeatMode(RepeatMode.CIRCLE);
        if (playState != PlayState.PLAY) {
            mMediaIF.setPlayState(PlayState.PLAY);
        }
    }
    
    private void commandCollectMode() {
        // TODO: 当前是Radio或BT模式不处理；如果是 磁盘音乐模式就处理。收藏当前的歌曲。
        int playState = mMediaIF.getPlayState();
        int source = mMediaIF.getCurSource();
        DebugLog.d(TAG, "commandCollectMode playState="+playState+"; source="+source);
        if (source == ModeDef.AUDIO) {
            mMediaIF.collectMusic(mMediaIF.getPlayItem());
        }
    }
    
    private void commandUnCollectMode() {
        // TODO: 当前是Radio或BT模式不处理；如果是 磁盘音乐模式就处理。取消收藏当前歌曲。
        int playState = mMediaIF.getPlayState();
        int source = mMediaIF.getCurSource();
        DebugLog.d(TAG, "commandUnCollectMode playState="+playState+"; source="+source);
        if (source == ModeDef.AUDIO) {
            mMediaIF.deleteCollectedMusic(mMediaIF.getPlayItem());
        }
    }
    
    private void commandPlayMusic(String filePath) {
        if (!TextUtils.isEmpty(filePath)) {
            // 指定歌曲（path）播放。
            int playState = mMediaIF.getPlayState();
            FileNode fileNode = mMediaIF.getPlayItem();
            if (fileNode != null && filePath.equals(fileNode.getFilePath())) {
                if (playState != PlayState.PLAY) {
                    mMediaIF.setPlayState(PlayState.PLAY);
                }
            } else {
                mMediaIF.play(filePath);
            }
            launchMusicPlayActivity();
        }
    }
    
    private void vrRadioOperate(Intent intent) {
        switch (intent.getIntExtra(VRRadio.KEY_COMMAND_CODE, 0)) {
        case VRRadio.COMMAND_COLLECT_RADIO:
            commandCollectRadio();
            break;
        case VRRadio.COMMAND_UNCOLLECT_RADIO:
            commandUnCollectRadio();
            break;
        case VRRadio.COMMAND_PLAY_COLLECT_RADIO:
            commandPlayCollectRadio();
            break;
        default:
            break;
        }
    }
    
    private void commandCollectRadio() {
        // 收藏当前播放电台。
        if (mRadioIF.isEnable()) {
            int freq = mRadioIF.getCurFreq();
            mRadioIF.collectFreq(this.getApplicationContext(), freq, true);
        }
    }
    
    private void commandUnCollectRadio() {
        // 取消收藏当前播放电台。
        if (mRadioIF.isEnable()) {
            int freq = mRadioIF.getCurFreq();
            mRadioIF.uncollectFreq(this.getApplicationContext(), freq, true);
        }
    }
    
    private void vrImageOperate(Intent vrIntent) {
        int value = 0;
        switch (vrIntent.getIntExtra(VRImage.KEY_COMMAND_CODE, 0)) {
        case VRImage.COMMAND_PLAY_IMAGE:
            value = VRIntent.PLAY_IMAGE;
            break;
        case VRImage.COMMAND_PAUSE_IMAGE:
            value = VRIntent.PAUSE_IMAGE;
            break;
        case VRImage.COMMAND_PRE_IMAGE:
            value = VRIntent.PRE_IMAGE;
            break;
        case VRImage.COMMAND_NEXT_IMAGE:
            value = VRIntent.NEXT_IMAGE;
            break;
        }
        if (value != 0) {
            Intent intent = new Intent(VRIntent.ACTION_OPERATE_IMAGE);
            intent.putExtra(VRIntent.KEY_IMAGE, value);
            sendBroadcast(intent);
        }
    }
    
    private void vrVideoOperate(Intent vrIntent) {
        int value = 0;
        switch (vrIntent.getIntExtra(VRVideo.KEY_COMMAND_CODE, 0)) {
        case VRVideo.COMMAND_PLAY_VIDEO:
            value = VRIntent.PLAY_VIDEO;
            break;
        case VRVideo.COMMAND_PAUSE_VIDEO:
            value = VRIntent.PAUSE_VIDEO;
            break;
        case VRVideo.COMMAND_PRE_VIDEO:
            value = VRIntent.PRE_VIDEO;
            break;
        case VRVideo.COMMAND_NEXT_VIDEO:
            value = VRIntent.NEXT_VIDEO;
            break;
        }
        if (value != 0) {
            Intent intent = new Intent(VRIntent.ACTION_OPERATE_VIDEO);
            intent.putExtra(VRIntent.KEY_VIDEO, value);
            sendBroadcast(intent);
        }
    }
    
    private void commandPlayCollectRadio() {
        // 播放收藏列表的中的电台（不用打开收音机播放的界面）。
        mRadioIF.playCollectFistFreq(this.getApplicationContext(), true);
    }

    @Override
    public void onDestroy() {
        mMediaIF.unregisterModeCallBack(this);
        mMediaIF.unregisterLocalCallBack(this);
        mBTIF.unregisterModeCallBack(this);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public static final int MSG_UPDATE_APPWIDGET = 1;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
            case MSG_UPDATE_APPWIDGET:
                removeMessages(what);
                sendBroadcast(new Intent("main_activity_update_ui"));
                break;
            }
        };
    };
    public Handler getHandler() {
        return mHandler;
    }
    
    private Handler mModeHandler = new Handler();
    /* 此方法只供按mode键和开机进入源时调用  */
    public Handler getModeHandler() {
        return mModeHandler;
    }
    
    @Override
    public void onUartDataChange(int mode, int len, byte[] datas) {}
    
    @Override
    public void onCarDataChange(int mode, int func, int data) {
        if (mode == ModeDef.MCU && func == McuFunc.KEY) {
            int keyState = data >> 8;
            int keyCode = data & 0xFF;
            if (keyState == McuDef.KeyState.PRESS_RELEASED) {
                if (keyCode == McuDef.KeyCode.MODE) {
                     DebugLog.d("Yearlay", "MediaService onCarDataChange handle Mode_Key...");
                     handleModeKey();
                } else if (keyCode == McuDef.KeyCode.POWER2) {
                    
                }
            }
        }
        if (mode == ModeDef.MCU) {
            Log.v(TAG, "onCarDataChange MCU func=" + func + ", data=" + data);
            switch (func) {
            case McuFunc.SOURCE:
                getModeHandler().removeCallbacksAndMessages(null);
                mMediaIF.sourceChanged(data);
                break;
            case McuFunc.KEY://按钮处理
                break;
            case McuFunc.POWER_STATE:
                if (data == PowerState.POWER_OFF) {
                    MediaInterfaceUtil.setMuteRecordPlayState(KeyEvent.KEYCODE_POWER);
                } else if (data == PowerState.POWER_ON) {
                    MediaInterfaceUtil.cancelMuteRecordPlayState(KeyEvent.KEYCODE_POWER);
                }
                break;
            }
            
        } else if (mode == ModeDef.BT) { // 通话开始或结束，声音需要处理
            int source = mMediaIF.getCurSource();
            
            Log.v(TAG, "onCarDataChange BT func=" + func + ", data=" + data + "; source="+source);
            
            /*if (func == BTFunc.CALL_STATE) {
                if (source == ModeDef.AUDIO || source == ModeDef.VIDEO) { // 处于当前源
                    if (data == BTCallState.IDLE) { // 打完电话，需要再切下通道，避免没声音
                        Log.v(TAG, "onCarDataChange openAvio");
                        mMediaIF.audioFocusChanged(PlayState.PLAY);
                    }
                }
            }*/
        } else if (mode == ModeDef.EQ) {
            if (func == EQFunc.MUTE) {
                if (data == 0) {  //取消静音
                    MediaInterfaceUtil.cancelMuteRecordPlayState(KeyEvent.KEYCODE_MUTE);
                } else { //静音
                    MediaInterfaceUtil.setMuteRecordPlayState(KeyEvent.KEYCODE_MUTE);
                }
            }
        }
    }

    private void launchMusicPlayActivity() {
        MediaInterfaceUtil.launchMusicPlayActivity(this);
    }
    
    private void handleModeKey() {
        if (!ModeSwitch.instance().isGoingFlag()) {
        	long start = System.currentTimeMillis();
            launchSourceActivity(ModeSwitch.instance().getNextMode(getApplicationContext()), true);
            long end = System.currentTimeMillis();
            Log.d(TAG, "handleModeKey consume time="+(end-start)+"ms");
        } else {
            DebugLog.e(TAG, "handleModeKey lost  isGoingFlag : true");
        }
    }
    
    private void launchSourceActivity(int mode, boolean autoPlay) {
        MediaInterfaceUtil.launchSourceActivity(mode, autoPlay);
    }
    
    private void checkLaunchFromBoot() {
        final int ms = MediaInterfaceUtil.checkSourceFromBoot(this);
        if (ms >= 0) {
            getModeHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkLaunchFromBoot();
                }
            }, ms);
        }
    }
    
    private void checkLaunchRadio() {
        boolean showMark = PlayStateSharedPreferences.instance(this).getModeMark();
        int currentMode = PlayStateSharedPreferences.instance(this).getSwitchMode();
        if (showMark && currentMode == ModeSwitch.RADIO_MODE) {
            launchSourceActivity(ModeSwitch.RADIO_MODE, true);
        }
    }

    @Override
    public void scanPath(int scanState, int deviceType) {
        switch (scanState) {
            case ScanState.SCANNING:
                noticeStartStatus(ScanState.SCANNING, deviceType);
                printScanStateMessage("Begin scanning device: " + MediaUtil.getDevicePath(deviceType),
                        ScanState.SCANNING, deviceType);
                break;
            case ScanState.COMPLETED:
                noticeStartStatus(ScanState.COMPLETED, deviceType);
                printScanStateMessage("End scanning device: " + MediaUtil.getDevicePath(deviceType), ScanState.COMPLETED, deviceType);
                break;
            case ScanState.REMOVE_STORAGE:
                noticeStartStatus(ScanState.REMOVE_STORAGE, deviceType);
                printScanStateMessage("Remove the device: " + MediaUtil.getDevicePath(deviceType), ScanState.REMOVE_STORAGE, deviceType);
                break;
            case ScanState.SCAN_ERROR:
                noticeStartStatus(ScanState.SCAN_ERROR, deviceType);
                printScanStateMessage("Error scanning device: " + MediaUtil.getDevicePath(deviceType), ScanState.SCAN_ERROR, deviceType);
                break;
            case ScanState.SCAN_THREAD_OVER:
                DebugLog.d("Yearlay", "Scan Thread is Over, Begin Id3ParseThread!!!");
                mScanner.beginID3ParseThread();
                break;
            case ScanState.ID3_PARSING:
                noticeStartStatus(ScanState.ID3_PARSING, deviceType);
                printScanStateMessage("id3 parsing device: " + MediaUtil.getDevicePath(deviceType), ScanState.ID3_PARSING, deviceType);
                break;
            case ScanState.ID3_PARSE_COMPLETED:
                noticeStartStatus(ScanState.ID3_PARSE_COMPLETED, deviceType);
                printScanStateMessage("id3 parse completed device: " + MediaUtil.getDevicePath(deviceType), ScanState.ID3_PARSE_COMPLETED, deviceType);
                break;
            case ScanState.COMPLETED_ALL:
                noticeStartStatus(ScanState.COMPLETED_ALL, deviceType);
                DebugLog.d("Yearlay", "All task is over!!!");
                break;
            default:
                break;
        }
    }
    
    // 扫描状态通知
    private void noticeStartStatus(int scanState, int mDeviceType) {
        Intent noticeIntent = new Intent();
        noticeIntent.setAction(MediaUtil.SCANNING_ACTION);
        noticeIntent.putExtra(MediaUtil.SCANNING_ACTION_EXTRA_SCAN_STATE, scanState);
        noticeIntent.putExtra(MediaUtil.SCANNING_ACTION_EXTRA_DEVICE_PATH,
                MediaUtil.getDevicePath(mDeviceType));
        sendBroadcast(noticeIntent);
    }
    
    private void printScanStateMessage(String message, int scanState, int deviceType) {
        DebugLog.d("Yearlay", "sendBroadcast <com.jsbd.fileserve> scanState:" + scanState
                + " && deviceType:" + deviceType +
                " && message: " + message);
    }

    @Override
    public void onBTDataChange(int mode, int func, int data) { // TODO 目的是给仪表发送信息。
        boolean needToSend = false;
        if (mode == ModeDef.BT) {
            switch (func) {
            case BTFunc.MUSIC_PLAY_STATE://400
                needToSend = mBTIF.music_isPlaying();
                break;
            case BTFunc.MUSIC_ID3_UPDATE://401
                needToSend = true;
                break;
            case BTFunc.CONN_STATE://101
                break;
            case BTFunc.FOUND_DEVICE://104 
                break;
            case BTFunc.BATTERY://110 一组
                break;
            case BTFunc.SIGNAL://111
                break;
            }
        }
        if (needToSend) {
            String title = mBTIF.music_getTitle();
            String artist = mBTIF.music_getArtist();
            String album = mBTIF.music_getAlbum();
            Meter_IF.sendMusicInfo(title, artist, album);
        }
    }

    @Override
    public void onDataChange(int mode, int func, int data1, int data2) { // TODO 目的是给仪表发送信息。
        DebugLog.i(TAG, "onDataChange MediaFunc: " + func);
        boolean needToSend = false;
        if (mode == mMediaIF.getMode()) {
            switch (func) {
            case MediaFunc.DEVICE_CHANGED://8 data1=deviceType, data2=isExist ? 1 : 0
            case MediaFunc.SCAN_STATE://1
                break;
            case MediaFunc.PREPARED:
                needToSend = mMediaIF.getMediaState() == MediaState.PREPARED;
                break;
            case MediaFunc.ERROR://104
            case MediaFunc.PLAY_OVER:
            case MediaFunc.PLAY_STATE:
                needToSend = mMediaIF.getPlayState() == PlayState.PLAY;
                break;
            case MediaFunc.REPEAT_MODE:
            case MediaFunc.RANDOM_MODE:
                break;
            default:
                break;
            }
        }
        if (needToSend) {
            String title = mMediaIF.getPlayId3Title();
            String artist = mMediaIF.getPlayId3Artist();
            String album = mMediaIF.getPlayId3Album();
            Meter_IF.sendMusicInfo(title, artist, album);
        }
    }

    @Override
    public void setCurInterface(int data) {}
}
