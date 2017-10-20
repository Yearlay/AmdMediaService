package com.haoke.ui.image;

import haoke.ui.util.HKViewPager;

import java.util.ArrayList;

import com.haoke.bean.FileNode;
import com.haoke.bean.ImageLoad;
import com.haoke.constant.MediaUtil;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.data.AllMediaList;
import com.haoke.data.OperateListener;
import com.haoke.data.PlayStateSharedPreferences;
import com.haoke.define.MediaDef.PlayState;
import com.haoke.mediaservice.R;
import com.haoke.ui.photoview.Media_Photo_View;
import com.haoke.ui.photoview.PhotoViewAttacher.OnMatrixChangedListener;
import com.haoke.ui.photoview.PhotoViewAttacher.OnPhotoTapListener;
import com.haoke.ui.widget.CustomDialog;
import com.haoke.ui.widget.CustomDialog.DIALOG_TYPE;
import com.haoke.util.DebugLog;
import com.haoke.window.HKWindowManager;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import android.support.v4.app.Fragment;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class PhotoPlayFragment extends Fragment implements OnClickListener,
        OnMatrixChangedListener, OnPhotoTapListener, OperateListener, ImageLoadingListener {
    private Context mContext;
    private View mRootView;
    private View mCtrlBar;
    private ImageView mPlayImageView;
    private ImageView mCollectView;
    private TextView mTitleTextView;
    private TextView mUnsupportView;
    private HKViewPager mViewPager;
    private PhotoPagerAdapter mAdapter;
    private Handler mActivityHandler;
    private int mDeviceType;
    private int mLastPosition;
    private boolean mPreFlag;
    
    private ProgressDialog mProgressDialog;
    
    public static int mPlayState;
    
    public void setPlayState(int playState) {
        mPlayState = playState;
        mPreFlag = false;
        if (mRootView != null) {
            updatePlayState(mPlayState);
        }
    }
    
    public void setCurrentPosition(int position) {
        mLastPosition = position;
        PlayStateSharedPreferences.instance(getActivity()).saveImageCurrentPosition(position);
    }
    
    private ArrayList<FileNode> mPhotoList = new ArrayList<FileNode>();
    
    // 更新列表
    public void updataList(ArrayList<FileNode> dataList, int deviceType) {
        mPhotoList.clear();
        mPhotoList.addAll(dataList);
        mDeviceType = deviceType;
        if (mRootView != null) {
            if (mPhotoList.size() == 0) {
                if (mActivityHandler != null) { // 回列表
                    mActivityHandler.sendEmptyMessage(Image_Activity_Main.SWITCH_TO_LIST_FRAGMENT);
                }
            } else {
                mAdapter.notifyDataSetChanged();
                mCollectView.setVisibility(deviceType == DeviceType.COLLECT ? View.GONE : View.VISIBLE);
                // 更新一下位置。
                int position = PlayStateSharedPreferences.instance(getActivity()).getImageCurrentPosition();
                position = position < 0 ? 0 : position;
                position = position >= mPhotoList.size() ? mPhotoList.size() - 1 : position; 
                mViewPager.setCurrentItem(position);
            }
        }
    }
    
    private OnPageChangeListener mPageChangeListener = new OnPageChangeListener() {
        @Override
        public void onPageScrollStateChanged(int state) {
            checkPlayStatus(); // 重新计时
            if (state == 1) {
                restorePhotoView();
            }
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
            checkPlayStatus(); // 重新计时
        }

        @Override
        public void onPageSelected(int position) {
            checkPlayStatus(); // 重新计时
            restorePhotoView();
            FileNode fileNode = mPhotoList.get(position);
            PlayStateSharedPreferences.instance(getActivity()).saveImageCurrentPosition(position);
            mCollectView.setImageResource(fileNode.getCollect() == 1 ?
                    R.drawable.media_collect : R.drawable.media_uncollect);
            if (mDeviceType != DeviceType.COLLECT && mCtrlBar.getVisibility() == View.VISIBLE) {
                mCollectView.setVisibility(View.VISIBLE);
            } else {
                mCollectView.setVisibility(View.GONE);
            }
            mTitleTextView.setText(fileNode.getTitleEx());
            
            if (fileNode.isUnSupportFlag() || fileNode.getFile().length() > 52428800) { // 不支持的图片。
                mCollectView.setVisibility(View.GONE);
                mUnsupportView.setVisibility(View.VISIBLE);
                mHandler.removeMessages(PLAY_ERROR);
                mHandler.sendEmptyMessageDelayed(PLAY_ERROR, 1000);
            } else {
                mUnsupportView.setVisibility(View.GONE);
            }
            mPreFlag = mLastPosition > position;
            mLastPosition = position;
        }
    };
    
    @Override
    public void onAttach(Activity activity) {
        if (activity instanceof Image_Activity_Main) {
            mActivityHandler = ((Image_Activity_Main) activity).getHandler();
        }
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = getActivity();
        mRootView = inflater.inflate(R.layout.image_play_fragment, null);
        mViewPager = (HKViewPager) mRootView.findViewById(R.id.image_play_viewpager);
        mAdapter = new PhotoPagerAdapter();
        mViewPager.setAdapter(mAdapter);

        mCtrlBar = mRootView.findViewById(R.id.image_play_ctrlbar);
        mCtrlBar.findViewById(R.id.image_ctrlbar_list).setOnClickListener(this);
        mCtrlBar.findViewById(R.id.image_ctrlbar_pre).setOnClickListener(this);
        mCtrlBar.findViewById(R.id.image_ctrlbar_next).setOnClickListener(this);
        mCtrlBar.findViewById(R.id.image_ctrlbar_turnr).setOnClickListener(this);
        mPlayImageView = (ImageView) mCtrlBar.findViewById(R.id.image_ctrlbar_pp);
        mPlayImageView.setOnClickListener(this);
        mCollectView = (ImageView) mRootView.findViewById(R.id.collect_image);
        mCollectView.setOnClickListener(this);
        mTitleTextView = (TextView) mRootView.findViewById(R.id.title_image);
        mUnsupportView = (TextView) mRootView.findViewById(R.id.not_support_text);
        
        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        return mRootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        // 更新图像
        if (mPhotoList.size() > 0) {
            int position = PlayStateSharedPreferences.instance(getActivity()).getImageCurrentPosition();
            position = position < 0 ? 0 : position;
            position = position >= mPhotoList.size() ? mPhotoList.size() - 1 : position; 
            mViewPager.setCurrentItem(position);
            PlayStateSharedPreferences.instance(getActivity()).saveImageCurrentPosition(position);
        } else {
            if (mActivityHandler != null) {
                mActivityHandler.sendEmptyMessage(Image_Activity_Main.SWITCH_TO_LIST_FRAGMENT);
            }
        }
        mViewPager.setOnPageChangeListener(mPageChangeListener);
        // 更新播放状态
        updatePlayState(mPlayState);
        // 启动状态栏隐藏计时器
        if (mCtrlBar.getVisibility() == View.VISIBLE) {
            startHideTimer();
        }
        mCollectView.setImageResource(mPhotoList.get(mViewPager.getCurrentItem()).getCollect() == 1 ?
                R.drawable.media_collect : R.drawable.media_uncollect);
        mCollectView.setVisibility(mDeviceType == DeviceType.COLLECT ? View.GONE : View.VISIBLE);
        mTitleTextView.setText(mPhotoList.get(mViewPager.getCurrentItem()).getTitleEx());
    }
    
    @Override
    public void onResume() {
        checkPlayStatus();
        HKWindowManager.fullScreen(getActivity(), true);
        getActivity().getWindow().getDecorView()
                .setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        super.onResume();
    }

    @Override
    public void onPause() {
        mHandler.removeMessages(NEXT_PLAY);
        if (mActivityHandler != null) {
            mActivityHandler.sendEmptyMessage(Image_Activity_Main.SHOW_BOTTOM);
        }
        HKWindowManager.fullScreen(getActivity(), false);
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mViewPager.setOnPageChangeListener(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private void updatePlayState(int playState) {
        checkPlayStatus();
        mPlayImageView.setImageResource(playState == PlayState.PLAY ?
                R.drawable.image_pause_icon_selector : R.drawable.image_play_icon_selector);
    }

    @Override
    public void onClick(View view) {
        stopHideTimer();
        switch (view.getId()) {
        case R.id.image_ctrlbar_list:
            if (mActivityHandler != null) { // 回列表
                mActivityHandler.sendEmptyMessage(Image_Activity_Main.SWITCH_TO_LIST_FRAGMENT);
            }
            break;
        case R.id.image_ctrlbar_pre:
            preImage();
            break;
        case R.id.image_ctrlbar_pp:
            mPlayState = (mPlayState == PlayState.PLAY) ? PlayState.PAUSE : PlayState.PLAY;
            updatePlayState(mPlayState);
            break;
        case R.id.image_ctrlbar_next:
            nextImage();
            break;
        case R.id.image_ctrlbar_turnr:
            View photoView = getCurPhotoView();
            if (photoView != null) {
                float rotation = photoView.getRotation();
                photoView.setRotation(-90f + rotation);
                checkPlayStatus(); // 重新计时
            }
            break;
        case R.id.collect_image:
            collectOrUncollect();
            break;
        case R.id.not_support_text:
        	slaverShow(mCtrlBar.getVisibility() != View.VISIBLE);
        	break;
        }
        startHideTimer();
    }
    
    public void preImage() {
        mHandler.removeMessages(PLAY_ERROR);
        if (mRootView != null) {
            if (getCurPhotoView() != null) {
                int position = PlayStateSharedPreferences.instance(getActivity()).getImageCurrentPosition();
                position--;
                position = position < 0 ? mPhotoList.size() - 1 : position;
                mViewPager.setCurrentItem(position);
                PlayStateSharedPreferences.instance(getActivity()).saveImageCurrentPosition(position);
                checkPlayStatus(); // 重新计时
            }
        }
    }
    
    public void nextImage() {
        mHandler.removeMessages(PLAY_ERROR);
        if (mRootView != null) {
            if (getCurPhotoView() != null) {
                int position = PlayStateSharedPreferences.instance(getActivity()).getImageCurrentPosition();
                position++;
                position = position >= mPhotoList.size() ? 0 : position;
                mViewPager.setCurrentItem(position);
                PlayStateSharedPreferences.instance(getActivity()).saveImageCurrentPosition(position);
                checkPlayStatus(); // 重新计时
            }
        }
    }
    
    private void collectOrUncollect() {
        final FileNode fileNode = mPhotoList.get(mViewPager.getCurrentItem());
        if (!fileNode.isFromCollectTable()) {
            if (fileNode.getCollect() == 0) {
                // 收藏图片。
                mProgressDialog.setTitle("收藏图片");
                mProgressDialog.setProgress(0);
                // mProgressDialog.show();  TODO
                if (AllMediaList.instance(mContext).getCollectSize(FileType.IMAGE) < MediaUtil.COLLECT_COUNT_MAX) {
                    AllMediaList.instance(mContext).collectMediaFile(fileNode, this);
                } else {
                    new AlertDialog.Builder(mContext).setTitle(R.string.collect_limit_dialog_title)
                    .setMessage(R.string.collect_limit_dialog_message)
                    .setPositiveButton(R.string.collect_limit_dialog_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AllMediaList.instance(mContext).deleteOldCollect(FileType.IMAGE);
                            AllMediaList.instance(mContext).collectMediaFile(fileNode, PhotoPlayFragment.this);
                        }
                    })
                    .setNegativeButton(R.string.collect_limit_dialog_cancel, null)
                    .show();
                }
            } else {
                // 取消收藏。
                mProgressDialog.setTitle("取消收藏图片");
                mProgressDialog.setProgress(0);
                // mProgressDialog.show(); TODO
                AllMediaList.instance(mContext).uncollectMediaFile(fileNode, this);
            }
        }
    }
    
    @Override
    public void onOperateCompleted(int operateValue, int progress, int resultCode) {
        if (operateValue == OperateListener.OPERATE_COLLECT) {
            mProgressDialog.setProgress(progress);
            if (resultCode == OperateListener.OPERATE_SUCEESS) {
                if (progress == 100) {
                    mCollectView.setImageResource(R.drawable.media_collect);
                    mProgressDialog.dismiss();
                }
            } else {
                Toast.makeText(mContext, "收藏图片异常", Toast.LENGTH_SHORT).show();
                mProgressDialog.dismiss();
            }
        } else if (operateValue == OperateListener.OPERATE_UNCOLLECT) {
            mProgressDialog.setProgress(progress);
            if (resultCode == OperateListener.OPERATE_SUCEESS) {
                if (progress == 100) {
                    mCollectView.setImageResource(R.drawable.media_uncollect);
                    mProgressDialog.dismiss();
                }
            } else {
                Toast.makeText(mContext, "取消收藏图片异常", Toast.LENGTH_SHORT).show();
                mProgressDialog.dismiss();
            }
        }
    }
    
    // 重置自动播放计时器
    private void checkPlayStatus() {
        if (mPlayState == PlayState.PLAY) {
            mHandler.removeMessages(NEXT_PLAY);
            mHandler.sendEmptyMessageDelayed(NEXT_PLAY, DELAY_TIME);
        } else {
            mHandler.removeMessages(NEXT_PLAY);
        }
    }

    // 启动托盘隐藏计时器
    private void startHideTimer() {
        mHandler.removeMessages(HIDE_CTRL);
        mHandler.sendEmptyMessageDelayed(HIDE_CTRL, DELAY_TIME);
    }

    // 停止托盘隐藏计时器
    private void stopHideTimer() {
        mHandler.removeMessages(HIDE_CTRL);
    }

    // 托盘显示控制
    private void slaverShow(boolean visible) {
        if (visible) {
            mCtrlBar.setVisibility(View.VISIBLE);
            mCollectView.setVisibility(mDeviceType == DeviceType.COLLECT ? View.GONE : View.VISIBLE);
            mTitleTextView.setVisibility(View.VISIBLE);
            startHideTimer();
        } else {
            mCtrlBar.setVisibility(View.GONE);
            mCollectView.setVisibility(View.GONE);
            mTitleTextView.setVisibility(View.GONE);
            stopHideTimer();
        }
    }

    // 获取当前的图片
    private Media_Photo_View getCurPhotoView() {
        Media_Photo_View view = null;
        int total = mPhotoList.size();
        int pos = mViewPager.getCurrentItem();
        if (pos >= 0 && pos < total) {
            view = (Media_Photo_View) mViewPager.findViewWithTag(pos);
        }
        return view;
    }

    // 恢复当前图片属性（比如角度等）
    private void restorePhotoView() {
        Media_Photo_View photoView = getCurPhotoView();
        if (photoView != null) {
            photoView.setRotation(0);
            photoView.zoomTo(1, 0, 0);
        }
    }

    // 图片缩放回调 OnMatrixChangedListener
    @Override
    public void onMatrixChanged(RectF rect) {
        checkPlayStatus(); // 重新计时
    }

    // 图片点击回调 OnPhotoTapListener
    @Override
    public void onPhotoTap(View view, float x, float y) {
        slaverShow(mCtrlBar.getVisibility() != View.VISIBLE);
        checkPlayStatus(); // 重新计时
    }

    private static final int DELAY_TIME = 5000;

    private static final int NEXT_PLAY = 1;
    private static final int HIDE_CTRL = 2;
    private static final int PLAY_ERROR = 3;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case NEXT_PLAY:
                mPreFlag = false;
                int position = PlayStateSharedPreferences.instance(getActivity()).getImageCurrentPosition();
                position++;
                if (position >= mPhotoList.size()) {
                    position = 0; // 循环播放
                }
                mViewPager.setCurrentItem(position);
                PlayStateSharedPreferences.instance(getActivity()).saveImageCurrentPosition(position);
                checkPlayStatus();
                break;
            case HIDE_CTRL:
                slaverShow(false);
                break;
            case PLAY_ERROR:
                if (mPreFlag) {
                    preImage();
                } else {
                    nextImage();
                }
                break;

            default:
                break;
            }
            super.handleMessage(msg);
        }
        
    };
    
    class PhotoPagerAdapter extends PagerAdapter {
        @Override
        public int getCount() {
            return mPhotoList.size();
        }
        @Override
        public boolean isViewFromObject(View view, Object obj) {
            return view == obj;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            FileNode fileNode = mPhotoList.get(position);
            
            View view = LayoutInflater.from(mContext).inflate(R.layout.image_photopager_item, null);
            Media_Photo_View photoView = (Media_Photo_View) view.findViewById(R.id.photopager_imageview);
            photoView.setOnMatrixChangeListener(PhotoPlayFragment.this);
            photoView.setOnPhotoTapListener(PhotoPlayFragment.this);
            if (fileNode.isUnSupportFlag() || fileNode.getFile().length() > 52428800) {
                int currentPos = PlayStateSharedPreferences.instance(mContext).getImageCurrentPosition();
                if (position == currentPos) {
                    mUnsupportView.setVisibility(View.VISIBLE);
                    mHandler.removeMessages(PLAY_ERROR);
                    mHandler.sendEmptyMessageDelayed(PLAY_ERROR, 1000);
                }
            } else {
                ImageLoad.instance(mContext).loadImageBitmap(photoView, R.drawable.image_icon_default,
                        fileNode, PhotoPlayFragment.this);
            }
            photoView.setTag(position);
            container.addView(view, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View view = (View) object;
            container.removeView(view);
        }
    }
    
    @Override
    public void onLoadingCancelled(String arg0, View arg1) {
    }

    @Override
    public void onLoadingComplete(String arg0, View arg1, Bitmap arg2) {
    }

    @Override
    public void onLoadingFailed(String uri, View view, FailReason reason) {
        FileNode failedFileNode = null;
        int failedPosition = -1;
        for (int index = 0; index < mPhotoList.size(); index++) {
        	FileNode fileNode = mPhotoList.get(index);
            if (uri != null && uri.equals("file://" + fileNode.getFilePath())) {
                failedFileNode = fileNode;
                failedPosition = index;
                break;
            }
        }
        if (failedFileNode != null) {
            failedFileNode.setUnSupportFlag(true);
            try {
                if (failedPosition == mViewPager.getCurrentItem()) {
                    mCollectView.setVisibility(View.GONE);
                    mUnsupportView.setVisibility(View.VISIBLE);
                    mHandler.removeMessages(PLAY_ERROR);
                    mHandler.sendEmptyMessageDelayed(PLAY_ERROR, 1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onLoadingStarted(String arg0, View arg1) {
    }
}
