package com.archermind.skin;

import com.archermind.skin.R;
import com.archermind.skinlib.SkinTheme;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Toast;

public class SkinActivity extends Activity implements OnCheckedChangeListener{
    private static final String TAG = "SkinActivity";
    private RadioGroup mSkinGroup;
    private RadioButton mSkinDefalut;
    private RadioButton mSkinOne;
    
    private void initView() {
        mSkinGroup = (RadioGroup) findViewById(R.id.skin_group);
        mSkinGroup.setOnCheckedChangeListener(this);

        mSkinDefalut = (RadioButton) findViewById(R.id.skin_default);
        mSkinOne = (RadioButton) findViewById(R.id.skin_one);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.skin_activity);
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        int skinValue = Settings.System.getInt(getContentResolver(), SkinTheme.SKIN_KEY_NAME, SkinTheme.SKIN_DEFAULT);
        switch (skinValue) {
            case SkinTheme.SKIN_DEFAULT:
                mSkinDefalut.setChecked(true);
                break;
            case SkinTheme.SKIN_ONE:
                mSkinOne.setChecked(true);
                break;
            default:
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int checkID) {
        switch (checkID) {
            case R.id.skin_default:
                Toast.makeText(getApplicationContext(), "Skin theme: " + SkinTheme.SKIN_DEFAULT, Toast.LENGTH_SHORT).show();
                Settings.System.putInt(getContentResolver(), SkinTheme.SKIN_KEY_NAME, SkinTheme.SKIN_DEFAULT);
                switchMcc(460);
                break;
            case R.id.skin_one:
                Toast.makeText(getApplicationContext(), "Skin theme: " + SkinTheme.SKIN_ONE, Toast.LENGTH_SHORT).show();
                Settings.System.putInt(getContentResolver(), SkinTheme.SKIN_KEY_NAME, SkinTheme.SKIN_ONE);
                switchMcc(454);
                break;
            default:
                break;
        }
    }

    private void switchMcc(int mcc) {
        try {  
            IActivityManager iActMag = ActivityManagerNative.getDefault();  
            Configuration config = iActMag.getConfiguration();
            Log.d("SkinActivity", "mcc="+config.mcc+"; mnc="+config.mnc);
            config.mcc = mcc;
            iActMag.updateConfiguration(config);  
        } catch (RemoteException e) {  
            Log.d(TAG, "switchMcc e="+e);
        }  
    }
}
