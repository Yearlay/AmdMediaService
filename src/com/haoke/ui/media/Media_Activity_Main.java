package com.haoke.ui.media;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.haoke.constant.MediaUtil.FileType;
import com.haoke.constant.VRConstant;
import com.haoke.data.AllMediaList;
import com.haoke.data.ModeSwitch;
import com.haoke.define.ModeDef;
import com.haoke.mediaservice.R;
import com.haoke.ui.music.MusicHomeFragment;
import com.amd.radio.Radio_Activity_Main;
import com.amd.radio.SearchRadioActivity;
import com.haoke.ui.widget.MyViewPaper;
import com.haoke.ui.widget.MyViewPaper.OnPageChangeListener;
import com.haoke.util.Media_IF;

public class Media_Activity_Main extends Activity implements OnClickListener {
    private static final String TAG = "Media_Activity_Main";
    private int mLayoutProps = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
    private Media_IF mMediaIF;
    private Media_Activity_Tab mActivityTab = null;
    private Radio_Activity_Main mRadioFragment = null;
    private MusicHomeFragment mHomeFragment = null;
    private MyViewPaper mViewPager = null;
    private View mSearchButton;
//    private ArrayList<Fragment> mFragList = null;
    private final int VIEWPAGER_ID_RADIO = 0;
    private final int VIEWPAGER_ID_MUSIC = 1;
    private int mCurLabel = ModeDef.AUDIO;
    private MediaPageChangeListener mPageChangeListener = new MediaPageChangeListener();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_activity_main);
        getWindow().getDecorView().setSystemUiVisibility(mLayoutProps);
        
        mMediaIF = Media_IF.getInstance();
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
        mViewPager.setOnPageChangeListener(mPageChangeListener);
        mViewPager.setOffscreenPageLimit(0);
        mSearchButton = findViewById(R.id.search_button);

        registerReceiver(mReceiver, new IntentFilter(VRConstant.VRIntent.ACTION_FINISH_MUSIC_RADIO));
        initCurSource();
        sendBroadcast(new Intent("main_activity_update_ui")); // 通知MediaWidgetProvider更新UI
        Log.d(TAG, "onCreate");
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        this.setIntent(intent);
        Log.d(TAG, "onNewIntent");
        initCurSource();
    }
    
    @Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "onStart");
		mRadioFragment.onStart();
		mHomeFragment.onStart();
	}

	@Override
	protected void onStop() {
		Log.d(TAG, "onStart");
		mRadioFragment.onStop();
		mHomeFragment.onStop();
		super.onStop();
	}

	@Override
	protected void onPause() {
		Log.d(TAG, "onPause");
		mRadioFragment.onPause();
		mHomeFragment.onPause();
		super.onPause();
	}

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume ");
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
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
		mRadioFragment.onDestroy();
		mHomeFragment.onDestroy();
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }
    
    private boolean isShowRadioLayout() {
    	return mViewPager.getCurrentItem() == VIEWPAGER_ID_RADIO;
    }
    
	private void initCurSource() {
        Intent intent = getIntent();
        int source = Media_IF.getCurSource();
        if (intent != null) {
            boolean fromIntent = false;
            boolean goPlay = false;
            boolean goHome = false;
            String musicMode = intent.getStringExtra("Mode_To_Music");
            Log.d(TAG, "initCurSource musicMode="+musicMode);
            if ("radio_intent".equals(musicMode)) {
            	source = ModeDef.RADIO;
            	fromIntent = true;
            } else if ("btMusic_intent".equals(musicMode)) {
            	source = ModeDef.BT;
            	fromIntent = true;
            } else if ("music_play_intent".equals(musicMode)) {
            	source = ModeDef.AUDIO;
            	fromIntent = true;
            	goPlay = true;
            } else if ("music_main_home".equals(musicMode)) {
            	source = ModeDef.AUDIO;
            	fromIntent = true;
            	goHome = true;
            }
            if (fromIntent) {
            	if (source == ModeDef.BT) {
            		replaceBtMusicFragment();
            	} else if (goPlay) {
            		goPlay(false, true);
            	} else if (goHome) {
            		goHome();
            	}
            } else {
            	if (source == ModeDef.AUDIO || source == ModeDef.BT) {
            		if (true || mViewPager.getCurrentItem() == VIEWPAGER_ID_MUSIC) {
                		goPlay(false, true);
            		}
            	}
            }
        }
        setCurSource(source, false);
    }
    
    private void setCurSource(int curSource) {
        setCurSource(curSource, true);
    }

    private void setCurSource(int curSource, boolean smoothScroll) {
        int tabSource = ModeDef.AUDIO;
        int tabItem = VIEWPAGER_ID_MUSIC;
        if (curSource == ModeDef.RADIO) {//3 收音
        	tabSource = ModeDef.RADIO;
        	tabItem = VIEWPAGER_ID_RADIO;
        } else if (curSource == ModeDef.AUDIO || curSource == ModeDef.BT) {//5、1  我的音乐
        	tabSource = ModeDef.AUDIO;
        	tabItem = VIEWPAGER_ID_MUSIC;
        } else { // 默认进入音乐
        	tabSource = ModeDef.AUDIO;
        	tabItem = VIEWPAGER_ID_MUSIC;
        }
        Log.d(TAG, "setCurSource tabSource="+tabSource+"; tabItem="+tabItem);
        mActivityTab.setCurSource(tabSource);
        mViewPager.setOnPageChangeListener(null);
        mViewPager.setCurrentItem(tabItem, smoothScroll);
        mViewPager.setOnPageChangeListener(mPageChangeListener);
        if (mViewPager.getCurrentItem() == VIEWPAGER_ID_RADIO) {
            mSearchButton.setVisibility(View.VISIBLE);
        } else if (curSource == ModeDef.BT || mHomeFragment.isBTMusicPlayFragment()) {
            mSearchButton.setVisibility(View.INVISIBLE);
        } else {
            mSearchButton.setVisibility(View.VISIBLE);
        }
        if (smoothScroll) {
        	if (mViewPager.getCurrentItem() == VIEWPAGER_ID_RADIO) {
        		mRadioFragment.onResume();
        		mHomeFragment.onPause();
        	} else {
        		mRadioFragment.onPause();
        		mHomeFragment.onResume();
        	}
        }
    }
    
    private void goHome() {
    	mHomeFragment.goHome();
    }
    
    private void goPlay(boolean toast, boolean noPlayGoHome) {
    	mHomeFragment.goPlay(toast, noPlayGoHome);
    }
    
    public void replaceBtMusicFragment() {
        mHomeFragment.replaceBtMusicFragment();
    }
    
    public void setCurPlayViewState(boolean isHomeFragment, boolean isAudioMusicPlayFragment, boolean isBtMusicPlayFragment,
            boolean isAudioMusicPlay, boolean isBTMusicPlay) {
        boolean showSearchButton = true;
        Boolean showUnderline = null;
        Integer viewState = null;
        if (isHomeFragment) {
            showUnderline = false;
            if (isAudioMusicPlay || isBTMusicPlay) {
                viewState = Media_Activity_Tab.VIEW_NORMAL_PLAYING;
            } else {
                viewState = Media_Activity_Tab.VIEW_NORMAL;
            }
        } else if (isAudioMusicPlayFragment) {
            showUnderline = true;
            if (isAudioMusicPlay) {
                viewState = Media_Activity_Tab.VIEW_CURRENT_PLAYING;
            } else {
                viewState = Media_Activity_Tab.VIEW_CURRENT;
            }
        } else if (isBtMusicPlayFragment) {
            showUnderline = true;
            if (isBTMusicPlay) {
                viewState = Media_Activity_Tab.VIEW_CURRENT_PLAYING;
            } else {
                viewState = Media_Activity_Tab.VIEW_CURRENT;
            }
            showSearchButton = false;
        }
        if (mViewPager.getCurrentItem() == VIEWPAGER_ID_RADIO) {
            showSearchButton = true;
        }
        Log.d(TAG, "setCurPlayViewState isHomeFragment="+isHomeFragment+"; isAudioMusicPlayFragment="+isAudioMusicPlayFragment
        		+"; isBtMusicPlayFragment="+isBtMusicPlayFragment+"; isAudioMusicPlay="+isAudioMusicPlay
        		+"; isBTMusicPlay="+isBTMusicPlay+"; showSearchButton="+showSearchButton);
        mActivityTab.setCurPlayViewState(showUnderline, viewState);
        mSearchButton.setVisibility(showSearchButton ? View.VISIBLE : View.INVISIBLE);
    }
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(android.content.Context context, Intent intent) {
            String action = intent.getAction();
            if (VRConstant.VRIntent.ACTION_FINISH_MUSIC_RADIO.equals(action)) {
                Media_Activity_Main.this.finish();
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

    class MediaPageChangeListener implements OnPageChangeListener {
        @Override public void onPageScrollStateChanged(int arg0) {
            mViewPager.setOffscreenPageLimit(1);
        }
        @Override public void onPageScrolled(int arg0, float arg1, int arg2) {}
        @Override
        public void onPageSelected(int arg0) {
            int source = ModeDef.NULL;
            switch (arg0) {
            case 0:
                source = ModeDef.RADIO;
                break;
            case 1:
                source = ModeDef.AUDIO;
                break;
            default:
                return;
            }
            setCurSource(source);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        Log.d(TAG, "onClick id="+id);
        if (id == R.id.media_tab_radio) {
            setCurSource(ModeDef.RADIO);
        } else if (id == R.id.media_tab_music) {
            setCurSource(ModeDef.AUDIO);
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
                goPlay(true, false);
        	}
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
    	Log.d(TAG, "onKeyUp keyCode="+keyCode);
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            int item = mViewPager.getCurrentItem();
            if (item == VIEWPAGER_ID_MUSIC) {
                if (mHomeFragment.isPlayFragment()) {
                    goHome();
                    return true;
                }
            }
        }
        return super.onKeyUp(keyCode, event);
    }
}
