package com.haoke.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import com.amd.bt.BTMusicManager;
import com.amd.bt.BT_IF;
import com.amd.bt.BT_Listener;
import com.amd.media.MediaInterfaceUtil;
import com.amd.media.VRInterfaceUtil;
import com.amd.radio.RadioManager;
import com.amd.radio.Radio_CarListener;
import com.amd.radio.Radio_IF;
import com.amd.util.Source;
import com.haoke.application.MediaApplication;
import com.haoke.btjar.main.BTDef.BTFunc;
import com.haoke.constant.MediaUtil;
import com.haoke.constant.MediaUtil.MediaFunc;
import com.haoke.constant.MediaUtil.MediaState;
import com.haoke.constant.MediaUtil.PlayState;
import com.haoke.constant.MediaUtil.ScanState;
import com.haoke.constant.MediaUtil.ScanType;
import com.haoke.data.AllMediaList;
import com.haoke.data.ModeSwitch;
import com.haoke.data.PlayStateSharedPreferences;
import com.haoke.define.CMSStatusDef.CMSStatusFuc;
import com.haoke.define.EQDef.EQFunc;
import com.haoke.define.GlobalDef;
import com.haoke.define.McuDef;
import com.haoke.define.McuDef.McuFunc;
import com.haoke.define.McuDef.PowerState;
import com.haoke.define.RadioDef.RadioFunc;
import com.haoke.define.ModeDef;
import com.haoke.receiver.MediaReceiver;
import com.haoke.scanner.MediaScanner;
import com.haoke.scanner.MediaScannerListner;
import com.haoke.ui.image.Image_Activity_Main;
import com.haoke.util.DebugLog;
import com.haoke.util.Media_CarListener;
import com.haoke.util.Media_IF;
import com.haoke.util.Media_Listener;
import com.jsbd.util.Meter_IF;

public class MediaService extends Service implements Media_CarListener, MediaScannerListner,
                Media_Listener, BT_Listener, Radio_CarListener {
    public static final String ACTION_MODE_RECORD = "com.jsbd.modeswitch.action";
    public static final String KEY_COMMAND_FROM = "isfrom";
    public static final int VALUE_FROM_SCAN = 1;
    public static final int VALUE_FROM_VR_APP = 2;
    public static final int VALUE_FROM_VR_MUSIC = 3;
    public static final int VALUE_FROM_VR_RADIO = 4;
    public static final int VALUE_FROM_VR_IMAGE = 5;
    public static final int VALUE_FROM_VR_VIDEO = 6;
    public static final int VALUE_FROM_CHECK_ALL_SRORAGE_SCAN_STATE = 7;
    
    private static final String TAG = "MediaService";
    private static MediaService mSelf = null;
    private MediaServiceBinder mBinder = null;
    private boolean ret;
    
    private Media_IF mMediaIF = null;
    private BT_IF mBTIF = null;
    private Radio_IF mRadioIF = null;
    private MediaScanner mScanner;
    
    private RadioManager mRadioManager = null;
    private BTMusicManager mBTMusicManager = null;

    private Handler mSkinHandler;
    
    public static MediaService getInstance() {
        return mSelf;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSelf = this;
        
        mSkinHandler = new Handler();
        
        mMediaIF = Media_IF.getInstance();
        mBTIF = BT_IF.getInstance();
        mRadioIF = Radio_IF.getInstance();
        mMediaIF.setContext(this);
        mMediaIF.bindCarService();
        mMediaIF.initMedia();
        
        mRadioManager = new RadioManager(this);
        mRadioManager.registerReceiver();
        
        mBTMusicManager = new BTMusicManager(this);
        
        mBinder = new MediaServiceBinder(this);
        
        //UsbAutoPlay.setServiceStartTime();

        // 发广播通知服务已经启动
        Intent intent = new Intent();
        intent.setAction(GlobalDef.MEDIA_SERVICE_ACTION_REBOOT);
        this.sendBroadcast(intent);
        
        registerReceiverInternal();

        mMediaIF.registerModeCallBack(this);
        
        mMediaIF.registerLocalCallBack(this);
        mBTIF.registerModeCallBack(this);
        mRadioIF.registerModeCallBack(this);
        
        AllMediaList.instance(getApplicationContext());
        
        checkLaunchFromBoot();
//        checkLaunchRadio(); // 开机的时候检查关机的时候，是否是Radio界面。
        
        mScanner = new MediaScanner(this, this);
        int pidID = android.os.Process.myPid();
        DebugLog.i("Yearlay", "MediaService pid: " + pidID);
        if (pidID > 2000) {
            if (!MediaUtil.checkAllStorageScanOver(getApplicationContext())) {
                mScanner.beginScanningAllStorage();
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        flags = Service.START_STICKY;
        if (intent != null) {
            String action = intent.getAction();
            if (action != null && ACTION_MODE_RECORD.equals(action)) {
                MediaInterfaceUtil.checkModeRecord(this, intent);
            } else {
                int from = intent.getIntExtra(KEY_COMMAND_FROM, 0);
                if (from == VALUE_FROM_SCAN) {
                    scanOperate(intent);
                } else if (from == VALUE_FROM_CHECK_ALL_SRORAGE_SCAN_STATE) {
                    if (!MediaUtil.checkAllStorageScanOver(getApplicationContext())) {
                        mScanner.beginScanningAllStorage();
                    } else {
                        DebugLog.i(TAG, "checkAllStorageScanOver true!");
                    }
                } else {
                    VRInterfaceUtil.VRCommand(intent);
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void scanOperate(Intent intent) {
        switch (intent.getIntExtra(ScanType.SCAN_TYPE_KEY, 0)) {
            case ScanType.SCAN_STORAGE: // 指定磁盘进行扫描。
                mScanner.beginScanningStorage(intent.getStringExtra(ScanType.SCAN_FILE_PATH));
                break;
            case ScanType.REMOVE_STORAGE:{ // 磁盘拔出的处理过程。
                mScanner.removeStorage(intent.getStringExtra(ScanType.SCAN_FILE_PATH));
                break;
            }
        }
    }
    
    @Override
    public void onDestroy() {
        mMediaIF.unregisterModeCallBack(this);
        mMediaIF.unregisterLocalCallBack(this);
        mBTIF.unregisterModeCallBack(this);
        mRadioManager.unregisterReceiver();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
        //return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }
    
    public RadioManager getRadioManager() {
        return mRadioManager;
    }
    
    public BTMusicManager getBtMusicManager() {
        return mBTMusicManager;
    }

    public static final int MSG_UPDATE_APPWIDGET_BASE = 100;
    public static final int MSG_UPDATE_APPWIDGET_ALL = MSG_UPDATE_APPWIDGET_BASE + MediaUtil.UpdateWidget.ALL;
    public static final int MSG_UPDATE_APPWIDGET_BT = MSG_UPDATE_APPWIDGET_BASE + MediaUtil.UpdateWidget.BTMUSIC;
    public static final int MSG_UPDATE_APPWIDGET_AUDIO = MSG_UPDATE_APPWIDGET_BASE + MediaUtil.UpdateWidget.AUDIO;
    public static final int MSG_UPDATE_APPWIDGET_RADIO = MSG_UPDATE_APPWIDGET_BASE + MediaUtil.UpdateWidget.RADIO;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
                case MSG_UPDATE_APPWIDGET_ALL:
                case MSG_UPDATE_APPWIDGET_BT:
                case MSG_UPDATE_APPWIDGET_AUDIO:
                case MSG_UPDATE_APPWIDGET_RADIO:
                    removeMessages(what);
                    int refreshMode = what - MSG_UPDATE_APPWIDGET_BASE;
                    Log.d(TAG, "refreshWidget refreshMode="+refreshMode);
                    //sendBroadcast(new Intent("main_activity_update_ui"));
                    // MediaWidgetProvider.refreshWidget(MediaService.this, refreshMode);
                    mBinder.refreshWidget(refreshMode);
                    break;
            }
        };
    };
    public Handler getHandler() {
        return mHandler;
    }
    
    private Handler mModeHandler = new Handler();
    /* 此方法只供按mode键和开机进入源时调用  */
    public void postModeHandlerRunnable(Runnable r, long delayMillis) {
        Log.d(TAG, "postModeHandlerRunnable delayMillis="+delayMillis);
        mModeHandler.postDelayed(r, delayMillis);
    }
    /* 此方法只供按mode键和开机进入源时调用  */
    public void removeModeHandlerMsg() {
        Log.d(TAG, "removeModeHandlerMsg");
        mModeHandler.removeCallbacksAndMessages(null);
    }
    
    public Handler getSkinHandler() {
        return mSkinHandler;
    }
    
    @Override
    public void onUartDataChange(int mode, int len, byte[] datas) {}
    
    @Override
    public void onCarDataChange(int mode, int func, int data) {
        if (Source.isMcuMode(mode) && func == McuFunc.KEY) {
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
        if (mode == ModeDef.CMS_STATUS && func == CMSStatusFuc.CAR_SPEED) {
			AllMediaList.sCarSpeed = data / 100.0f;
		}
        if (Source.isMcuMode(mode)) {
            Log.v(TAG, "onCarDataChange MCU func=" + func + ", data=" + data);
            switch (func) {
            case McuFunc.SOURCE:
                removeModeHandlerMsg();
                mMediaIF.sourceChanged(data);
                break;
            case McuFunc.KEY://按钮处理
                break;
            case McuFunc.POWER_STATE:
                if (data == PowerState.POWER_OFF) {
                    MediaInterfaceUtil.setMuteRecordPlayState(KeyEvent.KEYCODE_POWER);
                    //modify bug 20762 begin
                    //操作图片播放,暂停
                    boolean playImage = Image_Activity_Main.isPlayImage(MediaApplication.getInstance());
                    if (playImage) {
                        Intent intent = new Intent();
                        intent.setAction("power_off");
                        sendBroadcast(intent);
                    }
                  //modify bug 20762 end
                } else if (data == PowerState.POWER_ON) {
                    MediaInterfaceUtil.cancelMuteRecordPlayState(KeyEvent.KEYCODE_POWER);
                    //modify bug 20762 begin 
                    //操作图片播放,播放
                    boolean playImage = Image_Activity_Main.isPlayImage(MediaApplication.getInstance());
                    if (!playImage) {
                        Intent intent = new Intent();
                        intent.setAction("power_on");
                        sendBroadcast(intent);
                    }
                  //modify bug 20762 end
                }
                break;
            }
            
        } else if (Source.isEQMode(mode)) {
            if (func == EQFunc.MUTE) {
                if (data == 0) {  //取消静音
                    MediaInterfaceUtil.cancelMuteRecordPlayState(KeyEvent.KEYCODE_MUTE);
                } else { //静音
                    MediaInterfaceUtil.setMuteRecordPlayState(KeyEvent.KEYCODE_MUTE);
                }
            }
        }
    }

    private void handleModeKey() {
        if (!ModeSwitch.instance().isGoingFlag()) {
            long start = System.currentTimeMillis();
            int nextMode = ModeSwitch.instance().getNextMode(getApplicationContext());
            launchSourceActivity(nextMode, true);
            long end = System.currentTimeMillis();
            Log.d(TAG, "handleModeKey consume time="+(end-start)+"ms; nextMode="+nextMode);
        } else {
            DebugLog.e(TAG, "handleModeKey lost  isGoingFlag : true");
        }
    }
    
    private void launchSourceActivity(int mode, boolean autoPlay) {
        DebugLog.d(TAG, "launchSourceActivity mode="+mode+"; autoPlay="+autoPlay);
        MediaInterfaceUtil.launchSourceActivity(mode, autoPlay);
    }
    
    private int mBootWaitTimeOut = 0;
    private void checkLaunchFromBoot() {
        final int ms = MediaInterfaceUtil.checkSourceFromBoot(this);
        if (ms >= 0) {
            if (mBootWaitTimeOut > 80000) {
                Log.e(TAG, "checkLaunchFromBoot mBootWaitTimeOut="+mBootWaitTimeOut);
            } else {
                mBootWaitTimeOut += ms;
                postModeHandlerRunnable(new Runnable() {
                    @Override
                    public void run() {
                        checkLaunchFromBoot();
                    }
                }, ms);
            }
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
    public void onBTDataChange(int mode, int func, int data) { // 目的是给仪表发送信息。
        boolean needToSend = false;
        if (Source.isBTMode(mode) && Source.isBTMusicSource()) {
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
            if (!TextUtils.isEmpty(title)) {
                Meter_IF.sendMusicInfo(title, artist, album);
            }
        }
    }

    @Override
    public void onDataChange(int mode, int func, int data1, int data2) { // 目的是给仪表发送信息。
        DebugLog.i(TAG, "onDataChange MediaFunc: " + func);
        boolean needToSend = false;
        if (mode == mMediaIF.getMode() && Source.isAudioSource()) {
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
    
    private BroadcastReceiver mMediaReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MediaReceiver.isDynamicFlag = true;
            MediaReceiver.onReceiveEx(context, intent);
        }
    };
    private void registerReceiverInternal() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addDataScheme("file");
        filter.setPriority(10001);
        registerReceiver(mMediaReceiver, filter);
        DebugLog.i("Yearlay", "registerReceiverInternal isDynamicFlag!");
    }

    @Override
    public void onRadioCarDataChange(int mode, int func, int data) {
        if (Source.isMcuMode(mode)) {
            switch (func) {
            case McuFunc.SOURCE:
                break;
            }
        } else if (mode == mRadioIF.getMode()) {
            switch (func) {
            case RadioFunc.FREQ:
                sendMeterFreq(data);
                break;
            }
        }
    }

    @Override
    public void setRadioCurInterface(int data) {}
    
    private void sendMeterFreq(int freq) {
        if (!isRescanOrScan5S()) {
            Radio_IF.sendRadioInfo(mRadioIF.getCurBand(), freq);
        }
    }
    
    private boolean isRescanOrScan5S() {
        return mRadioIF.isRescanState() || mRadioIF.isScan5SState() || mRadioIF.isScanAutoNextState();
    }
}
