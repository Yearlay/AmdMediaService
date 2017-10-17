package com.haoke.ui.image;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import haoke.ui.util.HKTextView;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.haoke.bean.FileNode;
import com.haoke.bean.ImageLoad;
import com.haoke.mediaservice.R;
import com.haoke.window.HKWindowManager;

public class PhotoDetailFragment extends Fragment {
    private View mRootView;
    
    private FileNode mFileNode;
    public FileNode getFileNode() {
        return mFileNode;
    }
    
    public void setFileNode(FileNode fileNode) {
        mFileNode = fileNode;
        updateData(mRootView);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        HKWindowManager.fullScreen(this.getActivity(), false);
        // 初始化控件
        mRootView = inflater.inflate(R.layout.image_fragment_details, null);
        updateData(mRootView);
        return mRootView;
    }
    
    private void updateData(View rootView) {
    	if (rootView != null && mFileNode != null) {
    		HKTextView detailTextView = (HKTextView) mRootView.findViewById(R.id.image_detail_title);
    		HKTextView spaceTextView = (HKTextView) mRootView.findViewById(R.id.image_detail_space);
    		HKTextView rectTextView = (HKTextView) mRootView.findViewById(R.id.image_detail_rect);
    		HKTextView pathTextView = (HKTextView) mRootView.findViewById(R.id.image_detail_path);
    		HKTextView styleTextView = (HKTextView) mRootView.findViewById(R.id.image_detail_style);
    		HKTextView sizeTextView = (HKTextView) mRootView.findViewById(R.id.image_detail_size);
    		HKTextView setTimeTextview = (HKTextView) mRootView.findViewById(R.id.image_detail_settime);
    		detailTextView.setText(mFileNode.getFileName());
    		spaceTextView.setText("null");
    		rectTextView.setText("null");
    		pathTextView.setText(getPath(mFileNode.getFilePath()));
    		styleTextView.setText("null");
    		sizeTextView.setText(getSize(mFileNode.getFile()) + " 字节");
    		setTimeTextview.setText(getLastTime(mFileNode.getFile()));
    		
    		ImageView detailImageView = (ImageView) mRootView.findViewById(R.id.image_detail_icon);
    		detailImageView.setImageResource(R.drawable.image_details_img);
    		ImageLoad.instance(getActivity()).loadBitmap(detailImageView, R.drawable.image_details_img, mFileNode);
    	}
    }
    
    private String getLastTime(File file) {
        StringBuilder builder = new StringBuilder();
        Date date = new Date(file.lastModified());
        String year = new SimpleDateFormat("yyyy/MM/dd").format(date);
        String week = new SimpleDateFormat("EEE").format(date);
        int hour = date.getHours();
        String headHour = hour >= 12 ? "pm" : "am";
        hour = hour >= 12 ? hour - 12 : hour;
        builder.append(year + " ");
        builder.append(week + " ");
        builder.append(headHour + " ");
        builder.append(hour + ":" + date.getMinutes());
        return builder.toString();
    }
    
    //获取文件大小并用", "按千分位分隔
    private String getSize(File file) {
        long length = 0;
        StringBuilder builderSize = new StringBuilder();
        length = file.length();
        builderSize.append(length);
        int inserTime = builderSize.length()/3;
        for (int i = 0; i < inserTime; i++) {
            if (builderSize.length() > (3*(i+1) + 2*i)) {
                builderSize.insert(builderSize.length() - (3*(i+1) + 2*i), ", ");
            }
        }
        return builderSize.toString();
    }
    
    private String getPath(String path) {
        String parentPath = "USB";
        String[] result = path.split("/");
        StringBuilder builder = new StringBuilder();
        if (result != null && "usb_storage1".equals(result[3])) {
            parentPath = "USB2";
            builder.append(parentPath);
        }
        for (int i = 4; i < result.length - 1; i++) {
            builder.append(">");
            builder.append(result[i]);
        }
        return builder.toString();
    }
}
