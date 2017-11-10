package com.amd.media;

import com.amd.bt.BT_IF;
import com.amd.radio.Radio_IF;
import com.haoke.application.MediaApplication;
import com.haoke.bean.FileNode;
import com.haoke.btjar.main.BTDef.BTConnState;
import com.haoke.constant.MediaUtil.FileType;
import static com.haoke.constant.VRConstant.*;
import com.haoke.data.ModeSwitch;
import com.haoke.define.ModeDef;
import com.haoke.define.MediaDef.DeviceType;
import com.haoke.define.MediaDef.PlayState;
import com.haoke.define.MediaDef.RepeatMode;
import com.haoke.ui.image.Image_Activity_Main;
import com.haoke.ui.video.Video_Activity_Main;
import com.haoke.ui.video.Video_IF;
import com.haoke.util.DebugLog;
import com.haoke.util.Media_IF;

import android.content.Intent;
import android.text.TextUtils;
import static com.haoke.service.MediaService.*;

public class VRInterfaceUtil {
    private static final String TAG = "VRInterfaceUtil";
    private static VRInterfaceUtil sVRInterfaceUtil = null;
    private Media_IF mMediaIF = null;
    private BT_IF mBTIF = null;
    private Radio_IF mRadioIF = null;
    
    private VRInterfaceUtil() {
        mMediaIF = Media_IF.getInstance();
        mBTIF = BT_IF.getInstance();
        mRadioIF = Radio_IF.getInstance();
    }
    
    private static VRInterfaceUtil getInstance() {
        if (sVRInterfaceUtil == null) {
            sVRInterfaceUtil = new VRInterfaceUtil();
        }
        return sVRInterfaceUtil;
    }
    
    public static void VRCommand(Intent intent) {
        if (intent != null && !MediaInterfaceUtil.mediaCannotPlay()) {
            VRInterfaceUtil util = getInstance();
            switch (intent.getIntExtra(KEY_COMMAND_FROM, 0)) {
            case VALUE_FROM_SCAN:
                break;
            case VALUE_FROM_VR_APP:
                util.vrAppOperate(intent);
                break;
            case VALUE_FROM_VR_MUSIC:
                util.vrMusicOperate(intent);
                break;
            case VALUE_FROM_VR_RADIO:
                util.vrRadioOperate(intent);
                break;
            case VALUE_FROM_VR_IMAGE:
                util.vrImageOperate(intent);
                break;
            case VALUE_FROM_VR_VIDEO:
                util.vrVideoOperate(intent);
                break;
            default:
                break;
            }
        }
    }
    
    private void launchMusicPlayActivity() {
        MediaInterfaceUtil.launchMusicPlayActivity(MediaApplication.getInstance());
    }
    
    private void sendBroadcast(Intent intent) {
        MediaApplication.getInstance().sendBroadcast(intent);
    }
    
    private void startActivity(Intent intent) {
        MediaApplication.getInstance().startActivity(intent);
    }
    
    private void launchSourceActivity(int mode, boolean autoPlay) {
        MediaInterfaceUtil.launchSourceActivity(mode, autoPlay);
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
        int playState = Media_IF.getInstance().getPlayState();
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
            Intent intent = new Intent(MediaApplication.getInstance(), Video_Activity_Main.class);
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
            Intent intent = new Intent(MediaApplication.getInstance(), Image_Activity_Main.class);
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
        // 当前是Radio或BT模式不处理；如果是 磁盘音乐模式就处理。收藏当前的歌曲。
        int playState = mMediaIF.getPlayState();
        int source = Media_IF.getCurSource();
        DebugLog.d(TAG, "commandCollectMode playState="+playState+"; source="+source);
        if (source == ModeDef.AUDIO) {
            mMediaIF.collectMusic(mMediaIF.getPlayItem());
        }
    }
    
    private void commandUnCollectMode() {
        // 当前是Radio或BT模式不处理；如果是 磁盘音乐模式就处理。取消收藏当前歌曲。
        int playState = mMediaIF.getPlayState();
        int source = Media_IF.getCurSource();
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
        case VRRadio.COMMAND_PLAY_PREV_RADIO:
            commandPlayPrevRadio();
            break;
        case VRRadio.COMMAND_PLAY_NEXT_RADIO:
            commandPlayNextRadio();
            break;
        case VRRadio.COMMAND_SEARCH_NEXT_RADIO:
        	commandSearchNextRadio();
            break;
        case VRRadio.COMMAND_SEARCH_PREV_RADIO:
        	commandSearchPrevRadio();
            break;
        case VRRadio.COMMAND_SCAN_RADIO:
            commandScanRadio();
            break;
        case VRRadio.COMMAND_PLAY_FM_STATION_RADIO:
            commandPlayFMStationRadio(intent.getStringExtra(VRRadio.KEY_STATION_FREQ));
            break;
        case VRRadio.COMMAND_PLAY_AM_STATION_RADIO:
            commandPlayAMStationRadio(intent.getStringExtra(VRRadio.KEY_STATION_FREQ));
            break;
        case VRRadio.COMMAND_REFRESH_FM_RADIO:
            commandRefreshFMRadio();
            break;
        case VRRadio.COMMAND_REFRESH_AM_RADIO:
            commandRefreshAMRadio();
            break;
        default:
            break;
        }
    }
    
    private void commandCollectRadio() {
        // 收藏当前播放电台。
        if (mRadioIF.isEnable()) {
            int freq = mRadioIF.getCurFreq();
            mRadioIF.collectFreq(MediaApplication.getInstance(), freq, true);
        }
    }
    
    private void commandUnCollectRadio() {
        // 取消收藏当前播放电台。
        if (mRadioIF.isEnable()) {
            int freq = mRadioIF.getCurFreq();
            mRadioIF.uncollectFreq(MediaApplication.getInstance(), freq, true);
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
        mRadioIF.playCollectFistFreq(MediaApplication.getInstance(), true);
    }
    
    private void commandPlayPrevRadio() {
        // 切换到上一电台（频率小的）
        mRadioIF.setPreChannel();
    }
    
    private void commandPlayNextRadio() {
        // 切换到下一电台（频率大的）
        mRadioIF.setNextChannel();
    }
    
    private void commandSearchPrevRadio() {
        // 从当前波段向下搜索（频率小的）
        mRadioIF.setPreSearch();
    }
    
    private void commandSearchNextRadio() {
        // 从当前波段向上搜索（频率大的）
        mRadioIF.setNextSearch();
    }
    
    private void commandScanRadio() {
        // 扫描全波段电台
        mRadioIF.scanStore();
    }
    
    private void commandPlayFMStationRadio(String sfreq) {
        // 切换到调频，如果指定某个电台，会有station参数，需要打开界面
        mRadioIF.setCurBand();
        mRadioIF.setCurFreq(Radio_IF.sfreqToInt(sfreq));
        mRadioIF.setEnable(true);
        launchSourceActivity(ModeSwitch.RADIO_MODE, true);
    }
    
    private void commandPlayAMStationRadio(String sfreq) {
        // 切换到调幅，如果指定某个电台，会有station参数，需要打开界面
    }
    
    private void commandRefreshFMRadio() {
        // 刷新调频目录，如果当前不是调频，则先切到调频，再刷新
        mRadioIF.setCurBand();
        mRadioIF.scanStore();
    }
    
    private void commandRefreshAMRadio() {
        // 刷新调幅目录，如果当前不是调幅，则先切到调幅，再刷新
    }

}