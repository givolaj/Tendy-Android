package com.atn.tendy.discovery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DiscoveryServiceRecoveryReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, BackgroundDiscoveryService.class));
    }
}
