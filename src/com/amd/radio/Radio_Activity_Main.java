package com.amd.radio;

import com.haoke.data.AllMediaList;
import com.haoke.data.ModeSwitch;
import com.haoke.define.McuDef.McuFunc;
import com.haoke.define.RadioDef.RadioFunc;
import com.haoke.mediaservice.R;
import com.haoke.serviceif.CarService_Listener;
import com.amd.media.MediaInterfaceUtil;
import com.amd.radio.Radio_IF;
import com.amd.radio.Radio_CarListener;
import com.amd.util.Source;
import com.amd.util.SkinManager;

import android.content.Context;
import android.content.Intent;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class Radio_Activity_Main extends RelativeLayout implements Radio_CarListener, CarService_Listener,
        OnClickListener, OnLongClickListener, OnPageChangeListener {
    private static final String TAG = "Radio_Activity_Main";
    private static final int FREQ_COUNT_MAX = 30;
    
    private Context mContext;
    
    private ImageButton mCollectButton;
    private ImageView mAllImageView;
    private ImageView mPlayImageView;
    private ImageView mPreImageView;
    private ImageView mNextImageView;
    private TextView mFreqNumTextView;
    private TextView mFreqNameTextView;
    private TextView mSTTextView;
    private ImageView mPrePagerView;
    private ImageView mNextPagerView;
    private ViewPager viewPager;
    private RadioPagerAdapter<View> mPagerAdapter;
    
    private ImageView mScan5sView;
    private ImageView mRescanView;
    
    private static int tempFreq;
    private Radio_IF mIF;
    private boolean mAutoPlay = false;
    
    private SkinManager skinManager;

    public Radio_Activity_Main(Context context) {
        super(context);
    }
    
    public Radio_Activity_Main(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public Radio_Activity_Main(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Log.d(TAG, "Radio_Activity_Main init");
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Log.d(TAG, "onFinishInflate");
        mContext = getContext();
        mIF = Radio_IF.getInstance();
        mIF.registerCarCallBack(this);
        mIF.registerModeCallBack(this);
        skinManager = SkinManager.instance(getContext());
        
        mIF.initFavoriteData(getContext().getApplicationContext());
        Radio_SimpleSave.getInstance().getCurCityStationNameList();
        //高德广播
        Intent intent =new Intent();
        intent.setAction("AUTONAVI_STANDARD_BROADCAST_RECV");
        intent.putExtra("KEY_TYPE", 10029);
        this.getContext().sendBroadcast(intent);
        RelativeLayout rootView = this;
        mCollectButton = (ImageButton) rootView.findViewById(R.id.radio_fragment_ib_collect);
        viewPager = (ViewPager) rootView.findViewById(R.id.radio_fragment_viewpager);
        viewPager.setOnPageChangeListener(this);
        mRescanView = (ImageView) rootView.findViewById(R.id.radio_fragment_rescan);
        mScan5sView = (ImageView) rootView.findViewById(R.id.radio_fragment_scan_5s);
        mFreqNumTextView = (TextView) rootView.findViewById(R.id.radio_fragment_tv_cur);
        mFreqNameTextView = (TextView) rootView.findViewById(R.id.radio_fragment_tv_name);
        mSTTextView = (TextView) rootView.findViewById(R.id.radio_fragment_tv_st);
        mSTTextView.setVisibility(mIF.getST() ? View.VISIBLE : View.INVISIBLE);
        mAllImageView = (ImageView) rootView.findViewById(R.id.radio_fragment_all);
        mAllImageView.setOnClickListener(this);
        mPreImageView = (ImageView) rootView.findViewById(R.id.radio_fragment_pre);
        mPreImageView.setOnClickListener(this);
        mPreImageView.setOnLongClickListener(this);
        mNextImageView = (ImageView) rootView.findViewById(R.id.radio_fragment_next); 
        mNextImageView.setOnClickListener(this);
        mNextImageView.setOnLongClickListener(this);
        rootView.findViewById(R.id.radio_fragment_add).setOnClickListener(this);
        rootView.findViewById(R.id.radio_fragment_sub).setOnClickListener(this);
        mPlayImageView = (ImageView) rootView.findViewById(R.id.radio_fragment_pause_play);
        mPlayImageView.setOnClickListener(this);
        mPlayImageView.setImageDrawable(skinManager.getDrawable(mIF.isEnable() ? R.drawable.pause : R.drawable.play));
        mNextPagerView = (ImageView) rootView.findViewById(R.id.radio_fragment_down);
        mPrePagerView = (ImageView) rootView.findViewById(R.id.radio_fragment_up);
        mNextPagerView.setOnClickListener(this);
        mPrePagerView.setOnClickListener(this);
        rootView.findViewById(R.id.radio_fragment_ib_pm_am).setOnClickListener(this);
        mCollectButton.setOnClickListener(this);
        mRescanView.setOnClickListener(this);
        mScan5sView.setOnClickListener(this);

        mPagerAdapter = new RadioPagerAdapter<View>(LayoutInflater.from(this.getContext()));
        viewPager.setAdapter(mPagerAdapter);
        updateAllStations();
        
        mIF.setCurBand();
        mIF.scanListChannel();
    }
    
    public void onNewIntent(int source, boolean autoPlay) {
        mAutoPlay = autoPlay;
    }
    
    public void onStart() {
        //高德广播
        Intent intent =new Intent();
        intent.setAction("AUTONAVI_STANDARD_BROADCAST_RECV");
        intent.putExtra("KEY_TYPE", 10029);
        mContext.sendBroadcast(intent);
        
        if(Data_Common.tempFreq.size() > 0){
            int freq = Radio_IF.sfreqToInt(Data_Common.tempFreq.get(0));
            Data_Common.tempFreq.clear();
            if (MediaInterfaceUtil.mediaCannotPlay()) {
                return;
            }
            mIF.setCurFreq(freq);
            updateFreq(freq);
            if (!mIF.isEnable()) {
                mIF.setEnable(true);
            }
        }
    }
    
    public void onResume() {
        Log.d(TAG, "onResume mAutoPlay="+mAutoPlay);
        if (mAutoPlay) {
            mIF.setEnable(true);
            mAutoPlay = false;
        }
        AllMediaList.notifyAllLabelChange(getContext(), R.string.pub_radio);
        mPlayImageView.setImageDrawable(skinManager.getDrawable(mIF.isEnable() ? R.drawable.pause : R.drawable.play));
        updateFreq(mIF.getCurFreq());
        ModeSwitch.instance().setCurrentMode(mContext, true, ModeSwitch.RADIO_MODE);
        updateAll();
        refreshScanIcon();
        //refreshSkin();
    }
    
    public void refreshSkin() {
        mAllImageView.setImageDrawable(skinManager.getDrawable(R.drawable.all));
        mPreImageView.setImageDrawable(skinManager.getDrawable(R.drawable.pre));
        mNextImageView.setImageDrawable(skinManager.getDrawable(R.drawable.next));
        mPlayImageView.setImageDrawable(skinManager.getDrawable(mIF.isEnable() ? R.drawable.pause : R.drawable.play));
        updateFreq(mIF.getCurFreq());
        refreshScanIcon();
        onPageSelected(viewPager.getCurrentItem());
    }
    
    public void onPause() {
        Log.d(TAG, "onPause");
        ModeSwitch.instance().setCurrentMode(mContext, false, 0);
    }

    public void onStop() {
    }
    
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        mIF.unregisterCarCallBack(this);
        mIF.unregisterModeCallBack(this);
    }
    
    private void getViewPagerFragmentNum(){
        int num = Data_Common.stationList.size();
        Log.d(TAG, "getViewPagerFragmentNum num="+num);
        Data_Common.pager = num/5;
        Data_Common.reminder = num%5;
        if (Data_Common.reminder != 0) {
            Data_Common.pager += 1;
        }
        mNextPagerView.setVisibility(Data_Common.pager > 1 ? View.VISIBLE : View.INVISIBLE);
        mPrePagerView.setVisibility(Data_Common.pager > 1 ? View.VISIBLE : View.INVISIBLE);
        mPagerAdapter.notifyDataSetChanged();
    }

    class RadioPagerAdapter<T extends View> extends PagerAdapter {

        private LayoutInflater mInflater;
        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        public RadioPagerAdapter(LayoutInflater inflater) {
            mInflater = inflater;
        }
        @Override
        public int getCount() {
            return Data_Common.pager;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View)object);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {

            View view = mInflater.inflate(R.layout.radio_allfreq_viewpager, null);
            Button btn1 = (Button) view.findViewById(R.id.radio_btn1);
            Button btn2 = (Button) view.findViewById(R.id.radio_btn2);
            Button btn3 = (Button) view.findViewById(R.id.radio_btn3);
            Button btn4 = (Button) view.findViewById(R.id.radio_btn4);
            Button btn5 = (Button) view.findViewById(R.id.radio_btn5);

            btn1.setOnClickListener(new MyOnClickListener());
            btn2.setOnClickListener(new MyOnClickListener());
            btn3.setOnClickListener(new MyOnClickListener());
            btn4.setOnClickListener(new MyOnClickListener());
            btn5.setOnClickListener(new MyOnClickListener());
            
            if ((position < Data_Common.pager - 1) || ((position == Data_Common.pager - 1)&&(Data_Common.reminder == 0))) {
                btn1.setText(Data_Common.stationList.get(position*5).getSfreq());
                btn2.setText(Data_Common.stationList.get(position*5 + 1).getSfreq());
                btn3.setText(Data_Common.stationList.get(position*5 + 2).getSfreq());
                btn4.setText(Data_Common.stationList.get(position*5 + 3).getSfreq());
                btn5.setText(Data_Common.stationList.get(position*5 + 4).getSfreq());

            } else if (position == Data_Common.pager - 1) {
                int flag = 0;
                switch(Data_Common.reminder) {
                case 4:
                    btn4.setText(Data_Common.stationList.get(position*5 + 3).getSfreq());
                    flag = 4;
                case 3:
                    btn3.setText(Data_Common.stationList.get(position*5 + 2).getSfreq());
                    if (flag != 4) {
                        btn4.setText(R.string.radio_null);
                    }
                    flag = 3;
                case 2:
                    btn2.setText(Data_Common.stationList.get(position*5 + 1).getSfreq());
                    if (flag != 3) {
                        btn4.setText(R.string.radio_null);
                        btn3.setText(R.string.radio_null);
                    }
                    flag = 2;
                case 1:
                    btn1.setText(Data_Common.stationList.get(position*5).getSfreq());
                    if (flag != 2) {
                        btn4.setText(R.string.radio_null);
                        btn3.setText(R.string.radio_null);
                        btn2.setText(R.string.radio_null);
                    }
                }
                btn5.setText(R.string.radio_null);
            }

            if (view.getParent() == null) {
                container.addView(view);
            }
            return view;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
        
    }
    
    private class MyOnClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            int id = v.getId();
            String sfreq = ((Button)v).getText().toString();
            Log.d(TAG, "MyOnClickListener onClick sfreq="+sfreq);
            if (MediaInterfaceUtil.mediaCannotPlay()) {
                return;
            }
            int freq = -1;
            if (sfreq != null) { 
                for (int i=0; i<Data_Common.stationList.size(); i++) {
                    RadioStation station = Data_Common.stationList.get(i);
                    if (sfreq.equals(station.getSfreq())) {
                        freq = station.getFreq();
                        mIF.setCurFreq(freq);
                        updateFreq(freq);
                        if (!mIF.isEnable()) {
                            mIF.setEnable(true);
                        }
                        break;
                    }
                }
            }
            if (freq == -1) {
                Log.e("Radio_Activity_Main", "ERROR: onClick viewid="+id+"; sfreq="+sfreq);
            }
        }
        
    }

    // 更新频率点
    public void updateFreq(int data) {
        String freq_data = Radio_IF.freqToString(data);
        mFreqNumTextView.setText(freq_data);
        
        tempFreq = data;
        mFreqNameTextView.setText(Radio_SimpleSave.getInstance().getStationName(data));
        
        boolean isCollected = mIF.isCollected(mContext, data);
        if (isCollected) {
            mCollectButton.setImageDrawable(skinManager.getDrawable(R.drawable.media_collect));
        } else {
            mCollectButton.setImageDrawable(skinManager.getDrawable(R.drawable.media_uncollect));
        }
        
        if (!isRescanOrScan5S()) {
            Radio_IF.sendRadioInfo(mIF.getCurBand(), tempFreq);
        }
    }

    /*
     * 播放暂停
     */
    private void updateRadioEnable(boolean enable){
        mPlayImageView.setImageDrawable(skinManager.getDrawable(enable ? R.drawable.pause : R.drawable.play));
    }
    
    /**
     * 获取所有频率
     */
    private void updateAllStations() {
        //exitRescanAndScan5S(false);
        //exitRescan();
        Data_Common.stationList.clear();
        for (int i = 0; i < FREQ_COUNT_MAX; i++) {
            int freq = mIF.getChannel(i);
            if (freq != 0) {
                Log.d(TAG, "updateAllStations i="+i+"; freq="+freq);
                String freq_string = String.valueOf(freq);
                if (freq >= 8750) {
                    freq_string = freq_string.substring(0, freq_string.length()-2)+"."+freq_string.substring(freq_string.length()-2);
                } 
                String stationName = Radio_SimpleSave.getInstance().getStationName(freq);
                RadioStation station = new RadioStation(freq, freq_string, stationName);
                Data_Common.stationList.add(station);
            }
        }
        getViewPagerFragmentNum();
    }
    
    private void updateStatus() {
        mSTTextView.setVisibility(mIF.getST() ? View.VISIBLE : View.INVISIBLE);
    }

    @Override public void onPageScrollStateChanged(int arg0) {}
    @Override public void onPageScrolled(int arg0, float arg1, int arg2) {}
    @Override
    public void onPageSelected(int arg0) {
        if (arg0 == 0) {
            mPrePagerView.setImageDrawable(skinManager.getDrawable(R.drawable.radio_up));
            mPrePagerView.setAlpha(0.3f);
            mPrePagerView.setEnabled(false);
            mNextPagerView.setImageDrawable(skinManager.getDrawable(R.drawable.radio_down));
            mNextPagerView.setAlpha(1.0f);
            mNextPagerView.setEnabled(true);
        }else if(arg0 == Data_Common.pager -1){
            mPrePagerView.setImageDrawable(skinManager.getDrawable(R.drawable.radio_up));
            mPrePagerView.setAlpha(1.0f);
            mPrePagerView.setEnabled(true);
            mNextPagerView.setImageDrawable(skinManager.getDrawable(R.drawable.radio_down));
            mNextPagerView.setAlpha(0.3f);
            mNextPagerView.setEnabled(false);
        }else{
            mPrePagerView.setImageDrawable(skinManager.getDrawable(R.drawable.radio_up));
            mPrePagerView.setAlpha(1.0f);
            mPrePagerView.setEnabled(true);
            mNextPagerView.setImageDrawable(skinManager.getDrawable(R.drawable.radio_down));
            mNextPagerView.setAlpha(1.0f);
            mNextPagerView.setEnabled(true);
        }
    }
    
    @Override
    public void onClick(View v) {
        int id = v.getId();
        Log.d(TAG, "onClick id="+id);
        if (MediaInterfaceUtil.isButtonClickTooFast()) {
            return;
        }
        if(id == R.id.radio_fragment_all){
            mContext.startActivity(new Intent(mContext, Radio_To_Favorite.class));
            return;
        } else if (id == R.id.radio_fragment_down){
            int currentPage = viewPager.getCurrentItem();
            if (currentPage != Data_Common.pager -1) {
                viewPager.setCurrentItem((currentPage + 1));
            }
            return;
        } else if (id == R.id.radio_fragment_up){
            int currentPage = viewPager.getCurrentItem();
            if (currentPage != 0) {
                viewPager.setCurrentItem((currentPage - 1));
            }
            return;
        } else if (id == R.id.radio_fragment_ib_collect) {
            boolean isCollected = mIF.isCollected(mContext, tempFreq);
            if (isCollected) {
                if (mIF.uncollectFreq(mContext, tempFreq, true)) {
                    mCollectButton.setImageDrawable(skinManager.getDrawable(R.drawable.media_uncollect));
                }
            } else {
                if (mIF.collectFreq(mContext, tempFreq, true)) {
                    mCollectButton.setImageDrawable(skinManager.getDrawable(R.drawable.media_collect));
                }
            }
            return;
        }
        if (MediaInterfaceUtil.mediaCannotPlay()) {
            return;
        }
        if(id == R.id.radio_fragment_rescan){
//            if (mIF.isRescanState()) {
//                mIF.exitRescan();
//            } else {
//                mIF.scanStore();
//            }
            if (exitRescan()) {
            } else {
                exitScan5S();
                enterRescan();
            }
        } else if (id == R.id.radio_fragment_scan_5s) {
//            if (mIF.isScan5SState()) {
//                mIF.exitScan5S();
//            } else {
//                mIF.setScan();
//            }
            if (exitScan5S()) {
            } else {
                exitRescan();
                enterScan5S();
            }
        } else if (id == R.id.radio_fragment_add) {
            mIF.setNextStep();
        } else if (id == R.id.radio_fragment_sub) {
            mIF.setPreStep();
        } else if (id == R.id.radio_fragment_ib_pm_am) {
            mIF.setCurBand();
        } else if (id == R.id.radio_fragment_pre) {
            mIF.setPreStep();
        } else if (id == R.id.radio_fragment_next) {
            mIF.setNextStep();
        } else if (id == R.id.radio_fragment_pause_play) {
            boolean enable = mIF.isEnable();
            if (enable) {
                mIF.exitScan5S();
            }
            mIF.setEnable(!enable);
        }
    }
    
    @Override
    public boolean onLongClick(View v) {
        int id = v.getId();
        Log.d(TAG, "onLongClick id="+id);
        if (MediaInterfaceUtil.isButtonClickTooFast()) {
            return true;
        }
        if (MediaInterfaceUtil.mediaCannotPlay()) {
            return true;
        }
        if (id == R.id.radio_fragment_pre) {
            mIF.setPreSearch();
        } else if (id == R.id.radio_fragment_next) {
            mIF.setNextSearch();
        }
        return true;
    }
    
    private void sourceChanged(int source) {
        if (Source.isAudioSource(source) || Source.isVideoSource(source) || Source.isBTMusicSource(source)) {
            mIF.exitRescanAndScan5S(true);
        }
    }
    
    @Override public void setCurInterface(int data) {}
    @Override
    public void onCarDataChange(int mode, int func, int data) {
        Log.d(TAG, "onCarDataChange mode = " + mode + " , func = " + func + " , data = " + data);
        if (Source.isMcuMode(mode)) {
            switch (func) {
            case McuFunc.SOURCE:
                sourceChanged(data);
                break;
            }
        } else if (mode == mIF.getMode()) {
            switch (func) {
            case RadioFunc.FREQ:
                updatePlayStation();
                updateFreq(data);
                break;
            case RadioFunc.BAND:
                updateBand(data);
                break;
            case RadioFunc.AREA:
                break;
            case RadioFunc.ALL_CH:
                updateAllStations();
                break;
            case RadioFunc.CUR_CH:
                //showStateInfo();
                //updateAllStations();
                break;
            case RadioFunc.STATE:
                showStateInfo();
                isScanStateChange(data);
                break;
            case RadioFunc.LOC:
                updateStatus();
                break;
            case RadioFunc.ST:
                updateStatus();
                break;
            case RadioFunc.LISTEN:
                updateStatus();
                break;
            case RadioFunc.RDS:
                showStateInfo();
                break;
            case RadioFunc.RDS_TA:
            case RadioFunc.RDS_AF:
                updateStatus();
                showStateInfo();
                break;
            case RadioFunc.ENABLE:
                updateRadioEnable(mIF.isEnable());
                break;
            }
        }
    }

    @Override
    public void onServiceConn() {
        Log.d(TAG, "onServiceConn");
        mIF.setCurBand();    
        if (mFreqNumTextView!=null) {
            updateFreq(mIF.getCurFreq());
            mSTTextView.setVisibility(mIF.getST() ? View.VISIBLE : View.INVISIBLE);
            mPlayImageView.setImageDrawable(skinManager.getDrawable(mIF.isEnable() ? R.drawable.pause : R.drawable.play));
        }
    }
    
    private void updateAll() {}
    private void updatePlayStation() {}
    public void showStateInfo() {}
    private void updateBand(int data) {} // 更新波段
    
    private void enterScan5S() {
        mIF.setScan();
        mScan5sView.setImageDrawable(skinManager.getDrawable(R.drawable.radio_scan_5s_normal));
    }
    
    private boolean exitScan5S() {
        mScan5sView.setImageDrawable(skinManager.getDrawable(R.drawable.radio_scan_5s_pressed));
        return mIF.exitScan5S();
    }
    
    private void enterRescan() {
        mIF.scanStore();
        mRescanView.setImageDrawable(skinManager.getDrawable(R.drawable.radio_rescan_normal));
    }
    
    private boolean exitRescan() {
        mRescanView.setImageDrawable(skinManager.getDrawable(R.drawable.radio_rescan_pressed));
        return mIF.exitRescan();
    }
    
    private boolean isRescanOrScan5S() {
        return mIF.isRescanState() || mIF.isScan5SState() || mIF.isScanAutoNextState();
    }
    
    private void isScanStateChange(int data) {
        //data为2表示SCAN[Scan5S]， 为3表示SEARCH[Rescan]
        if (data == 2) {
            mScan5sView.setImageDrawable(skinManager.getDrawable(R.drawable.radio_scan_5s_normal));
            mRescanView.setImageDrawable(skinManager.getDrawable(R.drawable.radio_rescan_pressed));
        } else if (data == 3) {
            mScan5sView.setImageDrawable(skinManager.getDrawable(R.drawable.radio_scan_5s_pressed));
            mRescanView.setImageDrawable(skinManager.getDrawable(R.drawable.radio_rescan_normal));
        }
        if (data == 0) {
            mRescanView.setImageDrawable(skinManager.getDrawable(R.drawable.radio_rescan_pressed));
            mScan5sView.setImageDrawable(skinManager.getDrawable(R.drawable.radio_scan_5s_pressed));
        }
    }
    
    private void refreshScanIcon() {
        if (mIF.isRescanState()) {
            mRescanView.setImageDrawable(skinManager.getDrawable(R.drawable.radio_rescan_normal));
        } else {
            mRescanView.setImageDrawable(skinManager.getDrawable(R.drawable.radio_rescan_pressed));
        }
        if (mIF.isScan5SState()) {
            mScan5sView.setImageDrawable(skinManager.getDrawable(R.drawable.radio_scan_5s_normal));
        } else {
            mScan5sView.setImageDrawable(skinManager.getDrawable(R.drawable.radio_scan_5s_pressed));
        }
    }
}
