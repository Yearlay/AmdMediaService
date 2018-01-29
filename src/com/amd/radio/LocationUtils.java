package com.amd.radio;

import java.util.List;

import com.haoke.application.MediaApplication;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class LocationUtils {
    private static final String TAG = "LocationUtils";
    private static final boolean ENABLE_LINSTEN_LOCATION = false;
    private volatile static LocationUtils uniqueInstance;
    private LocationManager locationManager;
    private String locationProvider;
    private Location location;
    private Context mContext;

    private LocationUtils() {
        mContext = MediaApplication.getInstance();
        getLocation();
    }

    // 采用Double CheckLock(DCL)实现单例
    public static LocationUtils getInstance() {
        if (uniqueInstance == null) {
            synchronized (LocationUtils.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new LocationUtils();
                }
            }
        }
        return uniqueInstance;
    }

    private void getLocation() {
        // 1.获取位置管理器
        locationManager = (LocationManager) mContext
                .getSystemService(Context.LOCATION_SERVICE);
        // 2.获取位置提供器，GPS或是NetWork
        List<String> providers = locationManager.getProviders(true);
        if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
            // 如果是网络定位
            Log.d(TAG, "LocationManager.NETWORK_PROVIDER");
            locationProvider = LocationManager.NETWORK_PROVIDER;
        } else if (providers.contains(LocationManager.GPS_PROVIDER)) {
            // 如果是GPS定位
            Log.d(TAG, "LocationManager.GPS_PROVIDER");
            locationProvider = LocationManager.GPS_PROVIDER;
        } else {
            Log.d(TAG, "No LocationManager.PROVIDER!");
            return;
        }
        // 3.获取上次的位置，一般第一次运行，此值为null
        Location location = locationManager.getLastKnownLocation(locationProvider);
        if (location != null) {
            setLocation(location);
        }
        // 监视地理位置变化，第二个和第三个参数分别为更新的最短时间minTime和最短距离minDistace
        if (ENABLE_LINSTEN_LOCATION) {
            locationManager.requestLocationUpdates(locationProvider, 0, 0,
                    locationListener);
        }
    }

    private void setLocation(Location location) {
        this.location = location;
        String address = "纬度：" + location.getLatitude() + "经度："
                + location.getLongitude();
        Log.d(TAG, address);
    }

    // 获取经纬度
    public Location initLocation() {
        if (location == null) {
            getLocation();
        }
        return location;
    }

    // 移除定位监听
    public void removeLocationUpdatesListener() {
        if (ENABLE_LINSTEN_LOCATION) {
            return;
        }
        if (locationManager != null) {
            uniqueInstance = null;
            locationManager.removeUpdates(locationListener);
        }
    }

    /**
     * LocationListern监听器 参数：地理位置提供器、监听位置变化的时间间隔、位置变化的距离间隔、LocationListener监听器
     */

    LocationListener locationListener = new LocationListener() {

        /**
         * 当某个位置提供者的状态发生改变时
         */
        @Override
        public void onStatusChanged(String provider, int status, Bundle arg2) {

        }

        /**
         * 某个设备打开时
         */
        @Override
        public void onProviderEnabled(String provider) {

        }

        /**
         * 某个设备关闭时
         */
        @Override
        public void onProviderDisabled(String provider) {

        }

        /**
         * 手机位置发生变动
         */
        @Override
        public void onLocationChanged(Location location) {
            location.getAccuracy();// 精确度
            setLocation(location);
        }
    };
    
    private static boolean sReceiveFlag = false;
    public static void setReceiveFlag() {
        sReceiveFlag = true;
    }
    
    public static boolean sendGpsLonLatToAutoNavi() {
        boolean ret = false;
        Location location = null;
        if (!sReceiveFlag) {
            LocationUtils utils = getInstance();
            location = utils.initLocation();
            if (location != null) {
                Intent intent =new Intent();
                intent.setAction("AUTONAVI_STANDARD_BROADCAST_RECV");
                intent.putExtra("EXTRA_LAT", location.getLatitude());
                intent.putExtra("EXTRA_LON", location.getLongitude());
//                intentEx.putExtra("EXTRA_LAT", 30.26667);
//                intentEx.putExtra("EXTRA_LON", 120.20000);
                intent.putExtra("KEY_TYPE", 10077);
                getInstance().mContext.sendBroadcast(intent);
                ret = true;
            }
        } else {
            ret = true;
        }
        Log.d(TAG, "sendGpsLonLatToAutoNavi sReceiveFlag="+sReceiveFlag+"; location="+location);
        return ret;
    }

}