package com.file.server.scan;

import com.haoke.bean.FileNode;
import com.haoke.constant.DBConfig;
import com.haoke.constant.MediaUtil;
import com.haoke.define.MediaDef;
import com.haoke.scanner.MediaDbHelper;
import com.haoke.scanner.MediaDbHelper.TransactionTask;
import com.haoke.util.DebugLog;

public class ScanJni {
    static {
        System.loadLibrary("scanjni");
    }
    public native String stringFromJni();
    public static native String getPY(String fileName);
    public native void scanRootPath(String rootPath);
    
    private static final String TAG = "ScanJni";
    public int mFileCount;
    public int mMediaCount;
    
    private MediaDbHelper mMediaDbHelper;
    private boolean parseId3;
    
    
    public ScanJni(MediaDbHelper mediaDbHelper, boolean parseId3) {
        super();
        mMediaDbHelper = mediaDbHelper;
        this.parseId3 = parseId3;
        mFileCount = 0;
        mMediaCount = 0;
    }
    
    public void insertToDb(FileNode fileNode) {
        if (DBConfig.isMediaType(fileNode.getFileType())) {
            mMediaDbHelper.addToNeedToInsertList(new TransactionTask(fileNode, TransactionTask.INSERT_TASK));
            mMediaCount++;
        } else {
            mFileCount++;
        }
    }
}
