package com.jsbd.util;

import com.amd.util.Source;
import com.haoke.util.Media_IF;

import android.util.Log;

public class Meter_IF {

	public static final String TAG="AMD_Meter_IF";
	
	//-------------------------------媒体部分 start-----------------------//
	
	//切源相关
	public static final int SOURCE_AM=0;
	public static final int SOURCE_FM=1;
	public static final int SOURCE_USB=2;
	public static final int SOURCE_HDD=3;
	public static final int SOURCE_IPOD=4;
	public static final int SOURCE_BTAUDIO=5;
	public static final int SOURCE_MOBILE_LINK=6;
	public static final int SOURCE_OTHER=7;
	
	//收音机相关
	public static final int BAND_AM=1;
	public static final int BAND_FM=2;
	
	/**
	 * 设置当前源
	 * @param source
	 */
	public static void notifyMeterMediaSrc(int source){
		
		try {
			Log.d(TAG, "notifyMeterMediaSrc source:"+source);
			if(source >= SOURCE_AM && source <= SOURCE_OTHER){
				byte[] data = new byte[4];
				data[0] = 0x02;
				data[1] = 0x01;
				data[2] = 0x01;
				data[3] = (byte)source;
				Media_IF.getInstance().sendToDashbroad(data);
			}else{
				throw new Exception("source id error!!!");
			}
		} catch (Exception e) {
			Log.e(TAG, Log.getStackTraceString(e));
		}
		
	}
	
	/**
	 * 设置收音机信息
	 * @param band
	 * @param freq
	 */
	public static void sendRadioInfo(int band, int freq){
	    boolean isRadioSource = Source.isRadioSource();
		Log.d(TAG, "sendRadioInfo: band="+band+"; freq="+freq+"; isRadioSource="+isRadioSource);
		if (!isRadioSource) {
		    return;
		}
		try {
			if(band != BAND_AM && band != BAND_FM){
				return;
			}
			
			byte[] data = new byte[6];
			data[0] = 0x04;
			data[1] = 0x01;
			data[2] = 0x03;
			if(band == BAND_AM){
				data[3] = 0x01;
			}else{
				data[3] = 0x02;
			}
			
			data[4] = (byte)(freq>>8);
			data[5] = (byte)freq;
			
			Media_IF.getInstance().sendToDashbroad(data);
		} catch (Exception e) {
			Log.e(TAG, Log.getStackTraceString(e));
		}
	}
	
	/**
	 * 发送ID3信息
	 * @param musicNmae 歌曲名长度(byte[]最大60)
	 * @param musicSinger 歌手名长度(byte[]最大60)
	 * @param musicAlbum 专辑名长度(byte最大60)
	 */
	public static void sendMusicInfo(String musicNmae, String musicSinger, String musicAlbum){
		try {
		    boolean isAudioSource = Source.isAudioSource();
			Log.d(TAG, "sendMusicInfo: musicNmae="+musicNmae+"; musicSinger="+musicSinger+"; musicAlbum="+musicAlbum+"; isAudioSource="+isAudioSource);
			if (!isAudioSource) {
			    return;
			}
			if(musicNmae==null){
				musicNmae="";
			}
			if(musicSinger==null){
				musicSinger="";
			}
			if(musicAlbum==null){
				musicAlbum="";
			}
			
			byte[] musicNmaeData = musicNmae.getBytes("UTF-8");
			if(musicNmaeData.length>60){
				byte[] temp = new byte[60];
				for(int i = 0; i < temp.length; i++){
					temp[i] = musicNmaeData[i];
				}
				musicNmaeData = temp;
			}
			
			byte[] musicSingerData = musicSinger.getBytes("UTF-8");
			if(musicSingerData.length>60){
				byte[] temp = new byte[60];
				for(int i = 0; i < temp.length; i++){
					temp[i] = musicSingerData[i];
				}
				musicSingerData = temp;
			}
			
			byte[] musicAlbumData = musicAlbum.getBytes("UTF-8");
			if(musicAlbumData.length>60){
				byte[] temp = new byte[60];
				for(int i = 0; i < temp.length; i++){
					temp[i] = musicAlbumData[i];
				}
				musicAlbumData = temp;
			}
			
			//3为三个长度
			byte[] data = new byte[(3+musicNmaeData.length+musicSingerData.length+musicAlbumData.length)];
			
			for(int i = 0; i < data.length; i++){
				if(i == 0){//musicNmaeData长度
					data[i] = (byte) musicNmaeData.length;
				}else if(i < musicNmaeData.length + 1){//musicNmaeData内容
					data[i] = musicNmaeData[i - 1];
				}else if(i == musicNmaeData.length + 1){//musicSingerData长度
					data[i] = (byte) musicSingerData.length;
				}else if(i>=musicNmaeData.length + 2 
						&& i < musicNmaeData.length + musicSingerData.length + 2){//musicSingerData内容
					data[i] = musicSingerData[i - (musicNmaeData.length + 2)];
				}else if(i == musicNmaeData.length + musicSingerData.length + 2){
					data[i] = (byte) musicAlbumData.length;
				}else if(i >= musicNmaeData.length + musicSingerData.length + 3
						&& i < musicNmaeData.length + musicSingerData.length + musicAlbumData.length + 3){
					data[i] = musicAlbumData[i-(musicNmaeData.length + musicSingerData.length + 3)];
				}
			}
			
			byte[] sendData = new byte[data.length+3];
			sendData[0] = 0x02;
			sendData[1] = 0x02;
			sendData[2] = (byte) data.length;
			for(int i = 3; i < sendData.length; i++){
				sendData[i] = data[i-3];
			}
			Media_IF.getInstance().sendToDashbroad(sendData);
			
		} catch (Exception e) {
			Log.e(TAG, Log.getStackTraceString(e));
		}
	}
	
	//-------------------------------媒体部分 end-----------------------//
}
