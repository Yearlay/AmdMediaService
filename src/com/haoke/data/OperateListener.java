package com.haoke.data;

public interface OperateListener {
	public static final int OPERATE_DELETE = 1;
	public static final int OPERATE_COLLECT = 2;
	public static final int OPERATE_UNCOLLECT = 3;
	public static final int OPERATE_COPY_TO_LOCAL = 4;

	public static final int OPERATE_SUCEESS = 0;
	public static final int OPERATE_DELETE_NOT_EXIST = 1001;
	public static final int OPERATE_DELETE_READ_ONLY = 1002;
	public static final int OPERATE_DELETE_ERROR = 1003;
	public static final int OPERATE_COLLECT_COPY_FILE_FAILED = 2001;
	public static final int OPERATE_UNCOLLECT_FILE_NOT_EXIST = 3001;
	public static final int OPERATE_UNCOLLECT_DELETE_FILE_ERROR = 3002;
	
	/**
	 * 文件操作的回调函数。
	 * @param operateValue 当前的操作，1删除；2收藏；3取消收藏
	 * @param progress 当前的进度，例如 100%，值为100。
	 * @param resultCode 操作的返回状态，0表示成功，非0表示失败，可以看OperateListener中的常量解释。
	 */
	public void onOperateCompleted(int operateValue, int progress, int resultCode);
}
