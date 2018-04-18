package com.haoke.ui.image;

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
import com.haoke.bean.ImageLoad;
import com.haoke.bean.StorageBean;
import com.haoke.constant.MediaUtil;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.data.AllMediaList;
import com.haoke.data.OperateListener;
import com.haoke.mediaservice.R;
import com.haoke.ui.video.Video_Activity_Main;
import com.haoke.ui.widget.CopyDialog;
import com.haoke.ui.widget.CustomDialog;
import com.haoke.ui.widget.CustomDialog.DIALOG_TYPE;
import com.haoke.ui.widget.CustomDialog.OnDialogListener;
import com.haoke.ui.widget.HKTextView;
import com.haoke.util.DebugLog;

public class PhotoListLayout extends RelativeLayout implements OnItemClickListener, OnItemLongClickListener,
        OperateListener, OnDismissListener, OnCancelListener {
    public PhotoListLayout(Context context) {
        super(context);
    }

    public PhotoListLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PhotoListLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private Context mContext;
    private GridView mGridView;
    private TextView mEmptyView;
    private View mLoadingView;
    private ImageView mLoadingImageView;
    private PhotoAdapter mPhotoAdapter;
    private Handler mActivityHandler;
    private boolean isEditMode;
    private CustomDialog mProgressDialog;
    private CustomDialog mErrorDialog;
    
    private StorageBean mCurrentStorageBean;
    private ArrayList<FileNode> mPhotoList = new ArrayList<FileNode>();
    
    private SkinManager skinManager;
    private Drawable mLoadingImageDrawable;
    private Drawable mGridViewScrollbarThumb;
    private CopyDialog mCopyDialog;
    private final String TAG = this.getClass().getSimpleName();
    
    public void updataList(ArrayList<FileNode> dataList, StorageBean storageBean) {
        DebugLog.e(Image_Activity_Main.TAG,"updataList  size: " + dataList.size());
        mCurrentStorageBean = storageBean;
        mPhotoList.clear();
        mPhotoList.addAll(dataList);
        mPhotoAdapter.notifyDataSetChanged();
    }

    public void refreshView(StorageBean storageBean, boolean toHead) {
        if (mGridView == null || mPhotoAdapter == null) {
            return;
        }
        if (mPhotoList.size() > 0) {
            mGridView.requestFocusFromTouch();
            if (toHead) {
                mGridView.setSelection(0);
            }
        }
        if (storageBean.isMounted()) {
            DebugLog.e(Image_Activity_Main.TAG,"refreshView  isMounted");
            if (storageBean.isId3ParseCompleted()) {
                DebugLog.e(Image_Activity_Main.TAG,"refreshView  isId3ParseCompleted");
                mEmptyView.setText(R.string.media_no_file);
                mEmptyView.setVisibility(mPhotoList.size() <= 0 ? View.VISIBLE : View.GONE);
                mGridView.setVisibility(mPhotoList.size() <= 0 ? View.GONE : View.VISIBLE);
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
            DebugLog.e(Image_Activity_Main.TAG,"refreshView  is not Mounted");
            int noDataStr = (storageBean.getDeviceType() == DeviceType.USB1 ?
                    R.string.no_device_usb_one : R.string.no_device_usb_two);
            mEmptyView.setText(noDataStr);
            mEmptyView.setVisibility(View.VISIBLE);
            mGridView.setVisibility(View.GONE);
            mLoadingView.setVisibility(View.GONE);
        }
    }

    public void setActivityHandler(Handler handler, Context context) {
        mActivityHandler = handler;
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // 初始化控件
        mEmptyView = (TextView) findViewById(R.id.image_list_empty);
        mLoadingView = findViewById(R.id.loading_layout);
        mLoadingImageView = (ImageView) mLoadingView.findViewById(R.id.media_loading_imageview);

        mGridView = (GridView) findViewById(R.id.image_grid_list);
        mGridView.setSelector(new ColorDrawable(Color.TRANSPARENT));
        mGridView.setOnItemClickListener(this);
        mGridView.setOnItemLongClickListener(this);
        mPhotoAdapter = new PhotoAdapter();
        mGridView.setAdapter(mPhotoAdapter);
        
        if (mCurrentStorageBean != null) {
            refreshView(mCurrentStorageBean, true);
        }
        skinManager = SkinManager.instance(getContext());
    }
    
    public void refreshSkin(boolean loading) {
        if(loading || mLoadingImageDrawable == null){
            mLoadingImageDrawable = skinManager.getDrawable(R.drawable.media_loading_anim);
            mGridViewScrollbarThumb = skinManager.getDrawable(R.drawable.scrollbar_thumb);
        }
        
        if(!loading){
            mLoadingImageView.setImageDrawable(mLoadingImageDrawable);
            mPhotoAdapter.notifyDataSetChanged();
            SkinManager.setScrollViewDrawable(mGridView, mGridViewScrollbarThumb);
        }
    }
    
    public void dismissDialog() {
        DebugLog.e(Image_Activity_Main.TAG,"dismissDialog");
        if (mErrorDialog != null) {
            mErrorDialog.CloseDialog();
        }
        if (mProgressDialog != null) {
            mProgressDialog.CloseDialog();
        }
        if (mCopyDialog != null) {
            mCopyDialog.closeCopyDialog();
        }
    }
    
    public int getPhotoListSize() {
        return mPhotoList.size();
    }

    public FileNode getFileNode(int position) {
        FileNode fileNode = null;
        if (position < mPhotoList.size()) {
            fileNode = mPhotoList.get(position);
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
        FileNode fileNode = mPhotoList.get(position);
        fileNode.setSelected(!fileNode.isSelected());
        mPhotoAdapter.notifyDataSetChanged();
    }
    
    public void selectAll() {
        for (FileNode fileNode : mPhotoList) {
            fileNode.setSelected(true);
        }
        mPhotoAdapter.notifyDataSetChanged();
    }
    
    public void unSelectAll() {
        for (FileNode fileNode : mPhotoList) {
            fileNode.setSelected(false);
        }
        mPhotoAdapter.notifyDataSetChanged();
    }
    
    public void cancelEdit() {
        isEditMode = false;
        mPhotoAdapter.notifyDataSetChanged();
    }
    
    public void deleteSelected(final boolean isCollect) {
        if (mErrorDialog == null) {
            mErrorDialog = new CustomDialog();
        }
        if (AllMediaList.checkSelected(mContext, mPhotoList)) {
            mErrorDialog.ShowDialog(mContext, DIALOG_TYPE.TWO_BTN_MSG, R.string.image_delect_ok);
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
            mErrorDialog.ShowDialog(mContext, DIALOG_TYPE.ONE_BTN, R.string.image_delete_empty);
        }
    }

    private void doDelete(boolean isCollect) {
        ArrayList<FileNode> selectList = new ArrayList<FileNode>();
        for (FileNode fileNode : mPhotoList) {
            if (fileNode.isSelected()) {
                selectList.add(fileNode);
            }
        }
        if (selectList.size() > 0) {
            if (mProgressDialog == null) {
                mProgressDialog = new CustomDialog();
            }
            mProgressDialog.showProgressDialog(mContext, R.string.delete_image_progress_title, this);
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
        DebugLog.v(TAG, "copySelected --> mPhotoList.size() = "+ mPhotoList.size());
        if (AllMediaList.checkSelected(mContext, mPhotoList)) {
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
                    mPhotoAdapter.notifyDataSetChanged();
                }
            });
            mCopyDialog.showCopyDialog(mContext, mPhotoList, this);
        } else {
            if (mErrorDialog == null) {
                mErrorDialog = new CustomDialog();
            }
            mErrorDialog.ShowDialog(mContext, DIALOG_TYPE.ONE_BTN, R.string.image_copy_empty);
        }
    }
    
    private void doCopy() {
        DebugLog.v(TAG, "doCopy --> mPhotoList.size() = "+ mPhotoList.size());
        if (MediaUtil.checkAvailableSize(mPhotoList)) {
            ArrayList<FileNode> selectList = new ArrayList<FileNode>();
            for (FileNode fileNode : mPhotoList) {
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
                AllMediaList.instance(mContext).copyToLocal(selectList,PhotoListLayout.this);
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
    
    private void doOperateCopy(int progress, int resultCode) {
        DebugLog.v(TAG, "doOperateCopy --> progress = "+ progress + "; resultCode = "+ resultCode);
        if (mCopyDialog == null) {
            return;
        }
        mCopyDialog.updateProgressValue(progress);
        if (progress == 100) {
            if (mCopyDialog != null) {
                mCopyDialog.closeCopyDialog();;
                if (mActivityHandler != null) {
                    mActivityHandler.sendEmptyMessage(Video_Activity_Main.CANCEL_EDIT);
                }
            }
            unSelectAll();
            mActivityHandler.sendEmptyMessage(Image_Activity_Main.DISMISS_COPY_DIALOG);
        }
        if (resultCode != OperateListener.OPERATE_SUCEESS) {
            Toast.makeText(mContext, R.string.copy_picture_file_exception, Toast.LENGTH_SHORT).show();
        }
    }

    private void doOperateDelete(int progress, int resultCode) {
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
            if (resultCode == OperateListener.OPERATE_SUCEESS) {
                for (int i = 0; i < mPhotoList.size();) {
                    if (mPhotoList.get(i).isSelected()) {
                        mPhotoList.remove(i);
                    } else {
                        i++;
                    }
                }
            }
            unSelectAll();
            mActivityHandler.sendEmptyMessage(Image_Activity_Main.DISMISS_COPY_DIALOG);
        }
        if (resultCode != OperateListener.OPERATE_SUCEESS) {
            Toast.makeText(mContext, R.string.delete_picture_file_exception, Toast.LENGTH_SHORT).show();
        }
    }

    private void doOperateUncollect(int progress, int resultCode) {
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
            if (resultCode == OperateListener.OPERATE_SUCEESS) {
                for (int i = 0; i < mPhotoList.size();) {
                    if (mPhotoList.get(i).isSelected()) {
                        mPhotoList.remove(i);
                    } else {
                        i++;
                    }
                }
            }
            unSelectAll();
            mActivityHandler.sendEmptyMessage(Image_Activity_Main.DISMISS_COPY_DIALOG);
        }
        AllMediaList.instance(mContext).reLoadAllMedia(FileType.IMAGE);
    }
    
    @Override
    public void onOperateCompleted(int operateValue, int progress, int resultCode) {
        switch (operateValue) {
        case OperateListener.OPERATE_UNCOLLECT:
            doOperateUncollect(progress, resultCode);
            break;
        case OperateListener.OPERATE_DELETE:
            doOperateDelete(progress, resultCode);
            break;
        case OperateListener.OPERATE_COPY_TO_LOCAL:
            doOperateCopy(progress, resultCode);
            break;
        default:
            break;
        }
    }
    
    class PhotoAdapter extends BaseAdapter {
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (mPhotoList.size() <= 0) {
                return convertView;
            }
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
            DebugLog.e(Image_Activity_Main.TAG,"getView size: " + mPhotoList.size() + "   ,position: " + position);
            FileNode fileNode = mPhotoList.get(position);
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
            ImageLoad.instance(mContext).loadBitmap(mHolder.mPhotoImageView, defaultDrawable, fileNode);
            return convertView;
        }

        @Override
        public int getCount() {
            return mPhotoList.size();
        }
        @Override
        public FileNode getItem(int position) {
            return mPhotoList.get(position);
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
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (mActivityHandler != null) {
            mActivityHandler.obtainMessage(Image_Activity_Main.LONG_CLICK_LIST_ITEM, position, 0).sendToTarget();
        }
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mActivityHandler != null) {
            mActivityHandler.obtainMessage(Image_Activity_Main.CLICK_LIST_ITEM, position, 0).sendToTarget();
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
            mActivityHandler.sendEmptyMessage(Image_Activity_Main.CANCEL_EDIT);
        }
    }
    
    private void stopFileOperate() {
        AllMediaList.instance(mContext).stopOperateThread();
    }
}
