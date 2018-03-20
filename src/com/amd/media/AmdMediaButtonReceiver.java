package com.amd.media;

import com.amd.bt.BT_IF;
import com.amd.radio.Radio_IF;
import com.amd.util.AmdConfig;
import com.amd.util.Source;
import com.haoke.constant.MediaUtil.PlayState;
import com.haoke.ui.image.Image_Activity_Main;
import com.haoke.util.DebugLog;
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
        DebugLog.d(TAG, "onReceive action="+action);
        if (MediaInterfaceUtil.mediaCannotPlay()) {
            return;
        }
        if (Intent.ACTION_MEDIA_BUTTON.equals(action)) {
            KeyEvent event = (KeyEvent) intent
                    .getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null || event.getAction() != KeyEvent.ACTION_UP) {
                return;
            }
            boolean longPress = event.isLongPress();
            int repeatCount = event.getRepeatCount();
            int keycode = event.getKeyCode();
            DebugLog.d(TAG, "onReceive keycode="+keycode+"; longPress="+longPress+"; repeatCount="+repeatCount);
            if (longPress) {
                //return;
            }
            handleMediaKey(context, keycode, true);
        }
    }
    
    public static boolean onKeyUp(Context context, int keyCode) {
        if (AmdConfig.ENABLE_MEDIA_KEY_HANDLE_IN_ACTIVITY) {
            switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                if (AmdMediaButtonReceiver.handleMediaKey(context, keyCode, false)) {
                    return true;
                }
                break;
            }
        }
        return false;
    }
    
    private static boolean handleMediaKey(Context context, int keyCode, boolean receive) {
        boolean handle = false;
        if (!receive) {
            if (!Media_IF.hasAudioOrBtFocus()) {
                DebugLog.d(TAG, "handleMediaKey hasAudioOrBtFocus return!");
                return handle;
            }
        }
        if (MediaInterfaceUtil.isButtonClickTooFast()) {
            return true;
        }
        context = context.getApplicationContext();
        switch (keyCode) {
        case KeyEvent.KEYCODE_MEDIA_PLAY: //126
            handle = play();
            /*if (!Image_Activity_Main.isPlayImage(context)) {
                handle = play();
            } else {
                DebugLog.d(TAG, "isPlayImage!");
            }*/
            break;
        case KeyEvent.KEYCODE_MEDIA_PAUSE: //127
            handle = pause();
            /*if (!Image_Activity_Main.isPlayImage(context)) {
                handle = pause();
            } else {
                DebugLog.d(TAG, "isPlayImage!");
            }*/
            break;
        case KeyEvent.KEYCODE_MEDIA_PREVIOUS: //88
            handle = prev();
            break;
        case KeyEvent.KEYCODE_MEDIA_NEXT: //87
            handle = next();
            break;
        }
        DebugLog.d(TAG, "handleMediaKey   keyCode="+keyCode+"; handle="+handle);
        return handle;
    }
    
    private static boolean play() {
        int source = Media_IF.getCurSource();
        int playState = Media_IF.getInstance().getPlayState();
        boolean btPlaying = BT_IF.getInstance().music_isPlaying();
        boolean radioPlaying = Radio_IF.getInstance().isEnable();
        DebugLog.d(TAG, "play source="+source+"; playState="+playState+"; btPlaying="+btPlaying);
        if (source == Source.NULL) {
            Media_IF.getInstance().setRecordPlayState(PlayState.STOP);
            if (playState != PlayState.PLAY) {
                Media_IF.getInstance().setPlayState(PlayState.PLAY);
            }
            return true;
        } else if (Source.isAudioSource(source)) {
            Media_IF.getInstance().setRecordPlayState(PlayState.STOP);
            if (playState != PlayState.PLAY) {
                Media_IF.getInstance().setPlayState(PlayState.PLAY);
            }
            return true;
        } else if (Source.isBTMusicSource(source)) {
            BT_IF.getInstance().setRecordPlayState(PlayState.STOP);
            if (!btPlaying) {
            	BT_IF.getInstance().music_play();
            }
            return true;
        }else if(Source.isRadioSource(source)){
            Radio_IF.getInstance().setRecordRadioOnOff(false);
            if (!radioPlaying) {
                Radio_IF.getInstance().setEnable(true);
            }
            return true;
        }
        return false;
    }
    
    private static boolean pause() {
        int source = Media_IF.getCurSource();
        int playState = Media_IF.getInstance().getPlayState();
        boolean btPlaying = BT_IF.getInstance().music_isPlaying();
        boolean radioPlaying = Radio_IF.getInstance().isEnable();
        DebugLog.d(TAG, "pause source="+source+"; playState="+playState+"; btPlaying="+btPlaying);
        if (source == Source.NULL) {
            // do nothing
        } else if (Source.isAudioSource(source)) {
            Media_IF.getInstance().setRecordPlayState(PlayState.STOP);
            Media_IF.getInstance().setScanMode(false);
            if (playState == PlayState.PLAY) {
                Media_IF.getInstance().setPlayState(PlayState.PAUSE);
            }
            return true;
        }else if (Source.isBTMusicSource(source)) {
            BT_IF.getInstance().setRecordPlayState(PlayState.STOP);
            if (btPlaying) {
            	BT_IF.getInstance().music_pause();
            }
            return true;
        }else if(Source.isRadioSource(source)){
            Radio_IF.getInstance().setRecordRadioOnOff(false);
            if (radioPlaying) {
                Radio_IF.getInstance().setEnable(false);
            }
            return true;
        }
        return false;
    }
    
    private static boolean prev() {
        int source = Media_IF.getCurSource();
        DebugLog.d(TAG, "prev source="+source);
        if (source == Source.NULL) {
            int playState = Media_IF.getInstance().getPlayState();
            if (playState != PlayState.PLAY) {
                Media_IF.getInstance().setRecordPlayState(PlayState.STOP);
                Media_IF.getInstance().setScanMode(false);
                Media_IF.getInstance().setPlayState(PlayState.PLAY);
            }
            return true;
        } else if (Source.isAudioSource(source)) {
            Media_IF.getInstance().setRecordPlayState(PlayState.STOP);
            Media_IF.getInstance().setScanMode(false);
            if (!Media_IF.getInstance().playPre()) {
                DebugLog.d(TAG, "prev mIF.playPre is false");
                Media_IF.getInstance().setPlayState(PlayState.PLAY);
            }
            return true;
        }  else if (Source.isBTMusicSource(source)) {
            BT_IF.getInstance().setRecordPlayState(PlayState.STOP);
        	BT_IF.getInstance().music_pre();
        	return true;
        }else if(Source.isRadioSource(source)){
            DebugLog.d(TAG, "prev setPreChannel");
            Radio_IF.getInstance().setRecordRadioOnOff(false);
            Radio_IF.getInstance().setPreStep();
            return true;
        }
        return false;
    }
    
    private static boolean next() {
        int source = Media_IF.getCurSource();
        DebugLog.d(TAG, "next source="+source);
        if (source == Source.NULL) {
            int playState = Media_IF.getInstance().getPlayState();
            if (playState != PlayState.PLAY) {
                Media_IF.getInstance().setRecordPlayState(PlayState.STOP);
                Media_IF.getInstance().setScanMode(false);
            	Media_IF.getInstance().setPlayState(PlayState.PLAY);
            }
            return true;
        } else if (Source.isAudioSource(source)) {
            Media_IF.getInstance().setRecordPlayState(PlayState.STOP);
            Media_IF.getInstance().setScanMode(false);
            if (!Media_IF.getInstance().playNext()) {
                Media_IF.getInstance().setPlayState(PlayState.PLAY);
            }
            return true;
        }  else if (Source.isBTMusicSource(source)) {
            BT_IF.getInstance().setRecordPlayState(PlayState.STOP);
        	BT_IF.getInstance().music_next();
        	return true;
        }else if(Source.isRadioSource(source)){
            Radio_IF.getInstance().setRecordRadioOnOff(false);
            Radio_IF.getInstance().setNextStep();
            return true;
        }
        return false;
    }
}