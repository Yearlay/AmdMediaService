package com.haoke.constant;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import com.haoke.application.MediaApplication;
import com.haoke.bean.FileNode;
import com.haoke.bean.StorageBean;
import com.haoke.bean.UserBean;
import com.haoke.bean.UserInfoBean;
import com.haoke.constant.MediaUtil;
import com.haoke.data.AllMediaList;
import com.haoke.util.DebugLog;
import com.haoke.util.GsonUtil;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

public class MediaUtil {
    public final static int COLLECT_COUNT_MAX = 1000;
    private final static int CAPTURE_POSITION = 1 * 1000 * 1000;
    
    private final static int THUMBNAIL_MUSIC_WIDTH = 100; // 缩略图宽度
    private final static int THUMBNAIL_MUSIC_HEIGHT = 100; // 缩略图高度
    private final static int THUMBNAIL_IMAGE_AND_VIDEO_WIDTH = 380; // 缩略图宽度
    private final static int THUMBNAIL_IMAGE_AND_VIDEO_HEIGHT = 196; // 缩略图高度
    private final static int THUMBNAIL_IMAGE_DETAIL_WIDTH = 412;
    private final static int THUMBNAIL_IMAGE_DETAIL_HEIGHT = 412;
    public class ThumbnailType {
        public static final int LIST_ITEM_FOR_IMAGE_AND_VIDEO = 1;
        public static final int DETAIL_ICON_FOR_IMAGE = 2;
        public static final int LISR_ITEM_FOR_MUSIC = 3;
    }
    
    // 设备路径（可配置，非常量）
    public static String DEVICE_PATH_FLASH = "/mnt/media_rw/internal_sd/0";
    public static String DEVICE_PATH_SD_1 = "/mnt/ext_sdcard1";
    public static String DEVICE_PATH_SD_2 = "/mnt/ext_sdcard0";
    public static String DEVICE_PATH_USB_1 = "/storage/usb_storage";
    public static String DEVICE_PATH_USB_2 = "/storage/usb_storage1";
    public static String DEVICE_PATH_USB_3 = "/mnt/udisk3";
    public static String DEVICE_PATH_USB_4 = "/mnt/udisk4";
    
    public static String DEVICE_PATH_COLLECT = DEVICE_PATH_FLASH + "/collect";
    public static String LOCAL_COPY_DIR = DEVICE_PATH_FLASH + "/media";
    
    /**
     * 媒体功能ID
     */
    public static class MediaFunc {
        
        /** 扫描状态改变 value:1 */
        public static final int SCAN_STATE = 1;
        /** 单个id3扫描完成 value:2 */
        public static final int SCAN_ID3_SINGLE_OVER = 2;
        /** 部分id3扫描完成 value:3 */
        public static final int SCAN_ID3_PART_OVER = 3;
        /** 全部id3扫描完成 value:4 */
        public static final int SCAN_ID3_ALL_OVER = 4;
        /** 单个图片扫描完成 value:5 */
        public static final int SCAN_THUMBNAIL_SINGLE_OVER = 5;
        /** 全部图片扫描完成 value:6 */
        public static final int SCAN_THUMBNAIL_ALL_OVER = 6;
        /** 删除文件 value:7 */
        public static final int DELETE_FILE = 7;
        /** 外部设备改变 value:8 */
        public static final int DEVICE_CHANGED = 8;
        /** 收藏文件动作 value:9 */
        public static final int COLLECT_FILE = 9;
        /** 取消收藏文件动作 value:10 */
        public static final int UNCOLLECT_FILE = 10;

        // HKMedia
        /** 播放准备中 value:100 */
        public static final int PREPARING = 100;
        /** 播放准备就绪 value:101 */
        public static final int PREPARED = 101;
        /** 播放完成 value:102 */
        public static final int COMPLETION = 102;
        /** 播放进度完成 value:103 */
        public static final int SEEK_COMPLETION = 103;
        /** 播放出错 value:104 */
        public static final int ERROR = 104;
        /** 播放结束 value:105 */
        public static final int PLAY_OVER = 105;
        /** 播放状态改变 value:106 */
        public static final int PLAY_STATE = 106;
        /** 播放循环模式改变 value:107 */
        public static final int REPEAT_MODE = 107;
        /** 播放随机模式改变 value:108 */
        public static final int RANDOM_MODE = 108;
        
        /** 当前播放器类型 硬解或软解 value:11 */
        public static final int PLAY_TYPE = 11;
        
        //当前设备媒体有更新
        public static final int MEDIA_LIST_UPDATE = 1000;
        
        //拷贝文件
        public static final int MEDIA_COPY_FILE = 1001;
        
        //预览模式（播放10秒跳到下一首）
        public static final int MEDIA_SCAN_MODE = 1002;
    }
    
    public static class CopyState {
        /** 文件拷贝中 value:0 */
        public static final int COPYING = 0;
        /** 文件拷贝失败 value:1 */
        public static final int FAIL = 1;
        /** 文件拷贝成功 value:2 */
        public static final int SUCCESS = 2;
    }
    
    /**
     * 媒体设备分类
     */
    public class DeviceType {
        /** 无外部设备插入 value:0 */
        public static final byte NULL = 0;
        /** SD卡插槽1设备 value:1 */
        public static final byte SD1 = 1;
        /** SD卡插槽2设备 value:2 */
        public static final byte SD2 = 2;
        /** USB 插槽1设备 value:3 */
        public static final byte USB1 = 3;
        /** USB 插槽2设备 value:4 */
        public static final byte USB2 = 4;
        /** USB 插槽3设备 value:5 */
        public static final byte USB3 = 5;
        /** USB 插槽4设备 value:6 */
        public static final byte USB4 = 6;
        /** 本地存储设备 value:13 */
        public static final byte FLASH = 13;
        /** 收藏设备 value:19 */
        public static final byte COLLECT = 19;
    }
    
    /**
     * 媒体设备状态
     */
    public class MediaState {
        /** 空闲状态 value:0 */
        public static final int IDLE = 0;
        /** 准备中 value:0 */
        public static final int PREPARING = 1;
        /** 准备完成 value:0 */
        public static final int PREPARED = 2;
        /** 媒体文件出错 value:0 */
        public static final int ERROR = 3;
    }

    /**
     * 媒体播放状态
     */
    public class PlayState {
        /** 播放 value:0 */
        public static final int PLAY = 0;
        /** 暂停 value:1 */
        public static final int PAUSE = 1;
        /** 停止 value:2 */
        public static final int STOP = 2;
        /** 上一首 value:3 */
        public static final int PREV = 3;
        /** 下一首 value:4 */
        public static final int NEXT = 4;
    }

    /**
     * 循环播放模式
     */
    public class RepeatMode {
        /** 关闭循环模式 value:1 */
        public static final int OFF = 0;
        /** 单曲循环 value:1 */
        public static final int ONE = 1;
        /** 列表循环 value:1 */
        public static final int CIRCLE = 2;
        /** 列表随机 value:1 */
        public static final int RANDOM = 3;
    }

    /**
     * 随机播放模式
     */
    public class RandomMode {
        /** 打开随机播放 value:0 */
        public static final int OFF = 0;
        /** 关闭随机播放 value:1 */
        public static final int ON = 1;
    }

    /**
     * 文件删除状态
     */
    public class DeleteState {
        /** 文件删除中 value:0 */
        public static final int DELETING = 0;
        /** 文件删除失败 value:1 */
        public static final int FAIL = 1;
        /** 文件删除成功 value:2 */
        public static final int SUCCESS = 2;
    }
    
    /**
     * 其他状态类型
     */
    public class OperateState {
        /** 操作中 */
        public static final int OPERATING = 0;
        /** 操作失败 */
        public static final int FAIL = 1;
        /** 操作成功 */
        public static final int SUCCESS = 2;
    }
    
    /**
     * 当前使用的播放器类型 硬解或软解
     */
    public class PlayType {
        /** 当前使用硬解播放视频 value:0 */
        public static final int PLAY_MEDIA = 0;
        /** 当前使用软解播放视频 value:1 */
        public static final int PLAY_VITAMIO = 1;
    }
    
    public static class UpdateWidget {
        public static final int ALL = 0;
        public static final int RADIO = 1;
        public static final int AUDIO = 2;
        public static final int BTMUSIC = 3;
    }
    
    // 根据设备类型来获取路径
    public static String getDevicePath(int deviceType) {
        String path = null;
        if (deviceType == DeviceType.SD1) {
            path = DEVICE_PATH_SD_1;
        } else if (deviceType == DeviceType.SD2) {
            path = DEVICE_PATH_SD_2;
        } else if (deviceType == DeviceType.USB1) {
            path = DEVICE_PATH_USB_1;
        } else if (deviceType == DeviceType.USB2) {
            path = DEVICE_PATH_USB_2;
        } else if (deviceType == DeviceType.USB3) {
            path = DEVICE_PATH_USB_3;
        } else if (deviceType == DeviceType.USB4) {
            path = DEVICE_PATH_USB_4;
        } else if (deviceType == DeviceType.FLASH) {
            path = LOCAL_COPY_DIR;
        } else if (deviceType == DeviceType.COLLECT) {//增加音乐收藏路径
            path = DEVICE_PATH_COLLECT;
        }
        return path;
    }
    
    // 根据路径来获取设备类型
    public static int getDeviceType(String path) {
        int deviceType = DeviceType.NULL;
        if (path == null) {
            return deviceType;
        }
        if (path.startsWith(DEVICE_PATH_SD_1)) {
            return DeviceType.SD1;
        } else if (path.startsWith(DEVICE_PATH_SD_2)) {
            return DeviceType.SD2;
        } else if (path.startsWith(DEVICE_PATH_USB_2)) {
            return DeviceType.USB2;
        } else if (path.startsWith(DEVICE_PATH_USB_3)) {
            return DeviceType.USB3;
        } else if (path.startsWith(DEVICE_PATH_USB_4)) {
            return DeviceType.USB4;
        } else if (path.startsWith(LOCAL_COPY_DIR)) {
            return DeviceType.FLASH;
        } else if (path.startsWith(DEVICE_PATH_COLLECT)) {
            return DeviceType.COLLECT;
        } else if (path.startsWith(DEVICE_PATH_USB_1)) {
            return DeviceType.USB1;
        } 
        return deviceType;
    }

    public static final String[] AUDIO_EXTENSIONS = {
            "mp3", "flac", "m4r",
            "wav", "mp1", "mp2",
            "aac", "amr", "mid",
            "midi", "oga", "ra",
            "mka", "dts", "m4a",
            "ogg" };

    public static final String[] VIDEO_EXTENSIONS = {
            "mp4", "3gp", "3gpp",
            "3g2", "3gpp2", "mpeg",
            "mkv", "mov", 
            "flv", "f4v", "avi",
            "vob", "ts", "m2ts",
            "m4v", "divx", "asx"
    };

    public static final String[] IMAGE_EXTENSIONS = {
            "png", "jpg", "bmp",
            "jpeg", "gif", "ico",
            "tag" };
    
    public static HashSet<String> sVideoHash = new HashSet<String>();
    public static HashSet<String> sAudioHash = new HashSet<String>();
    public static HashSet<String> sImageHash = new HashSet<String>();
    static {
        sVideoHash.addAll(Arrays.asList(VIDEO_EXTENSIONS));
        sAudioHash.addAll(Arrays.asList(AUDIO_EXTENSIONS));
        sImageHash.addAll(Arrays.asList(IMAGE_EXTENSIONS));
    }
    
    /**
     * 媒体文件分类
     */
    public class FileType {
        /** 无媒体文件 value:0 */
        public static final byte NULL = 0;
        /** 音乐文件 value:1 */
        public static final byte AUDIO = 1;
        /** 视频文件 value:2 */
        public static final byte VIDEO = 2;
        /** 图片文件 value:3 */
        public static final byte IMAGE = 3;
        /** 文件夹 value:4 */
        public static final byte FOLDER = 4;
        /** 收藏 value:9 */
        public static final byte COLLECT = 9;
    };
    
    public static int getMediaType(String fileName) {
        int fileType = FileType.NULL;
        String postfix = (fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length())).toLowerCase();
        if (MediaUtil.sAudioHash.contains(postfix)) {
            fileType = FileType.AUDIO;
        } else if (MediaUtil.sVideoHash.contains(postfix)) {
            fileType = FileType.VIDEO;
        } else if (MediaUtil.sImageHash.contains(postfix)) {
            fileType = FileType.IMAGE;
        }
        return fileType;
    }
    
    public static class Position {
        /* 查的URI地址 中段。*/
        public static final String SD1_AUDIO_KEY = DBConfig.TABLE_AUDIO + DeviceType.SD1;
        public static final String SD1_VIDEO_KEY = DBConfig.TABLE_VIDEO + DeviceType.SD1;
        public static final String SD1_IMAGE_KEY = DBConfig.TABLE_IMAGE + DeviceType.SD1;
        public static final String SD2_AUDIO_KEY = DBConfig.TABLE_AUDIO + DeviceType.SD2;
        public static final String SD2_VIDEO_KEY = DBConfig.TABLE_VIDEO + DeviceType.SD2;
        public static final String SD2_IMAGE_KEY = DBConfig.TABLE_IMAGE + DeviceType.SD2;
        public static final String USB1_AUDIO_KEY = DBConfig.TABLE_AUDIO + DeviceType.USB1;
        public static final String USB1_VIDEO_KEY = DBConfig.TABLE_VIDEO + DeviceType.USB1;
        public static final String USB1_IMAGE_KEY = DBConfig.TABLE_IMAGE + DeviceType.USB1;
        public static final String USB2_AUDIO_KEY = DBConfig.TABLE_AUDIO + DeviceType.USB2;
        public static final String USB2_VIDEO_KEY = DBConfig.TABLE_VIDEO + DeviceType.USB2;
        public static final String USB2_IMAGE_KEY = DBConfig.TABLE_IMAGE + DeviceType.USB2;
        public static final String USB3_AUDIO_KEY = DBConfig.TABLE_AUDIO + DeviceType.USB3;
        public static final String USB3_VIDEO_KEY = DBConfig.TABLE_VIDEO + DeviceType.USB3;
        public static final String USB3_IMAGE_KEY = DBConfig.TABLE_IMAGE + DeviceType.USB3;
        public static final String USB4_AUDIO_KEY = DBConfig.TABLE_AUDIO + DeviceType.USB4;
        public static final String USB4_VIDEO_KEY = DBConfig.TABLE_VIDEO + DeviceType.USB4;
        public static final String USB4_IMAGE_KEY = DBConfig.TABLE_IMAGE + DeviceType.USB4;
        public static final String FLASH_AUDIO_KEY = DBConfig.TABLE_AUDIO + DeviceType.FLASH;
        public static final String FLASH_VIDEO_KEY = DBConfig.TABLE_VIDEO + DeviceType.FLASH;
        public static final String FLASH_IMAGE_KEY = DBConfig.TABLE_IMAGE + DeviceType.FLASH;
        public static final String COLLECT_AUDIO_KEY = DBConfig.TABLE_AUDIO + DeviceType.COLLECT;
        public static final String COLLECT_VIDEO_KEY = DBConfig.TABLE_VIDEO + DeviceType.COLLECT;
        public static final String COLLECT_IMAGE_KEY = DBConfig.TABLE_IMAGE + DeviceType.COLLECT;
    }
    
    public static final String SCANNING_ACTION = "com.jsbd.fileserve";
    public static final String SCANNING_ACTION_EXTRA_SCAN_STATE = "scanStatus";
    public static final String SCANNING_ACTION_EXTRA_DEVICE_PATH = "devicePath";
    
    public static class ScanState {
        public static final int IDLE = 0;
        public static final int SCANNING = 1;
        public static final int NO_MEDIA_STORAGE = 2;
        public static final int COMPLETED = 3;
        public static final int REMOVE_STORAGE = 4;
        public static final int SCAN_ERROR = 5;
        public static final int ID3_PARSING = 6;
        public static final int ID3_PARSE_COMPLETED = 7;
        public static final int COMPLETED_ALL = 8;
        public static final int SCAN_THREAD_OVER = 9;
    }
    
    public static class ScanType {
        public static final String SCAN_TYPE_KEY = "scan_type";
        public static final String SCAN_FILE_PATH = "scan_file_path";
        public static final int SCAN_STORAGE = 1;
        public static final int REMOVE_STORAGE = 2;
    }

    public static class ScanTaskType {
        public static final int MOUNTED = 0;
        public static final int UNMOUNTED = 1;
    }
    
    public static class ScanTask {
        public int mTaskType;
        public String mFilePath;
        public int mDeviceType;
        public boolean mIsInterrupted;
        public ScanTask(int taskType, String filePath) {
            mTaskType = taskType;
            mFilePath = filePath;
            mDeviceType = getDeviceType(filePath);
            mIsInterrupted = false;
        }
    }
    
    public static Bitmap BitmapScale(Bitmap bmp, int width, int height) {
        if (bmp == null)
            return null;
        if (width == 0 || height == 0)
            return bmp;

        float scaleWidth = (float) width / bmp.getWidth();
        float scaleHeight = (float) height / bmp.getHeight();
        Matrix matrix = new Matrix();
        float scale = scaleWidth > scaleHeight ? scaleWidth : scaleHeight;
        matrix.postScale(scale, scale);//按同样比例缩放，保持原比例，防止图片变形(qinjiazhi - 20170707)
        Bitmap scaleBmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(),
                bmp.getHeight(), matrix, true);
        int x = (scaleBmp.getWidth() - width)/2;
        int y = (scaleBmp.getHeight() - height)/2;
        scaleBmp = Bitmap.createBitmap(scaleBmp, x, y, width, height);//裁剪图片
        bmp.recycle();
        bmp = null;
        return scaleBmp;
    }
    
    public static byte[] BitmapToBytes(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }
    
    public static Bitmap BytesToBitmap(byte[] byteArray) {
        if (byteArray != null && byteArray.length != 0)
            return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        return null;
    }
    
    /**
     * 反射调用系统的方法StorageManager#getVolumeState来判断磁盘是否挂载。
     * @param context 上下文
     * @param storagePath 磁盘路径
     * @return
     */
    public static boolean checkMounted(Context context, String storagePath) {
        if (storagePath == null) {
            return false;
        }
        if (storagePath.contains(DEVICE_PATH_FLASH)) {
            return true;
        }
        StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        try {
            Method getVolumeState = storageManager.getClass().getMethod("getVolumeState", String.class);
            String state = (String) getVolumeState.invoke(storageManager, storagePath);
            return Environment.MEDIA_MOUNTED.equals(state);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * 检查挂载的所有的设备是否扫描完成。
     * @param context
     * @return true表示全部扫描完成，false表示有Mount的设备没有扫描。
     */
    public static boolean checkAllStorageScanOver(Context context) {
        boolean retFlag = true;
        for (int deviceType : DBConfig.sScan3zaDefaultList) {
            if (MediaUtil.checkMounted(context, MediaUtil.getDevicePath(deviceType))) { // 系统检查是否Mounted上了。
                if (AllMediaList.instance(context).getStoragBean(deviceType).isScanIdle()) { // AllMediaList检查是否已经扫描。
                    retFlag = false;
                    break;
                }
            }
        }
        return retFlag;
    }
    
    public static boolean pasteFileByte(Thread thread, File srcfile, File tarFile, String checkDir) {
        boolean ret = true;
        checkCollectDir(checkDir);
        // 是文件,读取文件字节流,同时记录进度
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(srcfile);// 读取源文件
            outputStream = new FileOutputStream(tarFile);// 要写入的目标文件
            System.gc();
            byte[] buffer = new byte[(int) Math.pow(2, 20)];// 每次最大读取的长度，字节，2的10次方=1MB。
            int length = -1;
            while ((length = inputStream.read(buffer)) != -1 && !thread.isInterrupted()) {
                // 累计每次读取的大小
                outputStream.write(buffer, 0, length);
            }
        } catch (Exception e) {
            try {
                if (outputStream != null) outputStream.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            tarFile.delete();
            e.printStackTrace();
            ret = false;
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (thread.isInterrupted()) {
            try {
                if (outputStream != null) outputStream.close();
                tarFile.delete();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            ret = false;
        }
        return ret;
    }
    
    private static void checkCollectDir(String checkDir) {
        File file = new File(checkDir);
        if (file != null && !file.exists()) {
            if (!file.mkdirs()) {
                Log.w("Yearlay", "mkdir collect path failed");
            }
        }
    }
    
    // 转化为时间格式（分秒）
    public static String TimeFormat(int num) {
        String sTime = "";
        int minute = (int) (num / 60);
        int second = (int) (num % 60);
        String sMinute = ConvertToDoubleNum(minute);
        String sSecond = ConvertToDoubleNum(second);
        sTime = sMinute + ":" + sSecond;
        return sTime;
    }
    
    // 转化为时间格式（时分秒）
    public static String TimeFormat_HMS(int num) {
        String sTime = "";
        int hour = (int) (num / 3600);
        int minute = (int) (num / 60) % 60;
        int second = (int) (num % 60);
        String sHour = ConvertToDoubleNum(hour);
        String sMinute = ConvertToDoubleNum(minute);
        String sSecond = ConvertToDoubleNum(second);
        sTime = sHour + ":" + sMinute + ":" + sSecond;
        return sTime;
    }
    
    // 将数字转化为两位字符串
    public static String ConvertToDoubleNum(int num) {
        return (num < 10) ? "0" + num : num + "";
    }
    
    public static boolean checkAvailableSize(ArrayList<FileNode> dataList) {
        long totalSize = 0;
        for (FileNode fileNode : dataList) {
            if (fileNode.isSelected()) {
                totalSize += fileNode.getFile().length();
            }
        }
        StatFs sf = new StatFs("/mnt/media_rw/internal_sd"); 
        long blockSize = sf.getBlockSize(); 
        long blockCount = sf.getBlockCount(); 
        long availCount = sf.getAvailableBlocks();
        return (availCount * blockSize - totalSize)  > 5368709120L;
    }
    
    private static String sUserName = null;
    public static boolean isLogin() {
        Context context = MediaApplication.getInstance();
        String infoStr = Settings.System.getString(context.getContentResolver(),"personal_user_info");
        if (TextUtils.isEmpty(infoStr)) {
            return true;
        }
        return false;
    }
    
    public static String getUserName() {
        if (sUserName == null) {
            Context context = MediaApplication.getInstance();
            String infoStr = Settings.System.getString(context.getContentResolver(),"personal_user_info");
            if (infoStr != null) {
                UserInfoBean userInfoBean = (UserInfoBean) GsonUtil.instance().getObjectFromJson(infoStr, UserInfoBean.class);
                if (userInfoBean != null) {
                    sUserName = userInfoBean.getCurrentUsername();
                }
            }
        }
        return sUserName == null ? "default_user_name" : sUserName;
    }
    
    public static void updateUserName() {
        sUserName = null;
    }
    
    public static ArrayList<UserBean> getUserList(Context context) {
    	ArrayList<UserBean> userList = new ArrayList<UserBean>();
        String infoStr = Settings.System.getString(context.getContentResolver(),"personal_user_info");
        if (infoStr != null) {
            UserInfoBean userInfoBean = (UserInfoBean) GsonUtil.instance().getObjectFromJson(infoStr, UserInfoBean.class);
            if (userInfoBean != null) {
                userList = userInfoBean.getUserList();
            }
        }
        return userList;
    }
    
    public static boolean sSdcardMountedEndToID3Over = false;
    public static boolean sUSB1MountedEndToID3Over = false;
    public static boolean sUSB2MountedEndToID3Over = false;
    public static void resetAllMountedEndToID3Over() {
        sSdcardMountedEndToID3Over = false;
        sUSB1MountedEndToID3Over = false;
        sUSB2MountedEndToID3Over = false;
    }
}
