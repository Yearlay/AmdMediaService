package com.haoke.ui.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.amd.bt.BT_IF;
import com.haoke.bean.FileNode;
import com.haoke.bean.ID3Parse;
import com.haoke.bean.ID3Parse.ID3ParseListener;
import com.haoke.btjar.main.BTDef.BTConnState;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.define.MediaDef.PlayState;
import com.haoke.define.ModeDef;
import com.haoke.mediaservice.R;
import com.haoke.util.DebugLog;
import com.haoke.util.Media_IF;
import com.amd.radio.Radio_IF;

public class MediaWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "MediaWidgetProvider";
    
    private PendingIntent getPendingIntent(Context context, int buttonId) {
        Intent intent = new Intent();
        intent.setClass(context, MediaWidgetProvider.class);
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        intent.setData(Uri.parse("harvic:" + buttonId));
        PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pIntent;
    }
    
    private static void setLabelInfo(Context context, RemoteViews remoteViews, int source) {
        int strId = R.string.launcher_card_media;
        if (source == ModeDef.RADIO) {
            strId = R.string.pub_radio;
        } else if (source == ModeDef.BT) {
            strId = R.string.pub_bt;
        } else if (source == ModeDef.AUDIO) {
            strId = R.string.launcher_card_media;
        }
        String title = context.getResources().getString(strId);
        remoteViews.setTextViewText(R.id.widget_media_style, title);
    }
    
    private static void setMusicInfo(Context context, RemoteViews remoteViews, int source) {
        String unkownStr = context.getResources().getString(R.string.media_unknow);
        String musicTitle  = null;
        String artist  = null;
        if (source == ModeDef.BT) {
            musicTitle = BT_IF.getInstance().music_getTitle();
            artist = BT_IF.getInstance().music_getArtist();
        } else if (source == ModeDef.AUDIO || source == ModeDef.NULL) {
            FileNode fileNode = getFileNode(context);
            if (fileNode != null) {
                musicTitle = fileNode.getTitleEx();
                artist = fileNode.getArtist();
            }
        } else {
            return;
        }
        musicTitle = TextUtils.isEmpty(musicTitle) ? unkownStr : musicTitle;
        artist = TextUtils.isEmpty(artist) ? unkownStr : artist;
        remoteViews.setTextViewText(R.id.widget_music_name, musicTitle + " - " + artist);
    }
    
    private static Bitmap mBitmap;
    
    private static void setShowImage(Context context, RemoteViews remoteViews) {
        if (Media_IF.getCurSource() == ModeDef.AUDIO) {
            FileNode fileNode = getFileNode(context);
            if (fileNode != null) {
                if (fileNode.getParseId3() == 1 && fileNode.getThumbnailPath() != null) {
                    if (mBitmap != null) {
                        mBitmap.recycle();
                    }
                    mBitmap = BitmapFactory.decodeFile(fileNode.getThumbnailPath());
                    remoteViews.setImageViewBitmap(R.id.widget_media_icon, mBitmap);
                } else {
                    remoteViews.setImageViewResource(R.id.widget_media_icon,
                            R.drawable.home1_card_media_default);
                }
            } else {
                remoteViews.setImageViewResource(R.id.widget_media_icon, R.drawable.home1_card_media_default);
            }
        } else if (Media_IF.getCurSource() == ModeDef.BT) {
            remoteViews.setImageViewResource(R.id.widget_media_icon, R.drawable.home1_card_media_default);
        }
    }
    
    private static void setMusicPlayButton(Context context, RemoteViews remoteViews, int source) {
        int resPlayId = R.drawable.main_home1_card_music_play_selector;
        int resPauseId = R.drawable.main_home1_card_music_pause_selector;
        if (source == ModeDef.BT) {
            remoteViews.setImageViewResource(R.id.widget_music_btn_play,
                    BT_IF.getInstance().music_isPlaying() ? resPauseId : resPlayId);
        } else if (source == ModeDef.AUDIO) {
            remoteViews.setImageViewResource(R.id.widget_music_btn_play,
                    Media_IF.getInstance().getPlayState() == PlayState.PLAY ? resPauseId : resPlayId);
        } else {
            remoteViews.setImageViewResource(R.id.widget_music_btn_play, resPlayId);
        }
    }
    
    private static void setRadioPlayButton(Context context, RemoteViews remoteViews, int source) {
        if (source == ModeDef.RADIO && Radio_IF.getInstance().isEnable()) {
            remoteViews.setImageViewResource(R.id.widget_radio_btn_play,
                    R.drawable.main_home1_card_radio_pause_selector);
        } else {
            remoteViews.setImageViewResource(R.id.widget_radio_btn_play,
                    R.drawable.main_home1_card_radio_play_selector);
        }
    }
    
    private static void setRadioInfo(Context context, RemoteViews remoteViews) {
        int band = Radio_IF.getInstance().getCurBand();//AM
        int curFreq = Radio_IF.getInstance().getCurFreq();//1055.9
        boolean ST = Radio_IF.getInstance().getST();
        String freq = String.valueOf(curFreq);
        if(curFreq >= 8750){ // FM
            freq = freq.substring(0, freq.length()-2) + "." + freq.substring(freq.length()-2);
        }
        remoteViews.setTextViewText(R.id.widget_radio_am, (band >= 1 && band <= 3) ? "FM" : "AM");
        remoteViews.setTextViewText(R.id.widget_radio__mhz, (band == 4 || band == 5) ? "KHZ" : "MHZ");
        remoteViews.setTextViewText(R.id.widget_radio_num, freq);
        remoteViews.setViewVisibility(R.id.widget_radio__st, ST ? View.VISIBLE : View.INVISIBLE);
    }
    
    @Override
    public void onEnabled(Context context) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.media_widget_provider);
        setonClickPendding(context, remoteViews);
        updateAppWidgets(context, remoteViews);
        super.onEnabled(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.media_widget_provider);
        setonClickPendding(context, remoteViews);
        setAllInfo(context, remoteViews, ModeDef.NULL);
        updateAppWidgets(context, remoteViews);
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
    
    /* widget 接受到广播时触发 */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive intent="+intent);
        if (intent.hasCategory(Intent.CATEGORY_ALTERNATIVE)) {
            int buttonId = Integer.parseInt(intent.getData().getSchemeSpecificPart());
            switch (buttonId) {
                case R.id.home1_rl_left: // 点击主框体，进入音乐界面。
                    Intent musicIntent = new Intent();
                    musicIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    musicIntent.setClassName("com.haoke.mediaservice", "com.haoke.ui.media.Media_Activity_Main");
                    musicIntent.putExtra("Mode_To_Music", "music_play_intent");
                    context.startActivity(musicIntent);
                    break;
                case R.id.widget_music_btn_play:
                    onClickMusicPlayButton(context);
                    break;
                case R.id.widget_music_btn_pre:
                    onClickMusicPreButton(context);
                    break;
                case R.id.widget_music_btn_next:
                    onClickMusicNextButton(context);
                    break;
                case R.id.radio_enter_layout:
                    Intent radio_intent = new Intent();
                    radio_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    radio_intent.setClassName("com.haoke.mediaservice", "com.haoke.ui.media.Media_Activity_Main");
                    radio_intent.putExtra("Mode_To_Music", "radio_intent");
                    context.startActivity(radio_intent);
                    break;
                case R.id.widget_radio_btn_play:
                    onClickRadioPlayButton();
                    break;
            }
        }
        if ("main_activity_update_ui".equals(intent.getAction())) {
            if (intent.getBooleanExtra("bt_disconnect", false)) {
                if (Media_IF.getCurSource() == ModeDef.BT) {
                    Media_IF.setCurSource(ModeDef.NULL);
                }
            }
            if (intent.getBooleanExtra("bt_connected", false)) {
                if (Media_IF.getCurSource() != ModeDef.AUDIO) {
                    // Media_IF.setCurSource(ModeDef.BT);
                }
            }
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.media_widget_provider);
            setAllInfo(context, remoteViews, ModeDef.NULL);
            updateAppWidgets(context, remoteViews);
        }
        super.onReceive(context, intent);
    }
    
    public static void refreshWidget(Context context, int refreshMode) {
        long start = System.currentTimeMillis();
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.media_widget_provider);
        setAllInfo(context, remoteViews, refreshMode);
        updateAppWidgets(context, remoteViews);
        Log.d(TAG, "refreshWidget consume="+(System.currentTimeMillis() - start));
    }
    
    private void setonClickPendding(Context context, RemoteViews remoteViews) {
        remoteViews.setOnClickPendingIntent(R.id.widget_music_btn_play, getPendingIntent(context, R.id.widget_music_btn_play));
        remoteViews.setOnClickPendingIntent(R.id.widget_music_btn_pre, getPendingIntent(context, R.id.widget_music_btn_pre));
        remoteViews.setOnClickPendingIntent(R.id.widget_music_btn_next, getPendingIntent(context, R.id.widget_music_btn_next));
        remoteViews.setOnClickPendingIntent(R.id.home1_rl_left, getPendingIntent(context, R.id.home1_rl_left));
        remoteViews.setOnClickPendingIntent(R.id.radio_enter_layout, getPendingIntent(context, R.id.radio_enter_layout));
        remoteViews.setOnClickPendingIntent(R.id.widget_radio_btn_play, getPendingIntent(context, R.id.widget_radio_btn_play));
    }
    
    private static void setAllInfo(Context context, RemoteViews remoteViews, int refreshMode) {
        int source = Media_IF.getCurSource();
        DebugLog.d("Yearlay", "setAllInfo source: " + source + "; refreshMode="+refreshMode);
        if (refreshMode == ModeDef.BT || refreshMode == ModeDef.RADIO || refreshMode == ModeDef.AUDIO || refreshMode == ModeDef.NULL) {
            setLabelInfo(context, remoteViews, source); // 更新Label信息。
        }
        if (refreshMode == ModeDef.BT || refreshMode == ModeDef.AUDIO || refreshMode == ModeDef.NULL) {
            int sourceEx = source;
            if (source != ModeDef.BT && refreshMode == ModeDef.BT) {
                if (!(BT_IF.getInstance().getConnState() == BTConnState.CONNECTED)) {
                    sourceEx = ModeDef.NULL;
                }
            }
            setMusicInfo(context, remoteViews, sourceEx); // 更新Music（蓝牙音乐或我的音乐）的信息。
            setShowImage(context, remoteViews); // 更新Music（蓝牙显示为默认，我的音乐显示专辑图）显示的图片。
        }
        if (refreshMode == ModeDef.RADIO || refreshMode == ModeDef.NULL) {
            setRadioInfo(context, remoteViews);
        }
        setMusicPlayButton(context, remoteViews, source); // 更新音乐的播放按键。
        setRadioPlayButton(context, remoteViews, source); // 更新收音机的播放按键。
    }
    
    private void onClickMusicPlayButton(Context context) {
        int source = Media_IF.getCurSource();
        if (source == ModeDef.BT) {
            if (BT_IF.getInstance().music_isPlaying()) {
                BT_IF.getInstance().music_pause();
                BT_IF.getInstance().setRecordPlayState(PlayState.PAUSE);
            } else {
                BT_IF.getInstance().music_play();
                BT_IF.getInstance().setRecordPlayState(PlayState.PLAY);
            }
        } else if (source == ModeDef.AUDIO) {
            if (getFileNode(context) != null) {
                Media_IF.getInstance().changePlayState();
            } else {
                Toast.makeText(context, R.string.no_media_can_play, Toast.LENGTH_SHORT).show();
            }
        } else {
            if (Media_IF.getCurSource() == ModeDef.RADIO) {
                Radio_IF.getInstance().setEnable(false);
            }
            if (Media_IF.sLastSource == ModeDef.BT) {
                // Media_IF.setCurSource(ModeDef.BT);
                BT_IF.getInstance().music_play();
            } else {
                // Media_IF.setCurSource(ModeDef.AUDIO);
                if (Media_IF.getInstance().getPlayingFileType() == FileType.AUDIO) {
                    // 只有是音乐的情况下，才会播放。
                    Media_IF.getInstance().setPlayState(PlayState.PLAY);
                } else {
                    // 如果不是音乐，就想办法播放音乐。 
                    FileNode fileNode = getFileNode(context);
                    if (fileNode != null) {
                        Media_IF.getInstance().play(getFileNode(context));
                    } else {
                        Toast.makeText(context, R.string.no_media_can_play, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }
    
    private void onClickMusicPreButton(Context context) {
        if (Media_IF.getCurSource() == ModeDef.BT) {
            BT_IF.getInstance().music_pre();
        } else if (Media_IF.getCurSource() == ModeDef.AUDIO) {
            FileNode fileNode = getFileNode(context);
            if (fileNode != null) {
                boolean ret = Media_IF.getInstance().playPre();
                if (!ret) {
                    DebugLog.e("Yearlay", "Error AppWidget#playPre ...");
                }
            } else {
                Toast.makeText(context, R.string.no_media_can_play, Toast.LENGTH_SHORT).show();
            }
        } else {
            if (Media_IF.getCurSource() == ModeDef.RADIO) {
                Radio_IF.getInstance().setEnable(false);
            }
            if (Media_IF.sLastSource == ModeDef.BT) {
                //Media_IF.setCurSource(ModeDef.BT);
                BT_IF.getInstance().music_play();
            } else {
                //Media_IF.setCurSource(ModeDef.AUDIO);
                FileNode fileNode = getFileNode(context);
                if (fileNode != null) {
                    Media_IF.getInstance().setAudioDevice(fileNode.getDeviceType());
                    if (!Media_IF.getInstance().playPre()) {
                        Media_IF.getInstance().changePlayState();
                    }
                } else {
                    Toast.makeText(context, R.string.no_media_can_play, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    private void onClickMusicNextButton(Context context) {
        if (Media_IF.getCurSource() == ModeDef.BT) {
            BT_IF.getInstance().music_next();
        } else if (Media_IF.getCurSource() == ModeDef.AUDIO) {
            FileNode fileNode = getFileNode(context);
            if (fileNode != null) {
                boolean ret = Media_IF.getInstance().playNext();
                if (!ret) {
                    DebugLog.e("Yearlay", "Error AppWidget#playNext ...");
                }
            } else {
                Toast.makeText(context, R.string.no_media_can_play, Toast.LENGTH_SHORT).show();
            }
        } else {
            if (Media_IF.getCurSource() == ModeDef.RADIO) {
                Radio_IF.getInstance().setEnable(false);
            }
            if (Media_IF.sLastSource == ModeDef.BT) {
                // Media_IF.setCurSource(ModeDef.BT);
                BT_IF.getInstance().music_play();
            } else {
                // Media_IF.setCurSource(ModeDef.AUDIO);
                FileNode fileNode = getFileNode(context);
                if (fileNode != null) {
                    // Media_IF.setCurSource(ModeDef.AUDIO);
                    Media_IF.getInstance().setAudioDevice(fileNode.getDeviceType());
                    if (!Media_IF.getInstance().playNext()) {
                        Media_IF.getInstance().changePlayState();
                    }
                } else {
                    Toast.makeText(context, R.string.no_media_can_play, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    private void onClickRadioPlayButton() {
        if (Media_IF.getCurSource() == ModeDef.BT) {
            BT_IF.getInstance().music_pause();
        } else if (Media_IF.getCurSource() == ModeDef.AUDIO) {
            Media_IF.getInstance().setPlayState(PlayState.PAUSE);
        }
        // Media_IF.setCurSource(ModeDef.RADIO);
        // Radio_IF.getInstance().requestAudioFocus(true);
        boolean radioPlay = Radio_IF.getInstance().isEnable();
        Radio_IF.getInstance().setEnable(!radioPlay);
    }
    
    private static void updateAppWidgets(Context context, RemoteViews remoteViews) {
        ComponentName componentName = new ComponentName(context, MediaWidgetProvider.class);
        AppWidgetManager.getInstance(context).updateAppWidget(componentName, remoteViews);
    }
    
    private static FileNode getFileNode(Context context) {
        FileNode fileNode = Media_IF.getInstance().getDefaultItem();
        if (fileNode != null && fileNode.getParseId3() == 0) {
            ID3Parse.instance().parseID3(context, fileNode, mID3ParseListener);
        }
        if (fileNode != null) {
            DebugLog.e("Yearlay", "AppWidget getDefaultItem : " + fileNode.getFilePath());
        } else {
            DebugLog.e("Yearlay", "AppWidget getDefaultItem : null");
        }
        return fileNode;
    }
    
    private static ID3ParseListener mID3ParseListener = new ID3ParseListener() {
        @Override
        public void onID3ParseComplete(Object object, FileNode fileNode) {
            if (object instanceof Context) {
                Context context = (Context) object;
                RemoteViews remoteView = new RemoteViews(context.getPackageName(), R.layout.media_widget_provider);
                setMusicInfo(context, remoteView, Media_IF.getCurSource());
                setShowImage(context, remoteView);
                updateAppWidgets(context, remoteView);
            }
        }
    };
}
