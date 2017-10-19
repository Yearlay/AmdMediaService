package com.amd.radio;

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
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class Radio_Activity_Main extends Fragment implements Radio_CarListener, CarService_Listener,
        OnClickListener, OnLongClickListener, OnPageChangeListener {
	private static final String TAG = "Radio_Activity_Main";
    private static final int FREQ_COUNT_MAX = 30;
    
    private ImageButton mCollectButton;
    private ImageView mPlayImageView;
    private TextView mFreqNumTextView;
    private TextView mFreqNameTextView;
    private TextView mSTTextView;
    private ImageView mPrePagerView;
    private ImageView mNextPagerView;
    private ViewPager viewPager;
    private RadioPagerAdapter<View> mPagerAdapter;
    
    private boolean isScan5S = false;
    private boolean isRescan = false;
    private static int tempFreq;
    private Radio_IF mIF;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);    
        mIF = Radio_IF.getInstance();
        mIF.registerCarCallBack(this);
        mIF.registerModeCallBack(this);
        
        mIF.initFavoriteData(getActivity().getApplicationContext());
        Radio_SimpleSave.getInstance().getCurCityStationNameList();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreateView");
        //高德广播
        Intent intent =new Intent();
        intent.setAction("AUTONAVI_STANDARD_BROADCAST_RECV");
        intent.putExtra("KEY_TYPE", 10029);
        getActivity().sendBroadcast(intent);
        
        View rootView = inflater.inflate(R.layout.radio_main_fragment, null);
        mCollectButton = (ImageButton) rootView.findViewById(R.id.radio_fragment_ib_collect);
        viewPager = (ViewPager) rootView.findViewById(R.id.radio_fragment_viewpager);
        viewPager.setOnPageChangeListener(this);
        ImageView rescan = (ImageView) rootView.findViewById(R.id.radio_fragment_rescan);
        View scan_5s = rootView.findViewById(R.id.radio_fragment_scan_5s);
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
        rescan.setOnClickListener(this);
        scan_5s.setOnClickListener(this);

        mPagerAdapter = new RadioPagerAdapter<View>(inflater);
        viewPager.setAdapter(mPagerAdapter);
        updateAllStations();
        
        mIF.setCurBand();
        mIF.scanListChannel();
        return rootView ;
    }
    
    @Override
    public void onStart() {
        super.onStart();
        //高德广播
        Intent intent =new Intent();
        intent.setAction("AUTONAVI_STANDARD_BROADCAST_RECV");
        intent.putExtra("KEY_TYPE", 10029);
        getActivity().sendBroadcast(intent);
        
        if(Data_Common.tempFreq.size() > 0){
            int freq = Radio_IF.sfreqToInt(Data_Common.tempFreq.get(0));
            Data_Common.tempFreq.clear();
            if (isRescan || isScan5S) {
            	isScan5S = false;
                isRescan = false;
                mIF.stopScan();
            }
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
    
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume isScan5S="+isScan5S);
//        if (isScan5S) {
//            mIF.setScan();
//        }
        if (this.getUserVisibleHint()) {
            updateSystemUILabel(ModeDef.RADIO, true);
        }
        updateFreq(mIF.getCurFreq());
    }
    
    @Override
	public void onPause() {
		super.onPause();
        Log.d(TAG, "onPause isScan5S="+isScan5S);
//        if (isScan5S) {
//        	isScan5S = false;
//            mIF.stopScan();
//        }
	}

	@Override
    public void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        mIF.unregisterCarCallBack(this);
        mIF.unregisterModeCallBack(this);
    }
    
    private void updateSystemUILabel(int curLabel, boolean force) {
		Activity activity = getActivity();
        if (activity != null && activity instanceof Media_Activity_Main) {
        	((Media_Activity_Main)activity).updateSystemUILabel(curLabel, force);
        }
	}
    
    private void getViewPagerFragmentNum(){
        int num = Data_Common.stationList.size();
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
        
        boolean isCollected = mIF.isCollected(getActivity(), data);
        if (isCollected) {
            mCollectButton.setImageResource(R.drawable.media_collect);
        } else {
            mCollectButton.setImageResource(R.drawable.media_uncollect);
        }
        
        if (!isRescan && !isScan5S) {
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
        isScan5S = false;
        isRescan = false;
        //Data_Common.stationList.clear();
        for (int i = 0; i < FREQ_COUNT_MAX; i++) {
            int freq = mIF.getChannel(i);
            if (freq != 0 && !haveFreq(freq)) {
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
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            ModeSwitch.instance().setCurrentMode(getActivity(), true, ModeSwitch.RADIO_MODE);
            updateAll();
            updateSystemUILabel(ModeDef.RADIO, true);
        } else {
    		ModeSwitch.instance().setCurrentMode(getActivity(), false, 0);
        }
    }
    
    @Override
    public void onClick(View v) {
        int id = v.getId();
        Log.d(TAG, "onClick id="+id+"; isRescan="+isRescan+"; isScan5S="+isScan5S);
        if(id == R.id.radio_fragment_all){
            startActivity(new Intent(getActivity(), Radio_To_Favorite.class));
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
            boolean isCollected = mIF.isCollected(getActivity(), tempFreq);
            if (isCollected) {
                if (mIF.uncollectFreq(getActivity(), tempFreq, true)) {
                    mCollectButton.setImageResource(R.drawable.media_uncollect);
                }
            } else {
                if (mIF.collectFreq(getActivity(), tempFreq, true)) {
                    mCollectButton.setImageResource(R.drawable.media_collect);
                }
            }
            return;
        }
        if (MediaInterfaceUtil.mediaCannotPlay()) {
            return;
        }
        if(id == R.id.radio_fragment_rescan){
            if (isRescan) {
                isRescan = false;
                mIF.stopScan();
            } else {
                if (isScan5S) {
                    isScan5S = false;
                    mIF.stopScan();
                }
                isRescan = true;
                mIF.scanStore();
            }
        } else if (id == R.id.radio_fragment_scan_5s) {
            if (isScan5S) {
                isScan5S = false;
                mIF.stopScan();
            } else {
                if (isRescan) {
                    isRescan = false;
                    mIF.stopScan();
                }
                if (!mIF.isEnable()) {
                	mIF.setEnable(true);
                }
                isScan5S = true;
                mIF.setScan();
            }
        } else if (id == R.id.radio_fragment_add) {
            mIF.setNextStep();
        } else if (id == R.id.radio_fragment_sub) {
            mIF.setPreStep();
        } else if (id == R.id.radio_fragment_ib_pm_am) {
            mIF.setCurBand();
        } else if (id == R.id.radio_fragment_pre) {
            isScan5S = false;
            isRescan = false;
            mIF.setPreStep();
        } else if (id == R.id.radio_fragment_next) {
            isScan5S = false;
            isRescan = false;
            mIF.setNextStep();
        } else if (id == R.id.radio_fragment_pause_play) {
        	if (isRescan || isScan5S) {
        		isRescan = false;
        		isScan5S = false;
        		mIF.stopScan();
        	}
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
			if (isScan5S || isRescan) {
				isScan5S = false;
				isRescan = false;
				mIF.stopScan();
			}
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
                if (isRescan) {
                	boolean enable = mIF.isEnable();
                    if (data == 3 && enable) {
                    	mIF.setEnable(false);
                    }
                    if (data == 0 && (mIF.getCurSource() == ModeDef.RADIO) && !enable) {
                    	isRescan = false;
                    	mIF.setEnable(true);
                    }
                }
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
}
