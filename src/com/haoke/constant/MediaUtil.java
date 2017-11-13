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

import com.haoke.bean.FileNode;
import com.haoke.bean.UserBean;
import com.haoke.bean.UserInfoBean;
import com.haoke.define.MediaDef;
import com.haoke.util.GsonUtil;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.provider.Settings;
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
     * 媒体功能ID【扩展】
     * {@link MediaDef.MediaFunc}
     */
    public static class MediaFuncEx {
        
        //当前设备媒体有更新
        public static final int MEDIA_LIST_UPDATE = 1000;
        
        //拷贝文件
        public static final int MEDIA_COPY_FILE = 1001; 
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
        /**  相机设备 value:10 */
        public static final byte CAMERA = 10;
        /** 亿连设备 value:11 */
        public static final byte ELINK = 11;
        /** IPOD设备 value:12 */
        public static final byte IPOD = 12;
        /** 本地存储设备 value:13 */
        public static final byte FLASH = 13;
        /** 收藏设备 value:13 */
        public static final byte COLLECT = 19;
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
    
    public static String getUserName(Context context) {
        String username = "";
        String infoStr = Settings.System.getString(context.getContentResolver(),"personal_user_info");
        if (infoStr != null) {
            UserInfoBean userInfoBean = (UserInfoBean) GsonUtil.instance().getObjectFromJson(infoStr, UserInfoBean.class);
            if (userInfoBean != null) {
                username = userInfoBean.getCurrentUsername();
            }
        }
        return username;
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
}
