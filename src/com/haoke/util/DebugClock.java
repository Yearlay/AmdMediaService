package com.haoke.util;

import com.haoke.constant.DebugConstant;


public class DebugClock {
    private long mStartTime;
    
    public long getStartTime() {
        return mStartTime;
    }

    public DebugClock() {
        if (!DebugConstant.DEBUG) return;
        mStartTime = System.currentTimeMillis();
    }
    
    public DebugClock(long startTime) {
        if (!DebugConstant.DEBUG) return;
        this.mStartTime = startTime;
    }
    
    public void markTime() {
        if (!DebugConstant.DEBUG) return;
        mStartTime = System.currentTimeMillis();
    }
    
    public long calculateTime(String tags, String runningInfo) {
        if (!DebugConstant.DEBUG) return 0;
        long takingTime = System.currentTimeMillis() - mStartTime;
        DebugLog.i(tags, runningInfo + " taking time: " + takingTime + "ms");
        return takingTime;
    }
}
