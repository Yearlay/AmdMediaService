package com.haoke.ui.video;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.VideoView;

import com.amd.media.AudioFocus;
import com.amd.media.AudioFocus.AudioFocusListener;
import com.amd.media.MediaInterfaceUtil;
import com.amd.util.Source;
import com.haoke.aidl.IMediaCallBack;
import com.haoke.application.MediaApplication;
import com.haoke.bean.FileNode;
import com.haoke.bean.ID3Parse;
import com.haoke.bean.StorageBean;
import com.haoke.constant.MediaUtil;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.constant.MediaUtil.MediaFunc;
import com.haoke.constant.MediaUtil.PlayState;
import com.haoke.constant.MediaUtil.ScanState;
import com.haoke.data.AllMediaList;
import com.haoke.data.LoadListener;
import com.haoke.service.MediaClient;
import com.haoke.util.DebugClock;
import com.haoke.util.Media_IF;

public class VideoPlayController implements AudioFocusListener {

	private static final String TAG = "VideoPlayController";
	public static final int MEDIA_MODE_AUDIO = 1;
	public static final int MEDIA_MODE_VIDEO = 2;

	public static boolean isVideoPlaying = false;

	protected Context mContext = null;
	private MyVideoView mVideView;
	private int mCurPlayTime = 0; //当前播放媒体时间点
	private int mCurPlayVideoIndex; // 当前播放媒体index
	private FileNode mCurFileNode; // 当前播放的文件
	private AllMediaList mAllMediaList;
	private int mFileType = FileType.VIDEO;
	private int mDeviceType = DeviceType.NULL;
	private int mPlayState = PlayState.STOP;
	private int mPlayStateBeforeLoseFocus = PlayState.STOP;

	private int mPlayingDeviceType = DeviceType.NULL;
	private int mPlayingFileType = FileType.VIDEO;
	private int mListSize = -1;
	private int mPlayingListSize = -1;
	private ArrayList<MediaClient> mClientList = new ArrayList<MediaClient>();

	private AudioFocus mAudioFocus;
	// private AudioManager mAudioManager;
	// protected ComponentName mComponentName;

	private VideoPlayLayout videoLayout;
	private boolean mPlayDefaultVideo = false;

	protected int mMediaMode = MEDIA_MODE_VIDEO;

	public VideoPlayController(MyVideoView v) {
		if (v == null) {
			Log.e(TAG, "VideoView is null!!!");
			return;
		}
		mVideView = v;
		mContext = MediaApplication.getInstance();
		mAllMediaList = AllMediaList.instance(mContext);
		mAllMediaList.registerLoadListener(mLoadListener);

		mAudioFocus = new AudioFocus(mContext);
		mAudioFocus.registerListener(this);

		// mAudioManager =
		// (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
		// mComponentName = new ComponentName(mContext,
		// AmdMediaButtonReceiver.class);
	}

	public void setVideoPlayLayout(VideoPlayLayout layout) {
		videoLayout = layout;
	}
	
	public void playDefaultVideo(boolean defaultVideo){
		mPlayDefaultVideo = defaultVideo;
	}

	private LoadListener mLoadListener = new LoadListener() {

		@Override
		public void onLoadCompleted(int deviceType, int fileType) {
			Log.d(TAG, "mLoadListener onLoadCompleted deviceType=" + deviceType + "; fileType=" + fileType);
			// 处理数据加载完成的事件: 主要是处理数据。
			if (deviceType == mDeviceType && fileType == mFileType) {
				Log.d(TAG, "mLoadListener onLoadCompleted MEDIA_LIST_UPDATE");
				loadData();
				onDataChanged(mMediaMode, MediaUtil.MediaFunc.MEDIA_LIST_UPDATE, deviceType, fileType);
			}
			if (deviceType == mPlayingDeviceType && fileType == mPlayingFileType) {
				setPlayingData(deviceType, fileType, false);
			}
		}

		@Override
		public void onScanStateChange(StorageBean storageBean) {
			// 处理磁盘状态 和 扫描状态发生改变的状态： 主要是更新UI的显示效果。
			Log.d(TAG, "mLoadListener onScanStateChange storageBean=" + storageBean + "; mDeviceType=" + mDeviceType + "; mPlayingDeviceType="
					+ mPlayingDeviceType);
			needToChange(storageBean.getDeviceType(), storageBean.getState());
			if (storageBean.getDeviceType() == mPlayingDeviceType) {
				if (!storageBean.isMounted()) {
					resetMediaPlayer();
					resetPlayingData(true);
				}
			} else if (mPlayingDeviceType == DeviceType.COLLECT && mCurFileNode != null) {
				if (mCurFileNode.getFromDeviceType() == storageBean.getDeviceType()) {
					if (!storageBean.isMounted()) {
						//resetMediaPlayer();
						resetPlayingData(true);
					}
				}
			}
		}

	};

	private void resetPlayingData(boolean force) {
		if (mPlayingDeviceType == DeviceType.NULL || mPlayingFileType == FileType.NULL) {
			force = true;
		}
		if (mDeviceType == DeviceType.NULL || mFileType == FileType.NULL) {
			force = false;
		}
		if (force) {
			mPlayingDeviceType = mDeviceType;
			mPlayingFileType = mFileType;
			int size = mAllMediaList.getMediaList(mDeviceType, mFileType).size();

			mPlayingListSize = mListSize = size;
			mCurPlayVideoIndex = -1;// changeFileNodeToIndex(fileNode);
			mCurFileNode = null;
		}
	}

	public void resetMediaPlayer() {
		DebugClock debugClock = new DebugClock();
		try {
			mVideView.stopPlayback();
			mPlayState = PlayState.STOP;
			mVideView.suspend();
			mVideView.invalidate();
			//mVideView.setBackgroundColor(Color.BLACK);

		} catch (Exception e) {
			Log.e(TAG, "resetMediaPlayer e=" + e);
		}
		debugClock.calculateTime(TAG, "resetMediaPlayer");
	}

	private void loadData() {
		ArrayList<FileNode> lists = mAllMediaList.getMediaList(mDeviceType, mFileType);
		mListSize = lists.size();
	}

	private void onDataChanged(int mode, int func, int data0, int data1) {
		Log.v(TAG, "onDataChanged mode=" + mode + ", func=" + func + ", data0=" + data0 + ", data1=" + data1);
		dispatchDataToClients(mode, func, data0, data1);
	}

	private void dispatchDataToClients(int mode, int func, int data0, int data1) {
		ArrayList<MediaClient> clientList = mClientList;
		synchronized (clientList) {
			for (int i = 0; i < clientList.size(); i++) {
				IMediaCallBack callBack = clientList.get(i).mCallBack;
				try {
					callBack.onDataChange(mode, func, data0, data1);
				} catch (RemoteException e) {
					Log.e(TAG, "dispatchDataToClients e=" + e.getMessage());
					Log.e(TAG, "dispatchDataToClients clientList.remove mode=" + clientList.get(i).mMode);
					clientList.remove(i);
				}
			}
		}
	}

	private void setPlayingData(int deviceType, int fileType, boolean force) {
		Log.d(TAG, "setPlayingData deviceType=" + deviceType + "; fileType=" + fileType + "; force=" + force);
		Log.d(TAG, "setPlayingData mPlayingDeviceType=" + mPlayingDeviceType + "; mPlayingFileType=" + mPlayingFileType + "; mPlayingPos=" + mCurPlayVideoIndex);
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
			}
			mCurPlayVideoIndex = changeFileNodeToIndex(mCurFileNode);
			mCurFileNode = getPlayFileNode();
		}

	}

	// 设置设备类型和文件类型
	public void setDeviceAndFileType(int deviceType, int fileType) {
		Log.d(TAG, "setDeviceAndFileType: deviceType=" + deviceType + "; fileType=" + fileType + "; mDeviceType=" + mDeviceType + "; mFileType=" + mFileType);
		if (mDeviceType != deviceType || mFileType != fileType) {
			mDeviceType = deviceType;
			mFileType = fileType;
			loadData();
			resetPlayingData(false);
		}
	}

	public FileNode getPlayFileNode() {
		FileNode fileNode = null;
		ArrayList<FileNode> lists = mAllMediaList.getMediaList(mPlayingDeviceType, mPlayingFileType);
		if (lists.size() > mCurPlayVideoIndex && mCurPlayVideoIndex >= 0) {
			fileNode = lists.get(mCurPlayVideoIndex);
		}
		return fileNode;
	}

	public FileNode getPlayFileNode(int pos) {
		ArrayList<FileNode> lists = mAllMediaList.getMediaList(mDeviceType, mFileType);
		if (lists.size() <= pos || pos < 0) {
			return null;
		}
		FileNode node = lists.get(pos);
		return node;
	}

	private FileNode getFileNodeByFilePath(String filePath) {
		FileNode fileNode = null;
		int deviceType = MediaUtil.getDeviceType(filePath);
		int fileType = MediaUtil.getMediaType(filePath);
		ArrayList<FileNode> lists = mAllMediaList.getMediaList(deviceType, fileType);
		for (int i = 0; i < lists.size(); i++) {
			FileNode node = lists.get(i);
			if (node.getFilePath().equals(filePath)) {
				fileNode = node;
			}
		}
		return fileNode;
	}

	private int changeFileNodeToIndex(FileNode fileNode) {
		int index = -1;
		if (fileNode != null) {
			ArrayList<FileNode> lists = mAllMediaList.getMediaList(mPlayingDeviceType, mPlayingFileType);
			for (int i = 0; i < lists.size(); i++) {
				FileNode list = lists.get(i);
				if (list.isSame(fileNode)) {
					index = i;
					break;
				}
			}
		}
		return index;
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
			} else if (state == StorageBean.FILE_SCANNING) {// state ==
															// StorageBean.MOUNTED
				onDataChanged(mMediaMode, MediaFunc.DEVICE_CHANGED, deviceType, 1);
			}
		}

	}

	public static boolean setCurSource(int deviceType) {
		int source = Source.NULL;
		if (deviceType == DeviceType.FLASH) {
			source = Source.VIDEO_FLASH;
		} else if (deviceType == DeviceType.USB1) {
			source = Source.VIDEO_USB1;
		} else if (deviceType == DeviceType.USB2) {
			source = Source.VIDEO_USB2;
		} else if (deviceType == DeviceType.COLLECT) {
			source = Source.VIDEO_COLLECT;
		}
		return Media_IF.setCurSource(source);
	}

	// 获取当前源
	public int getCurSource() {
		return Media_IF.getCurSource();
	}

	public boolean getMute() {
		return Media_IF.getMute();
	}

	public boolean limitToPlayVideoWhenDrive() {
		return Media_IF.limitToPlayVideoWhenDrive();
	}

	public int getCarSpeed() {
		return Media_IF.getCarSpeed();
	}

	public void cancelMute() {
		Media_IF.cancelMute();
	}

	public boolean getCallState() {
		return Media_IF.getCallState();
	}

	public boolean setVideoView(VideoView v) {
		if (v == null) {
			return false;
		} else {
			mVideView = (MyVideoView) v;
		}

		return true;
	}

	public VideoView getVideoView() {
		return mVideView;
	}

	public void playOrPause(boolean playOrPause) {
		// requestAudioFocus(true);
		// Log.d("luke",Log.getStackTraceString(new Throwable()));
		Log.e("luke", "playOrPause: " + playOrPause);

		if (playOrPause) {
			if (!requestAudioFocus(true)) {
				Log.e(TAG, "playOrPause requestAudioFocus fail!");
			}
			mPlayState = PlayState.PLAY;
			mVideView.start();
		} else {
			mPlayState = PlayState.PAUSE;
			mVideView.pause();
		}
		videoLayout.updatePlayState(!playOrPause);
		isVideoPlaying = playOrPause;
	}

	/**
	 * 如果 fileNode为null，则pos生效
	 */
	private boolean playOther(FileNode fileNode, int index) {
		if (MediaInterfaceUtil.mediaCannotPlay()) {
			return false;
		}
		FileNode node = null;

		if (fileNode == null) {
			ArrayList<FileNode> lists = mAllMediaList.getMediaList(mPlayingDeviceType, mPlayingFileType);
			if (lists.size() <= index || index < 0) {
				return false;
			}
			node = lists.get(index);
			if (node == null) {
				return false;
			}
		} else {
			node = fileNode;
		}
		isVideoPlaying = true;
		
		int playTime = node.getPlayTime();
		if(mPlayDefaultVideo || (mCurFileNode != null && mCurFileNode.isSamePathAndFrom(node))){
			node.setPlayTime(playTime);
		} else {
			node.setPlayTime(0); //从头开始播放
		}
		mPlayDefaultVideo = false;
		mCurFileNode = node;
		mCurPlayVideoIndex = changeFileNodeToIndex(node);
		videoLayout.setCurFileNode(mCurFileNode);
		Log.e("luke", "----playOther  node filetype " + node.getFileType() + " ,devicetype: " + node.getDeviceType() + " ,playingtime: " + node.getPlayTime());
		
		videoLayout.getLoadingAnimation().setVisibility(View.VISIBLE);
		
		Message msg = mHandler.obtainMessage(MSG_PARSE_ID3_INFO, mCurPlayVideoIndex, 0);
		mHandler.sendMessageDelayed(msg, 300);

		if (!requestAudioFocus(true)) {
			Log.e(TAG, "playOther requestAudioFocus fail!");
			isVideoPlaying = false;
			return false;
		}

		if (mPlayingDeviceType == DeviceType.NULL || mPlayingFileType == FileType.NULL) {
			setPlayingData(node.getDeviceType(), node.getFileType(), true);
		}
		changeSource(mPlayingDeviceType, mPlayingFileType);

		final String path = node.getFilePath(); // 获得播放路径

		mPlayState = PlayState.PLAY;
		resetMediaPlayer();
		DebugClock debugClock = new DebugClock();
		mVideView.setVideoPath(path); // 主要的耗时操作
		debugClock.calculateTime(TAG, "mVideView setVideoPath");
		Log.v("luke", "Play done ,waiting OnPrepare " + " ,----Time: " + node.getPlayTime() + "  ,filesize: " + node.getFile().length());
		setCurSource(node.getDeviceType());
		return true;
	}

	public boolean play(int index) {
		Log.v(TAG, "play Video index=" + index);
		setPlayingData(mDeviceType, mFileType, true);
		return playOther(null, index);
	}

	public boolean play(String filePath) {
		Log.v(TAG, "play Video filePath: " + filePath);
		int deviceType = MediaUtil.getDeviceType(filePath);
		int fileType = MediaUtil.getMediaType(filePath);
		if (deviceType == DeviceType.USB1 || deviceType == DeviceType.USB2 || deviceType == DeviceType.FLASH || deviceType == DeviceType.COLLECT) {
			if (fileType == FileType.VIDEO) {
				FileNode fileNode = getFileNodeByFilePath(filePath);
				if (fileNode != null) {
					setPlayingData(deviceType, fileType, true);
					return playOther(fileNode, -1);
				}
			}
		}
		Log.e(TAG, "play ERROR! deviceType=" + deviceType + "; fileType=" + fileType);
		return false;
	}

	public boolean play(FileNode fileNode) {
		Log.v("luke", "play Video fileNode=" + fileNode);
		int deviceType = fileNode.getDeviceType();
		int fileType = fileNode.getFileType();
		if (deviceType == DeviceType.USB1 || deviceType == DeviceType.USB2 || deviceType == DeviceType.FLASH || deviceType == DeviceType.COLLECT) {
			if (fileType == FileType.VIDEO) {
				setPlayingData(deviceType, fileType, true);
				return playOther(fileNode, -1);
			}
		}
		Log.e(TAG, "play ERROR! deviceType=" + deviceType + "; fileType=" + fileType);
		return false;
	}

	public boolean hasAudioFocus() {
		boolean ret = false;
		int audioFocusState = mAudioFocus.getFocusState();
		if (audioFocusState == AudioManager.AUDIOFOCUS_GAIN || audioFocusState == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
			ret = true;

		return ret;
	}

	// 设置当前音频焦点
	public boolean requestAudioFocus(boolean request) {
		if (request && hasAudioFocus()) {
			return true;
		} else {
			return mAudioFocus.requestAudioFocus(request);
		}
	}

	// 设置当前音频焦点
	public boolean requestTransientAudioFocus(boolean request) {
		return mAudioFocus.requestTransientAudioFocus(request);
	}

	// 上一曲
	public boolean playPre() {
		int pos = 0;
		pos = mCurPlayVideoIndex;
		pos--;
		Log.v(TAG, "pre pos:" + pos + ", total:" + mPlayingListSize);
		if (pos < 0) {
			pos = mPlayingListSize - 1;
		}

		return playOther(null, pos);
	}

	// 下一曲
	public boolean playNext() {
		int pos = 0;
		pos = mCurPlayVideoIndex;
		pos++;
		Log.v(TAG, "next pos:" + pos + ", total:" + mPlayingListSize);
		if (pos > mPlayingListSize - 1) {
			pos = 0;
		}
		return playOther(null, pos);
	}

	// 获取是否播放状态
	public boolean isPlayState() {
		return mVideView.isPlaying();
	}

	// 获取播放总时间
	public int getDuration() {
		try {
			if (mVideView != null) {
				return mVideView.getDuration() / 1000;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	// 设置播放当前时间
	public boolean setPosition(int time) {
		try {
			if (mVideView != null) {
				mVideView.seekTo(time * 1000);
				mHandler.removeMessages(MSG_SAVE_PLAYTIME);
				mHandler.sendEmptyMessage(MSG_SAVE_PLAYTIME);
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	// 获取播放当前时间
	public int getPosition() {
		try {
			if (mVideView != null) {
				return mVideView.getCurrentPosition() / 1000;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	public boolean setVideoIndex(int index) {
		ArrayList<FileNode> lists = mAllMediaList.getMediaList(mDeviceType, mFileType);
		if (index >= 0 && index < lists.size()) {
			mCurPlayVideoIndex = index;
			mCurFileNode = lists.get(index);
			return true;
		}
		return false;
	}

	public FileNode getCurFileNode() {
		return mCurFileNode;
	}
	
	public int getCurPlayTime(){
		return mCurPlayTime;
	}

	private int playTimeSaveCount = 0;
	private void savePlayTime(FileNode fileNode, int playTime) {
		if (fileNode == null) {
			return;
		}
		mCurPlayTime = playTime;
		playTimeSaveCount ++;
		if(playTimeSaveCount >= 3 ){
			playTimeSaveCount = 0;
			mAllMediaList.savePlayTime(fileNode, playTime);
		}
	}

	public Handler getControllerHandler() {
		return mHandler;
	}

	private static final int MSG_DELAY_PLAYTIME = 1000;
	private static final int MSG_SAVE_PLAYTIME = 1;
	private static final int MSG_SAVE_PLAYSTATE = 2;
	private static final int MSG_PARSE_ID3_INFO = 3;
	public static final int MSG_PLAY = 1001;
	public static final int MSG_PAUSE = 1002;
	public static final int MSG_PREPLAY = 1003;
	public static final int MSG_NEXTPLAY = 1004;
	public static final int VR_PLAY_STATE = 1005;
	public static final int VR_PAUSE_STATE = 1006;

	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case VR_PLAY_STATE:
				mPlayStateBeforeLoseFocus = PlayState.PLAY;
				break;
			case VR_PAUSE_STATE:
				mPlayStateBeforeLoseFocus = PlayState.PAUSE;
				break;
			case MSG_PLAY:
				Log.d(TAG, "mHandler MSG_PLAY");
				if (mVideView != null) {
					playOrPause(true);
				}
				break;

			case MSG_PAUSE:
				Log.d(TAG, "mHandler MSG_PAUSE");
				if (mVideView != null) {
					playOrPause(false);
				}
				break;

			case MSG_PREPLAY:
				Log.d(TAG, "mHandler MSG_PREPLAY");
				if (mVideView != null) {
					playPre();
				}
				break;
			case MSG_NEXTPLAY:
				Log.d(TAG, "mHandler MSG_NEXTPLAY");
				if (mVideView != null) {
					playNext();
				}
				break;
			case MSG_SAVE_PLAYTIME:
				int time = getPosition();
				savePlayTime(getPlayFileNode(), time);
				Log.e("luke","----MSG_SAVE_PLAYTIME: " + time);
				removeMessages(MSG_SAVE_PLAYTIME);
				sendEmptyMessageDelayed(MSG_SAVE_PLAYTIME, MSG_DELAY_PLAYTIME);
				break;
			case MSG_SAVE_PLAYSTATE:
				if (!hasMessages(msg.what)) {
					int fileType = msg.arg1;
					boolean playing = (msg.arg2 == 0 ? false : true);
					Log.d(TAG, "mHandler  MSG_SAVE_PLAYSTATE fileType=" + fileType + "; playing=" + playing);
					mAllMediaList.savePlayState(fileType, playing);
				}
				break;
			case MSG_PARSE_ID3_INFO:
				if (true) {
					final int pos = msg.arg1;
					int[] index = { 0, 0, 0, 0 };
					ArrayList<FileNode> lists = mAllMediaList.getMediaList(mPlayingDeviceType, mPlayingFileType);
					if (lists.size() > 1) {
						getPreAndNextIndex(lists.size(), pos, index);
						Log.d(TAG, "mHandler MSG_PARSE_ID3_INFO index=" + index[0] + ";" + index[1] + ";" + index[2] + ";" + index[3]);
						for (int i = 0; i < index.length; i++) {
							FileNode fileNode = null;
							try {
								fileNode = lists.get(index[i]);
								ID3Parse.instance().parseID3(mContext, fileNode, null);
							} catch (Exception e) {
								Log.e(TAG, "mHandler MSG_PARSE_ID3_INFO lists.get error! " + e);
							}
						}
					}
				}
				break;
			}
		};
	};

	// 开始播放时间记录
	public void startRecordTimer() {
		Log.e(TAG, "start timer");
		mHandler.removeMessages(MSG_SAVE_PLAYTIME);
		mHandler.sendEmptyMessageDelayed(MSG_SAVE_PLAYTIME, MSG_DELAY_PLAYTIME);
	}

	// 停止播放时间记录
	public void stopRecordTimer() {
		Log.e(TAG, "Stop timer");
		mHandler.removeMessages(MSG_SAVE_PLAYTIME);
	}

	private void getPreAndNextIndex(int size, int pos, int[] index) {
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
		Media_IF.setCurSource(source);
	}

	// 获取播放状态
	public int getPlayState() {
		if (isPlayState()) {
			return PlayState.PLAY;
		} else {
			return mPlayState;
		}
	}

	private boolean playStateTransformation(int state) {
		return ((state == PlayState.PLAY) ? true : false);
	}

	@Override
	public void audioFocusChanged(int state) {
		// TODO Auto-generated method stub
		int playState = getPlayState();
		Log.v("luke", "audioFocusChanged state=" + state + "; playState=" + playState + "; mPlayStateBeforeLoseFocus=" + mPlayStateBeforeLoseFocus);
		switch (state) {
		case PlayState.PLAY: // 获得焦点
			if (mPlayStateBeforeLoseFocus == PlayState.PLAY) {
				mPlayStateBeforeLoseFocus = PlayState.STOP;
				playOrPause(true);
				// videoLayout.updatePlayState(false);
			} else if (mPlayStateBeforeLoseFocus == PlayState.PAUSE) {
				playOrPause(false);
				// videoLayout.updatePlayState(true);
			} else {

			}
			mContext.registerReceiver(videoLayout.getVideoLayoutReciver(), new IntentFilter(Intent.ACTION_MEDIA_BUTTON));
			break;
		case PlayState.PAUSE: // 失去焦点
			mPlayStateBeforeLoseFocus = playState;
			videoLayout.setBeforePlaystate(playStateTransformation(mPlayStateBeforeLoseFocus));
			Log.v("luke", "audioFocusChanged setBeforePlaystate " + videoLayout.getBeforePlaystate());
			playOrPause(false);
			// videoLayout.updatePlayState(true);
			break;
		case PlayState.STOP:
			mContext.unregisterReceiver(videoLayout.getVideoLayoutReciver());
			mPlayStateBeforeLoseFocus = PlayState.STOP;
			// mVideView.stopPlayback();
			// videoLayout.setBeforePlaystate(playStateTransformation(mPlayStateBeforeLoseFocus));
			break;
		}

	}

}