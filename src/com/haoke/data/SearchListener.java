package com.haoke.data;

import java.util.ArrayList;

import com.haoke.bean.FileNode;

public interface SearchListener {
	public void onSearchCompleted(ArrayList<FileNode> searchList);
}
