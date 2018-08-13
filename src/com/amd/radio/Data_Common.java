package com.amd.radio;

import java.util.ArrayList;

import android.text.TextUtils;

public class Data_Common {
	public static final int REQUEST_FREQ = 1001;
	
	//public static ArrayList<String> list = new ArrayList<String>() ;
	public static ArrayList<RadioStation> stationList = new ArrayList<RadioStation>();
	public static ArrayList<String> tempFreq = new ArrayList<String>() ;
	public static ArrayList<RadioStation> collectAllFreqs = null;//new ArrayList<RadioStation>() ;
//	public static String type = "fm" ;
	public static int pager = 0 ;
	public static int reminder = 0 ;
	
	/*static {
        stationList.add(new RadioStation(8800, "88.00", "江苏人民广播电台健康广播11"));
        stationList.add(new RadioStation(8820, "88.20", "江苏人民广播电台健康广播"));
        stationList.add(new RadioStation(9360, "93.60", "江苏人民广播电台新闻广播"));
        stationList.add(new RadioStation(9380, "93.80", "江苏人民广播电台新闻广播"));
        stationList.add(new RadioStation(9800, "98.00", "沭阳县广播电视台广播节目"));
        stationList.add(new RadioStation(9850, "98.50", "沭阳县广播电视台广播节目"));
        stationList.add(new RadioStation(10100, "101.00", "江苏人民广播电台交通广播网"));
        stationList.add(new RadioStation(10130, "101.30", "江苏人民广播电台交通广播网"));
        stationList.add(new RadioStation(10150, "101.50", "中央人民广播电台中国之声"));
        stationList.add(new RadioStation(10170, "101.70", "中央人民广播电台中国之声"));
		stationList.add(new RadioStation(10200, "102.00", "宿迁人民广播电台综合广播"));
        stationList.add(new RadioStation(10250, "102.50", "宿迁人民广播电台综合广播"));
        stationList.add(new RadioStation(10310, "103.10", "泗洪县广播电视台广播节目"));
		stationList.add(new RadioStation(10340, "103.40", "泗洪县广播电视台广播节目"));
		stationList.add(new RadioStation(10770, "107.70", "宿迁人民广播电台综合广播"));
		stationList.add(new RadioStation(10790, "107.90", "宿迁人民广播电台综合广播"));
	}*/
	
//	/****
//	 * 通过频率值（整型或字符串）在所有电台中查找
//	 * @param freq 若为-1，则sfreq生效
//	 * @param sfreq 默认不生效
//	 */
//	public static RadioStation getRadioStation(int freq, String sfreq) {
//		if (freq == -1 && TextUtils.isEmpty(sfreq)) {
//			return null;
//		}
//		for (RadioStation station : stationList) {
//			if (freq != -1) {
//				if (station.getFreq() == freq) {
//					return station;
//				}
//			} else {
//				if (sfreq.equals(station.getFreq())) {
//					return station;
//				}
//			}
//		}
//		return null;
//	}
//	
//	
//	/****
//	 * 通过频率值（整型或字符串）<b>添加</b>对应的收藏
//	 * @param freq 若为-1，则sfreq生效
//	 * @param sfreq 默认不生效<p>
//	 * {@link Data_Common#removeCollectFreq}
//	 */
//	public static void addCollectFreq(int freq, String sfreq) {
//		if (freq == -1 && TextUtils.isEmpty(sfreq)) {
//			return ;
//		}
//		for (RadioStation station : stationList) {
//			if (freq != -1) {
//				if (station.getFreq() == freq) {
//					collectAllFreqs.add(station);
//					return;
//				}
//			} else {
//				if (sfreq.equals(station.getFreq())) {
//					collectAllFreqs.add(station);
//					return;
//				}
//			}
//		}
//	}
//	
	/****
	 * 通过频率值（整型或字符串）<b>删除</b>对应的收藏
	 * @param freq 若为-1，则sfreq生效
	 * @param sfreq 默认不生效<p>
	 * {@link Data_Common#addCollectFreq}
	 */
	public static void removeCollectFreq(int freq, String sfreq) {
		if (freq == -1 && TextUtils.isEmpty(sfreq)) {
			return ;
		}
		for (RadioStation station : collectAllFreqs) {
			if (freq != -1) {
				if (station.getFreq() == freq) {
					collectAllFreqs.remove(station);
					return;
				}
			} else {
				if (sfreq.equals(station.getFreq())) {
					collectAllFreqs.remove(station);
					return;
				}
			}
		}
	}
}
