package com.haoke.constant;

import java.util.ArrayList;

import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.constant.MediaUtil.FileType;

import android.annotation.SuppressLint;

@SuppressLint("UseValueOf")
public class DBConfig {
    /**
     * 数据库的名称："media_db"
     */
    public final static String DATABASE_NAME = "media_db";
    
    /**
     * 数据库的版本号：当前是2。
     */
    public final static int DATABASE_VERSION = 5;
    
    public final static String TABLE_AUDIO = "table_audio";
    public final static String TABLE_VIDEO = "table_video";
    public final static String TABLE_IMAGE = "table_image";
    
    public final static String TABLE_SAVE_MUSIC = "table_collect_music";
    
    public interface MediaColumns {
        public final static String FIELD_ID = "id";
        public final static String FIELD_FILE_PATH = "file_path";
        public final static String FIELD_FILE_NAME = "file_name";
        public final static String FIELD_FILE_NAME_PY = "file_name_py"; // 文件名拼音
        public final static String FIELD_FILE_LENGTH = "file_length";
        
        /* 下面的私信是ID3信息 */
        public final static String FIELD_PARSE_ID3 = "parse_id3";
        public final static String FIELD_TITLE = "title";
        public final static String FIELD_ARTIST = "artist";
        public final static String FIELD_ALBUM = "album";
        public final static String FIELD_COMPOSER = "composer";
        public final static String FIELD_GENRE = "genre";
        public final static String FIELD_DURATION = "duration";
        public final static String FIELD_TITLE_PY = "title_py";
        public final static String FIELD_ARTIST_PY = "artist_py";
        public final static String FIELD_ALBUM_PY = "album_py";
        public final static String FIELD_ALBUM_PIC = "album_pic";
        
        /* 下面的信息是收藏的信息 */
        public final static String FIELD_COLLECT = "collect";// 是否收藏，0：否，1收藏
        public final static String FIELD_FILE_COLLECT_PATH = "collect_path"; // 文件收藏的全路径
        public final static String FIELD_FILE_THUMBNAIL_PATH = "thumbnail_path"; // 缓存的路径。
        public final static String FIELD_USERNAME = "username";
    }
    
    public static ArrayList<Integer> sDeviceDefaultList = new ArrayList<Integer>();
    static {
        sDeviceDefaultList.add(new Integer(DeviceType.SD1));
        sDeviceDefaultList.add(new Integer(DeviceType.SD2));
        sDeviceDefaultList.add(new Integer(DeviceType.USB1));
        sDeviceDefaultList.add(new Integer(DeviceType.USB2));
        sDeviceDefaultList.add(new Integer(DeviceType.USB3));
        sDeviceDefaultList.add(new Integer(DeviceType.USB4));
        sDeviceDefaultList.add(new Integer(DeviceType.FLASH));
    }
    
    public static ArrayList<Integer> sScan3zaDefaultList = new ArrayList<Integer>();
    static {
        sScan3zaDefaultList.add(new Integer(DeviceType.USB1));
        sScan3zaDefaultList.add(new Integer(DeviceType.USB2));
        sScan3zaDefaultList.add(new Integer(DeviceType.FLASH));
    }
    
    public static ArrayList<Integer> sAudioDefaultList = new ArrayList<Integer>();
    static {
        sAudioDefaultList.add(new Integer(DeviceType.FLASH));
        sAudioDefaultList.add(new Integer(DeviceType.USB1));
        sAudioDefaultList.add(new Integer(DeviceType.USB2));
    }
    
    public static ArrayList<Integer> sImageDefaultList = new ArrayList<Integer>();
    static {
        sImageDefaultList.add(new Integer(DeviceType.FLASH));
        sImageDefaultList.add(new Integer(DeviceType.USB1));
        sImageDefaultList.add(new Integer(DeviceType.USB2));
    }
    
    public static ArrayList<Integer> sVideoDefaultList = new ArrayList<Integer>();
    static {
        sVideoDefaultList.add(new Integer(DeviceType.FLASH));
        sVideoDefaultList.add(new Integer(DeviceType.USB1));
        sVideoDefaultList.add(new Integer(DeviceType.USB2));
    }
    
    public static final String URI_HEAD = "content://";
    public static final String MEDIA_DB_AUTOHORITY = "com.haoke.media.contentprovider";
    
    public interface TableName {
        /* 查的URI地址 中段。*/
        public static final String SD1_AUDIO_TABLE_NAME = TABLE_AUDIO + DeviceType.SD1;
        public static final String SD1_VIDEO_TABLE_NAME = TABLE_VIDEO + DeviceType.SD1;
        public static final String SD1_IMAGE_TABLE_NAME = TABLE_IMAGE + DeviceType.SD1;
        public static final String SD2_AUDIO_TABLE_NAME = TABLE_AUDIO + DeviceType.SD2;
        public static final String SD2_VIDEO_TABLE_NAME = TABLE_VIDEO + DeviceType.SD2;
        public static final String SD2_IMAGE_TABLE_NAME = TABLE_IMAGE + DeviceType.SD2;
        public static final String USB1_AUDIO_TABLE_NAME = TABLE_AUDIO + DeviceType.USB1;
        public static final String USB1_VIDEO_TABLE_NAME = TABLE_VIDEO + DeviceType.USB1;
        public static final String USB1_IMAGE_TABLE_NAME = TABLE_IMAGE + DeviceType.USB1;
        public static final String USB2_AUDIO_TABLE_NAME = TABLE_AUDIO + DeviceType.USB2;
        public static final String USB2_VIDEO_TABLE_NAME = TABLE_VIDEO + DeviceType.USB2;
        public static final String USB2_IMAGE_TABLE_NAME = TABLE_IMAGE + DeviceType.USB2;
        public static final String USB3_AUDIO_TABLE_NAME = TABLE_AUDIO + DeviceType.USB3;
        public static final String USB3_VIDEO_TABLE_NAME = TABLE_VIDEO + DeviceType.USB3;
        public static final String USB3_IMAGE_TABLE_NAME = TABLE_IMAGE + DeviceType.USB3;
        public static final String USB4_AUDIO_TABLE_NAME = TABLE_AUDIO + DeviceType.USB4;
        public static final String USB4_VIDEO_TABLE_NAME = TABLE_VIDEO + DeviceType.USB4;
        public static final String USB4_IMAGE_TABLE_NAME = TABLE_IMAGE + DeviceType.USB4;
        public static final String FLASH_AUDIO_TABLE_NAME = TABLE_AUDIO + DeviceType.FLASH;
        public static final String FLASH_VIDEO_TABLE_NAME = TABLE_VIDEO + DeviceType.FLASH;
        public static final String FLASH_IMAGE_TABLE_NAME = TABLE_IMAGE + DeviceType.FLASH;
        
        public static final String COLLECT_AUDIO_TABLE_NAME = TABLE_AUDIO + DeviceType.COLLECT;
        public static final String COLLECT_VIDEO_TABLE_NAME = TABLE_VIDEO + DeviceType.COLLECT;
        public static final String COLLECT_IMAGE_TABLE_NAME = TABLE_IMAGE + DeviceType.COLLECT;
    }
    
    public interface UriAddress {
        /* 查的URI地址。*/
        public static final String URI_SD1_AUDIO_ADDR = URI_HEAD + MEDIA_DB_AUTOHORITY + "/" + TABLE_AUDIO + DeviceType.SD1;
        public static final String URI_SD1_VIDEO_ADDR = URI_HEAD + MEDIA_DB_AUTOHORITY + "/" + TABLE_VIDEO + DeviceType.SD1;
        public static final String URI_SD1_IMAGE_ADDR = URI_HEAD + MEDIA_DB_AUTOHORITY + "/" + TABLE_IMAGE + DeviceType.SD1;
        public static final String URI_SD2_AUDIO_ADDR = URI_HEAD + MEDIA_DB_AUTOHORITY + "/" + TABLE_AUDIO + DeviceType.SD2;
        public static final String URI_SD2_VIDEO_ADDR = URI_HEAD + MEDIA_DB_AUTOHORITY + "/" + TABLE_VIDEO + DeviceType.SD2;
        public static final String URI_SD2_IMAGE_ADDR = URI_HEAD + MEDIA_DB_AUTOHORITY + "/" + TABLE_IMAGE + DeviceType.SD2;
        public static final String URI_USB1_AUDIO_ADDR = URI_HEAD + MEDIA_DB_AUTOHORITY + "/" + TABLE_AUDIO + DeviceType.USB1;
        public static final String URI_USB1_VIDEO_ADDR = URI_HEAD + MEDIA_DB_AUTOHORITY + "/" + TABLE_VIDEO + DeviceType.USB1;
        public static final String URI_USB1_IMAGE_ADDR = URI_HEAD + MEDIA_DB_AUTOHORITY + "/" + TABLE_IMAGE + DeviceType.USB1;
        public static final String URI_USB2_AUDIO_ADDR = URI_HEAD + MEDIA_DB_AUTOHORITY + "/" + TABLE_AUDIO + DeviceType.USB2;
        public static final String URI_USB2_VIDEO_ADDR = URI_HEAD + MEDIA_DB_AUTOHORITY + "/" + TABLE_VIDEO + DeviceType.USB2;
        public static final String URI_USB2_IMAGE_ADDR = URI_HEAD + MEDIA_DB_AUTOHORITY + "/" + TABLE_IMAGE + DeviceType.USB2;
        public static final String URI_USB3_AUDIO_ADDR = URI_HEAD + MEDIA_DB_AUTOHORITY + "/" + TABLE_AUDIO + DeviceType.USB3;
        public static final String URI_USB3_VIDEO_ADDR = URI_HEAD + MEDIA_DB_AUTOHORITY + "/" + TABLE_VIDEO + DeviceType.USB3;
        public static final String URI_USB3_IMAGE_ADDR = URI_HEAD + MEDIA_DB_AUTOHORITY + "/" + TABLE_IMAGE + DeviceType.USB3;
        public static final String URI_USB4_AUDIO_ADDR = URI_HEAD + MEDIA_DB_AUTOHORITY + "/" + TABLE_AUDIO + DeviceType.USB4;
        public static final String URI_USB4_VIDEO_ADDR = URI_HEAD + MEDIA_DB_AUTOHORITY + "/" + TABLE_VIDEO + DeviceType.USB4;
        public static final String URI_USB4_IMAGE_ADDR = URI_HEAD + MEDIA_DB_AUTOHORITY + "/" + TABLE_IMAGE + DeviceType.USB4;
        public static final String URI_FLASH_AUDIO_ADDR = URI_HEAD + MEDIA_DB_AUTOHORITY + "/" + TABLE_AUDIO + DeviceType.FLASH;
        public static final String URI_FLASH_VIDEO_ADDR = URI_HEAD + MEDIA_DB_AUTOHORITY + "/" + TABLE_VIDEO + DeviceType.FLASH;
        public static final String URI_FLASH_IMAGE_ADDR = URI_HEAD + MEDIA_DB_AUTOHORITY + "/" + TABLE_IMAGE + DeviceType.FLASH;
        public static final String URI_COLLECT_AUDIO_ADDR = URI_HEAD + MEDIA_DB_AUTOHORITY + "/" + TABLE_AUDIO + DeviceType.COLLECT;
        public static final String URI_COLLECT_VIDEO_ADDR = URI_HEAD + MEDIA_DB_AUTOHORITY + "/" + TABLE_VIDEO + DeviceType.COLLECT;
        public static final String URI_COLLECT_IMAGE_ADDR = URI_HEAD + MEDIA_DB_AUTOHORITY + "/" + TABLE_IMAGE + DeviceType.COLLECT;
    }
    
    public static String getUriAddress(String tableName) {
        String uri = null;
        if (tableName.equals(TableName.SD1_AUDIO_TABLE_NAME)) {
            uri = UriAddress.URI_SD1_AUDIO_ADDR;
        } else if (tableName.equals(TableName.SD1_VIDEO_TABLE_NAME)) {
            uri = UriAddress.URI_SD1_VIDEO_ADDR;
        } else if (tableName.equals(TableName.SD1_IMAGE_TABLE_NAME)) {
            uri = UriAddress.URI_SD1_IMAGE_ADDR;
        } else if (tableName.equals(TableName.SD2_AUDIO_TABLE_NAME)) {
            uri = UriAddress.URI_SD2_AUDIO_ADDR;
        } else if (tableName.equals(TableName.SD2_VIDEO_TABLE_NAME)) {
            uri = UriAddress.URI_SD2_VIDEO_ADDR;
        } else if (tableName.equals(TableName.SD2_IMAGE_TABLE_NAME)) {
            uri = UriAddress.URI_SD2_IMAGE_ADDR;
        } else if (tableName.equals(TableName.USB1_AUDIO_TABLE_NAME)) {
            uri = UriAddress.URI_USB1_AUDIO_ADDR;
        } else if (tableName.equals(TableName.USB1_VIDEO_TABLE_NAME)) {
            uri = UriAddress.URI_USB1_VIDEO_ADDR;
        } else if (tableName.equals(TableName.USB1_IMAGE_TABLE_NAME)) {
            uri = UriAddress.URI_USB1_IMAGE_ADDR;
        } else if (tableName.equals(TableName.USB2_AUDIO_TABLE_NAME)) {
            uri = UriAddress.URI_USB2_AUDIO_ADDR;
        } else if (tableName.equals(TableName.USB2_VIDEO_TABLE_NAME)) {
            uri = UriAddress.URI_USB2_VIDEO_ADDR;
        } else if (tableName.equals(TableName.USB2_IMAGE_TABLE_NAME)) {
            uri = UriAddress.URI_USB2_IMAGE_ADDR;
        } else if (tableName.equals(TableName.USB3_AUDIO_TABLE_NAME)) {
            uri = UriAddress.URI_USB3_AUDIO_ADDR;
        } else if (tableName.equals(TableName.USB3_VIDEO_TABLE_NAME)) {
            uri = UriAddress.URI_USB3_VIDEO_ADDR;
        } else if (tableName.equals(TableName.USB3_IMAGE_TABLE_NAME)) {
            uri = UriAddress.URI_USB3_IMAGE_ADDR;
        } else if (tableName.equals(TableName.USB4_AUDIO_TABLE_NAME)) {
            uri = UriAddress.URI_USB4_AUDIO_ADDR;
        } else if (tableName.equals(TableName.USB4_VIDEO_TABLE_NAME)) {
            uri = UriAddress.URI_USB4_VIDEO_ADDR;
        } else if (tableName.equals(TableName.USB4_IMAGE_TABLE_NAME)) {
            uri = UriAddress.URI_USB4_IMAGE_ADDR;
        } else if (tableName.equals(TableName.FLASH_AUDIO_TABLE_NAME)) {
            uri = UriAddress.URI_FLASH_AUDIO_ADDR;
        } else if (tableName.equals(TableName.FLASH_VIDEO_TABLE_NAME)) {
            uri = UriAddress.URI_FLASH_VIDEO_ADDR;
        } else if (tableName.equals(TableName.FLASH_IMAGE_TABLE_NAME)) {
            uri = UriAddress.URI_FLASH_IMAGE_ADDR;
        } else if (tableName.equals(TableName.COLLECT_AUDIO_TABLE_NAME)) {
            uri = UriAddress.URI_COLLECT_AUDIO_ADDR;
        } else if (tableName.equals(TableName.COLLECT_VIDEO_TABLE_NAME)) {
            uri = UriAddress.URI_COLLECT_VIDEO_ADDR;
        } else if (tableName.equals(TableName.COLLECT_IMAGE_TABLE_NAME)) {
            uri = UriAddress.URI_COLLECT_IMAGE_ADDR;
        }
        
        return uri;
    }
    
    public interface UriType {
        /* 根据这个值来获得具体的 数据中的表 */
        public static final int SD1_AUDIO = 1;
        public static final int SD1_VIDEO = 2;
        public static final int SD1_IMAGE = 3;
        public static final int SD2_AUDIO = 11;
        public static final int SD2_VIDEO = 12;
        public static final int SD2_IMAGE = 13;
        public static final int USB1_AUDIO = 21;
        public static final int USB1_VIDEO = 22;
        public static final int USB1_IMAGE = 23;
        public static final int USB2_AUDIO = 31;
        public static final int USB2_VIDEO = 32;
        public static final int USB2_IMAGE = 33;
        public static final int USB3_AUDIO = 41;
        public static final int USB3_VIDEO = 42;
        public static final int USB3_IMAGE = 43;
        public static final int USB4_AUDIO = 51;
        public static final int USB4_VIDEO = 52;
        public static final int USB4_IMAGE = 53;
        public static final int FLASH_AUDIO = 131;
        public static final int FLASH_VIDEO = 132;
        public static final int FLASH_IMAGE = 133;
        public static final int COLLECT_AUDIO = 141;
        public static final int COLLECT_VIDEO = 142;
        public static final int COLLECT_IMAGE = 143;
    }
    
    public static int getFileTypeByUriType(int uriType) {
        int fileType = FileType.NULL;
        switch (uriType) {
        case DBConfig.UriType.SD1_AUDIO:
        case DBConfig.UriType.SD2_AUDIO:
        case DBConfig.UriType.USB1_AUDIO:
        case DBConfig.UriType.USB2_AUDIO:
        case DBConfig.UriType.USB3_AUDIO:
        case DBConfig.UriType.USB4_AUDIO:
        case DBConfig.UriType.FLASH_AUDIO:
        case DBConfig.UriType.COLLECT_AUDIO:
            fileType = FileType.AUDIO;
            break;
        case DBConfig.UriType.SD1_VIDEO:
        case DBConfig.UriType.SD2_VIDEO:
        case DBConfig.UriType.USB1_VIDEO:
        case DBConfig.UriType.USB2_VIDEO:
        case DBConfig.UriType.USB3_VIDEO:
        case DBConfig.UriType.USB4_VIDEO:
        case DBConfig.UriType.FLASH_VIDEO:
        case DBConfig.UriType.COLLECT_VIDEO:
            fileType = FileType.VIDEO;
            break;
        case DBConfig.UriType.SD1_IMAGE:
        case DBConfig.UriType.SD2_IMAGE:
        case DBConfig.UriType.USB1_IMAGE:
        case DBConfig.UriType.USB2_IMAGE:
        case DBConfig.UriType.USB3_IMAGE:
        case DBConfig.UriType.USB4_IMAGE:
        case DBConfig.UriType.FLASH_IMAGE:
        case DBConfig.UriType.COLLECT_IMAGE:
            fileType = FileType.IMAGE;
            break;
        }
        return fileType;
    }
    
    public static int getDeviceTypeByUriType(int uriType) {
        int deviceType = DeviceType.NULL;
        switch (uriType) {
        case DBConfig.UriType.SD1_AUDIO:
        case DBConfig.UriType.SD1_VIDEO:
        case DBConfig.UriType.SD1_IMAGE:
            deviceType = DeviceType.SD1;
            break;
        case DBConfig.UriType.SD2_AUDIO:
        case DBConfig.UriType.SD2_VIDEO:
        case DBConfig.UriType.SD2_IMAGE:
            deviceType = DeviceType.SD2;
            break;
        case DBConfig.UriType.USB1_AUDIO:
        case DBConfig.UriType.USB1_VIDEO:
        case DBConfig.UriType.USB1_IMAGE:
            deviceType = DeviceType.USB1;
            break;
        case DBConfig.UriType.USB2_AUDIO:
        case DBConfig.UriType.USB2_VIDEO:
        case DBConfig.UriType.USB2_IMAGE:
            deviceType = DeviceType.USB2;
            break;
        case DBConfig.UriType.USB3_AUDIO:
        case DBConfig.UriType.USB3_VIDEO:
        case DBConfig.UriType.USB3_IMAGE:
            deviceType = DeviceType.USB3;
            break;
        case DBConfig.UriType.USB4_AUDIO:
        case DBConfig.UriType.USB4_VIDEO:
        case DBConfig.UriType.USB4_IMAGE:
            deviceType = DeviceType.USB4;
            break;
        case DBConfig.UriType.FLASH_AUDIO:
        case DBConfig.UriType.FLASH_VIDEO:
        case DBConfig.UriType.FLASH_IMAGE:
            deviceType = DeviceType.FLASH;
            break;
        case DBConfig.UriType.COLLECT_AUDIO:
        case DBConfig.UriType.COLLECT_VIDEO:
        case DBConfig.UriType.COLLECT_IMAGE:
            deviceType = DeviceType.COLLECT;
            break;
        }
        return deviceType;
    }
    
    public static String getTableNameByUriType(int uriType) {
        String tableName = null;
        switch (uriType) {
        case DBConfig.UriType.SD1_AUDIO:
            tableName = DBConfig.TABLE_AUDIO + DeviceType.SD1;
            break;
        case DBConfig.UriType.SD1_VIDEO:
            tableName = DBConfig.TABLE_VIDEO + DeviceType.SD1;
            break;
        case DBConfig.UriType.SD1_IMAGE:
            tableName = DBConfig.TABLE_IMAGE + DeviceType.SD1;
            break;
        case DBConfig.UriType.SD2_AUDIO:
            tableName = DBConfig.TABLE_AUDIO + DeviceType.SD2;
            break;
        case DBConfig.UriType.SD2_VIDEO:
            tableName = DBConfig.TABLE_VIDEO + DeviceType.SD2;
            break;
        case DBConfig.UriType.SD2_IMAGE:
            tableName = DBConfig.TABLE_IMAGE + DeviceType.SD2;
            break;
        case DBConfig.UriType.USB1_AUDIO:
            tableName = DBConfig.TABLE_AUDIO + DeviceType.USB1;
            break;
        case DBConfig.UriType.USB1_VIDEO:
            tableName = DBConfig.TABLE_VIDEO + DeviceType.USB1;
            break;
        case DBConfig.UriType.USB1_IMAGE:
            tableName = DBConfig.TABLE_IMAGE + DeviceType.USB1;
            break;
        case DBConfig.UriType.USB2_AUDIO:
            tableName = DBConfig.TABLE_AUDIO + DeviceType.USB2;
            break;
        case DBConfig.UriType.USB2_VIDEO:
            tableName = DBConfig.TABLE_VIDEO + DeviceType.USB2;
            break;
        case DBConfig.UriType.USB2_IMAGE:
            tableName = DBConfig.TABLE_IMAGE + DeviceType.USB2;
            break;
        case DBConfig.UriType.USB3_AUDIO:
            tableName = DBConfig.TABLE_AUDIO + DeviceType.USB3;
            break;
        case DBConfig.UriType.USB3_VIDEO:
            tableName = DBConfig.TABLE_VIDEO + DeviceType.USB3;
            break;
        case DBConfig.UriType.USB3_IMAGE:
            tableName = DBConfig.TABLE_IMAGE + DeviceType.USB3;
            break;
        case DBConfig.UriType.USB4_AUDIO:
            tableName = DBConfig.TABLE_AUDIO + DeviceType.USB4;
            break;
        case DBConfig.UriType.USB4_VIDEO:
            tableName = DBConfig.TABLE_VIDEO + DeviceType.USB4;
            break;
        case DBConfig.UriType.USB4_IMAGE:
            tableName = DBConfig.TABLE_IMAGE + DeviceType.USB4;
            break;
        case DBConfig.UriType.FLASH_AUDIO:
            tableName = DBConfig.TABLE_AUDIO + DeviceType.FLASH;
            break;
        case DBConfig.UriType.FLASH_VIDEO:
            tableName = DBConfig.TABLE_VIDEO + DeviceType.FLASH;
            break;
        case DBConfig.UriType.FLASH_IMAGE:
            tableName = DBConfig.TABLE_IMAGE + DeviceType.FLASH;
            break;
        case DBConfig.UriType.COLLECT_AUDIO:
            tableName = DBConfig.TABLE_AUDIO + DeviceType.COLLECT;
            break;
        case DBConfig.UriType.COLLECT_VIDEO:
            tableName = DBConfig.TABLE_VIDEO + DeviceType.COLLECT;
            break;
        case DBConfig.UriType.COLLECT_IMAGE:
            tableName = DBConfig.TABLE_IMAGE + DeviceType.COLLECT;
            break;
        }
        return tableName;
    }
    
    public static String getTableName(int deviceType, int fileType) {
        String table = null;
        if (fileType == FileType.AUDIO) 
            table = DBConfig.TABLE_AUDIO + deviceType;
        else if (fileType == FileType.VIDEO) 
            table = DBConfig.TABLE_VIDEO + deviceType;
        else if (fileType == FileType.IMAGE) 
            table = DBConfig.TABLE_IMAGE + deviceType;
        return table;
    }
    
    public static boolean isMediaType(int fileType) {
        return fileType == FileType.AUDIO || fileType == FileType.VIDEO || fileType == FileType.IMAGE;
    }
}
