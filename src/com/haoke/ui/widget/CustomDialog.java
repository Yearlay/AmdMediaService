package com.haoke.ui.widget;

import com.amd.util.SkinManager;
import com.amd.util.SkinManager.SkinListener;
import com.haoke.mediaservice.R;
import com.haoke.util.DebugLog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CustomDialog implements OnClickListener, OnDismissListener {
    private static final String TAG = "CustomDialog";
    private Context mContext;
	private View mRootView;
	private Button mOkButton;
	private Button mCancelButton;
	private TextView mTextView;
	private TextView mTitleTextView;
	private LinearLayout mLinearLayout;
	
	private Drawable mRootViewDrawable;
	private Drawable mOkButtonDrawable;
	private Drawable mCancelButtonDrawable;
	private int mButtonColor;
	
	// 对话框类型枚举
	public enum DIALOG_TYPE {
		NULL,
		NONE_BTN,      // 无按钮：过1秒的时间，自动隐藏。
		ONE_BTN,       // 单按钮：Message提示，确认按钮。
		TWO_BTN_MSG,   // 双按钮：Message提示，确认按钮，取消按钮。
	}
	
	public interface OnDialogListener {
		abstract void OnDialogEvent(int id);
		abstract void OnDialogDismiss();
	}
	private OnDialogListener mDialogListener;
	private OnDismissListener mOtherOnDismissListener;
	public void SetDialogListener(OnDialogListener listener) {
		mDialogListener = listener;
	}

	private MyDialog mDialog;
	public Dialog getDialog() {
		return mDialog;
	}
	
	private void refreshSkin(boolean loading) {
	    if (mContext != null) {
	        SkinManager skinManager = SkinManager.instance(mContext);
	        if (loading || mRootViewDrawable==null) {
                mRootViewDrawable = skinManager.getDrawable(R.drawable.pub_msgbox_bg1);
                mOkButtonDrawable = skinManager.getDrawable(R.drawable.bd_dialog_button);
                mCancelButtonDrawable = skinManager.getDrawable(R.drawable.bd_dialog_button);
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
	            if (mLinearLayout != null && mLinearLayout.getChildCount() > 0) {
	                for (int index = 0; index < mLinearLayout.getChildCount(); index++) {
	                    View view = mLinearLayout.getChildAt(index);
	                    if (view instanceof AmdCheckBox) {
	                        AmdCheckBox amdCheckBox = ((AmdCheckBox) view);
	                        amdCheckBox.setChecked(amdCheckBox.isChecked());
	                    }
	                }
	            }
	        }
	    }
	}
	
	public void ShowDialog(Context context, DIALOG_TYPE type, int messageId) {
	    CloseDialog();
		mContext = context;
		mDialog = new MyDialog(context, R.style.pub_dialog);
		mDialog.setOnDismissListener(this);
		//mDialog.setContentObserver(mContentObserver);
		mDialog.setSkinListener(mSkinListener);
		switch (type) {
			case NONE_BTN:
				mDialog.setContentView(R.layout.custom_dialog_none);
				break;
			case ONE_BTN:
				mDialog.setContentView(R.layout.custom_dialog_one);
				break;
			case TWO_BTN_MSG:
				mDialog.setContentView(R.layout.custom_dialog_two);
				break;
			default:
				break;
		}
		mRootView = mDialog.findViewById(R.id.pub_dialog_layout);
		mOkButton = (Button) mDialog.findViewById(R.id.pub_dialog_ok);
		mCancelButton = (Button) mDialog.findViewById(R.id.pub_dialog_cancel);
		refreshSkin(true);
		refreshSkin(false);
		setDialogClickListener(mOkButton, this);
		setDialogClickListener(mCancelButton, this);
		mDialog.findViewById(R.id.pub_dialog_layout)
				.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
		
		mTitleTextView = (TextView) mDialog.findViewById(R.id.pub_dialog_title);
		int titleId = 0; // TODO
		if (mTitleTextView != null && titleId != 0) {
			mTitleTextView.setText(titleId);
		}
		mTextView = (TextView) mDialog.findViewById(R.id.pub_dialog_text);
		if (mTextView != null) {
			if (messageId != 0) {
				mTextView.setText(messageId);
			}
		}
		
		// 显示对话框
		try {
			mDialog.setCanceledOnTouchOutside(true);
			mDialog.show();
		} catch (Exception e) {
		}
		
		if (type == DIALOG_TYPE.NONE_BTN) {
			Handler handler = new Handler() {
				@Override
				public void handleMessage(Message msg) {
				    try {
				        Object obj = msg.obj;
				        if (obj instanceof MyDialog) {
				            MyDialog dialog = (MyDialog)obj;
				            if (mDialog.equals(dialog)) {
		                        CloseDialog();
				            }
				        }
                    } catch (Exception e) {
                        DebugLog.e(TAG, "NONE_BTN DISMISS" + e.toString());
                    }
					super.handleMessage(msg);
				}
				
			};
			Message msg = handler.obtainMessage(0, mDialog);
            handler.sendMessageDelayed(msg, 1000);
			//handler.sendEmptyMessageDelayed(0, 1000);
		}
	}
	
	private void setDialogClickListener(View view, OnClickListener listener) {
		if (view != null && view instanceof Button) {
			((Button) view).setOnClickListener(listener);
		}
	}

	@Override
	public void onClick(View view) {
		if (mDialogListener != null) {
			mDialogListener.OnDialogEvent(view.getId());
		}
		CloseDialog();
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		if (mDialogListener != null) {
			mDialogListener.OnDialogDismiss();
		}
		if (mOtherOnDismissListener != null) {
		    mOtherOnDismissListener.onDismiss(dialog);
		}
	}
	
	public void CloseDialog() {
		if (mDialog != null) {
		    mDialog.dismiss();
		}
	}
	
	public void CloseDialogEx() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
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
            //getContext().getContentResolver().registerContentObserver(MediaInterfaceUtil.URI_SKIN, false, contentObserver);
            SkinManager.registerSkinTop(skinListener);
        }

        @Override
        public void dismiss() {
            //getContext().getContentResolver().unregisterContentObserver(contentObserver);
            SkinManager.unregisterSkin(skinListener);
            super.dismiss();
        }
        
    }
}
