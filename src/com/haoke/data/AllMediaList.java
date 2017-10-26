package com.haoke.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.haoke.application.MediaApplication;
import com.haoke.bean.FileNode;
import com.haoke.bean.StorageBean;
import com.haoke.constant.DBConfig;
import com.haoke.constant.MediaUtil;
import com.haoke.constant.DBConfig.UriAddress;
import com.haoke.constant.DBConfig.UriType;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.mediaservice.R;
import com.haoke.scanner.MediaDbHelper;
import com.haoke.scanner.MediaDbHelper.TransactionTask;
import com.haoke.service.MediaService;
import com.haoke.util.DebugLog;

public class AllMediaList {
    private static final String TAG = "AllMediaList";
    private Object mLoadLock = new Object();
    /**
     * 存储的是数据库中的数据信息。
     * Key: 数据库中表的名字。
     * Value: 数据库表中的数据。
     */
    private HashMap<String, ArrayList<FileNode>> mAllMediaHash = new HashMap<String, ArrayList<FileNode>>();
    /**
     * 对数据库中表的监听ContentObserver的List列表。
     */
    private ArrayList<MediaContentObserver> mObserverList = new ArrayList<MediaContentObserver>();
    /**
     * 监听加载完成的函数回调列表。
     */
    private ArrayList<LoadListener> mLoadListenerList = new ArrayList<LoadListener>();
    
    private HashMap<String, StorageBean> mScanStateHash = new HashMap<String, StorageBean>();
    
    private Context mContext;
    private MediaDbHelper mMediaDbHelper; // 数据库Helper对象，功能局限于QUERY操作。
    private LocalHandler mLocalHandler; // 用于多线程通信的Handler对象。
    private LoadThread mLoadThread; // 加载线程
    private OperateThread mOperateThread; // 操作线程。
    public static float sCarSpeed;
    
    private static AllMediaList sAllMediaList;
    
    public static AllMediaList instance(Context context) {
        if (sAllMediaList == null) {
            sAllMediaList = new AllMediaList(context);
        }
        return sAllMediaList;
    }

    private AllMediaList(Context context) {
        this.mContext = context.getApplicationContext();
        mMediaDbHelper = new MediaDbHelper(mContext);
        mLocalHandler = new LocalHandler();
        
        registerObserverAll();
    }
    
    public ArrayList<FileNode> getMediaList(int deviceType, int fileType) {
        ArrayList<FileNode> mediaList = null;
        String tableName = DBConfig.getTableName(deviceType, fileType);
        mediaList = mAllMediaHash.get(tableName);
        if (mediaList == null) {
            // 如果找不到数据，就发起数据查询操作。 
            StorageBean storageBean = getStoragBean(deviceType);
            if (storageBean.isId3ParseCompleted()) {
                mLocalHandler.obtainMessage(BEGIN_LOAD_THREAD, deviceType, fileType, null).sendToTarget();
            }
            mediaList = new ArrayList<FileNode>();
        }
        return mediaList;
    }
    
    /**
     * 获取收藏列表的大小。实时数据。
     * @param fileType
     * @return
     */
    public int getCollectSize(int fileType) {
        ArrayList<FileNode> mediaList = null;
        String tableName = DBConfig.getTableName(DeviceType.COLLECT, fileType);
        mediaList = mAllMediaHash.get(tableName);
        int count = 0;
        if (mediaList != null) {
            count = mediaList.size();
        } else {
            DebugLog.e(TAG, "Error...getCollectSize mediaList is null! ");
        }
        return count;
    }
    
    private void clearMediaList(int deviceType, int fileType) {
        String tableName = DBConfig.getTableName(deviceType, fileType);
        ArrayList<FileNode> mediaList = mAllMediaHash.get(tableName);
        if (mediaList != null) {
            mediaList.clear();
        }
    }
    
    public void registerObserverAll() {
        unRegisterObserverAll();
        registerObserver(UriAddress.URI_USB1_AUDIO_ADDR, UriType.USB1_AUDIO);
        registerObserver(UriAddress.URI_USB1_VIDEO_ADDR, UriType.USB1_VIDEO);
        registerObserver(UriAddress.URI_USB1_IMAGE_ADDR, UriType.USB1_IMAGE);
        registerObserver(UriAddress.URI_USB2_AUDIO_ADDR, UriType.USB2_AUDIO);
        registerObserver(UriAddress.URI_USB2_VIDEO_ADDR, UriType.USB2_VIDEO);
        registerObserver(UriAddress.URI_USB2_IMAGE_ADDR, UriType.USB2_IMAGE);
        registerObserver(UriAddress.URI_FLASH_AUDIO_ADDR, UriType.FLASH_AUDIO);
        registerObserver(UriAddress.URI_FLASH_VIDEO_ADDR, UriType.FLASH_VIDEO);
        registerObserver(UriAddress.URI_FLASH_IMAGE_ADDR, UriType.FLASH_IMAGE);
        registerObserver(UriAddress.URI_COLLECT_AUDIO_ADDR, UriType.COLLECT_AUDIO);
        registerObserver(UriAddress.URI_COLLECT_VIDEO_ADDR, UriType.COLLECT_VIDEO);
        registerObserver(UriAddress.URI_COLLECT_IMAGE_ADDR, UriType.COLLECT_IMAGE);
    }
    
    private void registerObserver(String UriStr, int uriType) {
        MediaContentObserver observer = new MediaContentObserver(new Handler(), uriType);
        mContext.getContentResolver().registerContentObserver(Uri.parse(UriStr), true, observer);
        mObserverList.add(observer);
    }
    
    /**
     * 注销数据库的监听。
     */
    public void unRegisterObserverAll() {
        while (mObserverList.size() > 0) {
            MediaContentObserver observer = mObserverList.remove(0);
            mContext.getContentResolver().unregisterContentObserver(observer);
        }
    }
    
    /**
     * 添加加载的监听回调。
     * @param listener
     */
    public void registerLoadListener(LoadListener listener) {
        mLoadListenerList.add(listener);
    }
    
    /**
     * 取消加载监听的回调。
     * @param listener
     */
    public void unRegisterLoadListener(LoadListener listener) {
        mLoadListenerList.remove(listener);
    }
    
    private void notifyLoadComplete(int deviceType, int fileType) {
        for (LoadListener listener : mLoadListenerList) {
            listener.onLoadCompleted(deviceType, fileType);
        }
    }
    
    private void notifyScanStateChange(StorageBean storageBean) {
        int deviceType = storageBean.getDeviceType();
        if (!storageBean.isMounted()) {
            // 设备被移除，需要清空数据，并通知监听者：设备移除。
            clearMediaList(deviceType, FileType.IMAGE);
            clearMediaList(deviceType, FileType.AUDIO);
            clearMediaList(deviceType, FileType.VIDEO);
            callOnScanStateChange(storageBean);
        } else {
            if (storageBean.isId3ParseCompleted()) {
                mLocalHandler.obtainMessage(BEGIN_LOAD_ALL_THREAD, deviceType, 0, storageBean).sendToTarget();
            } else if (storageBean.getState() == StorageBean.FILE_SCANNING) {
                callOnScanStateChange(storageBean);
            }
        }
    }

    private void callOnScanStateChange(StorageBean storageBean) {
        for (LoadListener listener : mLoadListenerList) {
            listener.onScanStateChange(storageBean);
        }
        mContext.sendBroadcast(new Intent("main_activity_update_ui"));
        if (storageBean.isId3ParseCompleted() || !storageBean.isMounted()) {
            storageBean.setLoadCompleted(true);
        }
    }
    
    private static final int BEGIN_LOAD_THREAD = 1;
    private static final int ITEM_LOAD_COMPLETED = 2;
    private static final int SCAN_STATE_CHANGE = 3;
    private static final int BEGIN_OPERATE_THREAD = 4;
    private static final int ITEM_OPERATE_COMPLETED = 5;
    private static final int NOTIFY_LIST_ITEM_PROGRESS = 6;
    private static final int RELEASE_OPERATE_THREAD = 7;
    private static final int DELETE_FILENODE_FROM_LIST = 8;
    private static final int NOTIFY_SEARCH_BACKCALL = 9;
    private static final int NOTIFY_SCAN_LISTENER = 10;
    private static final int BEGIN_LOAD_ALL_THREAD = 11;
    
    @SuppressLint("HandlerLeak")
    class LocalHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
            case BEGIN_LOAD_THREAD:
                synchronized (mLoadLock) {
                    if (mLoadThread == null) {
                        mLoadThread = new LoadThread();
                    }
                    addToListAndStart(new LoadData(msg.arg1, msg.arg2, (StorageBean) msg.obj));
                    if (!mLoadThread.isAlive()) { // 如果线程没有启动， 就start线程。
                        mLoadThread.start();
                    }
                }
                break;
            case BEGIN_LOAD_ALL_THREAD:
                synchronized (mLoadLock) {
                    if (mLoadThread == null) {
                        mLoadThread = new LoadThread();
                    }
                    addToListAndStart(new LoadData(msg.arg1, FileType.IMAGE, null));
                    addToListAndStart(new LoadData(msg.arg1, FileType.AUDIO, null));
                    addToListAndStart(new LoadData(msg.arg1, FileType.VIDEO, (StorageBean) msg.obj));
                    if (!mLoadThread.isAlive()) { // 如果线程没有启动， 就start线程。
                        mLoadThread.start();
                    }
                }
                break;
            case ITEM_LOAD_COMPLETED:
                notifyLoadComplete(msg.arg1, msg.arg2);
                break;
            case SCAN_STATE_CHANGE:
                StorageBean storageBean = (StorageBean) msg.obj;
                notifyScanStateChange(storageBean);
                break;
            case BEGIN_OPERATE_THREAD:
                if (mOperateThread == null) {
                    mOperateThread = new OperateThread();
                }
                mOperateThread.addToListAndStart((OperateData)msg.obj);
                break;
            case ITEM_OPERATE_COMPLETED: {
                OperateData operateData = (OperateData)msg.obj;
                if (operateData.listener != null) {
                    operateData.listener.onOperateCompleted(operateData.operateValue, msg.arg1, msg.arg2);
                }
                break;
            }
            case NOTIFY_LIST_ITEM_PROGRESS: {
                OperateData operateData = (OperateData)msg.obj;
                if (operateData.listener != null) {
                    operateData.listener.onOperateCompleted(operateData.operateValue, msg.arg1, msg.arg2);
                }
                break;
            }
            case RELEASE_OPERATE_THREAD:
                mOperateThread = null;
                break;
            case DELETE_FILENODE_FROM_LIST:
                removeFileNodeFromList((FileNode) msg.obj);
                break;
            case NOTIFY_SEARCH_BACKCALL:
                SearchData searchData = (SearchData) msg.obj;
                if (searchData.listener != null) {
                    searchData.listener.onSearchCompleted(searchData.dataList);
                }
                break;
            case NOTIFY_SCAN_LISTENER:
                callOnScanStateChange((StorageBean)msg.obj);
                break;
            default:
                break;
            }
        }

        private void removeFileNodeFromList(FileNode fileNode) {
            ArrayList<FileNode> list = getMediaList(fileNode.getDeviceType(), fileNode.getFileType());
            if (!list.remove(fileNode)) {
                for (int i = 0; i < list.size(); i++) {
                    FileNode node = list.get(i);
                    if (node.isSame(fileNode)) {
                        list.remove(i);
                        break;
                    }
                }
            }
        }
    };
    
    class LoadData {
        int deviceType;
        int fileType;
        StorageBean storageBean;
        public LoadData(int deviceType, int fileType, StorageBean storageBean) {
            this.deviceType = deviceType;
            this.fileType = fileType;
            this.storageBean = storageBean;
        }
    }
    
    List<LoadData> mLoadMsgList = Collections.synchronizedList(new ArrayList<LoadData>());
    
    public void addToListAndStart(LoadData data) {
        for (int index = 0; index < mLoadMsgList.size(); index++) {
            LoadData loadData = mLoadMsgList.get(index);
            if (loadData.deviceType == data.deviceType && loadData.fileType == data.fileType) {
                return;
            }
        }
        mLoadMsgList.add(data);
    }
    
    class LoadThread extends Thread {
        @Override
        public void run() {
            synchronized (mLoadLock) {
                while (mLoadMsgList.size() > 0) {
                    LoadData data = mLoadMsgList.remove(0);
                    int deviceType = data.deviceType;
                    int fileType = data.fileType;
                    String tableName = DBConfig.getTableName(deviceType, fileType);
                    ArrayList<FileNode> mediaList = mAllMediaHash.get(tableName);
                    if (mediaList == null) {
                        mediaList = new ArrayList<FileNode>();
                        mAllMediaHash.put(tableName, mediaList);
                    }
                    mediaList.clear();
                    if (deviceType == DeviceType.COLLECT) {
                        mediaList.addAll(mMediaDbHelper.queryCollected(fileType, null, null, false));
                    } else {
                        mediaList.addAll(mMediaDbHelper.queryMedia(deviceType, fileType, null, null));
                    }
                    mLocalHandler.obtainMessage(ITEM_LOAD_COMPLETED, deviceType, fileType).sendToTarget();
                    // 如果notifyFlag为true, 就发起notify操作。
                    if (data.storageBean != null) {
                        mLocalHandler.obtainMessage(NOTIFY_SCAN_LISTENER, deviceType, fileType, data.storageBean).sendToTarget();
                    }
                }
                mLoadThread = null;
            }
        }
    }
    
    public class MediaContentObserver extends ContentObserver {
        private int mUriType;
        public MediaContentObserver(Handler handler, int uriType) {
            super(handler);
            mUriType = uriType;
        }

        @Override
        public void onChange(boolean selfChange) {
            int deviceType = DBConfig.getDeviceTypeByUriType(mUriType);
            int fileType = DBConfig.getFileTypeByUriType(mUriType);
            StorageBean storageBean = getStoragBean(deviceType);
            if (storageBean.isId3ParseCompleted()) {
                DebugLog.e(TAG, "onChange deviceType: " + deviceType + " && fileType: " + fileType);
                mLocalHandler.obtainMessage(BEGIN_LOAD_THREAD, deviceType, fileType, null).sendToTarget();
            }
            super.onChange(selfChange);
        }
    }
    
    public void reLoadAllMedia(int fileType) {
        for (int deviceType : DBConfig.sScan3zaDefaultList) {
            if (AllMediaList.instance(mContext).getStoragBean(deviceType).isMounted()) {
                DebugLog.d(TAG, "reLoadAllMedia");
                mLocalHandler.obtainMessage(BEGIN_LOAD_THREAD, deviceType, fileType, null).sendToTarget();
            }
        }
    }

    public StorageBean getStoragBean(int deviceType) {
        return getStoragBean(MediaUtil.getDevicePath(deviceType));
    }
    
    public StorageBean getStoragBean(String devicePath) {
        StorageBean storageBean = null;
        storageBean = mScanStateHash.get(devicePath);
        if (storageBean == null) {
            storageBean = new StorageBean(devicePath, 
                    MediaUtil.checkMounted(mContext, devicePath) ? StorageBean.MOUNTED : StorageBean.EJECT);
            mScanStateHash.put(devicePath, storageBean);
        }
        if (MediaUtil.DEVICE_PATH_COLLECT.equals(devicePath)) {
             storageBean.update(StorageBean.ID3_PARSE_COMPLETED);
        }
        return storageBean;
    }
    
    public void updateStorageBean(String devicePath, int state) {
        StorageBean storageBean = new StorageBean(devicePath, state);
        mScanStateHash.put(devicePath, storageBean);
        mLocalHandler.sendMessage(mLocalHandler.obtainMessage(SCAN_STATE_CHANGE, storageBean));
    }
    
    public int getLastDeviceType() {
        return PlayStateSharedPreferences.instance(mContext).getLastDeviceType();
    }
    
    public FileNode getPlayState(int deviceType, int fileType) {
        FileNode fileNode = null;
        String valueStr = PlayStateSharedPreferences.instance(mContext).getPlayState(deviceType, fileType);
        ArrayList<FileNode> list = getMediaList(deviceType, fileType);
        if (list.size() > 0) {
            if ("".equals(valueStr)) {
                //fileNode = getMediaList(deviceType, fileType).get(0);
            } else {
                String splitStr = PlayStateSharedPreferences.SPLIT_STR;
                String filePath = valueStr.substring(0, valueStr.indexOf(splitStr));
                String playTime = valueStr.substring(valueStr.indexOf(splitStr) + splitStr.length(), valueStr.length());
                for (FileNode node : list) {
                    if (node.getFilePath().equals(filePath)) {
                        fileNode = node;
                        fileNode.setPlayTime(Integer.valueOf(playTime));
                        break;
                    }
                }
            }
        }
        return fileNode;
    }
    
    public void savePlayState(FileNode fileNode, int playTime) {
        if (fileNode != null) {
            fileNode.setPlayTime(playTime);
            PlayStateSharedPreferences.instance(mContext).savePlayState(fileNode);
        }
    }
    
    public void clearPlayState(int deviceType, int fileType) {
        PlayStateSharedPreferences.instance(mContext).clearPlayState(deviceType, fileType);
    }
    
    public void savePlayMode(int playMode) {
        PlayStateSharedPreferences.instance(mContext).savePlayMode(playMode);
    }
    
    public int getPlayMode() {
        return PlayStateSharedPreferences.instance(mContext).getPlayMode();
    }
    
    /**
     * 拷贝操作，针对FileNode对象。
     */
    public void copyToLocal(FileNode fileNode, OperateListener listener) {
        ArrayList<FileNode> dataList = new ArrayList<FileNode>();
        dataList.add(fileNode);
        copyToLocal(dataList, listener);
    }
    
    /**
     * 拷贝操作，针对集合对象。
     */
    public void copyToLocal(ArrayList<FileNode> dataList, OperateListener listener) {
        mLocalHandler.sendMessage(mLocalHandler.obtainMessage(BEGIN_OPERATE_THREAD,
                new OperateData(OperateListener.OPERATE_COPY_TO_LOCAL, dataList, listener)));
    }
    
    /**
     * 主要用于取消复制的操作。
     */
    public void stopOperateThread() {
        if (mOperateThread != null) {
            mOperateThread.interrupt();
        }
    }
    
    /**
     * 删除操作，针对FileNode对象。
     */
    public void deleteMediaFile(FileNode fileNode, OperateListener listener) {
        ArrayList<FileNode> dataList = new ArrayList<FileNode>();
        dataList.add(fileNode);
        deleteMediaFiles(dataList, listener);
    }
    
    /**
     * 删除操作，针对集合对象。
     */
    public void deleteMediaFiles(ArrayList<FileNode> dataList, OperateListener listener) {
        mLocalHandler.sendMessage(mLocalHandler.obtainMessage(BEGIN_OPERATE_THREAD,
                new OperateData(OperateListener.OPERATE_DELETE, dataList, listener)));
    }
    
    /**
     * 收藏操作，针对FileNode对象。
     */
    public void collectMediaFile(FileNode fileNode, OperateListener listener) {
        ArrayList<FileNode> dataList = new ArrayList<FileNode>();
        dataList.add(fileNode);
        collectMediaFiles(dataList, listener);
    }
    
    /**
     * 收藏操作，针对集合对象。
     */
    public void collectMediaFiles(ArrayList<FileNode> dataList, OperateListener listener) {
        mLocalHandler.sendMessage(mLocalHandler.obtainMessage(BEGIN_OPERATE_THREAD,
                new OperateData(OperateListener.OPERATE_COLLECT, dataList, listener)));
    }
    
    /**
     * 取消收藏操作，针对FileNode对象。
     */
    public void uncollectMediaFile(FileNode fileNode, OperateListener listener) {
        ArrayList<FileNode> dataList = new ArrayList<FileNode>();
        dataList.add(fileNode);
        uncollectMediaFiles(dataList, listener);
    }
    
    /**
     * 取消收藏操作，针对集合对象。
     */
    public void uncollectMediaFiles(ArrayList<FileNode> dataList, OperateListener listener) {
        mLocalHandler.sendMessage(mLocalHandler.obtainMessage(BEGIN_OPERATE_THREAD,
                new OperateData(OperateListener.OPERATE_UNCOLLECT, dataList, listener)));
    }
    
    /**
     * 搜索音乐的接口。
     * @param searchStr
     * @param listener
     */
    public void searchMusic(String searchStr, SearchListener listener) {
        // 这个查询不能频繁调用。
        searchMedia(FileType.AUDIO, searchStr, listener);
    }
    
    public void searchImage(String searchStr, SearchListener listener) {
        // 这个查询不能频繁调用。
        searchMedia(FileType.IMAGE, searchStr, listener);
    }
    
    public void searchVideo(String searchStr, SearchListener listener) {
        // 这个查询不能频繁调用。
        searchMedia(FileType.VIDEO, searchStr, listener);
    }
    
    private void searchMedia(final int fileType, final String searchStr, final SearchListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<FileNode> dataList = mMediaDbHelper.searchMedia(fileType, searchStr);
                mLocalHandler.obtainMessage(NOTIFY_SEARCH_BACKCALL, new SearchData(dataList, listener)).sendToTarget();
            }
        }).start();
    }
    
    class SearchData {
        ArrayList<FileNode> dataList;
        SearchListener listener;
        public SearchData(ArrayList<FileNode> dataList, SearchListener listener) {
            this.dataList = dataList;
            this.listener = listener;
        }
    }
    
    class OperateData {
        int operateValue;
        ArrayList<FileNode> dataList;
        OperateListener listener;
        public OperateData(int operateValue, ArrayList<FileNode> dataList, OperateListener listener) {
            this.operateValue = operateValue;
            this.dataList = dataList;
            this.listener = listener;
        }
    }
    
    class OperateThread extends Thread {
        List<OperateData> mOperateList = Collections.synchronizedList(new ArrayList<OperateData>());
        volatile boolean isRunning;
        
        public void addToListAndStart(OperateData operateData) {
            mOperateList.add(operateData);
            if (!isRunning) {
                isRunning = true; // 防止非常快速地调用两次addToListAndStart（来不及调用run方法）。
                try {
                    start();
                } catch (Exception e) {
                }
            }
        }
        @Override
        public void run() {
            while (mOperateList.size() > 0) {
                OperateData operateData = mOperateList.remove(0);
                //1删除文件； 2收藏文件； 3取消收藏文件； 可查看OperateListener中的常量说明。
                switch (operateData.operateValue) {
                    case OperateListener.OPERATE_DELETE:
                        deleteMediaFiles(operateData.dataList, operateData, this);
                        break;
                    case OperateListener.OPERATE_COLLECT:
                        collectMediaFiles(operateData.dataList, operateData);
                        break;
                    case OperateListener.OPERATE_UNCOLLECT:
                        unCollectMediaFiles(operateData.dataList, operateData);
                        break;
                    case OperateListener.OPERATE_COPY_TO_LOCAL:
                        if (operateData.dataList.get(0).getFileType() == FileType.IMAGE) {
                            copyToLocal(operateData.dataList, operateData, this);
                        } else {
                            copyToLocalForFileSize(operateData.dataList, operateData, this);
                        }
                        break;
                    default:
                        break;
                }
                mLocalHandler.sendMessage(mLocalHandler.obtainMessage(ITEM_OPERATE_COMPLETED,
                        100, OperateListener.OPERATE_SUCEESS, operateData));
            }
            isRunning = false;
            mLocalHandler.sendEmptyMessage(RELEASE_OPERATE_THREAD);
        }
    }
    
    private void deleteMediaFiles(ArrayList<FileNode> list, OperateData operateData, Thread thread) {
        int currentprogress = 0;
        mMediaDbHelper.setStartFlag(true);
        for (FileNode fileNode : list) {
            if (thread.isInterrupted()) {
                break;
            }
            int resultCode = OperateListener.OPERATE_SUCEESS;
            File file = fileNode.getFile();
            if (file.exists()) {
                if (file.canWrite()) {
                    if (file.delete()) {
                        resultCode = OperateListener.OPERATE_SUCEESS;
                    } else {
                        resultCode = OperateListener.OPERATE_DELETE_ERROR;
                    }
                } else {
                    resultCode = OperateListener.OPERATE_DELETE_READ_ONLY;
                }
            } else {
                resultCode = OperateListener.OPERATE_DELETE_NOT_EXIST;
            }
            if (resultCode == OperateListener.OPERATE_SUCEESS ||
                    resultCode == OperateListener.OPERATE_DELETE_NOT_EXIST) {
                // 如果这条记录已经收藏，删除对应的收藏表的数据。
                if (fileNode.getCollect() == 1) {
                    FileNode collectFileNode = new FileNode(fileNode);
                    collectFileNode.setDeviceType(DeviceType.COLLECT);
                    mMediaDbHelper.addToNeedToInsertList(new TransactionTask(collectFileNode, TransactionTask.DELETE_TASK));
                }
                // 删除媒体表中的数据。
                mMediaDbHelper.addToNeedToInsertList(new TransactionTask(fileNode, TransactionTask.DELETE_TASK));
                mLocalHandler.obtainMessage(DELETE_FILENODE_FROM_LIST, fileNode).sendToTarget();
            }
            
            mLocalHandler.sendMessage(mLocalHandler.obtainMessage(NOTIFY_LIST_ITEM_PROGRESS,
                    (currentprogress * 100) / list.size(), resultCode, operateData));
            currentprogress++;
        }
        mMediaDbHelper.setStartFlag(false);
    }
    
    private void collectMediaFiles(ArrayList<FileNode> list, OperateData operateData) {
        int currentprogress = 0;
        mMediaDbHelper.setStartFlag(true);
        for (FileNode fileNode : list) {
            int resultCode = OperateListener.OPERATE_SUCEESS;
            fileNode.setCollect(1);
            fileNode.setCollectPath(fileNode.getFilePath());
            mMediaDbHelper.addToNeedToInsertList(new TransactionTask(fileNode, TransactionTask.UPDATE_TASK)); // 先更新 对应的媒体表中的数据。
            
            FileNode collectFileNode = new FileNode(fileNode);
            collectFileNode.setDeviceType(DeviceType.COLLECT); // 更新收藏对应的DeviceType
            mMediaDbHelper.addToNeedToInsertList(new TransactionTask(collectFileNode, TransactionTask.INSERT_TASK)); // 后更新 到对应的收藏表中
            currentprogress++;
            mLocalHandler.sendMessage(mLocalHandler.obtainMessage(NOTIFY_LIST_ITEM_PROGRESS,
                    (currentprogress * 100) / list.size(), resultCode, operateData));
        }
        mMediaDbHelper.setStartFlag(false);
    }
    
    private void unCollectMediaFiles(ArrayList<FileNode> list, OperateData operateData) {
        int currentprogress = 0;
        mMediaDbHelper.setStartFlag(true);
        for (FileNode fileNode : list) {
            int resultCode = OperateListener.OPERATE_SUCEESS;
            if (!fileNode.isFromCollectTable()) { // 来自媒体表。
                fileNode.setUnCollect();
                mMediaDbHelper.addToNeedToInsertList(new TransactionTask(fileNode, TransactionTask.UPDATE_TASK));
                // 然后再删除收藏表中的数据
                FileNode collectFileNode = new FileNode(fileNode);
                collectFileNode.setDeviceType(DeviceType.COLLECT);
                mMediaDbHelper.addToNeedToInsertList(new TransactionTask(collectFileNode, TransactionTask.DELETE_TASK));
            } else { // 来自收藏表。
                mMediaDbHelper.addToNeedToInsertList(new TransactionTask(fileNode, TransactionTask.DELETE_TASK));
                FileNode srcFileNode = new FileNode(fileNode);
                srcFileNode.setUnCollect();
                srcFileNode.setDeviceType(MediaUtil.getDeviceType(fileNode.getFilePath()));
                mMediaDbHelper.addToNeedToInsertList(new TransactionTask(srcFileNode, TransactionTask.UPDATE_TASK));
            }
            
            currentprogress++;
            mLocalHandler.sendMessage(mLocalHandler.obtainMessage(NOTIFY_LIST_ITEM_PROGRESS,
                    (currentprogress * 100) / list.size(), resultCode, operateData));
        }
        mMediaDbHelper.setStartFlag(false);
    }
    
    private void copyToLocal(ArrayList<FileNode> list, OperateData operateData, Thread thread) {
        int currentprogress = 0;
        mMediaDbHelper.setStartFlag(true);
        for (FileNode fileNode : list) {
            if (thread.isInterrupted()) {
                break;
            }
            int resultCode = OperateListener.OPERATE_SUCEESS;
            String destFilePath = MediaUtil.LOCAL_COPY_DIR + "/" +
                    fileNode.getFilePath().substring(fileNode.getFilePath().lastIndexOf('/') + 1);
            if (MediaUtil.pasteFileByte(thread, fileNode.getFile(), new File(destFilePath),
                    MediaUtil.LOCAL_COPY_DIR)) { // 文件拷贝成功。
                mMediaDbHelper.addToNeedToInsertList(new TransactionTask(new FileNode(destFilePath),
                        TransactionTask.DELETE_TASK));
                mMediaDbHelper.addToNeedToInsertList(new TransactionTask(new FileNode(destFilePath),
                        TransactionTask.INSERT_TASK)); // 后更新 到对应的收藏表中
            } else { // 文件拷贝失败。
                resultCode = OperateListener.OPERATE_COLLECT_COPY_FILE_FAILED;
                if (thread.isInterrupted()) {
                    resultCode = OperateListener.OPERATE_SUCEESS;
                }
            }
            currentprogress++;
            mLocalHandler.sendMessage(mLocalHandler.obtainMessage(NOTIFY_LIST_ITEM_PROGRESS,
                    (currentprogress * 100) / list.size(), resultCode, operateData));
            if (resultCode != OperateListener.OPERATE_SUCEESS) {
                break;
            }
        }
        mMediaDbHelper.setStartFlag(false);
    }
    
    private void copyToLocalForFileSize(ArrayList<FileNode> list, OperateData operateData, Thread thread) {
    	int resultCode = OperateListener.OPERATE_SUCEESS;
        int currentprogress = 0;
        mMediaDbHelper.setStartFlag(true);
        long totalSize = 0;
        long docopySize = 0;
        for (FileNode fileNode : list) {
            totalSize += fileNode.getFile().length();
        }
        for (FileNode fileNode : list) {
            if (thread.isInterrupted()) {
                break;
            }
            String destFilePath = MediaUtil.LOCAL_COPY_DIR + "/" + fileNode.getFileName();
            boolean ret = true;
            File file = new File(MediaUtil.LOCAL_COPY_DIR);
            if (file != null && !file.exists()) {
                if (!file.mkdirs()) {
                    Log.w("Yearlay", "mkdir collect path failed");
                }
            }
            File srcfile = fileNode.getFile();
            File tarFile = new File(destFilePath);
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
                    docopySize += length;
                    
                    int progress = (int) (docopySize  * 100 / totalSize);
                    if (progress != currentprogress && progress < 100) {
                        currentprogress = progress;
                        mLocalHandler.sendMessage(mLocalHandler.obtainMessage(NOTIFY_LIST_ITEM_PROGRESS,
                                currentprogress, resultCode, operateData));
                    }
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
            if (ret) { // 文件拷贝成功。
                mMediaDbHelper.addToNeedToInsertList(new TransactionTask(new FileNode(destFilePath),
                        TransactionTask.DELETE_TASK));
                mMediaDbHelper.addToNeedToInsertList(new TransactionTask(new FileNode(destFilePath),
                        TransactionTask.INSERT_TASK)); // 后更新 到对应的收藏表中
            } else { // 文件拷贝失败。
                resultCode = OperateListener.OPERATE_COLLECT_COPY_FILE_FAILED;
                if (thread.isInterrupted()) {
                    resultCode = OperateListener.OPERATE_SUCEESS;
                }
            }
            if (resultCode != OperateListener.OPERATE_SUCEESS) {
                break;
            }
        }
        currentprogress = 100;
        mLocalHandler.sendMessage(mLocalHandler.obtainMessage(NOTIFY_LIST_ITEM_PROGRESS,
                currentprogress, resultCode, operateData));
        mMediaDbHelper.setStartFlag(false);
    }
    
    public static void notifyAllLabelChange(Context context, int res) {
        String title = context.getResources().getString(res);
        DebugLog.d(TAG, "notifyAllLabelChange title="+title);
        Intent intent = new Intent("action.topstack.appname"); 
        intent.putExtra("data",title);
        context.sendBroadcast(intent); 
    }
    
    public static void notifyUpdateAppWidget() {
        try {
            Handler handler = MediaService.getInstance().getHandler();
            if (!handler.hasMessages(MediaService.MSG_UPDATE_APPWIDGET)) {
                Log.d(TAG, "notifyUpdateAppWidget sendBroadcast main_activity_update_ui");
                handler.sendEmptyMessageDelayed(MediaService.MSG_UPDATE_APPWIDGET, 200);
            }
        } catch (Exception e) {
            MediaApplication.getInstance().sendBroadcast(new Intent("main_activity_update_ui"));
        }
    }
    
    public void deleteOldCollect(final int fileType) {
        ArrayList<FileNode> mediaList = null;
        String tableName = DBConfig.getTableName(DeviceType.COLLECT, fileType);
        mediaList = mAllMediaHash.get(tableName);
        if (mediaList != null && mediaList.size() >= MediaUtil.COLLECT_COUNT_MAX) {
            FileNode oldFileNode = mediaList.get(0);
            for (int i = 1; i < mediaList.size(); i++) {
                FileNode fileNode = mediaList.get(i);
                if (!fileNode.getFile().exists()) {
                    oldFileNode = fileNode;
                    break;
                }
                if (fileNode.getId() < oldFileNode.getId()) {
                    oldFileNode = fileNode;
                }
            }
            uncollectMediaFile(oldFileNode, new OperateListener() {
                @Override
                public void onOperateCompleted(int operateValue, int progress, int resultCode) {
                    AllMediaList.instance(mContext).reLoadAllMedia(fileType);
                }
            });
        }
    }
    
    public static boolean checkSelected(Context context, ArrayList<FileNode> list) {
        boolean existSelectFlag = false;
        for (FileNode fileNode : list) {
            if (fileNode.isSelected()) {
                existSelectFlag = true;
                break;
            }
        }
        return existSelectFlag;
    }
}
