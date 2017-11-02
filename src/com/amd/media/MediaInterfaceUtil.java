package com.amd.media;

import java.util.ArrayList;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import com.amd.bt.BTMusic_IF;
import com.amd.bt.BT_IF;
import com.amd.radio.Radio_IF;
import com.haoke.application.MediaApplication;
import com.amd.media.AudioFocus;
import com.amd.media.AudioFocus.AudioFocusListener;
import com.haoke.bean.FileNode;
import com.haoke.bean.StorageBean;
import com.haoke.btjar.main.BTDef.BTConnState;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.data.AllMediaList;
import com.haoke.data.ModeSwitch;
import com.haoke.define.ModeDef;
import com.haoke.define.MediaDef.DeviceType;
import com.haoke.define.MediaDef.PlayState;
import com.haoke.service.BTMusicService;
import com.haoke.service.MediaService;
import com.haoke.service.RadioService;
import com.haoke.ui.media.Media_Activity_Main;
import com.haoke.ui.music.Music_Activity_List;
import com.haoke.ui.video.Video_Activity_Main;
import com.haoke.util.Media_IF;

public class MediaInterfaceUtil {
    private static final String TAG = "MediaInterfaceUtil";
    
    private static int sMediaPlayStateRecord = ModeDef.NULL;
    
    private static AudioFocus mAudioFocus;
    private static boolean sMuteKey_MuteState = false;
    private static boolean sPowerKey_MuteState = false;
    
    public static void resetMediaPlayStateRecord(int source) {
        Log.d(TAG, "resetMediaPlayStateRecord old is "+sMediaPlayStateRecord + "; caller is "+source);
        if (source == sMediaPlayStateRecord) {
            sMediaPlayStateRecord = ModeDef.NULL;
        }
    }
    
    public static void resetMediaPlayStateRecord() {
        Log.d(TAG, "resetMediaPlayStateRecord old is "+sMediaPlayStateRecord );
        sMediaPlayStateRecord = ModeDef.NULL;
    }
    
    private static void setMediaPlayStateRecord(int source) {
        Log.d(TAG, "sMediaPlayStateRecord source="+source+"; old is "+sMediaPlayStateRecord);
        sMediaPlayStateRecord = source;
    }
    
    public static int getMediaPlayStateRecord() {
        return sMediaPlayStateRecord;
    }
    
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
        resetMediaPlayStateRecord(); 
        boolean mute = Media_IF.getMute();
        if (mute) {
            int source = Media_IF.getCurSource();
            if (source == ModeDef.RADIO) {
                if (Radio_IF.getInstance().isEnable()) {
                    setMediaPlayStateRecord(source);
                    Radio_IF.getInstance().setEnable(false);
                }
            } else if (source == ModeDef.AUDIO) {
                if (Media_IF.getInstance().isPlayState()) {
                    setMediaPlayStateRecord(source);
                    Media_IF.getInstance().setPlayState(PlayState.PAUSE);
                }
            } else if (source == ModeDef.VIDEO) {
                if (Media_IF.getInstance().isPlayState()) {
                    setMediaPlayStateRecord(source);
                    Media_IF.getInstance().setPlayState(PlayState.PAUSE);
                }
            } else if (source == ModeDef.BT) {
                if (BT_IF.getInstance().music_isPlaying()) {
                    setMediaPlayStateRecord(source);
                    BT_IF.getInstance().music_pause();
                }
            }
        }
        Log.d(TAG, "setMute mute="+mute+"; sMediaPlayStateRecord="+sMediaPlayStateRecord);
    }
    
    public static void cancelMuteRecordPlayState(int key) {
        Log.d(TAG, "cancelMuteRecordPlayState sMuteKey_MuteState="+sMuteKey_MuteState+"; sPowerKey_MuteState="+sPowerKey_MuteState);
        if (true) {
            if (hasAudioFocus()) {
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
        int source = getMediaPlayStateRecord();
        if (source != ModeDef.NULL) {
            if (source == ModeDef.RADIO) {
                Radio_IF.getInstance().setEnable(true);
            } else if (source == ModeDef.AUDIO) {
                Media_IF.getInstance().setPlayState(PlayState.PLAY);
            } else if (source == ModeDef.VIDEO) {
                Media_IF.getInstance().setPlayState(PlayState.PLAY);
            } else if (source == ModeDef.BT) {
                if (BT_IF.getInstance().getConnState() == BTConnState.CONNECTED) {
                    BT_IF.getInstance().music_play();
                }
            }
            resetMediaPlayStateRecord();
        }
    }
    
    private static AudioFocusListener mAudioFocusListener = new AudioFocusListener() {
        @Override
        public void audioFocusChanged(int state) {
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
                if (!Radio_IF.getInstance().isEnable()) {
                    Radio_IF.getInstance().setEnable(true);
                }
            }
            break;
        case ModeSwitch.MUSIC_LOCAL_MODE:
            intent.setClass(context, Music_Activity_List.class);
            intent.putExtra("Mode_To_Music", "hddAudio_intent");
            if (autoPlay) {
                intent.putExtra("play_music", true);
                checkAndPlayDeviceType(DeviceType.FLASH, FileType.AUDIO);
            }
            break;
        case ModeSwitch.MUSIC_USB1_MODE:
            intent.setClass(context, Music_Activity_List.class);
            intent.putExtra("Mode_To_Music", "USB1_intent");
            if (autoPlay) {
                intent.putExtra("play_music", true);
                checkAndPlayDeviceType(DeviceType.USB1, FileType.AUDIO);
            }
            break;
        case ModeSwitch.MUSIC_USB2_MODE:
            intent.setClass(context, Music_Activity_List.class);
            intent.putExtra("Mode_To_Music", "USB2_intent");
            if (autoPlay) {
                intent.putExtra("play_music", true);
                checkAndPlayDeviceType(DeviceType.USB2, FileType.AUDIO);
            }
            break;
        case ModeSwitch.MUSIC_BT_MODE:
            intent.setClass(context, Media_Activity_Main.class);
            intent.putExtra("Mode_To_Music", "btMusic_intent");
            if (autoPlay) {
                if (!BT_IF.getInstance().music_isPlaying()) {
                    BT_IF.getInstance().music_play();
                }
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
        
        AllMediaList.notifyUpdateAppWidget(ModeDef.NULL);
        
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
        final int source = Media_IF.getCurSource();
        if (source == ModeDef.RADIO) {
            if (!Radio_IF.getInstance().isEnable()) {
                Radio_IF.getInstance().setEnable(true);
            }
            if (!isNaviApp(service)) {
                launchSourceActivity(ModeSwitch.RADIO_MODE, false);
            }
        } else if (source == ModeDef.AUDIO || source == ModeDef.VIDEO) {
            AllMediaList allMediaList = AllMediaList.instance(service);
            if (sLastDeviceType == -1) {
                if (source == ModeDef.AUDIO) {
                    sLastDeviceType = allMediaList.getLastDeviceType();
                } else {
                    sLastDeviceType = allMediaList.getLastDeviceTypeVideo();
                }
            }
            int fileType = (source == ModeDef.AUDIO ? FileType.AUDIO : FileType.VIDEO);
            boolean playing = true;//allMediaList.getPlayState(fileType);
            Log.d(TAG, "checkSourceFromBoot LastDeviceType="+sLastDeviceType+"; playing="+playing);
            if (playing && sLastDeviceType != DeviceType.NULL) {
                StorageBean storage = allMediaList.getStoragBean(sLastDeviceType);
                Log.d(TAG, "checkSourceFromBoot storage="+storage);
                if (storage.isLoadCompleted()) {
                    ArrayList<FileNode> lists = allMediaList.getMediaList(sLastDeviceType, fileType);
                    int size = lists.size();
                    if (size > 0) {
                        FileNode lastFileNode = allMediaList.getPlayTime(sLastDeviceType, fileType);
                        if (lastFileNode != null) {
                            if (source == ModeDef.AUDIO) {
                                checkAndPlayDeviceType(sLastDeviceType, fileType);
                            }
                            if (!isNaviApp(service)) {
                                if (source == ModeDef.AUDIO) {
                                    launchMusicPlayActivity(service);
                                } else {
                                    launchVideoPlayActivity(service, lastFileNode);
                                }
                            }
                        } else {
                            //TODO song not exist
                            Log.d(TAG, "checkSourceFromBoot song not exist!");
                        }
                    } else {
                        //TODO device has no song
                        Log.d(TAG, "checkSourceFromBoot device has no song");
                    }
                } else {
                    //TODO loading
                    boolean mounted = storage.isMounted();
                    Log.d(TAG, "checkSourceFromBoot loading mounted="+mounted);
                    if (mounted) {
                        ms = 500;
                    } else {
                        long end = System.currentTimeMillis();
                        if (start == -1) {
                            start = end;
                            ms = 500;
                        } else if (end - start > 40000) {
                            Log.d(TAG, "checkSourceFromBoot loading timeout!");
                        } else {
                            ms = 500;
                        }
                    }
                }
            }
        } else if (source == ModeDef.BT) {
            BT_IF btIF = BT_IF.getInstance();
            int state = btIF.getConnState();
            if (state == BTConnState.DISCONNECTED) {
                
            } else if (state == BTConnState.CONNECTED) {
                if (!BT_IF.getInstance().music_isPlaying()) {
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
}