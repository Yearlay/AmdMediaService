package com.haoke.ui.video;

import java.util.ArrayList;

import haoke.ui.util.HKTextView;
import haoke.ui.util.OnHKTouchListener;
import haoke.ui.util.TOUCH_ACTION;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.haoke.window.HKWindowManager;

public class VideoListFragment extends Fragment implements OnHKTouchListener,
        OnItemClickListener, OnItemLongClickListener, OperateListener, OnDismissListener{
    private Context mContext;
    private View rootView;
    private GridView mGridView;
    private TextView mEmptyView;
    private View mLoadingView;
    private VideoAdapter mVideoAdapter;
    private Handler mActivityHandler;
    private boolean isEditMode;
    private CustomDialog mProgressDialog;
    private CustomDialog mErrorDialog;
    private StorageBean mCurrentStorageBean;
    
    private ArrayList<FileNode> mVideoList = new ArrayList<FileNode>();
    
    public void updataList(ArrayList<FileNode> dataList, StorageBean storageBean) {
        mCurrentStorageBean = storageBean;
        mVideoList.clear();
        mVideoList.addAll(dataList);
        if (rootView != null) {
            refreshView(storageBean);
        }
    }

    private void refreshView(StorageBean storageBean) {
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
            String noDataStr = mContext.getString(R.string.music_no_device_usb) +
                    (storageBean.getDeviceType() == DeviceType.USB1 ? "1" : "2");
            mEmptyView.setText(noDataStr);
            mEmptyView.setVisibility(View.VISIBLE);
            mGridView.setVisibility(View.GONE);
            mLoadingView.setVisibility(View.GONE);
        }
    }
    
    @Override
    public void onAttach(Activity activity) {
        if (activity instanceof Video_Activity_Main) {
            mActivityHandler = ((Video_Activity_Main) activity).getHandler();
        }
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        mActivityHandler = null;
        super.onDetach();
    }
    
   @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        HKWindowManager.fullScreen(this.getActivity(), false);
        mContext = getActivity();
        
        // 初始化控件
        rootView = inflater.inflate(R.layout.photo_list_layout, null);
        mEmptyView = (TextView) rootView.findViewById(R.id.image_list_empty);
        mLoadingView = rootView.findViewById(R.id.loading_layout);

        mGridView = (GridView) rootView.findViewById(R.id.image_grid_list);
        mGridView.setChoiceMode(GridView.CHOICE_MODE_SINGLE);
        mGridView.setSelector(new ColorDrawable(Color.TRANSPARENT));
        mGridView.setOnItemClickListener(this);
        mGridView.setOnItemLongClickListener(this);
        mVideoAdapter = new VideoAdapter();
        mGridView.setAdapter(mVideoAdapter);
        
        if (mCurrentStorageBean != null) {
            refreshView(mCurrentStorageBean);
        }
        
        return rootView;
    }
   
   @Override
    public void onPause() {
        if (mErrorDialog != null) {
            mErrorDialog.CloseDialog();
        }
        if (mProgressDialog != null && mProgressDialog.getDialog() != null &&
                mProgressDialog.getDialog().isShowing()) {
            mProgressDialog.CloseDialog();
            Toast.makeText(mContext, R.string.file_operate_cancel, Toast.LENGTH_SHORT).show();
        }
        super.onPause();
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

    @Override
    public void OnHKTouchEvent(View view, TOUCH_ACTION action) {
        if (action == TOUCH_ACTION.BTN_CLICKED) {
            switch (view.getId()) {
            default:
                break;
            }
        } else if (action == TOUCH_ACTION.BTN_DOWN) {
        } else if (action == TOUCH_ACTION.BTN_UP) {
        }
    }
    
    public FileNode getFileNode(int position) {
        FileNode fileNode = null;
        if (position < mVideoList.size()) {
            fileNode = mVideoList.get(position);
        }
        return fileNode;
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
            if (selectList.size() > 0) {
                if (mProgressDialog == null) {
                    mProgressDialog = new CustomDialog();
                }
                mProgressDialog.showProgressDialog(mContext, R.string.copy_video_progress_title, this);
                AllMediaList.instance(mContext).copyToLocal(selectList, VideoListFragment.this);
            }
        } else {
            if (mErrorDialog == null) {
                mErrorDialog = new CustomDialog();
            }
            mErrorDialog.ShowDialog(mContext, DIALOG_TYPE.ONE_BTN, R.string.failed_check_available_size);
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
              mHolder.mItemSelectView = (ImageView) convertView.findViewById(R.id.item_select);
              mHolder.mPhotoName = (HKTextView) convertView.findViewById(R.id.item_filename);
              convertView.setTag(mHolder);
           }
          
           FileNode fileNode = mVideoList.get(position);
           mHolder.mPhotoName.setText(fileNode.getFileName());
           mHolder.mItemSelectView.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
           if (isEditMode) {
               mHolder.mItemSelectView.setImageResource(fileNode.isSelected() ?
                       R.drawable.music_selected_icon : R.drawable.music_selected_nomal);
           }
           
           mHolder.mPhotoImageView.setImageResource(R.drawable.image_icon_default);
           if (fileNode.getParseId3() == 1) {
               ImageLoad.instance(mContext).loadBitmap(mHolder.mPhotoImageView,
                       R.drawable.image_icon_default, fileNode);
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
                                    R.drawable.image_icon_default, fileNode);
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
