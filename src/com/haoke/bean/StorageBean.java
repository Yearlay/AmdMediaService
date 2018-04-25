package com.haoke.bean;

import com.haoke.constant.MediaUtil;

public class StorageBean {
    public static final int EJECT = 0;
    public static final int MOUNTED = 1;
    public static final int FILE_SCANNING = 2;
    public static final int SCAN_COMPLETED = 3;
    public static final int ID3_PARSING = 4;
    public static final int ID3_PARSE_COMPLETED = 5;
    private int mState;
    
    private String storagePath;
    private int deviceType;
    private int onlyRead;
    
    public StorageBean(String path, int state) {
        storagePath = path;
        deviceType = MediaUtil.getDeviceType(path);
        mState = state;
    }
    
    public void update(int state) {
        mState = state;
    }
    
    public int getState() {
        return mState;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
        deviceType = MediaUtil.getDeviceType(storagePath);
    }

    public int getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(int deviceType) {
        this.deviceType = deviceType;
    }

    public int getOnlyRead() {
        return onlyRead;
    }

    public void setOnlyRead(int onlyRead) {
        this.onlyRead = onlyRead;
    }
    
    public boolean isUnmounted() {
        return mState == EJECT;
    }
    
    public boolean isMounted() {
        if (storagePath == null) {
            return false;
        } else {
            if (storagePath.startsWith(MediaUtil.DEVICE_PATH_FLASH)) {
                return true;
            }
        }
        return mState != EJECT;
    }
    
    public boolean isScanCompleted() {
        return mState >= SCAN_COMPLETED;
    }
    
    public boolean isId3ParseCompleted() {
        return mState == ID3_PARSE_COMPLETED;
    }
    
    public boolean isScanIdle() {
        return mState <= MOUNTED;
    }
    
    public String toString() {
        return "StorageBean: deviceType=" + deviceType + "; mState=" + mState;
    }
    
    public boolean isAllOver() {
        return (isId3ParseCompleted() && isLoadCompleted) || !isMounted();
    }
    
    boolean isLoadCompleted;

	public boolean isLoadCompleted() {
        return isLoadCompleted;
    }

    public void setLoadCompleted(boolean isLoadCompleted) {
		this.isLoadCompleted = isLoadCompleted;
	}
}
