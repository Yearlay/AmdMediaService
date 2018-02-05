package com.amd.radio;

import java.util.ArrayList;

import com.amd.media.MediaInterfaceUtil;
import com.amd.radio.Radio_IF;
import com.amd.util.SkinManager;
import com.amd.util.SkinManager.SkinListener;
import com.haoke.data.AllMediaList;
import com.haoke.mediaservice.R;
import com.haoke.ui.widget.CustomDialog;
import com.haoke.ui.widget.CustomDialog.DIALOG_TYPE;
import com.haoke.ui.widget.CustomDialog.OnDialogListener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

public class Radio_To_Favorite extends Activity implements OnClickListener, OnItemClickListener ,OnItemSelectedListener {
	private Button mReturnButton;
    private GridView gridView;
    private TextView loadingView;
    private String TAG = "Radio_To_Favorite";
    private Radio_favorite_gridview_adapter adapter;
    
    private View mEditLayout;
    private Button mEditButton;
    private ArrayList<RadioStation> mFavoriteList = Data_Common.collectAllFreqs;
    private TextView mSelectAllTextView;
    private TextView mCancelTextView;
    private TextView mDeleteTextView;
    
    private CustomDialog mErrorDialog;
    
    private SkinManager skinManager;
    private Drawable gridViewDrawable;
    private Drawable mReturnButtonDrawable;
    private Drawable mEditButtonDrawable;
    private ColorStateList mTextViewColorStateList;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setContentView(R.layout.radio_activity_favorite);
        init();
        initView();
        initFavoriteData();
        skinManager = SkinManager.instance(getApplicationContext());
        Log.d(TAG, "onCreate");
    }
    
    private void init(){
    }
    
    private void initView() {
        loadingView = (TextView) this.findViewById(R.id.radio_favorite_loading);
        
        gridView = (GridView) this.findViewById(R.id.radio_all_gridview);
        gridView.setOnItemClickListener(this);
        gridView.setOnItemSelectedListener(this);
        
        mReturnButton = (Button) findViewById(R.id.radio_return);
        mReturnButton.setOnClickListener(this);
        mEditButton = (Button) findViewById(R.id.radio_edit);
        mEditButton.setOnClickListener(this);
        mEditLayout = this.findViewById(R.id.radio_edit_layout);
        mSelectAllTextView = (TextView)mEditLayout.findViewById(R.id.radio_edit_select_all);
        mSelectAllTextView.setOnClickListener(this);
        mCancelTextView = (TextView) mEditLayout.findViewById(R.id.radio_edit_select_cancel); 
        mCancelTextView.setOnClickListener(this);
        mDeleteTextView = (TextView) mEditLayout.findViewById(R.id.radio_edit_select_del); 
        mDeleteTextView.setOnClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    	if (MediaInterfaceUtil.mediaCannotPlay()) {
			return;
		}
        if (!adapter.changeSelected(position)) {
            TextView freq = (TextView) view.findViewById(R.id.radio_favorite_item_tv_hz);
            TextView name = (TextView) view.findViewById(R.id.radio_favorite_item_name);
            //freq.setTextColor(Color.parseColor("#55AFFE"));
            String freq_string = freq.getText().toString().trim();
            String freq_name = name.getText().toString().trim();
            Data_Common.tempFreq.clear();
            Data_Common.tempFreq.add(freq_string);
            Data_Common.tempFreq.add(freq_name);
            exitActivity();
        } else {
            updateSelectAllTextView();
        }
    }
    
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        
    }
    
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        adapter.changeSelected(position);
        updateSelectAllTextView();
    }
    
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.radio_return) {
            exitActivity();
        } else if(id == R.id.radio_edit) {
            enterEditMode();
        } else if(id == R.id.radio_edit_select_all) {
            selectAll(!checkAllSelected());
        } else if(id == R.id.radio_edit_select_cancel) {
            exitEditMode();
        } else if(id == R.id.radio_edit_select_del) {
            deleteSelected();
        }
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        return super.onCreateView(name, context, attrs);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AllMediaList.notifyAllLabelChange(this, R.string.pub_radio);
        refreshSkin(true);
        refreshSkin(false);
        SkinManager.registerSkin(mSkinListener);
        //getContentResolver().registerContentObserver(MediaInterfaceUtil.URI_SKIN, false, mContentObserver);
    }
    
    private void refreshSkin(boolean loading) {
        if (loading || gridViewDrawable==null) {
            gridViewDrawable = skinManager.getDrawable(R.drawable.radio_listselector);
            mReturnButtonDrawable = skinManager.getDrawable(R.drawable.all_faverite);
            mEditButtonDrawable = skinManager.getDrawable(R.drawable.music_date_edit);
            mTextViewColorStateList = skinManager.getColorStateList(R.drawable.text_color_selector);
        }
        if (!loading) {
            gridView.setSelector(gridViewDrawable);
            mReturnButton.setBackground(mReturnButtonDrawable);
            mEditButton.setCompoundDrawablesWithIntrinsicBounds(mEditButtonDrawable, null, null, null);
            mEditButton.setTextColor(mTextViewColorStateList);
            mSelectAllTextView.setTextColor(mTextViewColorStateList);
            mCancelTextView.setTextColor(mTextViewColorStateList);
            mDeleteTextView.setTextColor(mTextViewColorStateList);
            if (adapter != null) {
                adapter.notifyDataSetChanged();
                SkinManager.setScrollViewDrawable(gridView, skinManager.getDrawable(R.drawable.scrollbar_thumb));
            }
        }
    }

    @Override
    protected void onPause() {
        if (mErrorDialog != null) {
            mErrorDialog.CloseDialog();
        }
        SkinManager.unregisterSkin(mSkinListener);
        //getContentResolver().unregisterContentObserver(mContentObserver);
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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

    private void initFavoriteData() {
        if (mFavoriteList == null) {
            mFavoriteList = Radio_IF.getInstance().initFavoriteData(this.getApplicationContext());
        }
        
        if(mFavoriteList.size() != 0) {
            adapter = new Radio_favorite_gridview_adapter(mFavoriteList, this);
            gridView.setAdapter(adapter);
            mEditButton.setVisibility(View.VISIBLE);
            gridView.setVisibility(View.VISIBLE);
            loadingView.setVisibility(View.GONE);
        } else {
            showEmpty();
        }
    }
    
    public void showEmpty() {
        loadingView.setText("没有收藏的频道");
        loadingView.setVisibility(View.VISIBLE);
        mEditButton.setVisibility(View.INVISIBLE);
        gridView.setVisibility(View.INVISIBLE);
        mEditLayout.setVisibility(View.INVISIBLE);
    }
    
    private void enterEditMode() {
        mEditButton.setVisibility(View.INVISIBLE);
        mEditLayout.setVisibility(View.VISIBLE);
        adapter.enterEditMode();
    }
    
    private void exitEditMode() {
        mEditButton.setVisibility(View.VISIBLE);
        mEditLayout.setVisibility(View.INVISIBLE);
        adapter.exitEditMode();
        selectAll(false);
    }
    
    private void selectAll(boolean select) {
        if (select) {
            adapter.selectAll();
        } else {
            adapter.unSelectAll();
        }
        updateSelectAllTextView();
    }
    
    private void updateSelectAllTextView() {
        if (checkAllSelected()) {
            mSelectAllTextView.setText("撤销");
        } else {
            mSelectAllTextView.setText("全选");
        }
    }
    
    private boolean checkSelected() {
        boolean existSelectFlag = false;
        for (boolean selectFlag : adapter.getSelectedList()) {
            if (selectFlag) {
                existSelectFlag = true;
                break;
            }
        }
        return existSelectFlag;
    }
    
    private boolean checkAllSelected() {
        boolean allSelectFlag = true;
        for (boolean selectFlag : adapter.getSelectedList()) {
            if (!selectFlag) {
                allSelectFlag = false;
                break;
            }
        }
        return allSelectFlag;
    }
    
    private void deleteSelected() {
        if (mErrorDialog == null) {
            mErrorDialog = new CustomDialog();
        }
        if (checkSelected()) {
            mErrorDialog.ShowDialog(this, DIALOG_TYPE.TWO_BTN_MSG, R.string.radio_delect_ok);
            mErrorDialog.SetDialogListener(new OnDialogListener() {
                @Override
                public void OnDialogEvent(int id) {
                    switch (id) {
                    case R.id.pub_dialog_ok:
                        doUnCollect();
                        break;
                    case R.id.pub_dialog_cancel:
                        break;
                    }
                }
                @Override
                public void OnDialogDismiss() {
                }
            });
        } else {
            mErrorDialog.ShowDialog(this, DIALOG_TYPE.ONE_BTN, R.string.radio_delete_empty);
        }
    }
    
    private void doUnCollect() {
        ArrayList<RadioStation> list = adapter.deleteSelected();
        Radio_IF.getInstance().uncollectFreq(this.getApplicationContext(), list, false);
        if (mFavoriteList != null && mFavoriteList.size() == 0) {
            exitEditMode();
            showEmpty();
        }
        updateSelectAllTextView();
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
            Log.d(TAG, "onChange skin");
            refreshSkin(false);
        };
    };
}
