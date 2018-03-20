package com.amd.radio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class BDBroadcastReceiver extends BroadcastReceiver {

	private String TAG = "BDBroadcastReceiver";
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		
		String action = intent.getAction();
		Log.d(TAG, "onReceive action0319:" + action);
		if (action == null) {
			return;
		}
		if (action.equalsIgnoreCase("AUTONAVI_STANDARD_BROADCAST_SEND")) {
			int KEY_TYPE = intent.getIntExtra("KEY_TYPE", -1);
			Log.d(TAG, "AUTONAVI_STANDARD_BROADCAST_SEND KEY_TYPE:" + KEY_TYPE);
			if (KEY_TYPE == 10030) {
				String PROVINCE_NAME = intent.getStringExtra("PROVINCE_NAME");
				String CITY_NAME = intent.getStringExtra("CITY_NAME");
				if(PROVINCE_NAME==null){
					PROVINCE_NAME="";
				}
				if(CITY_NAME==null){
					CITY_NAME="";
				}
				LocationUtils.setReceiveFlag();
				Radio_SimpleSave.setCityFromReceiver(PROVINCE_NAME, CITY_NAME);
		        //Radio_SimpleSave.getInstance().setCity(PROVINCE_NAME, CITY_NAME);
				Log.d(TAG,
						"AUTONAVI_STANDARD_BROADCAST_SEND  PROVINCE_NAME:" + PROVINCE_NAME + " CITY_NAME:" + CITY_NAME);
			} else if (KEY_TYPE == 10077) {
			    String PROVINCE_NAME = intent.getStringExtra("PROVINCE_NAME");
                String CITY_NAME = intent.getStringExtra("CITY_NAME");
                if(PROVINCE_NAME==null){
                    PROVINCE_NAME="";
                }
                if(CITY_NAME==null){
                    CITY_NAME="";
                }
                LocationUtils.setReceiveFlag();
                Radio_SimpleSave.setCityFromReceiver(PROVINCE_NAME, CITY_NAME);
                //Radio_SimpleSave.getInstance().setCity(PROVINCE_NAME, CITY_NAME);
                Log.d(TAG,
                        "AUTONAVI_STANDARD_BROADCAST_SEND  PROVINCE_NAME:" + PROVINCE_NAME + " CITY_NAME:" + CITY_NAME);
			}
		}
	}

}
