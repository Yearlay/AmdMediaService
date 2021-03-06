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
import com.amd.media.AudioFocus;
import com.amd.media.AudioFocus.AudioFocusListener;
import com.amd.util.Source;
import com.haoke.bean.FileNode;
import com.haoke.bean.ID3Parse;
import com.haoke.bean.StorageBean;
import com.haoke.constant.MediaUtil;
import com.haoke.constant.MediaUtil.CopyState;
import com.haoke.data.AllMediaList;
import com.haoke.data.LoadListener;
import com.haoke.data.OperateListener;
import com.haoke.constant.MediaUtil.DeleteState;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.constant.MediaUtil.MediaFunc;
import com.haoke.constant.MediaUtil.MediaState;
import com.haoke.constant.MediaUtil.OperateState;
import com.haoke.constant.MediaUtil.PlayState;
import com.haoke.constant.MediaUtil.RandomMode;
import com.haoke.constant.MediaUtil.RepeatMode;
import com.haoke.constant.MediaUtil.ScanState;
import com.haoke.service.MediaClient;
import com.haoke.spectrum.Spectrum;
import com.haoke.util.DebugLog;
import com.haoke.util.Media_IF;
import com.haoke.video.VideoSurfaceView;

import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

public class AmdMediaManager implements AmdMediaPlayerListener, AudioFocusListener {
    public static final int MEDIA_MODE_AUDIO = 1;
    public static final int MEDIA_MODE_VIDEO = 2;
    
	protected String TAG = "AmdMediaManager";
	protected Context mContext = null;
	private AmdMediaPlayer mMediaPlayer = null;
	private boolean mScanMode = false;
	private int mRepeatMode = RepeatMode.CIRCLE;
	private int mErrorCount = 0; // 连续播放错误的次数
	private int mPlayState = PlayState.STOP;
	private int mRecordPlayState = PlayState.STOP; // 用来记忆被抢焦点前的播放状态，便于恢复播放

	private ArrayList<MediaClient> mClientList = new ArrayList<MediaClient>();
	
	private boolean mIsPlayDefault = false;
	
    private int mCurSource = Source.NULL;
    
    private AllMediaList mAllMediaList;

	private AudioFocus mAudioFocus;
	private Spectrum mSpectrum; // 频谱类

	//----------------start------------------/
	//当前设置的设备信息
	private int mDeviceType = DeviceType.NULL;
	private int mFileType = FileType.NULL;
	//private int mListSize = -1;
	//播放器播放的设备信息
	private int mPlayingDeviceType = DeviceType.NULL;
	private int mPlayingFileType = FileType.NULL;
	private int mPlayingListSize = -1;
	private int mPlayingPos = -1;
	private int[] mRandomNums;
	private int mRandomListPos = 0; // 当前随机列表焦点位置
	private FileNode mPlayingFileNode = null;
	private FileNode mPlayMusicFileNode;
	//当前媒体框中的歌曲对应的设备
	private int mWidgetDeviceType = DeviceType.NULL;
	//------------------end----------------/
	
	private AudioManager mAudioManager;
	protected ComponentName mComponentName;
	
	protected int mMediaMode = MEDIA_MODE_AUDIO;
	private int mPrevOrNextFlag = 0;  // 0 is no flag; 1 is prev, 2 is next

	public AmdMediaManager() {
		mContext = MediaApplication.getInstance();
		
		mAudioFocus = new AudioFocus(mContext);
		mAudioFocus.registerListener(this);
		
		mAllMediaList = AllMediaList.instance(mContext);

		mMediaPlayer = new AmdMediaPlayer(mContext);
		mMediaPlayer.setMediaPlayerListener(this);

		mAllMediaList.registerLoadListener(mLoadListener);

		mRepeatMode = mAllMediaList.getPlayMode();
		if(mRepeatMode == RepeatMode.OFF) {
			mRepeatMode = RepeatMode.CIRCLE;
		}
		
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
    	DebugLog.d(TAG, "setDeviceAndFileType: deviceType="+deviceType+"; fileType="+fileType
    			+ "; mDeviceType="+mDeviceType+"; mFileType="+mFileType);
    	if (mDeviceType != deviceType || mFileType != fileType) {
    		mDeviceType = deviceType;
    		mFileType = fileType;
    		loadData();
    		resetPlayingData(false);
    	}
    }
    	
	public int getMediaMode() {
		return mMediaMode;
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
    
    private static final int MSG_DELAY_PLAYTIME = 1000;
    private static final int MSG_SAVE_PLAYTIME = 1;
    private static final int MSG_SAVE_PLAYSTATE = 2;
    private static final int MSG_PARSE_ID3_INFO = 3;
    private static final int MSG_ERROR_PLAY = 4;
    private static final int MSG_PLAY_OVER = 5;
    private Handler mHandler = new Handler() {
    	public void handleMessage(android.os.Message msg) {
    		switch (msg.what) {
			case MSG_SAVE_PLAYTIME:
				int time = getPosition();
				//savePlayTime(getPlayItem(), time);
				savePlayTime(mPlayingFileNode, time);
				DebugLog.d(TAG, "mHandler MSG_SAVE_PLAYTIME time="+time+"; mPlayingPos="+mPlayingPos+"; mPlayingListSize="+mPlayingListSize);
				removeMessages(MSG_SAVE_PLAYTIME);
				mHandler.sendEmptyMessageDelayed(MSG_SAVE_PLAYTIME, MSG_DELAY_PLAYTIME);
				break;
			case MSG_SAVE_PLAYSTATE:
				if (!hasMessages(msg.what)) {
					int fileType = msg.arg1;
					boolean playing = (msg.arg2 == 0 ? false : true);
					DebugLog.d(TAG, "mHandler  MSG_SAVE_PLAYSTATE fileType="+fileType+"; playing="+playing);
					mAllMediaList.savePlayState(fileType, playing);
				}
				break;
			case MSG_PARSE_ID3_INFO:
				if (true) {
					final int pos = msg.arg1;
					int[] index = {0, 0, 0, 0};
					ArrayList<FileNode> lists = mAllMediaList.getMediaList(mPlayingDeviceType, mPlayingFileType);
					if (lists.size() > 1) {
						getPreAndNextIndex(lists.size(), pos, index);
						DebugLog.d(TAG, "mHandler MSG_PARSE_ID3_INFO index="+index[0]+";"+index[1]+";"+index[2]+";"+index[3]);
						for (int i=0; i < index.length; i++) {
							FileNode fileNode = null;
							try {
								fileNode = lists.get(index[i]);
								ID3Parse.instance().parseID3(mContext, fileNode, null);
							} catch (Exception e) {
								DebugLog.e(TAG, "mHandler MSG_PARSE_ID3_INFO lists.get error! "+e);
							}
						}
					}
				}
				break;
			case MSG_ERROR_PLAY:
			    if (true) {
			        removeMessages(msg.what);
			        if (mPrevOrNextFlag == 1) {
                        pre(true); // 自动播放上一曲 
                    } else {
                        next(true); // 自动播放下一曲
                    }
			    }
			    break;
			case MSG_PLAY_OVER:
			    if (true) {
			        removeMessages(msg.what);
			        playOver();
			    }
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
	
	public int getLastPlayItem(int deviceType, int fileType) {
        DebugLog.v(TAG, "getLastPlayItem deviceType="+deviceType+"; fileType="+fileType);
        FileNode node = mAllMediaList.getPlayTime(deviceType, fileType);
        if (node == null || !node.isExist(mContext)) {
            return -1;
        }
        return changeFileNodeToIndex(node);
	}

	// 播放指定设备的默认歌曲（在mode切源后调用），与getPlayDefaultIndex对应
	public boolean playDefault(int deviceType, int fileType) {
		DebugLog.v(TAG, "playDefault deviceType="+deviceType+"; fileType="+fileType);

        mPrevOrNextFlag = 2;
        mErrorCount = 0;
		FileNode node = mAllMediaList.getPlayTime(deviceType, fileType);
		ArrayList<FileNode> lists = mAllMediaList.getMediaList(deviceType, fileType);
		if (node == null || !node.isExist(mContext)) {
			if (lists.size() > 0) {
				setPlayingData(deviceType, fileType, true);
				return playOther(null, 0);
			}
			DebugLog.v(TAG, "playDefault no song!");
			return false;
		}
		return play(node);
	}
	
	// 播放指定歌曲
	public boolean play(int pos) {
		DebugLog.v(TAG, "play pos=" + pos);
		setPlayingData(mDeviceType, mFileType, true);
		mPrevOrNextFlag = 2;
		mErrorCount = 0;
		return playOther(null, pos);
	}
	
	private boolean playDeviceTypeWithPath(int deviceType, String filePath) {
	    mPrevOrNextFlag = 2;
        mErrorCount = 0;
        if (deviceType == DeviceType.NULL) {
            deviceType = MediaUtil.getDeviceType(filePath);
        }
        boolean val = false;
        int fileType = FileType.AUDIO;
        if (deviceType == DeviceType.USB1 || deviceType == DeviceType.USB2 ||
                deviceType == DeviceType.FLASH || deviceType == DeviceType.COLLECT) {
            if (fileType == FileType.AUDIO || fileType == FileType.VIDEO) {
                FileNode fileNode = getFileNodeByFilePath(deviceType, filePath);
                if (fileNode != null) {
                    setPlayingData(deviceType, fileType, true);
                    val = playOther(fileNode, -1);
                }
            }
        }
        DebugLog.e(TAG, "playDeviceTypeWithPath val="+val+"! deviceType="+deviceType+"; fileType="+fileType);
        return val;
	}
	
	public boolean play(String filePath) {
		return playDeviceTypeWithPath(DeviceType.NULL, filePath);
	}
	
    public boolean play(int deviceType, String filePath) {
        return playDeviceTypeWithPath(deviceType, filePath);
    }
	
	public boolean play(FileNode fileNode) {
		DebugLog.v(TAG, "play fileNode=" + fileNode);
		mPrevOrNextFlag = 2;
        mErrorCount = 0;
		int deviceType = fileNode.getDeviceType();
		int fileType = fileNode.getFileType();
		if (deviceType == DeviceType.USB1 || deviceType == DeviceType.USB2 ||
				deviceType == DeviceType.FLASH || deviceType == DeviceType.COLLECT) {
			if (fileType == FileType.AUDIO || fileType == FileType.VIDEO) {
				setPlayingData(deviceType, fileType, true);
				return playOther(fileNode, -1);
			}
		}
		DebugLog.e(TAG, "play ERROR! deviceType="+deviceType+"; fileType="+fileType);
		return false;
	}
	
	private FileNode getFileNodeByFilePath(int deviceType, String filePath) {
		FileNode fileNode = null;
		if (deviceType == DeviceType.NULL) {
		    deviceType = MediaUtil.getDeviceType(filePath);
		}
		ArrayList<FileNode> lists = mAllMediaList.getMediaList(deviceType, FileType.AUDIO);
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
		DebugLog.v(TAG, "playOther fileNode=" + fileNode + "; pos="+pos);
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
		} else {
			Message msg = mHandler.obtainMessage(MSG_PARSE_ID3_INFO, mPlayingPos, 0);
			mHandler.sendMessageDelayed(msg, 300);
		}
		
		if (!requestAudioFocus(true)) {
			DebugLog.e(TAG, "playOther requestAudioFocus fail!");
			return false;
		}
		
		mHandler.removeMessages(MSG_ERROR_PLAY);
		mHandler.removeMessages(MSG_PLAY_OVER);
		
		if (mPlayingFileNode!=null) {
			if (!mPlayingFileNode.isSamePathAndFrom(node)) {
				stopRecordTimer();
				if (mPlayingFileNode.getDeviceType() == node.getDeviceType()) {
					savePlayTime(node, 0);
				} else if (getPlayState() == PlayState.PLAY) {
					savePlayTime(mPlayingFileNode, getPosition());
				}
			} else {
				ArrayList<FileNode> lists = mAllMediaList.getMediaList(node.getDeviceType(), node.getFileType());
				if (lists.size() == 1) { //fix bug 17652
					savePlayTime(node, 0);
				}
			}
		} else {
		    stopRecordTimer();
		}
		
		mPlayingFileNode = node;
		if (mPlayingDeviceType == DeviceType.NULL || mPlayingFileType == FileType.NULL) {
			setPlayingData(node.getDeviceType(), node.getFileType(), true);
		}
		changeSource(mPlayingDeviceType, mPlayingFileType);
		if (mPlayingFileType == FileType.AUDIO && mRepeatMode == RepeatMode.RANDOM) { // 随机开
			mRandomListPos = changeIndexToRandomPos(mPlayingPos);
		}

		if (node.getFileType() == FileType.AUDIO) {
			mPlayMusicFileNode = node;
		}
		String path = node.getFilePath(); // 获得播放路径
		mMediaPlayer.setFileType(mPlayingFileType);
		boolean returnVal = mMediaPlayer.setDataSource(path);
//		if (returnVal) {
//			onDataChanged(mMediaMode, MediaFunc.PLAY_STATE, PlayState.PLAY, 0);
//		}
		return returnVal;
	}
	
	// 上一曲（若force为true，表示强制换曲，不受单曲循环影响）
	public boolean pre(boolean force) {
		DebugLog.v(TAG, "pre force="+force);

		mPrevOrNextFlag = 1;
//		setPlayingData(mDeviceType, mFileType, false);
		int pos = 0;
		int repeatMode = ((mPlayingFileType == FileType.AUDIO) ? mRepeatMode : RepeatMode.CIRCLE);
		if (repeatMode == RepeatMode.OFF) { // 顺序播放
			pos = mPlayingPos;
			pos--;
			DebugLog.v(TAG, "pre pos:" + pos + ", total:" + mPlayingListSize);
			if (pos < 0) { // 播放结束
				DebugLog.v(TAG, "pre playOver 1");
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
		DebugLog.v(TAG, "next force="+force+"; mRepeatMode="+mRepeatMode);

		mPrevOrNextFlag = 2;
//		setPlayingData(mDeviceType, mFileType, false);
		int pos = 0;
		int repeatMode = ((mPlayingFileType == FileType.AUDIO) ? mRepeatMode : RepeatMode.CIRCLE);
		if (repeatMode == RepeatMode.OFF) { // 顺序播放
			pos = mPlayingPos;
			pos++;
			DebugLog.v(TAG, "next pos:" + pos + ", total:" + mPlayingListSize);
			if (pos > mPlayingListSize - 1) { // 播放结束
				DebugLog.v(TAG, "next playOver 1");
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
			}
			mMediaPlayer.reset();
		} catch (Exception e) {
			DebugLog.e(TAG, "resetMediaPlayer e=" + e);
		}
		
		if (mPlayState != PlayState.STOP) {
		    mPlayState = PlayState.STOP;
		    onDataChanged(mMediaMode, MediaFunc.PLAY_STATE, PlayState.STOP, 0);
		}
		setRecordPlayState(PlayState.STOP);
	}
	
	// 设置播放状态
	public void setPlayState(int state) {
	    int curState = getPlayState();
		DebugLog.e(TAG, "setPlayState state=" + state + "; curState="+curState);
		if (state == PlayState.PLAY) {
			if (mPlayingFileNode != null) {
                if (mMediaPlayer.getMediaState() == MediaState.PREPARED) {
                    if (requestAudioFocus(true)) {
                        changeSource(mPlayingDeviceType, mPlayingFileType); // 确保当前源在媒体
                        mMediaPlayer.start();
                        mPlayState = PlayState.PLAY;
                        onDataChanged(mMediaMode, MediaFunc.PLAY_STATE, getPlayState(), 0);
                    } else {
                        DebugLog.e(TAG, "setPlayState requestAudioFocus fail!");
                    }
                } else {
                    setPlayingData(mPlayingFileNode.getDeviceType(), mPlayingFileNode.getFileType(), true);
                    playOther(mPlayingFileNode, -1);
				}
	    	} else {
	    		FileNode fileNode = getDefaultItem();
	    		if (fileNode == null) {
	    			DebugLog.e(TAG, "setPlayState: no song!");
//	    			android.widget.Toast.makeText(mContext, "没有歌曲文件！", android.widget.Toast.LENGTH_SHORT).show();
	    		} else {
                    play(fileNode);
	    			/*mPlayingFileNode = fileNode;
	    			setPlayingData(fileNode.getDeviceType(), fileNode.getFileType(), true);
	    			mPlayingFileNode = null;
	    			//modify bug 20923 begin
	    			if (mPlayingPos >= 0) {
	                    playOther(null, mPlayingPos);
	    			} else {
	    			    playOther(fileNode, -1);
	    			}
	    			//modify bug 20923 end */
	    		}
	    	}
    		return;
		} else if (state == PlayState.PAUSE) {
		    if (curState == PlayState.STOP) {
		        return;
		    } else {
	            mMediaPlayer.pause();
	            mPlayState = PlayState.PAUSE;
	            mHandler.obtainMessage(MSG_SAVE_PLAYSTATE, mPlayingFileType, 0).sendToTarget();
		    }
		} else if (state == PlayState.STOP) {
		    setRecordPlayState(PlayState.STOP);
			mMediaPlayer.stop();
			mPlayState = PlayState.STOP;
			mHandler.obtainMessage(MSG_SAVE_PLAYSTATE, mPlayingFileType, 0).sendToTarget();
		}
		onDataChanged(mMediaMode, MediaFunc.PLAY_STATE, getPlayState(), 0);
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
	    if (mScanMode != enable) {
	        mScanMode = enable;
	        onDataChanged(mMediaMode, MediaFunc.MEDIA_SCAN_MODE, enable ? 1 : 0, 0);
        }
	}
	
	//modify bug 21133 begin
	// 长按关闭屏幕，关闭预览模式
    public void setPowerOff() {
        setScanMode(false);
    }
    //modify bug 21133 end
	
	// 获取scan模式
	public boolean getScanMode() {
		return mScanMode;
	}

	// 设置循环模式
	public void setRepeatMode(int mode) {
		mRepeatMode = mode;
		mAllMediaList.savePlayMode(mode);
		onDataChanged(mMediaMode, MediaFunc.REPEAT_MODE, mode, 0);
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

	// 获取播放总时间，毫秒
	public int getDuration() {
		return mMediaPlayer.getDuration();
	}

	// 设置当前播放时间，毫秒
	public void setPosition(int time) {
		mMediaPlayer.seekTo(time);
	}

	// 获取当前播放时间，毫秒
	public int getPosition() {
		return mMediaPlayer.getPosition();
	}

	// 设置视频层
	public void setVideoView(VideoSurfaceView surfaceView) {
		mMediaPlayer.setVideoView(surfaceView);
	}

	// 设置焦点获取
	private void focusGain() {
		mMediaPlayer.focusGain();
	}
	
	// 设置焦点丢失
	private void focusLossed() {
		mMediaPlayer.focusLossed();
		mHandler.removeMessages(MSG_ERROR_PLAY);
		mPrevOrNextFlag = 0;
		mErrorCount = 0;
	}

	// 播放结束处理
	private void playOver() {
		DebugLog.v(TAG, "playOver");
		
		mErrorCount = 5;
		mPlayingPos = -1;
		mRandomListPos = 0;
		mPlayingFileNode = null;
		clearPlayRecord();
		mMediaPlayer.stop();
		mPlayState = PlayState.STOP;
		mPrevOrNextFlag = 0;

		onDataChanged(mMediaMode, MediaFunc.PLAY_OVER, 0, 0);
	}

	// 播放开始
	@Override
	public void onStart() {
		DebugLog.v(TAG, "onStart");
        mPrevOrNextFlag = 2;
		startRecordTimer(); // 开始播放时间记录
		mHandler.obtainMessage(MSG_SAVE_PLAYSTATE, mPlayingFileType, 1).sendToTarget();
	}

	// 播放暂停
	@Override
	public void onPause() {
		DebugLog.v(TAG, "onPause");
		mPrevOrNextFlag = 0;
		stopRecordTimer(); // 停止播放时间记录
	}

	// 播放停止
	@Override
	public void onStop() {
		DebugLog.v(TAG, "onStop");
		stopRecordTimer(); // 停止播放时间记录
		//mHandler.obtainMessage(MSG_SAVE_PLAYSTATE, mPlayingFileType, 0).sendToTarget();
	}

	// 换曲/歌曲已播放
	@Override
	public void onPreparing() {
		DebugLog.v(TAG, "onPreparing");
		onDataChanged(mMediaMode, MediaFunc.PREPARING,
				mMediaPlayer.getMediaState(), 0);
	}

	// 换曲/歌曲已播放
	@Override
	public void onPrepared() {
		DebugLog.v(TAG, "onPrepared");
		mErrorCount = 0;
		//mPrevOrNextFlag = 0; // move to onCompletion & onPause & onStart
		//mBakMediaScanner.readId3(); // 准备好媒体信息

		// 恢复之前的播放时间
		if (true || mIsPlayDefault) {
			mIsPlayDefault = false;

			FileNode fileNode = mAllMediaList.getPlayTime(mPlayingDeviceType, mPlayingFileType);
			if (mPlayingFileNode.isSamePathAndFrom(fileNode)) {
				int playTime = 0;
				if (fileNode != null) {
					playTime = fileNode.getPlayTime();
				}
				DebugLog.v(TAG, "onPrepared playTime=" + playTime);
				if (playTime >= 1000) {
					setPosition(playTime);
				}
			}
		}
		
		if (hasAudioFocus()) {
			mPlayState = PlayState.PLAY;
		} else {
			mPlayState = PlayState.PAUSE;
		}

		onDataChanged(mMediaMode, MediaFunc.PREPARED,
				mMediaPlayer.getMediaState(), 0);
	}

	// 播放结束
	@Override
	public void onCompletion() {
		DebugLog.v(TAG, "onCompletion");
		clearPlayRecord();
		mIsPlayDefault = false;
		mPrevOrNextFlag = 0;
		if (!mScanMode) {
			next(false); // 自动播放下一曲
		}
		onDataChanged(mMediaMode, MediaFunc.COMPLETION, 0, 0);
	}

	// 定点播放成功
	@Override
	public void onSeekCompletion() {
		DebugLog.v(TAG, "onSeekCompletion");
		onDataChanged(mMediaMode, MediaFunc.SEEK_COMPLETION, 0, 0);
	}
	
	@Override
	public void onServerDied() {
	    DebugLog.e(TAG, "onServerDied mErrorCount:" + mErrorCount+"; mPlayState="+mPlayState);
	    stopRecordTimer();
	    mIsPlayDefault = false;
	    if (mPlayState == PlayState.PLAY) {
	        
	    }
	}

	// 播放错误
	@Override
	public void onError() {
		DebugLog.v(TAG, "onError mErrorCount:" + mErrorCount);
        onDataChanged(mMediaMode, MediaFunc.ERROR, 0, 0);
		clearPlayRecord();
		mIsPlayDefault = false;
		if (!mScanMode) {
			if (getPlayingFileType() != FileType.VIDEO && mPrevOrNextFlag > 0/* && mPlayState == PlayState.PLAY*/) {
				if (mPlayingListSize > (mErrorCount+1) && mErrorCount < 4) {
					mErrorCount++;
					mHandler.sendEmptyMessageDelayed(MSG_ERROR_PLAY, 1100);
					/*if (mPrevOrNextFlag == 1) {
						pre(true); // 自动播放上一曲						
					} else {
						next(true); // 自动播放下一曲
					}*/
				} else {
					DebugLog.v(TAG, "onError playOver");
					mHandler.sendEmptyMessageDelayed(MSG_PLAY_OVER, 1100);
//					playOver();
				}
			} else {
			    mHandler.sendEmptyMessageDelayed(MSG_PLAY_OVER, 1100);
//			    playOver();
			}
		}
	}

	// 文件错误
	@Override
	public void onIOException() {
		DebugLog.v(TAG, "onIOException mErrorCount:" + mErrorCount);
        onDataChanged(mMediaMode, MediaFunc.ERROR, 0, 0);
		clearPlayRecord();
		mIsPlayDefault = false;
		if (!mScanMode) {
			if (getPlayingFileType() != FileType.VIDEO && mPrevOrNextFlag > 0/* && mPlayState == PlayState.PLAY*/) {
				if (mPlayingListSize > (mErrorCount+1) && mErrorCount < 4) {
					mErrorCount++;
					mHandler.sendEmptyMessageDelayed(MSG_ERROR_PLAY, 1100);
					/*if (mPrevOrNextFlag == 1) {
						pre(true); // 自动播放上一曲
					} else {
						next(true); // 自动播放下一曲
					}*/
				} else {
					DebugLog.v(TAG, "onIOException playOver");
					mHandler.sendEmptyMessageDelayed(MSG_PLAY_OVER, 1100);
//					playOver();
				}
			} else {
			    mHandler.sendEmptyMessageDelayed(MSG_PLAY_OVER, 1100);
//                playOver();
            }
		}
	}

	@Override
	public void onSurfaceCreated() {
		DebugLog.v(TAG, "onSurfaceCreated");
	}

	@Override
	public void onSurfaceDestroyed() {
		DebugLog.v(TAG, "onSurfaceDestroyed");
	}

	private void onDataChanged(int mode, int func, int data0, int data1) {
		DebugLog.v(TAG, "onDataChanged mode=" + mode + ", func=" + func + ", data0="
				+ data0 + ", data1=" + data1);
		dispatchDataToClients(mode, func, data0, data1);
	}
	
	// 清除播放记录
	private void clearPlayRecord() {
		DebugLog.d(TAG, "clearPlayRecord mPlayingDeviceType="+mPlayingDeviceType+"; mPlayingFileType="+mPlayingFileType);
		stopRecordTimer();
		mAllMediaList.clearPlayTime(mPlayingDeviceType, mPlayingFileType);
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
        	ArrayList<FileNode> lists = mAllMediaList.getMediaList(fileNode.getDeviceType(), fileNode.getFileType());
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
			DebugLog.d(TAG, "mLoadListener onLoadCompleted deviceType="+deviceType+"; fileType="+fileType);
			// 处理数据加载完成的事件: 主要是处理数据。
			if (deviceType == mDeviceType && fileType == mFileType) {
				DebugLog.d(TAG, "mLoadListener onLoadCompleted MEDIA_LIST_UPDATE");
				loadData();
				onDataChanged(mMediaMode, MediaUtil.MediaFunc.MEDIA_LIST_UPDATE, deviceType, fileType);
			}
			if (deviceType == mPlayingDeviceType && fileType == mPlayingFileType) {
				setPlayingData(deviceType, fileType, false);
			}
			if (mMediaMode == MEDIA_MODE_AUDIO && fileType == FileType.AUDIO
			        && (deviceType == mAllMediaList.getLastDeviceType()
			        || mWidgetDeviceType == deviceType)) {
			    mPlayMusicFileNode = null;
			    AllMediaList.notifyUpdateAppWidgetByAudio();
			}
		}
		
		@Override
		public void onScanStateChange(StorageBean storageBean) {
			// 处理磁盘状态 和 扫描状态发生改变的状态： 主要是更新UI的显示效果。
			DebugLog.d(TAG, "mLoadListener onScanStateChange storageBean="+storageBean+"; mDeviceType="+mDeviceType+"; mPlayingDeviceType="+mPlayingDeviceType);
//			if (storageBean.getDeviceType() == mDeviceType) {
//				needToChange(storageBean.getDeviceType(), storageBean.getState());
//			}
			needToChange(storageBean.getDeviceType(), storageBean.getState());
			if (storageBean.getDeviceType() == mPlayingDeviceType) {
				if (!storageBean.isMounted()) {
					resetMediaPlayer();
					resetPlayingData(true);
					if (mRepeatMode == RepeatMode.RANDOM) {
						setRepeatMode(RepeatMode.CIRCLE);//插拔U盘，断随机模式记忆
					}
				}
			} else if (mPlayingDeviceType == DeviceType.COLLECT && mPlayingFileNode != null) {
				if (mPlayingFileNode.getFromDeviceType() == storageBean.getDeviceType()) {
					if (!storageBean.isMounted()) {
						resetMediaPlayer();
						resetPlayingData(true);
						if (mRepeatMode == RepeatMode.RANDOM) {
							setRepeatMode(RepeatMode.CIRCLE);//插拔U盘，断随机模式记忆
						}
					}
				}
			}
			if (storageBean.isId3ParseCompleted() && mMediaMode == MEDIA_MODE_AUDIO) {
				MediaInterfaceUtil.insertUsbAndScanComplete(storageBean.getDeviceType());
			}
		}
		
	};
	
	private void loadData() {
		//ArrayList<FileNode> lists = mAllMediaList.getMediaList(mDeviceType, mFileType);
		//mListSize = lists.size();
	}
	
	private void setPlayingData(int deviceType, int fileType, boolean force) {
		ArrayList<FileNode> lists = mAllMediaList.getMediaList(deviceType, fileType);
		int size = lists.size();
        DebugLog.d(TAG, "setPlayingData deviceType="+deviceType+"; fileType="+fileType+"; force="+force+"; size="+size);
        DebugLog.d(TAG, "setPlayingData mPlayingDeviceType="+mPlayingDeviceType+"; mPlayingFileType="+mPlayingFileType+"; mPlayingPos="+mPlayingPos);
		if (mPlayingDeviceType == deviceType && mPlayingFileType == fileType) {
			if (size != mPlayingListSize) {
				force = true;
			}
		} else if (mPlayingDeviceType != deviceType || mPlayingFileType != fileType) {
			force = true;
			mPlayingFileNode = null;
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
	
	private void resetPlayingData(boolean forceEx) {
	    boolean force = false;
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
		}
		if (force || forceEx) {
            int size = mAllMediaList.getMediaList(mPlayingDeviceType, mPlayingFileType).size();
            if (size != mPlayingListSize) {
                resetRandomNum(size);
            }
            mPlayingListSize = size;
            //FileNode fileNode = mAllMediaList.getPlayState(mPlayingDeviceType, mPlayingFileType);
            mPlayingPos = -1;//changeFileNodeToIndex(fileNode);
            mRandomListPos = 0;//changeIndexToRandomPos(mPlayingPos);
            mPlayingFileNode = null;
		}
	}
	
	private void needToChange(int deviceType, int state) {
		if (deviceType == mDeviceType) {
			if (state == StorageBean.EJECT) { // 非挂载的状态。
				onDataChanged(mMediaMode, MediaFunc.DEVICE_CHANGED, deviceType, 0);
			} else {
				// 文件扫描的状态发生改变。
				switch (state) {
				case StorageBean.MOUNTED:
					onDataChanged(mMediaMode, MediaFunc.SCAN_STATE, ScanState.IDLE, 0);
					break;
				case StorageBean.FILE_SCANNING:
					onDataChanged(mMediaMode, MediaFunc.DEVICE_CHANGED, deviceType, 1);
					onDataChanged(mMediaMode, MediaFunc.SCAN_STATE, ScanState.SCANNING, 0);
					break;
				case StorageBean.SCAN_COMPLETED:
					break;
				case StorageBean.ID3_PARSING:
					break;
				case StorageBean.ID3_PARSE_COMPLETED:
					onDataChanged(mMediaMode, MediaFunc.SCAN_STATE, ScanState.COMPLETED_ALL, 0);
					break;
				}
			}
		} else {
			if (state == StorageBean.EJECT) {
				onDataChanged(mMediaMode, MediaFunc.DEVICE_CHANGED, deviceType, 0);
			} else if (state == StorageBean.FILE_SCANNING) {//state == StorageBean.MOUNTED
				onDataChanged(mMediaMode, MediaFunc.DEVICE_CHANGED, deviceType, 1);
			}
		}

	}
	
	/**
	 * 只供播放时，文件类型的改变
	 */
	private void changeSource(int deviceType, int fileType) {
		// 判断是否通知MCU切源
		int source = Source.NULL;
		if (fileType == FileType.AUDIO) {
			source = Source.getAudioSource(deviceType);
		} else if (fileType == FileType.VIDEO) {
			source = Source.getVideoSource(deviceType);
		} else {
			return;
		}
        com.amd.bt.BT_IF.forceCloseBT();
		Media_IF.setCurSource(source);
	}
	
	/**
	 * 切换播放源，参数为 {@link com.amd.util.Source} <p>
	 * eg. {@link com.amd.util.Source#RADIO}
	 */
	public void sourceChanged(int source) {
		DebugLog.v(TAG, "sourceChanged source=" + source);
		RecordDevicePlay.instance().sourceChanged(source);
	}
	
	public boolean hasAudioFocus() {
		boolean ret = false;
		int audioFocusState = mAudioFocus.getFocusState();
		if (audioFocusState == AudioManager.AUDIOFOCUS_GAIN
				|| audioFocusState == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
			ret = true;

		return ret;
	}
	
	// 设置当前音频焦点
	public boolean requestAudioFocus(boolean request) {
		if (request && hasAudioFocus()) {
			return true;
		} else {
		    if (getRecordPlayState() == PlayState.PLAY) {
		        DebugLog.d(TAG, "requestAudioFocus reset RecordPlayState!");
		        setRecordPlayState(PlayState.STOP);
		    }
			return mAudioFocus.requestAudioFocus(request);
		}
	}
	
	// 设置当前音频焦点
	public boolean requestTransientAudioFocus(boolean request) {
		return mAudioFocus.requestTransientAudioFocus(request);
	}
	
	@Override
	public void audioFocusChanged(int state) {
		int playState = getPlayState();
		int recordPlayState = getRecordPlayState();
		DebugLog.v(TAG, "HMI------------audioFocusChanged state=" + state + "; playState="+playState+"; recordPlayState="+recordPlayState);

		switch (state) {
		case PlayState.PLAY:
			if (recordPlayState == PlayState.PLAY) {
				setPlayState(recordPlayState);
				// 清除标志，避免原本是暂停，每次抢焦点都进行播放
				setRecordPlayState(PlayState.STOP);
			}
			if (mComponentName != null) {
				mAudioManager.registerMediaButtonEventReceiver(mComponentName);
			}
			focusGain();
			break;
			
		case PlayState.PAUSE:
//			if (playState == PlayState.STOP) {
//				DebugLog.v(TAG, "HMI------------audioFocusChanged STOP 1");
//				return;
//			}
			setRecordPlayState(playState);
			setPlayState(PlayState.PAUSE);
			focusLossed();
			break;
			
		case PlayState.STOP:
			//MediaInterfaceUtil.resetMediaPlayStateRecord(mPlayingFileType == FileType.AUDIO ? Source.AUDIO : Source.VIDEO);
			if (mComponentName != null) {
				mAudioManager.unregisterMediaButtonEventReceiver(mComponentName);
			}
//			if (playState == PlayState.STOP) {
//				DebugLog.v(TAG, "HMI------------audioFocusChanged STOP 2");
//				return;
//			}
			setRecordPlayState(PlayState.STOP);
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
        	returnVal = ScanState.NO_MEDIA_STORAGE;
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
	    if (mDeviceType == DeviceType.NULL) {
	        return 0;
	    }
	    return mAllMediaList.getMediaList(mDeviceType, mFileType).size();
		//return mListSize == -1 ? 0 : mListSize;
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
		if(mPlayingPos >= 0) {
	        ArrayList<FileNode> lists = mAllMediaList.getMediaList(mPlayingDeviceType, mPlayingFileType);
	        if (lists.size() > mPlayingPos) {
	            fileNode = lists.get(mPlayingPos);
	        }
		}
		return fileNode;
	}
	
	// 获得指定设备默认歌曲编号（在mode切源后调用）, 与playDefault对应
	public int getPlayDefaultIndex(int deviceType, int fileType) {
		DebugLog.v(TAG, "getPlayDefaultIndex deviceType="+deviceType+"; fileType="+fileType);

		FileNode node = mAllMediaList.getPlayTime(deviceType, fileType);
		ArrayList<FileNode> lists = mAllMediaList.getMediaList(deviceType, fileType);
		if (node == null) {
			if (lists.size() > 0) {
				return 0;
			}
			DebugLog.v(TAG, "getPlayDefaultIndex no song!");
			return -1;
		}
		int index = -1;
    	for (int i=0; i<lists.size(); i++) {
    		FileNode list = lists.get(i);
    		if (list.isSame(node)) {
    			index = i;
    			break;
    		}
    	}
		return index;
	}
	
	// 获得指定设备默认歌曲信息, 与playDefault对应
    public FileNode getPlayDefaultFileNode(int deviceType, int fileType) {
        DebugLog.v(TAG, "getPlayDefaultFileNode deviceType="+deviceType+"; fileType="+fileType);

        FileNode node = mAllMediaList.getPlayTime(deviceType, fileType);
        ArrayList<FileNode> lists = mAllMediaList.getMediaList(deviceType, fileType);
        if (node == null) {
            if (lists.size() > 0) {
                return lists.get(0);
            }
            DebugLog.v(TAG, "getPlayDefaultFileNode no song!");
            return null;
        }
        
        return node;
    }
	
	public FileNode getDefaultItem() {
		boolean loadFlag = true;
		if (mPlayMusicFileNode != null && mPlayMusicFileNode.isExist(mContext)) {
			loadFlag = false;
		} else {
			mPlayMusicFileNode = null;
		}
		
		if (loadFlag) {
			int[] deviceTypes = {getPlayingDeviceType(), mAllMediaList.getLastDeviceType(),
					DeviceType.USB1, DeviceType.USB2, DeviceType.FLASH, DeviceType.COLLECT};
			for (int deviceType : deviceTypes) {
				if (deviceType == DeviceType.NULL) {
					continue;
				}
				StorageBean bean = mAllMediaList.getStoragBean(deviceType);
				if (bean.isMounted() && bean.isId3ParseCompleted()) {
					ArrayList<FileNode> lists = mAllMediaList.getMediaList(deviceType, FileType.AUDIO);
					if (lists.size() > 0) {
						int position = -1;
						FileNode playFileNode = null;
						if (getPlayingDeviceType() == deviceType) {
							position = getPlayingPos();
						}
						if (position == -1) {
							playFileNode = mAllMediaList.getPlayTime(deviceType, FileType.AUDIO);
						}
						if (playFileNode== null) {
							position = position <= 0 ? 0 : position;
							position = position >= (lists.size() - 1) ? lists.size() - 1 : position;
							playFileNode = lists.get(position);
						}
						if (playFileNode != null && playFileNode.isExist(mContext)) {
							mPlayMusicFileNode = playFileNode;
							break;
						}
						for (FileNode fileNode : lists) {
							if (fileNode != null && fileNode.isExist(mContext)) {
								mPlayMusicFileNode = fileNode;
								break;
							}
						}
						break;
					}
				}
			}
		}
		if (mPlayMusicFileNode != null) {
	        mWidgetDeviceType = mPlayMusicFileNode.getDeviceType();
		} else {
		    mWidgetDeviceType = DeviceType.NULL;
		}
		return mPlayMusicFileNode;
	}
	
	public boolean collectMusic(FileNode fileNode) {
		boolean returnVal = true;
		if (fileNode != null) {
		    onDataChanged(mMediaMode, MediaFunc.COLLECT_FILE, OperateState.OPERATING, 0);
			mAllMediaList.collectMediaFile(fileNode, new OperateListener() {
                @Override
                public void onOperateCompleted(int operateValue, int progress,
                        int resultCode) {
                    if (resultCode == 0) {
                        onDataChanged(mMediaMode, MediaFunc.COLLECT_FILE, OperateState.SUCCESS, 0);
                    } else {
                        onDataChanged(mMediaMode, MediaFunc.COLLECT_FILE, OperateState.FAIL, 0);
                    }
                }
            });
		}
		return returnVal;
	}
	
	public boolean deleteCollectedMusic(FileNode fileNode) {
		boolean returnVal = true;
		if (fileNode != null) {
		    if (fileNode.getDeviceType()==DeviceType.COLLECT
		            && fileNode.isSamePathAndFrom(mPlayingFileNode)) {
                mPlayMusicFileNode = null;
                setPlayState(PlayState.PAUSE);
                setPlayState(PlayState.STOP);
            }
			onDataChanged(mMediaMode, MediaFunc.UNCOLLECT_FILE, OperateState.OPERATING, 0);
			mAllMediaList.uncollectMediaFile(fileNode, new OperateListener() {
				@Override
				public void onOperateCompleted(int operateValue, int progress,
						int resultCode) {
					loadData();
					if (resultCode == 0) {
						onDataChanged(mMediaMode, MediaFunc.UNCOLLECT_FILE, OperateState.SUCCESS, 0);
					} else {
						onDataChanged(mMediaMode, MediaFunc.UNCOLLECT_FILE, OperateState.FAIL, 0);
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
		DebugLog.e(TAG, "isCurItemSelected ERROR!! pos="+pos);
		return false;
	}

	public void selectFile(int pos, boolean isSelect) {
		FileNode fileNode = getItem(pos);
		if (fileNode != null) {
			fileNode.setSelected(isSelect);
		} else {
			DebugLog.e(TAG, "selectFile ERROR!! pos="+pos);
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
                    mPlayMusicFileNode = null;
                    setPlayState(PlayState.PAUSE);
                    setPlayState(PlayState.STOP);
                } else if (mPlayingFileNode != null 
                    && mPlayingFileNode.isFromCollectTable()
                    && fileNode.isSame(mPlayingFileNode)) {
                    // this "else if" for fix bug 18848
                    mPlayMusicFileNode = null;
                    setPlayState(PlayState.PAUSE);
                    setPlayState(PlayState.STOP);
                }
            }
        }
        if (selectedList.size() > 0) {
        	onDataChanged(mMediaMode, MediaFunc.DELETE_FILE, DeleteState.DELETING, -1);
        	OperateListener listener = new OperateListener() {
    			@Override
    			public void onOperateCompleted(int operateValue, int progress,
    					int resultCode) {
    				onDataChanged(mMediaMode, MediaFunc.DELETE_FILE, DeleteState.DELETING, progress);
    				if (progress == 100) {
    					loadData();
    					if (resultCode == 0) {
    						onDataChanged(mMediaMode, MediaFunc.DELETE_FILE, DeleteState.SUCCESS, 0);
    					} else {
    						onDataChanged(mMediaMode, MediaFunc.DELETE_FILE, DeleteState.FAIL, 0);
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
        	onDataChanged(mMediaMode, MediaFunc.MEDIA_COPY_FILE, CopyState.COPYING, -1);
        	OperateListener listener = new OperateListener() {
    			@Override
    			public void onOperateCompleted(int operateValue, int progress,
    					int resultCode) {
    				onDataChanged(mMediaMode, MediaFunc.MEDIA_COPY_FILE, CopyState.COPYING, progress);
    				if (progress == 100) {
    					if (resultCode == 0) {
    						onDataChanged(mMediaMode, MediaFunc.MEDIA_COPY_FILE, CopyState.SUCCESS, 0);
    					} else {
    						onDataChanged(mMediaMode, MediaFunc.MEDIA_COPY_FILE, CopyState.FAIL, 0);
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
					DebugLog.e(TAG, "dispatchDataToClients e=" + e.getMessage());
					DebugLog.e(TAG, "dispatchDataToClients clientList.remove mode="
							+ clientList.get(i).mMode);
					clientList.remove(i);
				}
			}
		}
	}
	
	private void getPreAndNextIndex(int size, int pos, int[] index) {
		if (mRepeatMode == RepeatMode.RANDOM) {
			int randPos = changeIndexToRandomPos(pos);
			randPos--;
			if (randPos < 0) {
				randPos = size - 1;
			}
			index[1] = mRandomNums[randPos];
			randPos--;
			if (randPos < 0) {
				randPos = size - 1;
			}
			index[0] = mRandomNums[randPos];
			randPos += 2;
			randPos++;
			if (randPos >= size) {
				randPos = 0;
			}
			index[2] = mRandomNums[randPos];
			randPos++;
			if (randPos >= size) {
				randPos = 0;
			}
			index[3] = mRandomNums[randPos];
		} else {
			int circlePos = pos;
			circlePos--;
			if (circlePos < 0) {
				circlePos = size - 1;
			}
			index[1] = circlePos;
			circlePos--;
			if (circlePos < 0) {
				circlePos = size - 1;
			}
			index[0] = circlePos;
			circlePos += 2;
			circlePos++;
			if (circlePos >= size) {
				circlePos = 0;
			}
			index[2] = circlePos;
			circlePos++;
			if (circlePos >= size) {
				circlePos = 0;
			}
			index[3] = circlePos;
		}
	}
	
	private void savePlayTime(FileNode fileNode, int playTime) {
	    if (fileNode == null) {
	        return;
	    }
		mAllMediaList.savePlayTime(fileNode, playTime);
		if (fileNode.getFileType() == FileType.AUDIO) {
			RecordDevicePlay.instance().saveLastPlayDevice(fileNode.getDeviceType());
		}
	}
}
