package com.amd.radio;

public interface Radio_CarListener {

	// TODO 请在以下添加接口
	void onRadioCarDataChange(int mode, int func, int data);
	void setRadioCurInterface(int data);             // 内部使用：设置当前界面
}
