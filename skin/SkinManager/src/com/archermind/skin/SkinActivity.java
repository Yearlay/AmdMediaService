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
    
    private RadioGroup mUserGroup;
    private RadioButton mSuperUser;
    private RadioButton mDefaultUser;
    private RadioButton mNicoUser;
    private RadioButton mNamaUser;
    private RadioButton mOtherUser;
    
    String mDefaultInfo = "{\"cur_user\":\"Leon\", \"cur_id\":1 , \"cur_per\":1, \"user_count\":4, \"user_details\": ["
            + "{\"id\":1, \"username\": \"Leon\", \"active\":1, \"permission\":1}, "
            + "{\"id\":2, \"username\": \"Fang\", \"active\":0, \"permission\":0}, "
            + "{\"id\":3, \"username\": \"Nico\", \"active\":0, \"permission\":2}, "
            + "{\"id\":4, \"username\": \"Nama\", \"active\":0, \"permission\":2}"
            + "]}";
    
    String mSuperInfo = "{\"cur_user\":\"Fang\", \"cur_id\":2 , \"cur_per\":0, \"user_count\":4, \"user_details\": ["
            + "{\"id\":1, \"username\": \"Leon\", \"active\":1, \"permission\":1}, "
            + "{\"id\":2, \"username\": \"Fang\", \"active\":0, \"permission\":0}, "
            + "{\"id\":3, \"username\": \"Nico\", \"active\":0, \"permission\":2}, "
            + "{\"id\":4, \"username\": \"Nama\", \"active\":0, \"permission\":2}"
            + "]}";
    
    String mNicoInfo = "{\"cur_user\":\"Nico\", \"cur_id\":3 , \"cur_per\":2, \"user_count\":4, \"user_details\": ["
            + "{\"id\":1, \"username\": \"Leon\", \"active\":1, \"permission\":1}, "
            + "{\"id\":2, \"username\": \"Fang\", \"active\":0, \"permission\":0}, "
            + "{\"id\":3, \"username\": \"Nico\", \"active\":0, \"permission\":2}, "
            + "{\"id\":4, \"username\": \"Nama\", \"active\":0, \"permission\":2}"
            + "]}";
    
    String mNamaInfo = "{\"cur_user\":\"Nama\", \"cur_id\":4 , \"cur_per\":2, \"user_count\":4, \"user_details\": ["
            + "{\"id\":1, \"username\": \"Leon\", \"active\":1, \"permission\":1}, "
            + "{\"id\":2, \"username\": \"Fang\", \"active\":0, \"permission\":0}, "
            + "{\"id\":3, \"username\": \"Nico\", \"active\":0, \"permission\":2}, "
            + "{\"id\":4, \"username\": \"Nama\", \"active\":0, \"permission\":2}"
            + "]}";
    
    private void initView() {
        mSkinGroup = (RadioGroup) findViewById(R.id.skin_group);
        mSkinDefalut = (RadioButton) findViewById(R.id.skin_default);
        mSkinOne = (RadioButton) findViewById(R.id.skin_one);
        
        mUserGroup = (RadioGroup) findViewById(R.id.user_group);
        mDefaultUser = (RadioButton) findViewById(R.id.user_default);
        mSuperUser = (RadioButton) findViewById(R.id.user_super);
        mNicoUser = (RadioButton) findViewById(R.id.user_nico);
        mNamaUser = (RadioButton) findViewById(R.id.user_nama);
        mOtherUser = (RadioButton) findViewById(R.id.user_other);
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
        
        int skinValue = Settings.System.getInt(getContentResolver(), SkinTheme.SKIN_KEY_NAME, -1);
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
        mSkinGroup.setOnCheckedChangeListener(this);
        
        String usersInfo = Settings.System.getString(getContentResolver(),"personal_user_info");
        if (mDefaultInfo.equals(usersInfo)) {
            mDefaultUser.setChecked(true);
        } else if (mSuperInfo.equals(usersInfo)) {
            mSuperUser.setChecked(true);
        } else if (mNicoInfo.equals(usersInfo)) {
            mNicoUser.setChecked(true);
        } else if (mNamaInfo.equals(usersInfo)) {
            mNamaUser.setChecked(true);
        } else {
            mOtherUser.setChecked(true);
        }
        mUserGroup.setOnCheckedChangeListener(this);
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
                
            case R.id.user_default:
                Settings.System.putString(getContentResolver(), "personal_user_info", mDefaultInfo);
                break;
            case R.id.user_super:
                Settings.System.putString(getContentResolver(), "personal_user_info", mSuperInfo);
                break;
            case R.id.user_nico:
                Settings.System.putString(getContentResolver(), "personal_user_info", mNicoInfo);
                break;
            case R.id.user_nama:
                Settings.System.putString(getContentResolver(), "personal_user_info", mNamaInfo);
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
