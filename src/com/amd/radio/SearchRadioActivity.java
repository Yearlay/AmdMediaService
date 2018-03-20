package com.amd.radio;

import java.util.ArrayList;

import com.amd.media.MediaInterfaceUtil;
import com.haoke.data.AllMediaList;
import com.haoke.mediaservice.R;
import com.haoke.util.DebugLog;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amd.util.SkinManager;
import com.amd.util.SkinManager.SkinListener;

// FM: 87.5 ~ 108.0
public class SearchRadioActivity extends Activity implements OnClickListener,
		OnItemClickListener ,/*Radio_CarListener, CarService_Listener,*/ TextWatcher{
	private static float FM_MIN_FREQ = 87.5f;
	private static float FM_MAX_FREQ = 108.0f;
//	private Radio_IF mIF;
	private String TAG = "SearchRadioActivity";
//	private int freqNum = 30;
//	private static final int DELEY_TIME = 5000;
	
	private RelativeLayout mNumGroup;
	private EditText mInputEditText;
	private String mInputStr = "";
	private ListView mResultListView;
	private SearchAdapter mSearchAdapter;
	private ImageButton mSearchClear;
	private Button mSearchcancel;
	private ImageButton mSearchIcon;
	
	private SkinManager mSkinManager;
	private Drawable mInputEditTextDrawable;
	private Drawable mSearchIconDrawable;
	private Drawable mSearchClearDrawable;
	private Drawable mSearchcancelDrawable;
	private ColorStateList mSearchcancelColorStateList;
	private Drawable mButton0Drawable;
	private Drawable mButton1Drawable;
    private Drawable mButton2Drawable;
    private Drawable mButton3Drawable;
    private Drawable mButton4Drawable;
    private Drawable mButton5Drawable;
    private Drawable mButton6Drawable;
    private Drawable mButton7Drawable;
    private Drawable mButton8Drawable;
    private Drawable mButton9Drawable;
    private Drawable mBackButtonBgDrawable;
    private Drawable mHideButtonBgDrawable;
    private Drawable mButtonPointBgDrawable;
    private Drawable mSearchNumBackDrawable;
    private Drawable mSearchNumHideDrawable;
    private Drawable mSearchNumOkDrawable;
    
	private Button mButton0;
	private Button mButton1;
	private Button mButton2;
	private Button mButton3;
	private Button mButton4;
	private Button mButton5;
	private Button mButton6;
	private Button mButton7;
	private Button mButton8;
	private Button mButton9;
	private Button mButtonPoint;
	private ImageButton mBackButton;
	private ImageButton mHideButton;
	private Button mOkButton;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
		setContentView(R.layout.radio_activity_search);
		mSkinManager = SkinManager.instance(getApplicationContext());
		initView();
	}
	
	private void refreshSkin(boolean loading) {
	    if (loading || mInputEditTextDrawable==null) {
	        mInputEditTextDrawable = mSkinManager.getDrawable(R.drawable.search_input_bg);
	        mSearchIconDrawable = mSkinManager.getDrawable(R.drawable.search_icon);
	        mSearchClearDrawable = mSkinManager.getDrawable(R.drawable.search_num_clear);
	        mSearchcancelDrawable = mSkinManager.getDrawable(R.drawable.search_cancel_bg);
	        mSearchcancelColorStateList = mSkinManager.getColorStateList(R.drawable.text_color_selector);
	        mButton0Drawable = mSkinManager.getDrawable(R.drawable.search_num_bg);
	        mButton1Drawable = mSkinManager.getDrawable(R.drawable.search_num_bg);
	        mButton2Drawable = mSkinManager.getDrawable(R.drawable.search_num_bg);
	        mButton3Drawable = mSkinManager.getDrawable(R.drawable.search_num_bg);
	        mButton4Drawable = mSkinManager.getDrawable(R.drawable.search_num_bg);
	        mButton5Drawable = mSkinManager.getDrawable(R.drawable.search_num_bg);
	        mButton6Drawable = mSkinManager.getDrawable(R.drawable.search_num_bg);
	        mButton7Drawable = mSkinManager.getDrawable(R.drawable.search_num_bg);
	        mButton8Drawable = mSkinManager.getDrawable(R.drawable.search_num_bg);
	        mButton9Drawable = mSkinManager.getDrawable(R.drawable.search_num_bg);
	        mBackButtonBgDrawable = mSkinManager.getDrawable(R.drawable.search_num_bg);
	        mHideButtonBgDrawable = mSkinManager.getDrawable(R.drawable.search_num_bg);
	        mButtonPointBgDrawable = mSkinManager.getDrawable(R.drawable.search_num_bg);
	        
	        mSearchNumBackDrawable = mSkinManager.getDrawable(R.drawable.search_num_back);
	        mSearchNumHideDrawable = mSkinManager.getDrawable(R.drawable.search_num_hide);
	        mSearchNumOkDrawable = mSkinManager.getDrawable(R.drawable.search_num_ok);
	    }
        if (!loading) {
            mInputEditText.setBackground(mInputEditTextDrawable);
            
            mSearchIcon.setBackground(mSearchIconDrawable);
            mSearchClear.setBackgroundDrawable(mSearchClearDrawable);
            
            mSearchcancel.setBackgroundDrawable(mSearchcancelDrawable);
            mSearchcancel.setTextColor(mSearchcancelColorStateList);
            
            mButton0.setBackground(mButton0Drawable);
            mButton1.setBackground(mButton1Drawable);
            mButton2.setBackground(mButton2Drawable);
            mButton3.setBackground(mButton3Drawable);
            mButton4.setBackground(mButton4Drawable);
            mButton5.setBackground(mButton5Drawable);
            mButton6.setBackground(mButton6Drawable);
            mButton7.setBackground(mButton7Drawable);
            mButton8.setBackground(mButton8Drawable);
            mButton9.setBackground(mButton9Drawable);
            
            mBackButton.setBackground(mBackButtonBgDrawable);
            mBackButton.setImageDrawable(mSearchNumBackDrawable);
            
            mHideButton.setBackground(mHideButtonBgDrawable);
            mHideButton.setImageDrawable(mSearchNumHideDrawable);
            
            mOkButton.setBackground(mSearchNumOkDrawable);
            mButtonPoint.setBackground(mButtonPointBgDrawable);
        }
	}

	private void initView() {
		mNumGroup = (RelativeLayout) findViewById(R.id.search_num_group);
		mInputEditText = (EditText) findViewById(R.id.search_input);
		mInputEditText.clearFocus();
		mInputEditText.setInputType(android.text.InputType.TYPE_NULL);
		mInputEditText.addTextChangedListener(this);
		mInputEditText.setOnClickListener(this);
		mSearchIcon = (ImageButton) findViewById(R.id.search_icon);
		mSearchClear = (ImageButton) findViewById(R.id.search_num_clear);
		mSearchClear.setOnClickListener(this);
		mSearchcancel = (Button) findViewById(R.id.search_cancel);
		mSearchcancel.setOnClickListener(this);

		mButton0 = (Button) findViewById(R.id.search_num_0);
		mButton0.setOnClickListener(this);
		mButton1 = (Button) findViewById(R.id.search_num_1);
		mButton1.setOnClickListener(this);
		mButton2 = (Button) findViewById(R.id.search_num_2);
		mButton2.setOnClickListener(this);
		mButton3 = (Button) findViewById(R.id.search_num_3);
		mButton3.setOnClickListener(this);
		mButton4 = (Button) findViewById(R.id.search_num_4);
		mButton4.setOnClickListener(this);
		mButton5 = (Button) findViewById(R.id.search_num_5);
		mButton5.setOnClickListener(this);
		mButton6 = (Button) findViewById(R.id.search_num_6);
		mButton6.setOnClickListener(this);
		mButton7 = (Button) findViewById(R.id.search_num_7);
		mButton7.setOnClickListener(this);
		mButton8 = (Button) findViewById(R.id.search_num_8);
		mButton8.setOnClickListener(this);
		mButton9 = (Button) findViewById(R.id.search_num_9);
		mButton9.setOnClickListener(this);
		mButtonPoint = (Button) findViewById(R.id.search_num_point);
		mButtonPoint.setOnClickListener(this);
		mBackButton = (ImageButton) findViewById(R.id.search_num_back);
		mBackButton.setOnClickListener(this);
		mHideButton = (ImageButton) findViewById(R.id.search_num_hide);
		mHideButton.setOnClickListener(this);
		mOkButton = (Button) findViewById(R.id.search_num_ok);
		mOkButton.setOnClickListener(this);
		
		mResultListView = (ListView) findViewById(R.id.search_result_list);
		mSearchAdapter = new SearchAdapter();
		mResultListView.setAdapter(mSearchAdapter);
		mResultListView.setOnItemClickListener(this);
		
		initNumButton();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		refreshSkin(true);
		refreshSkin(false);
		SkinManager.registerSkin(mSkinListener);
		//getContentResolver().registerContentObserver(MediaInterfaceUtil.URI_SKIN, false, mContentObserver);
		AllMediaList.notifyAllLabelChange(this, R.string.pub_radio);
	}
	
	@Override
    protected void onPause() {
	    SkinManager.unregisterSkin(mSkinListener);
        //getContentResolver().unregisterContentObserver(mContentObserver);
        super.onPause();
    }

	@Override
	public void onClick(View v) {
		int viewId = v.getId();
		if (viewId == R.id.search_num_0) {
			if (!mInputStr.equals("")) {
				mInputStr = mInputStr + "0";
			}
		} else if (viewId == R.id.search_num_1) {
			mInputStr = mInputStr + "1";
		} else if (viewId == R.id.search_num_2) {
			mInputStr = mInputStr + "2";
		} else if (viewId == R.id.search_num_3) {
			mInputStr = mInputStr + "3";
		} else if (viewId == R.id.search_num_4) {
			mInputStr = mInputStr + "4";
		} else if (viewId == R.id.search_num_5) {
			mInputStr = mInputStr + "5";
		} else if (viewId == R.id.search_num_6) {
			mInputStr = mInputStr + "6";
		} else if (viewId == R.id.search_num_7) {
			mInputStr = mInputStr + "7";
		} else if (viewId == R.id.search_num_8) {
			mInputStr = mInputStr + "8";
		} else if (viewId == R.id.search_num_9) {
			mInputStr = mInputStr + "9";
		} else if (viewId == R.id.search_num_point) {
			if (!mInputStr.equals("") && !mInputStr.contains(".")) {
				mInputStr = mInputStr + ".";
			}
		} else if (viewId == R.id.search_num_back) {
			if (mInputStr.length() > 0) {
				mInputStr = mInputStr.substring(0, mInputStr.length() - 1);
			}
		} else if (viewId == R.id.search_num_hide) {
			mNumGroup.setVisibility(View.GONE);
		} else if (viewId == R.id.search_num_ok) {
			if (isEnableAnyOne()) {
				mNumGroup.setVisibility(View.GONE);
				//modify bug 21030 begin
				if (mInputStr.length() > 1) {
				//modify bug 21030 end
	                if (mSearchAdapter.mResultStationList.size() == 1) {
	                    setCurFreq(mSearchAdapter.mResultStationList.get(0).getSfreq(), null);
	                }
				}
			} else {
				setCurFreq(mInputStr, null);
			}
		} else if (viewId == R.id.search_input) {
			mNumGroup.setVisibility(View.VISIBLE);
			hideKeyboard();
		} else if (viewId == R.id.search_num_clear) {
			mInputStr = "";
		} else if (viewId == R.id.search_cancel) {
			exitActivity();
			return;
		}
		if (mInputStr.length() > 7) {
			Toast.makeText(this, "你输入的字数已经超过了限制！", Toast.LENGTH_SHORT).show();
			mInputStr = mInputStr.substring(0, mInputStr.length() - 1);
		}
		mInputEditText.setText(mInputStr);
		mInputEditText.setSelection(mInputStr.length());
		if (mInputStr.length() == 0) {
			initNumButton();
		} else if (mInputStr.length() == 1) {
			secondNumButton();
		} else {
			updateNumButton();
		}
	}
	
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int index = ev.getActionIndex();
        if (index != 0) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int index = ev.getActionIndex();
        if (index != 0) {
            return true;
        }
        return super.onTouchEvent(ev);
    }
	
	private void updateNumButton() {
		mButton0.setEnabled(judgeEnable("0"));
		mButton1.setEnabled(judgeEnable("1"));
		mButton2.setEnabled(judgeEnable("2"));
		mButton3.setEnabled(judgeEnable("3"));
		mButton4.setEnabled(judgeEnable("4"));
		mButton5.setEnabled(judgeEnable("5"));
		mButton6.setEnabled(judgeEnable("6"));
		mButton7.setEnabled(judgeEnable("7"));
		mButton8.setEnabled(judgeEnable("8"));
		mButton9.setEnabled(judgeEnable("9"));
		// 87的时候只能输入点。
		mButtonPoint.setEnabled(judgeEnable(".") || mInputStr.equals("87"));
	}
	
	private void initNumButton() {
		mButton0.setEnabled(false);
		mButton1.setEnabled(true);
		mButton2.setEnabled(false);
		mButton3.setEnabled(false);
		mButton4.setEnabled(false);
		mButton5.setEnabled(false);
		mButton6.setEnabled(false);
		mButton7.setEnabled(false);
		mButton8.setEnabled(true);
		mButton9.setEnabled(true);
		mButtonPoint.setEnabled(false);
	}
	
	private void secondNumButton() {
		int value = Integer.valueOf(mInputStr);
		/*
		 * 只可能是：8,9,1
		 * 8：只能输入：7,8,9
		 * 9：只能输入：0,1,2,3,4,5,6,7,8,9
		 * 1：只能输入：0
		 */
		mButton0.setEnabled(value == 1 || value == 9);
		mButton1.setEnabled(value == 9);
		mButton2.setEnabled(value == 9);
		mButton3.setEnabled(value == 9);
		mButton4.setEnabled(value == 9);
		mButton5.setEnabled(value == 9);
		mButton6.setEnabled(value == 9);
		mButton7.setEnabled(value == 8 || value == 9);
		mButton8.setEnabled(value == 8 || value == 9);
		mButton9.setEnabled(value == 8 || value == 9);
		mButtonPoint.setEnabled(false);
	}
	
	private boolean judgeEnable(String value) {
		boolean ret = false;
		if (!TextUtils.isEmpty(value) && mInputStr.contains(".") && mInputStr.lastIndexOf(".") == mInputStr.length() - 2) {
			return false;
		}
		try {
			Float newF = new Float(mInputStr + value);
			float valueF = newF.floatValue();
			ret = valueF >= FM_MIN_FREQ && valueF <= FM_MAX_FREQ;
		} catch (Exception e) {
			DebugLog.e(TAG, "judgeEnable (mInputStr + value): " + mInputStr + value);
		}
		return ret;
	}
	
	private boolean isEnableAnyOne() {
		return mButton0.isEnabled() || mButton1.isEnabled() ||
				mButton2.isEnabled() || mButton3.isEnabled() ||
				mButton4.isEnabled() || mButton5.isEnabled() ||
				mButton6.isEnabled() || mButton7.isEnabled() ||
				mButton8.isEnabled() || mButton9.isEnabled() ||
				mButtonPoint.isEnabled();
	}
	
//	@Override
//	public void onWindowFocusChanged(boolean hasFocus) {
//		super.onWindowFocusChanged(hasFocus);
//		if (hasFocus) {
//			mIF.scanListChannel();
//		}
//	}

	private void init(){
//		mIF = Radio_IF.getInstance();
//		mIF.setContext(this);
//		mIF.registerCarCallBack(this);
//		mIF.registerModeCallBack(this); // 注册服务监听
//		mIF.bindCarService();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		DebugLog.e(TAG, "onItemClick position:" + position);
		RadioStation radioStation = mSearchAdapter.mResultStationList.get(position);
//		mIF.setCurFreq(radioStation.getFreq());
//		Data_Common.tempFreq.clear();
//		Data_Common.tempFreq.add(radioStation.getSfreq());
//		Data_Common.tempFreq.add(radioStation.getStationName());
		setCurFreq(radioStation.getSfreq(), radioStation.getStationName());
	}
	
	private void setCurFreq(String sfreq, String stationName) {
		if (MediaInterfaceUtil.mediaCannotPlay()) {
			return;
		}
		Data_Common.tempFreq.clear();
		Data_Common.tempFreq.add(sfreq);
		Data_Common.tempFreq.add(stationName);
		exitActivity();
	}
	
//	private Handler handler = new Handler(new Handler.Callback() {
//		
//		@Override
//		public boolean handleMessage(Message msg) {
//			switch (msg.what) {
//			case DELEY_TIME:
//				mIF.scanListChannel();
//				break;
//
//			default:
//				break;
//			}
//			return false;
//		}
//	});
	
//	@Override
//	public void onCarDataChange(int mode, int func, int data) {
//		DebugLog.v(TAG, "mode="+mode+"func="+func+"data="+data);
//		if (mode == mIF.getMode()) {
//			switch (func) {
//			case RadioFunc.FREQ:
//				updateFreq(data);
//				break;
//			case RadioFunc.STATE:
//				break;
//			case RadioFunc.ALL_CH:
//				updateAllStations();
//				break;
//			}
//		}
//	}
	
//	private void updateFreq(int data){
//		String freq_string = String.valueOf(data);
//		if(data >= 8750){ // fm
//			freq_string = freq_string.substring(0, freq_string.length()-2)+"."+freq_string.substring(freq_string.length()-2);
//		}
//		String stationName = Radio_SimpleSave.getInstance().getStationName(data);
//		RadioStation station = new RadioStation(data, freq_string, stationName);
//		Data_Common.stationList.add(station);
//	}
	
	/**
	 * 获取所有频率
	 */
//	public void updateAllStations() {
//		Data_Common.stationList.clear();
//		for (int i = 0; i < freqNum; i++) {
//			int freq = mIF.getChannel(i);
//			String freq_string = String.valueOf(freq);
//			if(freq >= 8750){ // fm
//				freq_string = freq_string.substring(0, freq_string.length()-2)+"."+freq_string.substring(freq_string.length()-2);
//			}
//			String stationName = Radio_SimpleSave.getInstance().getStationName(freq);
//			RadioStation station = new RadioStation(freq, freq_string, stationName);
//			Data_Common.stationList.add(station);		
//		}	
//	}

//	@Override
//	public void setCurInterface(int data) {}
//	@Override
//	public void onServiceConn() {}
	
	class SearchAdapter extends BaseAdapter {
		class ViewHolder {
			TextView freq ;
			TextView freq_name ;
		}
		
		ArrayList<RadioStation> mResultStationList = new ArrayList<RadioStation>();

		@Override
		public int getCount() {
			return mResultStationList.size();
		}

		@Override
		public Object getItem(int position) {
			return mResultStationList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parentGroup) {
			ViewHolder holder = null;
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.radio_search_listview_item, null);
				holder.freq = (TextView) convertView.findViewById(R.id.radio_search_item_freq);
				holder.freq_name = (TextView) convertView.findViewById(R.id.radio_search_item_name);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			convertView.setBackgroundDrawable(SkinManager.instance().getDrawable(R.drawable.music_list_item_selector));
			RadioStation station = mResultStationList.get(position);
			holder.freq.setText(station.getSfreq());
			holder.freq_name.setText(station.getStationName());
			return convertView;
		}
	}

	@Override
	public void afterTextChanged(Editable s) {
		mSearchAdapter.mResultStationList.clear();
		DebugLog.d("Yearlay", "afterTextChanged mInputStr: " + mInputStr);
		for (int i = 0; i < Data_Common.stationList.size(); i++) {
			if (!mInputStr.isEmpty() && Data_Common.stationList.get(i).getSfreq().contains(mInputStr)) {
				mSearchAdapter.mResultStationList.add(Data_Common.stationList.get(i));
			}
		}
		mSearchAdapter.notifyDataSetChanged();
	}

	@Override
	public void beforeTextChanged(CharSequence text, int start, int arg2, int arg3) {}

	@Override
	public void onTextChanged(CharSequence text, int arg1, int arg2, int arg3) {}
	
	public void hideKeyboard() {
		try {
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			if (imm.isActive()) {
				imm.hideSoftInputFromWindow(mInputEditText.getWindowToken(), 0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	private void exitActivity() {
		finish();
        Intent radioIntent = new Intent();
        radioIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        radioIntent.setClassName("com.haoke.mediaservice", "com.haoke.ui.media.Media_Activity_Main");
        radioIntent.putExtra("Mode_To_Music", "radio_intent");
        startActivity(radioIntent);
	}

	private SkinListener mSkinListener = new SkinListener(new Handler()) {
        @Override
        public void loadingSkinData() {
            refreshSkin(true);
        }

        @Override
        public void refreshViewBySkin() {
            refreshSkin(false);
            //modify bug 21029 begin
            mSearchAdapter.notifyDataSetChanged();
            //modify bug 21029 end
        };
    };
}
