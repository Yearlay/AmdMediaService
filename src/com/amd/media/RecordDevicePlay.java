package com.amd.media;

import java.io.File;
import java.util.ArrayList;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.amd.util.AmdConfig;
import com.amd.util.Source;
import com.haoke.application.MediaApplication;
import com.haoke.bean.FileNode;
import com.haoke.data.AllMediaList;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.util.Media_IF;

public class RecordDevicePlay {
	private static final String TAG = "RecordDevicePlay";
	private static final String MD5_VALUE_DEFAULT = "1111";
	
	private static RecordDevicePlay mSelf;
	private Context mContext;
	private DeviceInfo mLastPlayDevice = new DeviceInfo();
	
	private static final class DeviceInfo {
		public int mDeviceType = DeviceType.NULL;
		public String mDeviceMd5 = null;
		
		public String toString() {
			return "DeviceInfo: mDeviceType="+mDeviceType+"; mDeviceMd5="+mDeviceMd5;
		}
		
		public boolean isValid() {
			if (mDeviceType == DeviceType.NULL) {
				return false;
			}
			return true;
		}
	}

	private RecordDevicePlay() {
		mContext = MediaApplication.getInstance();
		//mLastPlayDevice.mDeviceType = AllMediaList.instance(mContext).getLastDeviceType();
		//mLastPlayDevice.mDeviceMd5 = MD5_VALUE_DEFAULT;
	}
	
	public static RecordDevicePlay instance() {
		if (mSelf == null) {
			mSelf = new RecordDevicePlay();
		}
		return mSelf;
	}
	
	public void saveLastPlayDevice(int deviceType) {
	    if (!AmdConfig.INSERT_USB_RECODRD_PLAY_MUSIC) {
	        return;
	    }
		if (mLastPlayDevice == null) {
			mLastPlayDevice = new DeviceInfo();
		}
		String deviceMd5 = MD5_VALUE_DEFAULT;
		mLastPlayDevice.mDeviceType = deviceType;
		mLastPlayDevice.mDeviceMd5 = deviceMd5;
	}
	
	public void clearLastPlayDevice() {
		mLastPlayDevice = null;
	}
	
	public void sourceChanged(int source) {
        if (!AmdConfig.INSERT_USB_RECODRD_PLAY_MUSIC) {
            return;
        }
		if (Source.isAudioSource(source)) {
			clearLastPlayDevice();
		}
	}
	
	/**
	 * @param deviceType
	 * @return false为设备不是最后一次播放的设备
	 */
	public boolean checkUsbPlay(int deviceType) {
        if (!AmdConfig.INSERT_USB_RECODRD_PLAY_MUSIC) {
            return false;
        }
		if (mLastPlayDevice==null) {
			Log.d(TAG, "checkUsbPlay mLastPlayDevice is null!");
			return false;
		}
		boolean currPlaying = Media_IF.getInstance().isPlayState();
		if (currPlaying) {
			Log.e(TAG, "checkUsbPlay meida is playing!");
			return false;
		}
		boolean lastPlaying = AllMediaList.instance(mContext).getPlayState(FileType.AUDIO);
		Log.d(TAG, "checkUsbPlay deviceType="+deviceType+"; lastPlaying="
		        +lastPlaying+"; deviceType="+deviceType+"; mLastPlayDevice="+mLastPlayDevice);
		if (!lastPlaying) {
			return false;
		}
		if (deviceType == mLastPlayDevice.mDeviceType) {
			String deviceMd5 = MD5_VALUE_DEFAULT;
			if (mLastPlayDevice.mDeviceMd5!=null && mLastPlayDevice.mDeviceMd5.equals(deviceMd5)) {
				return checkUsbFile(deviceType);
			} else {
				return false;
			}
		}
		return false;
	}
	
	private boolean checkUsbFile(int deviceType) {
		String filePath = AllMediaList.instance(mContext).getLastPlayPath(deviceType, FileType.AUDIO);
		if (TextUtils.isEmpty(filePath)) {
			return false;
		}
		File file = new File(filePath);
		if (file.exists() && file.canRead()) {
			Media_IF.getInstance().play(filePath);
		} else {
			ArrayList<FileNode> lists = AllMediaList.instance(mContext).getMediaList(deviceType, FileType.AUDIO);
			if (lists.size() > 0) {
				Media_IF.getInstance().play(lists.get(0));
			}
		}
		return true;
	}
	
}