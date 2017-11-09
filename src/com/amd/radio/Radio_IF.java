package com.amd.radio;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.amd.media.MediaInterfaceUtil;
import com.haoke.aidl.ICarCallBack;
import com.haoke.define.ModeDef;
import com.haoke.define.McuDef.McuFunc;
import com.haoke.define.RadioDef.Area;
import com.haoke.define.RadioDef.Band_5;
import com.haoke.define.RadioDef.RadioFunc;
import com.haoke.define.RadioDef.RadioState;
import com.haoke.service.RadioService;
import com.haoke.serviceif.CarService_Listener;
import com.haoke.serviceif.CarService_IF;
import com.haoke.util.Media_IF;
import com.jsbd.util.Meter_IF;

public class Radio_IF extends CarService_IF {

	private static final String TAG = "Radio_IF";
	private static Radio_IF mSelf = null;
	private Radio_CarCallBack mCarCallBack = null;
	private boolean mServiceConn = false;
    private boolean isScan5S = false;
    private boolean isRescan = false;
	
	public Radio_IF() {
		mMode = ModeDef.RADIO;
		mCarCallBack = new Radio_CarCallBack();

		// 以下处理服务回调
		mICallBack = new ICarCallBack.Stub() {
			@Override
			public void onDataChange(int mode, int func, int data)
					throws RemoteException {
				if (mode == ModeDef.MCU && func == McuFunc.SOURCE) {
				} else {
					mCarCallBack.onDataChange(mode, func, data);
				}
			}
		};
	}

	// 获取接口实例
	synchronized public static Radio_IF getInstance() {
		if (mSelf == null) {
			mSelf = new Radio_IF();
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

	// 注册车载服务回调（全局状态变化）
	public void registerCarCallBack(CarService_Listener listener) {
		mCarCallBack.registerCarCallBack(listener);
	}

	// 注销车载服务回调（全局状态变化）
	public void unregisterCarCallBack(CarService_Listener listener) {
		mCarCallBack.unregisterCarCallBack(listener);
	}

	// 注册车载服务回调（模块相关变化）
	public void registerModeCallBack(Radio_CarListener listener) {
		mCarCallBack.registerModeCallBack(listener);
	}

	// 注销车载服务回调（模块相关变化）
	public void unregisterModeCallBack(Radio_CarListener listener) {
		mCarCallBack.unregisterModeCallBack(listener);
	}
	
	//禁止UI层调用
	public void sendSouceChange(int source) {
		mCarCallBack.onDataChange(ModeDef.MCU, McuFunc.SOURCE, source);
	}

	// 设置当前源
	public boolean setCurSource(int source) {
		try {
//			Log.d(TAG, "setCurSource source="+source);
			return Media_IF.setCurSource(source);
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return false;
	}

	// 获取当前源
	public int getCurSource() {
		try {
			return Media_IF.getCurSource();
//			return mServiceIF.mcu_getCurSource();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return ModeDef.NULL;
	}

	// 设置当前音频焦点
	public boolean requestAudioFocus(boolean request) {
		try {
			return RadioService.getInstance().getRadioManager()
					.requestAudioFocus(request);
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return false;
	}

	// 获取收音机状态（扫描，浏览，搜索）
	public int getState() {
		int state = RadioState.NULL;
		try {
			state = mServiceIF.radio_getState();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		Log.d(TAG, "getState state="+state);
		return state;
	}

	// 获取频率值
	public int getCurFreq() {
		int freq = 0;
		try {
			freq = mServiceIF.radio_getCurFreq();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		Log.d(TAG, "getCurFreq freq="+freq);
		return freq;
	}

	// 设置频率值
	public void setCurFreq(int freq) {
		try {
			Log.d(TAG, "setCurFreq freq="+freq);
			mServiceIF.radio_setCurFreq(freq);
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}
	
	//播放暂停状态
	public boolean isEnable(){
		boolean enable = false;
		int source = getCurSource();
		try {
			if (source == ModeDef.RADIO) {
				enable = mServiceIF.radio_isEnable();
			}
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		Log.d(TAG, "isEnable enable="+enable);
		return enable;
	}
	
	//设置播放暂停
	public void setEnable(boolean enable){
		try {
			boolean focus = true;
			if (MediaInterfaceUtil.mediaCannotPlay()) {
				return;
			}
			Log.d(TAG, "setEnable enable="+enable);
			if (enable) {
				focus = RadioService.getInstance().getRadioManager().requestAudioFocus(true);
				Log.d(TAG, "setEnable enable="+enable+"; focus="+focus);
				if (focus) {
					setRadioSource();
		        	exitRescanAndScan5S(true);
					mServiceIF.radio_setEnable(enable);
				}
			} else {
				mServiceIF.radio_setEnable(enable);
			}
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}

	// 获取波段
	public int getCurBand() {
		int band = Band_5.FM1;
		try {
			band = mServiceIF.radio_getCurBand();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		Log.d(TAG, "getCurBand band="+band);
		return band;
	}

	// 设置波段
	public void setCurBand() {
		try {
			int band = Band_5.FM1;
			int curBand = getCurBand();
			switch (curBand) {
			case Band_5.FM1:
			case Band_5.FM2:
			case Band_5.FM3:
				band = curBand;
				break;

			case Band_5.AM1:
			case Band_5.AM2:
				band = Band_5.FM1;
				break;
			}
			Log.d(TAG, "setCurBand band="+band+"; mServiceIF="+mServiceIF);
			if (mServiceIF != null) {
				mServiceIF.radio_setCurBand(band);
			}

		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}

	// 获取收音区域
	public int getCurArea() {
		int area = Area.CHINA;
		try {
			area = mServiceIF.radio_getCurArea();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		Log.d(TAG, "getCurArea area="+area);
		return area;
	}

	// 设置收音区域
	public void setCurArea(byte area) {
		try {
			Log.d(TAG, "setCurArea area="+area);
			mServiceIF.radio_setCurArea(area);
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}

	// 获取所有预存台值
	public int getChannel(int index) {
		int freq = 0;
		try {
			// 索引从1开始，传0获取的是列表的波段类型
			freq = mServiceIF.radio_getChannel(index + 1);
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return freq;
	}

	// 获取当前高亮预存台
	public int getPlayChannel() {
		int index = -1;
		try {
			index = mServiceIF.radio_getPlayChannel();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return index;
	}

	// 设置预存台号对应频点为当前频点
	public void playChannel(int index) {
		try {
			int band = getCurBand();
			if (band <= Band_5.FM3) {
				index = (band - Band_5.FM1) * 6 + index;
			} else {
				index = (band - Band_5.AM1) * 6 + index;
			}
			mServiceIF.radio_playChannel(index);
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}

	// 保存当前频点到对应预存台号
	public void saveChannel(int index) {
		try {
			mServiceIF.radio_saveChannel(index);
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}

	// 浏览
	public void setPreview() {
		try {
			mServiceIF.radio_scanPreset();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}

	// 扫描
	public void setScan() {
		try {
			Log.d(TAG, "setScan");
			if (!isEnable()) {
	        	setEnable(true);
	        }
			isRescan = false;
			isScan5S = true;
			setRadioSource();
			mServiceIF.radio_scan();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}

	
	// 停止扫描
	public void stopScan() {
		try {
			Log.d(TAG, "stopScan");
			mServiceIF.radio_stopScanStore();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}
	
	
	// 左步进
	public void setPreStep() {
		try {
			boolean focus = RadioService.getInstance().getRadioManager().requestAudioFocus(true);
			Log.d(TAG, "setPreStep focus="+focus);
			if (focus) {
				setRadioSource();
	        	exitRescanAndScan5S(false);
				mServiceIF.radio_scanManualPre();
			}
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}

	// 右步进
	public void setNextStep() {
		try {
			boolean focus = RadioService.getInstance().getRadioManager().requestAudioFocus(true);
			Log.d(TAG, "setNextStep focus="+focus);
			if (focus) {
				setRadioSource();
	        	exitRescanAndScan5S(false);
				mServiceIF.radio_scanManualNext();
			}
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}

	// 左搜索
	public void setPreSearch() {
		try {
			boolean focus = RadioService.getInstance().getRadioManager().requestAudioFocus(true);
			Log.d(TAG, "setPreSearch focus="+focus);
			if (focus) {
				setRadioSource();
				mServiceIF.radio_scanAutoPre();
			}
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}

	// 右搜索
	public void setNextSearch() {
		try {
			boolean focus = RadioService.getInstance().getRadioManager().requestAudioFocus(true);
			Log.d(TAG, "setNextSearch focus="+focus);
			if (focus) {
				setRadioSource();
				mServiceIF.radio_scanAutoNext();
			}
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}

	// 设置ST
	public void setST() {
		try {
			Log.d(TAG, "setST");
			mServiceIF.radio_setST();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}

	// 设置LOC
	public void setLoc() {
		try {
			mServiceIF.radio_setLoc();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}

	// 设置AF
	public void setAF() {
		try {
			mServiceIF.radio_setRdsAF();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}

	// 设置TA
	public void setTA() {
		try {
			mServiceIF.radio_setRdsTA();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}

	// 获取RDS开关
	public boolean isRdsOpen() {
		return false;
		// return McuHelp.mSettingIF.getSystemModule(4)==1?true:false;
	}

	// 获取ST状态
	public boolean getST() {
		boolean value = false;
		try {
			value = mServiceIF.radio_getST() == 1 ? true : false;
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		Log.d(TAG, "getST value="+value);
		return value;
	}

	// 获取Listen状态
	public boolean getListen() {
		boolean value = false;
		try {
			value = mServiceIF.radio_getListen() == 1 ? true : false;
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return value;
	}

	// 获取Loc状态
	public boolean getLoc() {
		boolean value = false;
		try {
			value = mServiceIF.radio_getLoc() == 1 ? true : false;
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return value;
	}

	// 获取AF状态
	public boolean getAF() {
		boolean value = false;
		try {
			value = mServiceIF.radio_getRdsAF() == 1 ? true : false;
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return value;
	}

	// 获取TA状态
	public boolean getTA() {
		boolean value = false;
		try {
			value = mServiceIF.radio_getRdsTA() == 1 ? true : false;
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return value;
	}

	// 设置PTY类型
	public void setPty(int index) {
		try {
			mServiceIF.radio_setPty(index);
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}

	// 获取PTY搜索的类型
	public String getRdsMessage() {
		String value = "";
		try {
			value = mServiceIF.radio_getRdsMessage();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
		return value;
	}
	
	public void scanListChannel(){
		try {
			Log.d(TAG, "scanListChannel");
			mServiceIF.radio_scanListChannel();
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}
	
	// 扫描
	public void scanStore() {
		try {
			boolean focus = RadioService.getInstance().getRadioManager().requestAudioFocus(true);
			Log.d(TAG, "scanStore focus="+focus);
			if (focus) {
				isRescan = true;
				isScan5S = false;
				setRadioSource();
				setRecordRadioOnOff(false);
				mServiceIF.radio_scanStore();
			}
		} catch (Exception e) {
			Log.e(TAG, "HMI------------interface e=" + e.getMessage());
		}
	}
	
	public static void sendRadioInfo(int band, int freq) {
		Meter_IF.sendRadioInfo(band, freq);
	}
	
	private void setRadioSource() {
		setCurSource(ModeDef.RADIO);
	}
	
	private RadioDatabaseHelper dbHelper;
	private static final String RADIO_LOVE_STORE = "RadioLoveStore.db";
	private static final int DBVERSION = 1;
	
	public static int sfreqToInt(final String sfreq) {
		Log.d(TAG, "sfreqToInt sfreq="+sfreq);
		int freq = -1;
		float f = Float.valueOf(sfreq);
		
		f = f * 100;
		freq = (int) f;
		
		Log.d(TAG, "sfreqToInt return freq="+freq);
		return freq;
	}
	
	public static String freqToString(int freq) {
		String freq_data = String.valueOf(freq);
		if (freq >= 8750) {
			freq_data = freq_data.substring(0, freq_data.length()-2)+"."+freq_data.substring(freq_data.length()-2);
		}
		return freq_data;
	}
	
	public ArrayList<RadioStation> initFavoriteData(Context context) {
		if (dbHelper == null) {
			dbHelper = new RadioDatabaseHelper(context, RADIO_LOVE_STORE, null, DBVERSION);
		}
		if (Data_Common.collectAllFreqs != null) {
			return Data_Common.collectAllFreqs;
		}
		Data_Common.collectAllFreqs = new ArrayList<RadioStation>();
		Cursor cursor = dbHelper.getWritableDatabase().query("Radio_FM", null, null, null, null, null, null, null);
		try {
			if (cursor.moveToFirst()) {
				int freqIndex = cursor.getColumnIndex("freq");
				int ifreqIndex = cursor.getColumnIndex("ifreq");
				int snameIndex = cursor.getColumnIndex("sname");
				String freq, sname;
				int ifrq;
				do {
					freq = cursor.getString(freqIndex);
					ifrq = cursor.getInt(ifreqIndex);
					sname = cursor.getString(snameIndex);
					Data_Common.collectAllFreqs.add(new RadioStation(ifrq, freq, sname));
				} while (cursor.moveToNext());
			}
		} catch (Exception e) {
			Log.e(TAG, "initFavoriteData", e);
		} finally {
			if (cursor!=null) {
				cursor.close();
			}
		}
		return Data_Common.collectAllFreqs;
	}
	
	public boolean isCollected(Context context, int freq) {
		ArrayList<RadioStation> lists = initFavoriteData(context);
		for (RadioStation station : lists) {
			if (station.getFreq() == freq) {
				return true;
			}
		}
		return false;
	}
	
	public boolean collectFreq(Context context, int freq, boolean showToast) {
		if (Data_Common.collectAllFreqs.size()>=30) {
			if (showToast) {
                showToast(context, "最多收藏30条电台！");
			}
			return false;
		}
		if (isCollected(context, freq)) {
			return true;
		}
		if (dbHelper == null) {
			dbHelper = new RadioDatabaseHelper(context, RADIO_LOVE_STORE, null, DBVERSION);
		}
		String sfreq = freqToString(freq);
//		int freq = sfreqToInt(sfreq);
		String sname = Radio_SimpleSave.getInstance().getStationName(freq);
		ContentValues values = new ContentValues();
		values.put("name", sfreq);
		values.put("freq", sfreq);
		values.put("ifreq", freq);
		values.put("sname", sname);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		try {
			db.insert("Radio_FM", null, values); 
			if (showToast) {
			    showToast(context, "已加入收藏列表");
			}
			Data_Common.collectAllFreqs.add(new RadioStation(freq, sfreq, sname));
			mCarCallBack.onDataChange(mMode, RadioFunc.FREQ, getCurFreq());
		} catch (Exception e1) {
			Log.e(TAG, "collectFreq freq="+freq+"; e1="+e1);
			return false;
		}
		return true;
	}
	
    private Toast mToast = null;
	private void showToast(Context context, String msg) {
        if (mToast == null) {
            mToast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(msg);
            mToast.setDuration(Toast.LENGTH_SHORT);
        }
        mToast.show();
	}
	
	public boolean uncollectFreq(Context context, int freq, boolean showToast) {
		if (!isCollected(context, freq)) {
			return true;
		}
		ArrayList<RadioStation> lists = new ArrayList<RadioStation>();
		String sfreq = freqToString(freq);
		lists.add(new RadioStation(freq, sfreq, null));
		return uncollectFreq(context, lists, showToast);
	}
	
	public boolean uncollectFreq(Context context, ArrayList<RadioStation> list, boolean showToast) {
		if (dbHelper == null) {
			dbHelper = new RadioDatabaseHelper(context, RADIO_LOVE_STORE, null, DBVERSION);
		}
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		try {
/*			StringBuffer buffer = new StringBuffer();
			for (int i=0; i<list.size(); i++) {
				RadioStation station = list.get(i);
				buffer.append(station.getSfreq());
				if (i != list.size()-1) {
					buffer.append(",");
				}
			}
			db.delete("Radio_FM", "freq=?", new String[]{buffer.toString()});*/
			for (int i=0; i<list.size(); i++) {
				RadioStation station = list.get(i);
				db.delete("Radio_FM", "freq=?", new String[]{station.getSfreq()});
				Data_Common.removeCollectFreq(station.getFreq(), null);
			}
			mCarCallBack.onDataChange(mMode, RadioFunc.FREQ, getCurFreq());
			if (showToast) {
                showToast(context, "已取消收藏");
			}
		} catch (Exception e) {
			Log.e(TAG, "uncollectFreq", e);
		}
		return true;
	}
	public void clearColloctFreq(Context context) {
		ArrayList<RadioStation> stations = new ArrayList<RadioStation>();
		stations.addAll(initFavoriteData(context));
		if (stations.size() != 0) {
			uncollectFreq(context, stations, false);
		}
	}
	public void playCollectFistFreq(Context context, boolean showToast) {
		ArrayList<RadioStation> stations = initFavoriteData(context);
		if (stations == null || stations.size() == 0) {
			if (showToast) {
                showToast(context, "没有收藏的电台！");
			}
			return;
		}
		RadioStation station = stations.get(0);
		setCurFreq(station.getFreq());
		if (!isEnable()) {
			setEnable(true);
		}
	}
	
	// 设置播放状态（被抢焦点前）
	public void setRecordRadioOnOff(boolean on) {
		Log.d(TAG, "setRecordRadioOnOff on="+on);
		RadioService.getInstance().getRadioManager().setRecordRadioOnOff(on);
	}

	// 获取播放状态（被抢焦点前）
	public boolean getRecordRadioOn() {
		boolean on = RadioService.getInstance().getRadioManager().getRecordRadioOn();
		Log.d(TAG, "setRecordRadioOnOff on="+on);
		return on;
	}
	
	//获取是否在扫描状态
	public boolean isRescanState() {
		return isRescan;
	}
	
	//获取是否在预览状态
	public boolean isScan5SState() {
		return isScan5S;
	}
	
    public boolean exitScan5S() {
        boolean state = isScan5S;
        if (isScan5S) {
            isScan5S = false;
            stopScan();
        }
        return state;
    }
    
    public boolean exitRescan() {
    	boolean state = isRescan;
    	if (isRescan) {
            isRescan = false;
            stopScan();
        }
    	return state;
    }
    
    public void exitRescanAndScan5S(boolean stopScan) {
		if (stopScan) {
			if (isRescan || isScan5S) {
				stopScan();
			}
		}
		isRescan = false;
		isScan5S = false;
    }
    
    public void isScanStateChange(int data) {
    	//data为2表示SCAN[Scan5S]， 为3表示SEARCH[Rescan]
    	if (data == 2) {
    		isScan5S = true;
    		isRescan = false;
    	} else if (data == 3) {
    		isRescan = true;
    		isScan5S = false;
    	}
    	if (isRescan) {
        	boolean enable = isEnable();
            if (data == 3 && enable) {
            	setEnable(false);
            }
            if (data == 0) {
            	if ((getCurSource() == ModeDef.RADIO) && !enable) {
                	setEnable(true);
            	}
            	isRescan = false;
            }
        }
    	if (isScan5S) {
    		if (data == 0) {
    			isScan5S = false;
            }
    	}
    }
}
