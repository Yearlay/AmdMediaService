package com.haoke.ui.music;

import com.haoke.bean.FileNode;
import com.haoke.bean.ID3Parse;
import com.haoke.bean.ImageLoad;
import com.haoke.bean.ID3Parse.ID3ParseListener;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.define.MediaDef.PlayState;
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
    private int mFocusItemNo = -1;
    private int mTyep = 0;//当前模式：0 列表模式，1 编辑模式
    private ListView mListView;
    private String unknown;

    public Music_Adapter_List(Context context) {
        mContext = context;
        mIF = Media_IF.getInstance();
        unknown = mContext.getResources().getString(R.string.media_unknow);
    }
    
    public void setListView(ListView listView) {
        mListView = listView;
    }

    // 重新加载数据
    public void updateList() {
        mFocusItemNo = -1;
        mTotal = mIF.getListTotal();
        Log.v(TAG, "HMI------------updateList mTotal= " + mTotal);
        refreshData();
    }

    public void releaseFocus() {
        if (mFocusItemNo != -1) {
            mFocusItemNo = -1;
            refreshData();
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
            	
            	holder.mImageIcon.setImageResource(R.drawable.media_list_item_music);
                if (fileNode.getParseId3() == 1) {
                    ImageLoad.instance(mContext).loadBitmap(holder.mImageIcon,
                            R.drawable.media_list_item_music, fileNode);
                } else {
                    ID3Parse.instance().parseID3(position, fileNode, this);
                }
                // 控制焦点项显示
                if (isPlayItem(position)) {
                    if (isPlaying(position)) {
                        holder.mPlayStateImage.setImageResource(R.drawable.music_play_anim);
                    } else {
                        holder.mPlayStateImage.setImageResource(R.drawable.music_play_anim_1);
                    }
                    holder.mPlayStateImage.setVisibility(View.VISIBLE);
                } else {
                    holder.mPlayStateImage.setVisibility(View.GONE);
                }
                
                holder.mTitleText.setText(fileNode.getTitleEx());
                holder.mDescripeText.setText(getDescriptionText(fileNode));
                
                if (mTyep == 0) {
                    holder.mSelectBtn.setVisibility(View.GONE);
                } else if (mTyep == 1) {
                    holder.mSelectBtn.setVisibility(View.VISIBLE);
                } 
                
                //歌曲选中图标状态
                if (mIF.isCurItemSelected(position)) {
                    holder.mSelectBtn.setImageResource(R.drawable.music_selected_icon);
                } else {
                    holder.mSelectBtn.setImageResource(R.drawable.music_selected_nomal);
                }
                
                //选中歌曲名状态
                if (isPlayItem(position) || mIF.isCurItemSelected(position)) {
                    holder.mTitleText.setTextColor(mContext.getResources().getColor(R.color.hk_custom_text_p));
                } else {
                    holder.mTitleText.setTextColor(mContext.getResources().getColor(R.color.hk_custom_text));
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
    
    public void setPlayingAnim(boolean play) {
        if (play) {
            
        } else {
            
        }
    }
    
    public void setListType(int type) {
        mTyep = type;
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
        private ImageView mSelectBtn;
        private ImageView mImageIcon;//the icon
        private TextView mTitleText;//the nummber
        private TextView mDescripeText;//the name 
        private ImageView mPlayStateImage;
    }

    // 重新加载数据
    private void refreshData() {
        notifyDataSetChanged();
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
                            R.drawable.media_list_item_music, fileNode);
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
                        mIF.getCurSource() == com.haoke.define.ModeDef.AUDIO) {
                    val = true;
                }
            } else {
                Log.e(TAG, "isPlayItem why fileNode is null ?");
            }
        }
        return val;
    }
    
    private boolean isPlaying(int position) {
        if (mIF.getPlayState() == PlayState.PLAY) {
            return true;
        }
        return false;
    }
}
