package com.amd.media;

import java.util.ArrayList;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import com.amd.bt.BTMusic_IF;
import com.amd.bt.BT_IF;
import com.amd.radio.Radio_IF;
import com.amd.util.Source;
import com.haoke.application.MediaApplication;
import com.amd.media.AudioFocus;
import com.amd.media.AudioFocus.AudioFocusListener;
import com.archermind.skinlib.SkinTheme;
import com.haoke.bean.FileNode;
import com.haoke.bean.StorageBean;
import com.haoke.btjar.main.BTDef.BTConnState;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.data.AllMediaList;
import com.haoke.data.ModeSwitch;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.constant.MediaUtil.PlayState;
import com.haoke.service.BTMusicService;
import com.haoke.service.MediaService;
import com.haoke.service.RadioService;
import com.haoke.ui.media.Media_Activity_Main;
import com.haoke.ui.music.Music_Activity_List;
import com.haoke.ui.video.Video_Activity_Main;
import com.haoke.ui.video.Video_IF;
import com.haoke.util.Media_IF;

public class MediaInterfaceUtil {
    private static final String TAG = "MediaInterfaceUtil";
    
    //private static int sMediaPlayStateRecord = Source.NULL;
    
    public static final Uri URI_SKIN = Settings.System.getUriFor(SkinTheme.SKIN_KEY_NAME);
    
    private static AudioFocus mAudioFocus;
    private static boolean sMuteKey_MuteState = false;
    private static boolean sPowerKey_MuteState = false;
    
    /*public static void resetMediaPlayStateRecord(int source) {
        Log.d(TAG, "resetMediaPlayStateRecord old is "+sMediaPlayStateRecord + "; caller is "+source);
        if (source == sMediaPlayStateRecord) {
            sMediaPlayStateRecord = Source.NULL;
        }
    }
    
    public static void resetMediaPlayStateRecord() {
        Log.d(TAG, "resetMediaPlayStateRecord old is "+sMediaPlayStateRecord );
        sMediaPlayStateRecord = Source.NULL;
    }
    
    private static void setMediaPlayStateRecord(int source) {
        Log.d(TAG, "sMediaPlayStateRecord source="+source+"; old is "+sMediaPlayStateRecord);
        sMediaPlayStateRecord = source;
    }
    
    public static int getMediaPlayStateRecord() {
        return sMediaPlayStateRecord;
    }*/
    
    private static boolean hasAudioFocus() {
        boolean ret = false;
        if (mAudioFocus != null) {
            int audioFocusState = mAudioFocus.getFocusState();
            if (audioFocusState == AudioManager.AUDIOFOCUS_GAIN
                    || audioFocusState == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                ret = true;
        }
        return ret;
    }
    
    public static void setMuteRecordPlayState(int key) {
        if (true) {
            if (mAudioFocus == null) {
                mAudioFocus = new AudioFocus(MediaApplication.getInstance());
                mAudioFocus.registerListener(mAudioFocusListener);
            }
            if (key == KeyEvent.KEYCODE_MUTE) {
                sMuteKey_MuteState = Media_IF.getMute();
                if (sMuteKey_MuteState) {
                    mAudioFocus.requestTransientAudioFocus(true);
                }
            } else if (key == KeyEvent.KEYCODE_POWER) {
                if (hasAudioFocus()) {
                    
                } else {
                    sPowerKey_MuteState = true;
                    mAudioFocus.requestTransientAudioFocus(true);
                }
            }
            return;
        }
        /*resetMediaPlayStateRecord(); 
        boolean mute = Media_IF.getMute();
        if (mute) {
            int source = Media_IF.getCurSource();
            if (Source.isRadioSource(source)) {
                if (Radio_IF.getInstance().isEnable()) {
                    setMediaPlayStateRecord(source);
                    Radio_IF.getInstance().setEnable(false);
                }
            } else if (Source.isAudioSource(source)) {
                if (Media_IF.getInstance().isPlayState()) {
                    setMediaPlayStateRecord(source);
                    Media_IF.getInstance().setPlayState(PlayState.PAUSE);
                }
            } else if (Source.isVideoSource(source)) {
                if (Media_IF.getInstance().isPlayState()) {
                    setMediaPlayStateRecord(source);
                    Media_IF.getInstance().setPlayState(PlayState.PAUSE);
                }
            } else if (Source.isBTMusicSource(source)) {
                if (BT_IF.getInstance().music_isPlaying()) {
                    setMediaPlayStateRecord(source);
                    BT_IF.getInstance().music_pause();
                }
            }
        }
        Log.d(TAG, "setMute mute="+mute+"; sMediaPlayStateRecord="+sMediaPlayStateRecord);*/
    }
    
    public static void cancelMuteRecordPlayState(int key) {
        Log.d(TAG, "cancelMuteRecordPlayState sMuteKey_MuteState="+sMuteKey_MuteState+"; sPowerKey_MuteState="+sPowerKey_MuteState);
        if (true) {
            if (hasAudioFocus() || sMuteKey_MuteState || sPowerKey_MuteState) {
                Log.d(TAG, "cancelMuteRecordPlayState hasAudioFocus");
                if (key == KeyEvent.KEYCODE_MUTE) {
                    if (sMuteKey_MuteState) {
                        sMuteKey_MuteState = false;
                        mAudioFocus.requestTransientAudioFocus(false);
                    }
                } else if (key == KeyEvent.KEYCODE_POWER) {
                    if (sPowerKey_MuteState) {
                        sPowerKey_MuteState = false;
                        mAudioFocus.requestTransientAudioFocus(false);
                    }
                }
            }
            return;
        }
        /*int source = getMediaPlayStateRecord();
        if (source != Source.NULL) {
            if (Source.isRadioSource(source)) {
                Radio_IF.getInstance().setEnable(true);
            } else if (Source.isAudioSource(source)) {
                Media_IF.getInstance().setPlayState(PlayState.PLAY);
            } else if (Source.isVideoSource(source)) {
                Media_IF.getInstance().setPlayState(PlayState.PLAY);
            } else if (Source.isBTMusicSource(source)) {
                if (BT_IF.getInstance().getConnState() == BTConnState.CONNECTED) {
                    BT_IF.getInstance().music_play();
                }
            }
            resetMediaPlayStateRecord();
        }*/
    }
    
    private static AudioFocusListener mAudioFocusListener = new AudioFocusListener() {
        @Override
        public void audioFocusChanged(int state) {
            Log.d(TAG,  "audioFocusChanged state="+state+"; sMuteKey_MuteState="+sMuteKey_MuteState+"; sPowerKey_MuteState="+sPowerKey_MuteState);
            switch (state) {
            case PlayState.PLAY:
//                if (sMuteKey_MuteState) {
//                    if (!Media_IF.getMute()) {
//                        mAudioFocus.requestTransientAudioFocus(false);
//                    }
//                }
                break;
            case PlayState.PAUSE:
                break;
            case PlayState.STOP:
                if (Media_IF.getMute()) {
                    Media_IF.cancelMute();
                }
                sMuteKey_MuteState = false;
                sPowerKey_MuteState = false;
                break;
            }
        }
    };
    
    /**
     * 打电话时，不能点击媒体的播放按钮，即点击无效。
     * @return true为媒体不能播放，false为可以播放。
     */
    public static boolean mediaCannotPlay() {
        return Media_IF.getCallState();
    }
    
    private static void checkAndPlayDeviceType(final int deviceType, final int fileType) {
        if (fileType == FileType.AUDIO) {
            if (Media_IF.getInstance().isPlayState() && Media_IF.getInstance().getPlayingDevice() == deviceType) {
                Log.d(TAG, "checkAndPlayDeviceType return! deviceType="+deviceType);
                return;
            }
            Media_IF.getInstance().playDefault(deviceType, FileType.AUDIO);
        } else if (fileType == FileType.VIDEO) {
            
        }
    }
    
    /**
     * 启动桌面主界面。
     */
    public static void launchLauncherActivity(Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        context.startActivity(intent);
    }
    
    /**
     * 启动音乐主界面。
     */
    public static void launchMusicMainActivity(Context context) {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName("com.haoke.mediaservice", "com.haoke.ui.media.Media_Activity_Main");
        intent.putExtra("Mode_To_Music", "music_main_home");
        context.startActivity(intent);
    }
    
    /**
     * 启动音乐播放界面。
     */
    public static void launchMusicPlayActivity(Context context) {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName("com.haoke.mediaservice", "com.haoke.ui.media.Media_Activity_Main");
        intent.putExtra("Mode_To_Music", "music_play_intent");
        context.startActivity(intent);
    }
    
    /**
     * 启动视频播放界面。
     */
    public static void launchVideoPlayActivity(Context context, FileNode fileNode) {
        Intent intent = new Intent(context, Video_Activity_Main.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("isfrom", "MediaSearchActivity");
        intent.putExtra("filepath", fileNode.getFilePath());
        context.startActivity(intent);
    }
    
    /**
     * 启动相关界面。
     * @param mode为相关模式，autoPlay为自动播放
     */
    public static void launchSourceActivity(int mode, boolean autoPlay) {
        if (autoPlay) {
            MediaService.getInstance().getModeHandler().removeCallbacksAndMessages(null);
        }
        Context context = MediaApplication.getInstance();
        // ModeSwitch.instance().setGoingFlag(true);
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction("com.haoke.data.ModeSwitch");
        switch (mode) {
        case ModeSwitch.RADIO_MODE:
            intent.setClass(context, Media_Activity_Main.class);
            intent.putExtra("Mode_To_Music", "radio_intent");
            if (autoPlay) {
//                if (!Radio_IF.getInstance().isEnable()) {
//                    Radio_IF.getInstance().setEnable(true);
//                }
                intent.putExtra("autoPlay", true);
            }
            break;
        case ModeSwitch.MUSIC_LOCAL_MODE:
            intent.setClass(context, Music_Activity_List.class);
            intent.putExtra("Mode_To_Music", "hddAudio_intent");
            if (autoPlay) {
                intent.putExtra("play_music", true);
//                checkAndPlayDeviceType(DeviceType.FLASH, FileType.AUDIO);
            }
            break;
        case ModeSwitch.MUSIC_USB1_MODE:
            intent.setClass(context, Music_Activity_List.class);
            intent.putExtra("Mode_To_Music", "USB1_intent");
            if (autoPlay) {
                intent.putExtra("play_music", true);
//                checkAndPlayDeviceType(DeviceType.USB1, FileType.AUDIO);
            }
            break;
        case ModeSwitch.MUSIC_USB2_MODE:
            intent.setClass(context, Music_Activity_List.class);
            intent.putExtra("Mode_To_Music", "USB2_intent");
            if (autoPlay) {
                intent.putExtra("play_music", true);
//                checkAndPlayDeviceType(DeviceType.USB2, FileType.AUDIO);
            }
            break;
        case ModeSwitch.MUSIC_BT_MODE:
            intent.setClass(context, Media_Activity_Main.class);
            intent.putExtra("Mode_To_Music", "btMusic_intent");
            if (autoPlay) {
                //if (!BT_IF.getInstance().music_isPlaying()) {
                //    BT_IF.getInstance().music_play();
                //}
                BT_IF.getInstance().music_play();
                intent.putExtra("autoPlay", true);
            }
            break;
        }
        context.startActivity(intent);
    }
    
    public static boolean isRunningTopActivity(Context context, String PackName, String ClassName) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
            String currentPackageName = cn.getPackageName();
            if (!TextUtils.isEmpty(currentPackageName) && currentPackageName.equals(PackName)) {
                String currentClassName = cn.getClassName();
                if (TextUtils.isEmpty(ClassName)) {
                    return true;
                } else if (!TextUtils.isEmpty(currentClassName) && currentClassName.equals(ClassName)) {
                    return true;
                }
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "isRunningTopActivity error! e="+e);
        }
        return false;
    }
    
    private static boolean isNaviApp(Context context) {
        int naviOpenKey = Settings.System.getInt(context.getContentResolver(), "naviOpenKey", 0);
        boolean isTop = false;
        if (naviOpenKey == 0) {
            //高德导航
            //com.autonavi.auto.remote.fill.UsbFillActivity
            //com.autonavi.amapauto/.MainMapActivity
            isTop = isRunningTopActivity(context, "com.autonavi.amapauto", null);
        }
        Log.d(TAG, "isNaviApp naviOpenKey="+naviOpenKey+"; isTop="+isTop);
        return naviOpenKey == 0 ? isTop : true;
    }
    
    private static long start = -1;
    private static int sLastDeviceType = -1;
    /**
     * 开机时得跳到上一次关机时所播放的源。
     * 返回值大于等于0为需要延时的时间数，单位毫秒， -1为无需再次调用
     */
    public static int checkSourceFromBoot(final MediaService service) {
        if (BTMusicService.getInstance() == null || RadioService.getInstance() == null) {
            Log.d(TAG, "checkSourceFromBoot BTMusicService or RadioService not startup!");
            return 200;
        }
        if (!BT_IF.getInstance().isServiceConnected() || !BTMusic_IF.getInstance().isServiceConnected()
                || !Media_IF.getInstance().isServiceConnected() || !Radio_IF.getInstance().isServiceConnected()) {
            Log.d(TAG, "checkSourceFromBoot BT_IF or BTMusic_IF or Media_IF or Radio_IF cannot bind service!");
            return 200;
        }
        
        AllMediaList.notifyUpdateAppWidgetByAll();
        
        //null为carmanager没有收到mcu给的信号，true为断B+起来，false为断acc休眠起来
        final Boolean power = Media_IF.getInstance().isFirstPower();
        if (power == null) {
            Log.d(TAG, "checkSourceFromBoot power is null!");
            return 100;
        } else if (power.booleanValue()) {
            Log.d(TAG, "checkSourceFromBoot power is true! clear app data!");
            FirstPowerReceiver.clearAppDataFromBoot(service);
            return -1;
        }
        
        int ms = -1;
        //int source = ModeDef.NULL;
        final int source = Media_IF.getCurSource();
        if (Source.isRadioSource(source)) {
            if (!Radio_IF.getInstance().isEnable()) {
                if (Media_IF.getMute()) {
                    Media_IF.cancelMute();
                }
                Radio_IF.getInstance().setEnable(true);
            }
            if (!isNaviApp(service)) {
                launchSourceActivity(ModeSwitch.RADIO_MODE, false);
            }
        } else if (Source.isAudioSource(source) || Source.isVideoSource(source)) {
            AllMediaList allMediaList = AllMediaList.instance(service);
            if (sLastDeviceType == -1) {
                if (Source.isAudioSource(source)) {
                    sLastDeviceType = allMediaList.getLastDeviceType();
                } else {
                    sLastDeviceType = allMediaList.getLastDeviceTypeVideo();
                }
            }
            int fileType = (Source.isAudioSource(source) ? FileType.AUDIO : FileType.VIDEO);
            boolean currPlaying = Media_IF.getInstance().isPlayState() || Video_IF.getInstance().isPlayState();
            boolean lastPlaying = true;//allMediaList.getPlayState(fileType);
            Log.d(TAG, "checkSourceFromBoot LastDeviceType="+sLastDeviceType+"; lastPlaying="+lastPlaying+"; currPlaying="+currPlaying);
            if (!currPlaying && lastPlaying && sLastDeviceType != DeviceType.NULL) {
                if (sLastDeviceType == DeviceType.COLLECT) {
                    int waitMs = waitUsbMounted(service);
                    if (waitMs > 0) {
                        return waitMs;
                    }
                }
                StorageBean storage = allMediaList.getStoragBean(sLastDeviceType);
                Log.d(TAG, "checkSourceFromBoot storage="+storage);
                if (sLastDeviceType == DeviceType.COLLECT || storage.isLoadCompleted()) {
                    ArrayList<FileNode> lists = allMediaList.getMediaList(sLastDeviceType, fileType);
                    int size = lists.size();
                    if (size > 0) {
                        FileNode lastFileNode = allMediaList.getPlayTime(sLastDeviceType, fileType);
                        if (lastFileNode != null) {
                            if (Source.isAudioSource(source)) {
                                if (Media_IF.getMute()) {
                                    Media_IF.cancelMute();
                                }
                                checkAndPlayDeviceType(sLastDeviceType, fileType);
                            }
                            if (!isNaviApp(service)) {
                                if (Source.isAudioSource(source)) {
                                    launchMusicPlayActivity(service);
                                } else {
                                    launchVideoPlayActivity(service, lastFileNode);
                                }
                            }
                        } else {
                            // song not exist
                            Log.d(TAG, "checkSourceFromBoot song not exist!");
                        }
                    } else {
                        // device has no song
                        Log.d(TAG, "checkSourceFromBoot device has no song");
                    }
                } else {
                    // loading
                    boolean mounted = storage.isMounted();
                    Log.d(TAG, "checkSourceFromBoot loading mounted="+mounted);
                    if (mounted) {
                        long end = System.currentTimeMillis();
                        if (start == -1) {
                            start = end;
                            ms = 500;
                        } else if (end - start > 40000) {
                            Log.d(TAG, "checkSourceFromBoot loading timeout!");
                        } else {
                            ms = 500;
                        }
                    } else {
                        long end = System.currentTimeMillis();
                        if (start == -1) {
                            start = end;
                            ms = 500;
                        } else if (end - start > 40000) {
                            Log.d(TAG, "checkSourceFromBoot mounting timeout!");
                        } else {
                            ms = 500;
                        }
                    }
                }
            }
        } else if (Source.isBTMusicSource(source)) {
            BT_IF btIF = BT_IF.getInstance();
            int state = btIF.getConnState();
            if (state == BTConnState.DISCONNECTED) {
                
            } else if (state == BTConnState.CONNECTED) {
                if (!BT_IF.getInstance().music_isPlaying()) {
                    if (Media_IF.getMute()) {
                        Media_IF.cancelMute();
                    }
                    BT_IF.getInstance().music_play();
                }
                if (!isNaviApp(service)) {
                    launchSourceActivity(ModeSwitch.MUSIC_BT_MODE, false);
                }
            }
        }
        Log.d(TAG, "checkSourceFromBoot source="+source+"; ms="+ms);
        return ms;
    }
    
    private static long sWaitUsb1Mounted = 0;
    private static long sWaitUsb2Mounted = 0;
    private static int waitUsbMounted(final MediaService service) {
        AllMediaList allMediaList = AllMediaList.instance(service);
        StorageBean storage1 = allMediaList.getStoragBean(DeviceType.USB1);
        if (sWaitUsb1Mounted == -1) {
        } else if (storage1.isMounted()) {
            if (!storage1.isLoadCompleted()) {
                Log.d(TAG, "checkSourceFromBoot collect must wait usb1 load completed!");
                return 1000;
            }
        } else {
            long end = System.currentTimeMillis();
            if (sWaitUsb1Mounted == 0) {
                sWaitUsb1Mounted = end;
                return 1000;
            } else if (end - sWaitUsb1Mounted > 8000) {
                Log.d(TAG, "checkSourceFromBoot wait usb1 mounted timeout!");
                sWaitUsb1Mounted = -1;
            } else {
                return 1000;
            }
        }
        StorageBean storage2 = allMediaList.getStoragBean(DeviceType.USB2);
        if (sWaitUsb2Mounted == -1) {
        } else if (storage2.isMounted()) {
            if (!storage2.isLoadCompleted()) {
                Log.d(TAG, "checkSourceFromBoot collect must wait usb2 load completed!");
                return 1000;
            }
        } else {
            long end = System.currentTimeMillis();
            if (sWaitUsb2Mounted == 0) {
                sWaitUsb2Mounted = end;
                return 1000;
            } else if (end - sWaitUsb2Mounted > 8000) {
                Log.d(TAG, "checkSourceFromBoot wait usb2 mounted timeout!");
                sWaitUsb2Mounted = -1;
            } else {
                return 1000;
            }
        }
        return -1;
    }
    
    private static final long BUTTON_CLICK_DELAY = 400;
    private static long sLastClickTime = -1;
    public static boolean isButtonClickTooFast() {
        long clickTime = System.currentTimeMillis();
        long time = Math.abs(clickTime - sLastClickTime);
        if (time < BUTTON_CLICK_DELAY) {
            Log.d(TAG, "isButtonClickTooFast: you click too fast!");
            return true;
        }
        sLastClickTime = clickTime;
        return false;
    }
    
    private static final String KEY_MODE_RECORD_USER_NAME = "username";//用户
    private static final String KEY_MODE_RECORD_SOURCE = "source"; //源
    private static final String KEY_MODE_RECORD_DISPLAY = "display"; //是否显示界面
    private static final int SOURCE_RADIO = 0;    //播放收音机（默认的源）
    private static final int SOURCE_MUSIC = 1;    //播放音乐
    private static final int SOURCE_BTMUSIC = 2;  //播放蓝牙音乐
    private static final int SOURCE_VIDEO = 3;    //播放视频（视频忽略display参数，进入视频界面）
    private static final int DISPLAY_OFF = 0;     //后台播放（默认后台）
    private static final int DISPLAY_ON = 1;      //需要播放并显示界面
    private static int sModeRecordWaitTimeOut = 0;
    public static void checkModeRecord(MediaService service, Intent intent) {
        String username = intent.getStringExtra(KEY_MODE_RECORD_USER_NAME);
        int source = intent.getIntExtra(KEY_MODE_RECORD_SOURCE, SOURCE_RADIO);
        int display = intent.getIntExtra(KEY_MODE_RECORD_DISPLAY, DISPLAY_OFF);
        checkModeRecordInternal(service, username, source, display);
    }
    
    private static void checkModeRecordInternal(
            final MediaService service, final String username, 
            final int source, final int display) {
        final int ms = checkModeRecordInternalEx(service, username, source, display);
        if (ms >= 0) {
            if (sModeRecordWaitTimeOut > 80000) {
                Log.e(TAG, "checkModeRecordInternal sModeRecordWaitTimeOut="+sModeRecordWaitTimeOut);
            } else {
                sModeRecordWaitTimeOut += ms;
                service.getModeHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        checkModeRecordInternal(service, username, source, display);
                    }
                }, ms);
            }
        }
    }
    
    private static int checkModeRecordInternalEx(
            final MediaService service, final String username, 
            final int source, final int display) {
        if (BTMusicService.getInstance() == null || RadioService.getInstance() == null) {
            Log.d(TAG, "checkModeRecordInternalEx BTMusicService or RadioService not startup!");
            return 200;
        }
        if (!BT_IF.getInstance().isServiceConnected() || !BTMusic_IF.getInstance().isServiceConnected()
                || !Media_IF.getInstance().isServiceConnected() || !Radio_IF.getInstance().isServiceConnected()) {
            Log.d(TAG, "checkModeRecordInternalEx BT_IF or BTMusic_IF or Media_IF or Radio_IF cannot bind service!");
            return 200;
        }
        
        //AllMediaList.notifyUpdateAppWidget(ModeDef.NULL);
        
        //null为carmanager没有收到mcu给的信号，true为断B+起来，false为断acc休眠起来
        //final Boolean power = Media_IF.getInstance().isFirstPower();
        //if (power == null) {
        //    Log.d(TAG, "checkSourceFromBoot power is null!");
        //    return 100;
        //} else if (power.booleanValue()) {
        //    Log.d(TAG, "checkSourceFromBoot power is true! clear app data!");
        //    FirstPowerReceiver.clearAppDataFromBoot(service);
        //    return -1;
        //}
        
        int ms = -1;
        if (source == SOURCE_RADIO) {
            if (!Radio_IF.getInstance().isEnable()) {
                if (Media_IF.getMute()) {
                    Media_IF.cancelMute();
                }
                Radio_IF.getInstance().setEnable(true);
            }
            if (display == DISPLAY_ON) {
                launchSourceActivity(ModeSwitch.RADIO_MODE, false);
            }
        } else if (source == SOURCE_MUSIC || source == SOURCE_VIDEO) {
            AllMediaList allMediaList = AllMediaList.instance(service);
            if (sLastDeviceType == -1) {
                if (source == SOURCE_MUSIC) {
                    sLastDeviceType = allMediaList.getLastDeviceType();
                } else {
                    sLastDeviceType = allMediaList.getLastDeviceTypeVideo();
                }
            }
            int fileType = (source == SOURCE_MUSIC ? FileType.AUDIO : FileType.VIDEO);
            boolean currPlaying = Media_IF.getInstance().isPlayState() || Video_IF.getInstance().isPlayState();
            boolean lastPlaying = true;//allMediaList.getPlayState(fileType);
            Log.d(TAG, "checkModeRecordInternalEx LastDeviceType="+sLastDeviceType+"; lastPlaying="+lastPlaying+"; currPlaying="+currPlaying);
            if (!currPlaying && lastPlaying && sLastDeviceType != DeviceType.NULL) {
                if (sLastDeviceType == DeviceType.COLLECT) {
                    int waitMs = waitUsbMounted(service);
                    if (waitMs > 0) {
                        return waitMs;
                    }
                }
                StorageBean storage = allMediaList.getStoragBean(sLastDeviceType);
                Log.d(TAG, "checkModeRecordInternalEx storage="+storage);
                if (sLastDeviceType == DeviceType.COLLECT || storage.isLoadCompleted()) {
                    ArrayList<FileNode> lists = allMediaList.getMediaList(sLastDeviceType, fileType);
                    int size = lists.size();
                    if (size > 0) {
                        FileNode lastFileNode = allMediaList.getPlayTime(sLastDeviceType, fileType);
                        if (lastFileNode != null) {
                            if (Media_IF.getMute()) {
                                Media_IF.cancelMute();
                            }
                            if (fileType == FileType.AUDIO) {
                                checkAndPlayDeviceType(sLastDeviceType, fileType);
                                if (display == DISPLAY_ON) {
                                    launchMusicPlayActivity(service);
                                }
                            } else {
                                launchVideoPlayActivity(service, lastFileNode);
                            }
                        } else {
                            Log.d(TAG, "checkModeRecordInternalEx song not exist!");
                        }
                    } else {
                        Log.d(TAG, "checkModeRecordInternalEx device has no song");
                    }
                } else {
                    boolean mounted = storage.isMounted();
                    Log.d(TAG, "checkModeRecordInternalEx loading mounted="+mounted);
                    if (mounted) {
                        long end = System.currentTimeMillis();
                        if (start == -1) {
                            start = end;
                            ms = 500;
                        } else if (end - start > 40000) {
                            Log.d(TAG, "checkModeRecordInternalEx loading timeout!");
                        } else {
                            ms = 500;
                        }
                    } else {
                        long end = System.currentTimeMillis();
                        if (start == -1) {
                            start = end;
                            ms = 500;
                        } else if (end - start > 40000) {
                            Log.d(TAG, "checkModeRecordInternalEx mounting timeout!");
                        } else {
                            ms = 500;
                        }
                    }
                }
            }
        } else if (source == SOURCE_BTMUSIC) {
            BT_IF btIF = BT_IF.getInstance();
            int state = btIF.getConnState();
            if (state == BTConnState.DISCONNECTED) {
                
            } else if (state == BTConnState.CONNECTED) {
                if (Media_IF.getMute()) {
                    Media_IF.cancelMute();
                }
                BT_IF.getInstance().music_play();
                if (display == DISPLAY_ON) {
                    launchSourceActivity(ModeSwitch.MUSIC_BT_MODE, false);
                }
            }
        }
        Log.d(TAG, "checkModeRecordInternalEx source="+source+"; ms="+ms);
        return ms;
    }
}
