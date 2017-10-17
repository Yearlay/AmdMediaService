package com.haoke.ui.music;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.haoke.define.MediaDef.DeviceType;
import com.haoke.mediaservice.R;

public class Music_List_Tab extends RelativeLayout implements OnClickListener {
	
	//Tab右侧按钮控制
	private RelativeLayout mListTabLayout = null;
	private RelativeLayout mLoadLayout = null;
	private RelativeLayout mEditTabLayout = null;
	
	private TextView mAllButton = null;//全选
	private TextView mCancleButton = null;//取消选择
	private TextView mDelectButton = null;//删除
	private TextView mCopyButton = null;//拷贝到本地
	
	private ImageView mDeviceImageView = null;
	private TextView mDeviceTextView = null;
	
	private ImageView mLoadingImageView = null;
	
	private OnClickListener mClickListener = null;
	private Context mContext = null;
	
	private Animation mAnim = null;
	LinearInterpolator mInterpolator = null;

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
		mListTabLayout = (RelativeLayout) this.findViewById(R.id.music_tab_list_id);
		mLoadLayout = (RelativeLayout) this.findViewById(R.id.music_tab_loading_id);
		mEditTabLayout = (RelativeLayout) this.findViewById(R.id.music_tab_edit_id);
		mListTabLayout.setVisibility(View.VISIBLE);
		mLoadLayout.setVisibility(View.GONE);
		
		mDeviceImageView = (ImageView) this.findViewById(R.id.music_device_image);
		mDeviceTextView = (TextView) this.findViewById(R.id.music_device_text);
		
		mAllButton = (TextView) findViewById(R.id.music_edit_all);
		mCancleButton = (TextView) findViewById(R.id.music_edit_cancle);
		mDelectButton = (TextView) findViewById(R.id.music_edit_delect);
		mCopyButton = (TextView) findViewById(R.id.copy_to_local);
		mAllButton.setOnClickListener(this);
		mCancleButton.setOnClickListener(this);
		mDelectButton.setOnClickListener(this);
		mCopyButton.setOnClickListener(this);
		
		mLoadingImageView = (ImageView) this.findViewById(R.id.image_view_loading);
		
		mListTabLayout.setOnClickListener(this);
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
		if (mLoadLayout.getVisibility() != View.VISIBLE) {
			mLoadLayout.setVisibility(View.VISIBLE);
		}
//		mAnim = AnimationUtils.loadAnimation(mContext, R.anim.list_loading_animator);
//		mInterpolator = new LinearInterpolator();
//		mAnim.setInterpolator(mInterpolator);
//		mLoadingImageView.startAnimation(mAnim);
	}
	
	public void stopTabAnimator() {
//		if (mLoadingImageView.getAnimation() != null){
//			mAnim.cancel();
//			mLoadingImageView.clearAnimation();
//		}
		if (mLoadLayout.getVisibility() != View.GONE) {
			mLoadLayout.setVisibility(View.GONE);
		}
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
