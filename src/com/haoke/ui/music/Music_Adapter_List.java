package com.haoke.ui.music;

import com.amd.util.Source;
import com.amd.util.SkinManager;
import com.haoke.bean.FileNode;
import com.haoke.bean.ID3Parse;
import com.haoke.bean.ImageLoad;
import com.haoke.bean.ID3Parse.ID3ParseListener;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.constant.MediaUtil.PlayState;
import com.haoke.mediaservice.R;
import com.haoke.util.Media_IF;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class Music_Adapter_List extends BaseAdapter implements ID3ParseListener {

    private final String TAG = this.getClass().getSimpleName();
    private Context mContext;
    private Media_IF mIF;
    private int mTotal = 0;
    private boolean mEditMode = false;//当前模式：false 列表模式，true 编辑模式
    private ListView mListView;
    private String unknown;
    private SkinManager skinManager;
    private int mDeviceType = DeviceType.NULL;
    private int mLastPlayItem = -2;

    public Music_Adapter_List(Context context) {
        mContext = context;
        mIF = Media_IF.getInstance();
        unknown = mContext.getResources().getString(R.string.media_unknow);
        skinManager = SkinManager.instance(context);
    }
    
    public void setListView(ListView listView) {
        mListView = listView;
    }

    // 设备有改变，重新加载数据
    public boolean updateDeviceType(int deviceType, boolean force) {
        if (mDeviceType != deviceType || force) {
            mTotal = mIF.getMediaListSize(deviceType, FileType.AUDIO);
            mDeviceType = deviceType;
            resetLastPlayItem();
            return true;
        } else if (mDeviceType == deviceType) {
            int size = mIF.getMediaListSize(deviceType, FileType.AUDIO);
            if (size != mTotal) {
                mTotal = size;
                resetLastPlayItem();
                return true;
            }
        }
        return false;
    }
    
    public void resetLastPlayItem() {
        mLastPlayItem = -2;
    }

    public void setListType(boolean editMode, boolean notifyDataChange) {
        if (mEditMode != editMode) {
            mEditMode = editMode;
            if (notifyDataChange) {
                notifyDataSetChanged();
            }
        }
    }
    
    @Override
    public View getView(final int position, View convertView, ViewGroup arg2) {
        final ViewHolder holder;
        long start = System.currentTimeMillis();
        if (convertView != null) {
            holder = (ViewHolder) convertView.getTag();
        } else {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.music_list_item, null);
            holder.mItemBg = convertView.findViewById(R.id.music_list_item_bg);
            holder.mSelectBtn = (ImageView) convertView.findViewById(R.id.music_list_item_select);
            holder.mImageIcon = (ImageView) convertView.findViewById(R.id.music_item_icon);
            
            holder.mTitleText = (TextView) convertView.findViewById(R.id.music_listitem_title);
            holder.mDescripeText = (TextView) convertView.findViewById(R.id.music_item_text);
            
            holder.mPlayStateImage = (ImageView) convertView.findViewById(R.id.music_item_is_play);
            Log.v(TAG, "getView() setTag-position=" + position);
            convertView.setTag(holder);
        }
        
        setDate(holder, position);
        
        long end = System.currentTimeMillis();
        Log.d(TAG, "getView consume time="+(end-start)+"ms");
        
        return convertView;
    }
    
    private void setDate(ViewHolder holder, int position) {
        Log.v(TAG, "setDate() mTotal=" +mTotal);
        int itemNo = position;
        // 控制内容显示
        Bitmap thumb = null;
        if (itemNo < mTotal) {
            Log.v(TAG, "setDate() itemNo=" +itemNo +", mTotal=" +mTotal);
            FileNode fileNode = mIF.getItem(itemNo);
            if (fileNode != null) {
                holder.mItemBg.setBackgroundDrawable(skinManager.getDrawable(R.drawable.music_list_item_selector));
                holder.mImageIcon.setImageDrawable(skinManager.getDrawable(R.drawable.media_list_item_music));
                if (fileNode.getParseId3() == 1) {
                    ImageLoad.instance(mContext).loadBitmap(holder.mImageIcon,
                            skinManager.getDrawable(R.drawable.media_list_item_music), fileNode);
                } else {
                    ID3Parse.instance().parseID3(position, fileNode, this);
                }
                boolean isPlayingItem = isPlayItem(position);
                boolean isSelectedItem = mIF.isCurItemSelected(position);
                boolean isLastPlayItem = isLastPlayItem(position);
                // 控制焦点项显示
                if (isPlayingItem || isLastPlayItem) {
                    if (isPlaying(fileNode)) {
                        holder.mPlayStateImage.setImageDrawable(skinManager.getDrawable(R.drawable.music_play_anim));
                    } else {
                        holder.mPlayStateImage.setImageDrawable(skinManager.getDrawable(R.drawable.music_play_anim_1));
                    }
                    holder.mPlayStateImage.setVisibility(View.VISIBLE);
                } else {
                    holder.mPlayStateImage.setVisibility(View.GONE);
                }
                
                holder.mTitleText.setText(fileNode.getTitleEx());
                holder.mDescripeText.setText(getDescriptionText(fileNode));
                
                if (mEditMode) {
                    holder.mSelectBtn.setVisibility(View.VISIBLE);
                } else {
                    holder.mSelectBtn.setVisibility(View.GONE);
                } 
                
                //歌曲选中图标状态
                if (isSelectedItem) {
                    holder.mSelectBtn.setImageDrawable(skinManager.getDrawable(R.drawable.music_selected_icon));
                } else {
                    holder.mSelectBtn.setImageDrawable(skinManager.getDrawable(R.drawable.music_selected_nomal));
                }
                
                //选中歌曲名状态
                if (isPlayingItem || isSelectedItem || isLastPlayItem) {
                    holder.mTitleText.setTextColor(skinManager.getColor(R.color.hk_custom_text_p));
                } else {
                    holder.mTitleText.setTextColor(skinManager.getColor(R.color.hk_custom_text));
                }
            } else {
                Log.e(TAG, "setDate why fileNode is null ?");
            }
        }
        
    }
    
    private String getDescriptionText(FileNode fileNode) {
        StringBuffer buffer = new StringBuffer();
        buffer.append((fileNode.getArtist() == null) ? unknown : fileNode.getArtist());
        buffer.append(" - ");
        buffer.append((fileNode.getAlbum() == null) ? unknown : fileNode.getAlbum());
        buffer.append("   ");
        buffer.append(fileNode.getLastDate());
        buffer.append("   ");
        if (fileNode.isFromCollectTable()) {
            int fromDeviceType = fileNode.getFromDeviceType();
            if (fromDeviceType != DeviceType.NULL) {
                if (fromDeviceType == DeviceType.FLASH) {
                    buffer.append(mContext.getResources().getString(R.string.collect_from_local));
                } else if (fromDeviceType == DeviceType.USB1) {
                    buffer.append(mContext.getResources().getString(R.string.collect_from_usb1));
                } else if (fromDeviceType == DeviceType.USB2) {
                    buffer.append(mContext.getResources().getString(R.string.collect_from_usb2));
                }
            }
        }
        return buffer.toString();
    }
    
    @Override
    public int getCount() {
        return mTotal;
    }

    @Override
    public Object getItem(int arg0) {
        String name = "";
        return name;
    }

    @Override
    public long getItemId(int arg0) {
        return mTotal;
    }

    public static class ViewHolder {
    	private View mItemBg;
        private ImageView mSelectBtn;
        private ImageView mImageIcon;//the icon
        private TextView mTitleText;//the nummber
        private TextView mDescripeText;//the name 
        private ImageView mPlayStateImage;
    }

    @Override
    public void onID3ParseComplete(Object object, FileNode fileNode) {
        if (mListView == null) {
            return;
        }
        int position = (Integer) object;
        int firstChildPosition = mListView.getFirstVisiblePosition();
        try {
            if (position - firstChildPosition >= 0) {
                View childView = mListView.getChildAt(position - firstChildPosition);
                ViewHolder holder = (ViewHolder) childView.getTag();
                if (holder != null && fileNode.getParseId3() == 1) {
                    ImageLoad.instance(mContext).loadBitmap(holder.mImageIcon,
                            skinManager.getDrawable(R.drawable.media_list_item_music), fileNode);
                    String artist = fileNode.getArtist();
                    String album = fileNode.getAlbum();
                    if (artist == null) {
                        artist = unknown;
                    }
                    if (album == null) {
                        album = unknown;
                    }
                    holder.mTitleText.setText(fileNode.getTitleEx());
                    holder.mDescripeText.setText(getDescriptionText(fileNode));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "onID3ParseComplete position="+position+"; firstChildPosition="+firstChildPosition, e);
        }
    }
    
    private boolean isPlayItem(int position) {
        boolean val = false;
        if (position == mIF.getPlayPos()) {
            FileNode fileNode = mIF.getItem(position);
            if (fileNode != null) {
                if (fileNode.getDeviceType() == mIF.getPlayingDevice() &&
                        Source.isAudioSource()) {
                    val = true;
                }
            } else {
                Log.e(TAG, "isPlayItem why fileNode is null ?");
            }
        }
        return val;
    }
    
    private boolean isLastPlayItem(int position) {
        if (mLastPlayItem == -2) {
            mLastPlayItem = mIF.getLastPlayItem(mDeviceType, FileType.AUDIO);
        }
        return mLastPlayItem == position;
    }
    
    private boolean isPlaying(FileNode fileNode) {
        if (mIF.getPlayState() == PlayState.PLAY 
                && fileNode.getDeviceType() == mIF.getPlayingDevice()) {
            return true;
        }
        return false;
    }
}
