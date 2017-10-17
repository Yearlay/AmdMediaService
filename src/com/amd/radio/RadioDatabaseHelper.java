package com.amd.radio;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class RadioDatabaseHelper extends SQLiteOpenHelper {
	
	public static final String CREATE_RADIO_FM = "create table Radio_FM ("
			+ "name text primary key, "
			+ "ifreq integer, "
			+ "sname text, "
			+ "freq text)";
	
	
	public static final String CREATE_RADIO_AM = "create table Radio_AM ("
			+ "name text primary key, "
			+ "ifreq integer, "
			+ "sname text, "
			+ "freq text)";
	
//	private Context mContext;
	
	public RadioDatabaseHelper(Context context, String name, CursorFactory factory, int version) {
		super(context.getApplicationContext(), name, factory, version);
//		mContext = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_RADIO_FM);
		//db.execSQL(CREATE_RADIO_AM);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}

}
