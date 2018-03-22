package com.amd.radio;

import com.haoke.service.MediaService;
import com.haoke.util.DebugLog;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;

public class RadioCityListener {
    public static final String CITY_KEY = "amap_province_city";
    public static final Uri URI_CITY_KEY = Settings.System.getUriFor(CITY_KEY);
    private static final String TAG = "RadioCityListener";

    public static void registerRadioCityListener(final MediaService service) {
        ContentResolver contentResolver = service.getContentResolver();
        contentResolver.registerContentObserver(URI_CITY_KEY, false,
                new ContentObserver(null) {
                    public void onChange(boolean selfChange) {
                        getCityProvince(service);
                    };
                });
    }
    
    private static void getCityProvince(final MediaService service) {
        String string = Settings.System.getString(service.getContentResolver(), CITY_KEY);
        DebugLog.d(TAG, "getCityProvince city name string="+string);
        if (TextUtils.isEmpty(string)) {
            return;
        }
        LocationUtils.setReceiveFlag();
        String[] strings = string.split("#");
        String PROVINCE_NAME = "";
        String CITY_NAME = "";
        if (strings.length > 0) {
            PROVINCE_NAME = strings[0];
        }
        if (strings.length > 1) {
            CITY_NAME = strings[1];
        }
        Radio_SimpleSave.setCityFromSettingsProvider(service.getmBDReceiverHandler(), PROVINCE_NAME, CITY_NAME);
    }
}
