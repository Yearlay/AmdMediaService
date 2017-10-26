package com.haoke.ui.image;

import java.util.ArrayList;

import haoke.ui.util.HKTextView;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.TextView;

import com.haoke.bean.FileNode;
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

public class PhotoListLayout extends RelativeLayout implements OnItemClickListener, OnItemLongClickListener,
        OperateListener, OnDismissListener {
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
    private PhotoAdapter mPhotoAdapter;
    private Handler mActivityHandler;
    private boolean isEditMode;
    private CustomDialog mProgressDialog;
    private CustomDialog mErrorDialog;
    
    private StorageBean mCurrentStorageBean;
    private ArrayList<FileNode> mPhotoList = new ArrayList<FileNode>();
    
    public void updataList(ArrayList<FileNode> dataList, StorageBean storageBean) {
        mCurrentStorageBean = storageBean;
        mPhotoList.clear();
        mPhotoList.addAll(dataList);
        if (mGridView != null) {
            refreshView(storageBean);
        }
    }

    private void refreshView(StorageBean storageBean) {
        mGridView.requestFocusFromTouch();
        mGridView.setSelection(0);
        mPhotoAdapter.notifyDataSetChanged();
        if (storageBean.isMounted()) {
            if (storageBean.isId3ParseCompleted()) {
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

        mGridView = (GridView) findViewById(R.id.image_grid_list);
        mGridView.setChoiceMode(GridView.CHOICE_MODE_SINGLE);
        mGridView.setSelector(new ColorDrawable(Color.TRANSPARENT));
        mGridView.setOnItemClickListener(this);
        mGridView.setOnItemLongClickListener(this);
        mPhotoAdapter = new PhotoAdapter();
        mGridView.setAdapter(mPhotoAdapter);
        
        if (mCurrentStorageBean != null) {
            refreshView(mCurrentStorageBean);
        }
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
                public void OnDialogDismiss() {
                }
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
        if (mErrorDialog == null) {
            mErrorDialog = new CustomDialog();
        }
        if (AllMediaList.checkSelected(mContext, mPhotoList)) {
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
                    mPhotoAdapter.notifyDataSetChanged();
                }
            });
            mErrorDialog.showCoverDialog(mContext, mPhotoList);
        } else {
            mErrorDialog.ShowDialog(mContext, DIALOG_TYPE.ONE_BTN, R.string.image_copy_empty);
        }
    }
    
    private void doCopy() {
        if (MediaUtil.checkAvailableSize(mPhotoList)) {
            ArrayList<FileNode> selectList = new ArrayList<FileNode>();
            for (FileNode fileNode : mPhotoList) {
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
                mProgressDialog.showProgressDialog(mContext, R.string.copy_image_progress_title, this);
                AllMediaList.instance(mContext).copyToLocal(selectList, PhotoListLayout.this);
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
            }
            if ((operateValue == OperateListener.OPERATE_DELETE || operateValue == OperateListener.OPERATE_UNCOLLECT)
                    && resultCode == OperateListener.OPERATE_SUCEESS) {
                for (int i = 0; i < mPhotoList.size();) {
                    if (mPhotoList.get(i).isSelected()) {
                        mPhotoList.remove(i);
                    } else {
                        i++;
                    }
                }
            }
            unSelectAll();
        }
        if (operateValue == OperateListener.OPERATE_DELETE &&
                resultCode != OperateListener.OPERATE_SUCEESS) {
            Toast.makeText(mContext, "删除图片文件异常", Toast.LENGTH_SHORT).show();
        }
        if (operateValue == OperateListener.OPERATE_COPY_TO_LOCAL &&
                resultCode != OperateListener.OPERATE_SUCEESS) {
            Toast.makeText(mContext, "拷贝图片文件异常", Toast.LENGTH_SHORT).show();
        }
        if (operateValue == OperateListener.OPERATE_UNCOLLECT) { // 取消收藏操作完成。
            AllMediaList.instance(mContext).reLoadAllMedia(FileType.IMAGE);
        }
    }
    
    class PhotoAdapter extends BaseAdapter {
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
                mHolder.mItemSelectView.setImageResource(fileNode.isSelected() ?
                        R.drawable.music_selected_icon : R.drawable.music_selected_nomal);
            }
            mHolder.mPhotoImageView.setImageResource(R.drawable.image_icon_default);
            ImageLoad.instance(mContext).loadBitmap(mHolder.mPhotoImageView, R.drawable.image_icon_default, fileNode);
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
        AllMediaList.instance(mContext).stopOperateThread();
    }
}
