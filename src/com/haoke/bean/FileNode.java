package com.haoke.bean;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.file.server.scan.ScanJni;
import com.haoke.application.MediaApplication;
import com.haoke.constant.DBConfig;
import com.haoke.constant.MediaUtil;
import com.haoke.data.AllMediaList;
import com.haoke.define.MediaDef.DeviceType;
import com.haoke.define.MediaDef.FileType;
import com.haoke.scanner.MediaDbHelper;
import com.haoke.util.DebugLog;
import com.haoke.util.PingYingTool;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.text.TextUtils;

public class FileNode {
    private int id; // mIndex做为每个文件的唯一id
    private int deviceType = DeviceType.NULL; // 所属设备
    private int fileType = FileType.NULL; // 文件类型
    private String filePath; // 文件路径
    private String fileName; // 文件名称
    private String fileNamePY;
    private long fileSize; // 文件大小
    private String lastDate; // 文件日期
    
    private int parseId3;
    private String title; // 歌曲名
    private String artist; // 艺术家
    private String album; // 专辑名
    private String composer; // 作曲家
    private String genre; // 风格、流派
    private int duration; // 歌曲时间
    private String titlePY; // 歌曲名(拼音)
    private String artistPY; // 艺术家(拼音)
    private String albumPY; // 专辑名(拼音)
    private byte[] albumPicArray = null; // 专辑图片（临时存储，避免内存太大）
    
    private int collect;
    private String collectPath;
    private int fromDeviceType;
    private boolean updateDBByID;
    private boolean isFromCollectTable;

    public FileNode(String path) {
        // 需要确定fileType 和 deviceType
        File file = new File(path);
        filePath = file.getAbsolutePath();
        fileName = file.getName();
        fileNamePY = ScanJni.getPY(fileName);
        deviceType = MediaUtil.getDeviceType(path);
        fileType = MediaUtil.getMediaType(path);
        parseId3 = 0;
    }

    public FileNode(int index, int fileType, int deviceType) {
        id = index;
        this.fileType = fileType;
        this.deviceType = deviceType;
        parseId3 = 0;
    }
    
    public FileNode(FileNode node) {
        // 数据库中的数据。
        id = node.id;
        filePath = node.filePath;
        fileName = node.fileName;
        fileNamePY = node.fileNamePY;
        fileSize = node.fileSize;
        
        parseId3 = node.parseId3;
        title = node.title;
        artist = node.artist;
        album = node.album;
        composer = node.composer;
        genre = node.genre;
        duration = node.duration;
        titlePY = node.titlePY;
        artistPY = node.artistPY;
        albumPY = node.albumPY;
        albumPicArray = node.albumPicArray;

        collect = node.collect;
        collectPath = node.collectPath;
        thumbnailPath = node.thumbnailPath;

        // 其他的数据
        deviceType = node.deviceType;
        fileType = node.fileType;
    }
    
    public FileNode(String filePath, String fileName, String fileNamePY, int fileType) {
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileNamePY = fileNamePY;
        this.deviceType = MediaUtil.getDeviceType(filePath);
        this.fileType = fileType;
        if (fileType == FileType.IMAGE) {
            this.title = "";
            this.parseId3 = 0;
        }
        this.title = (fileType == FileType.IMAGE) ? fileName : "";
        this.parseId3 = (fileType == FileType.IMAGE) ? 1 : 0;
    }
    
    public FileNode(Cursor cursor) {
        id = cursor.getInt(cursor.getColumnIndex(DBConfig.MediaColumns.FIELD_ID));
        filePath = cursor.getString(cursor.getColumnIndex(DBConfig.MediaColumns.FIELD_FILE_PATH));
        fileName = cursor.getString(cursor.getColumnIndex(DBConfig.MediaColumns.FIELD_FILE_NAME));
        fileNamePY = cursor.getString(cursor.getColumnIndex(DBConfig.MediaColumns.FIELD_FILE_NAME_PY));
        fileSize = cursor.getLong(cursor.getColumnIndex(DBConfig.MediaColumns.FIELD_FILE_LENGTH));
        
        parseId3 = Integer.valueOf(cursor.getString(cursor.getColumnIndex(DBConfig.MediaColumns.FIELD_PARSE_ID3)));
        title = cursor.getString(cursor.getColumnIndex(DBConfig.MediaColumns.FIELD_TITLE));
        artist = cursor.getString(cursor.getColumnIndex(DBConfig.MediaColumns.FIELD_ARTIST));
        album = cursor.getString(cursor.getColumnIndex(DBConfig.MediaColumns.FIELD_ALBUM));
        composer = cursor.getString(cursor.getColumnIndex(DBConfig.MediaColumns.FIELD_COMPOSER));
        genre = cursor.getString(cursor.getColumnIndex(DBConfig.MediaColumns.FIELD_GENRE));
        duration = cursor.getInt(cursor.getColumnIndex(DBConfig.MediaColumns.FIELD_DURATION));
        titlePY = cursor.getString(cursor.getColumnIndex(DBConfig.MediaColumns.FIELD_TITLE_PY));
        artistPY = cursor.getString(cursor.getColumnIndex(DBConfig.MediaColumns.FIELD_ARTIST_PY));
        albumPY = cursor.getString(cursor.getColumnIndex(DBConfig.MediaColumns.FIELD_ALBUM_PY));
        albumPicArray = cursor.getBlob(cursor.getColumnIndex(DBConfig.MediaColumns.FIELD_ALBUM_PIC));
        
        collect = Integer.valueOf(cursor.getString(cursor.getColumnIndex(DBConfig.MediaColumns.FIELD_COLLECT)));
        collectPath = cursor.getString(cursor.getColumnIndex(DBConfig.MediaColumns.FIELD_FILE_COLLECT_PATH));
        thumbnailPath = cursor.getString(cursor.getColumnIndex(DBConfig.MediaColumns.FIELD_FILE_THUMBNAIL_PATH));
        
        deviceType = MediaUtil.getDeviceType(filePath);
        fileType = MediaUtil.getMediaType(filePath);
    }
    
    public ContentValues getContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DBConfig.MediaColumns.FIELD_FILE_PATH, filePath);
        contentValues.put(DBConfig.MediaColumns.FIELD_FILE_NAME, fileName);
        contentValues.put(DBConfig.MediaColumns.FIELD_FILE_NAME_PY, fileNamePY);
        contentValues.put(DBConfig.MediaColumns.FIELD_FILE_LENGTH, fileSize);
        
        contentValues.put(DBConfig.MediaColumns.FIELD_PARSE_ID3, parseId3);
        contentValues.put(DBConfig.MediaColumns.FIELD_TITLE, title);
        contentValues.put(DBConfig.MediaColumns.FIELD_ARTIST, artist);
        contentValues.put(DBConfig.MediaColumns.FIELD_ALBUM, album);
        contentValues.put(DBConfig.MediaColumns.FIELD_COMPOSER, composer);
        contentValues.put(DBConfig.MediaColumns.FIELD_GENRE, genre);
        contentValues.put(DBConfig.MediaColumns.FIELD_DURATION, duration);
        contentValues.put(DBConfig.MediaColumns.FIELD_TITLE_PY, titlePY);
        contentValues.put(DBConfig.MediaColumns.FIELD_ARTIST_PY, artistPY);
        contentValues.put(DBConfig.MediaColumns.FIELD_ALBUM_PY, albumPY);
        contentValues.put(DBConfig.MediaColumns.FIELD_ALBUM_PIC, albumPicArray);

        contentValues.put(DBConfig.MediaColumns.FIELD_COLLECT, collect);
        contentValues.put(DBConfig.MediaColumns.FIELD_FILE_COLLECT_PATH, collectPath);
        contentValues.put(DBConfig.MediaColumns.FIELD_FILE_THUMBNAIL_PATH, thumbnailPath);
        
        return contentValues;
    }

    public void release() {
        // TODO
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDeviceType() {
        return deviceType;
    }
    
    public String getDevicePath() {
        return MediaUtil.getDevicePath(deviceType);
    }

    public void setDeviceType(int deviceType) {
        this.deviceType = deviceType;
    }

    public int getFileType() {
        return fileType;
    }

    public void setFileType(int fileType) {
        this.fileType = fileType;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileNamePY() {
        return fileNamePY;
    }

    public void setFileNamePY(String fileNamePY) {
        this.fileNamePY = fileNamePY;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public int getParseId3() {
        return parseId3;
    }

    public void setParseId3(int parseId3) {
        this.parseId3 = parseId3;
    }

    public String getTitle() {
        return title;
    }
    
    public String getTitleEx() {
        if (TextUtils.isEmpty(title)) {
            return fileName;
        }
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getComposer() {
        return composer;
    }

    public void setComposer(String composer) {
        this.composer = composer;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getTitlePY() {
        return titlePY;
    }

    public void setTitlePY(String titlePY) {
        this.titlePY = titlePY;
    }

    public String getArtistPY() {
        return artistPY;
    }

    public void setArtistPY(String artistPY) {
        this.artistPY = artistPY;
    }

    public String getAlbumPY() {
        return albumPY;
    }

    public void setAlbumPY(String albumPY) {
        this.albumPY = albumPY;
    }

    public byte[] getAlbumPicArray() {
        return albumPicArray;
    }

    public void setAlbumPicArray(byte[] albumPicArray) {
        this.albumPicArray = albumPicArray;
    }

    public int getCollect() {
        return collect;
    }

    public void setCollect(int collect) {
        this.collect = collect;
    }

    public String getCollectPath() {
        return collectPath;
    }

    public void setCollectPath(String collectPath) {
        this.collectPath = collectPath;
    }

    public boolean isFromCollectTable() {
        return isFromCollectTable;
    }

    public void setFromCollectTable(boolean isFromCollectTable) {
        this.isFromCollectTable = isFromCollectTable;
        this.fromDeviceType = MediaUtil.getDeviceType(collectPath);
    }
    
    public void setUnCollect() {
        collect = 0;
        collectPath = "";
    }
    
    public int getFromDeviceType() {
        return fromDeviceType;
    }
    
    public boolean isUpdateDBByID() {
        return updateDBByID;
    }

    public void setUpdateDBByID(boolean updateDBByID) {
        this.updateDBByID = updateDBByID;
    }

    public File getFile() {
        return new File(this.filePath);
    }
    
    /**************************************************** 非数据库部分的数据  **/
    
    private Bitmap bitmap;
    
    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public static void sortMediaFileBeanList(List<FileNode> fileNodeList) {
        Collections.sort(fileNodeList, new Comparator<FileNode>() {
            @Override
            public int compare(FileNode left, FileNode right) {
                return left.getFileNamePY().compareToIgnoreCase(right.getFileNamePY());
            }
        });
    }
    
    private int playTime;

    public int getPlayTime() {
        return playTime;
    }

    public void setPlayTime(int playTime) {
        this.playTime = playTime;
    }
    
    public boolean isSame(FileNode fileNode) {
        if (fileNode == null) {
            return false;
        }
        return filePath.equals(fileNode.getFilePath());
    }
    
    public boolean isSamePathAndFrom(FileNode fileNode) {
        if (fileNode == null) {
            return false;
        }
        return filePath.equals(fileNode.getFilePath()) &&
                isFromCollectTable == fileNode.isFromCollectTable;
    }
    
    private boolean isSelected;

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }
    
    public String getLastDate() {
        if (lastDate == null) {
            File file = getFile();
            Date date = new Date(file.lastModified());
            lastDate = new SimpleDateFormat("yyyy-MM-dd").format(date);
        }
        return lastDate;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("id=");
        buffer.append(id);
        buffer.append("; deviceType="+deviceType);
        buffer.append(deviceType);
        buffer.append("; fileType=");
        buffer.append(fileType);
        buffer.append("; filePath=");
        buffer.append(filePath);
        buffer.append("; title=");
        buffer.append(title);
        buffer.append("; artist=");
        buffer.append(artist);
        buffer.append("; collect=");
        buffer.append(collect);
        buffer.append("; collectPath=");
        buffer.append(collectPath);
        buffer.append("; isFromCollectTable=");
        buffer.append(isFromCollectTable);
        return buffer.toString();
    }
    
    private String getSearchStr() {
        return (fileName + fileNamePY + title + titlePY + artist + artistPY + album + albumPY).toLowerCase();
    }
    
    public static ArrayList<FileNode> matchOperator(ArrayList<FileNode> sourceList, String searchStr) {
        ArrayList<FileNode> resultList = new ArrayList<FileNode>();
        if (sourceList.size() > 0) {
            for (FileNode fileNode : sourceList) {
                if (fileNode.getSearchStr().contains(searchStr.toLowerCase())) {
                    resultList.add(fileNode);
                }
            }
        }
        return resultList;
    }
    
    private String thumbnailPath;

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String mThumbnailPath) {
        this.thumbnailPath = mThumbnailPath;
    }
    
    private boolean unSupportFlag;
    public boolean isUnSupportFlag() {
        return unSupportFlag;
    }
    
    public void setUnSupportFlag(boolean unSupportFlag) {
        this.unSupportFlag = unSupportFlag;
    }
    
    public boolean isExist(Context context) {
        boolean existFlag = false;
        if (deviceType != DeviceType.COLLECT) {
            StorageBean storageBean = AllMediaList.instance(context).getStoragBean(deviceType);
            existFlag = storageBean.isMounted() && getFile().exists();
        } else {
            StorageBean storageBean = AllMediaList.instance(context).getStoragBean(fromDeviceType);
            existFlag = storageBean.isMounted() && getFile().exists();
        }
        return existFlag;
    }
    
    /**
     * 解析ID3信息：音乐和视频。
     */
    public void parseID3Info() {
        if (getFileType() == FileType.AUDIO) {
            parseMusicID3Info();
        } else if (getFileType() == FileType.VIDEO) {
            parseVideoID3Info();
        }
    }
    
    private void parseMusicID3Info() {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        FileInputStream is = null;
        try {
            if (getFile().exists()) {
                is = new FileInputStream(getFilePath());
                FileDescriptor fd = is.getFD();
                retriever.setDataSource(fd);
                
                if (getParseId3() == 0) {
                    String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                    if (title != null && !title.equals("")) {
                        setTitle(title);
                    }
                    setArtist(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
                    setAlbum(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM));
                    setComposer(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER));
                    setGenre(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE));
                    String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    if (duration != null && !duration.equals("")) {
                        setDuration(Integer.valueOf(duration));
                    }
                    setAlbumPicArray(null);
                    if (title != null) {
                        String py = PingYingTool.parseString(title);
                        setTitlePY(py);
                    }
                    if (getArtist() != null) {
                        String py = PingYingTool.parseString(getArtist());
                        if (py != null && py.length() > 0) {
                            py = py.toUpperCase();
                            setArtistPY(py);
                        }
                    }
                    if (getAlbum() != null) {
                        String py = PingYingTool.parseString(getAlbum());
                        if (py != null && py.length() > 0) {
                            py = py.toUpperCase();
                            setAlbumPY(py);
                        }
                    }
                    // 如果Thumbnail存在，不用通过MMR解析。 Thumbnail不存在，就通过MMR解析，解析成功才设置mThumbnailPath。
                    String bitmapPath = getDevicePath() + "/.geelyCache/" + getMD5Str(is);
                    if (saveBitmap(bitmapPath, retriever)) {
                        thumbnailPath = bitmapPath;
                    } else {
                        thumbnailPath = null;
                    }
                    
                    setParseId3(1);
                    
                    // 更新数据库信息。
                    Context context = MediaApplication.getInstance().getApplicationContext();
                    MediaDbHelper.instance(context).update(this);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            retriever.release();
        }
    }
    
    private void parseVideoID3Info() {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        FileInputStream is = null;
        try {
            if (getFile().exists()) {
                is = new FileInputStream(getFilePath());
                FileDescriptor fd = is.getFD();
                retriever.setDataSource(fd);
                if (getParseId3() == 0) {
                    String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                    if (title != null && !title.equals("")) {
                        setTitle(title);
                    }
                    String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    if (duration != null && !duration.equals("")) {
                        setDuration(Integer.valueOf(duration));
                    }
                    setAlbumPicArray(null);
                    if (title != null) {
                        String py = PingYingTool.parseString(title);
                        setTitlePY(py);
                    }
                    
                    // 如果Thumbnail存在，不用通过MMR解析。 Thumbnail不存在，就通过MMR解析，解析成功才设置mThumbnailPath。
                    String bitmapPath = getDevicePath() + "/.geelyCache/" + getMD5Str(is);
                    if (saveBitmap(bitmapPath, retriever)) {
                        thumbnailPath = bitmapPath;
                    } else {
                        thumbnailPath = null;
                    }
                    
                    setParseId3(1);
                    
                    // 更新数据库信息。
                    Context context = MediaApplication.getInstance().getApplicationContext();
                    MediaDbHelper.instance(context).update(this);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            retriever.release();
        }
    }
    
    private String getMD5Str(FileInputStream is)
            throws NoSuchAlgorithmException, IOException {
        StringBuilder md5Str = new StringBuilder();;
        // 生成MD5文件。
        MessageDigest md = MessageDigest.getInstance("MD5");
        long length = 1024;
        if (getFile().length() < length) {
            length = getFile().length();
        }
        byte[] buffer = new byte[(int) length];
        is.read(buffer, 0, buffer.length);
        // 第一步：“1K 文件内容” 生成MD5.
        String firstMD5 = (new BigInteger(1, md.digest())).toString();
        // 第一步：“firstMD5 + 文件名 + 文件长度 + 文件修改时间” 生成MD5.
        String fileInfo = getFileName() + 
                getFile().length() + "" + getFile().lastModified();
        byte[] md3Byte = md.digest((firstMD5 + fileInfo).getBytes());
        for (int i = 0; i < md3Byte.length; i++) {
            int temp = md3Byte[i] & 0xFF;
            String hexString = Integer.toHexString(temp);
            if (hexString.length() < 2) {
                md5Str.append("0").append(hexString);
            } else {
                md5Str.append(hexString);
            }
        }
        
        return md5Str.toString();
    }
    
    private boolean saveBitmap(String bitmapPath, MediaMetadataRetriever retriever) {
        File bitmapFile = new File(bitmapPath);
        if (bitmapFile.exists()) {
            return true;
        }
        boolean saveSuccees = false;
        if (!bitmapFile.exists()) {
            try {
                Bitmap bitmap = null;
                if (getFileType() == FileType.AUDIO) {
                    if (getFile().exists()) {
                        bitmap = MediaUtil.BytesToBitmap(retriever.getEmbeddedPicture());
                    }
                    if (bitmap != null) {
                        bitmap = MediaUtil.BitmapScale(bitmap, 100, 100);
                    }
                } else if (getFileType() == FileType.VIDEO) {
                    if (getDuration() > 1000000) { // 单位是微秒
                        bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_NEXT_SYNC);
                    } else {
                        bitmap = retriever.getFrameAtTime();
                    }
                    if (bitmap != null) {
                        bitmap = MediaUtil.BitmapScale(bitmap, 380, 198);
                    }
                }
                if (bitmap != null) {
                    File cacheDir = new File(getDevicePath() + "/.geelyCache");
                    if (cacheDir != null && !cacheDir.exists()) {
                        cacheDir.mkdirs();
                        //设置文件属性为隐藏
                        try {
                            Runtime.getRuntime().exec(" attrib +H  "+cacheDir.getAbsolutePath()); 
                        } catch (Exception e) {
                        }
                    }
                    FileOutputStream out = new FileOutputStream(bitmapFile);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.flush();
                    out.close();
                    saveSuccees = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return saveSuccees;
    }
}
