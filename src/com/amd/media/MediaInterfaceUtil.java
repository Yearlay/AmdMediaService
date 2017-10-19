package com.amd.media;

import android.media.AudioManager;
import android.util.Log;

import com.amd.bt.BT_IF;
import com.amd.radio.Radio_IF;
import com.haoke.application.MediaApplication;
import com.haoke.audiofocus.AudioFocus;
import com.haoke.audiofocus.AudioFocusListener;
import com.haoke.btjar.main.BTDef.BTConnState;
import com.haoke.define.ModeDef;
import com.haoke.define.MediaDef.PlayState;
import com.haoke.util.Media_IF;

public class MediaInterfaceUtil {
	private static final String TAG = "MediaInterfaceUtil";
	
	private static int sMediaPlayStateRecord = ModeDef.NULL;
	
	private static AudioFocus mAudioFocus;
	
	public static void resetMediaPlayStateRecord(int source) {
		Log.d(TAG, "resetMediaPlayStateRecord old is "+sMediaPlayStateRecord + "; caller is "+source);
		if (source == sMediaPlayStateRecord) {
			sMediaPlayStateRecord = ModeDef.NULL;
		}
	}
	
	public static void resetMediaPlayStateRecord() {
		Log.d(TAG, "resetMediaPlayStateRecord old is "+sMediaPlayStateRecord );
		sMediaPlayStateRecord = ModeDef.NULL;
	}
	
	private static void setMediaPlayStateRecord(int source) {
		Log.d(TAG, "sMediaPlayStateRecord source="+source+"; old is "+sMediaPlayStateRecord);
		sMediaPlayStateRecord = source;
	}
	
	public static int getMediaPlayStateRecord() {
		return sMediaPlayStateRecord;
	}
	
	private static boolean hasAudioFocus() {
		boolean ret = false;
		if (mAudioFocus != null) {
			int audioFocusState = mAudioFocus.getFocusState();
			if (audioFocusState == AudioManager.AUDIOFOCUS_GAIN
					|| audioFocusState == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
				ret = true;
		}
		return ret;
	}
	
	public static void setMuteRecordPlayState() {
		if (true) {
			if (mAudioFocus == null) {
				mAudioFocus = new AudioFocus(MediaApplication.getInstance());
				mAudioFocus.registerListener(mAudioFocusListener);
			}
			mAudioFocus.requestTransientAudioFocus(true);
			return;
		}
		resetMediaPlayStateRecord(); 
		boolean mute = Media_IF.getMute();
		if (mute) {
			int source = Media_IF.getCurSource();
			if (source == ModeDef.RADIO) {
				if (Radio_IF.getInstance().isEnable()) {
					setMediaPlayStateRecord(source);
					Radio_IF.getInstance().setEnable(false);
				}
			} else if (source == ModeDef.AUDIO) {
				if (Media_IF.getInstance().isPlayState()) {
					setMediaPlayStateRecord(source);
					Media_IF.getInstance().setPlayState(PlayState.PAUSE);
				}
			} else if (source == ModeDef.VIDEO) {
				if (Media_IF.getInstance().isPlayState()) {
					setMediaPlayStateRecord(source);
					Media_IF.getInstance().setPlayState(PlayState.PAUSE);
				}
			} else if (source == ModeDef.BT) {
				if (BT_IF.getInstance().music_isPlaying()) {
					setMediaPlayStateRecord(source);
					BT_IF.getInstance().music_pause();
				}
			}
		}
		Log.d(TAG, "setMute mute="+mute+"; sMediaPlayStateRecord="+sMediaPlayStateRecord);
	}
	
	public static void cancelMuteRecordPlayState() {
		Log.d(TAG, "cancelMuteRecordPlayState");
		if (true) {
			if (hasAudioFocus()) {
				mAudioFocus.requestTransientAudioFocus(false);
			}
			return;
		}
		int source = getMediaPlayStateRecord();
		if (source != ModeDef.NULL) {
			if (source == ModeDef.RADIO) {
				Radio_IF.getInstance().setEnable(true);
			} else if (source == ModeDef.AUDIO) {
				Media_IF.getInstance().setPlayState(PlayState.PLAY);
			} else if (source == ModeDef.VIDEO) {
				Media_IF.getInstance().setPlayState(PlayState.PLAY);
			} else if (source == ModeDef.BT) {
				if (BT_IF.getInstance().getConnState() == BTConnState.CONNECTED) {
					BT_IF.getInstance().music_play();
				}
			}
			resetMediaPlayStateRecord();
		}
	}
	
	private static AudioFocusListener mAudioFocusListener = new AudioFocusListener() {
		@Override
		public void audioFocusChanged(int state) {
			switch (state) {
			case PlayState.PLAY:
				break;
			case PlayState.PAUSE:
				break;
			case PlayState.STOP:
				if (Media_IF.getMute()) {
					Media_IF.cancelMute();
				}
				break;
			}
		}
	};
	
	/**
	 * 打电话时，不能点击媒体的播放按钮，即点击无效。
	 * @return true为媒体不能播放，false为可以播放。
	 */
	public static boolean mediaCannotPlay() {
		return Media_IF.getCallState();
	}
}