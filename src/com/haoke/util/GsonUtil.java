package com.haoke.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonUtil {

    /**
     * 将JavaBean转换成JSON，只会封装对象中不为空的列
     * 
     * @param obj
     * @return JSON string
     */
    private static GsonUtil sGsonUtil;
    
    private Gson mGson;
    
    public static synchronized GsonUtil instance() {
        if (sGsonUtil == null) {
            sGsonUtil = new GsonUtil();
        }
        return sGsonUtil;
    }

    public GsonUtil() {
        super();
        mGson = new GsonBuilder() // Gson 构造器
                .excludeFieldsWithoutExposeAnnotation() // 只解析暴露的对象
                .serializeNulls() // 解析空对象
                .create();
    }

    /**<b>Description: </b></br> 使用<b>Gson库</b>，将数据对象转换成json字符串。 */
    public String getJsonFromObject(Object object) {
        return mGson.toJson(object).toString();
    }
    
    /** <b>Description:</b> </br> 使用<b>Gson库</b>，将json串解析为指定解析对象。 */
    public Object getObjectFromJson(String json, Class<?> parseClass) {
        return mGson.fromJson(json, parseClass);
    }
}
