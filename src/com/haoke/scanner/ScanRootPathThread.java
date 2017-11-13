
package com.haoke.scanner;

import java.util.ArrayList;

import com.file.server.scan.ScanJni;
import com.haoke.bean.StorageBean;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.constant.MediaUtil.ScanState;
import com.haoke.constant.MediaUtil.ScanTask;
import com.haoke.constant.MediaUtil.ScanTaskType;
import com.haoke.data.AllMediaList;
import com.haoke.util.DebugClock;
import com.haoke.util.DebugLog;

// 设备拔出时，必须停止该线程
public class ScanRootPathThread extends Thread {
    private static final String TAG = "Yearlay";

    private MediaScannerListner mScannerListner;
    private MediaDbHelper mMediaDbHelper;
    
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
        int mediaCount = 0;
        int scanState = ScanState.IDLE;
        try {
            // TODO：清空数据库表的内容是临时的做法，需要针对重启设备校验文件来设计方案。
            int imageCount = mMediaDbHelper.queryMedia(scanTask.mDeviceType, FileType.IMAGE, null, null).size();
            int audioCount = mMediaDbHelper.queryMedia(scanTask.mDeviceType, FileType.AUDIO, null, null).size();
            int videoCount = mMediaDbHelper.queryMedia(scanTask.mDeviceType, FileType.VIDEO, null, null).size();
            DebugLog.d(TAG, " Scan check imageCount: " + imageCount
            		+ " && audioCount: " + audioCount + " && videoCount:" + videoCount);
            mediaCount = jniScanRootPath(scanTask.mFilePath, 1);
            if (mediaCount != (imageCount + audioCount + videoCount)) {
                DebugLog.d(TAG, " Scan check failed; Begin rescan !!!!!");
                mMediaDbHelper.clearDeviceData(scanTask.mDeviceType);
                mMediaDbHelper.setStartFlag(true);
                mediaCount = jniScanRootPath(scanTask.mFilePath, 0);
                mMediaDbHelper.setStartFlag(false);
            } else {
                DebugLog.d(TAG, " Scan check successful!!!");
            }
        } catch (Exception e) {
            scanState = ScanState.SCAN_ERROR;
            e.printStackTrace();
        }
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
        AllMediaList.instance(mMediaDbHelper.getContext()).updateStorageBean(scanTask.mFilePath, StorageBean.EJECT);
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

    private int jniScanRootPath(String filePath, int onlyGetMediaSizeFlag) {
        // TODO: 如果是目录重新扫描，应该是需要先删除与这个目录有关的数据库记录的。
        DebugClock debugClock = new DebugClock();
        ScanJni scanJni = new ScanJni(mMediaDbHelper);
        int count = scanJni.scanRootPath(filePath, onlyGetMediaSizeFlag);
        debugClock.calculateTime(TAG, getClass().getName()+"#jniScanRootPath onlyGetMediaSizeFlag:" + onlyGetMediaSizeFlag);
        DebugLog.d(TAG, "#jniScanRootPath count:" + count);
        return count;
    }
}
