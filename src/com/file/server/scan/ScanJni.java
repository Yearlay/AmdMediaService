package com.file.server.scan;

import android.text.TextUtils;

import com.haoke.bean.FileNode;
import com.haoke.constant.DBConfig;
import com.haoke.scanner.MediaDbHelper;
import com.haoke.scanner.MediaDbHelper.TransactionTask;

public class ScanJni {
    static {
        System.loadLibrary("scanjni");
    }
    public native String stringFromJni();
    public static native String getPY(String fileName);
    public native int scanRootPath(String rootPath, int onlyGetMediaSizeFlag);
    
    private static final String TAG = "ScanJni";
    
    private MediaDbHelper mMediaDbHelper;
    
    public ScanJni(MediaDbHelper mediaDbHelper) {
        super();
        mMediaDbHelper = mediaDbHelper;
    }
    
    public void insertToDb(FileNode fileNode) {
        if (DBConfig.isMediaType(fileNode.getFileType())) {
            mMediaDbHelper.addToNeedToInsertList(new TransactionTask(fileNode, TransactionTask.INSERT_TASK));
        }
    }
    
    public static String getPYEx(String hz) {
        if (TextUtils.isEmpty(hz)) {
            return "";
        }
        String str = null;
        if (hz.length() > 300) {
            str = hz.substring(0, 300);
        } else {
            str = hz;
        }
        return getPY(str);
    }
}
