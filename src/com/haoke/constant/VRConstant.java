package com.haoke.constant;

public class VRConstant {
	public static class VRApp {
		public static final String ACTION_APP = "com.jsbd.vr.app.action";
		public static final String KEY_FUCTION = "function";
		public static final int FUNCTION_OPEN = 1;  // 打开界面，操作也要执行。
		public static final int FUNCTION_CLOSE = 2; // 关闭界面，操作也要中止。
		public static final String KEY_OPERATOR = "value";
		public static final int OPERATOR_MUSIC = 0; // 音乐，播放当前歌曲，进入音乐播放界面。
		public static final int OPERATOR_VIDEO = 1; // 视频，播放当前视频，进入视频播放界面。
		public static final int OPERATOR_IMAGE = 2; // 图片，全屏显示图片。
		public static final int OPERATOR_BT = 3;    // 蓝牙音乐，播放蓝牙音乐，进入蓝牙音乐界面。
		public static final int OPERATOR_RADIO = 4; // 收音机，播放电台，进入收音机播放界面。
		public static final int OPERATOR_COLLECT = 22; // 收藏音乐，播放收藏歌曲，进入收藏音乐播放界面。
	}
	
	public static class VRMusic {
		public static final String ACTION_MUSIC_OPERATOR = "com.jsbd.vr.music.operation.action";
		public static final String KEY_COMMAND_CODE = "commandCode";
		public static final int COMMAND_SINGLE_MODE = 1; // 设置单曲循环，并播放。
		public static final int COMMAND_RANDOM_MODE = 2; // 设置随机循环，并播放。
		public static final int COMMAND_CIRCLE_MODE = 3; // 设置列表循环，并播放。
		public static final int COMMAND_COLLECT_MUSIC = 4; // 收藏当前播放歌曲。
		public static final int COMMAND_UNCOLLECT_MUSIC = 5; // 取消收藏当前播放歌曲。
		public static final int COMMAND_PLAY_MUSIC = 6;  // 指定歌曲（path）播放。
		public static final String KEY_MUSIC_PATH = "path";
	}
	
	public static class VRRadio {
		public static final String ACTION_RADIO_OPERATOR = "com.jsbd.vr.radio.operation.action";
		public static final String KEY_COMMAND_CODE = "commandCode";
		public static final String KEY_STATION_FREQ = "station";
		public static final int COMMAND_COLLECT_RADIO = 1;   // 收藏当前播放电台。
		public static final int COMMAND_UNCOLLECT_RADIO = 2; // 取消收藏当前播放电台。
		public static final int COMMAND_PLAY_COLLECT_RADIO = 3; // 播放收藏列表的中的电台。
		public static final int COMMAND_PLAY_PREV_RADIO = 4; // 切换到上一电台（频率小的）
		public static final int COMMAND_PLAY_NEXT_RADIO = 5; // 切换到下一电台（频率大的）
		public static final int COMMAND_SEARCH_PREV_RADIO = 6; // 从当前波段向上搜索（频率小的）
		public static final int COMMAND_SEARCH_NEXT_RADIO = 7; // 从当前波段向下搜索（频率大的）
		public static final int COMMAND_SCAN_RADIO = 8; // 扫描全波段电台
		public static final int COMMAND_PLAY_FM_STATION_RADIO = 9; // 切换到调频，如果指定某个电台，会有station参数，需要打开界面
		public static final int COMMAND_PLAY_AM_STATION_RADIO = 10; // 切换到调幅，如果指定某个电台，会有station参数，需要打开界面
		public static final int COMMAND_REFRESH_FM_RADIO = 11; // 刷新调频目录，如果当前不是调频，则先切到调频，再刷新
		public static final int COMMAND_REFRESH_AM_RADIO = 12; // 刷新调幅目录，如果当前不是调幅，则先切到调幅，再刷新
	}
	
	public static class VRImage {
		public static final String ACTION_IMAGE_OPERATOR = "com.jsbd.vr.picture.operation.action";
		public static final String KEY_COMMAND_CODE = "commandCode";
		public static final int COMMAND_PLAY_IMAGE = 1;
		public static final int COMMAND_PAUSE_IMAGE = 2;
		public static final int COMMAND_PRE_IMAGE = 3;
		public static final int COMMAND_NEXT_IMAGE = 4;
	}
	
	public static class VRVideo {
		public static final String ACTION_VIDEO_OPERATOR = "com.jsbd.vr.video.operation.action";
		public static final String KEY_COMMAND_CODE = "commandCode";
		public static final int COMMAND_PLAY_VIDEO = 1;
		public static final int COMMAND_PAUSE_VIDEO = 2;
		public static final int COMMAND_PRE_VIDEO = 3;
		public static final int COMMAND_NEXT_VIDEO = 4;
	}
	
	public static class VRIntent {
		public static final String ACTION_OPERATE_IMAGE = "com.jsbd.vr.operate.image";
		public static final String KEY_IMAGE = "operate_image";
		public static final int FINISH_IMAGE = 101;
		public static final int PLAY_IMAGE = 111;
		public static final int PAUSE_IMAGE = 112;
		public static final int PRE_IMAGE = 113;
		public static final int NEXT_IMAGE = 114;
		
		public static final String ACTION_OPERATE_VIDEO = "com.jsbd.vr.operate.video";
		public static final String KEY_VIDEO = "operate_video";
		public static final int FINISH_VIDEO = 201;
		public static final int PLAY_VIDEO = 211;
		public static final int PAUSE_VIDEO = 212;
		public static final int PRE_VIDEO = 213;
		public static final int NEXT_VIDEO = 214;
		
		public static final String ACTION_FINISH_MUSIC_RADIO = "com.jsbd.vr.finish.music_radio";
	}
	
}
