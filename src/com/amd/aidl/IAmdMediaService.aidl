package com.amd.aidl;

import com.amd.aidl.IAmdMediaCallBack;

interface IAmdMediaService {

	boolean registerCallBack(String mode, IAmdMediaCallBack callBack);
	boolean unregisterCallBack(String mode);

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
