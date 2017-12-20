package com.haoke.ui.video;

import java.util.ArrayList;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amd.util.SkinManager;
import com.haoke.bean.FileNode;
import com.haoke.bean.ID3Parse;
import com.haoke.bean.ImageLoad;
import com.haoke.bean.StorageBean;
import com.haoke.constant.MediaUtil;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.data.AllMediaList;
import com.haoke.data.OperateListener;
import com.haoke.mediaservice.R;
import com.haoke.ui.widget.CustomDialog;
import com.haoke.ui.widget.CustomDialog.DIALOG_TYPE;
import com.haoke.ui.widget.CustomDialog.OnDialogListener;
import com.haoke.ui.widget.HKTextView;

public class VideoListLayout extends RelativeLayout implements
        OnItemClickListener, OnItemLongClickListener, OperateListener, OnDismissListener{
    private Context mContext;
    private GridView mGridView;
    private TextView mEmptyView;
    private View mLoadingView;
    private ImageView mLoadingImageView;
    private VideoAdapter mVideoAdapter;
    private Handler mActivityHandler;
    private boolean isEditMode;
    private CustomDialog mProgressDialog;
    private CustomDialog mErrorDialog;
    private StorageBean mCurrentStorageBean;
    
    private SkinManager skinManager;
    
    private ArrayList<FileNode> mVideoList = new ArrayList<FileNode>();
    
    public VideoListLayout(Context context) {
        super(context);
    }
    
    public VideoListLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public VideoListLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mContext = getContext();
        mEmptyView = (TextView) findViewById(R.id.video_list_empty);
        mLoadingView = findViewById(R.id.loading_layout);
        mLoadingImageView = (ImageView) mLoadingView.findViewById(R.id.media_loading_imageview);
        mGridView = (GridView) findViewById(R.id.video_grid_list);
        mGridView.setSelector(new ColorDrawable(Color.TRANSPARENT));
        mGridView.setOnItemClickListener(this);
        mGridView.setOnItemLongClickListener(this);
        mVideoAdapter = new VideoAdapter();
        mGridView.setAdapter(mVideoAdapter);
        skinManager = SkinManager.instance(mContext);
    }
    
    public void refreshSkin() {
        mLoadingImageView.setImageDrawable(skinManager.getDrawable(R.drawable.media_loading_anim));
        mVideoAdapter.notifyDataSetChanged();
        SkinManager.setScrollViewDrawable(mGridView, skinManager.getDrawable(R.drawable.scrollbar_thumb));
    }
    
    public void updataList(ArrayList<FileNode> dataList, StorageBean storageBean) {
        mCurrentStorageBean = storageBean;
        mVideoList.clear();
        mVideoList.addAll(dataList);
        mGridView.requestFocusFromTouch();
        mGridView.setSelection(0);
        mVideoAdapter.notifyDataSetChanged();
        if (storageBean.isMounted()) {
            if (storageBean.isId3ParseCompleted()) {
                mEmptyView.setText(R.string.media_no_file);
                mEmptyView.setVisibility(mVideoList.size() <= 0 ? View.VISIBLE : View.GONE);
                mGridView.setVisibility(mVideoList.size() <= 0 ? View.GONE : View.VISIBLE);
                mLoadingView.setVisibility(View.GONE);
            } else {
                mEmptyView.setVisibility(View.GONE);
                mGridView.setVisibility(View.GONE);
                TextView showText = (TextView) mLoadingView.findViewById(R.id.media_text);
                int loadRes = R.string.music_loading_flash;
                if (storageBean.getDeviceType() == DeviceType.USB1) {
                    loadRes = R.string.music_loading_usb1;
                } else if (storageBean.getDeviceType() == DeviceType.USB2) {
                    loadRes = R.string.music_loading_usb2;
                }
                showText.setText(loadRes);
                mLoadingView.setVisibility(View.VISIBLE);
            }
        } else {
            int noDataStr = (storageBean.getDeviceType() == DeviceType.USB1 ?
                    R.string.no_device_usb_one : R.string.no_device_usb_two);
            mEmptyView.setText(noDataStr);
            mEmptyView.setVisibility(View.VISIBLE);
            mGridView.setVisibility(View.GONE);
            mLoadingView.setVisibility(View.GONE);
        }
    }

    public void setActivityHandler(Handler handler) {
        mActivityHandler = handler;
    }
   
    public void dismissDialog() {
        if (mErrorDialog != null) {
            mErrorDialog.CloseDialog();
        }
        if (mProgressDialog != null && mProgressDialog.getDialog() != null &&
                mProgressDialog.getDialog().isShowing()) {
            mProgressDialog.CloseDialog();
            Toast.makeText(mContext, R.string.file_operate_cancel, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (mActivityHandler != null) {
            mActivityHandler.obtainMessage(Video_Activity_Main.LONG_CLICK_LIST_ITEM, position, 0).sendToTarget();
        }
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mActivityHandler != null) {
            mActivityHandler.obtainMessage(Video_Activity_Main.CLICK_LIST_ITEM, position, 0).sendToTarget();
        }
    }

    public boolean isEditMode() {
        return isEditMode;
    }
    
    public void beginEdit() {
        isEditMode = true;
        unSelectAll();
    }
    
    public void selectItem(int position) {
        FileNode fileNode = mVideoList.get(position);
        fileNode.setSelected(!fileNode.isSelected());
        mVideoAdapter.notifyDataSetChanged();
    }
    
    public void selectAll() {
        for (FileNode fileNode : mVideoList) {
            fileNode.setSelected(true);
        }
        mVideoAdapter.notifyDataSetChanged();
    }
    
    public void unSelectAll() {
        for (FileNode fileNode : mVideoList) {
            fileNode.setSelected(false);
        }
        mVideoAdapter.notifyDataSetChanged();
    }
    
    public void cancelEdit() {
        isEditMode = false;
        mVideoAdapter.notifyDataSetChanged();
    }
    
    public void deleteSelected(final boolean isCollect) {
        if (mErrorDialog == null) {
            mErrorDialog = new CustomDialog();
        }
        if (AllMediaList.checkSelected(mContext, mVideoList)) {
            mErrorDialog.ShowDialog(mContext, DIALOG_TYPE.TWO_BTN_MSG, R.string.video_delect_ok);
            mErrorDialog.SetDialogListener(new OnDialogListener() {
                @Override
                public void OnDialogEvent(int id) {
                    switch (id) {
                    case R.id.pub_dialog_ok:
                        doDelete(isCollect);
                        break;
                    case R.id.pub_dialog_cancel:
                        break;
                    }
                }
                @Override
                public void OnDialogDismiss() {
                }
            });
        } else {
            mErrorDialog.ShowDialog(mContext, DIALOG_TYPE.ONE_BTN, R.string.video_delete_empty);
        }
    }

    private void doDelete(boolean isCollect) {
        ArrayList<FileNode> selectList = new ArrayList<FileNode>();
        for (FileNode fileNode : mVideoList) {
            if (fileNode.isSelected()) {
                selectList.add(fileNode);
            }
        }
        if (selectList.size() > 0) {
            if (mProgressDialog == null) {
                mProgressDialog = new CustomDialog();
            }
            mProgressDialog.showProgressDialog(mContext, R.string.delete_video_progress_title, this);
            if (isCollect) {
                AllMediaList.instance(mContext).uncollectMediaFiles(selectList, this);
            } else {
                AllMediaList.instance(mContext).deleteMediaFiles(selectList, this);
            }
        }
    }
    
    public void copySelected() {
        if (mErrorDialog == null) {
            mErrorDialog = new CustomDialog();
        }
        if (AllMediaList.checkSelected(mContext, mVideoList)) {
            mErrorDialog.SetDialogListener(new OnDialogListener() {
                @Override
                public void OnDialogEvent(int id) {
                    switch (id) {
                    case R.id.pub_dialog_ok:
                        doCopy();
                        break;
                    case R.id.pub_dialog_cancel:
                        break;
                    }
                }
                
                @Override
                public void OnDialogDismiss() {
                    mVideoAdapter.notifyDataSetChanged();
                }
            });
            mErrorDialog.showCoverDialog(mContext, mVideoList);
        } else {
            mErrorDialog.ShowDialog(mContext, DIALOG_TYPE.ONE_BTN, R.string.video_copy_empty);
        }
    }
    
    private void doCopy() {
        if (MediaUtil.checkAvailableSize(mVideoList)) {
            ArrayList<FileNode> selectList = new ArrayList<FileNode>();
            for (FileNode fileNode : mVideoList) {
                if (fileNode.isSelected()) {
                    selectList.add(fileNode);
                }
            }
            if (FileNode.existSameNameFile(selectList)) {
                Toast.makeText(mContext, R.string.copy_file_error_of_same_name,
                        Toast.LENGTH_SHORT).show();
            }
            if (selectList.size() > 0) {
                if (mProgressDialog == null) {
                    mProgressDialog = new CustomDialog();
                }
                mProgressDialog.showProgressDialog(mContext, R.string.copy_video_progress_title, this);
                AllMediaList.instance(mContext).copyToLocal(selectList, VideoListLayout.this);
            }
        } else {
            new CustomDialog().ShowDialog(mContext, DIALOG_TYPE.ONE_BTN, R.string.failed_check_available_size);
        }
    }
    
    @Override
    public void onOperateCompleted(int operateValue, int progress, int resultCode) {
        if (mProgressDialog != null) {
            mProgressDialog.updateProgressValue(progress);
        }
        if (progress == 100) {
            if (mProgressDialog != null) {
                mProgressDialog.CloseDialog();
                if (mActivityHandler != null) {
                	mActivityHandler.sendEmptyMessage(Video_Activity_Main.CANCEL_EDIT);
                }
            }
            if ((operateValue == OperateListener.OPERATE_DELETE || operateValue == OperateListener.OPERATE_UNCOLLECT)
                    && resultCode == OperateListener.OPERATE_SUCEESS) {
                for (int i = 0; i < mVideoList.size();) {
                    if (mVideoList.get(i).isSelected()) {
                        mVideoList.remove(i);
                    } else {
                        i++;
                    }
                }
            }
            unSelectAll();
            mActivityHandler.sendEmptyMessage(Video_Activity_Main.DISMISS_COPY_DIALOG);
        }
        if (operateValue == OperateListener.OPERATE_DELETE &&
                resultCode != OperateListener.OPERATE_SUCEESS) {
            Toast.makeText(mContext, "删除视频文件异常", Toast.LENGTH_SHORT).show();
        }
        if (operateValue == OperateListener.OPERATE_COPY_TO_LOCAL &&
                resultCode != OperateListener.OPERATE_SUCEESS) {
            Toast.makeText(mContext, "拷贝视频文件异常", Toast.LENGTH_SHORT).show();
        }
        if (operateValue == OperateListener.OPERATE_UNCOLLECT) { // 取消收藏操作完成。
            AllMediaList.instance(mContext).reLoadAllMedia(FileType.VIDEO);
        }
    }
    
    class VideoAdapter extends BaseAdapter implements ID3Parse.ID3ParseListener {
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
           ViewHolder mHolder = null;
           if (convertView != null) {
              mHolder = (ViewHolder) convertView.getTag();
           } else {
              mHolder = new ViewHolder();
              convertView = LayoutInflater.from(mContext).inflate(R.layout.photo_list_item, null);
              mHolder.mPhotoImageView = (ImageView) convertView.findViewById(R.id.item_photo);
              mHolder.mPhotoImageView.setBackgroundDrawable(skinManager.getDrawable(R.drawable.image_item_selector));
              mHolder.mItemSelectView = (ImageView) convertView.findViewById(R.id.item_select);
              mHolder.mPhotoName = (HKTextView) convertView.findViewById(R.id.item_filename);
              mHolder.mFromTextView = (TextView) convertView.findViewById(R.id.image_from_text);
              convertView.setTag(mHolder);
           }
          
           FileNode fileNode = mVideoList.get(position);
           mHolder.mPhotoName.setText(fileNode.getFileName());
           if (fileNode.isFromCollectTable()) {
               int resid = R.string.collect_from_local;
               int fromDeviceType = fileNode.getFromDeviceType();
               if (fromDeviceType != DeviceType.NULL) {
                   if (fromDeviceType == DeviceType.FLASH) {
                       resid = R.string.collect_from_local;
                   } else if (fromDeviceType == DeviceType.USB1) {
                       resid = R.string.collect_from_usb1;
                   } else if (fromDeviceType == DeviceType.USB2) {
                       resid = R.string.collect_from_usb2;
                   }
               }
               mHolder.mFromTextView.setText(resid);
               mHolder.mFromTextView.setVisibility(View.VISIBLE);
           } else {
               mHolder.mFromTextView.setVisibility(View.GONE);
           }
           mHolder.mItemSelectView.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
           if (isEditMode) {
               if (fileNode.isSelected()) {
                   mHolder.mItemSelectView.setImageDrawable(skinManager.getDrawable(R.drawable.music_selected_icon));
               } else {
                   mHolder.mItemSelectView.setImageDrawable(skinManager.getDrawable(R.drawable.music_selected_nomal));
               }
           }
           
           Drawable defaultDrawable = skinManager.getDrawable(R.drawable.image_icon_default);
           mHolder.mPhotoImageView.setImageDrawable(defaultDrawable);
           if (fileNode.getParseId3() == 1) {
               ImageLoad.instance(mContext).loadBitmap(mHolder.mPhotoImageView,
                       defaultDrawable, fileNode);
           } else {
               ID3Parse.instance().parseID3(position, fileNode, this);
           }
           return convertView;
        }

        @Override
        public int getCount() {
           return mVideoList.size();
        }
        @Override
        public FileNode getItem(int position) {
           return mVideoList.get(position);
        }
        @Override
        public long getItemId(int position) {
           return position;
        }
        
        class ViewHolder {
            private ImageView mPhotoImageView;
            private ImageView mItemSelectView;
            private HKTextView mPhotoName;
            private TextView mFromTextView;
        }

        @Override
        public void onID3ParseComplete(Object object, FileNode fileNode) {
            if (mGridView != null && 
                    mCurrentStorageBean.getDeviceType() == fileNode.getDeviceType()) {
                int position = (Integer) object;
                int firstChildPosition = mGridView.getFirstVisiblePosition();
                if (position - firstChildPosition >= 0) {
                    View childView = mGridView.getChildAt(position - firstChildPosition);
                    if (childView != null) {
                        ViewHolder holder = (ViewHolder) childView.getTag();
                        if (holder != null && fileNode.getParseId3() == 1) {
                            ImageLoad.instance(mContext).loadBitmap(holder.mPhotoImageView,
                                    skinManager.getDrawable(R.drawable.image_icon_default), fileNode);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        AllMediaList.instance(mContext).stopOperateThread();
    }
}
