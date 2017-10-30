package com.haoke.ui.widget;

import com.haoke.mediaservice.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.view.View;

public class AmdCheckBox extends RelativeLayout implements View.OnClickListener {
    private TextView mCheckBoxTextView;
    private ImageView mCheckBoxImageView;
    private boolean mIsChecked;

    public AmdCheckBox(Context context) {
        super(context);
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
        mCheckBoxImageView.setImageResource(mIsChecked ? R.drawable.item_switch_select : R.drawable.item_switch_no);
        
    }
    
    public void setText(String text) {
        mCheckBoxTextView.setText(text);
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
        if (onCheckedChangeListener != null) {
            onCheckedChangeListener.onCheckedChanged(null, mIsChecked);
        }
        updateImageView();
    }
    
    OnCheckedChangeListener onCheckedChangeListener;
    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        onCheckedChangeListener = listener;
    }

}
