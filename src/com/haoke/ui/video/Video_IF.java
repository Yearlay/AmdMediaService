package com.haoke.ui.video;

import android.content.ComponentName;
import android.os.RemoteException;
import android.util.Log;

import com.amd.media.AmdMediaManager;
import com.amd.media.MediaInterfaceUtil;
import com.haoke.aidl.IMediaCallBack;
import com.haoke.bean.FileNode;
import com.haoke.constant.MediaUtil.MediaState;
import com.haoke.constant.MediaUtil.PlayState;
import com.haoke.serviceif.CarService_Listener;
import com.haoke.util.Media_CallBack;
import com.haoke.util.Media_CarListener;
import com.haoke.util.Media_IF;
import com.haoke.util.Media_Listener;
import com.haoke.video.VideoSurfaceView;


class VideoManager extends AmdMediaManager {
	public VideoManager() {
		super();
		TAG = "VideoManager";
		mMediaMode = MEDIA_MODE_VIDEO;
		mComponentName = new ComponentName(mContext, VideoMediaButtonReceiver.class); 
	}
}

public class Video_IF {

	private static final String TAG = "Video_IF";
	private static Video_IF mSelf;
	private Media_CallBack mMediaCallBack = null; // MediaService的回调处理
	private IMediaCallBack.Stub mIMediaCallBack = null;
	private VideoManager mMediaManager = null;
	private boolean mVideoShow = false;

	private Video_IF() {
		mMediaManager = new VideoManager();
		mMediaCallBack = new Media_CallBack(getMode());
		mIMediaCallBack = new IMediaCallBack.Stub() {
			@Override
			public void onDataChange(int mode, int func, int data1, int data2)
					throws RemoteException {
				mMediaCallBack.onDataChange(mode, func, data1, data2);
			}
		};
	}

	// 获取接口实例
	synchronized public static Video_IF getInstance() {
		if (mSelf == null) {
			mSelf = new Video_IF();
		}
		return mSelf;
	}

	// 注册车载服务回调（全局状态变化）
	public void registerCarCallBack(CarService_Listener listener) {
		Media_IF.getInstance().registerCarCallBack(listener);
	}

	// 注销车载服务回调（全局状态变化）
	public void unregisterCarCallBack(CarService_Listener listener) {
		Media_IF.getInstance().unregisterCarCallBack(listener);
	}

	// 注册车载服务回调（模块相关变化）
	public void registerModeCallBack(Media_CarListener listener) {
		Media_IF.getInstance().registerModeCallBack(listener);
	}

	// 注销车载服务回调（模块相关变化）
	public void unregisterModeCallBack(Media_CarListener listener) {
		Media_IF.getInstance().unregisterModeCallBack(listener);
	}

	// 注册本地服务回调（模块相关变化）
	public void registerLocalCallBack(Media_Listener listener) {
		mMediaCallBack.registerMediaCallBack(listener);
	}

	// 注销本地服务回调（模块相关变化）
	public void unregisterLocalCallBack(Media_Listener listener) {
		mMediaCallBack.unregisterMediaCallBack(listener);
	}
	
	public int getMode() {
		return mMediaManager.getMediaMode();
	}
	
	public void bindCarService() {
		Media_IF.getInstance().bindCarService();
	}

	// 设置当前源
	public static boolean setCurSource(int source) {
		return Media_IF.setCurSource(source);
	}

	// 获取当前源
	public static int getCurSource() {
		return Media_IF.getCurSource();
	}
	
	public static boolean getMute() {
		return Media_IF.getMute();
	}
	
	public static boolean limitToPlayVideoWhenDrive() {
		return Media_IF.limitToPlayVideoWhenDrive();
	}
	
	public static int getCarSpeed() {
		return Media_IF.getCarSpeed();
	}
	
	public static void cancelMute() {
		Media_IF.cancelMute();
	}
	
	public static boolean getCallState() {
		return Media_IF.getCallState();
	}
	
	// 初始化媒体
	public void initMedia() {
		mMediaManager.initMedia(mMediaManager.getMediaMode(), mIMediaCallBack);
	}

	// 获取当前媒体状态
	public int getMediaState() {
		try {
			return mMediaManager.getCurMediaState();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return MediaState.IDLE;
	}
	
	// 设置视频层
	public void setVideoView(VideoSurfaceView view) {
		try {
			mMediaManager.setVideoView(view);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 播放(list中的postion)
	public boolean play(int pos) {
		try {
			Log.d(TAG, "play pos="+pos);
			if (MediaInterfaceUtil.mediaCannotPlay()) {
				return false;
			}
			return mMediaManager.play(pos);
		} catch (Exception e) {
			Log.e(TAG, "play pos="+pos, e);
		}
		return false;
	}
	
	// 播放(文件路径)
	public boolean play(String filePath) {
		try {
			Log.d(TAG, "play filePath="+filePath);
			if (MediaInterfaceUtil.mediaCannotPlay()) {
				return false;
			}
			return mMediaManager.play(filePath);
		} catch (Exception e) {
			Log.e(TAG, "play filePath="+filePath, e);
		}
		return false;
	}
	
	// 播放(FileNode)
	public boolean play(FileNode fileNode) {
		try {
			Log.d(TAG, "play fileNode="+fileNode);
			if (MediaInterfaceUtil.mediaCannotPlay()) {
				return false;
			}
			return mMediaManager.play(fileNode);
		} catch (Exception e) {
			Log.e(TAG, "play fileNode="+fileNode, e);
		}
		return false;
	}

	// 上一曲
	public boolean playPre() {
		try {
			Log.d(TAG, "playPre");
			if (MediaInterfaceUtil.mediaCannotPlay()) {
				return false;
			}
			return mMediaManager.pre(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	// 下一曲
	public boolean playNext() {
		try {
			Log.d(TAG, "playNext");
			if (MediaInterfaceUtil.mediaCannotPlay()) {
				return false;
			}
			return mMediaManager.next(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	// 改变播放状态
	public void changePlayState() {
		try {
			int state = getPlayState();
			if (state == PlayState.PLAY) {
				state = PlayState.PAUSE;
			} else {
				state = PlayState.PLAY;
			}
			setPlayState(state);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 设置播放状态
	public void setPlayState(int state) {
		try {
			Log.d(TAG, "setPlayState state="+state);
			if (state == PlayState.PLAY && MediaInterfaceUtil.mediaCannotPlay()) {
				return;
			}
			mMediaManager.setPlayState(state);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// 获取是否播放状态
	public boolean isPlayState() {
		return getPlayState() == PlayState.PLAY ? true : false;
	}
	
	// 获取播放状态
	public int getPlayState() {
		try {
			return mMediaManager.getPlayState();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return PlayState.STOP;
	}
	
	// 获取播放总时间
	public int getDuration() {
		try {
			return mMediaManager.getDuration() / 1000;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	// 设置播放当前时间
	public void setPosition(int time) {
		try {
			mMediaManager.setPosition(time * 1000);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 获取播放当前时间
	public int getPosition() {
		try {
			return mMediaManager.getPosition() / 1000;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	public void setCurScanner(int deviceType, int fileType) {
		mMediaManager.setDeviceAndFileType(deviceType, fileType);
	}

	public FileNode getItem(int pos) {
		return mMediaManager.getItem(pos);
	}
	
	public FileNode getPlayItem() {
		return mMediaManager.getPlayItem();
	}
	
	// 设置播放状态（被抢焦点前）
	public void setRecordPlayState(int state) {
		mMediaManager.setRecordPlayState(state);
	}

	// 获取播放状态（被抢焦点前）
	public int getRecordPlayState() {
		return mMediaManager.getRecordPlayState();
	}
	
	public void setVideoShow(boolean show) {
		mVideoShow = show;
		if (!show) {
			setRecordPlayState(PlayState.STOP);
		}
	}
	
	public boolean getVideoShow() {
		return mVideoShow;
	}
	
	//-------------------------------仪表接口开始-----------------------------
	public void sendToDashbroad(byte[] data){
		Media_IF.getInstance().sendToDashbroad(data);
	}
	//-------------------------------仪表接口结束-----------------------------
}
