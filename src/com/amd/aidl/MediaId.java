package com.amd.aidl;

public class MediaId {
	// 无
	public static final int ID_NULL = 0x0000;
	
	// 媒体框标题
	public static final int ID_MEDIA_LABEL = 0x0001;
	
	// 音乐专辑图
	public static final int ID_MUSIC_ALBUM_BMP = 0x0002;
	// 歌曲名
	public static final int ID_MUSIC_TITLE = 0x0004;
	// 歌曲播放状态
	public static final int ID_MUSIC_PLAY_STATE = 0x0008;
	
	// 收音波段
	public static final int ID_RADIO_BAND = 0x0010;
	// 收音播放状态
	public static final int ID_RADIO_ENABLE_STATE = 0x0020;
	// 收音频率
	public static final int ID_RADIO_FREQ = 0x0040;
	// 收音频率单位
	public static final int ID_RADIO_FREQ_UNIT = 0x0080;
	// 收音立体声标记
	public static final int ID_RADIO_ST = 0x0100;
	
	// 音乐框id3信息
	public static final int ID_MUSIC_ID3 = 
			ID_MUSIC_ALBUM_BMP | ID_MUSIC_TITLE;
	// 音乐框
	public static final int ID_MUSIC_ALL = 
			ID_MUSIC_ID3 | ID_MUSIC_PLAY_STATE | ID_MEDIA_LABEL;
	// 收音框
	public static final int ID_RADIO_ALL = 
			ID_RADIO_BAND | ID_RADIO_ENABLE_STATE | ID_RADIO_FREQ 
			| ID_RADIO_FREQ_UNIT | ID_RADIO_ST | ID_MEDIA_LABEL;
	// 所有界面
	public static final int ID_ALL = ID_MUSIC_ALL | ID_RADIO_ALL;
}