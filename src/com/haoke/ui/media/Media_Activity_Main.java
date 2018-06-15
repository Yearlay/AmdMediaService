package com.haoke.ui.media;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.haoke.constant.MediaUtil.FileType;
import com.haoke.constant.VRConstant;
import com.haoke.data.AllMediaList;
import com.haoke.data.ModeSwitch;
import com.haoke.mediaservice.R;
import com.haoke.ui.music.MusicHomeFragment;
import com.amd.bt.BT_IF;
import com.amd.media.AmdMediaButtonReceiver;
import com.amd.media.MediaInterfaceUtil;
import com.amd.media.MediaTools;
import com.amd.radio.Radio_Activity_Main;
import com.amd.radio.SearchRadioActivity;
import com.amd.util.SkinManager;
import com.amd.util.SkinManager.SkinListener;
import com.amd.util.Source;
import com.haoke.ui.widget.MyViewPaper;
import com.haoke.util.DebugLog;
import com.haoke.util.Media_IF;

public class Media_Activity_Main extends Activity implements OnClickListener {
    private static final String TAG = "Media_Activity_Main";
    private int mLayoutProps = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
    private Media_Activity_Tab mActivityTab = null;
    private Radio_Activity_Main mRadioFragment = null;
    private MusicHomeFragment mHomeFragment = null;
    private MyViewPaper mViewPager = null;
    private View mSearchButton;
    
    private final int VIEWPAGER_ID_RADIO = 0;
    private final int VIEWPAGER_ID_MUSIC = 1;
    
    public static final int MODE_RADIO = 1;   //收音
    public static final int MODE_AUDIO = 2;   //我的音乐
    public static final int MODE_MUSIC = 3;   //音乐播放界面
    public static final int MODE_BTMUSIC = 4; //蓝牙音乐播放界面
    private static int mCurrMode = MODE_RADIO;
    
    //private MediaPageChangeListener mPageChangeListener = new MediaPageChangeListener();
    private boolean pressBackToHome = false;
    private boolean mActResume = false;
    private boolean mMustFresh = false;
    private Handler mHandler = new Handler();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_activity_main);
        AllMediaList.launcherTocheckAllStorageScanState(this);
        getWindow().getDecorView().setSystemUiVisibility(mLayoutProps);
        
        mActivityTab = (Media_Activity_Tab) findViewById(R.id.media_activity_tab);
        mActivityTab.setClickListener(this);
//        mFragList = new ArrayList<Fragment>();
//        mRadioFragment = new Radio_Activity_Main();
//        mHomeFragment = new MusicHomeFragment();
//        mFragList.add(mRadioFragment);
//        mFragList.add(mHomeFragment);
        LayoutInflater inflater = getLayoutInflater();
        mHomeFragment = (MusicHomeFragment) inflater.inflate(R.layout.music_activity_home, null, false);
        mRadioFragment = (Radio_Activity_Main) inflater.inflate(R.layout.radio_main_fragment, null, false);
        mViewPager = (MyViewPaper) findViewById(R.id.media_activity_viewpager);
        mViewPager.setAdapter(new MediaPagerAdapter());
        //mViewPager.setOnPageChangeListener(mPageChangeListener);
        mViewPager.setOffscreenPageLimit(0);
        mViewPager.disableScroll(true);
        mSearchButton = findViewById(R.id.search_button);

        registerReceiver(mReceiver, new IntentFilter(VRConstant.VRIntent.ACTION_FINISH_MEDIA_ACTIVITY));
        initCurSource();
        SkinManager.registerSkin(mSkinListener);
        //getContentResolver().registerContentObserver(MediaInterfaceUtil.URI_SKIN, false, mContentObserver);
        AllMediaList.notifyUpdateAppWidgetByAll();// 通知MediaWidgetProvider更新UI
        refreshSkin(false);
        DebugLog.d(TAG, "onCreate");
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        this.setIntent(intent);
        DebugLog.d(TAG, "onNewIntent");
        initCurSource();
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        DebugLog.d(TAG, "onStart");
        mRadioFragment.onStart();
        mHomeFragment.onStart();
    }

    @Override
    protected void onStop() {
        DebugLog.d(TAG, "onStart");
        mRadioFragment.onStop();
        mHomeFragment.onStop();
        super.onStop();
    }

    @Override
    protected void onPause() {
        DebugLog.d(TAG, "onPause");
        mActResume = false;
        mRadioFragment.onPause();
        mHomeFragment.onPause();
        pressBackToHome = false;
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        DebugLog.d(TAG, "onResume");
        mActResume = true;
        //mHomeFragment.checkErrorDialog(mHandler, mShowMusicErrorDialog);
        if (mMustFresh) {
            refreshSkin(false);
        }
        if (isShowRadioLayout()) {
            mRadioFragment.onResume();
        } else {
            mHomeFragment.onResume();
        }
        if (getIntent() != null && "com.haoke.data.ModeSwitch".equals(getIntent().getAction())) {
            ModeSwitch.instance().setGoingFlag(false);
        }
        //解决：U盘歌曲播放界面，点击返回，点击蓝牙界面，进入蓝牙界面后，按HOME，然后再按导航的媒体键，会闪一下蓝牙音乐。
//      updateSystemUILabel(mCurLabel, true);
        if (isShowRadioLayout()) {
            ModeSwitch.instance().setCurrentMode(this, true, ModeSwitch.RADIO_MODE);
        }
//        mActivityTab.refreshSkin();
    }

    @Override
    public void onDestroy() {
        DebugLog.d(TAG, "onDestroy");
        mHandler.removeCallbacksAndMessages(null);
        mRadioFragment.onDestroy();
        mHomeFragment.onDestroy();
        super.onDestroy();
        unregisterReceiver(mReceiver);
        SkinManager.unregisterSkin(mSkinListener);
        //getContentResolver().unregisterContentObserver(mContentObserver);
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int index = ev.getActionIndex();
        if (index != 0) {
            return true;
        }
        try {
            return super.dispatchTouchEvent(ev);
        } catch (Exception e) {
        }
        return true;
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int index = ev.getActionIndex();
        if (index != 0) {
            return true;
        }
        return super.onTouchEvent(ev);
    }
    
    private void refreshSkin(boolean loading) {
        mActivityTab.refreshSkin(loading);
        mHomeFragment.refreshSkin(loading);
        mRadioFragment.refreshSkin(loading);
        if (!loading) {
            mMustFresh = false;
        }
    }

    private boolean isShowRadioLayout() {
        return mViewPager.getCurrentItem() == VIEWPAGER_ID_RADIO;
    }
    
    private void initCurSource() {
        Intent intent = getIntent();
        int source = Media_IF.getCurSource();
        int mode = mCurrMode;
        if (Source.isRadioSource(source)) {
            mode = MODE_RADIO;
        } else if (Source.isAudioSource(source)) {
            mode = MODE_AUDIO;
            if (mHomeFragment.isPlayFragment()) {
                mode = MODE_MUSIC;
            }
        } else if (Source.isBTMusicSource(source)) {
            mode = MODE_BTMUSIC;
        }
        if (intent != null) {
            boolean fromIntent = false;
            String musicMode = intent.getStringExtra("Mode_To_Music");
            boolean hasAutoPlay = intent.hasExtra("autoPlay");
            boolean autoPlay = hasAutoPlay ? intent.getBooleanExtra("autoPlay", false) : false;
            String filePathFromIntent = null;
            DebugLog.d(TAG, "initCurSource musicMode="+musicMode+"; autoPlay="+autoPlay);
            if ("radio_intent".equals(musicMode)) {
                mode = MODE_RADIO;
                fromIntent = true;
            } else if ("btMusic_intent".equals(musicMode)) {
                mode = MODE_BTMUSIC;
                fromIntent = true;
            } else if ("music_play_intent".equals(musicMode)) {
                mode = MODE_MUSIC;
                fromIntent = true;
                filePathFromIntent = intent.getStringExtra(MediaTools.INTENT_FILE_PATH);
                if (TextUtils.isEmpty(filePathFromIntent)) {
                    filePathFromIntent = null;
                } else {
                    mHomeFragment.playFilePath(filePathFromIntent);
                }
            } else if ("music_main_home".equals(musicMode)) {
                mode = MODE_AUDIO;
                fromIntent = true;
            }
            if (fromIntent) {
                if (hasAutoPlay) {
                    if (mode == MODE_RADIO) {
                        mRadioFragment.onNewIntent(mode, autoPlay);
                    } else {
                        mHomeFragment.onNewIntent(mode, autoPlay);
                    }
                }
                if (mode == MODE_BTMUSIC) {
                    replaceBtMusicFragment();
                } else if (mode == MODE_MUSIC) {
                    goPlay(false, false, filePathFromIntent!=null);
                } else if (mode == MODE_AUDIO) {
                    goHome();
                } else {
                    goRadio();
                }
            } else {
                if (mode == MODE_AUDIO || mode == MODE_MUSIC || mode == MODE_BTMUSIC) {
                    goPlay(false, false, false);
                } else {
                    goRadio();
                }
            }
        }
        if (intent != null && "com.haoke.data.ModeSwitch".equals(intent.getAction())) {
            pressBackToHome = true;
        }
     }
    
    private void goRadio() {
        if (!isShowRadioLayout()) {
            mViewPager.setCurrentItem(VIEWPAGER_ID_RADIO, false);
            mRadioFragment.onResume();
            mHomeFragment.onPause();
        }
        setCurPlayViewState();
    }
    
    private void goHome() {
        if (isShowRadioLayout()) {
            mViewPager.setCurrentItem(VIEWPAGER_ID_MUSIC, false);
            mHomeFragment.onResume();
            mRadioFragment.onPause();
        }
        mHomeFragment.goHome();
    }
    
    private void goPlay(boolean toast, boolean noPlayGoHome, boolean force) {
        if (isShowRadioLayout()) {
            mViewPager.setCurrentItem(VIEWPAGER_ID_MUSIC, false);
            mHomeFragment.onResume();
            mRadioFragment.onPause();
        }
        mHomeFragment.goPlay(toast, noPlayGoHome, force);
    }
    
    public void replaceBtMusicFragment() {
        if (isShowRadioLayout()) {
            mViewPager.setCurrentItem(VIEWPAGER_ID_MUSIC, false);
            mHomeFragment.replaceBtMusicFragment();
            mHomeFragment.onResume();
            mRadioFragment.onPause();
        } else {
            mHomeFragment.replaceBtMusicFragment();
        }
    }
    
    public boolean getActResumed() {
        return mActResume;
    }
    
    public void setCurPlayViewState() {
        boolean isRadioFragment = mViewPager.getCurrentItem() == VIEWPAGER_ID_RADIO;
        boolean isAudioMusicPlayFragment = mHomeFragment.isAudioPlayFragment();
        boolean isBtMusicPlayFragment = mHomeFragment.isBTMusicPlayFragment();
        boolean isHomeFragment = mHomeFragment.isMusicHomeFragment();
        int source = Media_IF.getCurSource();
        boolean isAudioMusicPlay = (Source.isAudioSource(source) && Media_IF.getInstance().isPlayState());
        boolean isBTMusicPlay = (Source.isBTMusicSource(source) && BT_IF.getInstance().music_isPlaying());
        setCurPlayViewState(isRadioFragment, isHomeFragment, isAudioMusicPlayFragment, isBtMusicPlayFragment,
                    isAudioMusicPlay, isBTMusicPlay);
    }
    
    private void setCurPlayViewState(boolean isRadioFragment, boolean isHomeFragment, 
            boolean isAudioMusicPlayFragment, boolean isBtMusicPlayFragment,
            boolean isAudioMusicPlay, boolean isBTMusicPlay) {
        boolean showSearchButton = true;
        Boolean showUnderline = null;
        Integer viewState = null;
        int mode = MODE_RADIO;
        if (isRadioFragment) {
            mode = MODE_RADIO;
            showUnderline = false;
            viewState = Media_Activity_Tab.VIEW_GONE;
        } else if (isHomeFragment) {
            mode = MODE_AUDIO;
            showUnderline = false;
            if (isAudioMusicPlay || isBTMusicPlay) {
                viewState = Media_Activity_Tab.VIEW_NORMAL_PLAYING;
            } else {
                viewState = Media_Activity_Tab.VIEW_NORMAL;
            }
        } else if (isAudioMusicPlayFragment) {
            mode = MODE_MUSIC;
            showUnderline = true;
            if (isAudioMusicPlay) {
                viewState = Media_Activity_Tab.VIEW_CURRENT_PLAYING;
            } else {
                viewState = Media_Activity_Tab.VIEW_CURRENT;
            }
        } else if (isBtMusicPlayFragment) {
            mode = MODE_BTMUSIC;
            showUnderline = true;
            if (isBTMusicPlay) {
                viewState = Media_Activity_Tab.VIEW_CURRENT_PLAYING;
            } else {
                viewState = Media_Activity_Tab.VIEW_CURRENT;
            }
            showSearchButton = false;
        }
        mCurrMode = mode;
        DebugLog.d(TAG, "setCurPlayViewState isHomeFragment="+isHomeFragment+"; isAudioMusicPlayFragment="+isAudioMusicPlayFragment
                +"; isBtMusicPlayFragment="+isBtMusicPlayFragment+"; isAudioMusicPlay="+isAudioMusicPlay
                +"; isBTMusicPlay="+isBTMusicPlay+"; showSearchButton="+showSearchButton);
//        mActivityTab.setCurPlayViewState(showUnderline, viewState);
        //modify bug 20831 begin
        mActivityTab.setCurPlayViewState(showUnderline, viewState,
                isAudioMusicPlayFragment, isBtMusicPlayFragment,
                isAudioMusicPlay);
        //modify bug 20831 end
        mSearchButton.setVisibility(showSearchButton ? View.VISIBLE : View.INVISIBLE);
    }
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(android.content.Context context, Intent intent) {
            String action = intent.getAction();
            if (VRConstant.VRIntent.ACTION_FINISH_MEDIA_ACTIVITY.equals(action)) {
                String which = intent.getStringExtra(VRConstant.VRIntent.KEY_CLOSE);
                if (TextUtils.isEmpty(which)) {
                    DebugLog.e(TAG, "mReceiver which[KEY_CLOSE] is empty! return");
                    return;
                }
                boolean willFinish = false;
                if (VRConstant.VRIntent.KEY_RADIO.equals(which)) {
                    if (isShowRadioLayout()) {
                        willFinish = true;
                    }
                } else if (VRConstant.VRIntent.KEY_BTMUSIC.equals(which)
                        || VRConstant.VRIntent.KEY_MUSIC.equals(which)) {
                    if (!isShowRadioLayout()) {
                        willFinish = true;
                    }
                }
                if (willFinish) {
                    Media_Activity_Main.this.finish();
                }
            }
        };
    };

    class MediaPagerAdapter<T extends View> extends PagerAdapter {
        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }
        
        @Override  
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);  
        }  
      
        @Override  
        public Object instantiateItem(ViewGroup container, int position) {
            ViewGroup obj = mHomeFragment;
            if (position == 0) {
                obj = mRadioFragment;  
            }
            if (obj.getParent() == null) {
                container.addView(obj);
            }
            return obj;  
        }  
      
        @Override  
        public int getItemPosition(Object object) {  
            return POSITION_NONE;  
        }  

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        DebugLog.d(TAG, "onClick id="+id);
        if (id == R.id.media_tab_radio) {
            goRadio();
        } else if (id == R.id.media_tab_music) {
            goHome();
        } else if (id == R.id.search_button) {
            if (mViewPager.getCurrentItem() == VIEWPAGER_ID_RADIO) {
                startActivity(new Intent(this, SearchRadioActivity.class));
            } else {
                Intent intent = new Intent(this, MediaSearchActivity.class);
                intent.putExtra(MediaSearchActivity.INTENT_KEY_FILE_TYPE, FileType.AUDIO);
                startActivity(intent);
            }
        } else if (id == R.id.media_tab_layout) {
            if (mViewPager.getCurrentItem() == VIEWPAGER_ID_MUSIC) {
                goPlay(true, false, false);
            }
        }
    }
    
    @Override
    public void onBackPressed() {
        DebugLog.d(TAG, "onBackPressed pressBackToHome="+pressBackToHome);
        if (pressBackToHome) {
            pressBackToHome = false;
            MediaInterfaceUtil.launchLauncherActivity(this);
            setIntent(null);
            finish();
            return;
        }
        int item = mViewPager.getCurrentItem();
        if (item == VIEWPAGER_ID_MUSIC) {
            if (mHomeFragment.isPlayFragment()) {
                goHome();
                return;
            }
        }
        super.onBackPressed();
    }
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        DebugLog.d(TAG, "onKeyUp keyCode="+keyCode);
        if (AmdMediaButtonReceiver.onKeyUp(this, keyCode)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
    
    private SkinListener mSkinListener = new SkinListener(new Handler()) {
        @Override
        public void loadingSkinData() {
            refreshSkin(true);
        }

        @Override
        public void refreshViewBySkin() {
            DebugLog.d(TAG, "onChange skin");
            if (mActResume) {
                refreshSkin(false);
            } else {
                mMustFresh = true;
            }
        };
    };

}