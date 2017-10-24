package com.amd.media;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;

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
import com.haoke.util.Media_IF;

public class MediaInterfaceUtil {
    private static final String TAG = "MediaInterfaceUtil";
    
    private static int sMediaPlayStateRecord = ModeDef.NULL;
    
    private static AudioFocus mAudioFocus;
    
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
    
    public static void setMuteRecordPlayState() {
        if (true) {
            if (mAudioFocus == null) {
                mAudioFocus = new AudioFocus(MediaApplication.getInstance());
                mAudioFocus.registerListener(mAudioFocusListener);
            }
            mAudioFocus.requestTransientAudioFocus(true);
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
    
    public static void cancelMuteRecordPlayState() {
        Log.d(TAG, "cancelMuteRecordPlayState");
        if (true) {
            if (hasAudioFocus()) {
                mAudioFocus.requestTransientAudioFocus(false);
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
    
    private static void checkAndPlayDeviceType(final MediaService service, final int deviceType, boolean post) {
        if (Media_IF.getInstance().isPlayState() && Media_IF.getInstance().getPlayingDevice() == deviceType) {
            Log.d(TAG, "checkAndPlayDeviceType return! deviceType="+deviceType);
            return;
        }
        if (post) {
            MediaService.getInstance().getHandler().post(new Runnable() {
                @Override
                public void run() {
                    Media_IF.getInstance().playDefault(deviceType, FileType.AUDIO);
                }
            });
        } else {
            Media_IF.getInstance().playDefault(deviceType, FileType.AUDIO);
        }
    }
    
    /**
     * 启动音乐主界面。
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
     * 启动相关界面。
     * @param mode为相关模式，autoPlay为自动播放
     */
    public static void launchSourceActivity(int mode, boolean autoPlay) {
        if (autoPlay) {
            MediaService.getInstance().getHandler().removeCallbacksAndMessages(null);
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
                checkAndPlayDeviceType(MediaService.getInstance(), DeviceType.FLASH, false);
            }
            break;
        case ModeSwitch.MUSIC_USB1_MODE:
            intent.setClass(context, Music_Activity_List.class);
            intent.putExtra("Mode_To_Music", "USB1_intent");
            if (autoPlay) {
                intent.putExtra("play_music", true);
                checkAndPlayDeviceType(MediaService.getInstance(), DeviceType.USB1, false);
            }
            break;
        case ModeSwitch.MUSIC_USB2_MODE:
            intent.setClass(context, Music_Activity_List.class);
            intent.putExtra("Mode_To_Music", "USB2_intent");
            if (autoPlay) {
                intent.putExtra("play_music", true);
                checkAndPlayDeviceType(MediaService.getInstance(), DeviceType.USB2, false);
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
    
    /**
     * 开机时得跳到上一次关机时所播放的源。
     * 返回值大于等于0为需要延时的时间数，单位毫秒， -1为无需再次调用
     */
    public static int checkSourceFromBoot(final MediaService service) {
        int ms = -1;
        if (BTMusicService.getInstance() == null || RadioService.getInstance() == null) {
            Log.d(TAG, "checkSourceFromBoot BTMusicService or RadioService not startup!");
            ms = 50;
        }
        if (ms >= 0) {
        } if (!BT_IF.getInstance().isServiceConnected() || !BTMusic_IF.getInstance().isServiceConnected()
                || !Media_IF.getInstance().isServiceConnected() || !Radio_IF.getInstance().isServiceConnected()) {
            Log.d(TAG, "checkSourceFromBoot BT_IF or BTMusic_IF or Media_IF or Radio_IF cannot bind service!");
            ms = 50;
        }
        
        final int source = Media_IF.getCurSource();
        if (ms >= 0) {
        } else if (source == ModeDef.RADIO) {
            launchSourceActivity(ModeSwitch.RADIO_MODE, true);
        } else if (source == ModeDef.AUDIO) {
            AllMediaList allMediaList = AllMediaList.instance(service);
            int deviceType = allMediaList.getLastDeviceType();
            if (deviceType != DeviceType.NULL) {
                StorageBean storage = allMediaList.getStoragBean(deviceType);
                if (storage.isId3ParseCompleted()) {
                    ArrayList<FileNode> lists = allMediaList.getMediaList(deviceType, FileType.AUDIO);
                    int size = lists.size();
                    if (size > 0) {
                        FileNode lastFileNode = allMediaList.getPlayState(deviceType, FileType.AUDIO);
                        if (lastFileNode != null) {
                            checkAndPlayDeviceType(service, deviceType, false);
                            launchMusicPlayActivity(service);
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
                    Log.d(TAG, "checkSourceFromBoot loading");
                    ms = 500;
                }
            }
        } else if (source == ModeDef.VIDEO) {
            
        } else if (source == ModeDef.BT) {
            BT_IF btIF = BT_IF.getInstance();
            int state = btIF.getConnState();
            if (state == BTConnState.DISCONNECTED) {
                
            } else if (state == BTConnState.CONNECTED) {
                launchSourceActivity(ModeSwitch.MUSIC_BT_MODE, true);
            }
        }
        Log.d(TAG, "checkSourceFromBoot source="+source+"; ms="+ms);
        return ms;
    }
}