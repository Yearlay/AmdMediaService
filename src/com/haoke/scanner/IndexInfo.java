/**
 * @author 林永彬
 * 说明：索引信息，记录mMediaMap的key，以及每个key中list的序号
 */

package com.haoke.scanner;

import android.util.Log;

public class IndexInfo {

	private String mKey; // mMediaMap的key
	private int mIndex; // 每个key中list的序号
	private String mFilePath; // 文件绝对路径
	private boolean mIsSelected = false; // 是否选中，可对选中的项进行操作
	private boolean mIsSaved = false;//是否已收藏歌曲

	public void setKey(String key) {
		synchronized (this) {
			mKey = key;
		}
	}

	public String getKey() {
		synchronized (this) {
			return mKey;
		}
	}

	public void setIndex(int index) {
		synchronized (this) {
			Log.v("IndexInfo", "setIndex() index=" + index);
			mIndex = index;
		}
	}

	public int getIndex() {
		synchronized (this) {
			Log.v("IndexInfo", "getIndex() mIndex=" + mIndex);
			return mIndex;
		}
	}

	public void setFilePath(String path) {
		synchronized (this) {
			mFilePath = path;
		}
	}

	public String getFilePath() {
		synchronized (this) {
			return mFilePath;
		}
	}

	public void select(boolean isSelect) {
		synchronized (this) {
			Log.v("IndexInfo", "select() isSelect=" + isSelect);
			mIsSelected = isSelect;
		}
	}

	public boolean isSelected() {
		synchronized (this) {
			Log.v("IndexInfo", "isSelected() mIsSelected=" + mIsSelected);
			return mIsSelected;
		}
	}

	public boolean isSaved() {
		synchronized (this) {
			Log.v("IndexInfo", "isSaved() mIsSaved=" + mIsSaved);
			return mIsSaved;
		}
	}

	public void setSaved(boolean saved) {
		synchronized (this) {
			Log.v("IndexInfo", "setSaved() saved=" + saved);
			this.mIsSaved = saved;
		}
	}
	
	
}
