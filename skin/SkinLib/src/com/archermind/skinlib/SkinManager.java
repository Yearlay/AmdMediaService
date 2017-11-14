package com.archermind.skinlib;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.util.Log;

public class SkinManager {
    private static final String TAG = "SkinManager";
    private static final String SKIN_MANAGER_PACKAGE_NAME = "com.archermind.skin";
    private static SkinManager sManager;
    
    private Context mContext;
    private Resources mResources;
    
    private Context mRemoteContext;
    private Resources mRemoteResources;
    
    private String mAppTag;
    
    public static SkinManager instance(Context context) {
        if (sManager == null) {
            sManager = new SkinManager(context.getApplicationContext());
        }
        return sManager;
    }
    
    private SkinManager(Context context) {
        try {
            mContext = context;
            mResources = mContext.getResources();
            mAppTag = SkinTheme.getAppTag(context.getPackageName());
            mRemoteContext = context.createPackageContext(SKIN_MANAGER_PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
            mRemoteResources = mRemoteContext.getResources();
        } catch (NameNotFoundException e) {
            Log.e(TAG, "init", e);
        }
    }

    private String getSkinTheme(ContentResolver contentResolver) {
        int skinValue = Settings.System.getInt(contentResolver, SkinTheme.SKIN_KEY_NAME, SkinTheme.SKIN_DEFAULT);
        String skinStr = null;
        if (skinValue != SkinTheme.SKIN_DEFAULT) {
            skinStr = String.format(mAppTag + "_%02d_", skinValue);
        }
        
        return skinStr;
    }
    
    private int getRemoteResId(int resId) {
        if (mRemoteContext == null || mRemoteResources == null) {
            return -1;
        }
        String skinHeadStr = getSkinTheme(mContext.getContentResolver());
        if (skinHeadStr != null) {
            try {
                String name = mResources.getResourceEntryName(resId);
                String type = mResources.getResourceTypeName(resId);
                return mRemoteResources.getIdentifier(skinHeadStr + name,
                        type, SKIN_MANAGER_PACKAGE_NAME);
            } catch (Exception e) {
                Log.e(TAG, "getRemoteResId e="+e);
            }
        }
        return 0;
    }
    
    public Bitmap decodeResource(int resId) {
        Bitmap bmp = null;
        int remoteId = getRemoteResId(resId);
        if (remoteId > 0) {
            bmp = BitmapFactory.decodeResource(mRemoteResources, remoteId);
        } else {
            bmp = BitmapFactory.decodeResource(mResources, resId);
        }
        return bmp;
    }
    
    public String getString(int resId) {
        String str = null;
        int remoteId = getRemoteResId(resId);
        if (remoteId > 0) {
            str = mRemoteResources.getString(remoteId);
        } else {
            str = mResources.getString(resId);
        }
        return str;
    }
    
    public int getColor(int resId) {
        int color = 0;
        int remoteId = getRemoteResId(resId);
        if (remoteId > 0) {
            color = mRemoteResources.getColor(remoteId);
        } else {
            color = mResources.getColor(resId);
        }
        return color;
    }
    
    public Drawable getDrawable(int resId) {
        Drawable drawable = null;
        int remoteId = getRemoteResId(resId);
        if (remoteId > 0) {
            drawable = mRemoteResources.getDrawable(remoteId);
        } else {
            drawable = mResources.getDrawable(resId);
        }
        return drawable;
    }
    
    public ColorStateList getColorStateList(int resId) {
        ColorStateList colorStateList = null;
        int remoteId = getRemoteResId(resId);
        if (remoteId > 0) {
            colorStateList = mRemoteResources.getColorStateList(remoteId);
        } else {
            colorStateList = mResources.getColorStateList(resId);
        }
        return colorStateList;
    }
}
