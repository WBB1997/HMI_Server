package com.wubeibei.hmi_server.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.wubeibei.hmi_server.MainActivity;


public class SelfBootReceiver extends BroadcastReceiver {
    private static final String TAG = "SelfBootReceiver";
    private static final String ACTION = "android.intent.action.BOOT_COMPLETED";
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(ACTION)){
            Intent i = new Intent(context, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }
}
