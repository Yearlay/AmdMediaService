package com.haoke.ui.music;

import com.amd.bt.BT_IF;
import com.amd.util.Source;
import com.amd.util.SkinManager;
import com.haoke.bean.FileNode;
import com.haoke.bean.ID3Parse;
import com.haoke.bean.ID3Parse.ID3ParseListener;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.mediaservice.R;
import com.haoke.ui.widget.RoundImageViewByXfermode;
import com.haoke.util.Media_IF;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Music_Play_Id3 extends LinearLayout implements OnClickListener, ID3ParseListener {
    private static final String TAG = "Music_Play_Id3";
    
    private static final String VERSION_INFO = "当前媒体apk版本为 20171130 19:20";
    
    private TextView mTrack;
    private TextView mAlbum;
    
    private RoundImageViewByXfermode mAlbumView = null;
    private TextView mDeviceView;
    private int showVersion = 0;
    
    private static Bitmap mDefaultBitmap;
    private static Bitmap mDefaultBTBitmap;
    private static Bitmap mBitmap;
    private String unknown;
    
    private boolean isBTPlay = false;
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d(TAG, "onDetachedFromWindow");
        /*mAlbumView.setImageBitmap(null);
        if (mDefaultBitmap != null && !mDefaultBitmap.isRecycled()) {
            mDefaultBitmap.recycle();
            mDefaultBitmap = null;
        }
        if (mDefaultBTBitmap != null && !mDefaultBTBitmap.isRecycled()) {
        	mDefaultBTBitmap.recycle();
        	mDefaultBTBitmap = null;
        }
        if (mBitmap != null && !mBitmap.isRecycled()) {
        	mBitmap.recycle();
        	mBitmap = null;
        }*/
    }
    
    public void setBTPlayMode(boolean btModeFlag) {
    	isBTPlay = btModeFlag;
    }
    
    public void updateId3Info() {
        if (isBTPlay) {
            updateBTInfo();
        } else {
            updateAudioInfo();
        }
    }
    
    public void quickRefreshId3Info() {
        if (isBTPlay) {
        } else {
            Media_IF mIF = Media_IF.getInstance();
            String artist = mIF.getPlayId3Artist();
            String album = mIF.getPlayId3Album();
            if (TextUtils.isEmpty(album)) {
                album = unknown;
            }
            if (TextUtils.isEmpty(artist)) {
                artist = unknown;
            }
            mAlbum.setText(artist + " - " + album);
            mTrack.setText(mIF.getPlayId3Title());
        }
    }
    
    private Bitmap getDefBitmap() {
        if (mDefaultBitmap == null) {
            Bitmap defBitmap = SkinManager.instance(getContext()).decodeResource(R.drawable.music_play_icon_def);
            if (defBitmap != null) {
                mDefaultBitmap = scaleBitmap(defBitmap);
            }
        }
        return mDefaultBitmap;
    }
    
    private Bitmap getDefBTBitmap() {
        if (mDefaultBTBitmap == null) {
            Bitmap defBitmap = SkinManager.instance(getContext()).decodeResource(R.drawable.bt_play_icon_def);
            if (defBitmap != null) {
            	mDefaultBTBitmap = scaleBitmap(defBitmap);
            }
        }
        return mDefaultBTBitmap;
    }
    
    private void updateBTInfo() {
        BT_IF mBTIF = BT_IF.getInstance();
        String track = null;
        track = mBTIF.music_getTitle();
        String artist = mBTIF.music_getArtist();
        String album = mBTIF.music_getAlbum();
        Log.v(TAG, "updateId3Info() track=" + track 
                + ", artist=" + artist + ", track=" + track);
        if (album == null || album == "") {
            album = unknown;
        }
        if (artist == null || artist == "") {
            artist = unknown;
        }
        mAlbum.setText(artist + " - " + album);
        if (track != null && track != "") {
            mTrack.setText(track);
        }
        mAlbumView.setImageBitmap(getDefBTBitmap());
        mDeviceView.setText(R.string.pub_bt);
    }
    
    private void updateAudioInfo() {
        FileNode fileNode = Media_IF.getInstance().getPlayItem();
        if (fileNode == null) {
            return;
        }
        String artist = fileNode.getArtist();
        String album = fileNode.getAlbum();
        if (TextUtils.isEmpty(album)) {
            album = unknown;
        }
        if (TextUtils.isEmpty(artist)) {
            artist = unknown;
        }
        mAlbum.setText(artist + " - " + album);
        mTrack.setText(fileNode.getTitleEx());
        
        if (fileNode.getParseId3() == 1 && fileNode.getThumbnailPath() != null) {
            if (mBitmap != null && !mBitmap.isRecycled()) {
                mBitmap.recycle();
                mBitmap = null;
            }
            Bitmap bitmap = BitmapFactory.decodeFile(fileNode.getThumbnailPath());
            if (bitmap != null) {
                mBitmap = scaleBitmap(bitmap);
                mAlbumView.setImageBitmap(mBitmap);
            } else {
                mAlbumView.setImageBitmap(getDefBitmap());
            }
        } else {
            mAlbumView.setImageBitmap(getDefBitmap());
            ID3Parse.instance().parseID3(mAlbumView, fileNode, this);
        }
        
        if (fileNode.getDeviceType() == DeviceType.USB1) {
            mDeviceView.setText(R.string.media_usb1_tab);
        } else if (fileNode.getDeviceType() == DeviceType.USB2) {
            mDeviceView.setText(R.string.media_usb2_tab);
        } else if (fileNode.getDeviceType() == DeviceType.FLASH) {
            mDeviceView.setText(R.string.media_flash_tab);
        } else if (Source.isBTMusicSource()) {
            mDeviceView.setText(R.string.pub_bt);
        } else {
            mDeviceView.setText(R.string.media_collect_tab);
        }
    }
    
    private Bitmap scaleBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width >= 500 && height >= 500) {    
            return bitmap;
        }
        Matrix matrix = new Matrix();
        float scaleWidth = 500/(float)width;
        float scaleHeight = 500/(float)height;
        float scale = scaleWidth > scaleHeight ? scaleWidth : scaleHeight;
        matrix.postScale(scale, scale);
        Bitmap newBM = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false);
        if (!bitmap.isRecycled()) {
            bitmap.recycle();
        }
        return newBM;
    }

    public Music_Play_Id3(Context context, AttributeSet attrs) {
        super(context, attrs);
        unknown = getResources().getString(R.string.media_unknow);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        
        mTrack = (TextView) this.findViewById(R.id.media_id3_track);
        mAlbum = (TextView) this.findViewById(R.id.media_id3_album);
        
        mAlbumView = (RoundImageViewByXfermode) findViewById(R.id.music_play_img_cycle);
        mDeviceView = (TextView) findViewById(R.id.music_device_type);
        
        if (Media_IF.getInstance().getPlayingDevice() == DeviceType.USB1) {
            mDeviceView.setText(R.string.media_usb1_tab);
        } else if (Media_IF.getInstance().getPlayingDevice() == DeviceType.USB2) {
            mDeviceView.setText(R.string.media_usb2_tab);
        } else if (Media_IF.getInstance().getPlayingDevice() == DeviceType.FLASH) {
            mDeviceView.setText(R.string.media_flash_tab);
        } else if (Source.isBTMusicSource()) {
            mDeviceView.setText(R.string.pub_bt);
        } else {
            mDeviceView.setText(R.string.media_collect_tab);
        }
        
        mAlbumView.setOnClickListener(this);
    }
    
    public void refreshSkin(SkinManager skinManager) {
        refreshBitmap();
        updateId3Info();
        mAlbumView.refreshSkin();
    }
    
    private void refreshBitmap() {
        if (mDefaultBitmap != null && !mDefaultBitmap.isRecycled()) {
            mDefaultBitmap.recycle();
            mDefaultBitmap = null;
        }
        if (mDefaultBTBitmap != null && !mDefaultBTBitmap.isRecycled()) {
            mDefaultBTBitmap.recycle();
            mDefaultBTBitmap = null;
        }
    }
    
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.music_play_img_cycle:
            showVersion ++ ;
            if (showVersion % 5 == 0) {
                Toast.makeText(getContext(), VERSION_INFO, Toast.LENGTH_LONG).show();
            }
            break;
        default:
            break;
        }
    }

    @Override
    public void onID3ParseComplete(Object object, FileNode fileNode) {
        try {
            if (fileNode.isSame(Media_IF.getInstance().getPlayItem())) {
                if (mBitmap != null && !mBitmap.isRecycled()) {
                    mBitmap.recycle();
                    mBitmap = null;
                }
                Bitmap bitmap = BitmapFactory.decodeFile(fileNode.getThumbnailPath());
                if (bitmap != null) {
                    mBitmap = scaleBitmap(bitmap);
                    mAlbumView.setImageBitmap(mBitmap);
                } else {
                    mAlbumView.setImageBitmap(getDefBitmap());
                }
                String track = fileNode.getTitleEx();
                String artist = fileNode.getArtist();
                String album = fileNode.getAlbum();
                if (TextUtils.isEmpty(album)) {
                    album = unknown;
                }
                if (TextUtils.isEmpty(artist)) {
                    artist = unknown;
                }
                mAlbum.setText(artist + " - " + album);
                mTrack.setText(track);
            }
        } catch (Exception e) {
            Log.e(TAG, "onLoadBitmapCompleted fileNode="+fileNode);
        }
    }
}
