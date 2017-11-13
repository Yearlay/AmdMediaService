package com.haoke.bean;

import java.util.ArrayList;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.haoke.util.GsonUtil;

public class UserInfoBean {
    @SerializedName("cur_user") @Expose private String currentUsername;
    @SerializedName("cur_id") @Expose private int currentUserId;
    @SerializedName("cur_per") @Expose private int currentPer;
    @SerializedName("user_count") @Expose private int userCount;
    @SerializedName("user_details") @Expose ArrayList<UserBean> userList;
    
    public String toGsonString() {
        return GsonUtil.instance().getJsonFromObject(this);
    }

	@Override
	public String toString() {
		String details = "[ ";
		if (userList != null) {
			for (UserBean userBean : userList) {
				if (userBean != null) {
					details = details + userBean.toString() + ",";
				}
			}
		}
		details = details + " ] ";
		return "UserInfoBean [cur_user=" + currentUsername + ", cur_id=" + currentUserId
				+ ", cur_per=" + currentPer + ", user_count=" + userCount
				+ ", user_details=" + details + "]";
	}

	public String getCurrentUsername() {
		return currentUsername;
	}

	public void setCurrentUsername(String currentUsername) {
		this.currentUsername = currentUsername;
	}

	public int getCurrentUserId() {
		return currentUserId;
	}

	public void setCurrentUserId(int currentUserId) {
		this.currentUserId = currentUserId;
	}

	public int getCurrentPer() {
		return currentPer;
	}

	public void setCurrentPer(int currentPer) {
		this.currentPer = currentPer;
	}

	public int getUserCount() {
		return userCount;
	}

	public void setUserCount(int userCount) {
		this.userCount = userCount;
	}

	public ArrayList<UserBean> getUserList() {
		return userList;
	}

	public void setUserList(ArrayList<UserBean> userList) {
		this.userList = userList;
	}
	
	
}
