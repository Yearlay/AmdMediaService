package com.amd.util;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import com.amd.media.MediaInterfaceUtil;
import com.haoke.application.MediaApplication;
import com.haoke.service.MediaService;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.shapes.Shape;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

public class SkinManager {
    public static final String SKIN_KEY_NAME_PRELOADING = "bd_theme_color_preloading";
    public static final String SKIN_KEY_NAME = "bd_theme_color";
    
    public static final int SKIN_DEFAULT = 0;
    public static final int SKIN_BLUE = SKIN_DEFAULT;
    public static final int SKIN_RED = 1;
    
    public static final Uri URI_SKIN_PRELOADING = Settings.System.getUriFor(SkinManager.SKIN_KEY_NAME_PRELOADING);
    public static final Uri URI_SKIN = Settings.System.getUriFor(SkinManager.SKIN_KEY_NAME);
    
    private static final String TAG = "AmdSkinManager";
    private static final String SKIN_MANAGER_PACKAGE_NAME = AmdConfig.ENABLE_LOCAl_SKIN_MANAGE ? "com.archermind.skin" : "com.jsbd.skin";
    private static SkinManager sManager;
    
    private Context mContext;
    private Resources mResources;
    
    private Context mRemoteContext;
    private Resources mRemoteResources;
    
    private Object mLock = new Object();
    private int mPreLoading = -1;
    private int mCurrSkin = -1;
    
    private String mAppTag;
    private ArrayList<WeakReference<SkinListener>> mSkinListeners;
    private Handler mServiceHandler;
    
    public static abstract class SkinListener {
        Handler mHandler;
        private SkinListener() {
            mHandler = null;
        }
        public SkinListener(Handler handler) {
            mHandler = handler;
        }
        public abstract void loadingSkinData();//加载资源文件
        public abstract void refreshViewBySkin();//刷新UI即可
    }
    
    private static HashMap<String, String> sAppTagHashMap = new HashMap<String, String>();
    static {
        sAppTagHashMap.put("com.haoke.mediaservice", "mediaservice");
    }
    
    public static String getAppTag(String packageName) {
        return sAppTagHashMap.get(packageName);
    }
    
    public static SkinManager instance() {
        if (sManager == null) {
            sManager = new SkinManager(MediaApplication.getInstance());
        }
        return sManager;
    }
    
    public static SkinManager instance(Context context) {
        if (sManager == null) {
            sManager = new SkinManager(context.getApplicationContext());
        }
        return sManager;
    }
    
    private SkinManager(Context context) {
        try {
            mContext = context;
            mServiceHandler = MediaService.getInstance().getSkinHandler();
            
            ContentResolver contentResolver = mContext.getContentResolver();
            contentResolver.registerContentObserver(URI_SKIN_PRELOADING, false, 
                new ContentObserver(mServiceHandler) {
                    public void onChange(boolean selfChange) {
                        Log.d(TAG, "onChange skin preloading");
                        loadingSkinData();
                    };
                });
            contentResolver.registerContentObserver(URI_SKIN, false, 
                new ContentObserver(mServiceHandler) {
                    public void onChange(boolean selfChange) {
                        Log.d(TAG, "onChange skin");
                        mCurrSkin = -1;
                        refreshViewBySkin();
                    };
                });
            
            mSkinListeners = new ArrayList<WeakReference<SkinListener>>();
            mResources = mContext.getResources();
            mAppTag = getAppTag(context.getPackageName());
            mRemoteContext = context.createPackageContext(SKIN_MANAGER_PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
            mRemoteResources = mRemoteContext.getResources();
        } catch (NameNotFoundException e) {
            Log.e(TAG, "init", e);
        }
    }
    
    private void loadingSkinData() {
        //加载资源文件
        synchronized (mLock) {
            mPreLoading = Settings.System.getInt(mContext.getContentResolver(), SKIN_KEY_NAME_PRELOADING, SKIN_DEFAULT);;
            for (int i=0; i<mSkinListeners.size(); i++) {
                SkinListener skinListener = mSkinListeners.get(i).get();
                if (skinListener != null) {
                    skinListener.loadingSkinData();
                }
            }
            mPreLoading = -1;
        }
    }
    
    private void refreshViewBySkin() {
        //刷新UI即可
        for (int i=0; i<mSkinListeners.size(); i++) {
            SkinListener skinListener = mSkinListeners.get(i).get();
            if (skinListener != null) {
                skinListener.refreshViewBySkin();
            }
        }
    }
    
    //注册监听
    public static void registerSkin(SkinListener listener) {
        boolean found = false;
        ArrayList<WeakReference<SkinListener>> listeners = instance().mSkinListeners;
        for (int i = 0; i < listeners.size(); i++) {
            SkinListener skinListener = listeners.get(i).get();
            if (skinListener == listener) {
                found = true;
                break;
            }
        }
        if (!found)
            listeners.add(new WeakReference<SkinListener>(listener));
    }
    
    //取消监听
    public static void unregisterSkin(SkinListener listener) {
        ArrayList<WeakReference<SkinListener>> listeners = instance().mSkinListeners;
        for (int i = 0; i < listeners.size(); i++) {
            SkinListener skinListener = listeners.get(i).get();
            if (skinListener == listener) {
                listeners.remove(i);
                break;
            }
        }
    }

    private String getSkinTheme(ContentResolver contentResolver) {
        synchronized (mLock) {
            if (mCurrSkin == -1) {
                mCurrSkin = Settings.System.getInt(contentResolver, SKIN_KEY_NAME, SKIN_DEFAULT);
            }
            int skinValue = mPreLoading != -1 ? mPreLoading : mCurrSkin;
            String skinStr = null;
            if (skinValue != SKIN_DEFAULT) {
                skinStr = String.format(mAppTag + "_%02d_", skinValue);
            }
            
            return skinStr;
        }
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
    
    private Shape getDrawableShape() {
        final float[] roundedCorners = new float[] { 5, 5, 5, 5, 5, 5, 5, 5 };
        return new RoundRectShape(roundedCorners, null, null);
    }
    
    private Drawable tileify(Drawable drawable, boolean clip) {
        if (drawable instanceof LayerDrawable) {
            LayerDrawable background = (LayerDrawable) drawable;
            final int N = background.getNumberOfLayers();
            Drawable[] outDrawables = new Drawable[N];
            for (int i = 0; i < N; i++) {
                int id = background.getId(i);
                outDrawables[i] = tileify(background.getDrawable(i),
                        (id == android.R.id.progress || id == android.R.id.secondaryProgress));
            }
            LayerDrawable newBg = new LayerDrawable(outDrawables);
            for (int i = 0; i < N; i++) {
                newBg.setId(i, background.getId(i));
            }
            return newBg;
        } else if (drawable instanceof BitmapDrawable) {
            final Bitmap tileBitmap = ((BitmapDrawable) drawable).getBitmap();
            final ShapeDrawable shapeDrawable = new ShapeDrawable(getDrawableShape());
            final BitmapShader bitmapShader = new BitmapShader(tileBitmap,
                    Shader.TileMode.REPEAT, Shader.TileMode.CLAMP);
            shapeDrawable.getPaint().setShader(bitmapShader);
            return (clip) ? new ClipDrawable(shapeDrawable, Gravity.LEFT,
                    ClipDrawable.HORIZONTAL) : shapeDrawable;
        }
        return drawable;
    }
    
    public Drawable getProgressDrawable(int resId) {
        Drawable drawable = null;
        int remoteId = getRemoteResId(resId);
        if (remoteId > 0) {
            drawable = mRemoteResources.getDrawable(remoteId);
        } else {
            drawable = mResources.getDrawable(resId);
        }
        return tileify(drawable, false);
    }
    
    public static void setScrollViewDrawable(ViewGroup mViewGroup, Drawable thumbDrawable) {
        try {
            Field mScrollCacheField = View.class.getDeclaredField("mScrollCache");
            mScrollCacheField.setAccessible(true);
            Object mScrollCache = mScrollCacheField.get(mViewGroup); // 从listview中获取bar

            Field scrollBarField = mScrollCache.getClass().getDeclaredField("scrollBar");
            scrollBarField.setAccessible(true);
            Object scrollBar = scrollBarField.get(mScrollCache);

            Method method1 = scrollBar.getClass().getDeclaredMethod("setVerticalThumbDrawable", Drawable.class);// 滚动条
            method1.setAccessible(true);
            Method method2 = scrollBar.getClass().getDeclaredMethod("setVerticalTrackDrawable", Drawable.class);// 滚动条背景
            method2.setAccessible(true);
            // method2.invoke(scrollBar,
            // getResources().getDrawable(R.drawable.search_bg));

            // Set your drawable here.
            // method1.invoke(scrollBar,
            // mContext.getResources().getDrawable(R.drawable.vertical));
            // method1.invoke(scrollBar,
            // (Drawable)(skinManager.getDrawable(R.drawable.vertical)));
            
            method1.invoke(scrollBar, thumbDrawable);

        } catch (Exception e) {
            Log.d(TAG, "setScrollViewDrawable: "+e.toString());
        }
    }
}
