package com.amd.radio;

import com.haoke.data.AllMediaList;
import com.haoke.data.ModeSwitch;
import com.haoke.define.ModeDef;
import com.haoke.define.McuDef.McuFunc;
import com.haoke.define.RadioDef.RadioFunc;
import com.haoke.mediaservice.R;
import com.haoke.serviceif.CarService_Listener;
import com.haoke.ui.media.Media_Activity_Main;
import com.amd.media.MediaInterfaceUtil;
import com.amd.radio.Radio_IF;
import com.amd.radio.Radio_CarListener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
    private ImageView mPlayImageView;
    private TextView mFreqNumTextView;
    private TextView mFreqNameTextView;
    private TextView mSTTextView;
    private ImageView mPrePagerView;
    private ImageView mNextPagerView;
    private ViewPager viewPager;
    private RadioPagerAdapter<View> mPagerAdapter;
    
    private ImageView mScan5sView;
    private ImageView mRescanView;
    
    private static boolean isScan5S = false;
    private static boolean isRescan = false;
    private static int tempFreq;
    private Radio_IF mIF;

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
        rootView.findViewById(R.id.radio_fragment_all).setOnClickListener(this);
        rootView.findViewById(R.id.radio_fragment_pre).setOnClickListener(this);
        rootView.findViewById(R.id.radio_fragment_pre).setOnLongClickListener(this);
        rootView.findViewById(R.id.radio_fragment_next).setOnClickListener(this);
        rootView.findViewById(R.id.radio_fragment_next).setOnLongClickListener(this);
        rootView.findViewById(R.id.radio_fragment_add).setOnClickListener(this);
        rootView.findViewById(R.id.radio_fragment_sub).setOnClickListener(this);
        mPlayImageView = (ImageView) rootView.findViewById(R.id.radio_fragment_pause_play);
        mPlayImageView.setOnClickListener(this);
        mPlayImageView.setImageResource(mIF.isEnable() ? R.drawable.pause : R.drawable.play);
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
    
    public void onStart() {
        //高德广播
        Intent intent =new Intent();
        intent.setAction("AUTONAVI_STANDARD_BROADCAST_RECV");
        intent.putExtra("KEY_TYPE", 10029);
        mContext.sendBroadcast(intent);
        
        if(Data_Common.tempFreq.size() > 0){
            int freq = Radio_IF.sfreqToInt(Data_Common.tempFreq.get(0));
            Data_Common.tempFreq.clear();
            exitRescanAndScan5S(true);
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
        Log.d(TAG, "onResume isScan5S="+isScan5S);
        AllMediaList.notifyAllLabelChange(getContext(), R.string.pub_radio);
        mPlayImageView.setImageResource(mIF.isEnable() ? R.drawable.pause : R.drawable.play);
        updateFreq(mIF.getCurFreq());
        ModeSwitch.instance().setCurrentMode(mContext, true, ModeSwitch.RADIO_MODE);
        updateAll();
        refreshScanIcon();
    }
    
	public void onPause() {
        Log.d(TAG, "onPause isScan5S="+isScan5S);
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
            mCollectButton.setImageResource(R.drawable.media_collect);
        } else {
            mCollectButton.setImageResource(R.drawable.media_uncollect);
        }
        
        if (!isRescanOrScan5S()) {
            Radio_IF.sendRadioInfo(mIF.getCurBand(), tempFreq);
        }
    }

    /*
     * 播放暂停
     */
    private void updateRadioEnable(boolean enable){
        mPlayImageView.setImageResource(enable ? R.drawable.pause : R.drawable.play);
    }
    
    /**
     * 获取所有频率
     */
    private void updateAllStations() {
        //exitRescanAndScan5S(false);
    	//exitRescan();
        //Data_Common.stationList.clear();
        for (int i = 0; i < FREQ_COUNT_MAX; i++) {
            int freq = mIF.getChannel(i);
            boolean addFlag = freq != 0 && !haveFreq(freq);
            if (freq != 0) {
                Log.d(TAG, "updateAllStations i="+i+"; freq="+freq+"; addFlag="+addFlag);
            }
            if (addFlag) {
                String freq_string = String.valueOf(freq);
                if (freq >= 8750) {
                    freq_string = freq_string.substring(0, freq_string.length()-2)+"."+freq_string.substring(freq_string.length()-2);
                } 
                String stationName = Radio_SimpleSave.getInstance().getStationName(freq);
                RadioStation station = new RadioStation(freq, freq_string, stationName);
                if (i < Data_Common.stationList.size()) {
                    Data_Common.stationList.set(i, station);
                } else {
                    Data_Common.stationList.add(station);
                }
            }
        }
        getViewPagerFragmentNum();
    }
    
    private boolean haveFreq(int freq) {
    	int j = 0;
    	for (; j < Data_Common.stationList.size(); j++) {
        	if (Data_Common.stationList.get(j).getFreq() == freq) {
        		break;
        	}
    	}
    	if (j ==  Data_Common.stationList.size()) {
    		return false;
    	}
		return true;
    }
    
    private void updateStatus() {
        mSTTextView.setVisibility(mIF.getST() ? View.VISIBLE : View.INVISIBLE);
    }

    @Override public void onPageScrollStateChanged(int arg0) {}
    @Override public void onPageScrolled(int arg0, float arg1, int arg2) {}
    @Override
    public void onPageSelected(int arg0) {
        if (arg0 == 0) {
            mPrePagerView.setImageResource(R.drawable.up_gray);
            mNextPagerView.setImageResource(R.drawable.down);
        }else if(arg0 == Data_Common.pager -1){
            mPrePagerView.setImageResource(R.drawable.up);
            mNextPagerView.setImageResource(R.drawable.down_gray);
        }else{
            mPrePagerView.setImageResource(R.drawable.up);
            mNextPagerView.setImageResource(R.drawable.down);
        }
    }
    
    @Override
    public void onClick(View v) {
        int id = v.getId();
        Log.d(TAG, "onClick id="+id+"; isRescan="+isRescan+"; isScan5S="+isScan5S);
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
                    mCollectButton.setImageResource(R.drawable.media_uncollect);
                }
            } else {
                if (mIF.collectFreq(mContext, tempFreq, true)) {
                    mCollectButton.setImageResource(R.drawable.media_collect);
                }
            }
            return;
        }
        if (MediaInterfaceUtil.mediaCannotPlay()) {
            return;
        }
        if(id == R.id.radio_fragment_rescan){
            if (exitRescan()) {
            } else {
            	exitScan5S();
            	enterRescan();
            }
        } else if (id == R.id.radio_fragment_scan_5s) {
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
        	exitRescanAndScan5S(false);
            mIF.setPreStep();
        } else if (id == R.id.radio_fragment_next) {
        	exitRescanAndScan5S(false);
            mIF.setNextStep();
        } else if (id == R.id.radio_fragment_pause_play) {
        	exitRescanAndScan5S(true);
        	boolean enable = mIF.isEnable();
        	mIF.setEnable(!enable);
        }
    }
    
    @Override
    public boolean onLongClick(View v) {
        int id = v.getId();
        Log.d(TAG, "onLongClick id="+id);
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
		if (source == ModeDef.AUDIO || source == ModeDef.VIDEO || source == ModeDef.BT) {
			exitRescanAndScan5S(true);
		}
	}
    
    @Override public void setCurInterface(int data) {}
    @Override
    public void onCarDataChange(int mode, int func, int data) {
        Log.d(TAG, "onCarDataChange mode = " + mode + " , func = " + func + " , data = " + data);
    	if (mode == ModeDef.MCU) {
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
                showStateInfo();
                updateAllStations();
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
        	mPlayImageView.setImageResource(mIF.isEnable() ? R.drawable.pause : R.drawable.play);
        }
    }
    
    private void updateAll() {}
    private void updatePlayStation() {}
    public void showStateInfo() {}
    private void updateBand(int data) {} // 更新波段
    
    private void enterScan5S() {
        if (!mIF.isEnable()) {
        	mIF.setEnable(true);
        }
    	isScan5S = true;
    	mIF.setScan();
    	mScan5sView.setImageResource(R.drawable.radio_scan_5s);
    }
    
    private boolean exitScan5S() {
    	boolean state = isScan5S;
    	if (isScan5S) {
            isScan5S = false;
            mIF.stopScan();
        }
    	mScan5sView.setImageResource(R.drawable.radio_scan_5s_select);
    	return state;
    }
    
    private void enterRescan() {
    	isRescan = true;
        mIF.scanStore();
        mRescanView.setImageResource(R.drawable.radio_rescan);
    }
    
    private boolean exitRescan() {
    	boolean state = isRescan;
    	if (isRescan) {
            isRescan = false;
            mIF.stopScan();
        }
    	mRescanView.setImageResource(R.drawable.radio_rescan_select);
    	return state;
    }
    
    private void exitRescanAndScan5S(boolean stopScan) {
		if (stopScan) {
			if (isRescan || isScan5S) {
				mIF.stopScan();
			}
		}
		isRescan = false;
		isScan5S = false;
		mRescanView.setImageResource(R.drawable.radio_rescan_select);
		mScan5sView.setImageResource(R.drawable.radio_scan_5s_select);
    }
    
    private boolean isRescanOrScan5S() {
    	return isRescan || isScan5S;
    }
    
    private void isScanStateChange(int data) {
    	//data为2表示SCAN[Scan5S]， 为3表示SEARCH[Rescan]
    	if (data == 2) {
    		isScan5S = true;
    		mScan5sView.setImageResource(R.drawable.radio_scan_5s);
    	} else if (data == 3) {
    		isRescan = true;
    		mRescanView.setImageResource(R.drawable.radio_rescan);
    	}
    	if (isRescan) {
        	boolean enable = mIF.isEnable();
            if (data == 3 && enable) {
            	mIF.setEnable(false);
            }
            if (data == 0) {
            	if ((mIF.getCurSource() == ModeDef.RADIO) && !enable) {
                	mIF.setEnable(true);
            	}
            	isRescan = false;
            	mRescanView.setImageResource(R.drawable.radio_rescan_select);
            }
        }
    	if (isScan5S) {
    		if (data == 0) {
    			isScan5S = false;
    			mScan5sView.setImageResource(R.drawable.radio_scan_5s_select);
            }
    	}
    }
    
    private void refreshScanIcon() {
    	isRescan = mIF.isRescanState();
    	isScan5S = mIF.isScan5SState();
    	if (isRescan) {
    		mRescanView.setImageResource(R.drawable.radio_rescan);
    	} else {
    		mRescanView.setImageResource(R.drawable.radio_rescan_select);
    	}
    	if (isScan5S) {
    		mScan5sView.setImageResource(R.drawable.radio_scan_5s);
    	} else {
    		mScan5sView.setImageResource(R.drawable.radio_scan_5s_select);
    	}
    }
}
