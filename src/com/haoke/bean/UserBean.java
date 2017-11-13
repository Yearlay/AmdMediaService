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
    
}
