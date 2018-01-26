package com.haoke.ui.media;

import com.amd.util.SkinManager;
import com.haoke.mediaservice.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;

public class Media_Activity_Tab extends RelativeLayout implements OnCheckedChangeListener, View.OnClickListener {
    public void setClickListener(OnClickListener listener) {
        mOnClickListener = listener;
    }

    /*public void setCurSource(int mode) {
        if (mCurMode == mode) {
            return;
        }
        mCurMode = mode;
        View view = null;
        switch (mode) {
        case Media_Activity_Main.MODE_RADIO:
            view = mBtnRadio;
            break;
        case Media_Activity_Main.MODE_AUDIO:
            view = mBtnMusic;
            break;
        default:
            view = null;
            break;
        }
        if (view != null) {
            setFocusBtn(view.getId());
            mBtnLayout.setEnabled(view == mBtnRadio ? false : true);
            mBtnCurPlay.setVisibility(view == mBtnRadio ? View.INVISIBLE : View.VISIBLE);
            mCurPlayUnderLineView.setVisibility((view == mBtnRadio || !mShowCurPlayUnderLine) ? View.INVISIBLE : View.VISIBLE);
        } else {
            setFocusBtn(-1);
            mBtnLayout.setEnabled(true);
            mBtnCurPlay.setVisibility(View.VISIBLE);
            mCurPlayUnderLineView.setVisibility(View.VISIBLE);
        }
    }*/
    
    // ------------------------------外部接口 end------------------------------

    // 内部变量
    private final String TAG = this.getClass().getSimpleName();
    private ImageView mBtnCurPlay;
    private boolean mShowCurPlayUnderLine = false;
    private int mViewState = VIEW_NORMAL;
    private RadioGroup mRadioGroup;
    private RadioButton mBtnNetMusic;
    private RadioButton mBtnNetRadio;
    private RadioButton mBtnRadio;
    private RadioButton mBtnMusic;
    private ImageButton mBtnSearch;
    private View mCurPlayUnderLineView;
    private View mBtnLayout;
    private OnClickListener mOnClickListener = null;
    private int mCurMode = -1;
    SkinManager skinManager;

    public Media_Activity_Tab(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mBtnCurPlay = (ImageView) this.findViewById(R.id.media_tab_current);
        mCurPlayUnderLineView = this.findViewById(R.id.media_tab_current_underlined);
        mRadioGroup = (RadioGroup) this.findViewById(R.id.media_tab_group);
        mBtnNetMusic = (RadioButton) mRadioGroup.findViewById(R.id.media_tab_net_music);
        mBtnNetRadio = (RadioButton) mRadioGroup.findViewById(R.id.media_tab_net_radio);
        mBtnRadio = (RadioButton) mRadioGroup.findViewById(R.id.media_tab_radio);
        mBtnMusic = (RadioButton) mRadioGroup.findViewById(R.id.media_tab_music);
        mBtnSearch = (ImageButton) findViewById(R.id.search_button);
        mBtnSearch.setOnClickListener(this);
        mRadioGroup.setOnCheckedChangeListener(this);
        mBtnLayout = findViewById(R.id.media_tab_layout);
        mBtnLayout.setOnClickListener(this);
        mBtnLayout.setEnabled(true);
    }
    
    public void refreshSkin() {
    	skinManager = SkinManager.instance(getContext());
        mBtnRadio.setTextColor(skinManager.getColorStateList(R.drawable.tab_textcolor_selector));
        mBtnRadio.setBackgroundDrawable(skinManager.getDrawable(R.drawable.tab_backgroud_selector));
        mBtnMusic.setTextColor(skinManager.getColorStateList(R.drawable.tab_textcolor_selector));
        mBtnMusic.setBackgroundDrawable(skinManager.getDrawable(R.drawable.tab_backgroud_selector));
        mBtnSearch.setImageDrawable(skinManager.getDrawable(R.drawable.media_search_selector));
        mCurPlayUnderLineView.setBackground(skinManager.getDrawable(R.drawable.top_bar_underlined_p));
    }
    
    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.media_tab_layout) {
            if (mOnClickListener != null)
                mOnClickListener.onClick(view);
        } else if (id == R.id.search_button) {
            if (mOnClickListener != null)
                mOnClickListener.onClick(view);
        }
    }

    // 设置焦点按钮
    private void setFocusBtn(int id) {
        mRadioGroup.setOnCheckedChangeListener(null);
        mRadioGroup.check(id);
        mRadioGroup.setOnCheckedChangeListener(this);
    }
    
    public static final int VIEW_GONE = 0;   //隐藏
    public static final int VIEW_NORMAL = 1; //白色无动画
    public static final int VIEW_NORMAL_PLAYING = 2; //白色有动画
    public static final int VIEW_CURRENT = 3; //蓝色无动画
    public static final int VIEW_CURRENT_PLAYING = 4; //蓝色有动画
    public void setCurPlayViewState(Boolean showUnderline, Integer viewState) {
        boolean underline = (showUnderline == null) ? mShowCurPlayUnderLine : showUnderline;
        int state = (viewState == null) ? mViewState : viewState;
        int playResId = R.drawable.ico_media_play_n;
        int focusViewId = -1;
        switch (state) {
        case VIEW_GONE:
            focusViewId = mBtnRadio.getId();
            break;
        case VIEW_NORMAL:
            playResId = R.drawable.ico_media_play_n;
            focusViewId = mBtnMusic.getId();
            break;
        case VIEW_NORMAL_PLAYING:
            playResId = R.drawable.music_play_anim_n;
            focusViewId = mBtnMusic.getId();
            break;
        case VIEW_CURRENT:
            playResId = R.drawable.music_play_anim_1;
            break;
        case VIEW_CURRENT_PLAYING:
            playResId = R.drawable.music_play_anim;
            break;
        }
        mShowCurPlayUnderLine = underline;
        mViewState = state;
        mCurPlayUnderLineView.setVisibility((underline&&mBtnCurPlay.getVisibility()==VISIBLE) ? View.VISIBLE : View.INVISIBLE);
        if (state == VIEW_GONE) {
            mBtnLayout.setEnabled(false);
            mBtnCurPlay.setVisibility(View.INVISIBLE);
        } else {
            if (state == VIEW_CURRENT_PLAYING) {
                mBtnLayout.setEnabled(false);
            } else {
                mBtnLayout.setEnabled(true);
            }
            mBtnCurPlay.setVisibility(View.VISIBLE);
            mBtnCurPlay.setImageDrawable(SkinManager.instance(getContext()).getDrawable(playResId));
        }
        setFocusBtn(focusViewId);
    }
    
    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        View view = group.findViewById(checkedId);
        if (mOnClickListener != null)
            mOnClickListener.onClick(view);
    }

}
