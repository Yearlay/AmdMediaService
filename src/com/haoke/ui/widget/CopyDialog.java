package com.haoke.ui.widget;

import java.io.File;
import java.util.ArrayList;

import com.amd.util.SkinManager;
import com.amd.util.SkinManager.SkinListener;
import com.haoke.bean.FileNode;
import com.haoke.constant.MediaUtil;
import com.haoke.mediaservice.R;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class CopyDialog implements OnClickListener {
    private static final String TAG = "CopyDialog";
    private Context mContext;
    private View mRootView;
    
    private LinearLayout mCheckLayout; // 拷贝检查layout
    
    private LinearLayout mCoverLayout; // 拷贝覆盖layout
    private LinearLayout mCoverList;
    private Button mOkButton;
    private Button mCancelButton;
    
    private LinearLayout mProgressLayout; // 拷贝进度layout
    private TextView mTextView;
    
    private Drawable mRootViewDrawable;
    private Drawable mOkButtonDrawable;
    private Drawable mCancelButtonDrawable;
    
    private ArrayList<FileNode> mDataList;
    
    public interface OnDialogListener {
        abstract void OnDialogEvent(int id);
        abstract void OnDialogDismiss();
    }
    private OnDialogListener mDialogListener;
    public void SetDialogListener(OnDialogListener listener) {
        mDialogListener = listener;
    }

    private MyDialog mDialog;
    public Dialog getDialog() {
        return mDialog;
    }
    
    public void showCopyDialog(Context context, ArrayList<FileNode> dataList, OnCancelListener cancelListener) {
        closeCopyDialog();
        mContext = context;
        mDialog = new MyDialog(context, R.style.pub_dialog);
        mDialog.setSkinListener(mSkinListener);
        mDialog.setCanceledOnTouchOutside(true);
        mDialog.setOnCancelListener(cancelListener);
        mDialog.setContentView(R.layout.custom_dialog_copy);
        mRootView = mDialog.findViewById(R.id.copy_main_layout);
        mRootView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        // 初始化界面: 
        // 1) 拷贝文件前检查layout;
        mCheckLayout = (LinearLayout) mDialog.findViewById(R.id.check_layout);
        // 2) 文件覆盖提示layout;
        mCoverLayout = (LinearLayout) mDialog.findViewById(R.id.cover_layout);
        mCoverList = (LinearLayout) mDialog.findViewById(R.id.cover_list);
        
        mDataList = dataList;
        
        mOkButton = (Button) mDialog.findViewById(R.id.copy_ok);
        mOkButton.setOnClickListener(this);
        mCancelButton = (Button) mDialog.findViewById(R.id.copy_cancel);
        mCancelButton.setOnClickListener(this);
        // 3) 文件拷贝layout;
        mProgressLayout = (LinearLayout) mDialog.findViewById(R.id.progress_layout);
        mTextView = (TextView) mDialog.findViewById(R.id.copy_progress_text);
        
        refreshSkin(true);
        refreshSkin(false);
        
        // 显示对话框
        try {
            mDialog.show();
        } catch (Exception e) {
        }
        // 开始检查拷贝的文件是否存在。
        mHandler.sendEmptyMessage(CHECK_SHOW);
    }
    
    private int mCopyState = 0;
    private static final int CHECK_SHOW = 1;
    private static final int CHECK_RESULT = 2;
    private static final int COVER_SHOW = 3;
    private static final int PROGRESS_SHOW = 4;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CHECK_SHOW:
                    checkShow();
                    break;
                case CHECK_RESULT:
                    checkResult();
                    break;
                    
                case COVER_SHOW:
                    coverShow();
                    break;
                    
                case PROGRESS_SHOW:
                    progressShow(msg.arg1);
                    break;

                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };
    
    private void checkShow() {
        //modify bug 20928 begin
        if (mDialog == null) {
            return;
        }
        //modify bug 20928 begin
        mCopyState = CHECK_SHOW;
        mCheckLayout.setVisibility(View.VISIBLE);
        mCoverLayout.setVisibility(View.INVISIBLE);
        mProgressLayout.setVisibility(View.INVISIBLE);
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (FileNode fileNode : mDataList) {
                    String destFilePath = MediaUtil.LOCAL_COPY_DIR + "/" +
                            fileNode.getFilePath().substring(fileNode.getFilePath().lastIndexOf('/') + 1);
                    File destFile = new File(destFilePath);
                    fileNode.setCopyDestExist(destFile.exists());
                }
                mHandler.sendEmptyMessage(CHECK_RESULT);
            }
        }).start();
    }
    
    private void checkResult() {
        mCopyState = CHECK_RESULT;
        for (final FileNode fileNode : mDataList) {
            if (fileNode.isCopyDestExist() && fileNode.isSelected()) {
                AmdCheckBox checkBox = new AmdCheckBox(mContext);
                checkBox.setText(fileNode.getFileName());
                checkBox.setChecked(true);
                checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        fileNode.setSelected(isChecked);
                    }
                });
                mCoverList.addView(checkBox);
            }
        }
        if (mCoverList.getChildCount() > 0) {
            mHandler.sendEmptyMessage(COVER_SHOW);
        } else {
            if (mDialogListener != null) {
                mDialogListener.OnDialogEvent(R.id.copy_ok);
            }
        }
    }
    
    private void coverShow() {
        //modify bug 20928 begin
        if (mDialog == null) {
            return;
        }
        //modify bug 20928 end
        mCopyState = COVER_SHOW;
        mCheckLayout.setVisibility(View.INVISIBLE);
        mCoverLayout.setVisibility(View.VISIBLE);
        mProgressLayout.setVisibility(View.INVISIBLE);
    }

    private void progressShow(int value) {
        //modify bug 20928 begin
        if (mDialog == null) {
            return;
        }
        //modify bug 20928 end
        if (mCopyState != PROGRESS_SHOW) {
            mCopyState = PROGRESS_SHOW;
            mCheckLayout.setVisibility(View.INVISIBLE);
            mCoverLayout.setVisibility(View.INVISIBLE);
            mProgressLayout.setVisibility(View.VISIBLE);
        }
        if (mDialog != null && mTextView != null) {
            mTextView.setText(value + "%");
        }
    }
    
    public boolean isCopying() {
        return mDialog != null && mDialog.isShowing() && mCopyState == PROGRESS_SHOW;
    }

    public void updateProgressValue(int value) {
        mHandler.obtainMessage(PROGRESS_SHOW, value, 0).sendToTarget();
    }
    
    @Override
    public void onClick(View view) {
        if (mDialogListener != null) {
            mDialogListener.OnDialogEvent(view.getId());
        }
    }

    public void closeCopyDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
        if (mDialogListener != null) {
            mDialogListener.OnDialogDismiss();
        }
    }
    
    private void refreshSkin(boolean loading) {
        if (mContext != null) {
            SkinManager skinManager = SkinManager.instance(mContext);
            if (loading || mRootViewDrawable==null) {
                mRootViewDrawable = skinManager.getDrawable(R.drawable.pub_msgbox_bg1);
                mOkButtonDrawable = skinManager.getDrawable(R.drawable.bd_dialog_button);
                mCancelButtonDrawable = skinManager.getDrawable(R.drawable.bd_dialog_button);
            }
            if (!loading) {
                if (mRootView != null) {
                    mRootView.setBackgroundDrawable(mRootViewDrawable);
                }
                if (mOkButton != null) {
                    mOkButton.setBackgroundDrawable(mOkButtonDrawable);
                }
                if (mCancelButton != null) {
                    mCancelButton.setBackgroundDrawable(mCancelButtonDrawable);
                }
                if (mCoverList != null && mCoverList.getChildCount() > 0) {
                    for (int index = 0; index < mCoverList.getChildCount(); index++) {
                        View view = mCoverList.getChildAt(index);
                        if (view instanceof AmdCheckBox) {
                            AmdCheckBox amdCheckBox = ((AmdCheckBox) view);
                            amdCheckBox.setChecked(amdCheckBox.isChecked());
                        }
                    }
                }
            }
        }
    }
    
    private SkinListener mSkinListener = new SkinListener(new Handler()) {
        @Override
        public void loadingSkinData() {
            refreshSkin(true);
        }

        @Override
        public void refreshViewBySkin() {
            refreshSkin(false);
        };
    };
    
    class MyDialog extends Dialog {
        SkinListener skinListener = null;
        
        public MyDialog(Context context) {
            super(context);
        }
        
        public MyDialog(Context context, int theme) {
            super(context, theme);
        }

        public MyDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
            super(context, cancelable, cancelListener);
        }

        public void setSkinListener(SkinListener listener) {
            skinListener = listener;
            SkinManager.registerSkinTop(skinListener);
        }

        @Override
        public void dismiss() {
            SkinManager.unregisterSkin(skinListener);
            super.dismiss();
        }
    }
}
