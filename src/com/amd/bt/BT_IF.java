package com.amd.bt;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import com.amd.media.MediaInterfaceUtil;
import com.amd.util.Source;
import com.haoke.aidl.IBTCallBack;
import com.haoke.btjar.main.BTDef.BTConnState;
import com.haoke.btjar.main.BTDef.BTDeviceState;
import com.haoke.btjar.main.BTDef.BTOnOffState;
import com.haoke.btjar.main.BTDef.BTPairState;
import com.haoke.btjar.main.BTDef.BTSearchState;
import com.haoke.constant.MediaUtil.PlayState;
import com.haoke.service.BTMusicService;
import com.haoke.service.MediaService;
import com.haoke.serviceif.BTService_IF;
import com.haoke.serviceif.BTService_Listener;

public class BT_IF extends BTService_IF {

	private static final String TAG = "AMD_BT_IF";
	private static BT_IF mSelf = null;
	private BT_CallBack mBTCallBack = null;
	private boolean mServiceConn = false;

	public BT_IF() {
		mMode = com.haoke.define.ModeDef.BTMUSIC;
		mBTCallBack = new BT_CallBack();

		// 以下处理服务回调
		mICallBack = new IBTCallBack.Stub() {
			@Override
			public void onDataChange(int mode, int func, int data)
					throws RemoteException {
				mBTCallBack.onDataChange(mode, func, data);
			}
		};
	}

	// 获取接口实例
	synchronized public static BT_IF getInstance() {
		if (mSelf == null) {
			mSelf = new BT_IF();
		}
		return mSelf;
	}
	
	private void setBTSource() {
		Source.setBTMusicSource();
	}

	// 设置上下文
	public void setContext(Context context) {
		mContext = context;
	}

	// 注册BT服务回调（全局状态变化）
	public void registerBTCallBack(BTService_Listener listener) {
		mBTCallBack.registerBTCallBack(listener);
	}

	// 注销BT服务回调（全局状态变化）
	public void unregisterBTCallBack(BTService_Listener listener) {
		mBTCallBack.unregisterBTCallBack(listener);
	}

	// 注册BT服务回调（模块相关变化）
	public void registerModeCallBack(BT_Listener listener) {
		mBTCallBack.registerModeCallBack(listener);
	}

	// 注销BT服务回调（模块相关变化）
	public void unregisterModeCallBack(BT_Listener listener) {
		mBTCallBack.unregisterModeCallBack(listener);
	}

	@Override
	protected void onServiceConn() {
		Log.v(TAG, "HMI------------onServiceConnected");
		mBTCallBack.onServiceConn();
		mServiceConn = true;
	}
	
	@Override
	protected void onServiceDisConn() {
		super.onServiceDisConn();
		Log.v(TAG, "HMI------------onServiceDisConn");
		mServiceConn = false;
	}
	
	public boolean isServiceConnected() {
		return mServiceConn;
	}

	// 打开通道
	private void music_open() {
		try {
			Log.v(TAG, "music_open()");
			mServiceIF.music_open();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}

	// 关闭通道
	private void music_close() {
		try {
			Log.v(TAG, "music_close()");
			mServiceIF.music_close();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}
	
	// 播放
	public void music_play() {
		try {
			if (MediaInterfaceUtil.mediaCannotPlay()) {
				return;
			}
			boolean focus = BTMusic_IF.getInstance().requestAudioFocus(true);
			Log.v(TAG, "music_play() focus="+focus);
			if (focus) {
				music_open();
				setBTSource();
				mServiceIF.music_play();
			}
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}

	// 暂停
	public void music_pause() {
		try {
			Log.v(TAG, "music_pause()");
			mServiceIF.music_pause();
//			music_close();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}
	
	// 打开通道，适合焦点获取时调用
	public void music_openEx() {
		music_open();
	}
	
	// 关闭通道并暂停，适合焦点被抢时调用
	public void music_close_pause() {
		try {
			Log.v(TAG, "music_close_pause()");
			music_close();
			mServiceIF.music_pause();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}
	
	// 停止
	public void music_stop() {
		try {
			Log.v(TAG, "music_stop(), but use music_pause!");
			music_close();
			//mServiceIF.music_stop(); // bug 17089
			mServiceIF.music_pause();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}

	// 上一曲
	public void music_pre() {
		try {
			if (MediaInterfaceUtil.mediaCannotPlay()) {
				return;
			}
			boolean focus = BTMusic_IF.getInstance().requestAudioFocus(true);
			Log.v(TAG, "music_pre() focus="+focus);
			if (focus) {
				music_open();
				setBTSource();
				mServiceIF.music_pre();
			}
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}

	// 下一曲
	public void music_next() {
		try {
			if (MediaInterfaceUtil.mediaCannotPlay()) {
				return;
			}
			boolean focus = BTMusic_IF.getInstance().requestAudioFocus(true);
			Log.v(TAG, "music_next() focus="+focus);
			if (focus) {
				music_open();
				setBTSource();
				mServiceIF.music_next();
			}
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}

	// 播放状态
	public boolean music_isPlaying() {
	    boolean isPlaying = false;
		try {
			if (getConnState() == BTConnState.CONNECTED) {
			    isPlaying = mServiceIF.music_isPlaying();
			}
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		Log.d(TAG, "music_isPlaying isPlaying="+isPlaying);
		return isPlaying;
	}

	// 获取ID3标题
	public String music_getTitle() {
		try {
			return mServiceIF.music_getTitle();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return null;
	}

	// 获取ID3艺术家
	public String music_getArtist() {
		try {
			return mServiceIF.music_getArtist();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return null;
	}

	// 获取ID3专辑
	public String music_getAlbum() {
		try {
			return mServiceIF.music_getAlbum();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return null;
	}
	
	// 打开蓝牙
	public boolean openBT() {
		try {
			return mServiceIF.openBT();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return false;
	}

	// 关闭蓝牙
	public boolean closeBT() {
		try {
			return mServiceIF.closeBT();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return false;
	}
	
	// 获取蓝牙是否打开
	public boolean isBTEnable() {
		try {
			return mServiceIF.isBTEnable();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return false;
	}

	// 获取蓝牙开关状态
	public int getOnOffState() {
		try {
			return mServiceIF.getOnOffState();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return BTOnOffState.OFF;
	}

	// 获取扫描状态
	public int getSearchState() {
		try {
			return mServiceIF.getSearchState();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return BTSearchState.IDLE;
	}

	// 获取配对状态
	public int getPairState() {
		try {
			return mServiceIF.getPairState();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return BTPairState.UNPAIR;
	}

	// 获取连接状态
	public int getConnState() {
		try {
			return mServiceIF.getConnState();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return BTConnState.DISCONNECTED;
	}
	
	// 获取连接协议状态（跟getConnState的返回值一个定义）
	public boolean getAgreementState() {
	    try {
	        int avrcp = mServiceIF.getAvrcpState();
	        int a2dp = mServiceIF.getA2dpState();
	        Log.d(TAG, "getAgreementState avrcp="+avrcp+"; a2dp="+a2dp);
            return (avrcp == BTConnState.CONNECTED) && (a2dp == BTConnState.CONNECTED);
        } catch (Exception e) {
            Log.e(TAG, "getAgreementState e=" + e.getMessage());
        }
	    return false;
	}

	// 搜索外部设备
	public void searchDevices() {
		try {
			mServiceIF.searchDevices();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e111=" + e.getMessage());
		}
	}

	// 取消搜索外部设备
	public void cancelSearchDevices() {
		try {
			mServiceIF.cancelSearchDevices();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}

	// 连接设备
	public void connectDevice(int index) {
		try {
			mServiceIF.connectDevice(index);
		} catch (Exception e) {
    		Log.e(TAG, "HMI------------interface e="+e.getMessage());
        }
	}
	
	// 连接设备
	public void pairDevice(int index) {
		try {
			mServiceIF.pairDevice(index);
		} catch (Exception e) {
    		Log.e(TAG, "HMI------------interface e="+e.getMessage());
        }
	}
	
	
	// 连接设备
	public void connectLastDevice() {
		try {
			mServiceIF.connectLastDevice();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}

	// 断开设备
	public void disconnectDevice() {
		try {
			mServiceIF.disconnectDevice();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}

	// 取消配对设备
	public void dispairDevice(int index) {
		try {
			mServiceIF.deletePairedDevice(index);
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}
	
	// 取消ALL配对设备
	public void dispairAllDevice(int index) {
		try {
			for (int i = 0; i <= index; i++) {
				mServiceIF.deletePairedDevice(i);
			}
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e="+e.getMessage());
		}
	}

	// 获取设备总数
	public int getDeviceTotal() {
		try {
			int pairedTotal = mServiceIF.getPairedListTotal();
			int searchedTotal = mServiceIF.getSearchedListTotal();
			Log.e(TAG, "HMI------------getDeviceTotal pairedTotal=" + pairedTotal);
			Log.e(TAG, "HMI------------getDeviceTotal searchedTotal=" + searchedTotal);
			return pairedTotal + searchedTotal;
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return 0;
	}
	
	// 获取设备总数
	public int getPairedListTotal() {
		try {
			int pairedTotal = mServiceIF.getPairedListTotal();
			Log.e(TAG, "HMI------------getDeviceTotal pairedTotal=" + pairedTotal);
			return pairedTotal;
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return 0;
	}
	
	// 获取设备总数
	public int getSearchedListTotal() {
		try {
			int searchedTotal = mServiceIF.getSearchedListTotal();
			Log.e(TAG, "HMI------------getDeviceTotal searchedTotal=" + searchedTotal);
			return  searchedTotal;
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return 0;
	}

	// 获取设备名称
	public String getPairedDeviceName(int index) {
		try {
			return mServiceIF.getPairedDeviceName(index);
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return null;
	}

	// 获取设备名称
	public String getSearchedDeviceName(int index) {
		try {
			return mServiceIF.getSearchedDeviceName(index);
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return null;
	}

	// 获取设备地址
	public String getPairedDeviceAddr(int index) {
		try {
			return mServiceIF.getPairedDeviceAddr(index);
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return null;
	}

	// 获取设备地址
	public String getSearchedDeviceAddr(int index) {
		try {
			return mServiceIF.getSearchedDeviceAddr(index);
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return null;
	}

	// 获取设备状态
	public int getPairedDeviceState(int index) {
		try {
			return mServiceIF.getPairedDeviceState(index);
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return BTDeviceState.UNPAIR;
	}

	// 获取设备状态
	public int getSearchedDeviceState(int index) {
		try {
			return mServiceIF.getSearchedDeviceState(index);
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return BTDeviceState.UNPAIR;
	}

	// 设置蓝牙名称
	public void setBTName(String name) {
		try {
			mServiceIF.setBTName(name);
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}

	// 获取蓝牙名称)
	public String getBTName() {
		try {
			return mServiceIF.getBTName();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return null;
	}

	// 设置PIN码
	public void setPin(String pin) {
		try {
			mServiceIF.setPin(pin);
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}

	// 获取PIN码
	public String getPin() {
		try {
			return mServiceIF.getPin();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return null;
	}

	// 设置蓝牙可见性
	public void setDiscoverable(boolean isVisible) {
		try {
			mServiceIF.setDiscoverable(isVisible);
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}

	// 获取蓝牙可见性
	public boolean getDiscoverable() {
		try {
			return mServiceIF.getDiscoverable();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return false;
	}

	// 设置蓝牙是否自动连接
	public void setAutoConnect(boolean value) {
		try {
			mServiceIF.setAutoConnect(value);
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}

	// 获取蓝牙是否自动连接
	public boolean getAutoConnect() {
		try {
			return mServiceIF.getAutoConnect();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return false;
	}

	public int getMode() {
		// TODO Auto-generated method stub
		return mMode;
	}
	
	// 设置历史播放状态
	public void setRecordPlayState(int state) {
		try {
			MediaService.getInstance().getBtMusicManager().setRecordPlayState(state);
        } catch (Exception e) {
    		Log.e(TAG, "HMI------------setRecordPlayState e="+e.getMessage());
        }	
	}

	// 获取历史播放状态
	public int getRecordPlayState() {
		try {
			return MediaService.getInstance().getBtMusicManager().getRecordPlayState();
        } catch (Exception e) {
    		Log.e(TAG, "HMI------------getRecordPlayState e="+e.getMessage());
        }	
		return PlayState.STOP;
	}
	
	// 获取通话状态
	public static int getCallState() throws RemoteException{
        return getInstance().mServiceIF.getCallState();
	}
}
