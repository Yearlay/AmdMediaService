package com.haoke.data;

import com.haoke.bean.StorageBean;

public interface LoadListener {
	public void onLoadCompleted(int deviceType, int fileType);
	public void onScanStateChange(StorageBean storageBean);
}
