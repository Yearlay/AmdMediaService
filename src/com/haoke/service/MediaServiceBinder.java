package com.haoke.service;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.amd.aidl.IAmdMediaCallBack;
import com.amd.aidl.IAmdMediaService;
import com.amd.bt.BT_IF;
import com.amd.radio.Radio_IF;
import com.haoke.bean.FileNode;
import com.haoke.bean.ID3Parse;
import com.haoke.bean.ID3Parse.ID3ParseListener;
import com.haoke.define.ModeDef;
import com.haoke.mediaservice.R;
import com.haoke.ui.widget.MediaWidgetProvider;
import com.haoke.util.DebugLog;
import com.haoke.util.Media_IF;

import static com.amd.aidl.MediaId.*;


public class MediaServiceBinder extends IAmdMediaService.Stub {
	private static final String TAG = "MediaServiceBinder";
	private ArrayList<MediaClientEx> mClientList = new ArrayList<MediaClientEx>();
	private Context mContext;
	private String mOldId3Info = null;

	public static class MediaClientEx {
		public int mMode = 0;
		public IAmdMediaCallBack mCallBack = null;
		
		public MediaClientEx(int mode, IAmdMediaCallBack callBack) {
			mMode = mode;
			mCallBack = callBack;
		}
	}
	
	public MediaServiceBinder(Context context) {
		mContext = context;
	}
	
	@Override
	public boolean registerCallBack(int mode, IAmdMediaCallBack callBack)
			throws RemoteException {
		Log.d(TAG, "registerCallBack mode = " + mode);
		synchronized (mClientList) {
			int size = mClientList.size();
			for (int i = size - 1; i >= 0; i--) {
				if (mode == mClientList.get(i).mMode) {
					mClientList.remove(i);
				}
			}
			mClientList.add(new MediaClientEx(mode, callBack));
		}
		return true;
	}

	@Override
	public boolean unregisterCallBack(int mode) throws RemoteException {
		Log.d(TAG, "unregisterCallBack mode = " + mode);
		synchronized (mClientList) {
			int size = mClientList.size();
			for (int i = size - 1; i >= 0; i--) {
				if (mode == mClientList.get(i).mMode) {
					mClientList.remove(i);
				}
			}
		}
		return true;
	}

	@Override
	public boolean clickMusicPlay() throws RemoteException {
		Log.d(TAG, "clickMusicPlay");
		MediaWidgetProvider.onClickMusicPlayButton(mContext);
		return true;
	}

	@Override
	public boolean clickMusicPre() throws RemoteException {
		Log.d(TAG, "clickMusicPre");
		MediaWidgetProvider.onClickMusicPreButton(mContext);
		return true;
	}

	@Override
	public boolean clickMusicNext() throws RemoteException {
		Log.d(TAG, "clickMusicNext");
		MediaWidgetProvider.onClickMusicNextButton(mContext);
		return true;
	}

	@Override
	public boolean clickRadioEnable() throws RemoteException {
		Log.d(TAG, "clickRadioEnable");
		MediaWidgetProvider.onClickRadioPlayButton();
		return true;
	}

	@Override
	public boolean clickOther(String tag) throws RemoteException {
		Log.d(TAG, "clickOther tag="+tag);
		if ("music".equals(tag)) {
			Intent musicIntent = new Intent();
            musicIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            musicIntent.setClassName("com.haoke.mediaservice", "com.haoke.ui.media.Media_Activity_Main");
            musicIntent.putExtra("Mode_To_Music", "music_play_intent");
            mContext.startActivity(musicIntent);
			return true;
		} else if ("radio".equals(tag)) {
			Intent radio_intent = new Intent();
            radio_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            radio_intent.setClassName("com.haoke.mediaservice", "com.haoke.ui.media.Media_Activity_Main");
            radio_intent.putExtra("Mode_To_Music", "radio_intent");
            mContext.startActivity(radio_intent);
			return true;
		}
		return false;
	}
	
	@Override
	public String getMediaLabel() throws RemoteException {
        int strId = R.string.launcher_card_media;
        int source = Media_IF.getCurSource();
        if (source == ModeDef.RADIO) {
            strId = R.string.pub_radio;
        } else if (source == ModeDef.BT) {
            strId = R.string.pub_bt;
        } else if (source == ModeDef.AUDIO) {
            strId = R.string.launcher_card_media;
        }
        String title = mContext.getResources().getString(strId);
		Log.d(TAG, "getMediaLabel title="+title);
        return title;
	}

	@Override
	public boolean isPlayingMusic() throws RemoteException {
		int source = Media_IF.getCurSource();
		boolean isPlaying = false;
		if (source == ModeDef.BT) {
			isPlaying = BT_IF.getInstance().music_isPlaying();
		} else if (source == ModeDef.AUDIO) {
			isPlaying = Media_IF.getInstance().isPlayState();
		} else {
			isPlaying = false;
		}
		Log.d(TAG, "isPlayingMusic isPlaying="+isPlaying);
		return isPlaying;
	}

	@Override
	public Bitmap getMusicId3AlbumBmp() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMusicId3Info() throws RemoteException {
		String unkownStr = mContext.getResources().getString(R.string.media_unknow);
		String musicTitle  = null;
        String artist  = null;
        int source = Media_IF.getCurSource();
        if (source == ModeDef.BT) {
            musicTitle = BT_IF.getInstance().music_getTitle();
            artist = BT_IF.getInstance().music_getArtist();
            musicTitle = TextUtils.isEmpty(musicTitle) ? unkownStr : musicTitle;
            artist = TextUtils.isEmpty(artist) ? unkownStr : artist;
            mOldId3Info = musicTitle + " - " + artist;
        } else if (source == ModeDef.AUDIO || source == ModeDef.NULL) {
            FileNode fileNode = getFileNode(mContext);
            if (fileNode != null) {
                musicTitle = fileNode.getTitleEx();
                artist = fileNode.getArtist();
            }
            musicTitle = TextUtils.isEmpty(musicTitle) ? unkownStr : musicTitle;
            artist = TextUtils.isEmpty(artist) ? unkownStr : artist;
            mOldId3Info = musicTitle + " - " + artist;
        }
		Log.d(TAG, "getMusicId3Info mOldId3Info="+mOldId3Info);
		return mOldId3Info;
	}

	@Override
	public boolean isEnableRadio() throws RemoteException {
		boolean enable = Radio_IF.getInstance().isEnable();
		return enable;
	}

	@Override
	public String getRadioBand() throws RemoteException {
		//Radio_IF.getInstance().getCurBand();
		return "FM";
	}
	
	@Override
	public String getRadioFreq() throws RemoteException {
		int freq = Radio_IF.getInstance().getCurFreq();
		String sfreq = Radio_IF.freqToString(freq);
		Log.d(TAG, "getRadioFreq return " + sfreq);
		return sfreq;
	}

	@Override
	public String getRadioFreqUnit() throws RemoteException {
		return "MHZ";
	}
	
	@Override
	public boolean isRadioST() throws RemoteException {
		return Radio_IF.getInstance().getST();
	}
	
	@Override
	public String getOtherInfo(String tag) throws RemoteException {
		Log.d(TAG, "getOtherInfo tag=" + tag);
		return null;
	}
	
	private FileNode getFileNode(Context context) {
        FileNode fileNode = Media_IF.getInstance().getDefaultItem();
        if (fileNode != null && fileNode.getParseId3() == 0) {
            ID3Parse.instance().parseID3(context, fileNode, mID3ParseListener);
        }
        if (fileNode != null) {
            DebugLog.d(TAG, "getFileNode fileNode=" + fileNode);
        } else {
            DebugLog.e(TAG, "getFileNode fileNode is null");
        }
        return fileNode;
    }
    
    private ID3ParseListener mID3ParseListener = new ID3ParseListener() {
        @Override
        public void onID3ParseComplete(Object object, FileNode fileNode) {
            if (fileNode == null || fileNode.getParseId3() == 0) {
                return;
            }
            mHandler.obtainMessage(ID_MUSIC_ID3, 0, 0, null).sendToTarget();
        }
    };
    
    private Handler mHandler = new Handler() {
    	public void handleMessage(android.os.Message msg) {
    		removeMessages(msg.what);
    		dispatchDataToClients(msg.what, msg.arg1, (String)msg.obj);
    	};
    };
    
    private void dispatchDataToClients(int id, int data0, String data1) {
		synchronized (mClientList) {
			for (int i = 0; i < mClientList.size(); i++) {
				MediaClientEx client = mClientList.get(i);
				IAmdMediaCallBack callBack = client.mCallBack;
				Log.d(TAG, "dispatchDataToClients mode="+client.mMode+"; ");
				try {
					callBack.onDataChange(id, data0, data1);
				} catch (RemoteException e) {
					Log.e(TAG, "dispatchDataToClients e=" + e.getMessage());
					Log.e(TAG, "dispatchDataToClients clientList.remove mode="
							+ mClientList.get(i).mMode);
					mClientList.remove(i);
				}
			}
		}
	}
    
    public void refreshWidget(int refreshMode) {
    	int id = ID_NULL;
    	int data0 = 0;
    	String data1 = null;
    	if (refreshMode == ModeDef.NULL) {
    		id = ID_ALL;
    	} else if (refreshMode == ModeDef.AUDIO || refreshMode == ModeDef.BT) {
    		id = ID_MUSIC_ALL;
    	} else if (refreshMode == ModeDef.RADIO) {
    		id = ID_RADIO_ALL;
    	}
    	if (id != ID_NULL) {
        	mHandler.obtainMessage(id, data0, 0, data1).sendToTarget();
    	}
    }
}