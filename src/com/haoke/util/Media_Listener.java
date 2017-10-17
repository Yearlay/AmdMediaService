package com.haoke.util;

public interface Media_Listener {

	// TODO 请在以下添加接口
	void onDataChange(int mode, int func, int data1, int data2);
	void setCurInterface(int data);             	// 内部使用：设置当前界面
}
