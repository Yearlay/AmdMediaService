package com.haoke.ui.video;

import java.util.ArrayList;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
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
import com.haoke.ui.widget.CopyDialog;
import com.haoke.ui.widget.CustomDialog;
import com.haoke.ui.widget.CustomDialog.DIALOG_TYPE;
import com.haoke.ui.widget.CustomDialog.OnDialogListener;
import com.haoke.ui.widget.DeleteProgressDialog;
import com.haoke.ui.widget.HKTextView;
import com.haoke.util.DebugLog;

public class VideoListLayout extends RelativeLayout implements
        OnItemClickListener, OnItemLongClickListener, OperateListener, OnDismissListener, OnCancelListener{
    private Context mContext;
    private GridView mGridView;
    private TextView mEmptyView;
    private View mLoadingView;
    private ImageView mLoadingImageView;
    private VideoAdapter mVideoAdapter;
    private Handler mActivityHandler;
    private boolean isEditMode;
    private CopyDialog mCopyDialog;
    private DeleteProgressDialog mProgressDialog;
    private CustomDialog mErrorDialog;
    private StorageBean mCurrentStorageBean;
    
    private SkinManager skinManager;
    private Drawable mLoadingImageDrawable;
    private Drawable mGridViewScrollbarThumb;
    private final String TAG = this.getClass().getSimpleName();
    
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
    
    public void refreshSkin(boolean loading) {
    	if(loading || mLoadingImageDrawable == null){
    		mLoadingImageDrawable = skinManager.getDrawable(R.drawable.media_loading_anim);
    		mGridViewScrollbarThumb = skinManager.getDrawable(R.drawable.scrollbar_thumb);
    	}
    	
    	if(!loading){
    		mLoadingImageView.setImageDrawable(mLoadingImageDrawable);
    		mVideoAdapter.notifyDataSetChanged();
    		SkinManager.setScrollViewDrawable(mGridView, mGridViewScrollbarThumb);
    	}
    }
    
    public void onPause() {
        mLoadingImageView.setImageDrawable(null); // 需要这个操作，解决mLoadingImageView在界面销毁之后不回收的问题。
    }
    
    public void updataList(ArrayList<FileNode> dataList, StorageBean storageBean, boolean toHead) {
        mCurrentStorageBean = storageBean;
        mVideoList.clear();
        mVideoList.addAll(dataList);
        if (mVideoList.size() > 0) {
            mGridView.requestFocusFromTouch();
            if (toHead) {
                mGridView.setSelection(0);
            }
        }
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
        if (mProgressDialog != null) {
            mProgressDialog.closeDialog();
        }
        if (mCopyDialog != null) {
            mCopyDialog.closeCopyDialog();
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
                public void OnDialogDismiss() {}
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
            if (mProgressDialog != null) {
                mProgressDialog.closeDialog();
            }
            mProgressDialog = new DeleteProgressDialog();
            mProgressDialog.showProgressDialog(mContext, R.string.delete_video_progress_title);
            if (isCollect) {
                AllMediaList.instance(mContext).uncollectMediaFiles(selectList, this);
            } else {
                AllMediaList.instance(mContext).deleteMediaFiles(selectList, this);
            }
        }
    }
    
    public void copySelected() {
        if (mCopyDialog != null) {
            mCopyDialog.closeCopyDialog();
        }
        mCopyDialog = new CopyDialog();
        if (AllMediaList.checkSelected(mContext, mVideoList)) {
            mCopyDialog.SetDialogListener(new CopyDialog.OnDialogListener() {
                @Override
                public void OnDialogEvent(int id) {
                    switch (id) {
                    case R.id.copy_ok:
                        mCopyDialog.updateProgressValue(0);
                        doCopy();
                        break;
                    case R.id.copy_cancel:
                        mCopyDialog.closeCopyDialog();
                        break;
                    }
                }
                
                @Override
                public void OnDialogDismiss() {
                    stopFileOperate();
                    mVideoAdapter.notifyDataSetChanged();
                }
            });
            mCopyDialog.showCopyDialog(mContext, mVideoList, this);
        } else {
            if (mErrorDialog == null) {
                mErrorDialog = new CustomDialog();
            }
            mErrorDialog.ShowDialog(mContext, DIALOG_TYPE.ONE_BTN, R.string.video_copy_empty);
        }
    }
    
    private void doCopy() {
        DebugLog.v(TAG, "doCopy --> mVideoList.size() = "+ mVideoList.size());
        if (MediaUtil.checkAvailableSize(mVideoList)) {
            ArrayList<FileNode> selectList = new ArrayList<FileNode>();
            for (FileNode fileNode : mVideoList) {
                if (fileNode.isSelected()) {
                    selectList.add(fileNode);
                }
            }
            DebugLog.v(TAG, "doCopy --> selectList.size() = "+ selectList.size());
            if (FileNode.existSameNameFile(selectList)) {
                Toast.makeText(mContext, R.string.copy_file_error_of_same_name,
                        Toast.LENGTH_SHORT).show();
            }
            if (selectList.size() > 0) {
                AllMediaList.instance(mContext).copyToLocal(selectList, VideoListLayout.this);
            } else if (mCopyDialog != null) {
                mCopyDialog.closeCopyDialog();
            }
        } else {
            //modify bug 20966 begin
            mCopyDialog.closeCopyDialog();
            //modify bug 20966 end
            new CustomDialog().ShowDialog(mContext, DIALOG_TYPE.ONE_BTN, R.string.failed_check_available_size);
        }
    }
    
    private Toast mDeleteErrorToast;
    private Toast mCopyErrorEndToast;
    
    private void doOperateDelete(int progress, int resultCode) {
        if (mProgressDialog != null) {
            mProgressDialog.updateProgressValue(progress);
        }
        if (progress == 100) {
            if (mProgressDialog != null) {
                mProgressDialog.closeDialog();
                if (mActivityHandler != null) {
                    mActivityHandler.sendEmptyMessage(Video_Activity_Main.CANCEL_EDIT);
                }
            }
            if (resultCode == OperateListener.OPERATE_SUCEESS) {
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
        if (resultCode != OperateListener.OPERATE_SUCEESS) {
            if (mDeleteErrorToast == null) {
                mDeleteErrorToast = Toast.makeText(mContext, R.string.delete_video_file_exception, Toast.LENGTH_SHORT);
            }
            mDeleteErrorToast.show();
        }
    }
    
    private void doOperateUnCollect(int progress, int resultCode) {
        if (mProgressDialog != null) {
            mProgressDialog.updateProgressValue(progress);
        }
        if (progress == 100) {
            if (mProgressDialog != null) {
                mProgressDialog.closeDialog();
                if (mActivityHandler != null) {
                    mActivityHandler.sendEmptyMessage(Video_Activity_Main.CANCEL_EDIT);
                }
            }
            if (resultCode == OperateListener.OPERATE_SUCEESS) {
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
        AllMediaList.instance(mContext).reLoadAllMedia(FileType.VIDEO);
    }
    
    private void doOperateCopy(int progress, int resultCode) {
        DebugLog.v(TAG, "doOperateCopy --> progress = "+ progress +"; resultCode ="+ resultCode);
        if (mCopyDialog == null) {
            return;
        }
        mCopyDialog.updateProgressValue(progress);
        if (progress == 100) {
            mCopyDialog.closeCopyDialog();
            if (mActivityHandler != null) {
                mActivityHandler.sendEmptyMessage(Video_Activity_Main.CANCEL_EDIT);
            }
            unSelectAll();
            mActivityHandler.sendEmptyMessage(Video_Activity_Main.DISMISS_COPY_DIALOG);
        }
        if (resultCode != OperateListener.OPERATE_SUCEESS) {
            if (mCopyErrorEndToast == null) {
                mCopyErrorEndToast = Toast.makeText(mContext, R.string.copy_video_file_exception, Toast.LENGTH_SHORT);
            }
            mCopyErrorEndToast.show();
        }
    }
    
    @Override
    public void onOperateCompleted(int operateValue, int progress, int resultCode) {
        switch (operateValue) {
            case OperateListener.OPERATE_DELETE:
                doOperateDelete(progress, resultCode);
                break;
            case OperateListener.OPERATE_UNCOLLECT:
                doOperateUnCollect(progress, resultCode);
                break;
            case OperateListener.OPERATE_COPY_TO_LOCAL:
                doOperateCopy(progress, resultCode);
                break;

            default:
                break;
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
              mHolder.mFromTextView = (TextView) convertView.findViewById(R.id.image_from_text);
              convertView.setTag(mHolder);
           }
           mHolder.mPhotoImageView.setBackgroundDrawable(skinManager.getDrawable(R.drawable.image_item_selector));
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
        stopFileOperate();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (mCopyDialog != null) {
            mCopyDialog.interruptCheckOperator();
        }
        stopFileOperate();
        if (mActivityHandler != null) {
            mActivityHandler.sendEmptyMessage(Video_Activity_Main.CANCEL_EDIT);
        }
    }
    
    private void stopFileOperate() {
        AllMediaList.instance(mContext).stopOperateThread();
    }
}
