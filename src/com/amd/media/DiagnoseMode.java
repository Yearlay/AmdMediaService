package com.amd.media;

import com.haoke.constant.MediaUtil.RepeatMode;
import com.haoke.define.McuDef;
import com.haoke.util.Media_IF;

import android.util.Log;

/**
 * 诊断模式, diagnose mode
 */
public class DiagnoseMode {
    private static final String TAG = "DiagnoseMode";
    
    public static boolean handleMusicPlayMode(int keyCode) {
        int oldMode = Media_IF.getInstance().getRepeatMode();
        int playMode = -1;
        if (keyCode == McuDef.KeyCode.INTRO) { // 全部循环（诊断模式下）
            playMode = RepeatMode.CIRCLE;
        } else if (keyCode == McuDef.KeyCode.RANDOM) { // 随机播放（诊断模式下）
            playMode = RepeatMode.RANDOM;
        } else if (keyCode == McuDef.KeyCode.REPEAT) { // 单曲循环（诊断模式下）
            playMode = RepeatMode.ONE;
        }
        Log.e(TAG, "handleMusicPlayMode: oldMode="+oldMode+"; playMode="+playMode);
        if (playMode > 0) {
            if (oldMode != playMode) {
                Media_IF.getInstance().setRepeatMode(playMode);
            }
            sendMusicPlayMode(playMode);
            return true;
        }
        return false;
    }
    
    private static void sendMusicPlayMode(int playMode) {
        Log.e(TAG, "sendMusicPlayMode: playMode="+playMode);
        if (RepeatMode.ONE == playMode
            || RepeatMode.CIRCLE == playMode
            || RepeatMode.RANDOM == playMode) {
            sendMode(playMode);
        }
    }
    
    private static void sendMode(int mode) {
        boolean isPlay = Media_IF.getInstance().isPlayState();//播放状态
        byte[] data = new byte[1];
        int value = isPlay?0x01:0x00;
        if(mode == RepeatMode.CIRCLE){//浏览播放
            value = value | (0x02 << 1);
        }else if(mode == RepeatMode.RANDOM){//随机播放
            value = value | (0x03 << 1);
        }else if(mode == RepeatMode.ONE){//重复播放
            value = value | 0x08;
        }
        data[0] = (byte) value;
        Media_IF.getInstance().sendDataToMcu((byte)0x07, (byte)0x01, data);
    }
}