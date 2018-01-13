package com.atn.tendy.discovery;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.RemoteViews;

import com.atn.tendy.Dtos;
import com.atn.tendy.MainActivity;
import com.atn.tendy.R;
import com.atn.tendy.utils.Utils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import ch.uepaa.p2pkit.P2PKit;
import ch.uepaa.p2pkit.P2PKitStatusListener;
import ch.uepaa.p2pkit.StatusResult;
import ch.uepaa.p2pkit.discovery.DiscoveryListener;
import ch.uepaa.p2pkit.discovery.DiscoveryPowerMode;
import ch.uepaa.p2pkit.discovery.Peer;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

public class BackgroundDiscoveryService extends Service {
    static long DISCOVERY_INTERVALS = 45 * 1000;
    SharedPreferences prefs;
    final static int NOTIFICATION_ID = 123459876;
    IBinder mBinder = new ServiceBinder();

    public class ServiceBinder extends Binder {
        public BackgroundDiscoveryService getService() {
            return BackgroundDiscoveryService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public BackgroundDiscoveryService() {
    }

    @Override
    public void onCreate() {
        prefs = getDefaultSharedPreferences(this);
    }

    @Override
    public void onDestroy() {
        cleanup();
        super.onDestroy();
    }

    public void cleanup() {
        try {
            if (P2PKit.isEnabled()) {
                P2PKit.stopDiscovery();
            }
        } catch (Throwable e) {
            //e.printStackTrace();
        }
    }

    public void setTimer(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, DiscoveryServiceRecoveryReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        boolean isActive = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("isAppActive", false);
        long timeToLaunch = DISCOVERY_INTERVALS;
        if (!isActive)
            timeToLaunch = (long) (DISCOVERY_INTERVALS * 3.5);
        if (Build.VERSION.SDK_INT >= 23) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + timeToLaunch, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + timeToLaunch, pi);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        setTimer(this);
        discover();
        startService(new Intent(getApplicationContext(), ForegroundNotificationService.class));
        setupNotification();
        Utils.runWithDelay(new Runnable() {
            @Override
            public void run() {
                stopService(new Intent(getApplicationContext(), ForegroundNotificationService.class));
            }
        }, 1500);
        return START_STICKY;
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

    private final P2PKitStatusListener mStatusListener = new P2PKitStatusListener() {
        @Override
        public void onEnabled() {

        }

        @Override
        public void onDisabled() {

        }

        @Override
        public void onError(StatusResult statusResult) {

        }

        @Override
        public void onException(Throwable throwable) {

        }
    };

    private final DiscoveryListener mDiscoveryListener = new DiscoveryListener() {
        @Override
        public void onStateChanged(final int state) {

        }

        @Override
        public void onPeerDiscovered(final Peer peer) {
            addProfileToProfilesList(new String(peer.getDiscoveryInfo()));
        }

        @Override
        public void onPeerLost(final Peer peer) {
        }

        @Override
        public void onPeerUpdatedDiscoveryInfo(Peer peer) {
            addProfileToProfilesList(new String(peer.getDiscoveryInfo()));
        }

        @Override
        public void onProximityStrengthChanged(Peer peer) {

        }
    };


    boolean shouldSendNotification = false;

    synchronized private void addProfileToProfilesList(final String readMessage) {
        if (readMessage == null || readMessage.isEmpty()) return;
        try {
            final Dtos.Profile me = new Dtos.Profile(prefs.getString("profile", ""));
            final String chatId = Utils.getChatId(me.identifier, readMessage);
            if (FirebaseAuth.getInstance().getCurrentUser() != null &&
                    FirebaseAuth.getInstance().getCurrentUser().getUid().equals(readMessage))
                return;
            FirebaseDatabase.getInstance().getReference("chats").child(chatId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getValue() == null) {
                        FirebaseDatabase.getInstance().getReference("profiles").child(readMessage).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                Dtos.Profile profile = dataSnapshot.getValue(Dtos.Profile.class);
                                if (profile == null || profile.username == null) return;
                                try {
                                    shouldSendNotification = true;
                                    JSONArray arr = new JSONArray(prefs.getString("discoveryArray", "[]"));
                                    JSONObject o = new JSONObject(profile.toJsonString());
                                    o.put("dateAdded", Utils.getUnifiedTime());
                                    for (int i = 0; i < arr.length(); i++) {
                                        if (arr.getJSONObject(i).getString("identifier").equals(o.getString("identifier"))) {
                                            arr.remove(i);
                                            shouldSendNotification = false;
                                            break;
                                        }
                                    }
                                    arr.put(o);
                                    prefs.edit().putString("discoveryArray", arr.toString()).commit();
                                    if (shouldSendNotification)
                                        sendAroundYouNotification(arr);
                                    try {
                                        Intent intent = new Intent("discovery");
                                        LocalBroadcastManager.getInstance(BackgroundDiscoveryService.this).sendBroadcast(intent);
                                    } catch (Throwable e) {
                                        e.printStackTrace();
                                    }
                                } catch (Throwable e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        } catch (Throwable e) {
            FirebaseCrash.report(new Exception("got identifier: " + readMessage + " not in db"));
        }
    }

    final static public int DISCOVERY_NOTIFICATION_ID = 1;

    synchronized private void sendAroundYouNotification(JSONArray arr) {
        if (arr.length() == 0) return;
        if (prefs.getBoolean("isAppActive", true)) return;
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        final RemoteViews remoteViews = getRemoteViewsForDiscoveryNotification(arr);
        Utils.sendNotification(this, remoteViews, remoteViews, remoteViews, pendingIntent, DISCOVERY_NOTIFICATION_ID, true);
    }

    private RemoteViews getRemoteViewsForDiscoveryNotification(JSONArray arr) {
        RemoteViews remoteViews = new RemoteViews(getPackageName(),
                R.layout.discovery_notification);
        remoteViews.setTextViewText(R.id.title, String.format(getString(R.string.tendi_found_1_s_people_around_you), arr.length() + ""));
        return remoteViews;
    }

    private void discover() {
        try {
            boolean isActive = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("isAppActive", false);
            final DiscoveryPowerMode mode = isActive ? DiscoveryPowerMode.HIGH_PERFORMANCE : DiscoveryPowerMode.LOW_POWER;
            boolean isOn = prefs.getBoolean("discoveryState", true);
            if (isOn && Utils.haveInternetConnection(this)) {
                final String profileJson = prefs.getString("profile", "");
                if (FirebaseAuth.getInstance().getCurrentUser() != null &&
                        BackgroundDiscoveryService.this != null &&
                        getApplicationContext() != null) {
                    if (!P2PKit.isEnabled())
                        P2PKit.enable(getApplicationContext(), getString(R.string.p2pkit_api_key), mStatusListener);
                    P2PKit.stopDiscovery();
                    if (Dtos.Profile.isValidProfile(profileJson) && P2PKit.isEnabled())
                        P2PKit.startDiscovery(new Dtos.Profile(profileJson).identifier.getBytes(), mode, mDiscoveryListener);
                }
            } else {
                try {
                    if(!isOn) {
                        if(P2PKit.isEnabled())
                            P2PKit.stopDiscovery();
                    }
                } catch (Throwable e) {}
            }
        } catch (Throwable e) {

        }
    }


}
