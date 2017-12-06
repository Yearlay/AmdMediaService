package com.amd.util;

import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.define.ModeDef;
import com.haoke.define.ModeDef.MediaType;
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
    
    /**
     * 判断当前源是不是音乐
     */
    public static boolean isAudioSource() {
        return isAudioSource(Media_IF.getCurSource());
    }
    
    /**
     * 判断我们的源是不是音乐
     */
    public static boolean isAudioSource(int source) {
        if (source > AUDIO && source < AUDIO_MAX) {
            return true;
        }
        return false;
    }
    
    /**
     * 通过设备类型得到音乐源
     */
    public static int getAudioSource(int deviceType) {
        return AUDIO + deviceType;
    }
    
    /**
     * 判断当前源是不是视频
     */
    public static boolean isVideoSource() {
        return isVideoSource(Media_IF.getCurSource());
    }
    
    /**
     * 判断我们的源是不是视频
     */
    public static boolean isVideoSource(int source) {
        if (source > VIDEO && source < VIDEO_MAX) {
            return true;
        }
        return false;
    }
    
    /**
     * 通过设备类型得到视频源
     */
    public static int getVideoSource(int deviceType) {
        return VIDEO + deviceType;
    }
    
    /**
     * 判断当前源是不是蓝牙音乐
     */
    public static boolean isBTMusicSource() {
        return Media_IF.getCurSource() == Source.BT;
    }
    
    /**
     * 判断我们的源是不是蓝牙音乐
     */
    public static boolean isBTMusicSource(int source) {
        return source == Source.BT;
    }
    
    /**
     * 设置当前源为蓝牙音乐
     */
    public static boolean setBTMusicSource() {
        return Media_IF.setCurSource(Source.BT);
    }
    
    /**
     * 判断模式是否为mcu，仅限onCarDataChange或者CallBack调用
     */
    public static boolean isMcuMode(int mode) {
        return mode == com.haoke.define.ModeDef.MCU;
    }
    
    /**
     * 判断模式是否为bt，仅限onCarDataChange或者CallBack调用
     */
   public static boolean isBTMode(int mode) {
        return mode == com.haoke.define.ModeDef.BT;
    }
    
   /**
    * 暂时作废，因为都是用的BT.
    * 判断模式是否为蓝牙音乐，仅限onCarDataChange或者CallBack调用
    */
    public static boolean isBTMusicMode(int mode) {
        return mode == com.haoke.define.ModeDef.BTMUSIC;
    }
    
    /**
     * 判断模式是否为EQ（静音在这里面），仅限onCarDataChange或者CallBack调用
     */
    public static boolean isEQMode(int mode) {
        return mode == com.haoke.define.ModeDef.EQ;
    }
    
    public static boolean isCmsStatusMode(int mode) {
        return mode == com.haoke.define.ModeDef.CMS_STATUS;
    }
    
    /**
     * 判断模式是否为收音，仅限onCarDataChange或者CallBack调用
     */
    public static boolean isRadioMode(int mode) {
        return mode == com.haoke.define.ModeDef.RADIO;
    }
    
    /**
     * 判断当前源是不是收音
     */
    public static boolean isRadioSource() {
        return Media_IF.getCurSource() == Source.RADIO;
    }
    
    /**
     * 判断我们的源是不是收音
     */
    public static boolean isRadioSource(int source) {
        return source == Source.RADIO;
    }
    
    /**
     * 判断当前源为收音
     */
    public static boolean setRadioSource() {
        return Media_IF.setCurSource(Source.RADIO);
    }
    
    /**
     * 将mcu的source、type转换为我们的source;返回值为Source.xx
     */
    public static int changeToOurSource(int source, int type) {
        int our = NULL;
        int base = (type == MediaType.AUDIO) ? AUDIO : VIDEO;
        if (source == ModeDef.RADIO) {
            our = RADIO;
        } else if (source == ModeDef.BT || source == ModeDef.BTMUSIC) {
            our = BT;
        } else if (source == ModeDef.USB1) {
            our = base + DeviceType.USB1;
        } else if (source == ModeDef.USB2) {
            our = base + DeviceType.USB2;
        } else if (source == ModeDef.USB3) {
            our = base + DeviceType.USB3;
        } else if (source == ModeDef.USB4) {
            our = base + DeviceType.USB4;
        } else if (source == ModeDef.FAVOR) {
            our = base + DeviceType.COLLECT;
        } else if (source == ModeDef.FLASH) {
            our = base + DeviceType.FLASH;
        }
        return our;
    }
    
    /**
     * 从我们的source中取出设备类型,返回 DeviceType
     */
    public static int getDeviceType(int source) {
        if (isAudioSource(source)) {
            return source - AUDIO;
        } else if (isVideoSource(source)) {
            return source - VIDEO;
        } else {
            return DeviceType.NULL;
        }
    }
    
    /**
     * 只供Media_IF调用;返回值为com.haoke.define.ModeDef.xx
     */
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
            int deviceType = getDeviceType(source);
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