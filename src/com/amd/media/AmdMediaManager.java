/*
 *describe:本类封装多媒体所有的对外方法，以及实现功能处理逻辑
 *author:林永彬
 *date:2016.10.29
 */

package com.amd.media;

import java.util.ArrayList;
import java.util.Random;

import com.haoke.aidl.IMediaCallBack;
import com.haoke.application.MediaApplication;
import com.haoke.audiofocus.AudioFocus;
import com.haoke.audiofocus.AudioFocusListener;
import com.haoke.bean.FileNode;
import com.haoke.bean.StorageBean;
import com.haoke.constant.MediaUtil;
import com.haoke.constant.MediaUtil.CopyState;
import com.haoke.constant.MediaUtil.MediaFuncEx;
import com.haoke.data.AllMediaList;
import com.haoke.data.LoadListener;
import com.haoke.data.OperateListener;
import com.haoke.define.MediaDef.DeleteState;
import com.haoke.define.MediaDef.DeviceType;
import com.haoke.define.MediaDef.FileType;
import com.haoke.define.MediaDef.MediaFunc;
import com.haoke.define.MediaDef.MediaState;
import com.haoke.define.MediaDef.PlayState;
import com.haoke.define.MediaDef.RandomMode;
import com.haoke.define.MediaDef.RepeatMode;
import com.haoke.define.MediaDef.ScanState;
import com.haoke.define.ModeDef;
import com.haoke.service.MediaClient;
import com.haoke.spectrum.Spectrum;
import com.haoke.util.Media_IF;
import com.haoke.video.VideoSurfaceView;

import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;

public class AmdMediaManager implements AmdMediaPlayerListener, AudioFocusListener {

	private static final String TAG = "AmdMediaManager";
	private Context mContext = null;
	private AmdMediaPlayer mMediaPlayer = null;
	private boolean mScanMode = false;
	private int mRepeatMode = RepeatMode.OFF;
	private int mErrorCount = 0; // 连续播放错误的次数
	private int mPlayState = PlayState.STOP;
	private int mRecordPlayState = PlayState.STOP; // 用来记忆被抢焦点前的播放状态，便于恢复播放

	private ArrayList<MediaClient> mClientList = new ArrayList<MediaClient>();
	
	private boolean mIsPlayDefault = false;
	
    private int mCurSource = ModeDef.NULL;
    
    private AllMediaList mAllMediaList;

	private AudioFocus mAudioFocus;
	private Spectrum mSpectrum; // 频谱类

	//----------------start------------------/
	//当前设置的设备信息
	private int mDeviceType = DeviceType.NULL;
	private int mFileType = FileType.NULL;
	private int mListSize = -1;
	//播放器播放的设备信息
	private int mPlayingDeviceType = DeviceType.NULL;
	private int mPlayingFileType = FileType.NULL;
	private int mPlayingListSize = -1;
	private int mPlayingPos = -1;
	private int[] mRandomNums;
	private int mRandomListPos = 0; // 当前随机列表焦点位置
	private FileNode mPlayingFileNode = null;
	private FileNode mPlayMusicFileNode;
	//------------------end----------------/
	
	private AudioManager mAudioManager;
	private ComponentName mComponentName;

	public AmdMediaManager() {
		mContext = MediaApplication.getInstance();
		
		mAudioFocus = new AudioFocus(mContext);
		mAudioFocus.registerListener(this);
		
		mAllMediaList = AllMediaList.instance(mContext);

		mMediaPlayer = new AmdMediaPlayer(mContext);
		mMediaPlayer.setMediaPlayerListener(this);

		mAllMediaList.registerLoadListener(mLoadListener);

		mRepeatMode = mAllMediaList.getPlayMode();
		
		mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
		mComponentName = new ComponentName(mContext, AmdMediaButtonReceiver.class); 
	}
	
	public void destory() {
		mAudioFocus.unRegisterListener(this);
		mAllMediaList.unRegisterLoadListener(mLoadListener);
		mAllMediaList = null;
		mMediaPlayer.setMediaPlayerListener(null);
		mClientList.clear();
		mMediaPlayer = null;
		mContext = null;
	}
	
    // 设置设备类型和文件类型
    public void setDeviceAndFileType(int deviceType, int fileType) {
    	Log.d(TAG, "setDeviceAndFileType: deviceType="+deviceType+"; fileType="+fileType
    			+ "; mDeviceType="+mDeviceType+"; mFileType="+mFileType);
    	if (mDeviceType != deviceType || mFileType != fileType) {
    		mDeviceType = deviceType;
    		mFileType = fileType;
    		loadData();
    		resetPlayingData(false);
    	}
    }

    // 获取设备类型
    public int getDeviceType() {
        return mDeviceType;
    }
    
    // 获取播放的设备类型
    public int getPlayingDeviceType() {
    	return mPlayingDeviceType;
    }

    // 获取文件类型
    public int getFileType() {
        return mFileType;
    }
    
    // 获取播放的文件类型
    public int getPlayingFileType() {
        return mPlayingFileType;
    }
    
    private static final int MSG_DELAY_PLAYTIME = 3000;
    private static final int MSG_SAVE_PLAYTIME = 1;
    private Handler mHandler = new Handler() {
    	public void handleMessage(android.os.Message msg) {
    		switch (msg.what) {
			case MSG_SAVE_PLAYTIME:
				int time = getPosition();
				mAllMediaList.savePlayState(getPlayItem(), time);
				Log.d(TAG, "MSG_SAVE_PLAYTIME time="+time+"; mPlayingPos="+mPlayingPos+"; mPlayingListSize="+mPlayingListSize);
				removeMessages(MSG_SAVE_PLAYTIME);
				sendEmptyMessageDelayed(MSG_SAVE_PLAYTIME, MSG_DELAY_PLAYTIME);
				break;
			}
    	};
    };

	// 开始播放时间记录
	private void startRecordTimer() {
		mHandler.removeMessages(MSG_SAVE_PLAYTIME);
		mHandler.sendEmptyMessageDelayed(MSG_SAVE_PLAYTIME, MSG_DELAY_PLAYTIME);
	}

	// 停止播放时间记录
	private void stopRecordTimer() {
		mHandler.removeMessages(MSG_SAVE_PLAYTIME);
	}

	// 播放默认歌曲（在加载完成后调用）
	public boolean playDefault() {
		Log.v(TAG, "playDefault");

		FileNode node = mAllMediaList.getPlayState(mPlayingDeviceType, mPlayingFileType);
		// 之前没有路径记录，默认第1首
		if (node == null) {
			return playOther(null, 0);
		}
		boolean result = playOther(node, -1);
		if (result) {
			mIsPlayDefault = true;
		}
		return result;
	}
	
	// 播放指定歌曲
	public boolean play(int pos) {
		Log.v(TAG, "play pos=" + pos);
		setPlayingData(mDeviceType, mFileType, true);
		return playOther(null, pos);
	}
	
	public boolean play(String filePath) {
		Log.v(TAG, "play filePath=" + filePath);
		int deviceType = MediaUtil.getDeviceType(filePath);
		int fileType = MediaUtil.getMediaType(filePath);
		if (deviceType == DeviceType.USB1 || deviceType == DeviceType.USB2 ||
				deviceType == DeviceType.FLASH || deviceType == DeviceType.COLLECT) {
			if (fileType == FileType.AUDIO || fileType == FileType.VIDEO) {
				FileNode fileNode = getFileNodeByFilePath(filePath);
				if (fileNode != null) {
					setPlayingData(deviceType, fileType, true);
					return playOther(fileNode, -1);
				}
			}
		}
		Log.e(TAG, "play ERROR! deviceType="+deviceType+"; fileType="+fileType);
		return false;
	}
	
	public boolean play(FileNode fileNode) {
		Log.v(TAG, "play fileNode=" + fileNode);
		int deviceType = fileNode.getDeviceType();
		int fileType = fileNode.getFileType();
		if (deviceType == DeviceType.USB1 || deviceType == DeviceType.USB2 ||
				deviceType == DeviceType.FLASH || deviceType == DeviceType.COLLECT) {
			if (fileType == FileType.AUDIO || fileType == FileType.VIDEO) {
				setPlayingData(deviceType, fileType, true);
				return playOther(fileNode, -1);
			}
		}
		Log.e(TAG, "play ERROR! deviceType="+deviceType+"; fileType="+fileType);
		return false;
	}
	
	private FileNode getFileNodeByFilePath(String filePath) {
		FileNode fileNode = null;
		int deviceType = MediaUtil.getDeviceType(filePath);
		int fileType = MediaUtil.getMediaType(filePath);
		ArrayList<FileNode> lists = mAllMediaList.getMediaList(deviceType, fileType);
		for (int i=0; i<lists.size(); i++) {
			FileNode node = lists.get(i);
			if (node.getFilePath().equals(filePath)) {
				fileNode = node;
			}
		}
		return fileNode;
	}
	
	/**
	 * 如果 fileNode为null，则pos生效
	 */
	private boolean playOther(FileNode fileNode, int pos) {
		Log.v(TAG, "playOther fileNode=" + fileNode + "; pos="+pos);
		FileNode node = null;
		
		if (fileNode == null) {
			ArrayList<FileNode> lists = mAllMediaList.getMediaList(mPlayingDeviceType, mPlayingFileType);
			if (lists.size() <= pos || pos < 0) {
				return false;
			}
			node = lists.get(pos);
			if (node == null) {
				return false;
			}
			mPlayingPos = pos;
		} else {
			node = fileNode;
			mPlayingPos = changeFileNodeToIndex(node);
		}
		
		if (mPlayingPos == -1) {
			return false;
		}
		
		changeSource(mPlayingFileType);
		requestAudioFocus(true);
		if (mPlayingFileType == FileType.AUDIO && mRepeatMode == RepeatMode.RANDOM) { // 随机开
			mRandomListPos = changeIndexToRandomPos(mPlayingPos);
		}

		mPlayingFileNode = node;
		if (node.getFileType() == FileType.AUDIO) {
			mPlayMusicFileNode = node;
		}
		String path = node.getFilePath(); // 获得播放路径
		mMediaPlayer.setFileType(mPlayingFileType);
		boolean returnVal = mMediaPlayer.setDataSource(path);
//		if (returnVal) {
//			onDataChanged(ModeDef.MEDIA, MediaFunc.PLAY_STATE, PlayState.PLAY, 0);
//		}
		return returnVal;
	}
	
	// 上一曲（若force为true，表示强制换曲，不受单曲循环影响）
	public boolean pre(boolean force) {
		Log.v(TAG, "pre force="+force);

//		setPlayingData(mDeviceType, mFileType, false);
		int pos = 0;
		int repeatMode = ((mPlayingFileType == FileType.AUDIO) ? mRepeatMode : RepeatMode.CIRCLE);
		if (repeatMode == RepeatMode.OFF) { // 顺序播放
			pos = mPlayingPos;
			pos--;
			Log.v(TAG, "pre pos:" + pos + ", total:" + mPlayingListSize);
			if (pos < 0) { // 播放结束
				Log.v(TAG, "pre playOver 1");
				playOver();
				return false;
			}

		} else if (repeatMode == RepeatMode.RANDOM) { // 随机开
			mRandomListPos--;
			if (mRandomListPos < 0) {
				mRandomListPos = mPlayingListSize - 1;
			}
			pos = mRandomNums[mRandomListPos];

		} else { // 循环
			if (repeatMode == RepeatMode.ONE) { // 单曲循环
				if (force) {
					pos = mPlayingPos;
					pos--;
					if (pos < 0) { // 播放结束
						pos = mPlayingListSize - 1;
					}
				} else {
					pos = mPlayingPos;
				}
			} else if (repeatMode == RepeatMode.CIRCLE) { // 全部循环
				pos = mPlayingPos;
				pos--;
				if (pos < 0) {
					pos = mPlayingListSize - 1;
				}
			}
		}
		return playOther(null, pos);
	}

	// 下一曲（若force为true，表示强制换曲，不受单曲循环影响）
	public boolean next(boolean force) {
		Log.v(TAG, "next force="+force+"; mRepeatMode="+mRepeatMode);

//		setPlayingData(mDeviceType, mFileType, false);
		int pos = 0;
		int repeatMode = ((mPlayingFileType == FileType.AUDIO) ? mRepeatMode : RepeatMode.CIRCLE);
		if (repeatMode == RepeatMode.OFF) { // 顺序播放
			pos = mPlayingPos;
			pos++;
			Log.v(TAG, "next pos:" + pos + ", total:" + mPlayingListSize);
			if (pos > mPlayingListSize - 1) { // 播放结束
				Log.v(TAG, "next playOver 1");
				playOver();
				return false;
			}

		} else if (repeatMode == RepeatMode.RANDOM) { // 随机开
			mRandomListPos++;
			if (mRandomListPos >= getRandomListTotal()) {
				mRandomListPos = 0;
			}
			pos = mRandomNums[mRandomListPos];

		} else { // 循环
			if (repeatMode == RepeatMode.ONE) { // 单曲循环
				if (force) {
					pos = mPlayingPos;
					pos++;
					if (pos > mPlayingListSize - 1) { // 播放结束
						pos = 0;
					}
				} else {
					pos = mPlayingPos;
				}
			} else if (repeatMode == RepeatMode.CIRCLE) { // 全部循环
				pos = mPlayingPos;
				pos++;
				if (pos > mPlayingListSize - 1) {
					pos = 0;
				}
			}
		}
		return playOther(null, pos);
	}

	// 重置播放器
	public void resetMediaPlayer() {
		try {
			if (mMediaPlayer.getMediaState() == MediaState.PREPARED) {
				mMediaPlayer.stop();
				mPlayState = PlayState.STOP;
			}
			mMediaPlayer.reset();

		} catch (Exception e) {
			Log.e(TAG, "resetMediaPlayer e=" + e);
		}
	}
	
	// 设置播放状态
	public void setPlayState(int state) {
		Log.v(TAG, "setPlayState state=" + state);
		if (state == PlayState.PLAY) {
			if (mPlayingFileNode != null) {
				changeSource(mPlayingFileType); // 确保当前源在媒体
				requestAudioFocus(true);
	    		mMediaPlayer.start();
	    		mPlayState = PlayState.PLAY;
	    		onDataChanged(ModeDef.MEDIA, MediaFunc.PLAY_STATE, getPlayState(), 0);
	    	} else {
	    		FileNode fileNode = getDefaultItem();
	    		if (fileNode == null) {
	    			Log.e(TAG, "setPlayState: no song!");
//	    			android.widget.Toast.makeText(mContext, "没有歌曲文件！", android.widget.Toast.LENGTH_SHORT).show();
	    		} else {
	    			mPlayingFileNode = fileNode;
	    			setPlayingData(fileNode.getDeviceType(), fileNode.getFileType(), true);
	    			mPlayingFileNode = null;
	    			playOther(null, mPlayingPos);
	    		}
	    	}
    		return;
		} else if (state == PlayState.PAUSE) {
			mMediaPlayer.pause();
			mPlayState = PlayState.PAUSE;
		} else if (state == PlayState.STOP) {
			mMediaPlayer.stop();
			mPlayState = PlayState.STOP;
		}
		onDataChanged(ModeDef.MEDIA, MediaFunc.PLAY_STATE, getPlayState(), 0);
	}
	
	// 获取播放状态
	public int getPlayState() {
		if (mMediaPlayer.isPlaying()) {
			return PlayState.PLAY;
		} else {
			return mPlayState;
		}
	}

	// 设置播放状态（被抢焦点前）
	public void setRecordPlayState(int state) {
		mRecordPlayState = state;
	}

	// 获取播放状态（被抢焦点前）
	public int getRecordPlayState() {
		return mRecordPlayState;
	}
	
	// 设置scan模式，每首歌播放10秒
	public void setScanMode(boolean enable) {
		mScanMode = enable;
	}
	
	// 获取scan模式
	public boolean getScanMode() {
		return mScanMode;
	}

	// 设置循环模式
	public void setRepeatMode(int mode) {
		mRepeatMode = mode;
		mAllMediaList.savePlayMode(mode);
		onDataChanged(ModeDef.MEDIA, MediaFunc.REPEAT_MODE, mode, 0);
	}

	// 获取循环模式
	public int getRepeatMode() {
		return mRepeatMode;
	}

	// 获取随机模式
	public int getRandomMode() {
		return (mRepeatMode == RepeatMode.RANDOM) ? RandomMode.ON : RandomMode.OFF;
	}

	// 重置频谱
	public void resetSpectrum() {
		if (mSpectrum != null) {
			mSpectrum.setVisualizer(null);
			mSpectrum = null;
		}

		mSpectrum = new Spectrum(); // 初始化频谱类
		mSpectrum.initSpectrum(mMediaPlayer.getMediaPlayer());
	}

	// 获取频谱
	public Spectrum getSpectrum() {
		return mSpectrum;
	}

	// 获取频谱数据
	public int getSpectrumData(int index) {
		if (mSpectrum == null) {
			return 0;
		}
		return mSpectrum.getFreqData(index);
	}

	// 获取播放总时间
	public int getDuration() {
		return mMediaPlayer.getDuration();
	}

	// 设置当前播放时间
	public void setPosition(int time) {
		mMediaPlayer.seekTo(time);
	}

	// 获取当前播放时间
	public int getPosition() {
		return mMediaPlayer.getPosition();
	}

	// 设置视频层
	public void setVideoView(VideoSurfaceView surfaceView) {
		mMediaPlayer.setVideoView(surfaceView);
	}

	// 设置焦点丢失
	public void focusLossed() {
		mMediaPlayer.focusLossed();
	}

	// 播放结束处理
	private void playOver() {
		Log.v(TAG, "playOver");
		
		mErrorCount = 5;
		mPlayingPos = -1;
		mRandomListPos = 0;
		mPlayingFileNode = null;
		clearPlayRecord();
		mMediaPlayer.stop();
		mPlayState = PlayState.STOP;

		onDataChanged(ModeDef.MEDIA, MediaFunc.PLAY_OVER, 0, 0);
	}

	// 播放开始
	@Override
	public void onStart() {
		Log.v(TAG, "onStart");
		startRecordTimer(); // 开始播放时间记录
	}

	// 播放暂停
	@Override
	public void onPause() {
		Log.v(TAG, "onPause");
		stopRecordTimer(); // 停止播放时间记录
	}

	// 播放停止
	@Override
	public void onStop() {
		Log.v(TAG, "onStop");
		stopRecordTimer(); // 停止播放时间记录
	}

	// 换曲/歌曲已播放
	@Override
	public void onPreparing() {
		Log.v(TAG, "onPreparing");
		onDataChanged(ModeDef.MEDIA, MediaFunc.PREPARING,
				mMediaPlayer.getMediaState(), 0);
	}

	// 换曲/歌曲已播放
	@Override
	public void onPrepared() {
		Log.v(TAG, "onPrepared");
		mErrorCount = 0;
		//mBakMediaScanner.readId3(); // 准备好媒体信息

		Log.v(TAG, "onPrepared mIsPlayDefault=" + mIsPlayDefault);
		// 恢复之前的播放时间
		if (mIsPlayDefault) {
			mIsPlayDefault = false;

			FileNode fileNode = mAllMediaList.getPlayState(mPlayingDeviceType, mPlayingFileType);
			int playTime = 0;
			if (fileNode != null) {
				playTime = fileNode.getPlayTime();
			}
			Log.v(TAG, "onPrepared playTime=" + playTime);
			if (playTime >= 1000) {
				setPosition(playTime);
			}

		} else {
			mAllMediaList.savePlayState(getPlayItem(), 0);
		}
		mPlayState = PlayState.PLAY;

		Log.v(TAG, "onPrepared onDataChanged");
		onDataChanged(ModeDef.MEDIA, MediaFunc.PREPARED,
				mMediaPlayer.getMediaState(), 0);
		Log.v(TAG, "onPrepared over");
	}

	// 播放结束
	@Override
	public void onCompletion() {
		Log.v(TAG, "onCompletion");
		if (!mScanMode) {
			next(false); // 自动播放下一曲
		}
		mIsPlayDefault = false;
		onDataChanged(ModeDef.MEDIA, MediaFunc.COMPLETION, 0, 0);
	}

	// 定点播放成功
	@Override
	public void onSeekCompletion() {
		Log.v(TAG, "onSeekCompletion");
		onDataChanged(ModeDef.MEDIA, MediaFunc.SEEK_COMPLETION, 0, 0);
	}

	// 播放错误
	@Override
	public void onError() {
		Log.v(TAG, "onError mErrorCount:" + mErrorCount);
		if (!mScanMode) {
			if (mErrorCount < 5) {
				mErrorCount++;
				if (getPlayingFileType() != FileType.VIDEO) {
					next(true); // 自动播放下一曲
				}
			} else {
				Log.v(TAG, "onError playOver");
				playOver();
			}
		}
		mIsPlayDefault = false;

		onDataChanged(ModeDef.MEDIA, MediaFunc.ERROR, 0, 0);
	}

	// 文件错误
	@Override
	public void onIOException() {
		Log.v(TAG, "onIOException mErrorCount:" + mErrorCount);
		if (!mScanMode) {
			if (mErrorCount < 5) {
				mErrorCount++;
				next(true); // 自动播放下一曲
			} else {
				Log.v(TAG, "onIOException playOver");
				playOver();
			}
		}
		mIsPlayDefault = false;

		onDataChanged(ModeDef.MEDIA, MediaFunc.ERROR, 0, 0);
	}

	@Override
	public void onSurfaceCreated() {
		Log.v(TAG, "onSurfaceCreated");
	}

	@Override
	public void onSurfaceDestroyed() {
		Log.v(TAG, "onSurfaceDestroyed");
	}

	private void onDataChanged(int mode, int func, int data0, int data1) {
		Log.v(TAG, "onDataChanged mode=" + mode + ", func=" + func + ", data0="
				+ data0 + ", data1=" + data1);
		dispatchDataToClients(mode, func, data0, data1);
	}
	
	// 清除播放记录
	private void clearPlayRecord() {
		mAllMediaList.clearPlayState(mPlayingDeviceType, mPlayingFileType);
	}
	
    // 准备好随机列表
    private void resetRandomNum(int size) {
    	mRandomNums = new int[size];
    	for (int i=0; i<size; i++) {
    		mRandomNums[i] = i;
    	}
    	Random random = new Random();
    	for (int i=0; i<size; i++) {
    		int j = random.nextInt(size);
    		int k = mRandomNums[i];
    		mRandomNums[i] = mRandomNums[j];
    		mRandomNums[j] = k;
    	}
    }

    // 获取随机列表总数
    private int getRandomListTotal() {
        if (mRandomNums == null) {
            return 0;
        }
        return mRandomNums.length;
    }

    // 将序号转成在随机列表中的位置
    private int changeIndexToRandomPos(int index) {
    	if (mRandomNums != null) {
            for (int i = 0; i<mRandomNums.length; i++) {
            	if (mRandomNums[i] == index) {
            		return i;
            	}
            }
        }
        return 0;
    }
    
    private int changeFileNodeToIndex(FileNode fileNode) {
    	int index = -1;
    	if (fileNode != null) {
        	ArrayList<FileNode> lists = mAllMediaList.getMediaList(mPlayingDeviceType, mPlayingFileType);
        	for (int i=0; i<lists.size(); i++) {
        		FileNode list = lists.get(i);
        		if (list.isSame(fileNode)) {
        			index = i;
        			break;
        		}
        	}
    	}
    	return index;
    }
    
    private LoadListener mLoadListener = new LoadListener() {
		
		@Override
		public void onLoadCompleted(int deviceType, int fileType) {
			Log.d(TAG, "mLoadListener onLoadCompleted deviceType="+deviceType+"; fileType="+fileType);
			// 处理数据加载完成的事件: 主要是处理数据。
			if (deviceType == mDeviceType && fileType == mFileType) {
				Log.d(TAG, "mLoadListener onLoadCompleted MEDIA_LIST_UPDATE");
				loadData();
				onDataChanged(ModeDef.MEDIA, MediaUtil.MediaFuncEx.MEDIA_LIST_UPDATE, deviceType, fileType);
			}
			if (deviceType == mPlayingDeviceType && fileType == mPlayingFileType) {
				setPlayingData(deviceType, fileType, false);
			}
		}
		
		@Override
		public void onScanStateChange(StorageBean storageBean) {
			// 处理磁盘状态 和 扫描状态发生改变的状态： 主要是更新UI的显示效果。
			Log.d(TAG, "mLoadListener onScanStateChange storageBean="+storageBean+"; mDeviceType="+mDeviceType+"; mPlayingDeviceType="+mPlayingDeviceType);
//			if (storageBean.getDeviceType() == mDeviceType) {
//				needToChange(storageBean.getDeviceType(), storageBean.getState());
//			}
			needToChange(storageBean.getDeviceType(), storageBean.getState());
			if (storageBean.getDeviceType() == mPlayingDeviceType) {
				if (!storageBean.isMounted()) {
					resetMediaPlayer();
					resetPlayingData(true);
					if (mRepeatMode == RepeatMode.RANDOM) {
						setRepeatMode(RepeatMode.OFF);//插拔U盘，断随机模式记忆
					}
				}
			}
		}
		
	};
	
	private void loadData() {
		ArrayList<FileNode> lists = mAllMediaList.getMediaList(mDeviceType, mFileType);
		mListSize = lists.size();
	}
	
	private void setPlayingData(int deviceType, int fileType, boolean force) {
		Log.d(TAG, "setPlayingData deviceType="+deviceType+"; fileType="+fileType+"; force="+force);
		Log.d(TAG, "setPlayingData mPlayingDeviceType="+mPlayingDeviceType+"; mPlayingFileType="+mPlayingFileType+"; mPlayingPos="+mPlayingPos);
		ArrayList<FileNode> lists = mAllMediaList.getMediaList(deviceType, fileType);
		int size = lists.size();
		if (mPlayingDeviceType == deviceType && mPlayingFileType == fileType) {
			if (size != mPlayingListSize) {
				force = true;
			}
		} else if (mPlayingDeviceType != deviceType || mPlayingFileType != fileType) {
			force = true;
		}
		if (force) {
			mPlayingDeviceType = deviceType;
			mPlayingFileType = fileType;
			if (size != mPlayingListSize) {
				mPlayingListSize = size;
				resetRandomNum(mPlayingListSize);
			}
			mPlayingPos = changeFileNodeToIndex(mPlayingFileNode);
			mRandomListPos = changeIndexToRandomPos(mPlayingPos);
			mPlayingFileNode = getPlayItem();
		}
		
	}
	
	private void resetPlayingData(boolean force) {
		if (mPlayingDeviceType == DeviceType.NULL ||
	           mPlayingFileType == FileType.NULL) {
			force = true;
		}
		if (mDeviceType == DeviceType.NULL ||
				mFileType == FileType.NULL) {
			force = false;
		}
		if (force) {
			mPlayingDeviceType = mDeviceType;
			mPlayingFileType = mFileType;
			int size = mAllMediaList.getMediaList(mDeviceType, mFileType).size();
			if (size != mPlayingListSize) {
				resetRandomNum(size);
			}
			mPlayingListSize = mListSize = size;
			//FileNode fileNode = mAllMediaList.getPlayState(mPlayingDeviceType, mPlayingFileType);
			mPlayingPos = -1;//changeFileNodeToIndex(fileNode);
			mRandomListPos = 0;//changeIndexToRandomPos(mPlayingPos);
			mPlayingFileNode = null;
		}
	}
	
	private void needToChange(int deviceType, int state) {
		if (deviceType == mDeviceType) {
			if (state == StorageBean.EJECT) { // 非挂载的状态。
				onDataChanged(ModeDef.MEDIA, MediaFunc.DEVICE_CHANGED, deviceType, 0);
			} else {
				// 文件扫描的状态发生改变。
				switch (state) {
				case StorageBean.MOUNTED:
					onDataChanged(ModeDef.MEDIA, MediaFunc.SCAN_STATE, ScanState.IDLE, 0);
					break;
				case StorageBean.FILE_SCANNING:
					onDataChanged(ModeDef.MEDIA, MediaFunc.DEVICE_CHANGED, deviceType, 1);
					onDataChanged(ModeDef.MEDIA, MediaFunc.SCAN_STATE, ScanState.SCANNING, 0);
					break;
				case StorageBean.SCAN_COMPLETED:
					break;
				case StorageBean.ID3_PARSING:
					break;
				case StorageBean.ID3_PARSE_COMPLETED:
					onDataChanged(ModeDef.MEDIA, MediaFunc.SCAN_STATE, ScanState.COMPLETED_ALL, 0);
					break;
				}
			}
		} else {
			if (state == StorageBean.EJECT) {
				onDataChanged(ModeDef.MEDIA, MediaFunc.DEVICE_CHANGED, deviceType, 0);
			} else if (state == StorageBean.FILE_SCANNING) {//state == StorageBean.MOUNTED
				onDataChanged(ModeDef.MEDIA, MediaFunc.DEVICE_CHANGED, deviceType, 1);
			}
		}

	}
	
	/**
	 * 只供播放时，文件类型的改变
	 */
	private void changeSource(int fileType) {
		// 判断是否通知MCU切源
		int source = ModeDef.NULL;
		if (fileType == FileType.AUDIO) {
			source = ModeDef.AUDIO;
		} else if (fileType == FileType.VIDEO) {
			source = ModeDef.VIDEO;
		}
		if (mCurSource != source) {
			mCurSource = source;
			com.haoke.util.Media_IF.setCurSource(mCurSource);
		}
	}
	
	/**
	 * 切换播放源，参数为 {@link com.haoke.define.ModeDef} <p>
	 * eg. {@link com.haoke.define.ModeDef#VIDEO}
	 */
	public void sourceChanged(int source) {
		Log.v(TAG, "sourceChanged source=" + source);
		if (source == ModeDef.IMAGE) {
			return;
		}
		if (source != ModeDef.AUDIO && source != ModeDef.VIDEO) {
			mCurSource = ModeDef.NULL;
//			if (getPlayState() == PlayState.PLAY) {
//				Log.v(TAG, "sourceChanged setPlayState PAUSE");
//				setPlayState(PlayState.PAUSE); // 暂停播放
//			}
		}
		if (source != ModeDef.AUDIO && source != ModeDef.VIDEO 
				&& source != ModeDef.IMAGE ) {
			mCurSource = source;
		}
	}
	
	private boolean hasAudioFocus() {
		boolean ret = false;
		int audioFocusState = mAudioFocus.getFocusState();
		if (audioFocusState == AudioManager.AUDIOFOCUS_GAIN
				|| audioFocusState == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
			ret = true;

		return ret;
	}
	
	// 设置当前音频焦点
	public void requestAudioFocus(boolean request) {
		if (request && hasAudioFocus()) {
			
		} else {
			mAudioFocus.requestAudioFocus(request);
		}
	}
	
	// 设置当前音频焦点
	public void requestTransientAudioFocus(boolean request) {
		mAudioFocus.requestTransientAudioFocus(request);
	}
	
	@Override
	public void audioFocusChanged(int state) {
		int playState = getPlayState();
		int recordPlayState = getRecordPlayState();
		Log.v(TAG, "HMI------------audioFocusChanged state=" + state);
		Log.v(TAG, "HMI------------audioFocusChanged playState=" + playState);
		Log.v(TAG, "HMI------------audioFocusChanged recordPlayState=" + recordPlayState);

		switch (state) {
		case PlayState.PLAY:
			if (recordPlayState == PlayState.PLAY) {
				setPlayState(recordPlayState);
				// 清除标志，避免原本是暂停，每次抢焦点都进行播放
				setRecordPlayState(PlayState.STOP);
			}
			mAudioManager.registerMediaButtonEventReceiver(mComponentName);
			break;
			
		case PlayState.PAUSE:
			if (playState == PlayState.STOP) {
				Log.v(TAG, "HMI------------audioFocusChanged STOP 1");
				return;
			}
			setRecordPlayState(playState);
			setPlayState(PlayState.PAUSE);
			focusLossed();
			break;
			
		case PlayState.STOP:
			MediaInterfaceUtil.resetMediaPlayStateRecord(mPlayingFileType == FileType.AUDIO ? ModeDef.AUDIO : ModeDef.VIDEO);
			mAudioManager.unregisterMediaButtonEventReceiver(mComponentName);
			if (playState == PlayState.STOP) {
				Log.v(TAG, "HMI------------audioFocusChanged STOP 2");
				return;
			}
			setRecordPlayState(playState);
			setPlayState(PlayState.PAUSE);
			focusLossed();
			break;
		}
	}
	
	public int getScanState(int deviceType) {
		if (deviceType == DeviceType.COLLECT) {
			return ScanState.COMPLETED_ALL;
		}
		String devicePath = MediaUtil.getDevicePath(deviceType);
        StorageBean storageBean = mAllMediaList.getStoragBean(devicePath);
        int returnVal = ScanState.IDLE;
        int state = storageBean.getState();
        switch (state) {
        case StorageBean.EJECT:
        	returnVal = ScanState.NO_DEVICE;
        	break;
        case StorageBean.MOUNTED:
        	returnVal = ScanState.IDLE;
        	break;
        case StorageBean.FILE_SCANNING:
        case StorageBean.SCAN_COMPLETED:
        case StorageBean.ID3_PARSING:
        	returnVal = ScanState.SCANNING;
        	break;
        case StorageBean.ID3_PARSE_COMPLETED:
        	returnVal = ScanState.COMPLETED_ALL;
        	break;
        }
        return returnVal;
	}
		
	public int getCurScanState() {
        return getScanState(mDeviceType);
	}
	
	public int getCurMediaState() {
		return mMediaPlayer.getMediaState();
	}
	
	public int getCurListTotal() {
		return mListSize == -1 ? 0 : mListSize;
	}
	
	public int getPlayingPos() {
		return mPlayingPos;
	}
	
	public int getMediaListSize(int deviceType, int fileType) {
		ArrayList<FileNode> lists = mAllMediaList.getMediaList(deviceType, fileType);
		return lists.size();
	}
	
	public FileNode getItem(int pos) {
		ArrayList<FileNode> lists = mAllMediaList.getMediaList(mDeviceType, mFileType);
		if (lists.size() <= pos || pos < 0) {
			return null;
		}
		FileNode node = lists.get(pos);
		return node;
	}
	
	public FileNode getPlayItem() {
		FileNode fileNode = null;
		ArrayList<FileNode> lists = mAllMediaList.getMediaList(mPlayingDeviceType, mPlayingFileType);
		if (lists.size() > mPlayingPos && mPlayingPos >= 0) {
			fileNode = lists.get(mPlayingPos);
		}
		return fileNode;
	}
	
	public FileNode getDefaultItem() {
		boolean loadFlag = false;
		if (mPlayMusicFileNode == null) {
			loadFlag = true;
		} else {
			StorageBean bean = mAllMediaList.getStoragBean(mPlayMusicFileNode.getDeviceType());
			if (!bean.isMounted()) { // 设备没有挂载。
				mPlayMusicFileNode = null;
				loadFlag = true;
			} else { // 如果设备存在.
				if (!mPlayMusicFileNode.getFile().exists()) { // 判断文件是否存在。
					mPlayMusicFileNode = null;
					loadFlag = true;
				}
			}
		}
		
		if (loadFlag) {
			int[] deviceTypes = {getPlayingDeviceType(), mAllMediaList.getLastDeviceType(),
					DeviceType.USB1, DeviceType.USB2, DeviceType.COLLECT, DeviceType.FLASH};
			for (int deviceType : deviceTypes) {
				if (deviceType == DeviceType.NULL) {
					continue;
				}
				StorageBean bean = mAllMediaList.getStoragBean(deviceType);
				if (bean.isMounted() && bean.isId3ParseCompleted()) {
					ArrayList<FileNode> lists = mAllMediaList.getMediaList(deviceType, FileType.AUDIO);
					if (lists.size() > 0) {
						int position = getPlayingPos();
						position = position <= 0 ? 0 : position;
						position = position >= (lists.size() - 1) ? lists.size() - 1 : position;
						do {
							FileNode fileNode = lists.get(position);
							if (fileNode != null) {
								mPlayMusicFileNode = fileNode;
							} else {
								position++;
								position = position >= lists.size() ? 0 : position;
							}
						} while (mPlayMusicFileNode == null);
						break;
					}
				}
			}
		}
		return mPlayMusicFileNode;
	}
	
	public boolean collectMusic(FileNode fileNode) {
		boolean returnVal = true;
		if (fileNode != null) {
			//TODO 
			mAllMediaList.collectMediaFile(fileNode, null);
		}
		return returnVal;
	}
	
	public boolean deleteCollectedMusic(FileNode fileNode) {
		boolean returnVal = true;
		if (fileNode != null) {
			onDataChanged(ModeDef.MEDIA, MediaFunc.DELETE_FILE, DeleteState.DELETING, 0);
			mAllMediaList.uncollectMediaFile(fileNode, new OperateListener() {
				@Override
				public void onOperateCompleted(int operateValue, int progress,
						int resultCode) {
					loadData();
					if (resultCode == 0) {
						onDataChanged(ModeDef.MEDIA, MediaFunc.DELETE_FILE, DeleteState.SUCCESS, 0);
					} else {
						onDataChanged(ModeDef.MEDIA, MediaFunc.DELETE_FILE, DeleteState.FAIL, 0);
					}
				}
			});
		}
		return returnVal;
	}
	
	public boolean isCurItemSelected(int pos) {
		FileNode fileNode = getItem(pos);
		if (fileNode != null) {
			return fileNode.isSelected();
		}
		Log.e(TAG, "isCurItemSelected ERROR!! pos="+pos);
		return false;
	}

	public void selectFile(int pos, boolean isSelect) {
		FileNode fileNode = getItem(pos);
		if (fileNode != null) {
			fileNode.setSelected(isSelect);
		} else {
			Log.e(TAG, "selectFile ERROR!! pos="+pos);
		}
	}

	public void selectAll(boolean isSelect) {
		ArrayList<FileNode> lists = mAllMediaList.getMediaList(mDeviceType, mFileType);
		for (FileNode list : lists) {
			list.setSelected(isSelect);
		}
	}
	
	public void deleteStart() {
		ArrayList<FileNode>  allList = mAllMediaList.getMediaList(mDeviceType, mFileType);
        ArrayList<FileNode> selectedList = new ArrayList<FileNode>();
        for (FileNode fileNode : allList) {
            if (fileNode.isSelected()) {
                selectedList.add(fileNode);
                if (fileNode.isSamePathAndFrom(mPlayingFileNode)) {
                    setPlayState(PlayState.PAUSE);
                    setPlayState(PlayState.STOP);
                }
            }
        }
        if (selectedList.size() > 0) {
        	onDataChanged(ModeDef.MEDIA, MediaFunc.DELETE_FILE, DeleteState.DELETING, -1);
        	OperateListener listener = new OperateListener() {
    			@Override
    			public void onOperateCompleted(int operateValue, int progress,
    					int resultCode) {
    				onDataChanged(ModeDef.MEDIA, MediaFunc.DELETE_FILE, DeleteState.DELETING, progress);
    				if (progress == 100) {
    					loadData();
    					if (resultCode == 0) {
    						onDataChanged(ModeDef.MEDIA, MediaFunc.DELETE_FILE, DeleteState.SUCCESS, 0);
    					} else {
    						onDataChanged(ModeDef.MEDIA, MediaFunc.DELETE_FILE, DeleteState.FAIL, 0);
    					}
    				}
    				if (operateValue == OperateListener.OPERATE_UNCOLLECT) { // 取消收藏操作完成。
    		        	AllMediaList.instance(mContext).reLoadAllMedia(FileType.AUDIO);
    		        }
    			}
    		};
    		if (mDeviceType == DeviceType.COLLECT) {
    			mAllMediaList.uncollectMediaFiles(selectedList, listener);
    		} else {
                mAllMediaList.deleteMediaFiles(selectedList, listener);
    		}
        }
	}
	
	public void copyStart() {
		ArrayList<FileNode>  allList = mAllMediaList.getMediaList(mDeviceType, mFileType);
        ArrayList<FileNode> selectedList = new ArrayList<FileNode>();
        for (FileNode fileNode : allList) {
            if (fileNode.isSelected()) {
                selectedList.add(fileNode);
            }
        }
        if (selectedList.size() > 0) {
        	onDataChanged(ModeDef.MEDIA, MediaFuncEx.MEDIA_COPY_FILE, CopyState.COPYING, -1);
        	OperateListener listener = new OperateListener() {
    			@Override
    			public void onOperateCompleted(int operateValue, int progress,
    					int resultCode) {
    				onDataChanged(ModeDef.MEDIA, MediaFuncEx.MEDIA_COPY_FILE, CopyState.COPYING, progress);
    				if (progress == 100) {
    					if (resultCode == 0) {
    						onDataChanged(ModeDef.MEDIA, MediaFuncEx.MEDIA_COPY_FILE, CopyState.SUCCESS, 0);
    					} else {
    						onDataChanged(ModeDef.MEDIA, MediaFuncEx.MEDIA_COPY_FILE, CopyState.FAIL, 0);
    					}
    				}
    			}
    		};
    		if (mDeviceType == DeviceType.USB1 || mDeviceType == DeviceType.USB2) {
    			mAllMediaList.copyToLocal(selectedList, listener);
    		}
        }
	}
	
	private int findClient(int mode) {
		int ret = -1;
		ArrayList<MediaClient> clientList = mClientList;
		for (int i = 0; i < clientList.size(); i++) {
			if (mode == clientList.get(i).mMode) {
				ret = i;
				break;
			}
		}
		return ret;
	}
	
	// 初始化媒体
	public void initMedia(int mode, IMediaCallBack callBack) {
		try {
			ArrayList<MediaClient> clientList = mClientList;
			int index = findClient(mode);
			if (index != -1) { // 之前存在记录，便清除
				clientList.remove(index);
			}
			clientList.add(new MediaClient(mode, callBack));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 将数据分派到各个客户端
	 * @param mode 模块ID
	 * @param func 功能ID
	 * @param data 数据
	 */
	private void dispatchDataToClients(int mode, int func, int data0, int data1) {
		ArrayList<MediaClient> clientList = mClientList;
		synchronized (clientList) {
			for (int i = 0; i < clientList.size(); i++) {
				IMediaCallBack callBack = clientList.get(i).mCallBack;
				try {
					callBack.onDataChange(mode, func, data0, data1);
					
				} catch (RemoteException e) {
					Log.e(TAG, "dispatchDataToClients e=" + e.getMessage());
					Log.e(TAG, "dispatchDataToClients clientList.remove mode="
							+ clientList.get(i).mMode);
					clientList.remove(i);
				}
			}
		}
	}
}
