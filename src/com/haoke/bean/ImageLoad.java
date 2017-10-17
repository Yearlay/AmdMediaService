package com.haoke.bean;

import com.haoke.constant.MediaUtil.FileType;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import android.content.Context;
import android.widget.ImageView;

public class ImageLoad {
    private static final int MAX_SIZE =  50 * 1024 * 1024;
    private Context mContext;
    
    private static ImageLoad sInstance;
    
    public static ImageLoad instance(Context context) {
        if (sInstance == null) {
            sInstance = new ImageLoad(context);
        }
        return sInstance;
    }

    public ImageLoad(Context context) {
        super();
        mContext = context;
        
        ImageLoaderConfiguration.Builder config = new ImageLoaderConfiguration.Builder(mContext);
        config.threadPriority(Thread.NORM_PRIORITY - 2);
        config.denyCacheImageMultipleSizesInMemory();
        config.diskCacheFileNameGenerator(new Md5FileNameGenerator());
        config.diskCacheSize(MAX_SIZE); // 50 MiB
        config.tasksProcessingOrder(QueueProcessingType.LIFO);
        ImageLoader.getInstance().init(config.build());
    }
    
    public ImageLoader getImageLoader() {
        return ImageLoader.getInstance();
    }
    
    public static DisplayImageOptions getOptions(int defaultId) {
        return new DisplayImageOptions.Builder()
        .showImageForEmptyUri(defaultId) // 设置图片Uri为空或是错误的时候显示的图片
        .cacheInMemory(true)  //设置下载的图片是否缓存在内存中
        .cacheOnDisk(true)
        .considerExifParams(true)
        .build();
    }
    
    public void loadBitmap(final ImageView imageView, int defaultId, final FileNode fileNode) {
        if (fileNode.getFileType() == FileType.IMAGE) {
            if (fileNode.getFile().length() > MAX_SIZE) {
                imageView.setImageResource(defaultId);
            } else {
                ImageLoader.getInstance().displayImage("file://" + fileNode.getFilePath(),
                        imageView, getOptions(defaultId), null);
            }
        } else {
            if (fileNode.getThumbnailPath() == null) {
                imageView.setImageResource(defaultId);
            } else {
                ImageLoader.getInstance().displayImage("file://" + fileNode.getThumbnailPath(),
                        imageView, getOptions(defaultId), null);
            }
        }
    }
    
    public void loadImageBitmap(final ImageView imageView, int defaultId,
    		FileNode fileNode, ImageLoadingListener listener) {
        if (fileNode.getFileType() == FileType.IMAGE) {
            if (fileNode.getFile().length() > MAX_SIZE) {
                imageView.setImageResource(defaultId);
            } else {
                ImageLoader.getInstance().displayImage("file://" + fileNode.getFilePath(),
                        imageView, getOptions(defaultId), listener);
            }
        }
    }

}
