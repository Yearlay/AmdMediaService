
package com.haoke.scanner;

import java.util.ArrayList;

import android.content.Intent;

import com.amd.util.AmdConfig;
import com.haoke.bean.FileNode;
import com.haoke.bean.StorageBean;
import com.haoke.constant.DBConfig;
import com.haoke.constant.MediaUtil;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.constant.MediaUtil.ScanState;
import com.haoke.data.AllMediaList;
import com.haoke.util.DebugClock;
import com.haoke.util.DebugLog;

// 设备拔出时，必须停止该线程
public class ID3ParseThread extends Thread {
    private static final String TAG = "ID3ParseThread";

    private MediaScannerListner mScannerListner;
    private MediaDbHelper mMediaDbHelper;
    
    public void changeScanState(int scanState, int deviceType) {
        if (mScannerListner != null) {
            mScannerListner.scanPath(scanState, deviceType);
        }
    }

    public ID3ParseThread(MediaScannerListner scannerListner,
            MediaDbHelper mediaDbHelper) {
        mScannerListner = scannerListner;
        mMediaDbHelper = mediaDbHelper;
    }

    public void run() {
        DebugClock debugClock = new DebugClock();
        changeScanState(ScanState.ID3_PARSING, -1);
        // 收藏表，需要校验一下。
        mMediaDbHelper.setStartFlag(true);
        mMediaDbHelper.updateCollectInfoByFileExist(FileType.AUDIO);
        mMediaDbHelper.updateCollectInfoByFileExist(FileType.VIDEO);
        mMediaDbHelper.updateCollectInfoByFileExist(FileType.IMAGE);
        mMediaDbHelper.setStartFlag(false);
        // 需要针对收藏表来检验对应的媒体表。
        mMediaDbHelper.setStartFlag(true);
        mMediaDbHelper.updateMediaInfoAccordingToCollect(FileType.AUDIO); // 参照"table_audio19"表更新对应的媒体表。
        mMediaDbHelper.updateMediaInfoAccordingToCollect(FileType.VIDEO); // 参照"table_video19"表更新对应的媒体表。
        mMediaDbHelper.updateMediaInfoAccordingToCollect(FileType.IMAGE); // 参照"table_image19"表更新对应的媒体表。
        mMediaDbHelper.setStartFlag(false);
        
        AllMediaList allMediaList = AllMediaList.instance(mMediaDbHelper.getContext());
        // 图片不需要进行ID3解析。
        for (int deviceType : DBConfig.sScan3zaDefaultList) {
            String devicePath = MediaUtil.getDevicePath(deviceType);
            StorageBean storageBean = allMediaList.getStoragBean(devicePath);
            if (storageBean.isMounted()) {
                allMediaList.updateStorageBean(devicePath, StorageBean.ID3_PARSE_COMPLETED);
            }
        }

        if (!isInterrupted()) {
            changeScanState(ScanState.ID3_PARSE_COMPLETED, -1);
            changeScanState(ScanState.COMPLETED_ALL, -1);
        } else {
            DebugLog.i(TAG, "ID3ParseThread is interrupted !!!");
        }
        // 通知AllMediaList来更新一下收藏的列表。
        mMediaDbHelper.notifyCollectChange();
        // 通知更新一下APPWidget。
        debugClock.calculateTime(TAG, getClass().getName()+"#id3_update");
        
        if (AmdConfig.SCAN_OVER_LAUNCHER_PARSE_AUDIO_ID3_INFO) {
            parseId3InfoOfAudio();
        }
    }
    
    public void parseId3InfoOfAudio() {
        DebugClock debugClock = new DebugClock();
        AllMediaList allMediaList = AllMediaList.instance(mMediaDbHelper.getContext());
        int deviceSize = DBConfig.sScan3zaDefaultList.size();
        for (int index = 0; index < deviceSize; index++ ) {
            int devicetype = DBConfig.sScan3zaDefaultList.get(index);
            if (Thread.interrupted()) { // 线程中断的话，就直接return；
                debugClock.calculateTime(TAG, "Thread.interrupted! parseId3InfoOfAudio interrupted!");
                return;
            }
            if (allMediaList.getStoragBean(devicetype).isMounted()) {
                ArrayList<FileNode> mediaList = allMediaList.getMediaList(devicetype, FileType.AUDIO);
                try {
                    for (FileNode fileNode : mediaList) {
                        if (Thread.interrupted()) { // 线程中断的话，就直接return；
                            debugClock.calculateTime(TAG, "Thread.interrupted! parseId3InfoOfAudio interrupted!");
                            return;
                        }
                        if (fileNode.getParseId3() == 0) {
                            fileNode.parseID3Info();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                DebugLog.e(TAG, "parseId3InfoOfAudio error. device not mounted : " + devicetype);
            }
        }
        debugClock.calculateTime(TAG, "parseId3InfoOfAudio over!");
        
        Intent intent = new Intent("com.haoke.scanner.id3parse.over");
        String[] storagesStr = new String[3];
        storagesStr[0] = MediaUtil.sSdcardMountedEndToID3Over ? MediaUtil.LOCAL_COPY_DIR : null;
        storagesStr[1] = MediaUtil.sUSB1MountedEndToID3Over ? MediaUtil.DEVICE_PATH_USB_1 : null;
        storagesStr[2] = MediaUtil.sUSB2MountedEndToID3Over ? MediaUtil.DEVICE_PATH_USB_2 : null;
        intent.putExtra("storages", storagesStr);
        mMediaDbHelper.getContext().sendBroadcast(intent);
        MediaUtil.resetAllMountedEndToID3Over();
    }
}
