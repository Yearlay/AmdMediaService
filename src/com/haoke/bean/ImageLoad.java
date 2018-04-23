package com.haoke.bean;

import java.io.File;

import com.amd.util.AmdConfig;
import com.haoke.constant.MediaUtil.FileType;
import com.haoke.mediaservice.R;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.utils.StorageUtils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

public class ImageLoad {
    private static final int MAX_SIZE =  AmdConfig.MAX_SIZE_OF_IMAGE * 1024 * 1024;
    private Context mContext;
    
    private static ImageLoad sInstance;
    
    public static ImageLoad instance(Context context) {
        if (sInstance == null) {
            sInstance = new ImageLoad(context.getApplicationContext());
        }
        return sInstance;
    }

    public ImageLoad(Context context) {
        super();
        mContext = context;
        if (!AmdConfig.IMAGELOADER_OFF) {
            File cacheDir = StorageUtils.getOwnCacheDirectory(mContext, "imageloader/Cache");
            DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
            .cacheInMemory(true).cacheOnDisc(true).build();
            
            ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(mContext)
            .defaultDisplayImageOptions(defaultOptions)
            .memoryCache(new LruMemoryCache(AmdConfig.CACHE_SIZE_OF_IMAGELOADER * 1024 * 1024))
            .memoryCacheSize(AmdConfig.CACHE_SIZE_OF_IMAGELOADER * 1024 * 1024)
            .discCacheSize(AmdConfig.CACHE_SIZE_OF_IMAGELOADER * 3 * 1024 * 1024)
            .discCacheFileCount(100)
            .diskCache(new UnlimitedDiskCache(cacheDir))
            .threadPriority(Thread.NORM_PRIORITY - 2)
            .denyCacheImageMultipleSizesInMemory()
            .diskCacheFileNameGenerator(new Md5FileNameGenerator())
            .tasksProcessingOrder(QueueProcessingType.LIFO)
            .writeDebugLogs()
            .build();
            
            ImageLoader.getInstance().init(config);
        }
    }
    
    public static DisplayImageOptions getOptions(Drawable defaultDrawable) {
        return new DisplayImageOptions.Builder()
        .showImageForEmptyUri(defaultDrawable) // 设置图片Uri为空或是错误的时候显示的图片
        .cacheInMemory(true)  //设置下载的图片是否缓存在内存中
        .cacheOnDisk(true)
        .considerExifParams(true)
        .build();
    }
    
    /**
     * TODO: 有一个BUG，就是ImageView可能会被Adapter复用然后出现混乱。
     * @param imageView
     * @param defaultDrawable
     * @param fileNode
     * @return
     */
    public boolean loadBitmap(final ImageView imageView, Drawable defaultDrawable, final FileNode fileNode) {
        boolean retFlag = false;
        if (AmdConfig.IMAGELOADER_OFF) {
            imageView.setImageDrawable(defaultDrawable);
            return retFlag;
        }
        if (fileNode.getFileType() == FileType.IMAGE) {
            if (fileNode.getFile().length() > MAX_SIZE) {
                imageView.setImageDrawable(defaultDrawable);
            } else {
                ImageLoader.getInstance().displayImage("file://" + fileNode.getFilePath(),
                        imageView, getOptions(defaultDrawable), null);
                retFlag = true;
            }
        } else {
            if (fileNode.getThumbnailPath() == null) {
                imageView.setImageDrawable(defaultDrawable);
            } else {
                ImageLoader.getInstance().displayImage("file://" + fileNode.getThumbnailPath(),
                        imageView, getOptions(defaultDrawable), null);
                retFlag = true;
            }
        }
        return retFlag;
    }
    
    public void loadImageBitmap(final ImageView imageView, Drawable defaultDrawable,
    		FileNode fileNode, ImageLoadingListener listener) {
        if (AmdConfig.IMAGELOADER_OFF) {
            imageView.setImageDrawable(defaultDrawable);
            return;
        }
        if (fileNode.getFileType() == FileType.IMAGE) {
            if (fileNode.getFile().length() > MAX_SIZE) {
                imageView.setImageDrawable(defaultDrawable);
            } else {
                ImageLoader.getInstance().displayImage("file://" + fileNode.getFilePath(),
                        imageView, getOptions(defaultDrawable), listener);
            }
        }
    }
    
    public boolean displayImage(ImageView imageView, Drawable defaultDrawable,
            FileNode fileNode, ImageLoadingListener listener) {
        boolean retFlag = false;
        String showFilePath = fileNode.getFileType() == FileType.IMAGE ?
                fileNode.getFilePath() : fileNode.getThumbnailPath();
        if (showFilePath == null || (new File(showFilePath)).length() > MAX_SIZE) {
            imageView.setImageDrawable(defaultDrawable);
        } else {
            ImageLoader.getInstance().displayImage("file://" + fileNode.getFilePath(),
                    imageView, getOptions(defaultDrawable), listener);
            retFlag = true;
        }
        return retFlag;
    }

}
