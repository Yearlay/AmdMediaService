package com.amd.media;

import com.amd.bt.BT_IF;
import com.amd.util.Source;
import com.haoke.constant.MediaUtil.PlayState;
import com.haoke.ui.image.Image_Activity_Main;
import com.haoke.ui.video.Video_IF;
import com.haoke.util.Media_IF;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

public class AmdMediaButtonReceiver extends BroadcastReceiver {
    private static final String TAG = "AmdMediaButtonReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
    	onMediaButtonReceive(context, intent);
    }
    
    public static void onMediaButtonReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive action="+action);
        if (MediaInterfaceUtil.mediaCannotPlay()) {
            return;
        }
        if (Intent.ACTION_MEDIA_BUTTON.equals(action)) {
            KeyEvent event = (KeyEvent) intent
                    .getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null || event.getAction() != KeyEvent.ACTION_UP) {
                return;
            }
            int keycode = event.getKeyCode();
            Log.d(TAG, "onReceive keycode="+keycode);
            switch (keycode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY: //126
                if (!Image_Activity_Main.isPlayImage(context)) {
                    play();
                }
                break;
            case KeyEvent.KEYCODE_MEDIA_PAUSE: //127
                if (!Image_Activity_Main.isPlayImage(context)) {
                    pause();
                }
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS: //88
                prev();
                break;
            case KeyEvent.KEYCODE_MEDIA_NEXT: //87
                next();
                break;
            }
        }
    }
    
    private static void play() {
        int source = Media_IF.getCurSource();
        int playState = Media_IF.getInstance().getPlayState();
        boolean btPlaying = BT_IF.getInstance().music_isPlaying();
        Log.d(TAG, "play source="+source+"; playState="+playState+"; btPlaying="+btPlaying);
        if (source == Source.NULL) {
//            Media_IF.setCurSource(ModeDef.AUDIO);
            if (playState != PlayState.PLAY) {
                Media_IF.getInstance().setPlayState(PlayState.PLAY);
            }
        } else if (Source.isAudioSource(source)) {
            if (playState != PlayState.PLAY) {
                Media_IF.getInstance().setPlayState(PlayState.PLAY);
            }
        } else if (Source.isVideoSource(source)) {
        	int state = Video_IF.getInstance().getPlayState();
        	if (state != PlayState.PLAY) {
        		Video_IF.getInstance().setPlayState(PlayState.PLAY);
            }
        } else if (Source.isBTMusicSource(source)) {
            if (!btPlaying) {
            	BT_IF.getInstance().music_play();
            }
        }
    }
    
    private static void pause() {
        int source = Media_IF.getCurSource();
        int playState = Media_IF.getInstance().getPlayState();
        boolean btPlaying = BT_IF.getInstance().music_isPlaying();
        Log.d(TAG, "pause source="+source+"; playState="+playState+"; btPlaying="+btPlaying);
        if (source == Source.NULL) {
            // do nothing
        } else if (Source.isAudioSource(source)) {
            if (playState == PlayState.PLAY) {
                Media_IF.getInstance().setScanMode(false);
                Media_IF.getInstance().setPlayState(PlayState.PAUSE);
            }
        } else if (Source.isVideoSource(source)) {
        	int state = Video_IF.getInstance().getPlayState();
        	if (state != PlayState.PLAY) {
        		Video_IF.getInstance().setPlayState(PlayState.PAUSE);
            }
        } else if (Source.isBTMusicSource(source)) {
            if (btPlaying) {
            	BT_IF.getInstance().music_pause();
            }
        }
    }
    
    private static void prev() {
        int source = Media_IF.getCurSource();
        Log.d(TAG, "prev source="+source);
        if (source == Source.NULL) {
//            Media_IF.setCurSource(ModeDef.AUDIO);
            int playState = Media_IF.getInstance().getPlayState();
            if (playState != PlayState.PLAY) {
                Media_IF.getInstance().setScanMode(false);
                Media_IF.getInstance().setPlayState(PlayState.PLAY);
            }
        } else if (Source.isAudioSource(source)) {
            Media_IF.getInstance().setScanMode(false);
            if (!Media_IF.getInstance().playPre()) {
                Log.d(TAG, "prev mIF.playPre is false");
                Media_IF.getInstance().setPlayState(PlayState.PLAY);
            }
        } else if (Source.isVideoSource(source)) {
            if (!Video_IF.getInstance().playPre()) {
                Log.d(TAG, "prev mIF.playPre is false");
                Video_IF.getInstance().setPlayState(PlayState.PLAY);
            }
        } else if (Source.isBTMusicSource(source)) {
        	BT_IF.getInstance().music_pre();
        }
    }
    
    private static void next() {
        int source = Media_IF.getCurSource();
        Log.d(TAG, "next source="+source);
        if (source == Source.NULL) {
            int playState = Media_IF.getInstance().getPlayState();
            if (playState != PlayState.PLAY) {
                Media_IF.getInstance().setScanMode(false);
            	Media_IF.getInstance().setPlayState(PlayState.PLAY);
            }
        } else if (Source.isAudioSource(source)) {
            Media_IF.getInstance().setScanMode(false);
            if (!Media_IF.getInstance().playNext()) {
                Log.d(TAG, "next mIF.playNext is false");
                Media_IF.getInstance().setPlayState(PlayState.PLAY);
            }
        } else if (Source.isVideoSource(source)) {
            if (!Video_IF.getInstance().playNext()) {
                Log.d(TAG, "next mIF.playNext is false");
                Video_IF.getInstance().setPlayState(PlayState.PLAY);
            }
        } else if (Source.isBTMusicSource(source)) {
        	BT_IF.getInstance().music_next();
        }
    }
}