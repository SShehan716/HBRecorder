package com.ss.Misty_Screen_Recoder_lite;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SmartRecordingReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("SMART_RECORDING_ACTION".equals(intent.getAction())) {
            int requestId = intent.getIntExtra("request_id", -1);
            if (requestId != -1) {
                // Forward to the fragment through a local broadcast
                Intent localIntent = new Intent("SMART_RECORDING_ACTION");
                localIntent.putExtra("request_id", requestId);
                context.sendBroadcast(localIntent);
            }
        }
    }
}
