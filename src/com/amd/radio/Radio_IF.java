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
import com.amd.util.Source;
import com.haoke.aidl.ICarCallBack;
import com.haoke.application.MediaApplication;
import com.haoke.bean.UserBean;
import com.haoke.constant.MediaUtil;
import com.haoke.define.McuDef.McuFunc;
import com.haoke.define.RadioDef.Area;
import com.haoke.define.RadioDef.Band_5;
import com.haoke.define.RadioDef.RadioFunc;
import com.haoke.define.RadioDef.RadioState;
import com.haoke.mediaservice.R;
import com.haoke.serviceif.CarService_Listener;
import com.haoke.serviceif.CarService_IF;
import com.haoke.util.DebugLog;
import com.haoke.util.Media_IF;
import com.jsbd.util.Meter_IF;

public class Radio_IF extends CarService_IF {

	private static final String TAG = "Radio_IF";
	private static Radio_IF mSelf = null;
	private Radio_CarCallBack mCarCallBack = null;
	private boolean mServiceConn = false;
    private boolean isScan5S = false;
    private boolean isRescan = false;
    private boolean isScanAutoNext = false;
    
    /* @see RadioFunc */
    public static final int RADIOFUNCCOLLECT = 0x1001;
	
	public Radio_IF() {
		mMode = com.haoke.define.ModeDef.RADIO;
		mCarCallBack = new Radio_CarCallBack();

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
	public void registerModeCallBack(Radio_CarListener listener) {
		mCarCallBack.registerModeCallBack(listener);
	}

	// 注销车载服务回调（模块相关变化）
	public void unregisterModeCallBack(Radio_CarListener listener) {
		mCarCallBack.unregisterModeCallBack(listener);
	}
	
	//禁止UI层调用
	public void sendSouceChange(int source) {
		mCarCallBack.onDataChange(com.haoke.define.ModeDef.MCU, McuFunc.SOURCE, source);
	}

	// 设置当前源
	public boolean setCurSource(int source) {
	    return Media_IF.setCurSource(source);
	}

	// 获取当前源
	public int getCurSource() {
	    return Media_IF.getCurSource();
	}
	
	public static RadioManager getRadioManager() {
	    RadioManager radioManager = null;
	    try {
	        radioManager = com.haoke.service.MediaService.getInstance().getRadioManager();
        } catch (Exception e) {
            DebugLog.e(TAG, "getRadioManager e="+e);
        }
	    return radioManager;
	}
	
	// 判断收音机是否有焦点
    public boolean hasAudioFocus() {
        try {
            return getRadioManager().hasAudioFocus();
        } catch (Exception e) {
            DebugLog.e(TAG, "HMI------------hasAudioFocus e=" + e.getMessage());
        }
        return false;
    }

	// 设置当前音频焦点
	public boolean requestAudioFocus(boolean request) {
		try {
			return getRadioManager()
					.requestAudioFocus(request);
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------requestAudioFocus e=" + e.getMessage());
		}
		return false;
	}

	// 获取收音机状态（扫描，浏览，搜索）
	public int getState() {
		int state = RadioState.NULL;
		try {
			state = mServiceIF.radio_getState();
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------getState e=" + e.getMessage());
		}
		DebugLog.d(TAG, "getState state="+state);
		return state;
	}

	// 获取频率值
	public int getCurFreq() {
		int freq = 0;
		try {
			freq = mServiceIF.radio_getCurFreq();
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------getCurFreq e=" + e.getMessage());
		}
		DebugLog.d(TAG, "getCurFreq freq="+freq);
		return freq;
	}

	// 设置频率值
	public void setCurFreq(int freq) {
		try {
			DebugLog.d(TAG, "setCurFreq freq="+freq);
			mServiceIF.radio_setCurFreq(freq);
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------setCurFreq e=" + e.getMessage());
		}
	}
	
	//播放暂停状态
	public boolean isEnable(){
		boolean enable = false;
		int source = getCurSource();
		try {
			if (Source.isRadioSource(source)) {
				enable = mServiceIF.radio_isEnable();
			}
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------isEnable e=" + e.getMessage());
		}
		DebugLog.d(TAG, "isEnable enable="+enable);
		return enable;
	}
	
	//设置播放暂停
	public void setEnable(boolean enable){
		try {
			boolean focus = true;
			DebugLog.d(TAG, "setEnable enable="+enable);
			if (enable) {
	            if (MediaInterfaceUtil.mediaCannotPlay()) {
	                return;
	            }
	            setRecordRadioOnOff(false);
				focus = getRadioManager().requestAudioFocus(true);
				DebugLog.d(TAG, "setEnable enable="+enable+"; focus="+focus);
				if (focus) {
					setRadioSource();
		        	//exitRescanAndScan5S(true);//ENABLE_RADIO_MUTEX_LOGIC
					mServiceIF.radio_setEnable(enable);
				}
			} else {
				mServiceIF.radio_setEnable(enable);
			}
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------setEnable e=" + e.getMessage());
		}
	}

	// 获取波段
	public int getCurBand() {
		int band = Band_5.FM1;
		try {
			band = mServiceIF.radio_getCurBand();
			switch (band) {
			case Band_5.FM1:
			case Band_5.FM2:
			case Band_5.FM3:
				break;
			case Band_5.AM1:
			case Band_5.AM2:
				band = Band_5.FM1;
				break;
			}
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------getCurBand e=" + e.getMessage());
		}
		DebugLog.d(TAG, "getCurBand band="+band);
		return band;
	}

	// 设置波段
	public void setCurBand() {
		try {
			int band = getCurBand();
			DebugLog.d(TAG, "setCurBand band="+band+"; mServiceIF="+mServiceIF);
			if (mServiceIF != null) {
				mServiceIF.radio_setCurBand(band);
			}

		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------setCurBand e=" + e.getMessage());
		}
	}

	// 获取收音区域
	public int getCurArea() {
		int area = Area.CHINA;
		try {
			area = mServiceIF.radio_getCurArea();
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------getCurArea e=" + e.getMessage());
		}
		DebugLog.d(TAG, "getCurArea area="+area);
		return area;
	}

	// 设置收音区域
	public void setCurArea(byte area) {
		try {
			DebugLog.d(TAG, "setCurArea area="+area);
			mServiceIF.radio_setCurArea(area);
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------setCurArea e=" + e.getMessage());
		}
	}

	// 获取所有预存台值
	public int getChannel(int index) {
		int freq = 0;
		try {
			// 索引从1开始，传0获取的是列表的波段类型
			freq = mServiceIF.radio_getChannel(index + 1);
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------getChannel e=" + e.getMessage());
		}
		return freq;
	}

	// 获取当前高亮预存台
	public int getPlayChannel() {
		int index = -1;
		try {
			index = mServiceIF.radio_getPlayChannel();
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------getPlayChannel e=" + e.getMessage());
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
			DebugLog.e(TAG, "HMI------------playChannel e=" + e.getMessage());
		}
	}

	// 保存当前频点到对应预存台号
	public void saveChannel(int index) {
		try {
			mServiceIF.radio_saveChannel(index);
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------saveChannel e=" + e.getMessage());
		}
	}

	// 浏览
	public void setPreview() {
		try {
			mServiceIF.radio_scanPreset();
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------setPreview e=" + e.getMessage());
		}
	}

	// 扫描, 播放5秒，再搜索下一个
	public void setScan() {
		try {
			//if (!isEnable()) { //ENABLE_RADIO_MUTEX_LOGIC
	        	//setEnable(true);
	        //}
			boolean focus = getRadioManager().requestAudioFocus(true);
            DebugLog.d(TAG, "setScan focus="+focus);
            if (focus) {
                isRescan = false;
                isScan5S = true;
                setRadioSource();
                mServiceIF.radio_scan();
            }
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------setScan e=" + e.getMessage());
		}
	}

	
	// 停止扫描
	public void stopScan() {
		try {
			DebugLog.d(TAG, "stopScan");
			mServiceIF.radio_stopScanStore();
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------stopScan e=" + e.getMessage());
		}
	}
	
	
	// 左步进
	public void setPreStep() {
		try {
			boolean focus = getRadioManager().requestAudioFocus(true);
			DebugLog.d(TAG, "setPreStep focus="+focus);
			if (focus) {
				setRadioSource();
	        	//exitRescanAndScan5S(false);//ENABLE_RADIO_MUTEX_LOGIC
				mServiceIF.radio_scanManualPre();
			}
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------setPreStep e=" + e.getMessage());
		}
	}

	// 右步进
	public void setNextStep() {
		try {
			boolean focus = getRadioManager().requestAudioFocus(true);
			DebugLog.d(TAG, "setNextStep focus="+focus);
			if (focus) {
				setRadioSource();
	        	//exitRescanAndScan5S(false);//ENABLE_RADIO_MUTEX_LOGIC
				mServiceIF.radio_scanManualNext();
			}
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------setNextStep e=" + e.getMessage());
		}
	}

	// 左搜索
	public void setPreSearch() {
		try {
			boolean focus = getRadioManager().requestAudioFocus(true);
			DebugLog.d(TAG, "setPreSearch focus="+focus);
			if (focus) {
				setRadioSource();
				mServiceIF.radio_scanAutoPre();
			}
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------setPreSearch e=" + e.getMessage());
		}
	}

	// 右搜索
	public void setNextSearch() {
		try {
			boolean focus = getRadioManager().requestAudioFocus(true);
			DebugLog.d(TAG, "setNextSearch focus="+focus);
			if (focus) {
				setRadioSource();
				mServiceIF.radio_scanAutoNext();
			}
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------setNextSearch e=" + e.getMessage());
		}
	}

	// 设置ST
	public void setST() {
		try {
			DebugLog.d(TAG, "setST");
			mServiceIF.radio_setST();
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------setST e=" + e.getMessage());
		}
	}

	// 设置LOC
	public void setLoc() {
		try {
			mServiceIF.radio_setLoc();
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------setLoc e=" + e.getMessage());
		}
	}

	// 设置AF
	public void setAF() {
		try {
			mServiceIF.radio_setRdsAF();
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------setAF e=" + e.getMessage());
		}
	}

	// 设置TA
	public void setTA() {
		try {
			mServiceIF.radio_setRdsTA();
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------setTA e=" + e.getMessage());
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
			DebugLog.e(TAG, "HMI------------getST e=" + e.getMessage());
		}
		DebugLog.d(TAG, "getST value="+value);
		return value;
	}

	// 获取Listen状态
	public boolean getListen() {
		boolean value = false;
		try {
			value = mServiceIF.radio_getListen() == 1 ? true : false;
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------getListen e=" + e.getMessage());
		}
		return value;
	}

	// 获取Loc状态
	public boolean getLoc() {
		boolean value = false;
		try {
			value = mServiceIF.radio_getLoc() == 1 ? true : false;
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------getLoc e=" + e.getMessage());
		}
		return value;
	}

	// 获取AF状态
	public boolean getAF() {
		boolean value = false;
		try {
			value = mServiceIF.radio_getRdsAF() == 1 ? true : false;
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------getAF e=" + e.getMessage());
		}
		return value;
	}

	// 获取TA状态
	public boolean getTA() {
		boolean value = false;
		try {
			value = mServiceIF.radio_getRdsTA() == 1 ? true : false;
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------getTA e=" + e.getMessage());
		}
		return value;
	}

	// 设置PTY类型
	public void setPty(int index) {
		try {
			mServiceIF.radio_setPty(index);
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------setPty e=" + e.getMessage());
		}
	}

	// 获取PTY搜索的类型
	public String getRdsMessage() {
		String value = "";
		try {
			value = mServiceIF.radio_getRdsMessage();
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------getRdsMessage e=" + e.getMessage());
		}
		return value;
	}
	
	public void scanListChannel(){
		try {
			DebugLog.d(TAG, "scanListChannel");
			mServiceIF.radio_scanListChannel();
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------scanListChannel e=" + e.getMessage());
		}
	}
	
	// 扫描
	public void scanStore() {
		try {
			boolean focus = getRadioManager().requestAudioFocus(true);
			DebugLog.d(TAG, "scanStore focus="+focus);
			if (focus) {
				isRescan = true;
				isScan5S = false;
				setRadioSource();
				setRecordRadioOnOff(false);
				mServiceIF.radio_scanStore();
			}
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------scanStore e=" + e.getMessage());
		}
	}
	
	// 上一电台
	public void setPreChannel() {
		try {
			boolean focus = getRadioManager().requestAudioFocus(true);
			DebugLog.d(TAG, "setPreChannel focus="+focus);
			if (focus) {
				setRadioSource();
				mServiceIF.radio_setPreChannel();
				//mServiceIF.radio_setEnable(true);
			}
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------setPreChannel e=" + e.getMessage());
		}
	}
	
	// 下一电台
	public void setNextChannel() {
		try {
			boolean focus = getRadioManager().requestAudioFocus(true);
			DebugLog.d(TAG, "setNextChannel focus="+focus);
			if (focus) {
				setRadioSource();
				mServiceIF.radio_setNextChannel();
				//mServiceIF.radio_setEnable(true);
			}
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------setNextChannel e=" + e.getMessage());
		}
	}
	
	private int getStation(boolean pre) {
		int size = Data_Common.stationList.size();
		if (size > 0) {
			int index = -1;
			int freq = getCurFreq();
			for (int i=0; i<size; i++) {
				RadioStation station = Data_Common.stationList.get(i);
				if (station.getFreq() == freq) {
					index = i;
					break;
				}
			}
            if (index == -1) {
                index = size;
                for (int i=0; i<size; i++) {
                    RadioStation station = Data_Common.stationList.get(i);
                    int tempFreq = station.getFreq();
                    if (tempFreq > freq) {
                        if (pre) {
                            index = i;
                        } else {
                            if (i == 0) {
                                index = size - 1;
                            } else {
                                index = i - 1;
                            }
                        }
                        break;
                    }
                }
            }
            if (index != -1) {
				if (pre) {
					index --;
				} else {
					index ++;
				}
				if (index < 0) {
					index = size - 1;
				} else if (index > size -1) {
					index = 0;
				}
				return Data_Common.stationList.get(index).getFreq();
			}
		}
		return -1;
	}
	
	// 上一电台, 暂时废弃
	public void setPreStation() {
		try {
			int freq = getStation(true);
			DebugLog.d(TAG, "setPreStation freq="+freq);
			if (freq != -1) {
				setCurFreq(freq);
				setEnable(true);
			} else {
			    DebugLog.e(TAG, "setPreStation error. stationList size is 0 !!");
			}
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------setPreStation e=" + e.getMessage());
		}
	}
	
	// 下一电台, 暂时废弃
	public void setNextStation() {
		try {
			int freq = getStation(false);
			DebugLog.d(TAG, "setNextStation freq="+freq);
			if (freq != -1) {
				setCurFreq(freq);
				setEnable(true);
			} else {
                DebugLog.e(TAG, "setNextStation error. stationList size is 0 !!");
            }
		} catch (Exception e) {
			DebugLog.e(TAG, "HMI------------setNextStation e=" + e.getMessage());
		}
	}
	
	public static void sendRadioInfo(int band, int freq) {
		Meter_IF.sendRadioInfo(band, freq);
	}
	
	private void setRadioSource() {
		Source.setRadioSource();
	}
	
	private RadioDatabaseHelper dbHelper;
	private static final String RADIO_LOVE_STORE = "RadioLoveStore.db";
	private static final int DBVERSION = 2;
	private static final String USERNAME_HEAD = "radio_love_";
	
	public static int sfreqToInt(final String sfreq) {
		DebugLog.d(TAG, "sfreqToInt sfreq="+sfreq);
		int freq = -1;
		float f = Float.valueOf(sfreq);
		
		f = f * 100;
		freq = (int) f;
		
		DebugLog.d(TAG, "sfreqToInt return freq="+freq);
		return freq;
	}
	
	public static String freqToString(int freq) {
		String freq_data = String.valueOf(freq);
		if (freq >= 8750) {
			freq_data = freq_data.substring(0, freq_data.length()-2)+"."+freq_data.substring(freq_data.length()-2);
		}
		return freq_data;
	}
	
	public static String getCurrUserName() {
	    return USERNAME_HEAD + MediaUtil.getUserName();
	}
	
	public ArrayList<RadioStation> initFavoriteData(Context context) {
		if (dbHelper == null) {
			dbHelper = new RadioDatabaseHelper(context, RADIO_LOVE_STORE, null, DBVERSION);
		}
		if (Data_Common.collectAllFreqs != null) {
			return Data_Common.collectAllFreqs;
		}
		Data_Common.collectAllFreqs = new ArrayList<RadioStation>();
		String username = getCurrUserName();
		Cursor cursor = dbHelper.getWritableDatabase().query("Radio_FM", null, "username=?", new String[]{username}, null, null, null, null);
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
			DebugLog.e(TAG, "initFavoriteData" + e);
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
                showToast(context, context.getString(R.string.a_maximum_collection_of_30_radio_stations));
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
		values.put("username", getCurrUserName());
		values.put("freq", sfreq);
		values.put("ifreq", freq);
		values.put("sname", sname);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		try {
			db.insert("Radio_FM", null, values); 
			if (showToast) {
			    showToast(context, context.getString(R.string.added_to_the_collection_list));
			}
			Data_Common.collectAllFreqs.add(new RadioStation(freq, sfreq, sname));
			mCarCallBack.onDataChange(mMode, RADIOFUNCCOLLECT, getCurFreq());
		} catch (Exception e1) {
			DebugLog.e(TAG, "collectFreq freq="+freq+"; e1="+e1);
			return false;
		}
		return true;
	}
	
    private Toast mToast = null;
	private void showToast(Context context, String msg) {
        if (mToast == null) {
            mToast = Toast.makeText(context.getApplicationContext(), msg, Toast.LENGTH_SHORT);
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
		String username = getCurrUserName();
		try {
			/*StringBuffer buffer = new StringBuffer();
			for (int i=0; i<list.size(); i++) {
				RadioStation station = list.get(i);
				buffer.append('\'');
				buffer.append(station.getSfreq());
				buffer.append('\'');
				if (i != list.size()-1) {
					buffer.append(",");
				}
			}
			db.delete("Radio_FM", "username=? AND freq IN (?)", new String[]{username, buffer.toString()});*/
			for (int i=0; i<list.size(); i++) {
				RadioStation station = list.get(i);
				db.delete("Radio_FM", "username=? AND freq=?", new String[]{username, station.getSfreq()});
				Data_Common.removeCollectFreq(station.getFreq(), null);
			}
			mCarCallBack.onDataChange(mMode, RADIOFUNCCOLLECT, getCurFreq());
			if (showToast) {
                showToast(context, context.getString(R.string.cancel_the_collection));
			}
		} catch (Exception e) {
			DebugLog.e(TAG, "uncollectFreq" + e);
		}
		return true;
	}
	public void clearColloctFreq() {
	    Context context = MediaApplication.getInstance();
	    ArrayList<UserBean> users = MediaUtil.getUserList(context);
	    if (dbHelper == null) {
            dbHelper = new RadioDatabaseHelper(context, RADIO_LOVE_STORE, null, DBVERSION);
        }
	    SQLiteDatabase db = dbHelper.getWritableDatabase();
	    StringBuffer buffer = new StringBuffer();
	    for (int i=0; i<users.size(); i++) {
	        UserBean user = users.get(i);
	        buffer.append('\'');
	        buffer.append(USERNAME_HEAD);
	        buffer.append(user.getUsername());
	        buffer.append('\'');
            if (i != users.size()-1) {
                buffer.append(",");
            }
        }
	    String sql = "delete from Radio_FM where username NOT IN ("+ buffer.toString() +");";
	    DebugLog.d(TAG, "clearColloctFreq sql="+sql);
	    db.execSQL(sql);
	}
	
	public void playCollectFistFreq(Context context, boolean showToast) {
		ArrayList<RadioStation> stations = initFavoriteData(context);
		if (stations == null || stations.size() == 0) {
			if (showToast) {
                showToast(context, context.getString(R.string.no_collection_of_radio_stations));
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
		DebugLog.d(TAG, "setRecordRadioOnOff on="+on);
		getRadioManager().setRecordRadioOnOff(on);
	}

	// 获取播放状态（被抢焦点前）
	public boolean getRecordRadioOn() {
		boolean on = getRadioManager().getRecordRadioOn();
		DebugLog.d(TAG, "setRecordRadioOnOff on="+on);
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
	
	//获取是否在扫描到下一电台状态
	public boolean isScanAutoNextState() {
	    return isScanAutoNext;
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
    		isScanAutoNext = false;
    	} else if (data == 3) {
    		isRescan = true;
    		isScan5S = false;
    		isScanAutoNext = false;
    	} else if (data == 4) {
    	    isRescan = false;
    	    isScan5S = false;
    	    isScanAutoNext = true;
    	}
    	if (isRescan) {
        	boolean enable = isEnable();
            if (data == 3 && enable) {
            	//setEnable(false);//ENABLE_RADIO_MUTEX_LOGIC
            }
            if (data == 0) {
            	if (Source.isRadioSource() && !enable) {
                	//setEnable(true);//ENABLE_RADIO_MUTEX_LOGIC
            	}
            	isRescan = false;
            }
        }
    	if (isScan5S) {
    		if (data == 0) {
    			isScan5S = false;
            }
    	}
    	if (isScanAutoNext) {
    	    if (data == 0) {
    	        isScanAutoNext = false;
    	    }
    	}
    }
}
