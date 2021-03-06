package com.haoke.ui.music;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amd.util.SkinManager;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.mediaservice.R;

public class Music_List_Tab extends RelativeLayout implements OnClickListener {
	private boolean initViewFlag;
	//Tab右侧按钮控制
	private RelativeLayout mListTabLayout = null;
	private RelativeLayout mEditTabLayout = null;
	
	private ImageView mEditIcon;
	private TextView mEditTextView;
	
	private TextView mAllButton = null;//全选
	private TextView mCancleButton = null;//取消选择
	private TextView mDelectButton = null;//删除
	private TextView mCopyButton = null;//拷贝到本地
	
	private ImageView mDeviceImageView = null;
	private TextView mDeviceTextView = null;
	
	private OnClickListener mClickListener = null;
	private Context mContext = null;
	
	private Animation mAnim = null;
	LinearInterpolator mInterpolator = null;

	private Drawable mEditIconDrawable;
	private ColorStateList mTextColorStateList;

	public Music_List_Tab(Context context) {
		this(context, null);
	}

	public Music_List_Tab(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public Music_List_Tab(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	public void setOnTabClickListener(OnClickListener listener) {
		mClickListener = listener;
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		initView();
	}
	
	private void initView() {
		mListTabLayout = (RelativeLayout) findViewById(R.id.music_tab_list_id);
		mEditTabLayout = (RelativeLayout) findViewById(R.id.music_tab_edit_id);
		mListTabLayout.setVisibility(View.VISIBLE);
		
		mDeviceImageView = (ImageView) findViewById(R.id.music_device_image);
		mDeviceTextView = (TextView) findViewById(R.id.music_device_text);
		
		mAllButton = (TextView) findViewById(R.id.music_edit_all);
		mCancleButton = (TextView) findViewById(R.id.music_edit_cancle);
		mDelectButton = (TextView) findViewById(R.id.music_edit_delect);
		mCopyButton = (TextView) findViewById(R.id.copy_to_local);
		mAllButton.setOnClickListener(this);
		mCancleButton.setOnClickListener(this);
		mDelectButton.setOnClickListener(this);
		mCopyButton.setOnClickListener(this);
		
		mListTabLayout.setOnClickListener(this);
		mEditIcon = (ImageView) findViewById(R.id.image_view_edit_id);
		mEditTextView = (TextView) findViewById(R.id.music_list_edit);
		initViewFlag = true;
	}
	
	public void refreshSkin(boolean loading, SkinManager skinManager) {
	    if (loading || mEditIconDrawable==null) {
	        mEditIconDrawable = skinManager.getDrawable(R.drawable.music_date_edit);
	        mTextColorStateList = skinManager.getColorStateList(R.drawable.text_color_selector);
	    }
		if (initViewFlag && !loading) {
			mEditIcon.setImageDrawable(mEditIconDrawable);
			mEditTextView.setTextColor(mTextColorStateList);
			mAllButton.setTextColor(mTextColorStateList);
			mCancleButton.setTextColor(mTextColorStateList);
			mDelectButton.setTextColor(mTextColorStateList);
			mCopyButton.setTextColor(mTextColorStateList);
		}
	}
	
	public void setLeftName(int type) {
		if (type == DeviceType.USB2 || type == DeviceType.USB1) {
			mDeviceImageView.setImageResource(R.drawable.music_icon_usb);
			mDeviceTextView.setText((type == DeviceType.USB1) ? R.string.music_device_usb1 : R.string.music_device_usb2);
			mCopyButton.setVisibility(View.VISIBLE);
		} else if (type == DeviceType.FLASH) {
			mDeviceImageView.setImageResource(R.drawable.music_local_small);
			mDeviceTextView.setText(R.string.music_local);
			mCopyButton.setVisibility(View.GONE);
		} else {
			mDeviceImageView.setImageResource(R.drawable.love_grey);
			mDeviceTextView.setText(R.string.music_save);
			mCopyButton.setVisibility(View.GONE);
		}
	}
	
	public void updateListTab(){
		mListTabLayout.setVisibility(View.GONE);
		mEditTabLayout.setVisibility(View.VISIBLE);
	}
	
	public void updateEditTab() {

		mListTabLayout.setVisibility(View.VISIBLE);
		mEditTabLayout.setVisibility(View.GONE);
	}
	
	public void updateBtndate(boolean isSelect) {
		if (isSelect) {
			mAllButton.setText(R.string.music_choose_remove);
		}else{
			mAllButton.setText(R.string.music_choose_all);
		}
	}
	
	public void startTabAnimator(Context context) {
		mContext = context;
		if (mListTabLayout.getVisibility() != View.GONE) {
			mListTabLayout.setVisibility(View.GONE);
		}
		if (mEditTabLayout.getVisibility() != View.GONE) {
			mEditTabLayout.setVisibility(View.GONE);
		}
	}
	
	public void stopTabAnimator() {
		if (mEditTabLayout.getVisibility() != View.GONE) {
			mEditTabLayout.setVisibility(View.GONE);
		}
		if (mListTabLayout.getVisibility() != View.VISIBLE) {
			mListTabLayout.setVisibility(View.VISIBLE);
		}
	}
	
	@Override
	public void onClick(View v) {
		if (mClickListener != null) {
			mClickListener.onClick(v);
		}
	}
}
