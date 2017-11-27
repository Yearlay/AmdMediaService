package com.amd.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RadioButton;

public class BeepRadioButton extends RadioButton {
    
    private OnClickListener listener = new OnClickListener() {
        @Override
        public void onClick(View v) {
        }
    };
    
    
    public BeepRadioButton(Context context) {
        this(context, null);
    }
    
    public BeepRadioButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(listener);
    }

}
