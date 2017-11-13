package com.haoke.bean;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.haoke.util.GsonUtil;

public class UserBean {
    @SerializedName("id") @Expose private int id;
    @SerializedName("username") @Expose private String username;
    @SerializedName("active") @Expose private int active;
    @SerializedName("permission") @Expose private int permission;
    
    public String toGsonString() {
        return GsonUtil.instance().getJsonFromObject(this);
    }

    @Override
    public String toString() {
        return "UserBean [id=" + id + ", username=" + username + ", active="
                + active + ", permission=" + permission + "]";
    }

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public int getActive() {
		return active;
	}

	public void setActive(int active) {
		this.active = active;
	}

	public int getPermission() {
		return permission;
	}

	public void setPermission(int permission) {
		this.permission = permission;
	}
    
}
