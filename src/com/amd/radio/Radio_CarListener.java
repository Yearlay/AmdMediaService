package com.amd.radio;

public interface Radio_CarListener {

	// TODO 请在以下添加接口
	void onCarDataChange(int mode, int func, int data);
	void setCurInterface(int data);             // 内部使用：设置当前界面
}
