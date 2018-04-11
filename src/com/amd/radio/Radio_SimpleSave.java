package com.amd.radio;

import java.util.ArrayList;

import com.haoke.application.MediaApplication;
import com.haoke.service.MediaService;
import com.haoke.util.DebugLog;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class Radio_SimpleSave {
	private static final int SET_CITY = 0;
    private static final String TAG = "Radio_SimpleSave";
	//private Context mContext = null;
	private SharedPreferences mPreferences = null;
	private SharedPreferences.Editor mEditor = null;
	private final String DBNAME = "city_store";
	
	private static Radio_SimpleSave simpleSave = null;
	private static String province_name = "";
	private static String city_name = "";
	
	private final String BAND_FM = "BAND.FM";
	//private final String BAND_AM = "BAND.AM";
	
	private ArrayList<RadioStation> FMlistStationName = null;
	//private ArrayList<RadioStation> AMlistStationName = null;
	

	public Radio_SimpleSave() {
		try {
			mPreferences = 	MediaApplication.getInstance().getSharedPreferences(DBNAME, Context.MODE_PRIVATE);
			mEditor = mPreferences.edit();
			
		} catch (Exception e) {
			DebugLog.e(TAG, "Create e=" + e.getMessage());
		}
	}
	
	public static Radio_SimpleSave getInstance(){
		if (simpleSave == null) {
			simpleSave = new Radio_SimpleSave();
		}
		return simpleSave;
	}
	
	
//	public Radio_SimpleSave(Context context, String dbName) {
//		try {
//			mContext = context;
//			mPreferences = mContext.getSharedPreferences(dbName, Context.MODE_MULTI_PROCESS);
//			mEditor = mPreferences.edit();
//			
//		} catch (Exception e) {
//			DebugLog.e(TAG, "Create e=" + e.getMessage());
//		}
//	}
	
	
	public void setCity(String province, String city){
		if(city!=null && city.length()>0){			
			String[] citys = city.split("市");
			if(citys!=null && citys.length>0){	
				city = citys[0];
			}
		}else{
			city="";
		}
		if(province==null){
			province="";
		}
		DebugLog.d(TAG, "PROVINCE_NAME:"+province_name+" CITY_NAME:"+city_name+" province:"+province+" city:"+city);
		if(!province_name.equalsIgnoreCase(province) || !city_name.equalsIgnoreCase(city)){
            province_name = province;
            city_name = city;
            //Message msg = Message.obtain();
            //msg.what = SET_CITY;
            //handler.removeMessages(SET_CITY);
            //handler.sendEmptyMessageDelayed(SET_CITY, 1000);
        }
	}
	
	
	public void getCurCityStationNameList(){
		try {
			DebugLog.d(TAG, "getCurCityStationList PROVINCE_NAME:"+province_name+" CITY_NAME:"+city_name);
				String sqlstr=null;
				if(province_name!=null && province_name.length()>0 && city_name!=null && city_name.length()>0){				
					sqlstr = "select FREQ,NAME,CITY from radio_fm where PROVINCE like '%"+province_name+"%'";
					Cursor cursor=null;
					SQLiteDatabase db = AssetsDatabaseManager.getInstance(MediaApplication.getInstance()).getDatabase("radio_station.db");
					cursor = db.rawQuery(sqlstr, null);
					if(cursor!=null){
						FMlistStationName = new ArrayList<RadioStation>();
						ArrayList<RadioStation> tempFMList = new ArrayList<RadioStation>();
						
						while (cursor.moveToNext()) {
							
							int stationfreq = (int) cursor.getLong(0);
							String name = cursor.getString(1);
							String city = cursor.getString(2);
							//DebugLog.d(TAG, "GetAllSaveStation SQLiteDatabase stationfreq:"+stationfreq+" name:"+name);
							
							RadioStation station = new RadioStation(stationfreq, "",name);
							if(city.contains(city_name)){								
								FMlistStationName.add(station);
							}else{
								tempFMList.add(station);
							}
						}
						FMlistStationName.addAll(tempFMList);
						tempFMList=null;
					}

					//AM电台查询
				/*	sqlstr = "select FREQ,NAME,CITY from radio_am where PROVINCE like '%"+province_name+"%'";// and ( CITY like '%"+CITY_NAME+"%')";		
					SQLiteDatabase adb = AssetsDatabaseManager.getInstance(RadioService.getInstance()).getDatabase("radio_station.db");
					cursor = db.rawQuery(sqlstr, null);
					if(cursor!=null){
						AMlistStationName = new ArrayList<RadioStation>();
						ArrayList<RadioStation> tempAMList = new ArrayList<RadioStation>();
						while (cursor.moveToNext()) {	
							int stationfreq = (int) cursor.getLong(0);
							String name = cursor.getString(1);
							String city = cursor.getString(2);
							DebugLog.d(TAG, "GetAllSaveStation SQLiteDatabase stationfreq:"+stationfreq+" name:"+name);
							RadioStation station = new RadioStation(stationfreq, "",name);
							if(city.contains(city_name)){								
								AMlistStationName.add(station);
							}else{
								tempAMList.add(station);
							}
						}
						AMlistStationName.addAll(tempAMList);
						tempAMList=null;
					}*/
				
				}
		} catch (Exception e) {
			// TODO: handle exception
			DebugLog.e(TAG, "Exception: "+Log.getStackTraceString(e));
		}finally {
		}
	}
	
	

	/**
	 * 获取制定电台名称
	 * @param freq 指定电台
	 * @return 电台名称
	 */
	@SuppressWarnings("unused")
	public String getStationName(int freq){
		String name="";
		if (true) {
			// FM中文名暂时做隐藏，不要删除逻辑
			// return name;
		}
		try {
			String mBand = GetBandByFreq(freq);
			
			ArrayList<RadioStation> list = new ArrayList<RadioStation>();
			//if(mBand == BAND_FM){
				list = FMlistStationName;
//			}else if(mBand == BAND_AM){
//				list = AMlistStationName;
//			}
			if(list!=null && list.size()>0){
				for(RadioStation radio_station:list){
					if(freq == radio_station.getFreq()){
						String freqname = radio_station.getStationName();
						name = freqname;
						break;
					}
				}
			}
			
		} catch (Exception e) {
			// TODO: handle exception
			DebugLog.e(TAG, Log.getStackTraceString(e));
		}
		DebugLog.d(TAG, "getRadio_Station_Name freq:"+freq+" CITY_NAME:"+city_name+" name:"+name);
		return name;
	}
	
	
	/**
	 * 根据指定频道判断BAND值
	 * @param freq 指定频道
	 * @return BAND
	 */
	public String GetBandByFreq(int freq)
	{
		String band = "";
		if(freq>0){
			if(freq>8750){
				return BAND_FM;
			}
//			else{
//				return BAND_AM;
//			}
		}
		DebugLog.d(TAG, "GetBandByFreq freq:"+freq+" mBand:"+band);
		return band;
	}
	
	

	public String GetString(String name, String dfValue) {
		return mPreferences.getString(name, dfValue);
	}

	public int GetInt(String name, int dfValue) {
		return mPreferences.getInt(name, dfValue);
	}


	public boolean GetBoolean(String name, boolean dfValue) {
		return mPreferences.getBoolean(name, dfValue);
	}
	

	public void PutString(String name, String value) {
		try {
			mEditor.putString(name, value);
			mEditor.apply();
		} catch (Exception e) {
			DebugLog.e(TAG, "PutData e=" + e.getMessage());
		}
	}
	

	public void PutInt(String name, int value) {
		try {
			mEditor.putInt(name, value);
			mEditor.apply();
		} catch (Exception e) {
			DebugLog.e(TAG, "PutData e=" + e.getMessage());
		}
	}
	

	public void PutBoolean(String name, boolean value) {
		try {
			mEditor.putBoolean(name, value);
			mEditor.apply();
		} catch (Exception e) {
			DebugLog.e(TAG, "PutData e=" + e.getMessage());
		}
	}
	
    private static String receiver_province_name = "";
    private static String receiver_city_name = "";
    public static void setCityFromReceiver(String province, String city) {
        MediaService service = MediaService.getInstance();
        if (service != null) {
            Handler handler = service.getmBDReceiverHandler();
            DebugLog.d(TAG, "setCityFromReceiver PROVINCE_NAME:" + receiver_province_name
                    + " CITY_NAME:" + receiver_city_name + " province:" + province
                    + " city:" + city);
            if (province == null) {
                province = "";
            }
            if (city == null) {
                city = "";
            }
            if (province.equals(receiver_province_name)) {
                if (city.equals(receiver_city_name)) {
                    return;
                }
            }
            receiver_province_name = province;
            receiver_city_name = city;
            if (city != null && city.length() > 0) {
                String[] citys = city.split("市");
                if (citys != null && citys.length > 0) {
                    city = citys[0];
                }
            } else {
                city = "";
            }
            province_name = province;
            city_name = city;
            handler.removeCallbacksAndMessages(null);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Radio_SimpleSave instance = Radio_SimpleSave.getInstance();
                    // 城市改变更新列表
                    if (instance != null) {
                        instance.PutString("PROVINCE_NAME", province_name);
                        instance.PutString("CITY_NAME", city_name);
                    }
                    instance.getCurCityStationNameList();
                }
            }, 1000);
        }
    }
    
    public static void setCityFromSettingsProvider(Handler handler, String province, String city) {
        DebugLog.d(TAG, "setCityFromSettingsProvider province = "+ province + "; city = "+ city);
        if (province == null) {
            province = "";
        }
        if (city == null) {
            city = "";
        }
        if (city != null && city.length() > 0) {
            String[] citys = city.split("市");
            if (citys != null && citys.length > 0) {
                city = citys[0];
            }
        } else {
            city = "";
        }
        if (province.equals(province_name) && city.equals(city_name)) {
            DebugLog.e(TAG, "setCityFromSettingsProvider all is equal!");
            return;
        }
        province_name = province;
        city_name = city;
        DebugLog.d(TAG, "setCityFromSettingsProvider province_name = "+ province_name + "; city_name = "+ city_name);
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Radio_SimpleSave instance = Radio_SimpleSave.getInstance();
                instance.getCurCityStationNameList();
            }
        }, 1000);
    }
}
