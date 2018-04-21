package com.haoke.ui.widget;

import com.amd.util.SkinManager;
import com.amd.util.SkinManager.SkinListener;
import com.haoke.data.AllMediaList;
import com.haoke.mediaservice.R;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

public class DeleteProgressDialog implements OnDismissListener, OnCancelListener {
    private static final String TAG = "DeleteProgressDialog";
    private View mRootView;
    private TextView mTextView;
    private TextView mTitleTextView;
    private Drawable mRootViewDrawable;
    
    private MyDialog mDialog;
    
    public void showProgressDialog(Context context, int titleID) {
        mDialog = new MyDialog(context, R.style.pub_dialog);
        mDialog.setSkinListener(mSkinListener);
        mDialog.setCanceledOnTouchOutside(true);
        mDialog.setOnDismissListener(this);
        mDialog.setOnCancelListener(this);
        mDialog.setContentView(R.layout.custom_dialog_progress);
        mRootView = mDialog.findViewById(R.id.pub_dialog_layout);
        refreshSkin(true);
        refreshSkin(false);
        mTitleTextView = (TextView) mDialog.findViewById(R.id.pub_dialog_title);
        mTitleTextView.setText(titleID);
        mTextView = (TextView) mDialog.findViewById(R.id.pub_dialog_text);
        mTextView.setText("0%");
        try {
            mDialog.show();
        } catch (Exception e) {
        }
    }
    
    public void updateProgressValue(int value) {
        if (mDialog != null && mTextView != null) {
            mTextView.setText(value + "%");
        }
    }
    
    @Override
    public void onDismiss(DialogInterface dialog) {
        AllMediaList.instance().stopOperateThread();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        AllMediaList.instance().stopOperateThread();
    }
    
    public void closeDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
    }
    
    private void refreshSkin(boolean loading) {
        SkinManager skinManager = SkinManager.instance();
        if (loading || mRootViewDrawable==null) {
            mRootViewDrawable = skinManager.getDrawable(R.drawable.pub_msgbox_bg1);
        }
        if (!loading) {
            if (mRootView != null) {
                mRootView.setBackgroundDrawable(mRootViewDrawable);
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
