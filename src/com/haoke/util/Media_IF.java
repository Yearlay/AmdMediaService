package com.haoke.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.util.Log;

import com.amd.media.AmdMediaManager;
import com.amd.media.MediaInterfaceUtil;
import com.haoke.aidl.ICarCallBack;
import com.haoke.aidl.IMediaCallBack;
import com.haoke.application.MediaApplication;
import com.haoke.audiofocus.AudioFocus;
import com.haoke.bean.FileNode;
import com.haoke.aidl.ICarUartCallBack;
import com.haoke.constant.MediaUtil;
import com.haoke.define.BTDef.*;
import com.haoke.define.CMSStatusDef.CMSStatusFuc;
import com.haoke.define.CMSStatusDef.TrafficRestriction;
import com.haoke.define.MediaDef.DeviceType;
import com.haoke.define.MediaDef.FileType;
import com.haoke.define.MediaDef.MediaState;
import com.haoke.define.MediaDef.PlayState;
import com.haoke.define.MediaDef.RandomMode;
import com.haoke.define.MediaDef.RepeatMode;
import com.haoke.define.MediaDef.ScanState;
import com.haoke.define.ModeDef;
import com.haoke.serviceif.CarService_IF;
import com.haoke.serviceif.CarService_Listener;
import com.haoke.video.VideoSurfaceView;
import com.jsbd.util.Meter_IF;

public class Media_IF extends CarService_IF {

	private static final String TAG = "Media_IF";
	private static Media_IF mSelf = null;
	private Media_CarCallBack mCarCallBack = null; // CarService的回调处理
	private Media_CallBack mMediaCallBack = null; // MediaService的回调处理
	private IMediaCallBack.Stub mIMediaCallBack = null;
	private Activity mMusicActivity = null;
	private Activity mVideoActivity = null;
	private Activity mImageActivity = null;
	
	private int mAudioDevice = DeviceType.NULL;
	
	private AmdMediaManager mMediaManager = null;

	public Media_IF() {
		mMode = ModeDef.MEDIA;
		
		mMediaManager = new AmdMediaManager();
		
		mCarCallBack = new Media_CarCallBack();
		mMediaCallBack = new Media_CallBack(mMode);

		// 以下处理服务回调
		mICallBack = new ICarCallBack.Stub() {
			@Override
			public void onDataChange(int mode, int func, int data)
					throws RemoteException {
				mCarCallBack.onDataChange(mode, func, data);
			}
		};
		
		mIUartCallBack = new ICarUartCallBack.Stub() {
			@Override
			public void onUartDataChange(int arg0, int arg1, byte[] arg2)
					throws RemoteException {
				mCarCallBack.onUartDataChange(arg0, arg1, arg2);
			}
		};
		mIMediaCallBack = new IMediaCallBack.Stub() {
			@Override
			public void onDataChange(int mode, int func, int data1, int data2)
					throws RemoteException {
				mMediaCallBack.onDataChange(mode, func, data1, data2);
			}
		};
	}

	// 获取接口实例
	synchronized public static Media_IF getInstance() {
		if (mSelf == null) {
			mSelf = new Media_IF();
		}
		return mSelf;
	}

	// 设置上下文
	public void setContext(Context context) {
		mContext = context;
	}

	// 获取模式
	public int getMode() {
		return mMode;
	}

	// 服务已经绑定成功，需要刷新动作
	@Override
	protected void onServiceConn() {
		mCarCallBack.onServiceConn();
	}

	// 注册车载服务回调（全局状态变化）
	public void registerCarCallBack(CarService_Listener listener) {
		mCarCallBack.registerCarCallBack(listener);
	}

	// 注销车载服务回调（全局状态变化）
	public void unregisterCarCallBack(CarService_Listener listener) {
		mCarCallBack.unregisterCarCallBack(listener);
	}

	// 注册车载服务回调（模块相关变化）
	public void registerModeCallBack(Media_CarListener listener) {
		mCarCallBack.registerModeCallBack(listener);
	}

	// 注销车载服务回调（模块相关变化）
	public void unregisterModeCallBack(Media_CarListener listener) {
		mCarCallBack.unregisterModeCallBack(listener);
	}

	// 注册本地服务回调（模块相关变化）
	public void registerLocalCallBack(Media_Listener listener) {
		mMediaCallBack.registerMediaCallBack(listener);
	}

	// 注销本地服务回调（模块相关变化）
	public void unregisterLocalCallBack(Media_Listener listener) {
		mMediaCallBack.unregisterMediaCallBack(listener);
	}

	// 设置主界面
	public void setMusicActivity(Activity activity) {
		mMusicActivity = activity;
	}

	// 获取主界面
	public Activity getMusicActivity() {
		return mMusicActivity;
	}

	// 设置主界面
	public void setVideoActivity(Activity activity) {
		mVideoActivity = activity;
	}

	// 获取主界面
	public Activity getVideoActivity() {
		return mVideoActivity;
	}

	// 设置主界面
	public void setImageActivity(Activity activity) {
		mImageActivity = activity;
	}

	// 获取主界面
	public Activity getImageActivity() {
		return mImageActivity;
	}

	// 设置当前界面
	public void setInterface(int id) {
		mMediaCallBack.setInterface(id);
	}

	public static int sLastSource;
	// 设置当前源
	public static boolean setCurSource(int source) {
		try {
			int lastSource = getCurSource();
			if (lastSource != source) {
				sLastSource = lastSource;
				Log.d(TAG, "setCurSource source="+source);
				return getInstance().mServiceIF.mcu_setCurSource(source);
			}
		} catch (Exception e) {
			Log.e(TAG, "setCurSource exception");
			e.printStackTrace();
		}
		return false;
	}

	// 获取当前源
	public static int getCurSource() {
		try {
			int source = getInstance().mServiceIF.mcu_getCurSource();
			Log.d(TAG, "getCurSource source="+source);
			return source;
		} catch (Exception e) {
			Log.e(TAG, "getCurSource error e="+e);
		}
		return ModeDef.NULL;
	}
	
	public static boolean getMute() {
		try {
			int mute = getInstance().mServiceIF.eq_getMute();
			Log.e(TAG, "getMute mute="+mute);
			return mute == 1 ? true : false;
		} catch (Exception e) {
			Log.e(TAG, "getMute error e="+e);
		}
		return false;
	}
	
	public static boolean limitToPlayVideoWhenDrive() {
		boolean limitFlag = false;
		try {
			int status = getInstance().mServiceIF.getCMSStatus(CMSStatusFuc.TRAFFIC_RESTRICTION);
			limitFlag = (status == TrafficRestriction.ON);
		} catch (Exception e) {
			Log.e(TAG, "limitToPlayVideoWhenDrive error e="+e);
		}
		return limitFlag;
	}
	
	public static void cancelMute() {
		try {
			if (getMute()) {
				Log.e(TAG, "getMute cancelMute");
				getInstance().mServiceIF.eq_setMute();
			}
		} catch (Exception e) {
			Log.e(TAG, "cancelMute error e="+e);
		}
	}
	
	public static boolean getCallState() {
		try {
			int state = getInstance().mServiceIF.bt_getCallState();
			Log.e(TAG, "getCallState state="+state);
			return state == BTCallState.IDLE ? false : true;
		} catch (Exception e) {
			Log.e(TAG, "getCallState error e="+e);
		}
		return false;
	}
	
	public AudioFocus getAudioFocus() {
		return null;
	}
	
	// 设置当前音频焦点
	public void requestAudioFocus(boolean request) {
		mMediaManager.requestAudioFocus(request);
	}
	
	// 设置当前音频焦点
	public void requestTransientAudioFocus(boolean request) {
		mMediaManager.requestTransientAudioFocus(request);
	}

	// 初始化媒体
	public void initMedia() {
		try {
			mMediaManager.initMedia(mMode, mIMediaCallBack);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 浏览指定设备
	public boolean browseDevice(int deviceType, int fileType) {
		mMediaManager.setDeviceAndFileType(deviceType, fileType);
		return true;
	}

	public int getAudioDevice() {
		return mAudioDevice;
	}

	public void setAudioDevice(int audioDevice) {
		this.mAudioDevice = audioDevice;
		mMediaManager.setDeviceAndFileType(audioDevice, FileType.AUDIO);
	}

	// 获取后台设备类型
	public int getPlayingDevice() {
		try {
			return mMediaManager.getPlayingDeviceType();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return DeviceType.NULL;
	}

	// 获取当前设备类型
	public int getMediaDevice() {
		try {
			return mMediaManager.getDeviceType();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return DeviceType.NULL;
	}

	// 获取后台文件类型
	public int getPlayingFileType() {
		try {
			return mMediaManager.getPlayingFileType();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return FileType.NULL;
	}

	// 获取当前文件类型
	public int getMediaFileType() {
		try {
			return mMediaManager.getFileType();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return FileType.NULL;
	}

	// 当前设备是否存在
	public boolean isDeviceExist(String path) {
		return MediaUtil.checkMounted(mContext, path);
	}

	// 获取扫描状态
	public int getScanState(int deviceType) {
		try {
			return mMediaManager.getScanState(deviceType);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ScanState.IDLE;
	}

	// 获取当前扫描状态
	public int getScanState() {
		try {
			return mMediaManager.getCurScanState();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ScanState.IDLE;
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
	
	public boolean isCurItemSelected(int pos) {
		try {
			return mMediaManager.isCurItemSelected(pos);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
		
	}

	// 选中/取消选中文件
	public void selectFile(int pos, boolean isSelect) {
		try {
			mMediaManager.selectFile(pos, isSelect);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 全选/取消全选文件
	public void selectAll(boolean isSelect) {
		try {
			mMediaManager.selectAll(isSelect);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 开始删除
	public void deleteStart() {
		try {
			mMediaManager.deleteStart();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// 开始拷贝
	public void copyStart() {
		try {
			mMediaManager.copyStart();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// 获取当前列表总数
	public int getListTotal() {
		try {
			return mMediaManager.getCurListTotal();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	// 获取当前列表项序号
	public int getListItemIndex(int pos) {
		try {
			return pos;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	// 获取当前列表项标题
	public String getListItemTitle(int pos) {
		try {
			return mMediaManager.getItem(pos).getTitleEx();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	// 获取当前列表项艺术家
	public String getListItemArtist(int pos) {
		try {
			return mMediaManager.getItem(pos).getArtist();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	// 获取当前列表项专辑名
	public String getListItemAlbum(int pos) {
		try {
			return mMediaManager.getItem(pos).getAlbum();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	// 获取当前列表项类型
	public int getListItemType(int pos) {
		try {
			return mMediaManager.getItem(pos).getFileType();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return FileType.NULL;
	}

	public String getListItemLastDate(int pos) {
		try {
			return mMediaManager.getItem(pos).getLastDate();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	// 获取当前播放列表项序号
	public int getPlayIndex() {
		try {
			return mMediaManager.getPlayingPos();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	// 获取当前播放列表项位置
	public int getPlayPos() {
		try {
			return mMediaManager.getPlayingPos();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
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
	
	// 设置scan模式，每首歌播放10秒
	public void setScanMode(boolean enable) {
		mMediaManager.setScanMode(enable);
	}
	
	// 获取scan模式
	public boolean getScanMode() {
		return mMediaManager.getScanMode();
	}

	// 设置循环模式
	public void setRepeatMode(int mode) {
		try {
			mMediaManager.setRepeatMode(mode);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 获取循环模式
	public int getRepeatMode() {
		try {
			return mMediaManager.getRepeatMode();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return RepeatMode.CIRCLE;
	}

	// 获取随机模式
	public int getRandomMode() {
		try {
			return mMediaManager.getRandomMode();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return RandomMode.OFF;
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

	// 获取歌曲名
	public String getPlayId3Title() {
		try {
			return mMediaManager.getPlayItem().getTitleEx();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	// 获取艺术家
	public String getPlayId3Artist() {
		try {
			return mMediaManager.getPlayItem().getArtist();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// 获取专辑名
	public String getPlayId3Album() {
		try {
			return mMediaManager.getPlayItem().getAlbum();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// 获取作曲家
	public String getPlayId3Composer() {
		try {
			return mMediaManager.getPlayItem().getComposer();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// 获取风格
	public String getPlayId3Genre() {
		try {
			return mMediaManager.getPlayItem().getGenre();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// 重置频谱
	public void resetSpectrum() {
		try {
			mMediaManager.resetSpectrum();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 获取单个频谱值
	public int getSpectrumData(int index) {
		try {
			return mMediaManager.getSpectrumData(index);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public int getBakDeviceType() {
		try {
			return mMediaManager.getPlayingDeviceType();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return DeviceType.NULL;
	}
	
	public boolean isCollected(int pos) {
		try {
			return mMediaManager.getItem(pos).getCollect() == 0 ? false : true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean collectMusic(int pos) {
		return collectMusic(getItem(pos));
	}
	
	public boolean collectMusic(FileNode fileNode) {
		try {
			return mMediaManager.collectMusic(fileNode);
		} catch (Exception e) {
			Log.e(TAG, "collectMusic fileNode="+fileNode, e);
		}
		return false;
	}
	
	public boolean deleteCollectedMusic(int pos) {
		return deleteCollectedMusic(getItem(pos));
	}
	
	public boolean deleteCollectedMusic(FileNode fileNode) {
		try {
			return mMediaManager.deleteCollectedMusic(fileNode);
		} catch (Exception e) {
			Log.e(TAG, "deleteCollectedMusic fileNode="+fileNode, e);
		}
		return false;
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
	
	public FileNode getDefaultItem() {
		return mMediaManager.getDefaultItem();
	}
	
	public void finishVideoActivity() {
		try {
			if (getVideoActivity() != null) {
				getVideoActivity().finish();
				setVideoActivity(null);
			}
		} catch (Exception e) {
			Log.e(TAG, "HMI------------Finish Activity e=" + e.getMessage());
		}
	}
	
	public void sourceChanged(int source) {
		mMediaManager.sourceChanged(source);
		if (source != ModeDef.VIDEO) {
			finishVideoActivity();
		}
	}
	
//	public void audioFocusChanged(int state) {
//		mMediaManager.audioFocusChanged(state);
//	}
	
	public int getMediaListSize(int deviceType, int fileType) {
		return mMediaManager.getMediaListSize(deviceType, fileType);
	}
	
	// 设置播放状态（被抢焦点前）
	public void setRecordPlayState(int state) {
		mMediaManager.setRecordPlayState(state);
	}

	// 获取播放状态（被抢焦点前）
	public int getRecordPlayState() {
		return mMediaManager.getRecordPlayState();
	}
	
	//-------------------------------仪表接口开始-----------------------------
	private static void setMeterIfSource(int source) {
		int meterSource = Meter_IF.SOURCE_OTHER;
		
		Meter_IF.notifyMeterMediaSrc(source);
	}
	
	public void sendToDashbroad(byte[] data){
		try {
			if(mServiceIF!=null){
				mServiceIF.sendToDashbroad(data);
			}
		} catch (Exception e) {
			Log.e(TAG, Log.getStackTraceString(e));
		}
	}
	//-------------------------------仪表接口结束-----------------------------
}
