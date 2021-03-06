package com.haoke.util;

import android.app.Activity;
import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import com.amd.bt.BTMusic_IF;
import com.amd.bt.BT_IF;
import com.amd.media.AmdMediaManager;
import com.amd.media.MediaInterfaceUtil;
import com.haoke.aidl.ICarCallBack;
import com.haoke.aidl.IMediaCallBack;
import com.amd.radio.Radio_IF;
import com.amd.util.AmdConfig;
import com.amd.util.Source;
import com.haoke.bean.FileNode;
import com.haoke.btjar.main.BTDef.BTCallState;
import com.haoke.aidl.ICarUartCallBack;
import com.haoke.constant.MediaUtil;
import com.haoke.data.ModeSwitch;
import com.haoke.data.PlayStateSharedPreferences;
import com.haoke.define.CMSStatusDef.BootChangeSsourceStatus;
import com.haoke.define.CMSStatusDef.CMSStatusFuc;
import com.haoke.define.CMSStatusDef.CarplayCallState;
import com.haoke.define.CMSStatusDef.TBOXStatus;
import com.haoke.define.CMSStatusDef.TrafficRestriction;
import com.haoke.define.CMSStatusDef.VehicleStatus;
import com.haoke.define.McuDef;
import com.haoke.define.McuDef.McuFunc;
import com.haoke.define.McuDef.PowerState;
import com.haoke.define.SystemDef;
import com.haoke.define.SystemDef.ScreenState;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.constant.MediaUtil.MediaState;
import com.haoke.constant.MediaUtil.PlayState;
import com.haoke.constant.MediaUtil.RandomMode;
import com.haoke.constant.MediaUtil.RepeatMode;
import com.haoke.constant.MediaUtil.ScanState;
import com.haoke.serviceif.CarService_IF;
import com.haoke.serviceif.CarService_Listener;
import com.haoke.video.VideoSurfaceView;
import com.jsbd.util.Meter_IF;

public class Media_IF extends CarService_IF {

	private static final String TAG = "Media_IF"+AmdConfig.APP_VERSION_DATE;
	private static Media_IF mSelf = null;
	private Media_CarCallBack mCarCallBack = null; // CarService的回调处理
	private Media_CallBack mMediaCallBack = null; // MediaService的回调处理
	private IMediaCallBack.Stub mIMediaCallBack = null;
	private Activity mMusicActivity = null;
	private Activity mVideoActivity = null;
	private Activity mImageActivity = null;
	
	private int mAudioDevice = DeviceType.NULL;
	
	private AmdMediaManager mMediaManager = null;
	
	private boolean mServiceConn = false;

	public Media_IF() {
		mMode = com.haoke.define.ModeDef.MEDIA;
		
		mMediaManager = new AmdMediaManager();
		
		mCarCallBack = new Media_CarCallBack();
		mMediaCallBack = new Media_CallBack(mMediaManager.getMediaMode());

		// 以下处理服务回调
		mICallBack = new ICarCallBack.Stub() {
			@Override
			public void onDataChange(int mode, int func, int data)
					throws RemoteException {
				if (Source.isMcuMode(mode) && func == McuFunc.SOURCE) {
				} else {
					mCarCallBack.onDataChange(mode, func, data);
				}
			}
		};
		
		/*mIUartCallBack = new ICarUartCallBack.Stub() {
			@Override
			public void onUartDataChange(int arg0, int arg1, byte[] arg2)
					throws RemoteException {
				mCarCallBack.onUartDataChange(arg0, arg1, arg2);
			}
		};*/
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
		return mMediaManager.getMediaMode();
	}

	// 服务已经绑定成功，需要刷新动作
	@Override
	protected void onServiceConn() {
		mCarCallBack.onServiceConn();
		mServiceConn = true;
	}
	
	@Override
	protected void onServiceDisConn() {
		super.onServiceDisConn();
		DebugLog.v(TAG, "HMI------------onServiceDisConn");
		mServiceConn = false;
	}
	
	public boolean isServiceConnected() {
		return mServiceConn;
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
	
	//禁止UI层调用
	public void sendSouceChange(int source) {
		mCarCallBack.onDataChange(com.haoke.define.ModeDef.MCU, McuFunc.SOURCE, source);
		com.amd.bt.BTMusic_IF.getInstance().sendSouceChange(source);
		com.amd.radio.Radio_IF.getInstance().sendSouceChange(source);
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
	
	public static void resetSource(Context context) {
		DebugLog.d(TAG, "resetSource");
		if (!setCurSource(Source.NULL)) {
			setSourceToSettings(Source.NULL);
			sLastSource = Source.NULL;
			sCurSource = Source.NULL;
		}
	}
	
	private static int getSourceFromSettings() {
	    return PlayStateSharedPreferences.getSource(Source.NULL);
	}
	
	private static boolean setSourceToSettings(int source) {
	    return PlayStateSharedPreferences.saveSource(source);
	}

	public static int sLastSource = Source.NULL;
	private static int sCurSource = -1;
	// 设置当前源
	public static boolean setCurSource(int source) {
		boolean success = false;
		//int mcuSource = com.haoke.define.ModeDef.NULL;
        int exSource = Source.getDeviceFromSource(source);
        int exType = Source.getTypeFromSource(source);
		int lastSource = getCurSource();
		if (lastSource != source) {
            try {
                success = setSourceToSettings(source);
                if (success) {
                    getInstance().sendSouceChange(source);
                    sLastSource = lastSource;
                    sCurSource = source;
                    if (source == Source.NULL && Source.isBTMusicSource(sLastSource)) {
                        sLastSource = Source.NULL;
                    }
                    ModeSwitch.setCurSourceMode(source);
                }
                //mcuSource = Source.changeToMcuSource(source);
                // getInstance().mServiceIF.mcu_setCurSource(mcuSource);
                if (exSource != com.haoke.define.ModeDef.NULL) {
                    getInstance().mServiceIF.mcu_setCurSourceEx(exSource, exType);
                }
                DebugLog.d(TAG, "mcu_setCurSourceEx exSource: " + exSource + " && exType: " + exType);
            } catch (Exception e) {
                DebugLog.e(TAG, "setCurSource exception: " + e);
            }
            DebugLog.d(TAG, "setCurSource from: " + lastSource + " && to: " + source + "; success=" + success);
        } else {
            try {
                ModeSwitch.setCurSourceMode(source);
                int mcu_source = getInstance().mServiceIF.mcu_getCurSource();
                if (mcu_source != exSource && exSource != com.haoke.define.ModeDef.NULL) {
                    getInstance().mServiceIF.mcu_setCurSourceEx(exSource, exType);
                    DebugLog.d(TAG, "mcu_setCurSourceEx11 mcu_source: " + mcu_source +"; exSource: " + exSource + " && exType: " + exType);
                }
            } catch (Exception e) {
                DebugLog.e(TAG, "setCurSource exception11: " + e);
            }
        }
		return success;
	}

	// 获取当前源
	public static int getCurSource() {
		try {
		    
			if (sCurSource == -1) {
				sCurSource = getSourceFromSettings();
			}
			DebugLog.d(TAG, "getCurSource sCurSource=" + sCurSource);
			return sCurSource;
		} catch (Exception e) {
			DebugLog.e(TAG, "getCurSource error e="+e);
		}
		return Source.NULL;
	}
	
	public static boolean getMute() {
		try {
			int mute = getInstance().mServiceIF.eq_getMute();
			DebugLog.e(TAG, "getMute mute="+mute);
			return mute == 1 ? true : false;
		} catch (Exception e) {
			DebugLog.e(TAG, "getMute error e="+e);
		}
		return false;
	}
	
	public static boolean limitToPlayVideoWhenDrive() {
		boolean limitFlag = false;
		try {
			int status = getInstance().mServiceIF.getCMSStatus(CMSStatusFuc.TRAFFIC_RESTRICTION);
			limitFlag = (status == TrafficRestriction.ON);
		} catch (Exception e) {
			DebugLog.e(TAG, "limitToPlayVideoWhenDrive error e="+e);
		}
		return limitFlag;
	}
	
	public static int getCarSpeed() {
		int speed = 0;
		try {
			speed = getInstance().mServiceIF.getCMSStatus(CMSStatusFuc.CAR_SPEED);
		} catch (Exception e) {
			DebugLog.e(TAG, "getCarSpeed error e="+e);
		}
		return speed;
	}
	
	public static void cancelMute() {
		try {
			if (getMute()) {
				DebugLog.e(TAG, "getMute cancelMute");
				getInstance().mServiceIF.eq_setMute();
			}
		} catch (Exception e) {
			DebugLog.e(TAG, "cancelMute error e="+e);
		}
	}
	
	public static boolean getOnlyBtCallState() {
        try {
            /*int state = BT_IF.getCallState();
            if (state != BTCallState.IDLE) {
                DebugLog.d(TAG, "BT_IF.getCallState state="+state);
                return true;
            }*/
            boolean state = BT_IF.isTalking();
            DebugLog.d(TAG, "BT_IF.isTalking state="+state);
            return state;
        } catch (Exception e) {
            DebugLog.e(TAG, "getCallState error1 e="+e);
        }
        return false;
	}
	
	public static boolean getCallState() {
		if (getOnlyBtCallState()) {
		    return true;
		}
		try {
            int carplayState = getInstance().mServiceIF.getCMSStatus(CMSStatusFuc.CARPLAY_CALL_STS);
            DebugLog.d(TAG, "getCallState carplayState="+carplayState);
            if (carplayState != CarplayCallState.CARPLAY_NOT_CALLING) {
                return true;
            }
        } catch (Exception e) {
            DebugLog.e(TAG, "getCallState error2 e="+e);
        }
		try {
            int tboxState = getInstance().mServiceIF.getCMSStatus(CMSStatusFuc.TBOX_STATUS);
            DebugLog.d(TAG, "getCallState tboxState="+tboxState);
            if (tboxState != TBOXStatus.HANGUP) {
                return true;
            }
        } catch (Exception e) {
            DebugLog.e(TAG, "getCallState error2 e="+e);
        }
		return false;
	}
	
	public static boolean getScreenOn() {
	    try {
	        int state = getInstance().mServiceIF.getScreenState();
	        DebugLog.d(TAG, "getScreenOn state="+state);
	        return state == ScreenState.SCREEN_ON;
        } catch (Exception e) {
            DebugLog.e(TAG, "getScreenOn error e="+e);
        }
	    return true;
	}
	
	public static void setScreenOn() {
	    try {
            if (!getScreenOn()) {
                DebugLog.d(TAG, "setScreenOn SCREEN_ON");
                getInstance().mServiceIF.setScreenState(ScreenState.SCREEN_ON);
            }
        } catch (Exception e) {
            DebugLog.e(TAG, "setScreenOn error e="+e);
        }
	}
	
	private static int getCMSStatus(int func) {
	    try {
	        int status = getInstance().mServiceIF.getCMSStatus(func);
	        DebugLog.d(TAG, "getCMSStatus func="+func+"; status="+status);
            return status;
        } catch (Exception e) {
            DebugLog.e(TAG, "getCMSStatus error e="+e);
        }
	    return -1;
	}
	
	// 是否倒车
	public static boolean isCarReversing() {
	    int status = getCMSStatus(CMSStatusFuc.VEHICLE_STATUS);
	    return status == VehicleStatus.REVERSING;
	}
	
	// 是否已经切过源了
	public static boolean isBootSourceChanged() {
	    int status = getCMSStatus(CMSStatusFuc.BOOT_CHANGE_SOURCE);
	    return status == BootChangeSsourceStatus.STATUS_CHANGED;
	}
	
	public static boolean isPowerOn() {
	    try {
	        int status = getInstance().mServiceIF.mcu_getPowerState();
	        DebugLog.d(TAG, "isPowerOn status="+status);
	        return status == PowerState.POWER_ON;
        } catch (Exception e) {
            DebugLog.e(TAG, "isPowerOn error e="+e);
        }
	    return true;
	}
	
	/**
	 * @return null为carmanager没有收到mcu给的信号，true为断B+起来，false为断acc休眠起来
	 */
	public Boolean isFirstPower() {
		Boolean val = null;
		if (mServiceConn) {
			try {
				int state = getInstance().mServiceIF.mcu_getFirstPower();
				if (state == McuDef.FirstPower.NULL) {
					val = null;
				} else if (state == McuDef.FirstPower.POWER_FIRST) {
					val = true;
				} else {
					val = false;
				}
			} catch (Exception e) {
				DebugLog.e(TAG, "isFirstPower error e="+e);
			}
		}
		DebugLog.e(TAG, "isFirstPower mServiceConn="+mServiceConn+"; val="+val);
		return val;
	}
	
	public static boolean hasAudioOrBtFocus() {
	    int focus = 0;
	    if (Source.isBTMusicSource()) {
	        if (BTMusic_IF.getInstance().hasAudioFocus()) {
	            focus = 1;
	        }
	    } else if (Source.isAudioSource()) {
	        if (getInstance().mMediaManager.hasAudioFocus()) {
	            focus = 2;
	        }
	    }else if (Source.isRadioSource()){
	        if (Radio_IF.getInstance().hasAudioFocus()) {
                focus = 3;
            }
	    }
	    DebugLog.d(TAG, "hasAudioOrBtFocus focus="+focus);
		return focus > 0;
	}
	
	// 设置当前音频焦点
	public boolean requestAudioFocus(boolean request) {
		return mMediaManager.requestAudioFocus(request);
	}
	
	// 设置当前音频焦点
	public boolean requestTransientAudioFocus(boolean request) {
		return mMediaManager.requestTransientAudioFocus(request);
	}

	// 初始化媒体
	public void initMedia() {
		try {
			mMediaManager.initMedia(mMediaManager.getMediaMode(), mIMediaCallBack);
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
	
	// 只设音频源和焦点
	public void setAudioSourceAndRequestFocus(int deviceType) {
	    if (MediaInterfaceUtil.mediaCannotPlay()) {
            return;
        }
	    if (mMediaManager.requestAudioFocus(true)) {
	        setCurSource(Source.getAudioSource(deviceType));
	    }
	}
	
	// 播放指定设备的默认歌曲（在mode切源后调用），与getPlayDefaultIndex对应
	public boolean playDefault(int deviceType, int fileType) {
		try {
			DebugLog.d(TAG, "playDefault deviceType="+deviceType+"; fileType="+fileType);
			if (MediaInterfaceUtil.mediaCannotPlay()) {
				return false;
			}
			return mMediaManager.playDefault(deviceType, fileType);
		} catch (Exception e) {
			DebugLog.e(TAG, "playDefault" + e);
		}
		return false;
	}

	// 播放(list中的postion)
	public boolean play(int pos) {
		try {
			DebugLog.d(TAG, "play pos="+pos);
			if (MediaInterfaceUtil.mediaCannotPlay()) {
				return false;
			}
			return mMediaManager.play(pos);
		} catch (Exception e) {
			DebugLog.e(TAG, "play pos="+pos + e);
		}
		return false;
	}
	
	// 播放(文件路径)
	public boolean play(String filePath) {
		try {
			DebugLog.d(TAG, "play filePath="+filePath);
			if (MediaInterfaceUtil.mediaCannotPlay()) {
				return false;
			}
			return mMediaManager.play(filePath);
		} catch (Exception e) {
			DebugLog.e(TAG, "play filePath="+filePath + e);
		}
		return false;
	}
	
	// 播放(设备名【可能为收藏】，文件路径)
    public boolean play(int deviceType, String filePath) {
        try {
            DebugLog.d(TAG, "play deviceType="+deviceType+"; filePath="+filePath);
            if (MediaInterfaceUtil.mediaCannotPlay()) {
                return false;
            }
            return mMediaManager.play(deviceType, filePath);
        } catch (Exception e) {
            DebugLog.e(TAG, "play filePath="+filePath + e);
        }
        return false;
    }
	
	// 播放(FileNode)
	public boolean play(FileNode fileNode) {
		try {
			DebugLog.d(TAG, "play fileNode="+fileNode);
			if (MediaInterfaceUtil.mediaCannotPlay()) {
				return false;
			}
			return mMediaManager.play(fileNode);
		} catch (Exception e) {
			DebugLog.e(TAG, "play fileNode="+fileNode + e);
		}
		return false;
	}

	// 上一曲
	public boolean playPre() {
		try {
			DebugLog.d(TAG, "playPre");
			if (MediaInterfaceUtil.mediaCannotPlay()) {
				return false;
			}
			if (getPosition() > 10) {
                setPosition(0);
                if (getPlayState() != PlayState.PLAY) {
                    setPlayState(PlayState.PLAY);
                }
                return true;
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
			DebugLog.d(TAG, "playNext");
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
			DebugLog.d(TAG, "setPlayState state="+state);
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
		return getPlayState() == PlayState.PLAY;
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
	
	//modify bug 21133 begin
	// 设置长按关屏，关闭预览模式
    public void setPowerOff() {
        mMediaManager.setPowerOff();
    }
    //modify bug 21133 end
	
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
		    FileNode node = mMediaManager.getItem(pos);
		    if (node != null) {
	            return node.getCollect() == 0 ? false : true;
		    }
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
			DebugLog.e(TAG, "collectMusic fileNode="+fileNode + e);
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
			DebugLog.e(TAG, "deleteCollectedMusic fileNode="+fileNode + e);
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
	
	// 获得指定设备默认歌曲编号（在mode切源后调用）, 与playDefault对应
	public int getPlayDefaultIndex(int deviceType, int fileType) {
		try {
			return mMediaManager.getPlayDefaultIndex(deviceType, fileType);
		} catch (Exception e) {
			DebugLog.e(TAG, "getPlayDefaultIndex e=" + e);
		}
		return -1;
	}
	
	// 获得指定设备默认歌曲信息, 与playDefault对应
    public FileNode getPlayDefaultFileNode(int deviceType, int fileType) {
        try {
            return mMediaManager.getPlayDefaultFileNode(deviceType, fileType);
        } catch (Exception e) {
            DebugLog.e(TAG, "getPlayDefaultFileNode e=" + e);
        }
        return null;
    }
	
	public FileNode getDefaultItem() {
		return mMediaManager.getDefaultItem();
	}
	
	public int getLastPlayItem(int deviceType, int fileType) {
	    return mMediaManager.getLastPlayItem(deviceType, fileType);
	}
	
	public void finishVideoActivity() {
		try {
			if (getVideoActivity() != null) {
				getVideoActivity().finish();
				setVideoActivity(null);
			}
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------Finish Activity e=" + e.getMessage());
		}
	}
	
	public void sourceChanged(int source) {
		mMediaManager.sourceChanged(source);
		if (Source.isVideoSource(source)) {
			finishVideoActivity();
		}
	}
	
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
			DebugLog.e(TAG, "sendToDashbroad" + e);
		}
	}
	//-------------------------------仪表接口结束-----------------------------
	//-------------------------------发送数据给mcu 开始-----------------------------
	public void sendDataToMcu(byte addr, int cmd, byte[] data) {
       try {
            if (mServiceIF != null) {
                DebugLog.d(TAG, "sendDataToMcu addr:"+addr+" cmd:"+cmd);
                mServiceIF.mcu_sendDataToMcu(addr, cmd, data);
            } else {
                DebugLog.e(TAG, "mServiceIF==null");
            }
        } catch (Exception e) {
            DebugLog.e(TAG, "sendDataToMcu" + e);
        }
	}
	//-------------------------------发送数据给mcu 结束-----------------------------
	
    public boolean is3HPsystem() {
        boolean is3HP = false;
        try {
            if (mServiceIF != null) {
                is3HP = (mServiceIF.sys_getProjectID() == SystemDef.ProjectID.CAR_FE_3HP);
            } else {
                DebugLog.e(TAG, "mServiceIF==null");
            }
        } catch  (Exception e) {
            DebugLog.e(TAG, "is3HPsystem" + e);
        }
        DebugLog.d(TAG, "is3HPsystem is3HP: " + is3HP);
        return is3HP;
    }
}
