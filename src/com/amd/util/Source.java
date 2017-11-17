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
        if (isAudioSource(source)) {
            return ModeDef.AUDIO;
        } else if (isVideoSource(source)) {
            return ModeDef.VIDEO;
        } else if (isRadioSource(source)) {
            return ModeDef.RADIO;
        } else if (isBTMusicSource(source)) {
            return ModeDef.BT;
        } else {
            return ModeDef.NULL;
        }
    }
    
    /**
     * 只供Media_IF调用;返回值为com.haoke.define.ModeDef.xx
     */
    public static int getDeviceFromSource(int source) {
        if (isAudioSource(source) || isVideoSource(source)) {
            int deviceType = isAudioSource(source) ? source - AUDIO : source - VIDEO;
            int mode = ModeDef.NULL;
            switch (deviceType) {
            case DeviceType.COLLECT:
                mode = ModeDef.FAVOR;
                break;
            case DeviceType.FLASH:
                mode = ModeDef.FLASH;
                break;
            case DeviceType.USB1:
                mode = ModeDef.USB1;
                break;
            case DeviceType.USB2:
                mode = ModeDef.USB2;
                break;
            case DeviceType.USB3:
                mode = ModeDef.USB3;
                break;
            case DeviceType.USB4:
                mode = ModeDef.USB4;
                break;
            }
            return mode;
        } else if (isRadioSource(source)) {
            return ModeDef.RADIO;
        } else if (isBTMusicSource(source)) {
            return ModeDef.BT;
        } else {
            return ModeDef.NULL;
        }
    }
    
    /**
     * 只供Media_IF调用;返回值为com.haoke.define.ModeDef.MediaType.xx
     */
    public static int getTypeFromSource(int source) {
        if (isVideoSource(source)) {
            return com.haoke.define.ModeDef.MediaType.VIDEO;
        } else {
            return com.haoke.define.ModeDef.MediaType.AUDIO;
        }
    }
    
}