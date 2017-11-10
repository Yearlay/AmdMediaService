package com.amd.aidl;

import com.amd.aidl.IAmdMediaCallBack;

interface IAmdMediaService {

	boolean registerCallBack(int mode, IAmdMediaCallBack callBack);
	boolean unregisterCallBack(int mode);

	boolean clickMusicPlay();
	boolean clickMusicPre();
	boolean clickMusicNext();
	boolean clickRadioEnable();
	boolean clickOther(String tag);
	
	String getMediaLabel();
	
	boolean isPlayingMusic();
	Bitmap getMusicId3AlbumBmp();
	String getMusicId3Info();
	
	boolean isEnableRadio();
	String getRadioBand();
	String getRadioFreq();
	String getRadioFreqUnit();
	boolean isRadioST();
	
	String getOtherInfo(String tag);
}
