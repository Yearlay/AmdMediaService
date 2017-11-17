package com.haoke.scanner;

import com.haoke.constant.DBConfig;
import com.haoke.constant.MediaUtil.DeviceType;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

/**
 * 完成重构，重构日期：20170824
 * 目前支持的URI是查询的URI。
 * @author yelei
 *
 */
public class MediaContentProvider extends ContentProvider {

	private static final UriMatcher mUriMatcher  = new UriMatcher(UriMatcher.NO_MATCH);
	private MediaDbHelper mMediaDbHelper = null;

	static {
		mUriMatcher.addURI(DBConfig.MEDIA_DB_AUTOHORITY, DBConfig.TableName.SD1_AUDIO_TABLE_NAME, DBConfig.UriType.SD1_AUDIO);
		mUriMatcher.addURI(DBConfig.MEDIA_DB_AUTOHORITY, DBConfig.TableName.SD1_VIDEO_TABLE_NAME, DBConfig.UriType.SD1_VIDEO);
		mUriMatcher.addURI(DBConfig.MEDIA_DB_AUTOHORITY, DBConfig.TableName.SD1_IMAGE_TABLE_NAME, DBConfig.UriType.SD1_IMAGE);
		mUriMatcher.addURI(DBConfig.MEDIA_DB_AUTOHORITY, DBConfig.TableName.SD2_AUDIO_TABLE_NAME, DBConfig.UriType.SD2_AUDIO);
		mUriMatcher.addURI(DBConfig.MEDIA_DB_AUTOHORITY, DBConfig.TableName.SD2_VIDEO_TABLE_NAME, DBConfig.UriType.SD2_VIDEO);
		mUriMatcher.addURI(DBConfig.MEDIA_DB_AUTOHORITY, DBConfig.TableName.SD2_IMAGE_TABLE_NAME, DBConfig.UriType.SD2_IMAGE);
		mUriMatcher.addURI(DBConfig.MEDIA_DB_AUTOHORITY, DBConfig.TableName.USB1_AUDIO_TABLE_NAME, DBConfig.UriType.USB1_AUDIO);
		mUriMatcher.addURI(DBConfig.MEDIA_DB_AUTOHORITY, DBConfig.TableName.USB1_VIDEO_TABLE_NAME, DBConfig.UriType.USB1_VIDEO);
		mUriMatcher.addURI(DBConfig.MEDIA_DB_AUTOHORITY, DBConfig.TableName.USB1_IMAGE_TABLE_NAME, DBConfig.UriType.USB1_IMAGE);
		mUriMatcher.addURI(DBConfig.MEDIA_DB_AUTOHORITY, DBConfig.TableName.USB2_AUDIO_TABLE_NAME, DBConfig.UriType.USB2_AUDIO);
		mUriMatcher.addURI(DBConfig.MEDIA_DB_AUTOHORITY, DBConfig.TableName.USB2_VIDEO_TABLE_NAME, DBConfig.UriType.USB2_VIDEO);
		mUriMatcher.addURI(DBConfig.MEDIA_DB_AUTOHORITY, DBConfig.TableName.USB2_IMAGE_TABLE_NAME, DBConfig.UriType.USB2_IMAGE);
		mUriMatcher.addURI(DBConfig.MEDIA_DB_AUTOHORITY, DBConfig.TableName.USB3_AUDIO_TABLE_NAME, DBConfig.UriType.USB3_AUDIO);
		mUriMatcher.addURI(DBConfig.MEDIA_DB_AUTOHORITY, DBConfig.TableName.USB3_VIDEO_TABLE_NAME, DBConfig.UriType.USB3_VIDEO);
		mUriMatcher.addURI(DBConfig.MEDIA_DB_AUTOHORITY, DBConfig.TableName.USB3_IMAGE_TABLE_NAME, DBConfig.UriType.USB3_IMAGE);
		mUriMatcher.addURI(DBConfig.MEDIA_DB_AUTOHORITY, DBConfig.TableName.USB4_AUDIO_TABLE_NAME, DBConfig.UriType.USB4_AUDIO);
		mUriMatcher.addURI(DBConfig.MEDIA_DB_AUTOHORITY, DBConfig.TableName.USB4_VIDEO_TABLE_NAME, DBConfig.UriType.USB4_VIDEO);
		mUriMatcher.addURI(DBConfig.MEDIA_DB_AUTOHORITY, DBConfig.TableName.USB4_IMAGE_TABLE_NAME, DBConfig.UriType.USB4_IMAGE);
		mUriMatcher.addURI(DBConfig.MEDIA_DB_AUTOHORITY, DBConfig.TableName.FLASH_AUDIO_TABLE_NAME, DBConfig.UriType.FLASH_AUDIO);
		mUriMatcher.addURI(DBConfig.MEDIA_DB_AUTOHORITY, DBConfig.TableName.FLASH_VIDEO_TABLE_NAME, DBConfig.UriType.FLASH_VIDEO);
		mUriMatcher.addURI(DBConfig.MEDIA_DB_AUTOHORITY, DBConfig.TableName.FLASH_IMAGE_TABLE_NAME, DBConfig.UriType.FLASH_IMAGE);
	}

	@Override
	public boolean onCreate() {
		mMediaDbHelper = MediaDbHelper.instance(getContext());
		return false;
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		Cursor c = null;
		SQLiteDatabase db = mMediaDbHelper.getWritableDatabase();
		String tableName = DBConfig.getTableNameByUriType(mUriMatcher.match(uri));
		if (tableName != null) {
			c = db.query(tableName, projection, selection, selectionArgs, null, null, null);
		}
		return c;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}
}
