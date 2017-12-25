package com.haoke.ui.widget;

import java.io.File;
import java.util.ArrayList;

import com.amd.media.MediaInterfaceUtil;
import com.amd.util.SkinManager;
import com.haoke.bean.FileNode;
import com.haoke.constant.MediaUtil;
import com.haoke.mediaservice.R;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class CustomDialog implements OnClickListener, OnDismissListener {
    private static final String TAG = "CustomDialog";
    private Context mContext;
	private View mRootView;
	private Button mOkButton;
	private Button mCancelButton;
	private TextView mTextView;
	private TextView mTitleTextView;
	private LinearLayout mLinearLayout;
	
	// 对话框类型枚举
	public enum DIALOG_TYPE {
		NULL,
		NONE_BTN,      // 无按钮：过1秒的时间，自动隐藏。
		ONE_BTN,       // 单按钮：Message提示，确认按钮。
		TWO_BTN_MSG,   // 双按钮：Message提示，确认按钮，取消按钮。
		TWO_BTN_COVER  // 双按钮：覆盖列表展示，确认按钮，取消按钮。
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
	
	private void refreshSkin() {
	    if (mContext != null) {
	        SkinManager skinManager = SkinManager.instance(mContext);
	        if (mRootView != null) {
	            mRootView.setBackgroundDrawable(skinManager.getDrawable(R.drawable.pub_msgbox_bg1));
	        }
	        if (mOkButton != null) {
	            mOkButton.setBackgroundDrawable(skinManager.getDrawable(R.drawable.bd_dialog_button));
	        }
	        if (mCancelButton != null) {
	            mCancelButton.setBackgroundDrawable(skinManager.getDrawable(R.drawable.bd_dialog_button));
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
	
	public void ShowDialog(Context context, DIALOG_TYPE type, int messageId) {
	    CloseDialog();
		mContext = context;
		mDialog = new MyDialog(context, R.style.pub_dialog);
		mDialog.setOnDismissListener(this);
		mDialog.setContentObserver(mContentObserver);
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
		refreshSkin();
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
	                    CloseDialog();
                    } catch (Exception e) {
                        Log.e(TAG, "NONE_BTN DISMISS" + e.toString());
                    }
					super.handleMessage(msg);
				}
				
			};
			handler.sendEmptyMessageDelayed(0, 1000);
		}
	}
	
	public void showProgressDialog(Context context, int titleID, OnDismissListener listener) {
	    CloseDialog();
		mContext = context;
		mDialog = new MyDialog(context, R.style.pub_dialog);
		mDialog.setContentObserver(mContentObserver);
		mOtherOnDismissListener = listener;
		mDialog.setOnDismissListener(this);
		mDialog.setContentView(R.layout.custom_dialog_progress);
		mRootView = mDialog.findViewById(R.id.pub_dialog_layout);
		refreshSkin();
		mTitleTextView = (TextView) mDialog.findViewById(R.id.pub_dialog_title);
		mTitleTextView.setText(titleID);
		mTextView = (TextView) mDialog.findViewById(R.id.pub_dialog_text);
		mTextView.setText("0%");
		try {
			mDialog.setCanceledOnTouchOutside(false);
			mDialog.show();
		} catch (Exception e) {
		}
	}
	
	public void updateProgressValue(int value) {
		if (mDialog != null && mTextView != null) {
			mTextView.setText(value + "%");
		}
	}
	
	public void showCoverDialog(Context context, ArrayList<FileNode> dataList) {
	    CloseDialog();
		mContext = context;
		mDialog = new MyDialog(context, R.style.pub_dialog);
		mDialog.setContentObserver(mContentObserver);
		mDialog.setOnDismissListener(this);
		mDialog.setContentView(R.layout.custom_dialog_two_cover);
		// 初始化界面
		mRootView = mDialog.findViewById(R.id.pub_dialog_layout);
		mOkButton = (Button) mDialog.findViewById(R.id.pub_dialog_ok);
		mCancelButton = (Button) mDialog.findViewById(R.id.pub_dialog_cancel);
		refreshSkin();
		setDialogClickListener(mDialog.findViewById(R.id.pub_dialog_ok), this);
		setDialogClickListener(mDialog.findViewById(R.id.pub_dialog_cancel), this);
		mDialog.findViewById(R.id.pub_dialog_layout)
				.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
		mTitleTextView = (TextView) mDialog.findViewById(R.id.pub_dialog_title);
		mTitleTextView.setText(R.string.cover_dialog_title);
		
		mLinearLayout = (LinearLayout) mDialog.findViewById(R.id.cover_linearlayout);
        for (final FileNode fileNode : dataList) {
            String destFilePath = MediaUtil.LOCAL_COPY_DIR + "/" +
                    fileNode.getFilePath().substring(fileNode.getFilePath().lastIndexOf('/') + 1);
            File destFile = new File(destFilePath);
            if (destFile.exists() && fileNode.isSelected()) {
                AmdCheckBox checkBox = new AmdCheckBox(context);
                checkBox.setText(fileNode.getFileName());
                checkBox.setChecked(true);
                checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        fileNode.setSelected(isChecked);
                    }
                });
                mLinearLayout.addView(checkBox);
            }
        }
        
		// 显示对话框
		try {
			if (mLinearLayout.getChildCount() > 0) {
				mDialog.setCanceledOnTouchOutside(true);
				mDialog.show();
			} else {
				if (mDialogListener != null) {
					mDialogListener.OnDialogEvent(R.id.pub_dialog_ok);
				}
			}
		} catch (Exception e) {
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
	
	private ContentObserver mContentObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            refreshSkin();
        };
    };
    
    class MyDialog extends Dialog {
        ContentObserver contentObserver = null;
        
        public MyDialog(Context context) {
            super(context);
        }
        
        public MyDialog(Context context, int theme) {
            super(context, theme);
        }

        public MyDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
            super(context, cancelable, cancelListener);
        }

        public void setContentObserver(ContentObserver cObserver) {
            contentObserver = cObserver;
            getContext().getContentResolver().registerContentObserver(MediaInterfaceUtil.URI_SKIN, false, contentObserver);
        }

        @Override
        public void dismiss() {
            getContext().getContentResolver().unregisterContentObserver(contentObserver);
            super.dismiss();
        }
        
    }
}
