
package com.haoke.scanner;

import java.io.File;

import com.haoke.bean.StorageBean;
import com.haoke.constant.DBConfig;
import com.haoke.constant.MediaUtil;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.constant.MediaUtil.ScanState;
import com.haoke.constant.MediaUtil.ScanTaskType;
import com.haoke.data.AllMediaList;
import com.haoke.service.MediaService;
import com.haoke.util.DebugLog;

import android.content.Context;

public class MediaScanner {
    private final String TAG = "MediaScanner";
    private Context mContext;
    private MediaScannerListner mScannerListner;

    private int mDeviceType = DeviceType.NULL;
    public int getDeviceType() {
        return mDeviceType;
    }

    private ScanRootPathThread mScanRootPathThread;
    private ID3ParseThread mID3ParseThread;
    
    public MediaScanner(Context context, MediaScannerListner listener) {
        mContext = context;
        mScannerListner = listener;
    }
    
    public ScanRootPathThread getScanRootPathThread() {
        if (mScanRootPathThread == null) {
            DebugLog.i(TAG, "mScanRootPathThread is null and new ScanRootPathThread!");
            mScanRootPathThread = new ScanRootPathThread(mScannerListner,
                    MediaDbHelper.instance(mContext.getApplicationContext()));
            mScanRootPathThread.setPriority(Thread.MIN_PRIORITY);
        }
        return mScanRootPathThread;
    }
    
    /**
     * 开始扫描所有的存储(3za默认的有：USB1，USB2，FLASH)。
     */
    public void beginScanningAllStorage() {
        for (int deviceType : DBConfig.sScan3zaDefaultList) {
            String devicePath = MediaUtil.getDevicePath(deviceType);
            if (MediaUtil.checkMounted(mContext, MediaUtil.getDevicePath(deviceType)) &&
                    AllMediaList.instance(mContext).getStoragBean(deviceType).isScanIdle()) {
                DebugLog.d(TAG, "Begin scaning deviceType : " + deviceType);
                AllMediaList.instance(mContext).updateStorageBean(devicePath, StorageBean.MOUNTED);
                beginScanningStorage(MediaUtil.getDevicePath(deviceType));
            } else {
                DebugLog.e(TAG, "Error, Not mounted, device path: " + MediaUtil.getDevicePath(deviceType));
            }
        }
    }

    /**
     * 针对某个磁盘进行扫描操作。
     * @param rootPath
     */
    public void beginScanningStorage(String rootPath) {
        DebugLog.i(TAG, "MediaScanner#beginScanningStorage rootPath: " + rootPath);
        if ("/storage/internal_sd".equals(rootPath)) {
            rootPath = MediaUtil.LOCAL_COPY_DIR;
        }
        interruptID3ParseThread();
        getScanRootPathThread().addDeviceTask(ScanTaskType.MOUNTED, rootPath);
    }
    
    /**
     * 设备拔出的时候，调用这个函数清空该设备的数据库记录。
     * @param devicePath
     */
    public void removeStorage(String devicePath) {
        interruptID3ParseThread();
        getScanRootPathThread().interruptTask(devicePath);
        getScanRootPathThread().addDeviceTask(ScanTaskType.UNMOUNTED, devicePath);
        if (mScannerListner != null) {
            mScannerListner.scanPath(ScanState.REMOVE_STORAGE, MediaUtil.getDeviceType(devicePath));
        }
    }
    
    /**
     * 当所有的文件扫描操作结束之后，触发ID3扫描的线程。
     */
    public void beginID3ParseThread() {
        mScanRootPathThread = null;
        interruptID3ParseThread();
        mID3ParseThread = new ID3ParseThread(mScannerListner,
                MediaDbHelper.instance(mContext.getApplicationContext()));
        mID3ParseThread.setPriority(Thread.MIN_PRIORITY);
        mID3ParseThread.start();
    }
    
    private void interruptID3ParseThread() {
        if (mID3ParseThread != null && mID3ParseThread.isAlive()) {
            DebugLog.i(TAG, "interruptID3ParseThread");
            mID3ParseThread.interrupt();
        }
    }
}
