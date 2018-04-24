package com.haoke.ui.widget;

import java.io.File;
import java.util.ArrayList;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.amd.util.SkinManager;
import com.amd.util.SkinManager.SkinListener;
import com.haoke.bean.FileNode;
import com.haoke.constant.MediaUtil;
import com.haoke.mediaservice.R;
import com.haoke.util.DebugLog;

public class CopyDialog implements OnClickListener {
    private static final String TAG = "CopyDialog";
    private Context mContext;
    private View mRootView;
    
    private LinearLayout mCheckLayout; // 拷贝检查layout
    
    private LinearLayout mCoverLayout; // 拷贝覆盖layout
    private ListView mCoverList;
    private CheckAdapter mCheckAdapter;
    private Button mOkButton;
    private Button mCancelButton;
    
    private LinearLayout mProgressLayout; // 拷贝进度layout
    private TextView mTextView;
    
    private Drawable mRootViewDrawable;
    private Drawable mOkButtonDrawable;
    private Drawable mCancelButtonDrawable;
    private ColorStateList mTextColorStateList;
    private int mButtonColor;
    
    private ArrayList<FileNode> mCopyDataList = new ArrayList<>();
    private ArrayList<FileNode> mCoverDataList = new ArrayList<>();
    
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
        mCoverList = (ListView) mDialog.findViewById(R.id.cover_list);
        
        mCopyDataList.clear();
        if (dataList != null) {
            for (FileNode fileNode : dataList) {
                if (fileNode.isSelected()) {
                    mCopyDataList.add(fileNode);
                }
            }
        }
        DebugLog.v(TAG, "showCopyDialog --> mDataList.size() = "+ mCopyDataList.size());
//        mDataList = dataList;
        
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
    private static final int PROGRESS_SHOW_WITHOUT_COVER = 5;
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
                //modify bug 21351 begin
                case PROGRESS_SHOW_WITHOUT_COVER:
                    if (mDialogListener != null) {
                        mDialogListener.OnDialogEvent(R.id.copy_ok);
                    }
                    break;
                //modify bug 21351 end

                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };
    
    private Thread mCheckThread;
    
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
        
        mCheckThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mCoverDataList.clear();
                for (FileNode fileNode : mCopyDataList) {
                    if (mCheckThread.isInterrupted()) {
                        break;
                    }
                    String destFilePath = MediaUtil.LOCAL_COPY_DIR + "/" +
                            fileNode.getFilePath().substring(fileNode.getFilePath().lastIndexOf('/') + 1);
                    File destFile = new File(destFilePath);
                    if (destFile.exists() && fileNode.isSelected()) {
                        mCoverDataList.add(fileNode);
                    }
                    fileNode.setCopyDestExist(destFile.exists());
                }
                //modify bug 21351 begin
                DebugLog.v(TAG, "checkShow --> list.size() = "+ mCoverDataList.size());
                if (mCoverDataList.size() > 0) {
                    mHandler.sendEmptyMessage(CHECK_RESULT);
                } else {
                    mHandler.sendEmptyMessage(PROGRESS_SHOW_WITHOUT_COVER);
                }
                //modify bug 21351 end
            }
        });
        mCheckThread.setName("checkThread");
        mCheckThread.start();
    }
    
    public void interruptCheckOperator() {
        if (mCheckThread != null && mCopyState == CHECK_SHOW) {
            mCheckThread.interrupt();
        }
    }
    
    private void checkResult() {
        mCopyState = CHECK_RESULT;
        mCheckAdapter = new CheckAdapter();
        mCoverList.setAdapter(mCheckAdapter);
        DebugLog.v(TAG, "checkResult --> checkAdapter.getCount() = "+ mCheckAdapter.getCount());
        if (mCheckAdapter.getCount() > 0) {
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
        DebugLog.v(TAG, "progressShow --> mCopyState =" + mCopyState);
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
        progressShow(value);
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
                mTextColorStateList = skinManager.getColorStateList(R.drawable.text_color_selector);
                mButtonColor = skinManager.getColor(R.color.bd_text_down);
            }
            if (!loading) {
                if (mRootView != null) {
                    mRootView.setBackgroundDrawable(mRootViewDrawable);
                }
                if (mOkButton != null) {
                    mOkButton.setBackgroundDrawable(mOkButtonDrawable);
                    mOkButton.setTextColor(mButtonColor);
                }
                if (mCancelButton != null) {
                    mCancelButton.setBackgroundDrawable(mCancelButtonDrawable);
                    mCancelButton.setTextColor(mButtonColor);
                }
                if (mCheckAdapter != null) {
                    mCheckAdapter.notifyDataSetChanged();
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
    
    class CheckAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mCoverDataList == null ? 0 : mCoverDataList.size();
        }

        @Override
        public FileNode getItem(int position) {
            return mCoverDataList == null ? null : mCoverDataList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AmdCheckBox amdCheckBox = null;
            if (convertView != null && convertView instanceof AmdCheckBox) {
                amdCheckBox = (AmdCheckBox) convertView;
            } else {
                amdCheckBox = new AmdCheckBox(mContext);
            }
            amdCheckBox.setTextColor(mTextColorStateList);
            final FileNode fileNode = mCoverDataList.get(position);
            amdCheckBox.setFileNode(fileNode);
            return amdCheckBox;
        }
        
    }
}
