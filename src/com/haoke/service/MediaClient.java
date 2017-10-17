package com.haoke.service;

import com.haoke.aidl.IMediaCallBack;

public class MediaClient {
	
	public int mMode = 0;
	public IMediaCallBack mCallBack = null;
	
	public MediaClient(int mode, IMediaCallBack callBack) {
		mMode = mode;
		mCallBack = callBack;
	}
}
