package com.amd.util;

import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.define.ModeDef;
import com.haoke.util.Media_IF;

public class Source {
    public static final int NULL = 0;
    public static final int RADIO = 1;
    public static final int BT = 2;
    
    private static final int AUDIO = 100;
    public static final int AUDIO_COLLECT = AUDIO + DeviceType.COLLECT;
    public static final int AUDIO_FLASH = AUDIO + DeviceType.FLASH;
    public static final int AUDIO_USB1 = AUDIO + DeviceType.USB1;
    public static final int AUDIO_USB2 = AUDIO + DeviceType.USB2;
    public static final int AUDIO_USB3 = AUDIO + DeviceType.USB3;
    public static final int AUDIO_USB4 = AUDIO + DeviceType.USB4;
    private static final int AUDIO_MAX = 199;
    
    private static final int VIDEO = 200;
    public static final int VIDEO_COLLECT = VIDEO + DeviceType.COLLECT;
    public static final int VIDEO_FLASH = VIDEO + DeviceType.FLASH;
    public static final int VIDEO_USB1 = VIDEO + DeviceType.USB1;
    public static final int VIDEO_USB2 = VIDEO + DeviceType.USB2;
    public static final int VIDEO_USB3 = VIDEO + DeviceType.USB3;
    public static final int VIDEO_USB4 = VIDEO + DeviceType.USB4;
    private static final int VIDEO_MAX = 299;
    
    public static boolean isAudioSource() {
        return isAudioSource(Media_IF.getCurSource());
    }
    
    public static boolean isAudioSource(int source) {
        if (source > AUDIO && source < AUDIO_MAX) {
            return true;
        }
        return false;
    }
    
    public static int getAudioSource(int deviceType) {
        return AUDIO + deviceType;
    }
    
    public static boolean isVideoSource() {
        return isVideoSource(Media_IF.getCurSource());
    }
    
    public static boolean isVideoSource(int source) {
        if (source > VIDEO && source < VIDEO_MAX) {
            return true;
        }
        return false;
    }
    
    public static int getVideoSource(int deviceType) {
        return VIDEO + deviceType;
    }
    
    public static boolean isBTMusicSource() {
        return Media_IF.getCurSource() == Source.BT;
    }
    
    public static boolean isBTMusicSource(int source) {
        return source == Source.BT;
    }
    
    public static boolean setBTMusicSource() {
        return Media_IF.setCurSource(Source.BT);
    }
    
    public static boolean isMcuMode(int mode) {
        return mode == com.haoke.define.ModeDef.MCU;
    }
    
    public static boolean isBTMode(int mode) {
        return mode == com.haoke.define.ModeDef.BT;
    }
    
    public static boolean isBTMusicMode(int mode) {
        return mode == com.haoke.define.ModeDef.BTMUSIC;
    }
    
    public static boolean isEQMode(int mode) {
        return mode == com.haoke.define.ModeDef.EQ;
    }
    
    public static boolean isRadioMode(int mode) {
        return mode == com.haoke.define.ModeDef.RADIO;
    }
    
    public static boolean isRadioSource() {
        return Media_IF.getCurSource() == Source.RADIO;
    }
    
    public static boolean isRadioSource(int source) {
        return source == Source.RADIO;
    }
    
    public static boolean setRadioSource() {
        return Media_IF.setCurSource(Source.RADIO);
    }
    
    public static int changeToMcuSource(int source) {
        int newSource = ModeDef.NULL;
        switch (source) {
        case NULL:
            newSource = ModeDef.NULL;
            break;
        case RADIO:
            newSource = ModeDef.RADIO;
            break;
        case BT:
            newSource = ModeDef.BT;
            break;
        case AUDIO:
        case AUDIO_COLLECT:
        case AUDIO_FLASH:
        case AUDIO_USB1:
        case AUDIO_USB2:
        case AUDIO_USB3:
        case AUDIO_USB4:
            newSource = ModeDef.AUDIO;
            break;
        case VIDEO:
        case VIDEO_COLLECT:
        case VIDEO_FLASH:
        case VIDEO_USB1:
        case VIDEO_USB2:
        case VIDEO_USB3:
        case VIDEO_USB4:
            newSource = ModeDef.VIDEO;
            break;
        }
        return newSource;
    }
    
}