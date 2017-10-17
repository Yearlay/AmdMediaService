package com.haoke.bean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.haoke.util.DebugLog;
import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;

public class ID3Parse {
	public interface ID3ParseListener {
		public void onID3ParseComplete(Object object, FileNode fileNode);
	}
	
    private static final String TAG = "ID3Parse";
    
    private static ID3Parse sID3Parse;
    
    public static synchronized ID3Parse instance() {
        if (sID3Parse == null) {
            sID3Parse = new ID3Parse();
        }
        return sID3Parse;
    }
    private ID3Parse() {
        mLoadHandler = new LoadHandler();
    }
    
    public void parseID3(Object object, FileNode fileNode, ID3ParseListener listener) {
        if (fileNode != null && fileNode.getParseId3() == 0) {
        	mLoadHandler.sendMessage(mLoadHandler.obtainMessage(LOADING_BEGIN, 
        			new LoadData(object, fileNode, listener)));
        }
    }
    
    private LoadHandler mLoadHandler; // 用于多线程通信的Handler对象。
    private LoadThread mLoadThread;   // 加载线程
    
    private static final int LOADING_BEGIN = 1;
    private static final int LOADING_END = 2;
    private static final int RELEASE_THREAD = 3;
    
    @SuppressLint("HandlerLeak")
    class LoadHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
            case LOADING_BEGIN:
                if (mLoadThread == null) {
                    mLoadThread = new LoadThread();
                }
                mLoadThread.addToListAndStart((LoadData)msg.obj);
                break;
            case LOADING_END:
                LoadData loadData = (LoadData) msg.obj;
                loadData.listener.onID3ParseComplete(loadData.object, loadData.fileNode);
                break;
            case RELEASE_THREAD:
                mLoadThread = null;
                break;
            default:
                break;
            }
        }
    };
    
    private class LoadData {
        FileNode fileNode;
        Object object;
        ID3ParseListener listener;
        public LoadData(Object object, FileNode fileNode, ID3ParseListener listener) {
            this.object = object;
            this.fileNode = fileNode;
            this.listener = listener;
        }
    }
    
    public class LoadThread extends Thread {
        List<LoadData> mLoadMsgList = Collections.synchronizedList(new ArrayList<LoadData>());
        volatile boolean isRunning;
        
        public void addToListAndStart(LoadData data) {
            if (mLoadMsgList.size() > 1) {
                mLoadMsgList.add(1, data);
            } else {
                mLoadMsgList.add(data);
            }
            if (!isRunning) {
                isRunning = true; // 防止非常快速地调用两次addToListAndStart（来不及调用run方法）。
                try {
                    start();
                } catch (Exception e) {
                }
            }
        }
        
        @Override
        public void run() {
            try {
                while (mLoadMsgList.size() > 0) {
                    LoadData loaddata = mLoadMsgList.remove(0);
                    FileNode fileNode = loaddata.fileNode;
                    fileNode.parseID3Info();
                    mLoadHandler.sendMessage(mLoadHandler.obtainMessage(LOADING_END, loaddata));
                }
            } catch (Exception e) {
                DebugLog.e(TAG, "LoadThread running exception: " + e.toString());
            }
            isRunning = false;
            mLoadHandler.sendEmptyMessage(RELEASE_THREAD);
        }
    }
}
