package com.amd.media;

import java.util.ArrayList;

import com.haoke.constant.MediaUtil.PlayState;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

public class AudioFocus {

	private static final String TAG = "AMD_AudioFocus";
	private Context mContext = null;
	private int mAudioFocusState = AudioManager.AUDIOFOCUS_LOSS;
	private AudioManager mAudioManager = null;
	private ArrayList<AudioFocusListener> mListenerList = new ArrayList<AudioFocusListener>();
	
	public interface AudioFocusListener {
		void audioFocusChanged(int state);	// 通知播放状态变化
	}

	public AudioFocus(Context context) {
		mContext = context;
		mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
	}
	
	public void registerListener(AudioFocusListener listener) {
		boolean found = false;
		for (int i = 0; i < mListenerList.size(); i++) {
			if (mListenerList.get(i) == listener) {
				found = true;
				break;
			}
		}
		if (!found)
			mListenerList.add(listener);
	}

	public void unRegisterListener(AudioFocusListener listener) {
		for (int i = 0; i < mListenerList.size(); i++) {
			if (mListenerList.get(i) == listener) {
				mListenerList.remove(i);
				break;
			}
		}
	}

	
	/**
	 * 申请音频焦点
	 * @param request 申请焦点true，否则false
	 * @param StreamType 音频流类型，比如AudioManager.STREAM_MUSIC
	 * @param durationHint 申请焦点时长,比如AudioManager.AUDIOFOCUS_GAIN
	 */
	public boolean requestAudioFocus(boolean request, int StreamType, int durationHint) {
		Log.d(TAG, "requestAudioFocus, request: " + request + ", StreamType" + StreamType
				+ ", durationHint: " + durationHint);
		return requestAudioFocus(request, StreamType, durationHint, false);
	}
	
	/**
	 * 申请音频焦点
	 * @param request 申请焦点true，否则false
	 * @param StreamType 音频流类型，比如AudioManager.STREAM_MUSIC
	 * @param durationHint 申请焦点时长,比如AudioManager.AUDIOFOCUS_GAIN
	 * @param forceRequest 是否强制申请，强制true,否则false，强制申请的话，如果已持有焦点，会先释放，再申请焦点，非特殊情况不建议使用
	 */
	public boolean requestAudioFocus(boolean request, int StreamType, 
								int durationHint, boolean forceRequest) {
		Log.d(TAG, "requestAudioFocus, request: " + request + ", StreamType" + StreamType
					+ ", durationHint: " + durationHint + ", forceRequest: " + forceRequest);
		
		if (mAudioManager == null) {
			Log.w(TAG, "requestAudioFocus mAudioManager == null");
			return false;
		}
		
		if (request && forceRequest && hasAudioFocus()) {
			mAudioManager.abandonAudioFocus(mAudioFocusListener);
			mAudioFocusState = AudioManager.AUDIOFOCUS_LOSS;
		}
		
		if (request) {
			if (!hasAudioFocus()) {
				int ret = mAudioManager.requestAudioFocus(mAudioFocusListener,
						StreamType, durationHint);
				
				if (ret == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
					mAudioFocusState = durationHint;
					onPlayStateChange(PlayState.PLAY);
					return true;
				} else {
					Log.w(TAG, "requestAudioFocus failed");
					return false;
				}
			} else {
				onPlayStateChange(PlayState.PLAY);
				return true;
			}
		} else {
			onPlayStateChange(PlayState.STOP);
			mAudioManager.abandonAudioFocus(mAudioFocusListener);
			mAudioFocusState = AudioManager.AUDIOFOCUS_LOSS;
			return true;
		}
	}
	
	public boolean requestAudioFocus(boolean request) {
		if (mAudioManager == null) {
			Log.e(TAG, "requestAudioFocus mAudioManager == null");
			return false;
		}
		
		if (request) {
			int ret = mAudioManager.requestAudioFocus(mAudioFocusListener,
					AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
			if (ret == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
				Log.v(TAG, "requestAudioFocus AUDIOFOCUS_REQUEST_GRANTED");
				mAudioFocusState = AudioManager.AUDIOFOCUS_GAIN;
				onPlayStateChange(PlayState.PLAY);
				return true;
			} else {
				Log.e(TAG, "requestAudioFocus failed");
				return false;
			}
		} else {
			onPlayStateChange(PlayState.STOP);
			mAudioManager.abandonAudioFocus(mAudioFocusListener);
			mAudioFocusState = AudioManager.AUDIOFOCUS_LOSS;
			return true;
		}
	}
	
	public boolean requestTransientAudioFocus(boolean request) {
		if (mAudioManager == null) {
			Log.e(TAG, "requestTransientAudioFocus mAudioManager == null");
			return false;
		}
		
		if (request) {
			int ret = mAudioManager.requestAudioFocus(mAudioFocusListener,
					AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
			if (ret == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
				Log.v(TAG, "requestTransientAudioFocus AUDIOFOCUS_GAIN_TRANSIENT");
				mAudioFocusState = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT;
//				onPlayStateChange(PlayState.PLAY);
				return true;
			} else {
				Log.e(TAG, "requestTransientAudioFocus GAIN_TRANSIENT failed");
				return false;
			}
		} else {
//			onPlayStateChange(PlayState.STOP);
			mAudioManager.abandonAudioFocus(mAudioFocusListener);
			mAudioFocusState = AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;
			return true;
		}
	}

	private AudioManager.OnAudioFocusChangeListener mAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
		@Override
		public void onAudioFocusChange(int arg0) {
			Log.v(TAG, "onAudioFocusChange arg0=" + arg0);
			switch (arg0) {
			case AudioManager.AUDIOFOCUS_GAIN:
				mAudioFocusState = arg0;
				onPlayStateChange(PlayState.PLAY);
				break;
			case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
				mAudioFocusState = arg0;
				onPlayStateChange(PlayState.PLAY);
				break;
			case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
				break;
			case AudioManager.AUDIOFOCUS_LOSS:
				requestAudioFocus(false);
				break;
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
				mAudioFocusState = arg0;
				onPlayStateChange(PlayState.PAUSE);
				break;
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				break;
			default:
				break;
			}
		}
	};

	public int getFocusState() {
		return mAudioFocusState;	
	}
	
	public boolean hasAudioFocus() {
		boolean ret = false;
		if (mAudioFocusState == AudioManager.AUDIOFOCUS_GAIN
				|| mAudioFocusState == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
				|| mAudioFocusState == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
			ret = true;

		return ret;
	}

	private void onPlayStateChange(int state) {
		Log.v(TAG, "onPlayStateChange state=" + state);
		for (int i = 0; i < mListenerList.size(); i++) {
			AudioFocusListener listener = mListenerList.get(i);
			if (listener == null)
				continue;

			listener.audioFocusChanged(state);
		}
	}
}
