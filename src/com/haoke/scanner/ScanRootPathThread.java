
package com.haoke.scanner;

import java.util.ArrayList;

import com.file.server.scan.ScanJni;
import com.haoke.bean.StorageBean;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.constant.MediaUtil.ScanState;
import com.haoke.constant.MediaUtil.ScanTask;
import com.haoke.constant.MediaUtil.ScanTaskType;
import com.haoke.data.AllMediaList;
import com.haoke.util.DebugClock;
import com.haoke.util.DebugLog;

// 设备拔出时，必须停止该线程
public class ScanRootPathThread extends Thread {
    private static final String TAG = "FileServer";

    private MediaScannerListner mScannerListner;
    private MediaDbHelper mMediaDbHelper;
    private int mFileCount;
    private int mMediaCount;
    private long mInsertDbTime;
    
    public void changeScanState(int scanState, int deviceType) {
        if (mScannerListner != null) {
            mScannerListner.scanPath(scanState, deviceType);
        }
    }
    
    private ArrayList<ScanTask> mDeviceTaskList = new ArrayList<ScanTask>();

    public void addDeviceTask(int taskType, String filePath) {
        if (mDeviceTaskList.size() > 0) {
            for (ScanTask deviceTask : mDeviceTaskList) {
                if (deviceTask.mTaskType == taskType &&
                        deviceTask.mFilePath.equals(filePath)) {
                    return;
                }
            }
        }
        DebugLog.i(TAG, "ScanRootPathThread#addDeviceTask filePath: " + filePath);
        mDeviceTaskList.add(new ScanTask(taskType, filePath));
        if (!isAlive()) {
            try {
                start();
            } catch (Exception e) {
            }
        } else {
            DebugLog.i(TAG, "ScanRootPathThread#addDeviceTask running, TaskList size: " + mDeviceTaskList.size());
        }
    }
    private ScanTask mCurrentDeviceTask;

    public ScanTask getCurrentDeviceTask() {
        return mCurrentDeviceTask;
    }

    public ScanRootPathThread(MediaScannerListner scannerListner,
            MediaDbHelper mediaDbHelper) {
        mScannerListner = scannerListner;
        mMediaDbHelper = mediaDbHelper;
    }

    public void run() {
        while (mDeviceTaskList.size() > 0) {
            doDeviceTasks();
        }
        changeScanState(ScanState.SCAN_THREAD_OVER, -1);
    }
    
    private void doDeviceTasks() {
        while (mDeviceTaskList.size() > 0) {
            mCurrentDeviceTask = mDeviceTaskList.get(0);
            DebugLog.i(TAG, "Begin doDeviceTasks# mTaskType: " + mCurrentDeviceTask.mTaskType
                    + " && mFilePath: " + mCurrentDeviceTask.mFilePath);
            switch (mCurrentDeviceTask.mTaskType) {
                case ScanTaskType.MOUNTED:
                    scanStorage(mCurrentDeviceTask);
                    break;
                case ScanTaskType.UNMOUNTED:
                    removeStorage(mCurrentDeviceTask);
                    break;
                case ScanTaskType.DIRECTORY:
                case ScanTaskType.FILE:
                    jniScanRootPath(mCurrentDeviceTask.mFilePath, true);
                    break;
                default:
                    break;
            }
            DebugLog.i(TAG, "End doDeviceTasks# mTaskType: " + mCurrentDeviceTask.mTaskType
                    + " && mFilePath: " + mCurrentDeviceTask.mFilePath);
            mDeviceTaskList.remove(mCurrentDeviceTask);
        }
        mCurrentDeviceTask = null;
    }
    
    private void scanStorage(ScanTask scanTask) {
        DebugLog.i(TAG, "scanStorage Path: " + scanTask.mFilePath);
        AllMediaList.instance(mMediaDbHelper.getContext()).updateStorageBean(scanTask.mFilePath, StorageBean.FILE_SCANNING);
        changeScanState(ScanState.SCANNING, scanTask.mDeviceType);
        int scanState = ScanState.IDLE;
        try {
            // TODO：清空数据库表的内容是临时的做法，需要针对重启设备校验文件来设计方案。
            mMediaDbHelper.clearDeviceData(scanTask.mDeviceType);
            mMediaDbHelper.setStartFlag(true);
            jniScanRootPath(scanTask.mFilePath, false);
            mMediaDbHelper.setStartFlag(false);
        } catch (Exception e) {
            scanState = ScanState.SCAN_ERROR;
            e.printStackTrace();
        }
        DebugLog.d(TAG, " Scan over, mFileCount : " + mFileCount + ", mMediaCount : " + mMediaCount);
        scanState = (scanState == ScanState.IDLE) ? ScanState.COMPLETED : scanState;
        if (scanState == ScanState.COMPLETED) {
            AllMediaList.instance(mMediaDbHelper.getContext()).updateStorageBean(scanTask.mFilePath, StorageBean.SCAN_COMPLETED);
        } else {
            DebugLog.e(TAG, "Exception scanStorage error !!!!!!!! filePath: " + scanTask.mFilePath);
            AllMediaList.instance(mMediaDbHelper.getContext()).updateStorageBean(scanTask.mFilePath, StorageBean.EJECT);
        }
        changeScanState(scanState, scanTask.mDeviceType);
    }

    private void removeStorage(ScanTask scanTask) {
        DebugClock debugClock = new DebugClock();
        // 更新磁盘表信息。
        AllMediaList.instance(mMediaDbHelper.getContext()).updateStorageBean(scanTask.mFilePath, StorageBean.EJECT);
        if (scanTask.mDeviceType != DeviceType.NULL) {
            mMediaDbHelper.clearDeviceData(scanTask.mDeviceType);
        }
        debugClock.calculateTime(TAG, getClass().getName()+"#removeStorage");
    }

    public void interruptTask(String storagePath) {
        if (mCurrentDeviceTask != null && mCurrentDeviceTask.mFilePath.equals(storagePath)) {
            mCurrentDeviceTask.mIsInterrupted = true;
        }
        if (mDeviceTaskList.size() > 0) {
            for (int i = 0; i < mDeviceTaskList.size(); i++) {
                ScanTask scanTask = mDeviceTaskList.get(i);
                if (scanTask.mFilePath.equals(storagePath)) {
                    mDeviceTaskList.remove(scanTask);
                    i--;
                }
            }
        }
    }

    private void jniScanRootPath(String filePath, boolean parseId3) {
        // TODO: 如果是目录重新扫描，应该是需要先删除与这个目录有关的数据库记录的。
        DebugClock debugClock = new DebugClock();
        ScanJni scanJni = new ScanJni(mMediaDbHelper, parseId3);
        scanJni.scanRootPath(filePath);
        mMediaCount = scanJni.mMediaCount;
        mFileCount = scanJni.mFileCount;
        debugClock.calculateTime(TAG, getClass().getName()+"#jniScanRootPath" + " InsertDbTime: " + mInsertDbTime );
    }
}
