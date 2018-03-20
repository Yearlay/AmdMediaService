package com.amd.radio;

import com.haoke.util.DebugLog;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class RadioDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "RadioDatabaseHelper";
	
	public static final String CREATE_RADIO_FM = "create table Radio_FM ("
	        + "username text, "
			+ "ifreq integer, "
			+ "sname text, "
			+ "freq text)";
	
	
	public static final String CREATE_RADIO_AM = "create table Radio_AM ("
            + "username text, "
			+ "ifreq integer, "
			+ "sname text, "
			+ "freq text)";
	
	public RadioDatabaseHelper(Context context, String name, CursorFactory factory, int version) {
		super(context.getApplicationContext(), name, factory, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_RADIO_FM);
		//db.execSQL(CREATE_RADIO_AM);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	    DebugLog.d(TAG, "onUpgrade oldVersion="+oldVersion+"; newVersion="+newVersion);
	    int curVersion = oldVersion;
		if (curVersion == 1) {
		    db.execSQL("DROP TABLE IF EXISTS Radio_FM");
		}
		onCreate(db);
	}

}
