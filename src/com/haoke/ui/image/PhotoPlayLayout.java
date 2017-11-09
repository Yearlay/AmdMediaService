package com.haoke.ui.image;

import haoke.ui.util.HKViewPager;

import java.util.ArrayList;

import com.archermind.skinlib.SkinManager;
import com.haoke.bean.FileNode;
import com.haoke.bean.ImageLoad;
import com.haoke.constant.MediaUtil;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.data.AllMediaList;
import com.haoke.data.OperateListener;
import com.haoke.define.MediaDef.PlayState;
import com.haoke.mediaservice.R;
import com.haoke.ui.photoview.Media_Photo_View;
import com.haoke.ui.photoview.PhotoViewAttacher.OnMatrixChangedListener;
import com.haoke.ui.photoview.PhotoViewAttacher.OnPhotoTapListener;
import com.haoke.util.DebugLog;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class PhotoPlayLayout extends RelativeLayout implements OnClickListener,
        OnMatrixChangedListener, OnPhotoTapListener, OperateListener, ImageLoadingListener {
    public PhotoPlayLayout(Context context) {
        super(context);
    }

    public PhotoPlayLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PhotoPlayLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private Context mContext;
    private View mCtrlBar;
    private ImageView mBackImageView;
    private ImageView mPreImageView; 
    private ImageView mNextImageView;
    private ImageView mTurnImageView;
    private ImageView mPlayImageView;
    private ImageView mCollectView;
    private TextView mTitleTextView;
    private TextView mUnsupportView;
    private HKViewPager mViewPager;
    private PhotoPagerAdapter mAdapter;
    private Handler mActivityHandler;
    private int mDeviceType;
    private int mCurPosition;
    private int mLastPosition;
    private boolean mPreFlag;
    
    private ProgressDialog mProgressDialog;
    
    public static int mPlayState;
    
    private SkinManager skinManager;
    
    public void setPlayState(int playState) {
        mPlayState = playState;
        mPreFlag = false;
        if (mViewPager != null) {
            updatePlayState(mPlayState);
        }
    }
    
    public void setCurrentPosition(int position) {
        mLastPosition = mCurPosition;
        mCurPosition = position;
    }
    
    private ArrayList<FileNode> mPhotoList = new ArrayList<FileNode>();
    
    // 更新列表
    public void updateList(ArrayList<FileNode> dataList, int deviceType) {
        mPhotoList = dataList;
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
        mDeviceType = deviceType;
        if (mViewPager != null) {
            if (dataList.size() == 0) {
                if (mActivityHandler != null) { // 回列表
                    mActivityHandler.sendEmptyMessage(Image_Activity_Main.SWITCH_TO_LIST_FRAGMENT);
                }
            } else {
                mAdapter = new PhotoPagerAdapter();
                mViewPager.setAdapter(mAdapter);
                // 更新一下位置。
                mCurPosition = mCurPosition < 0 ? 0 : mCurPosition;
                mCurPosition = mCurPosition >= mPhotoList.size() ? mPhotoList.size() - 1 : mCurPosition;
                mViewPager.setCurrentItem(mCurPosition, false);
            }
        }
    }
    
    public void setActivityHandler(Handler handler, Context context) {
        mActivityHandler = handler;
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mViewPager = (HKViewPager) findViewById(R.id.image_play_viewpager);

        mCtrlBar = findViewById(R.id.image_play_ctrlbar);
        mBackImageView = (ImageView) mCtrlBar.findViewById(R.id.image_ctrlbar_list);
        mBackImageView.setOnClickListener(this);
        mPreImageView = (ImageView) mCtrlBar.findViewById(R.id.image_ctrlbar_pre);
        mPreImageView.setOnClickListener(this);
        mNextImageView = (ImageView) mCtrlBar.findViewById(R.id.image_ctrlbar_next);
        mNextImageView.setOnClickListener(this);
        mTurnImageView = (ImageView) mCtrlBar.findViewById(R.id.image_ctrlbar_turnr);
        mTurnImageView.setOnClickListener(this);
        mPlayImageView = (ImageView) mCtrlBar.findViewById(R.id.image_ctrlbar_pp);
        mPlayImageView.setOnClickListener(this);
        mCollectView = (ImageView) findViewById(R.id.collect_image);
        mCollectView.setOnClickListener(this);
        mTitleTextView = (TextView) findViewById(R.id.title_image);
        mUnsupportView = (TextView) findViewById(R.id.not_support_text);
        
        mProgressDialog = new ProgressDialog(getContext());
        mProgressDialog.setCancelable(false);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        
        skinManager = SkinManager.instance(getContext());
    }

    public void onResume() {
        DebugLog.d("Yearlay", "onResume mCurPosition: " + mCurPosition);
        refreshSkin();
        if (mPhotoList.size() > 0) {
            mCurPosition = mCurPosition < 0 ? 0 : mCurPosition;
            mCurPosition = mCurPosition >= mPhotoList.size() ? mPhotoList.size() - 1 : mCurPosition; 
            mViewPager.setCurrentItem(mCurPosition, false);
            DebugLog.d("Yearlay", "onResume setCurrentItem mCurPosition: " + mCurPosition);
        } else {
            if (mActivityHandler != null) {
                mActivityHandler.sendEmptyMessage(Image_Activity_Main.SWITCH_TO_LIST_FRAGMENT);
            }
        }
        // 更新播放状态
        // 启动状态栏隐藏计时器
        if (mCtrlBar.getVisibility() == View.VISIBLE) {
            startHideTimer();
        }
        updateCollectView();
        mTitleTextView.setText(mPhotoList.get(mViewPager.getCurrentItem()).getTitleEx());
        updatePlayState(mPlayState);
        FileNode fileNode = mPhotoList.get(mCurPosition);
        if (fileNode.isUnSupportFlag() || fileNode.getFile().length() > 52428800) { // 不支持的图片。
            mCollectView.setVisibility(View.GONE);
            mUnsupportView.setVisibility(View.VISIBLE);
            mHandler.removeMessages(PLAY_ERROR);
            mHandler.sendEmptyMessageDelayed(PLAY_ERROR, 1000);
        } else {
            mUnsupportView.setVisibility(View.GONE);
        }
        
        mViewPager.setOnPageChangeListener(mPageChangeListener);
        mViewPager.setOnTouchListener(mTouchListener);
    }
    
    private void refreshSkin() {
        mBackImageView.setImageDrawable(skinManager.getStateListDrawable(R.drawable.image_back_icon_selector));
        mPreImageView.setImageDrawable(skinManager.getStateListDrawable(R.drawable.image_pre_icon_selector));
        mNextImageView.setImageDrawable(skinManager.getStateListDrawable(R.drawable.image_next_icon_selector));
        mTurnImageView.setImageDrawable(skinManager.getStateListDrawable(R.drawable.image_turn_icon_selector));
    }

    public void onPause() {
        mHandler.removeMessages(NEXT_PLAY);
        if (mActivityHandler != null) {
            mActivityHandler.sendEmptyMessage(Image_Activity_Main.SHOW_BOTTOM);
        }
        mViewPager.setOnPageChangeListener(null);
    }

    private void updatePlayState(int playState) {
        checkPlayStatus();
        mPlayImageView.setImageDrawable(skinManager.getStateListDrawable(playState == PlayState.PLAY ?
                R.drawable.image_pause_icon_selector : R.drawable.image_play_icon_selector));
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
        if (mViewPager != null) {
            if (getCurPhotoView() != null) {
                mCurPosition--;
                mCurPosition = mCurPosition < 0 ? mPhotoList.size() - 1 : mCurPosition;
                if (mCurPosition == (mPhotoList.size() - 1)) {
                    mViewPager.setCurrentItem(mCurPosition, false);
                } else {
                    mViewPager.setCurrentItem(mCurPosition);
                }
                checkPlayStatus(); // 重新计时
            }
        }
    }
    
    public void nextImage() {
        mHandler.removeMessages(PLAY_ERROR);
        if (mViewPager != null) {
            if (getCurPhotoView() != null) {
                mCurPosition++;
                mCurPosition = mCurPosition >= mPhotoList.size() ? 0 : mCurPosition;
                if (mCurPosition == 0) {
                    mViewPager.setCurrentItem(mCurPosition, false);
                } else {
                    mViewPager.setCurrentItem(mCurPosition);
                }
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
                            AllMediaList.instance(mContext).collectMediaFile(fileNode, PhotoPlayLayout.this);
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
                    mCollectView.setImageDrawable(skinManager.getDrawable(R.drawable.media_collect));
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
                    mCollectView.setImageDrawable(skinManager.getDrawable(R.drawable.media_uncollect));
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
            updateCollectView();
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
                mCurPosition++;
                if (mCurPosition >= mPhotoList.size()) {
                    mCurPosition = 0; // 循环播放
                }
                if (mCurPosition == 0) {
                    mViewPager.setCurrentItem(mCurPosition, false);
                } else {
                    mViewPager.setCurrentItem(mCurPosition);
                }
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
            photoView.setOnMatrixChangeListener(PhotoPlayLayout.this);
            photoView.setOnPhotoTapListener(PhotoPlayLayout.this);
            if (fileNode.isUnSupportFlag() || fileNode.getFile().length() > 52428800) {
                if (position == mCurPosition) {
                    mUnsupportView.setVisibility(View.VISIBLE);
                    mHandler.removeMessages(PLAY_ERROR);
                    mHandler.sendEmptyMessageDelayed(PLAY_ERROR, 1000);
                }
            } else {
                ImageLoad.instance(mContext).loadImageBitmap(photoView,
                        skinManager.getDrawable(R.drawable.image_icon_default),
                        fileNode, PhotoPlayLayout.this);
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
    
    private boolean isDragPage;
    private boolean isHeadToEnd;
    private boolean isEndToHead;
    private OnPageChangeListener mPageChangeListener = new OnPageChangeListener() {
        @Override
        public void onPageScrollStateChanged(int state) {
            checkPlayStatus(); // 重新计时
            if (state == 1) {
                restorePhotoView();
            }
            isDragPage = state == 1;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            checkPlayStatus(); // 重新计时
            if (position == mPhotoList.size() - 1 && isDragPage) {
                isEndToHead = positionOffsetPixels == 0;
                isHeadToEnd = false;
                DebugLog.d("Yearlay", " ....End.... and sroll offset: " + positionOffsetPixels +
                        " && isEndToHead: " + isEndToHead);
            }
            if (position == 0 && isDragPage) {
                isHeadToEnd = positionOffsetPixels == 0;
                isEndToHead = false;
                DebugLog.d("Yearlay", " ....HEAD... and sroll offset: " + positionOffsetPixels +
                        " && isHeadToEnd: " + isHeadToEnd);
            }
        }

        @Override
        public void onPageSelected(int position) {
            checkPlayStatus(); // 重新计时
            restorePhotoView();
            FileNode fileNode = mPhotoList.get(position);
            mCollectView.setImageDrawable(skinManager.getDrawable(fileNode.getCollect() == 1 ?
                    R.drawable.media_collect : R.drawable.media_uncollect));
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
            setCurrentPosition(position);
            DebugLog.d("Yearlay", "onPageSelected mCurPosition: " + mCurPosition);
        }
    };
    
    private void updateCollectView() {
        FileNode fileNode = mPhotoList.get(mCurPosition);
        if (fileNode == null || mCollectView == null) {
            return;
        }
        boolean showFlag = !fileNode.isFromCollectTable();
        if (showFlag) {
            mCollectView.setImageDrawable(skinManager.getDrawable(fileNode.getCollect() == 1 ?
                    R.drawable.media_collect : R.drawable.media_uncollect));
        }
        if (mCtrlBar.getVisibility() == View.VISIBLE) {
            mCollectView.setVisibility(showFlag ? View.VISIBLE : View.GONE);
        }
    }
    
    private OnTouchListener mTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    break;
                case MotionEvent.ACTION_UP:
                    if (mCurPosition == 0 && isDragPage) {
                        if (isHeadToEnd) {
                            DebugLog.d("Yearlay", " ...HEAD to END...");
                            isHeadToEnd = false;
                            mViewPager.setCurrentItem(mPhotoList.size() - 1, false);
                            return true;
                        }
                    }
                    if (mCurPosition == mPhotoList.size() - 1 && isDragPage) {
                        if (isEndToHead) {
                            DebugLog.d("Yearlay", " ...END to HEAD...");
                            isEndToHead = false;
                            nextImage();
                            mViewPager.setCurrentItem(0, false);
                            return true;
                        }
                    }
                    break;
            }
            return false;
        }
    };
}
