package com.amd.radio;

public class RadioStation {
	
	private int freq;
	private String sfreq;
	private String stationName;
	
	public RadioStation(int freq,String sfreq,String stationName){
		this.freq = freq;
		this.stationName = stationName;
		this.sfreq = sfreq;
	}
	
	public void setFreq(int freq){
		this.freq = freq;
	}
	
	public int getFreq(){
		return this.freq;
	}
	
	public void setSfreq(String sfreq){
		this.sfreq = sfreq;
	}
	
	public String getSfreq(){
		return this.sfreq;
	}
	
	public void setStationName(String stationName){
		this.stationName = stationName;
	}
	
	public String getStationName(){
		return this.stationName;
	}
}
