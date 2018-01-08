package com.haoke.ui.widget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.amd.bt.BT_IF;
import com.haoke.bean.FileNode;
import com.haoke.bean.ID3Parse;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.constant.MediaUtil.PlayState;
import com.haoke.mediaservice.R;
import com.haoke.util.DebugLog;
import com.haoke.util.Media_IF;
import com.amd.media.MediaInterfaceUtil;
import com.amd.radio.Radio_IF;
import com.amd.util.Source;

public class MediaWidgetProvider {
    
    private static void showToastNoMedia(Context context) {
        try {
            //MediaInterfaceUtil.showToast(R.string.no_media_can_play, Toast.LENGTH_SHORT);
            Toast.makeText(context, R.string.no_media_can_play, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e("MediaWidgetProvider", "showToast e="+e);
        }
    }

    public static void onClickMusicPlayButton(Context context) {
        int source = Media_IF.getCurSource();
        if (Source.isBTMusicSource(source)) {
            if (BT_IF.getInstance().music_isPlaying()) {
                BT_IF.getInstance().music_pause();
                BT_IF.getInstance().setRecordPlayState(PlayState.PAUSE);
            } else {
                BT_IF.getInstance().music_play();
                BT_IF.getInstance().setRecordPlayState(PlayState.PLAY);
            }
        } else if (Source.isAudioSource(source)) {
            if (getFileNode(context) != null) {
                Media_IF.getInstance().changePlayState();
            } else {
                showToastNoMedia(context);
            }
        } else {
            if (Source.isBTMusicSource(Media_IF.sLastSource)) {
                BT_IF.getInstance().music_play();
            } else {
                if (Media_IF.getInstance().getPlayingFileType() == FileType.AUDIO) {
                    // 只有是音乐的情况下，才会播放。
                    Media_IF.getInstance().setPlayState(PlayState.PLAY);
                } else {
                    // 如果不是音乐，就想办法播放音乐。 
                    FileNode fileNode = getFileNode(context);
                    if (fileNode != null) {
                        Media_IF.getInstance().play(getFileNode(context));
                    } else {
                        showToastNoMedia(context);
                    }
                }
            }
        }
    }
    
    public static void onClickMusicPreButton(Context context) {
        if (Source.isBTMusicSource()) {
            BT_IF.getInstance().music_pre();
        } else if (Source.isAudioSource()) {
            FileNode fileNode = getFileNode(context);
            if (fileNode != null) {
                boolean ret = Media_IF.getInstance().playPre();
                if (!ret) {
                    DebugLog.e("Yearlay", "Error AppWidget#playPre ...");
                }
            } else {
                showToastNoMedia(context);
            }
        } else {
            if (Source.isBTMusicSource(Media_IF.sLastSource)) {
                BT_IF.getInstance().music_play();
            } else {
                FileNode fileNode = getFileNode(context);
                if (fileNode != null) {
                    Media_IF.getInstance().setAudioDevice(fileNode.getDeviceType());
                    if (!Media_IF.getInstance().playPre()) {
                        Media_IF.getInstance().changePlayState();
                    }
                } else {
                    showToastNoMedia(context);
                }
            }
        }
    }
    
    public static void onClickMusicNextButton(Context context) {
        if (Source.isBTMusicSource()) {
            BT_IF.getInstance().music_next();
        } else if (Source.isAudioSource()) {
            FileNode fileNode = getFileNode(context);
            if (fileNode != null) {
                boolean ret = Media_IF.getInstance().playNext();
                if (!ret) {
                    DebugLog.e("Yearlay", "Error AppWidget#playNext ...");
                }
            } else {
                showToastNoMedia(context);
            }
        } else {
            if (Source.isBTMusicSource(Media_IF.sLastSource)) {
                BT_IF.getInstance().music_play();
            } else {
                FileNode fileNode = getFileNode(context);
                if (fileNode != null) {
                    Media_IF.getInstance().setAudioDevice(fileNode.getDeviceType());
                    if (!Media_IF.getInstance().playNext()) {
                        Media_IF.getInstance().changePlayState();
                    }
                } else {
                    showToastNoMedia(context);
                }
            }
        }
    }
    
    public static void onClickRadioPlayButton() {
        boolean radioPlay = Radio_IF.getInstance().isEnable();
        Radio_IF.getInstance().exitRescanAndScan5S(true);
        Radio_IF.getInstance().setEnable(!radioPlay);
    }
    
    private static void updateAppWidgets(Context context, RemoteViews remoteViews) {
        ComponentName componentName = new ComponentName(context, MediaWidgetProvider.class);
        AppWidgetManager.getInstance(context).updateAppWidget(componentName, remoteViews);
    }
    
    private static FileNode getFileNode(Context context) {
        FileNode fileNode = Media_IF.getInstance().getDefaultItem();
        if (fileNode != null && fileNode.getParseId3() == 0) {
            ID3Parse.instance().parseID3(context, fileNode, null);
        }
        if (fileNode != null) {
            DebugLog.e("Yearlay", "AppWidget getDefaultItem : " + fileNode.getFilePath());
        } else {
            DebugLog.e("Yearlay", "AppWidget getDefaultItem : null");
        }
        return fileNode;
    }
}
