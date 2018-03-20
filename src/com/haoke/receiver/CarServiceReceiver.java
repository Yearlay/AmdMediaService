/*
*describe:此类用于接收服务重启消息
*author:林永彬
*date:2016.10.12
*/

package com.haoke.receiver;

import com.amd.bt.BT_IF;
import com.haoke.define.GlobalDef;
import com.haoke.util.DebugLog;
import com.haoke.util.Media_IF;
import com.amd.radio.Radio_IF;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CarServiceReceiver extends BroadcastReceiver {
	
	private final String TAG = this.getClass().getSimpleName();
	
	@Override
	public void onReceive(Context arg0, Intent intent) {
		// TODO Auto-generated method stub
		DebugLog.v(TAG, "onReceive intent="+intent.getAction());	
		if (intent.getAction().equals(GlobalDef.CAR_SERVICE_ACTION_REBOOT)) {
			Media_IF.getInstance().bindCarService(); // 重新绑定
			Radio_IF.getInstance().bindCarService(); // 重新绑定
			
		} else if (intent.getAction().equals(GlobalDef.BT_SERVICE_ACTION_REBOOT)) {
			BT_IF.getInstance().bindBTService(); // 重新绑定
		}
	}
}
