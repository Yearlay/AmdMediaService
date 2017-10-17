package com.amd.radio;

public class Radio_Global {
	
	// 将频率点从整型转为字符串
	public static final int FREQ_MAX_LEN = 6;
	public static String freqIntToString(int freq) {
		String result = freq + "";
		if (result.length() > FREQ_MAX_LEN - 1)
			return null;

		StringBuffer sBuffer = new StringBuffer(10);
		sBuffer.append(result);
		if (freq > 5000) {
			if (freq > 9999) {
				result = sBuffer.insert(3, ".").toString();
			} else {
				result = sBuffer.insert(2, ".").toString();
			}
		}
		return result;
	}
}
