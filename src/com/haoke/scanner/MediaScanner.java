
package com.haoke.scanner;

import com.haoke.bean.StorageBean;
import com.haoke.constant.DBConfig;
import com.haoke.constant.MediaUtil;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.constant.MediaUtil.ScanState;
import com.haoke.constant.MediaUtil.ScanTaskType;
import com.haoke.data.AllMediaList;
import com.haoke.util.DebugLog;

import android.content.Context;

public class MediaScanner {
    private final String TAG = "Yearlay";
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
            if (MediaUtil.checkMounted(mContext, devicePath, false)) { // 系统检查是否Mounted上了。
                if (AllMediaList.instance(mContext).getStoragBean(deviceType).isScanIdle()) { // AllMediaList检查是否已经扫描。
                    DebugLog.d(TAG, "Begin scaning deviceType : " + deviceType);
                    AllMediaList.instance(mContext).updateStorageBean(devicePath, StorageBean.MOUNTED);
                    beginScanningStorage(MediaUtil.getDevicePath(deviceType));
                } else {
                    DebugLog.i(TAG, "beginScanningAllStorage device is scanning or scan completed : " + devicePath);
                }
            } else {
                DebugLog.i(TAG, "beginScanningAllStorage Not mounted, device path: " + devicePath);
            }
        }
    }

    /**
     * 针对某个磁盘进行扫描操作。
     * @param rootPath
     */
    public void beginScanningStorage(String rootPath) {
        DebugLog.i(TAG, "MediaScanner#beginScanningStorage rootPath: " + rootPath);
        interruptID3ParseThread();
        if (MediaUtil.LOCAL_COPY_DIR.equals(rootPath)) {
            MediaUtil.sSdcardMountedEndToID3Over = true;
        } else if (MediaUtil.DEVICE_PATH_USB_1.equals(rootPath)) {
            MediaUtil.sUSB1MountedEndToID3Over = true;
        } else if (MediaUtil.DEVICE_PATH_USB_2.equals(rootPath)) {
            MediaUtil.sUSB2MountedEndToID3Over = true;
        }
        getScanRootPathThread().addDeviceTask(ScanTaskType.MOUNTED, rootPath);
    }
    
    /**
     * 设备拔出的时候，只需要修改。
     * @param devicePath
     */
    public void removeStorage(String devicePath) {
        DebugLog.i(TAG, "MediaScanner#removeStorage devicePath: " + devicePath);
        AllMediaList.instance(mContext).updateStorageBean(devicePath, StorageBean.EJECT);
        MediaDbHelper.instance(mContext).notifyCollectChange();
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
