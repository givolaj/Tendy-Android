package com.atn.tendy.notifications;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.RemoteViews;

import com.atn.tendy.Dtos;
import com.atn.tendy.MainActivity;
import com.atn.tendy.R;
import com.atn.tendy.utils.Logs;
import com.atn.tendy.utils.Utils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.nostra13.universalimageloader.core.ImageLoader;

public class TheFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "notifications_service";
    public final static int INVITE_NOTIFICATION_ID = 111;
    public final static int DISCOVERY_NOTIFICATION_ID = 112;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) { //need to check if is different user also
            return;
        }

        if (remoteMessage.getData() != null) {
            final Dtos.ChatMessage msg = new Dtos.ChatMessage(remoteMessage.getData().get("data"));
            sendNotification(msg);
        }
    }

    private void sendNotification(final Dtos.ChatMessage msg) {
        if (msg != null) {
            Logs.log("msg", msg.toJsonString());
            Intent intent = new Intent("chatMessage");
            LocalBroadcastManager.getInstance(TheFirebaseMessagingService.this).sendBroadcast(intent);
            FirebaseDatabase.getInstance().getReference("chatPartners")
                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .child(msg.sender)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Dtos.ChatPartner partner = dataSnapshot.getValue(Dtos.ChatPartner.class);
                            if (partner == null || partner.status == null || partner.partner == null) return;
                            Dtos.Profile profile = new Dtos.Profile(partner.partner);
                            if (partner.status.equals(Dtos.ChatPartner.STATE_BLOCKED)) return;
                            if (PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                                    .getString("currentChatPartner", "").equals(profile.identifier) &&
                                    !msg.key.equals("")) {
                                return;
                            }
                            Intent intent = new Intent(TheFirebaseMessagingService.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.putExtra("chatMessage", true);
                            intent.putExtra("buddy", profile.toJsonString());
                            intent.putExtra("partner", partner.toJsonString());
                            intent.putExtra("msg", msg.toJsonString());
                            final PendingIntent pendingIntent = PendingIntent.getActivity(TheFirebaseMessagingService.this, 0, intent,
                                    PendingIntent.FLAG_UPDATE_CURRENT);
                            RemoteViews _remoteViews = null;
                            int _notificationId = 0;
                            if (partner.status.equals(Dtos.ChatPartner.STATE_INVITED)) {
                                _remoteViews = getInviteNotificationView(profile);
                                _notificationId = INVITE_NOTIFICATION_ID;
                            }
                            else {
                                _remoteViews = getMessageNotificationView(msg, profile);
                                _notificationId = Utils.uniqueNumberFromString(msg.sender);
                            }
                            if(msg.key.equals(""))
                                _notificationId = DISCOVERY_NOTIFICATION_ID;
                            final RemoteViews remoteViews = _remoteViews;
                            final int notificationId = _notificationId;
                            Bitmap bitmap = ImageLoader.getInstance().loadImageSync(profile.imageUrl, Utils.displayImageOptions);
                            if(bitmap != null){
                                remoteViews.setImageViewBitmap(R.id.profileImage, Utils.getCircleBitmap(bitmap));
                            }
                            Utils.sendNotification(TheFirebaseMessagingService.this, remoteViews, remoteViews, remoteViews, pendingIntent, notificationId, true);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Logs.log(databaseError.getMessage());
                        }
                    });
        }
    }

    private RemoteViews getInviteNotificationView(Dtos.Profile profile) {
        RemoteViews remoteViews = new RemoteViews(getPackageName(),
                R.layout.chat_message_notification);
        remoteViews.setTextViewText(R.id.title, profile.username);
        remoteViews.setTextViewText(R.id.subtitle, getString(R.string.invites_you_to_chat));
        return remoteViews;
    }

    private RemoteViews getMessageNotificationView(Dtos.ChatMessage msg, Dtos.Profile profile) {
        final RemoteViews remoteViews = new RemoteViews(getPackageName(),
                R.layout.chat_message_notification);
        remoteViews.setTextViewText(R.id.title, profile.username);
        if (!msg.text.equals("")) {
            remoteViews.setTextViewText(R.id.subtitle, msg.text);
        } else {
            remoteViews.setTextViewText(R.id.subtitle,
                    String.format(getString(R.string.just_sent_image),
                            profile.username,
                            (profile.gender.equals("man") ? getString(R.string.maleSent) : getString(R.string.femaleSent))));
        }
        return remoteViews;
    }


}
