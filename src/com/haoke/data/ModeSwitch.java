package com.haoke.data;

import android.content.Context;

import com.amd.bt.BT_IF;
import com.amd.util.Source;
import com.haoke.btjar.main.BTDef.BTConnState;
import com.haoke.constant.MediaUtil;
import com.haoke.util.Media_IF;

public class ModeSwitch {
    private static final String TAG = "ModeSwitch";
    private static ModeSwitch sInstance;
    synchronized public static ModeSwitch instance() {
        if (sInstance == null) {
            sInstance = new ModeSwitch();
        }
        return sInstance;
    }
    
    /**
     * 收音机 --> 本地音乐 --> USB1音乐 --> USB2音乐 --> BT音乐  ... --> 收音机。
     */
    public static final int EMPTY_MODE = 0;
    public static final int RADIO_MODE = Source.RADIO;
    public static final int MUSIC_LOCAL_MODE = Source.AUDIO_FLASH;
    public static final int MUSIC_USB1_MODE = Source.AUDIO_USB1;
    public static final int MUSIC_USB2_MODE = Source.AUDIO_USB2;
    public static final int MUSIC_BT_MODE = Source.BT;
    public static final int MUSIC_COLLECT_MODE = Source.AUDIO_COLLECT;
    
    public static final int[] sModeList = new int[] {
        RADIO_MODE, MUSIC_LOCAL_MODE, MUSIC_USB1_MODE, MUSIC_USB2_MODE,
        MUSIC_BT_MODE, };
    
    private boolean goingFlag;

    public boolean isGoingFlag() {
        return goingFlag;
    }

    public void setGoingFlag(boolean goingFlag) {
        this.goingFlag = goingFlag;
    }

    public void setCurrentMode(Context context, boolean markShow, int currentMode) {
    	PlayStateSharedPreferences.instance(context).saveSwitchMode(markShow);
        if (currentMode != 0) {
            PlayStateSharedPreferences.instance(context).saveSwitchMode(currentMode);
        }
    }
    
    private static int sCurSourceMode = EMPTY_MODE;
    public static void setCurSourceMode(int source) {
        sCurSourceMode = source;
    }
    
    public int getNextMode(Context context) {
//        int currentMode = PlayStateSharedPreferences.instance(context).getSwitchMode();
//        int currentMode = Media_IF.getCurSource();
        int currentMode = sCurSourceMode;
        int nextIndex = 0;
        for (int index = 0; index < sModeList.length; index++) {
            if (currentMode == sModeList[index]) {
                nextIndex = (index == sModeList.length - 1) ? 0 : index + 1;
            }
        }
        int nextMode = sModeList[nextIndex];
        boolean getModeFlag = false;
        while (!getModeFlag) {
            switch (nextMode) {
            case RADIO_MODE: // 收音机是一直存在的界面，不需要判断什么。
                getModeFlag = true;
                break;
            case MUSIC_LOCAL_MODE: // 本地音乐也是一直存在的。
                getModeFlag = true;
                break;
            case MUSIC_USB1_MODE: // 需要判断USB1是否存在。不存在，就进入USB2。
                if (AllMediaList.instance(context).getStoragBean(MediaUtil.DEVICE_PATH_USB_1).isMounted()) {
                    getModeFlag = true;
                } else {
                    nextMode = MUSIC_USB2_MODE;
                }
                break;
            case MUSIC_USB2_MODE: // 需要判断USB2是否存在。不存在，就进入BT模式。
                if (AllMediaList.instance(context).getStoragBean(MediaUtil.DEVICE_PATH_USB_2).isMounted()) {
                    getModeFlag = true;
                } else {
                    nextMode = MUSIC_BT_MODE;
                }
                break;
            case MUSIC_BT_MODE: // 需要判断BT连接是否存在。不存在，就进入Radio界面。
                if (BT_IF.getInstance().getConnState() == BTConnState.CONNECTED) {
                    getModeFlag = true;
                } else {
                    nextMode = RADIO_MODE;
                }
                break;
            }
        }
        setCurSourceMode(nextMode);
        return nextMode;
    }
}
