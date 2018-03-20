package com.haoke.ui.media;

import java.util.ArrayList;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.amd.media.MediaInterfaceUtil;
import com.amd.util.SkinManager;
import com.amd.util.SkinManager.SkinListener;
import com.amd.util.Source;
import com.haoke.bean.FileNode;
import com.haoke.bean.ID3Parse;
import com.haoke.bean.ImageLoad;
import com.haoke.bean.StorageBean;
import com.haoke.constant.MediaUtil.DeviceType;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.data.AllMediaList;
import com.haoke.data.LoadListener;
import com.haoke.data.SearchListener;
import com.haoke.mediaservice.R;
import com.haoke.ui.image.Image_Activity_Main;
import com.haoke.ui.video.Video_Activity_Main;
import com.haoke.util.Media_IF;

public class MediaSearchActivity extends Activity implements OnClickListener, LoadListener,
        OnItemClickListener, TextView.OnEditorActionListener, SearchListener, TextWatcher {
    private String TAG = "MediaSearchActivity";
    public static final String INTENT_KEY_FILE_TYPE = "file_type";
    private EditText mInputEditText;
    private ImageButton mSearchIcon;
    private ImageButton mClearButton;
    private Button mCancelButton;
    private String mInputStr = "";
    private ListView mResultListView;
    private TextView mNotifyText;
    private String unknown;
    private SkinManager skinManager;
    private Drawable mInputEditTextDrawable;
    private Drawable mSearchIconDrawable;
    private Drawable mClearButtonDrawable;
    private Drawable mCancelButtonDrawable;
    private ColorStateList mCancelButtonColorStateList;
    private ProgressDialog mProgressDialog;

    
    private static final int PROGRESS_DIALOG = 1;
    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
        case PROGRESS_DIALOG:
        	if(mProgressDialog == null) {
        		mProgressDialog = new ProgressDialog(MediaSearchActivity.this);
        		mProgressDialog.setMessage(getResources().getText(R.string.search_media_waiting_msg));
        	}
            return mProgressDialog;
        default:
            return null;
        }
    }

    private SearchAdapter mSearchAdapter;
    private int mFileType = FileType.NULL;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setContentView(R.layout.music_search_activity);
        init();
        initView();
        AllMediaList.instance(getApplicationContext()).registerLoadListener(this);
        skinManager = SkinManager.instance(getApplicationContext());
    }
    
    @Override
    protected void onDestroy() {
        AllMediaList.instance(getApplicationContext()).unRegisterLoadListener(this);
        super.onDestroy();
    }

    private void init() {
        Intent intent = getIntent();
        mFileType = intent.getByteExtra(INTENT_KEY_FILE_TYPE, FileType.NULL);
        unknown = getString(R.string.media_unknow);
    }
    
    private void initView() {
        mInputEditText = (EditText) findViewById(R.id.search_input);
        mInputEditText.setOnEditorActionListener(this);
        mInputEditText.addTextChangedListener(this);
        disableEditTextLongClick(mInputEditText);
        mSearchIcon = (ImageButton) findViewById(R.id.search_icon);
        mClearButton = (ImageButton) findViewById(R.id.search_num_clear);
        mClearButton.setOnClickListener(this);
        mCancelButton = (Button) findViewById(R.id.search_cancel);
        mCancelButton.setOnClickListener(this);
        
        
        mResultListView = (ListView) findViewById(R.id.search_result_list);
        mSearchAdapter = new SearchAdapter();
        mResultListView.setAdapter(mSearchAdapter);
        mResultListView.setOnItemClickListener(this);
        
        mNotifyText = (TextView) findViewById(R.id.search_notify_text);
    }
    
    private void disableEditTextLongClick(EditText view) {
        view.setLongClickable(false);
        view.setCustomSelectionActionModeCallback(new android.view.ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return false;
            }
            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }
            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }
            @Override
            public void onDestroyActionMode(ActionMode mode) {
            }
        });
    }
    
    private void updateLabel() {
        int resId = -1;
        if (mFileType == FileType.AUDIO) {
            resId = R.string.pub_music;
        } else if (mFileType == FileType.IMAGE) {
            resId = R.string.pub_image;
        } else if (mFileType == FileType.VIDEO) {
            resId = R.string.pub_video;
        }
        if (resId != -1) {
            AllMediaList.notifyAllLabelChange(getApplicationContext(), resId);
        }
    }
    
    private void refreshSkin(boolean loading) {
        if (loading || mInputEditTextDrawable==null) {
            mInputEditTextDrawable = skinManager.getDrawable(R.drawable.search_input_bg);
            mSearchIconDrawable = skinManager.getDrawable(R.drawable.search_icon);
            mClearButtonDrawable = skinManager.getDrawable(R.drawable.search_num_clear);
            mCancelButtonDrawable = skinManager.getDrawable(R.drawable.search_cancel_bg);
            mCancelButtonColorStateList = skinManager.getColorStateList(R.drawable.text_color_selector);
        }
        if (!loading) {
            mInputEditText.setBackgroundDrawable(mInputEditTextDrawable);
            mSearchIcon.setBackgroundDrawable(mSearchIconDrawable);
            mClearButton.setBackgroundDrawable(mClearButtonDrawable);
            mCancelButton.setBackgroundDrawable(mCancelButtonDrawable);
            mCancelButton.setTextColor(mCancelButtonColorStateList);
            mSearchAdapter.notifyDataSetChanged();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateLabel();
        refreshSkin(true);
        refreshSkin(false);
        SkinManager.registerSkin(mSkinListener);
        //getContentResolver().registerContentObserver(MediaInterfaceUtil.URI_SKIN, false, mContentObserver);
    }

    @Override
    protected void onPause() {
        SkinManager.unregisterSkin(mSkinListener);
        //getContentResolver().unregisterContentObserver(mContentObserver);
        super.onPause();
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

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.search_num_clear) {
            mInputStr = "";
            mInputEditText.setText(mInputStr);
            mInputEditText.setSelection(mInputStr.length());
            
            mSearchAdapter.mResultStationList.clear();
            mSearchAdapter.notifyDataSetChanged();
            mNotifyText.setVisibility(View.INVISIBLE);
        } else if (viewId == R.id.search_cancel) {
            finish();
            return;
        }
    }
    
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.e(TAG, "onItemClick position:" + position);
        if (MediaInterfaceUtil.mediaCannotPlay()) {
            return;
        }
        FileNode fileNode = mSearchAdapter.mResultStationList.get(position);
        if (mFileType == FileType.AUDIO) {
            boolean isPlayClick = false;
            if (fileNode != null && Source.isAudioSource() 
                    && Media_IF.getInstance().getPlayingDevice() == fileNode.getDeviceType()) {
                if (Media_IF.getInstance().isPlayState()) {
                    FileNode playFileNode = Media_IF.getInstance().getPlayItem();
                    if (playFileNode != null && playFileNode.isSamePathAndFrom(fileNode)) {
                        isPlayClick = true;
                    }
                }
            }
            if (!isPlayClick) {
                Media_IF.getInstance().play(fileNode);
            }
            Intent musicIntent = new Intent();
            musicIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            musicIntent.setClassName("com.haoke.mediaservice", "com.haoke.ui.media.Media_Activity_Main");
            musicIntent.putExtra("Mode_To_Music", "music_play_intent");
            startActivity(musicIntent);
        } else if (mFileType == FileType.IMAGE) {
            Intent intent = new Intent(getApplicationContext(), Image_Activity_Main.class);
            intent.putExtra("isfrom", "MediaSearchActivity");
            intent.putExtra("filepath", fileNode.getFilePath());
            startActivity(intent);
        } else if (mFileType == FileType.VIDEO) {
            Intent intent = new Intent(getApplicationContext(), Video_Activity_Main.class);
            intent.putExtra("isfrom", "MediaSearchActivity");
            intent.putExtra("filepath", fileNode.getFilePath());
            startActivity(intent);
        }
        finish();
    }
    
    class SearchAdapter extends BaseAdapter implements ID3Parse.ID3ParseListener {
        class ViewHolder {
            ImageView mItemBgView;
            TextView mDeviceTypeView;
            ImageView mIconView;
            TextView mTitleView;
            TextView mArtistView;
            TextView mDateView;
        }
        
        ArrayList<FileNode> mResultStationList = new ArrayList<FileNode>();

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
            Log.d(TAG, "getView position="+position+"; convertView="+convertView);
            ViewHolder holder = null;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.music_search_listview_item, null);
                holder.mItemBgView = (ImageView) convertView.findViewById(R.id.search_item_bg);
                holder.mDeviceTypeView = (TextView) convertView.findViewById(R.id.music_item_device_type);
                holder.mIconView = (ImageView) convertView.findViewById(R.id.music_item_icon);
                holder.mTitleView = (TextView) convertView.findViewById(R.id.music_listitem_title);
                holder.mArtistView = (TextView) convertView.findViewById(R.id.music_item_text);
                holder.mDateView = (TextView) convertView.findViewById(R.id.music_item_date);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            
            holder.mDeviceTypeView.setBackgroundColor(skinManager.getColor(R.color.hk_custom_text_p));
            //modify bug 21038 begin
            holder.mItemBgView.setBackgroundDrawable(skinManager.getDrawable(R.drawable.music_list_item_selector));
            //modify bug 21038 end
            
            FileNode fileNode = mResultStationList.get(position);
            // 显示数据来自哪个存储。
            int deviceType = fileNode.getDeviceType();
            if (deviceType == DeviceType.USB1) {
                holder.mDeviceTypeView.setText(R.string.media_usb1_tab);
            } else if (deviceType == DeviceType.USB2) {
                holder.mDeviceTypeView.setText(R.string.media_usb2_tab);
            } else if (deviceType == DeviceType.FLASH) {
                holder.mDeviceTypeView.setText(R.string.media_flash_tab);
            } else {
                holder.mDeviceTypeView.setText(R.string.media_collect_tab);
            }
            // 显示title或者文件名
            String title = fileNode.getTitleEx();
            holder.mTitleView.setText(title);
            // 显示艺术家和专辑名称
            if (mFileType == FileType.AUDIO || mFileType == FileType.VIDEO) {
                String artist = fileNode.getArtist();
                String album = fileNode.getAlbum();
                if (artist == null) {
                    artist = unknown;
                }
                if (album == null) {
                    album = unknown;
                }
                
                holder.mArtistView.setText(artist + " - " + album);
            } else if (mFileType == FileType.IMAGE) {
                holder.mArtistView.setVisibility(View.GONE);
            }
            // 显示文件的时间。
            holder.mDateView.setText(fileNode.getLastDate());
            
            // 设置图片的显示效果。
            Bitmap thumb = null;
            if (fileNode.getFileType() == FileType.AUDIO) {
                holder.mIconView.setImageResource(R.drawable.media_list_item_music);
                if (fileNode.getParseId3() == 1) {
                    ImageLoad.instance(MediaSearchActivity.this).loadBitmap(holder.mIconView,
                            skinManager.getDrawable(R.drawable.media_list_item_music), fileNode);
                }
            } else if (fileNode.getFileType() == FileType.VIDEO) {
                holder.mIconView.setImageDrawable(skinManager.getDrawable(R.drawable.image_icon_default));
                if (fileNode.getParseId3() == 1) {
                    ImageLoad.instance(MediaSearchActivity.this).loadBitmap(holder.mIconView,
                            skinManager.getDrawable(R.drawable.image_icon_default), fileNode);
                }
            } else if (fileNode.getFileType() == FileType.IMAGE) {
                holder.mIconView.setImageDrawable(skinManager.getDrawable(R.drawable.image_icon_default));
                ImageLoad.instance(getApplicationContext()).loadBitmap(holder.mIconView,
                        skinManager.getDrawable(R.drawable.image_icon_default), fileNode);
            }
            return convertView;
        }

        @Override
        public void onID3ParseComplete(Object object, FileNode fileNode) {
            int position = (Integer) object;
            int firstVisiblePosition = mResultListView.getFirstVisiblePosition(); //屏幕内当前可以看见的第一条数据
            if(position-firstVisiblePosition>=0){
                //1.获取当前点击的条目的view
                View itemView = mResultListView.getChildAt(position - firstVisiblePosition);
                //2.查找出相应的控件
                ViewHolder holder = (itemView == null) ? null : (ViewHolder) itemView.getTag();
                //3.更新ui
                if (holder != null && fileNode.getParseId3() == 1) {
                    ImageLoad.instance(MediaSearchActivity.this).loadBitmap(holder.mIconView,
                            skinManager.getDrawable(fileNode.getFileType() == FileType.AUDIO ? 
                                    R.drawable.media_list_item_music : R.drawable.image_icon_default),
                                    fileNode);
                    if (mFileType == FileType.AUDIO || mFileType == FileType.VIDEO) {
                        String artist = fileNode.getArtist();
                        String album = fileNode.getAlbum();
                        if (artist == null) {
                            artist = unknown;
                        }
                        if (album == null) {
                            album = unknown;
                        }
                        holder.mArtistView.setText(artist + " - " + album);
                        holder.mTitleView.setText(fileNode.getTitleEx());
                    }
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEND
                || actionId == EditorInfo.IME_ACTION_SEARCH
                || actionId == EditorInfo.IME_ACTION_GO) {
            doSearch();
            InputMethodManager imm = (InputMethodManager) textView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm.isActive()) {
                imm.hideSoftInputFromWindow(textView.getApplicationWindowToken(), 0);
            }
        }
        return true;
    }

    private void doSearch() {
        String searchStr = mInputEditText.getEditableText().toString();
        if (!TextUtils.isEmpty(searchStr)) {
            if (mFileType == FileType.AUDIO) {
                AllMediaList.instance(getApplicationContext()).searchMusic(searchStr, this);
            } else if (mFileType == FileType.VIDEO) {
                AllMediaList.instance(getApplicationContext()).searchVideo(searchStr, this);
            } else if (mFileType == FileType.IMAGE) {
                AllMediaList.instance(getApplicationContext()).searchImage(searchStr, this);
            }
            showDialog(PROGRESS_DIALOG);
            mNotifyText.setVisibility(View.INVISIBLE);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onSearchCompleted(ArrayList<FileNode> searchList) {
        Log.d(TAG, "onSearchCompleted searchList.size="+searchList.size());
        mSearchAdapter.mResultStationList.clear();
        mSearchAdapter.mResultStationList.addAll(searchList);
        mSearchAdapter.notifyDataSetChanged();
        //dismissDialog(PROGRESS_DIALOG);
        removeDialog(PROGRESS_DIALOG);
        if (searchList.size() == 0) {
            mNotifyText.setText(R.string.search_media_list_empty);
            mNotifyText.setVisibility(View.VISIBLE);
        } else {
            mNotifyText.setVisibility(View.INVISIBLE);
        }
    }
    
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (TextUtils.isEmpty(mInputEditText.getEditableText().toString())) {
            mNotifyText.setVisibility(View.INVISIBLE);
            mSearchAdapter.mResultStationList.clear();
            mSearchAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void afterTextChanged(Editable s) {}

    @Override
    public void onLoadCompleted(int deviceType, int fileType) {
        doSearch();
    }

    @Override
    public void onScanStateChange(StorageBean storageBean) {
        //doSearch();
    }

    private SkinListener mSkinListener = new SkinListener(new Handler()) {
        @Override
        public void loadingSkinData() {
            refreshSkin(true);
        }

        @Override
        public void refreshViewBySkin() {
            refreshSkin(false);
        };
    };
}
