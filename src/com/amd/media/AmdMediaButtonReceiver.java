package com.amd.media;

import com.amd.bt.BT_IF;
import com.haoke.define.ModeDef;
import com.haoke.define.MediaDef.PlayState;
import com.haoke.ui.image.Image_Activity_Main;
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
        if (source == ModeDef.NULL) {
//            Media_IF.setCurSource(ModeDef.AUDIO);
            if (playState != PlayState.PLAY) {
            	Media_IF.getInstance().setPlayState(PlayState.PLAY);
            }
        } else if (source == ModeDef.AUDIO || source == ModeDef.VIDEO) {
            if (playState != PlayState.PLAY) {
            	Media_IF.getInstance().setPlayState(PlayState.PLAY);
            }
        } else if (source == ModeDef.BT) {
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
        if (source == ModeDef.NULL) {
            // do nothing
        } else if (source == ModeDef.AUDIO || source == ModeDef.VIDEO) {
            if (playState == PlayState.PLAY) {
            	Media_IF.getInstance().setPlayState(PlayState.PAUSE);
            }
        } else if (source == ModeDef.BT) {
            if (btPlaying) {
            	BT_IF.getInstance().music_pause();
            }
        }
    }
    
    private static void prev() {
        int source = Media_IF.getCurSource();
        Log.d(TAG, "prev source="+source);
        if (source == ModeDef.NULL) {
//            Media_IF.setCurSource(ModeDef.AUDIO);
            int playState = Media_IF.getInstance().getPlayState();
            if (playState != PlayState.PLAY) {
            	Media_IF.getInstance().setPlayState(PlayState.PLAY);
            }
        } else if (source == ModeDef.AUDIO || source == ModeDef.VIDEO) {
            if (!Media_IF.getInstance().playPre()) {
                Log.d(TAG, "prev mIF.playPre is false");
                Media_IF.getInstance().setPlayState(PlayState.PLAY);
            }
        } else if (source == ModeDef.BT) {
        	BT_IF.getInstance().music_pre();
        }
    }
    
    private static void next() {
        int source = Media_IF.getCurSource();
        Log.d(TAG, "next source="+source);
        if (source == ModeDef.NULL) {
//            Media_IF.setCurSource(ModeDef.AUDIO);
            int playState = Media_IF.getInstance().getPlayState();
            if (playState != PlayState.PLAY) {
            	Media_IF.getInstance().setPlayState(PlayState.PLAY);
            }
        } else if (source == ModeDef.AUDIO || source == ModeDef.VIDEO) {
            if (!Media_IF.getInstance().playNext()) {
                Log.d(TAG, "next mIF.playNext is false");
                Media_IF.getInstance().setPlayState(PlayState.PLAY);
            }
        } else if (source == ModeDef.BT) {
        	BT_IF.getInstance().music_next();
        }
    }
}