package com.archermind.skinlib;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.provider.Settings;
import android.widget.ImageView;
import android.widget.TextView;

public class SkinManager {
    private static final String TAG = "SkinManager";
    private static final String SKIN_MANAGER_PACKAGE_NAME = "com.archermind.skin";
    private static SkinManager sManager;
    
    private Context mRemoteContext;
    private Context mContext;
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
            mAppTag = SkinTheme.getAppTag(context.getPackageName());
            mRemoteContext = context.createPackageContext(SKIN_MANAGER_PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
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
    
    public String getString(int resId) {
        return getString(mContext.getResources().getResourceEntryName(resId));
    }
    
    public String getString(String name) {
        String str = null;
        String skinHeadStr = getSkinTheme(mContext.getContentResolver());
        int remoteStringId = 0;
        if (skinHeadStr != null && mRemoteContext != null) {
            remoteStringId = mRemoteContext.getResources().getIdentifier(skinHeadStr + name,
                    "string", SKIN_MANAGER_PACKAGE_NAME);
            if (remoteStringId != 0) {
                str = mRemoteContext.getResources().getString(remoteStringId);
            }
        }
        if (remoteStringId == 0) {
            int stringId = mContext.getResources().getIdentifier(name,
                    "string", mContext.getPackageName());
            str = mContext.getResources().getString(stringId);
        }
        return str;
    }
    
    public int getColor(int resId) {
        return getColor(mContext.getResources().getResourceEntryName(resId));
    }
    
    public int getColor(String name) {
        int color = 0;
        String skinHeadStr = getSkinTheme(mContext.getContentResolver());
        int remoteColorId = 0;
        if (skinHeadStr != null && mRemoteContext != null) {
            remoteColorId = mRemoteContext.getResources().getIdentifier(skinHeadStr + name,
                    "color", SKIN_MANAGER_PACKAGE_NAME);
            if (remoteColorId != 0) {
                color = mRemoteContext.getResources().getColor(remoteColorId);
            }
        }
        if (remoteColorId == 0) {
            int colorId = mContext.getResources().getIdentifier(name,
                    "color", mContext.getPackageName());
            color = mContext.getResources().getColor(colorId);
        }
        return color;
    }
    
    public Drawable getDrawable(int resId) {
        return getDrawable(mContext.getResources().getResourceEntryName(resId));
    }
    
    public Drawable getDrawable(String name) {
        Drawable drawable = null;
        String skinHeadStr = getSkinTheme(mContext.getContentResolver());
        int remoteDrawableId = 0;
        if (skinHeadStr != null && mRemoteContext != null) {
            remoteDrawableId = mRemoteContext.getResources().getIdentifier(skinHeadStr + name,
                    "drawable", SKIN_MANAGER_PACKAGE_NAME);
            if (remoteDrawableId != 0) {
                drawable = mRemoteContext.getResources().getDrawable(remoteDrawableId);
            }
        }
        if (remoteDrawableId == 0) {
            int drawableId = mContext.getResources().getIdentifier(name,
                    "drawable", mContext.getPackageName());
            drawable = mContext.getResources().getDrawable(drawableId);
        }
        return drawable;
    }
    
    public StateListDrawable getStateListDrawable(int resId) {
        return getStateListDrawable(mContext.getResources().getResourceEntryName(resId));
    }
    
    public StateListDrawable getStateListDrawable(String name) {
        StateListDrawable stateListDrawable = null;
        String skinHeadStr = getSkinTheme(mContext.getContentResolver());
        int remoteSelectorId = 0;
        try {
            if (skinHeadStr != null && mRemoteContext != null) {
                Resources rs = mRemoteContext.getResources();
                remoteSelectorId = rs.getIdentifier(skinHeadStr + name, "drawable", SKIN_MANAGER_PACKAGE_NAME);
                if (remoteSelectorId != 0) {
                    stateListDrawable = (StateListDrawable) StateListDrawable.createFromXml(rs,
                            rs.getXml(remoteSelectorId));
                }
            }
            if (remoteSelectorId == 0) {
                Resources rs = mContext.getResources();
                int selectorId = rs.getIdentifier(name, "drawable", mContext.getPackageName());
                stateListDrawable = (StateListDrawable) StateListDrawable.createFromXml(rs,
                        rs.getXml(selectorId));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stateListDrawable;
    }
    
    public ColorStateList getColorStateList(int resId) {
        return getColorStateList(mContext.getResources().getResourceEntryName(resId));
    }
    
    public ColorStateList getColorStateList(String name) {
        ColorStateList colorStateList = null;
        String skinHeadStr = getSkinTheme(mContext.getContentResolver());
        int remoteSelectorId = 0;
        if (skinHeadStr != null && mRemoteContext != null) {
            remoteSelectorId = mRemoteContext.getResources().getIdentifier(skinHeadStr + name,
                    "drawable", SKIN_MANAGER_PACKAGE_NAME);
            if (remoteSelectorId != 0) {
                colorStateList = mRemoteContext.getResources().getColorStateList(remoteSelectorId);
            }
        }
        if (remoteSelectorId == 0) {
            int selectorId = mContext.getResources().getIdentifier(name,
                    "drawable", mContext.getPackageName());
            colorStateList = mContext.getResources().getColorStateList(selectorId);
        }
        return colorStateList;
    }
}
