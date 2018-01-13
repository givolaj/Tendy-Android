package com.atn.tendy.discovery;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.atn.tendy.R;

import static com.atn.tendy.discovery.BackgroundDiscoveryService.NOTIFICATION_ID;

public class ForegroundNotificationService extends Service {
    public ForegroundNotificationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void setupNotification() {
        NotificationCompat.Builder b = new NotificationCompat.Builder(this);
        b.setOngoing(true)
                .setContentTitle(getString(R.string.serviceNotificationTitle))
                .setContentText(getString(R.string.serviceNotificationContent))
                .setSmallIcon(R.drawable.ic_tendy_icon)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(getString(R.string.serviceNotificationContent)))
                .setPriority(NotificationCompat.PRIORITY_LOW);
        startForeground(NOTIFICATION_ID, b.build());
    }

    private void stopNotification() {
        stopForeground(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        setupNotification();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopNotification();
        super.onDestroy();
    }
}
