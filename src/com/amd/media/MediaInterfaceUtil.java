package com.amd.media;

import java.io.File;
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
import android.widget.Toast;

import com.amd.bt.BTMusic_IF;
import com.amd.bt.BT_IF;
import com.amd.media.AudioFocus.AudioFocusListener;
import com.amd.radio.Radio_IF;
import com.amd.util.AmdConfig;
import com.amd.util.SkinManager;
import com.amd.util.Source;
import com.haoke.application.MediaApplication;
import com.haoke.bean.FileNode;
import com.haoke.bean.StorageBean;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.constant.MediaUtil.PlayState;
import com.haoke.data.AllMediaList;
import com.haoke.data.ModeSwitch;
import com.haoke.define.ModeDef;
import com.haoke.define.ModeDef.MediaType;
import com.haoke.mediaservice.R;
import com.haoke.service.MediaService;
import com.haoke.ui.media.Media_Activity_Main;
import com.haoke.ui.music.Music_Activity_List;
import com.haoke.ui.video.Video_Activity_Main;
import com.haoke.util.DebugLog;
import com.haoke.util.Media_IF;

public class MediaInterfaceUtil {
    private static final String TAG = "MediaInterfaceUtil";
    
    // 按静音按键是否暂停音乐
    private static final boolean MUTE_PAUSE_MUSIC = false;
    
    //private static int sMediaPlayStateRecord = Source.NULL;
    
    public static final Uri URI_SKIN = Settings.System.getUriFor(SkinManager.SKIN_KEY_NAME);
    
    private static AudioFocus mAudioFocus;
    private static boolean sMuteKey_MuteState = false;
    private static boolean sPowerKey_MuteState = false;
    
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
        if (MUTE_PAUSE_MUSIC) {
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
    }
    
    public static void cancelMuteRecordPlayState(int key) {
        if (MUTE_PAUSE_MUSIC) {
            DebugLog.d(TAG, "cancelMuteRecordPlayState sMuteKey_MuteState="+sMuteKey_MuteState+"; sPowerKey_MuteState="+sPowerKey_MuteState);
            if (hasAudioFocus() || sMuteKey_MuteState || sPowerKey_MuteState) {
                DebugLog.d(TAG, "cancelMuteRecordPlayState hasAudioFocus");
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
    }
    
    private static AudioFocusListener mAudioFocusListener = new AudioFocusListener() {
        @Override
        public void audioFocusChanged(int state) {
            DebugLog.d(TAG,  "audioFocusChanged state="+state+"; sMuteKey_MuteState="+sMuteKey_MuteState+"; sPowerKey_MuteState="+sPowerKey_MuteState);
            switch (state) {
            case PlayState.PLAY:
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
    
    private static Toast sToast;
    public static void showToast(int resId, int duration) {
        showToast(MediaApplication.getInstance().getString(resId), duration);
    }
    
    public static void showToast(String string, int duration) {
        if (sToast == null) {
            sToast = Toast.makeText(MediaApplication.getInstance(), 
                    string, duration);
        } else {
            sToast.setText(string);
            sToast.setDuration(duration);
        }
        sToast.show();
    }
    
    /**
     * 打电话时，不能点击媒体的播放按钮，即点击无效。
     * @return true为媒体不能播放，false为可以播放。
     */
    private static boolean mediaCannotPlayIntenal(boolean showToast) {
        boolean calling = Media_IF.getCallState();
        if (showToast && calling) {
            showToast(R.string.in_call_cannot_play, Toast.LENGTH_SHORT);
        }
        return calling;
    }
    
    public static boolean mediaCannotPlay() {
        return mediaCannotPlayIntenal(true);
    }
    
    public static boolean mediaCannotPlayNoToast() {
        return mediaCannotPlayIntenal(false);
    }
    
    private static boolean checkAndPlayDeviceType(final int deviceType, final int fileType) {
        if (fileType == FileType.AUDIO) {
            if (Media_IF.getInstance().isPlayState() && Media_IF.getInstance().getPlayingDevice() == deviceType) {
                DebugLog.d(TAG, "checkAndPlayDeviceType return! deviceType="+deviceType);
                return true;
            }
            return Media_IF.getInstance().playDefault(deviceType, FileType.AUDIO);
        }
        return false;
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
     * 启动收音界面。
     */
    public static void launchRadioActivity(Context context, boolean autoPlay) {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName("com.haoke.mediaservice", "com.haoke.ui.media.Media_Activity_Main");
        intent.putExtra("Mode_To_Music", "radio_intent");
        if (autoPlay) {
            intent.putExtra("autoPlay", true);
        }
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
     * 启动蓝牙音乐播放界面。
     */
    public static void launchBtMusicPlayActivity(Context context, boolean autoPlay) {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName("com.haoke.mediaservice", "com.haoke.ui.media.Media_Activity_Main");
        intent.putExtra("Mode_To_Music", "btMusic_intent");
        if (autoPlay) {
            intent.putExtra("autoPlay", true);
        }
        context.startActivity(intent);
    }
    
    /**
     * 启动视频播放界面。
     */
    public static void launchVideoPlayActivity(Context context, FileNode fileNode) {
        Intent intent = new Intent(context, Video_Activity_Main.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("isfrom", "MediaSearchActivity");
        intent.putExtra("flag", "bootStartPlay");
        intent.putExtra("filepath", fileNode.getFilePath());
        context.startActivity(intent);
    }
    
    /**
     * 启动相关界面。注意：按back键会回到桌面。
     * @param mode为相关模式，autoPlay为自动播放
     */
    public static void launchSourceActivity(int mode, boolean autoPlay) {
        DebugLog.d(TAG, "launchSourceActivity mode="+mode+"; autoPlay="+autoPlay);
        if (autoPlay) {
            MediaService.getInstance().removeModeHandlerMsg();
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
                intent.putExtra("autoPlay", true);
            }
            break;
        case ModeSwitch.MUSIC_LOCAL_MODE:
            intent.setClass(context, Music_Activity_List.class);
            intent.putExtra("Mode_To_Music", "hddAudio_intent");
            if (autoPlay) {
                intent.putExtra("play_music", true);
            }
            break;
        case ModeSwitch.MUSIC_USB1_MODE:
            intent.setClass(context, Music_Activity_List.class);
            intent.putExtra("Mode_To_Music", "USB1_intent");
            if (autoPlay) {
                intent.putExtra("play_music", true);
            }
            break;
        case ModeSwitch.MUSIC_USB2_MODE:
            intent.setClass(context, Music_Activity_List.class);
            intent.putExtra("Mode_To_Music", "USB2_intent");
            if (autoPlay) {
                intent.putExtra("play_music", true);
            }
            break;
        case ModeSwitch.MUSIC_BT_MODE:
            intent.setClass(context, Media_Activity_Main.class);
            intent.putExtra("Mode_To_Music", "btMusic_intent");
            if (autoPlay) {
                //BT_IF.getInstance().music_play();
                intent.putExtra("autoPlay", true);
            }
            break;
        case ModeSwitch.MUSIC_COLLECT_MODE:
            intent.setClass(context, Music_Activity_List.class);
            intent.putExtra("Mode_To_Music", "COLLECT_intent");
            if (autoPlay) {
                intent.putExtra("play_music", true);
            }
            break;
        }
        context.startActivity(intent);
    }
    
    public static void launchSourceActivityFromDeiveType(int deviceType, int fileType, boolean autoPlay) {
        int mode = ModeSwitch.EMPTY_MODE;
        String value = null;
        switch (deviceType) {
        case DeviceType.USB1:
            mode = ModeSwitch.MUSIC_USB1_MODE;
            value = "USB1";
            break;
        case DeviceType.USB2:
            mode = ModeSwitch.MUSIC_USB2_MODE;
            value = "USB2";
            break;
        case DeviceType.FLASH:
            mode = ModeSwitch.MUSIC_LOCAL_MODE;
            value = "FLASH";
            break;
        case DeviceType.COLLECT:
            mode = ModeSwitch.MUSIC_COLLECT_MODE;
            value = "COLLECT";
            break;
        }
        if (mode == ModeSwitch.EMPTY_MODE) {
        } else if (fileType == FileType.AUDIO) {
            if (mode != ModeSwitch.EMPTY_MODE) {
                launchSourceActivity(mode, autoPlay);
            }
        } else if (fileType == FileType.VIDEO) {
            Context context = MediaApplication.getInstance();
            Intent intent = new Intent(context, Video_Activity_Main.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("isfrom", "modeSwitch");
            intent.putExtra("deviceType", deviceType);
            context.startActivity(intent);
        }
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
            DebugLog.e(TAG, "isRunningTopActivity error! e="+e);
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
        DebugLog.d(TAG, "isNaviApp naviOpenKey="+naviOpenKey+"; isTop="+isTop);
        return naviOpenKey == 0 ? isTop : true;
    }
    
    private static final String DEVICE_PATH_USB_1 = "/mnt/media_rw/usb_storage";
    private static final String DEVICE_PATH_USB_2 = "/mnt/media_rw/usb_storage1";
    public static boolean isUsbOn(int deviceType) {
        String str = null;
        if (deviceType == DeviceType.USB1) {
            str = DEVICE_PATH_USB_1;
        } else if (deviceType == DeviceType.USB2) {
            str = DEVICE_PATH_USB_2;
        } else {
            return true;
        }
        File file = new File(str);
        if (file.exists() && file.isDirectory() && file.canRead()
                && file.canExecute()) {
            return true;
        }
        return false;
    }
    
    private static long start = -1;
    private static int sLastDeviceType = -1;
    /**
     * 开机时得跳到上一次关机时所播放的源。
     * 返回值大于等于0为需要延时的时间数，单位毫秒， -1为无需再次调用
     */
    public static int checkSourceFromBoot(final MediaService service) {
        //if (BTMusicService.getInstance() == null || RadioService.getInstance() == null) {
        //    DebugLog.d(TAG, "checkSourceFromBoot BTMusicService or RadioService not startup!");
        //    return 200;
        //}
        if (!BT_IF.getInstance().isServiceConnected() || !BTMusic_IF.getInstance().isServiceConnected()
                || !Media_IF.getInstance().isServiceConnected() || !Radio_IF.getInstance().isServiceConnected()) {
            DebugLog.d(TAG, "checkSourceFromBoot BT_IF or BTMusic_IF or Media_IF or Radio_IF cannot bind service!");
            return 200;
        }
        
        AllMediaList.notifyUpdateAppWidgetByAll();
        
        //null为carmanager没有收到mcu给的信号，true为断B+起来，false为断acc休眠起来
        final Boolean power = Media_IF.getInstance().isFirstPower();
        if (power == null) {
            DebugLog.d(TAG, "checkSourceFromBoot power is null!");
            return 100;
        } else if (power.booleanValue()) {
            DebugLog.d(TAG, "checkSourceFromBoot power is true! clear app data!");
            FirstPowerReceiver.clearAppDataFromBoot(service);
            return -1;
        }
        
        DebugLog.d(TAG, "checkSourceFromBoot return -1");
        return -1;
    }
    
    private static long sWaitUsb1Mounted = 0;
    private static long sWaitUsb2Mounted = 0;
    private static int waitUsbMounted(final MediaService service) {
        AllMediaList allMediaList = AllMediaList.instance(service);
        StorageBean storage1 = allMediaList.getStoragBean(DeviceType.USB1);
        if (sWaitUsb1Mounted == -1) {
        } else if (storage1.isMounted()) {
            if (!storage1.isAllOver()) {
                DebugLog.d(TAG, "checkSourceFromBoot collect must wait usb1 load completed!");
                return 1000;
            }
        } else {
            long end = System.currentTimeMillis();
            if (sWaitUsb1Mounted == 0) {
                sWaitUsb1Mounted = end;
                return 500;
            } else if (end - sWaitUsb1Mounted > 400) {
                DebugLog.d(TAG, "checkSourceFromBoot wait usb1 mounted timeout!");
                sWaitUsb1Mounted = -1;
            } else {
                return 500;
            }
        }
        StorageBean storage2 = allMediaList.getStoragBean(DeviceType.USB2);
        if (sWaitUsb2Mounted == -1) {
        } else if (storage2.isMounted()) {
            if (!storage2.isAllOver()) {
                DebugLog.d(TAG, "checkSourceFromBoot collect must wait usb2 load completed!");
                return 1000;
            }
        } else {
            long end = System.currentTimeMillis();
            if (sWaitUsb2Mounted == 0) {
                sWaitUsb2Mounted = end;
                return 500;
            } else if (end - sWaitUsb2Mounted > 400) {
                DebugLog.d(TAG, "checkSourceFromBoot wait usb2 mounted timeout!");
                sWaitUsb2Mounted = -1;
            } else {
                return 500;
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
            DebugLog.d(TAG, "isButtonClickTooFast: you click too fast!");
            return true;
        }
        sLastClickTime = clickTime;
        return false;
    }
    
    private static final String KEY_MODE_RECORD_USER_NAME = "username";//用户
    private static final String KEY_MODE_RECORD_SOURCE = "source"; //源,source源,参见ModeDef里面的定义，默认为Modef.RADIO;假如为蓝牙音乐，则前提为蓝牙已连接
    private static final String KEY_MODE_RECORD_TYPE = "type"; //类型, 参见MediaType.xx里面的定义，  若为VIDEO，则忽略Display参数，进入视频界面
    private static final String KEY_MODE_RECORD_DISPLAY = "display"; //是否显示界面
    private static final int DISPLAY_OFF = 0;     //后台播放（默认后台）
    private static final int DISPLAY_ON = 1;      //需要播放并显示界面
    private static int sModeRecordWaitTimeOut = 0;
    private static long sRunStart = -1;
    public static void checkModeRecord(MediaService service, Intent intent) {
        String username = intent.getStringExtra(KEY_MODE_RECORD_USER_NAME);
        int source = intent.getIntExtra(KEY_MODE_RECORD_SOURCE, ModeDef.RADIO);
        int type = intent.getIntExtra(KEY_MODE_RECORD_TYPE, MediaType.AUDIO);
        int display = intent.getIntExtra(KEY_MODE_RECORD_DISPLAY, DISPLAY_OFF);
        int ourSource = Source.changeToOurSource(source, type);
        DebugLog.d(TAG, "checkModeRecord source="+source+"; type="+type+"; display="+display+"; ourSource="+ourSource);
        UsbAutoPlay.setServiceStartTime();
        checkModeRecordInternal(service, username, ourSource, display);
    }
    
    private static void checkModeRecordInternal(
            final MediaService service, final String username, 
            final int ourSource, final int display) {
        final int ms = checkModeRecordInternalEx(service, username, ourSource, display);
        if (ms >= 0) {
            if (sModeRecordWaitTimeOut > 80000) {
                DebugLog.e(TAG, "checkModeRecordInternal sModeRecordWaitTimeOut="+sModeRecordWaitTimeOut);
                sWaitUsb1Mounted = 0;
                sWaitUsb2Mounted = 0;
                sModeRecordWaitTimeOut = 0;
                sRunStart = -1;
            } else {
                sModeRecordWaitTimeOut += ms;
                service.postModeHandlerRunnable(new Runnable() {
                    @Override
                    public void run() {
                        checkModeRecordInternal(service, username, ourSource, display);
                    }
                }, ms);
            }
        } else {
            sWaitUsb1Mounted = 0;
            sWaitUsb2Mounted = 0;
            sModeRecordWaitTimeOut = 0;
            sRunStart = -1;
        }
    }
    
    private static int checkModeRecordInternalEx(
            final MediaService service, final String username, 
            final int ourSource, final int display) {
        //if (BTMusicService.getInstance() == null || RadioService.getInstance() == null) {
        //    DebugLog.d(TAG, "checkModeRecordInternalEx BTMusicService or RadioService not startup!");
        //    return 200;
        //}
        if (!BT_IF.getInstance().isServiceConnected() || !BTMusic_IF.getInstance().isServiceConnected()
                || !Media_IF.getInstance().isServiceConnected() || !Radio_IF.getInstance().isServiceConnected()) {
            DebugLog.d(TAG, "checkModeRecordInternalEx BT_IF or BTMusic_IF or Media_IF or Radio_IF cannot bind service!");
            return 200;
        }
        
        //AllMediaList.notifyUpdateAppWidgetByAll();
        
        //null为carmanager没有收到mcu给的信号，true为断B+起来，false为断acc休眠起来
        //final Boolean power = Media_IF.getInstance().isFirstPower();
        //if (power == null) {
        //    DebugLog.d(TAG, "checkSourceFromBoot power is null!");
        //    return 100;
        //} else if (power.booleanValue()) {
        //    DebugLog.d(TAG, "checkSourceFromBoot power is true! clear app data!");
        //    FirstPowerReceiver.clearAppDataFromBoot(service);
        //    return -1;
        //}
        
        int ms = -1;
        if (Source.isRadioSource(ourSource)) {
            if (!Radio_IF.getInstance().isEnable()) {
                Radio_IF.getInstance().setEnable(true);
            }
            if (display == DISPLAY_ON) {
                launchSourceActivity(ModeSwitch.RADIO_MODE, false);
            }
        } else if (Source.isBTMusicSource(ourSource)) {
            BT_IF btIF = BT_IF.getInstance();
            boolean connected = btIF.isBtMusicConnected();
            if (!connected) {
                long end = System.currentTimeMillis();
                if (sRunStart == -1) {
                    sRunStart = end;
                    ms = 500;
                } else if (end - sRunStart > 40000) {
                    DebugLog.d(TAG, "checkModeRecordInternalEx loading BtConnected timeout! open Radio!");
                    if (!Radio_IF.getInstance().isEnable()) {
                        Radio_IF.getInstance().setEnable(true);
                    }
                    if (display == DISPLAY_ON) {
                        launchSourceActivity(ModeSwitch.RADIO_MODE, false);
                    }
                } else {
                    ms = 500;
                }
            } else {
                BT_IF.getInstance().music_play();
                if (display == DISPLAY_ON) {
                    launchSourceActivity(ModeSwitch.MUSIC_BT_MODE, false);
                }
            }
        } else if (Source.isAudioSource(ourSource) || Source.isVideoSource(ourSource)) {
            AllMediaList allMediaList = AllMediaList.instance(service);
            int runDeviceType = Source.getDeviceType(ourSource);
            int fileType = (Source.isAudioSource(ourSource) ? FileType.AUDIO : FileType.VIDEO);
            boolean currPlaying = false; //Media_IF.getInstance().isPlayState() || VideoPlayController.isVideoPlaying;
            boolean lastPlaying = true;//allMediaList.getPlayState(fileType);
            DebugLog.d(TAG, "checkModeRecordInternalEx runDeviceType="+runDeviceType+"; lastPlaying="+lastPlaying+"; currPlaying="+currPlaying);
            if (!currPlaying && lastPlaying && runDeviceType != DeviceType.NULL) {
                if (runDeviceType == DeviceType.COLLECT) {
                    int waitMs = waitUsbMounted(service);
                    if (waitMs > 0) {
                        return waitMs;
                    }
                }
                StorageBean storage = allMediaList.getStoragBean(runDeviceType);
                DebugLog.d(TAG, "checkModeRecordInternalEx storage="+storage);
                if (runDeviceType == DeviceType.COLLECT || storage.isAllOver()) {
                    ArrayList<FileNode> lists = allMediaList.getMediaList(runDeviceType, fileType);
                    int size = lists.size();
                    if (size > 0) {
                        boolean success = false;
                        if (fileType == FileType.AUDIO) {
                            success = checkAndPlayDeviceType(runDeviceType, fileType);
                            if (success) {
                                if (display == DISPLAY_ON) {
                                    launchMusicPlayActivity(service);
                                }
                            } else {
                                DebugLog.d(TAG, "checkModeRecordInternalEx song not exist!");
                                if (display == DISPLAY_ON) {
                                    launchSourceActivityFromDeiveType(runDeviceType, fileType, false);
                                }
                            }
                        } else {
                            FileNode lastFileNode = allMediaList.getPlayTime(runDeviceType, fileType);
                            if (lastFileNode != null && lastFileNode.isExist(service)) {
                                success = true;
                            } else {
                                if (lastFileNode != null && lastFileNode.isExist(service)) {
                                    success = true;
                                } else {
                                    for (FileNode fileNode : lists) {
                                        if (fileNode != null && fileNode.isExist(service)) {
                                            lastFileNode = fileNode;
                                            success = true;
                                            break;
                                        }
                                    }
                                }
                            }
                            if(success) {
                                launchVideoPlayActivity(service, lastFileNode);
                            } else {
                                DebugLog.d(TAG, "checkModeRecordInternalEx video not exist!");
                            }
                        }
                    } else {
                        DebugLog.d(TAG, "checkModeRecordInternalEx device has no song");
                        if (display == DISPLAY_ON) {
                            launchSourceActivityFromDeiveType(runDeviceType, fileType, false);
                        }
                    }
                } else {
                    boolean mounted = storage.isMounted();
                    DebugLog.d(TAG, "checkModeRecordInternalEx loading mounted="+mounted);
                    if (mounted) {
                        long end = System.currentTimeMillis();
                        if (sRunStart == -1) {
                            sRunStart = end;
                            ms = 500;
                        } else if (end - sRunStart > 40000) {
                            DebugLog.d(TAG, "checkModeRecordInternalEx loading timeout!");
                            checkAllDeviceAndJump(service, username, ourSource, display);
                        } else {
                            ms = 500;
                        }
                    } else {
                        long end = System.currentTimeMillis();
                        if (sRunStart == -1) {
                            sRunStart = end;
                            ms = 500;
                        } else if (end - sRunStart > 40000) {
                            DebugLog.d(TAG, "checkModeRecordInternalEx mounting timeout!");
                            checkAllDeviceAndJump(service, username, ourSource, display);
                        } else {
                            ms = 500;
                        }
                    }
                }
            }
        }
        DebugLog.d(TAG, "checkModeRecordInternalEx ourSource="+ourSource+"; ms="+ms);
        return ms;
    }
    
    private static void checkAllDeviceAndJump(final MediaService service, final String username, 
            final int ourSource, final int display) {
        AllMediaList allMediaList = AllMediaList.instance(service);
        int runDeviceType = Source.getDeviceType(ourSource);
        int fileType = (Source.isAudioSource(ourSource) ? FileType.AUDIO : FileType.VIDEO);
        int lastDeviceType = DeviceType.NULL;
        if (fileType == FileType.VIDEO) {
            lastDeviceType = allMediaList.getLastDeviceTypeVideo();
        } else {
            lastDeviceType = allMediaList.getLastDeviceType();
        }
        final int[] deviceTypes = {lastDeviceType, DeviceType.USB1, 
                DeviceType.USB2, DeviceType.FLASH, DeviceType.COLLECT};
        boolean exist = false;
        for (int deviceType : deviceTypes) {
            if (deviceType == DeviceType.NULL || deviceType == runDeviceType) {
                continue;
            }
            StorageBean bean = allMediaList.getStoragBean(deviceType);
            if (bean.isMounted() && bean.isId3ParseCompleted()) {
                ArrayList<FileNode> lists = allMediaList.getMediaList(deviceType, fileType);
                if (lists.size() > 0) {
                    FileNode playFileNode = allMediaList.getPlayTime(deviceType, fileType);
                    if (playFileNode != null && playFileNode.isExist(service)) {
                        exist = true;
                    } else {
                        for (FileNode fileNode : lists) {
                            if (fileNode != null && fileNode.isExist(service)) {
                                playFileNode = fileNode;
                                exist = true;
                                break;
                            }
                        }
                    }
                    if (exist) {
                        DebugLog.d(TAG, "checkAllDeviceAndJump playFileNode="+playFileNode);
                        if (fileType == FileType.AUDIO) {
                            checkAndPlayDeviceType(deviceType, fileType);
                            if (display == DISPLAY_ON) {
                                launchMusicPlayActivity(service);
                            }
                        } else {
                            launchVideoPlayActivity(service, playFileNode);
                        }
                        break;
                    }
                }
            }
        }
        
        if (!exist) {
            DebugLog.d(TAG, "checkAllDeviceAndJump exist is false! runDeviceType="+runDeviceType+"; fileType="+fileType);
            launchSourceActivityFromDeiveType(runDeviceType, fileType, false);
        }
    }
    
    public static void insertUsbAndScanComplete(final int deviceType) {
        if (AmdConfig.INSERT_USB_AUTO_PLAY_MUSIC) {
            int ms = UsbAutoPlay.playDefaultMusic(deviceType);
            DebugLog.d(TAG, "insertUsbAndScanComplete ms="+ms);
            if (ms > 0) {
                MediaService.getInstance().getHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        UsbAutoPlay.playDefaultMusic(deviceType);
                    }
                }, ms);
            }
        } else if (AmdConfig.INSERT_USB_RECODRD_PLAY_MUSIC){
            RecordDevicePlay.instance().checkUsbPlay(deviceType);
        }
    }
}
