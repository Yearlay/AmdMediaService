package com.haoke.ui.widget;

import com.amd.util.SkinManager;
import com.haoke.bean.FileNode;
import com.haoke.mediaservice.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.view.View;

public class AmdCheckBox extends RelativeLayout implements View.OnClickListener {
    private TextView mCheckBoxTextView;
    private ImageView mCheckBoxImageView;
    private boolean mIsChecked;
    private SkinManager skinManager;

    public AmdCheckBox(Context context) {
        super(context);
        skinManager = SkinManager.instance(context);
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.amd_checkbox_layout, this, true);
        mCheckBoxTextView = (TextView) findViewById(R.id.checkbox_textview);
        mCheckBoxImageView = (ImageView) findViewById(R.id.checkbox_imageview);
        mCheckBoxTextView.setOnClickListener(this);
        mCheckBoxImageView.setOnClickListener(this);
        updateImageView();
    }
    
    private void updateImageView() {
        mCheckBoxImageView.setImageDrawable(skinManager.getDrawable(
                mIsChecked ? R.drawable.item_switch_select : R.drawable.item_switch_no));
        
    }
    
    public void setChecked(boolean check) {
        mIsChecked = check;
        updateImageView();
    }
    
    public boolean isChecked() {
        return mIsChecked;
    }

    @Override
    public void onClick(View arg0) {
        mIsChecked = !mIsChecked;
        if (mFileNode != null) {
            mFileNode.setSelected(mIsChecked);
        }
        updateImageView();
    }
    
    FileNode mFileNode;
    public void setFileNode(FileNode fileNode) {
        mFileNode = fileNode;
        mCheckBoxTextView.setText(mFileNode.getFileName());
        setChecked(mFileNode.isSelected());
    }
}
