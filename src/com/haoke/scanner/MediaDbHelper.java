package com.haoke.scanner;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.haoke.bean.FileNode;
import com.haoke.bean.ID3Parse;
import com.haoke.bean.StorageBean;
import com.haoke.bean.UserBean;
import com.haoke.constant.DBConfig;
import com.haoke.constant.MediaUtil;
import com.haoke.constant.DBConfig.TableName;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.data.AllMediaList;
import com.haoke.util.DebugClock;
import com.haoke.util.DebugLog;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

public class MediaDbHelper extends SQLiteOpenHelper {
    private static final String TAG = "MediaDbHelper";
    private Context mContext = null;
    
    public Context getContext() {
        return mContext;
    }
    
    public MediaDbHelper(Context context) {
        super(context, DBConfig.DATABASE_NAME, null, DBConfig.DATABASE_VERSION);
        mContext = context;
    }
    
    private static MediaDbHelper sMediaDbHelper = null;
    public static MediaDbHelper instance(Context context) {
        if (sMediaDbHelper == null) {
            sMediaDbHelper = new MediaDbHelper(context.getApplicationContext());
        }
        return sMediaDbHelper;
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        for (int deviceType : DBConfig.sDeviceDefaultList) {
            db.execSQL(getCreateTableString(DBConfig.TABLE_AUDIO + deviceType));
            db.execSQL(getCreateTableString(DBConfig.TABLE_VIDEO + deviceType));
            db.execSQL(getCreateTableString(DBConfig.TABLE_IMAGE + deviceType));
        }

        db.execSQL(getCreateTableString(DBConfig.TABLE_AUDIO + DeviceType.COLLECT));
        db.execSQL(getCreateTableString(DBConfig.TABLE_VIDEO + DeviceType.COLLECT));
        db.execSQL(getCreateTableString(DBConfig.TABLE_IMAGE + DeviceType.COLLECT));
    }
    
    private String getCreateTableString(String table) {
        String sql = "create table "+table+"("+
                DBConfig.MediaColumns.FIELD_ID            +" integer primary key autoincrement,"+
                DBConfig.MediaColumns.FIELD_FILE_PATH     +" nvarchar(256),"+
                DBConfig.MediaColumns.FIELD_FILE_NAME     +" nvarchar(256),"+
                DBConfig.MediaColumns.FIELD_FILE_NAME_PY  +" nvarchar(256),"+
                DBConfig.MediaColumns.FIELD_FILE_LENGTH   +" long,"+
                DBConfig.MediaColumns.FIELD_PARSE_ID3     +" integer DEFAULT 0,"+
                DBConfig.MediaColumns.FIELD_TITLE         +" nvarchar(256),"+
                DBConfig.MediaColumns.FIELD_ARTIST        +" nvarchar(256),"+
                DBConfig.MediaColumns.FIELD_ALBUM         +" nvarchar(256),"+
                DBConfig.MediaColumns.FIELD_COMPOSER      +" nvarchar(256),"+
                DBConfig.MediaColumns.FIELD_GENRE         +" nvarchar(256),"+
                DBConfig.MediaColumns.FIELD_DURATION      +" integer,"+
                DBConfig.MediaColumns.FIELD_TITLE_PY      +" nvarchar(256),"+
                DBConfig.MediaColumns.FIELD_ARTIST_PY     +" nvarchar(256),"+
                DBConfig.MediaColumns.FIELD_ALBUM_PY      +" nvarchar(256),"+
                DBConfig.MediaColumns.FIELD_ALBUM_PIC     +" blob,"+
                DBConfig.MediaColumns.FIELD_COLLECT       +" integer DEFAULT 0," +
                DBConfig.MediaColumns.FIELD_FILE_COLLECT_PATH + " text," +
                DBConfig.MediaColumns.FIELD_FILE_THUMBNAIL_PATH + " text," +
                DBConfig.MediaColumns.FIELD_USERNAME + " text" + ")";
        return sql;
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (int deviceType : DBConfig.sDeviceDefaultList) {
            db.execSQL("DROP TABLE IF EXISTS " + DBConfig.TABLE_AUDIO + deviceType);
            db.execSQL("DROP TABLE IF EXISTS " + DBConfig.TABLE_VIDEO + deviceType);
            db.execSQL("DROP TABLE IF EXISTS " + DBConfig.TABLE_IMAGE + deviceType);
        }
        
        db.execSQL("DROP TABLE IF EXISTS " + DBConfig.TABLE_AUDIO + DeviceType.COLLECT);
        db.execSQL("DROP TABLE IF EXISTS " + DBConfig.TABLE_VIDEO + DeviceType.COLLECT);
        db.execSQL("DROP TABLE IF EXISTS " + DBConfig.TABLE_IMAGE + DeviceType.COLLECT);
        if (oldVersion < 4) {
            db.execSQL("DROP TABLE IF EXISTS " + DBConfig.TABLE_SAVE_MUSIC);
        }
        onCreate(db);
    }

    public void addRecord(int deviceType, int fileType, FileNode node) {
        insert(getWritableDatabase(), DBConfig.getTableName(deviceType, fileType), null, node.getContentValues());
    }
    
    public FileNode queryRecord(int deviceType, int fileType, int index) {
        FileNode node = null;
        ArrayList<FileNode> list = queryMedia(deviceType, fileType,
                DBConfig.MediaColumns.FIELD_ID + "=?", new String[]{index + ""});
        if (list.size() >= 1) {
            node = list.get(0);
        }
        return node;
    }

    public void clearData(int deviceType, int fileType) {
        String tableName = DBConfig.getTableName(deviceType, fileType);
        String sqlStr = "DELETE FROM " + tableName;
        execSQL(getWritableDatabase(), tableName, sqlStr);
    }
    
    public void clearDeviceData(int deviceType) {
        clearData(deviceType, FileType.AUDIO);
        clearData(deviceType, FileType.VIDEO);
        clearData(deviceType, FileType.IMAGE);
    }
    
    public void insert(FileNode fileNode) {
        insertTask(fileNode);
    }
    
    public void update(FileNode fileNode) {
        updateTask(fileNode);
    }
    
    public void delete(FileNode fileNode) {
        deleteTask(fileNode);
    }
    
    public void updateCollectInfoByFileExist(int fileType) {
        ArrayList<FileNode> collectList = queryCollected(fileType, true);
        if (collectList.size() > 0) {
            for (FileNode collectNode : collectList) {
                if (!collectNode.getFile().exists()) { // 对应的文件不存在的情况。
                    DebugLog.e(TAG, "This file is not exist: " + collectNode.getFilePath());
                    int oldDeviceType = collectNode.getFromDeviceType();
                    String oldDevicePath = MediaUtil.getDevicePath(oldDeviceType);
                    String filePath = collectNode.getFilePath();
                    String secondPath = filePath.substring(filePath.indexOf(oldDevicePath) + oldDevicePath.length(), filePath.length());
                    
                    File checkFile = null;
                    if (MediaUtil.DEVICE_PATH_USB_1.equals(oldDevicePath)) {
                        checkFile = new File(MediaUtil.DEVICE_PATH_USB_2 + secondPath);
                    } else if (MediaUtil.DEVICE_PATH_USB_2.equals(oldDevicePath)) {
                        checkFile = new File(MediaUtil.DEVICE_PATH_USB_1 + secondPath);
                    }
                    if (checkFile != null && checkFile.exists()) { // 在另外的一个U盘中存在。
                        String newFilePath = checkFile.getAbsolutePath();
                        collectNode.setFilePath(newFilePath);
                        collectNode.setCollectPath(newFilePath);
                        DebugLog.e(TAG, "refresh the path: " + newFilePath);
                        collectNode.setUpdateDBByID(true);
                        addToNeedToInsertList(new TransactionTask(collectNode, TransactionTask.UPDATE_TASK));
                    }
                }
            }
        }
    }
    
    /**
     * 参照收藏表的内容来更新对应的媒体表的Collect信息。
     * @param fileType 类型有音乐，视频和图片。
     */
    public void updateMediaInfoAccordingToCollect(int fileType) {
        ArrayList<FileNode> collectList = queryCollected(fileType, false);
        if (collectList.size() > 0) {
            for (FileNode collectNode : collectList) {
                FileNode mediaNode = new FileNode(collectNode);
                mediaNode.setCollect(1);
                mediaNode.setFilePath(collectNode.getCollectPath());
                mediaNode.setCollectPath(collectNode.getFilePath());
                mediaNode.setDeviceType(MediaUtil.getDeviceType(mediaNode.getFilePath()));
                addToNeedToInsertList(new TransactionTask(mediaNode, TransactionTask.UPDATE_TASK));
            }
        }
    }
    
    public void notifyCollectChange() {
        mContext.getContentResolver().notifyChange(
                Uri.parse(DBConfig.getUriAddress(TableName.COLLECT_AUDIO_TABLE_NAME)), null);
        mContext.getContentResolver().notifyChange(
                Uri.parse(DBConfig.getUriAddress(TableName.COLLECT_VIDEO_TABLE_NAME)), null);
        mContext.getContentResolver().notifyChange(
                Uri.parse(DBConfig.getUriAddress(TableName.COLLECT_IMAGE_TABLE_NAME)), null);
    }
    
    public static class TransactionTask {
        public static final int INSERT_TASK = 1;
        public static final int UPDATE_TASK = 2;
        public static final int DELETE_TASK = 3;
        public FileNode mFileNode;
        public int mTaskType;
        public TransactionTask(FileNode fileNode, int taskType) {
            mFileNode = fileNode;
            mTaskType = taskType;
        }
    }
    
    private List<TransactionTask> mNeedToInsertList = Collections.synchronizedList(new ArrayList<TransactionTask>());
    private boolean mStartFlag = false;
    
    public void setStartFlag(boolean mStartFlag) {
        this.mStartFlag = mStartFlag;
        if (!mStartFlag) {
            doTaskList();
        }
    }

    public void addToNeedToInsertList(TransactionTask task) {
        if (mNeedToInsertList.size() < 500) {
            mNeedToInsertList.add(task);
        }
        if (mNeedToInsertList.size() >= 500 || !mStartFlag) {
            doTaskList();
        }
    }
    
    private void insertTask(FileNode fileNode) {
        insert(getWritableDatabase(), DBConfig.getTableName(fileNode.getDeviceType(), fileNode.getFileType()),
                null, fileNode.getContentValues());
    }
    
    private void deleteTask(FileNode fileNode) {
        delete(getWritableDatabase(), DBConfig.getTableName(fileNode.getDeviceType(), fileNode.getFileType()),
                DBConfig.MediaColumns.FIELD_FILE_PATH + "=?",
                new String[]{fileNode.getFilePath()});
    }
    
    private void updateTask(FileNode fileNode) {
        if (fileNode.isUpdateDBByID()) {
            update(getWritableDatabase(), DBConfig.getTableName(fileNode.getDeviceType(), fileNode.getFileType()),
                    fileNode.getContentValues(), DBConfig.MediaColumns.FIELD_ID + "=?",
                    new String[]{fileNode.getId() + ""});
        } else {
            update(getWritableDatabase(), DBConfig.getTableName(fileNode.getDeviceType(), fileNode.getFileType()),
                    fileNode.getContentValues(), DBConfig.MediaColumns.FIELD_FILE_PATH + "=?",
                    new String[]{fileNode.getFilePath()});
        }
    }
    
    private void doTaskList() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            // TODO: 多线程操作List的安全性问题。
            while (mNeedToInsertList.size() > 0) {
                TransactionTask task = mNeedToInsertList.remove(0);
                switch (task.mTaskType) {
                case TransactionTask.INSERT_TASK:
                    insertTask(task.mFileNode);
                    break;
                case TransactionTask.DELETE_TASK:
                    deleteTask(task.mFileNode);
                    break;
                case TransactionTask.UPDATE_TASK:
                    updateTask(task.mFileNode);
                    break;
                default:
                    break;
                }
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            tryCatchEndTransaction(db);
        }
    }
    
    // 添加SQLiteFullException的判断来规避“因为磁盘被写满而爆出的崩溃问题”。
    private void tryCatchEndTransaction(SQLiteDatabase db) {
        try {
            db.endTransaction();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void execSQL(SQLiteDatabase db, String tableName, String sqlStr) {
        db.execSQL(sqlStr);
        mContext.getContentResolver().notifyChange(Uri.parse(DBConfig.getUriAddress(tableName)), null);
    }
    
    private long insert(SQLiteDatabase db, String tableName, String nullColumnHack, ContentValues values) {
        long ret = db.insert(tableName, nullColumnHack, values);
        if (ret > 0) {
            mContext.getContentResolver().notifyChange(Uri.parse(DBConfig.getUriAddress(tableName)), null);
        } else {
            DebugLog.e(TAG, "Error insert!");
        }
        return ret;
    }
    
    private int delete(SQLiteDatabase db, String tableName, String whereClause, String[] whereArgs) {
        int ret = db.delete(tableName, whereClause, whereArgs);
        if (ret > 0) {
            mContext.getContentResolver().notifyChange(Uri.parse(DBConfig.getUriAddress(tableName)), null);
        }
        return ret;
    }
    
    private int update(SQLiteDatabase db, String tableName, ContentValues values,
            String whereStr, String[] whereArgs) {
        int ret = -1;
        if (tableName != null) {
            ret = db.update(tableName, values, whereStr, whereArgs);
            if (ret > 0) {
                // 大小不变的情况下，不通知。 TODO
                // mContext.getContentResolver().notifyChange(Uri.parse(DBConfig.getUriAddress(tableName)), null);
            }
        } else {
            DebugLog.e(TAG, "update failed tableName is null: ");
            Thread.dumpStack();
        }
        return ret;
    }
    
    public ArrayList<FileNode> query(String tableName, String selection, String[] selectionArgs, boolean allFlag) {
        ArrayList<FileNode> fileNodeList = new ArrayList<FileNode>();
        boolean isCollect = tableName.equals(DBConfig.TableName.COLLECT_AUDIO_TABLE_NAME) ||
                tableName.equals(DBConfig.TableName.COLLECT_VIDEO_TABLE_NAME) ||
                tableName.equals(DBConfig.TableName.COLLECT_IMAGE_TABLE_NAME);
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().query(tableName,
                    null, selection, selectionArgs, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    FileNode fileNode = new FileNode(cursor);
                    if (isCollect) {
                        fileNode.setFromCollectTable(true);
                    }
                    if (allFlag) {
                        fileNodeList.add(fileNode);
                    } else {
                        if (fileNode.getFile().exists()) {
                            if (isCollect) { // 如果是收藏表中的数据。还需要判断磁盘是否挂载。
                                StorageBean storageBean = AllMediaList.instance(mContext)
                                        .getStoragBean(fileNode.getDeviceType());
                                if (storageBean.isMounted()) {
                                    fileNodeList.add(fileNode);
                                }
                            } else {
                                fileNodeList.add(fileNode);
                            }
                        }
                    }
                } while (cursor.moveToNext());
            }
            if (fileNodeList.size() == 0) {
                DebugLog.e(TAG, "tableName: + " + tableName +
                        "数据库中没有记录 selection: " + selection + " selectionArgs: " + selectionArgs);
            } else {
                FileNode.sortMediaFileBeanList(fileNodeList);
                DebugLog.d(TAG, "tableName: + " + tableName +
                        "数据库中数据总数： " + fileNodeList.size());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return fileNodeList;
    }
    
    public ArrayList<FileNode> queryMedia(int deviceType, int fileType, String selection, String[] selectionArgs) {
        ArrayList<FileNode> mediaList = query(DBConfig.getTableName(deviceType, fileType), selection, selectionArgs, false);
        for (int i = 0; i < mediaList.size(); i++) { // 数据库中没有deviceType和fileType，所以需要进行填充。
            mediaList.get(i).setDeviceType(deviceType);
            mediaList.get(i).setFileType(fileType);
        }
        return mediaList;
    }
    
    private ArrayList<FileNode> queryCollected(String username, int fileType, boolean allFlag) {
        String selection = null;
        String[] selectionArgs = null;
        if (!TextUtils.isEmpty(username)) {
            selection = DBConfig.MediaColumns.FIELD_USERNAME + "=?";
            selectionArgs = new String[]{username};
        }
        ArrayList<FileNode> collectList = query(DBConfig.getTableName(DeviceType.COLLECT, fileType), selection, selectionArgs, allFlag);
        for (int i = 0; i < collectList.size(); i++) { // 数据库中没有deviceType和fileType，所以需要进行填充。
            collectList.get(i).setDeviceType(DeviceType.COLLECT);
            collectList.get(i).setFileType(fileType);
        }
        return collectList;
    }
    
    public ArrayList<FileNode> queryCollected(int fileType, boolean allFlag) {
        return queryCollected(MediaUtil.getUserName(), fileType, allFlag);
    }
    
    /**
     * 根据FileType来获得数据库中的所有的媒体列表（用来做Search操作）。
     */
    public ArrayList<FileNode> searchMedia(int fileType, String searchStr) {
        ArrayList<FileNode> searchList = new ArrayList<FileNode>();
        ArrayList<Integer> mediaList = null;
        if (fileType == FileType.AUDIO) {
            mediaList = DBConfig.sAudioDefaultList;
        } else if (fileType == FileType.VIDEO) {
            mediaList = DBConfig.sVideoDefaultList;
        } else if (fileType == FileType.IMAGE) {
            mediaList = DBConfig.sImageDefaultList;
        }
        if (mediaList != null) {
            for (int deviceType : mediaList) {
                String devicePath = MediaUtil.getDevicePath(deviceType);
                if (MediaUtil.checkMounted(mContext, devicePath) &&
                        AllMediaList.instance(getContext()).getStoragBean(deviceType).isMounted()) {
                    ArrayList<FileNode> list = queryMedia(deviceType, fileType, null, null);
                    searchList.addAll(FileNode.matchOperator(list, searchStr));
                }
            }
            ArrayList<FileNode> collectList = queryCollected(fileType, false);
            if (collectList.size() > 0) {
                searchList.addAll(FileNode.matchOperator(collectList, searchStr));
            }
        }
        return searchList;
    }
    
    private void updateCollectDataFromChangeUseList(ArrayList<UserBean> userList, int fileType) {
        ArrayList<FileNode> collectList = queryCollected(null, fileType, true);
        ArrayList<FileNode> deleteList = new ArrayList<FileNode>(collectList);
        if (collectList.size() > 0 && userList.size() > 0) {
            for (FileNode fileNode : collectList) {
                for (UserBean userBean : userList) {
                    if (userBean.getUsername().equals(fileNode.getUsername())) {
                        deleteList.remove(fileNode);
                    }
                }
            }
        }
        DebugLog.i(TAG, "updateCollectDataFromChangeUseList collectList size: " + collectList.size());
        DebugLog.i(TAG, "updateCollectDataFromChangeUseList deleteList size: " + deleteList.size());
        AllMediaList.instance(mContext).uncollectMediaFiles(deleteList, null);
    }
    
    public void updateCollectDataFromChangeUseList() {
        ArrayList<UserBean> userList = MediaUtil.getUserList(mContext);
        updateCollectDataFromChangeUseList(userList, FileType.IMAGE);
        updateCollectDataFromChangeUseList(userList, FileType.AUDIO);
        updateCollectDataFromChangeUseList(userList, FileType.VIDEO);
    }
    
    /**
     * 当收藏表加载完成，参照收藏表来更新媒体表中的媒体收藏的信息。 不适用媒体表中的collect和collectPath字段。
     */
    public void updateCollectDataOfMediaTableFromCollectTable(int deviceType, int fileType) {
        DebugClock debugClock = new DebugClock();
        for (int devicetype : DBConfig.sScan3zaDefaultList) {
            updateCollectDataOfMediaTable(devicetype, fileType);
        }
        debugClock.calculateTime(TAG, "updateCollectDataOfMediaTableFromCollectTable fileType: " + fileType);
    }
    
    private void updateCollectDataOfMediaTable(int deviceType, int fileType) {
        AllMediaList allMediaList = AllMediaList.instance(mContext);
        ArrayList<FileNode> collectList = allMediaList.getMediaList(DeviceType.COLLECT, fileType);
        if (allMediaList.getStoragBean(deviceType).isMounted()) {
            ArrayList<FileNode> mediaList = allMediaList.getMediaList(deviceType, fileType);
            for (int index = 0; index < mediaList.size(); index++) {
                try {
                    FileNode fileNode = mediaList.get(index);
                    boolean isContain = judgeContain(fileNode, collectList);
                    fileNode.setCollect(isContain ? 1 : 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private boolean judgeContain(FileNode fileNode, ArrayList<FileNode> collectList) {
        boolean isContain = false;
        for (FileNode item : collectList) {
            if (item.isSame(fileNode)) {
                isContain = true;
                break;
            }
        }
        return isContain;
    }
}
