package com.haoke.util;

public interface Media_CarListener {

	// TODO 请在以下添加接口
	void onCarDataChange(int mode, int func, int data);
	void onUartDataChange(int mode, int len, byte[] datas);
}
