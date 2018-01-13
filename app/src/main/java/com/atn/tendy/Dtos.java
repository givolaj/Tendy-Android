package com.atn.tendy;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.atn.tendy.utils.Logs;
import com.atn.tendy.utils.PushServer;
import com.atn.tendy.utils.Utils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import org.json.JSONObject;

import java.util.Comparator;

/**
 * Created by Admin on 11/07/2017.
 */

public class Dtos {
    public static class Profile {
        public String username,
                age,
                profession,
                something,
                gender,
                imageUrl,
                lastEntry,
                identifier,
                pushToken,
                deviceType;

        public Profile(String username, String age, String profession, String something, String gender, String imageUrl, String hardwareAddress, String lastEntry, String identifier, String pushToken) {
            this.username = username;
            this.age = age;
            this.profession = profession;
            this.something = something;
            this.gender = gender;
            this.imageUrl = imageUrl;
            this.lastEntry = lastEntry;
            this.identifier = identifier;
            this.pushToken = pushToken;
            this.deviceType = "android";
        }

        public Profile() {
        }

        public Profile(String json) {
            try {
                JSONObject o = new JSONObject(json);
                this.username = o.getString("username");
                this.age = o.getString("age");
                this.profession = o.getString("profession");
                this.something = o.getString("something");
                this.gender = o.getString("gender");
                this.imageUrl = o.getString("imageUrl");
                this.lastEntry = o.getString("lastEntry");
                this.identifier = o.getString("identifier");
                this.pushToken = o.getString("pushToken");
                try {
                    this.deviceType = o.getString("deviceType");
                } catch (Exception e){e.printStackTrace(); this.deviceType = "android";}
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public String toString() {
            return "Profile{" +
                    "username='" + username + '\'' +
                    ", age='" + age + '\'' +
                    ", profession='" + profession + '\'' +
                    ", something='" + something + '\'' +
                    ", gender='" + gender + '\'' +
                    ", imageUrl='" + imageUrl + '\'' +
                    ", lastEntry='" + lastEntry + '\'' +
                    ", identifier='" + identifier + '\'' +
                    ", pushToken='" + pushToken + '\'' +
                    '}';
        }

        public String toJsonString() {
            try {
                JSONObject o = new JSONObject();
                o.put("username", username);
                o.put("age", age);
                o.put("profession", profession);
                o.put("something", something);
                o.put("gender", gender);
                o.put("imageUrl", imageUrl);
                o.put("lastEntry", lastEntry);
                o.put("identifier", identifier);
                o.put("pushToken", pushToken);
                try {
                    o.put("deviceType", deviceType);
                } catch (Exception e){e.printStackTrace(); o.put("deviceType", "android");}
                return o.toString();
            } catch (Exception e) {
                return "{error: 'toJsonString failed:" + e.toString() + "' }";
            }
        }

        public static boolean isValidProfile(String json) {
            try {
                JSONObject o = new JSONObject(json);
                return o.has("username") &&
                        o.has("age") &&
                        o.has("profession") &&
                        o.has("something") &&
                        o.has("gender") &&
                        o.has("imageUrl") &&
                        o.has("lastEntry") &&
                        o.has("identifier") &&
                        o.has("pushToken");
            } catch (Exception e) {
                return false;
            }
        }

        public View setupDiscoveryItem(Context context, View item) {
            if (item == null)
                item = View.inflate(context, R.layout.discovery_list_item, null);
            DiscoveryItemPlaceHolder holder;
            if (item.getTag() != null)
                holder = (DiscoveryItemPlaceHolder) item.getTag();
            else {
                holder = new DiscoveryItemPlaceHolder();
                holder.profileImage = (ImageView) item.findViewById(R.id.profileImage);
                holder.title = (TextView) item.findViewById(R.id.title);
                holder.subtitle1 = (TextView) item.findViewById(R.id.subtitle1);
                holder.subtitle2 = (TextView) item.findViewById(R.id.subtitle2);
                holder.date = (TextView) item.findViewById(R.id.date);
                item.setTag(holder);
                Utils.setFontToViewGroup(context, (ViewGroup) item, "open");
            }
            ImageLoader.getInstance().displayImage(imageUrl, holder.profileImage, Utils.displayImageOptions);
            holder.title.setText(username);
            String sub1 = gender.equals("man") ? context.getString(R.string.male) : (gender.equals("woman") ? context.getString(R.string.female) : context.getString(R.string.other));
            if (age != null && !age.isEmpty())
                sub1 += " | " + age;
            if (profession != null && !profession.isEmpty())
                sub1 += " | " + profession;
            holder.subtitle1.setText(sub1);
            holder.subtitle2.setText(something);
            holder.date.setText(lastEntry);
            return item;
        }

        public class DiscoveryItemPlaceHolder {
            ImageView profileImage;
            TextView title, subtitle1, subtitle2, date;
        }

        public static Comparator<? super JSONObject> comparator = new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject o1, JSONObject o2) {
                Profile p1 = new Profile(o1.toString());
                Profile p2 = new Profile(o2.toString());
                return p1.username.compareTo(p2.username);
            }
        };
    }


    public static class ChatMessage {
        private static final float CELL_PADDING = 8.5f;
        public static String wantsToStayInTouch1 = "wants to stay in touch";
        public static String wantsToStayInTouch2 = "רוצה להשאר בקשר";
        public String imageUrl, text, key, dateAdded, sender, deleted;

        public ChatMessage(String imageUrl, String text, String key, String dateAdded, String sender) {
            this.imageUrl = imageUrl;
            this.text = text;
            this.key = key;
            this.dateAdded = dateAdded;
            this.sender = sender;
            this.deleted = "";
        }

        public ChatMessage() {
            text = "";
            key = "";
            dateAdded = Utils.getUnifiedTime() + "";
            imageUrl = "";
            sender = "";
            deleted = "";
        }

        public ChatMessage(String json) {
            try {
                JSONObject o = new JSONObject(json);
                this.text = o.getString("text");
                this.key = o.getString("key");
                this.dateAdded = o.getString("dateAdded");
                this.imageUrl = o.getString("imageUrl");
                this.sender = o.getString("sender");
                this.deleted = o.getString("deleted");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public String toString() {
            return "ChatMessage{" +
                    "imageUrl='" + imageUrl + '\'' +
                    ", text='" + text + '\'' +
                    ", key='" + key + '\'' +
                    ", dateAdded='" + dateAdded + '\'' +
                    ", sender='" + sender + '\'' +
                    ", deleted='" + deleted + '\'' +
                    '}';
        }

        public String toJsonString() {
            try {
                JSONObject o = new JSONObject();
                o.put("text", text);
                o.put("key", key);
                o.put("dateAdded", dateAdded);
                o.put("imageUrl", imageUrl);
                o.put("sender", sender);
                o.put("deleted", deleted);
                return o.toString();
            } catch (Exception e) {
                return "{error: 'toJsonString failed:" + e.toString() + "' }";
            }
        }

        public View setupMessageItem(final Context context, View item, final Profile myProfile) {
            Logs.log("chatMessage", toJsonString());
            if (item == null)
                item = View.inflate(context, R.layout.chat_messages_list_item, null);
            ChatMessageItemPlaceHolder holder;
            if (item.getTag() != null)
                holder = (ChatMessageItemPlaceHolder) item.getTag();
            else {
                holder = new ChatMessageItemPlaceHolder();
                holder.image = (ImageView) item.findViewById(R.id.image);
                holder.text = (TextView) item.findViewById(R.id.text);
                holder.date = (TextView) item.findViewById(R.id.date);
                holder.content = (LinearLayout) item.findViewById(R.id.content);
                item.setTag(holder);
                Utils.setFontToViewGroup(context, (ViewGroup) item, "open");
            }
            if (deleted.contains(myProfile.identifier)) {
                holder.text.setVisibility(View.VISIBLE);
                holder.image.setVisibility(View.GONE);
                holder.text.setText(context.getString(R.string.deleted_message));
            } else if (imageUrl != null && !imageUrl.equals("")) {
                holder.text.setVisibility(View.GONE);
                holder.image.setVisibility(View.VISIBLE);
                final ChatMessageItemPlaceHolder _holder = holder;
                ImageLoader.getInstance().displayImage(imageUrl, holder.image, Utils.displayImageOptions, new ImageLoadingListener() {
                    @Override
                    public void onLoadingStarted(String imageUri, View view) {

                    }

                    @Override
                    public void onLoadingFailed(String imageUri, View view, FailReason failReason) {

                    }

                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {

                        final Bitmap bm = loadedImage;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Utils.saveImageToExternal(context, key, bm);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                        _holder.image.setImageBitmap(Utils.resizeBitmap(loadedImage, 256));
                    }

                    @Override
                    public void onLoadingCancelled(String imageUri, View view) {

                    }
                });
            } else {
                holder.text.setVisibility(View.VISIBLE);
                holder.image.setVisibility(View.GONE);
                holder.text.setText(text);
            }

            holder.date.setText(DateUtils.getRelativeTimeSpanString(Utils.parseLong(dateAdded) - PushServer.timeDiff - 10000));

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.content.getLayoutParams();
            if (!myProfile.identifier.equals(sender)) {
                holder.content.setBackgroundResource(R.drawable.chat_bubble_partner);
                params.removeRule(RelativeLayout.ALIGN_PARENT_END);
                params.addRule(RelativeLayout.ALIGN_PARENT_START);
                holder.content.setLayoutParams(params);
            } else {
                holder.content.setBackgroundResource(R.drawable.chat_bubble_me);
                params.removeRule(RelativeLayout.ALIGN_PARENT_START);
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                holder.content.setLayoutParams(params);
            }
            if (text.contains(wantsToStayInTouch1) || text.contains(wantsToStayInTouch2)) {
                holder.content.setBackgroundResource(R.drawable.chat_bubble_special);
                ScaleAnimation anim = new ScaleAnimation(0.99f, 1.01f, 0.99f, 1.01f);
                anim.setInterpolator(new LinearInterpolator());
                anim.setRepeatCount(Animation.INFINITE);
                anim.setRepeatMode(Animation.REVERSE);
                anim.setDuration(1000);
                item.setAnimation(anim);
            } else {
                item.clearAnimation();
            }
            float scale = context.getResources().getDisplayMetrics().density;
            int dpAsPixels = (int) (CELL_PADDING * scale + 0.5f);
            holder.content.setPadding(dpAsPixels, dpAsPixels, dpAsPixels, dpAsPixels);

            return item;
        }

        public class ChatMessageItemPlaceHolder {
            ImageView image;
            TextView text, date;
            LinearLayout content;
        }

        public static Comparator<? super JSONObject> comparator = new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject o1, JSONObject o2) {
                ChatMessage m1 = new ChatMessage(o1.toString());
                ChatMessage m2 = new ChatMessage(o2.toString());
                Long l1 = Utils.parseLong(m1.dateAdded);
                Long l2 = Utils.parseLong(m2.dateAdded);
                return l1.compareTo(l2);
            }
        };
    }

    public static class ChatPartner {
        public static final long CHAT_VALIDITY_INTERVAL = Logs.DEBUG_MODE ? 3 * 60 * 1000 : 2 * 60 * 60 * 1000;
        public static final String
                STATE_HAVE_INVITED = "haveInvited",
                STATE_INVITED = "invited",
                STATE_CONNECTED = "connected",
                STATE_FOREVER = "forever",
                STATE_BLOCKED = "blocked";

        public String partner, status, dateAdded;
        public Boolean realProfile = false;
        public long lastVisited = 0;

        public ChatPartner(String partner, String status, String dateAdded) {
            this.partner = partner;
            this.status = status;
            this.dateAdded = dateAdded;
        }

        public ChatPartner() {
        }

        public ChatPartner(String json) {
            try {
                JSONObject o = new JSONObject(json);
                this.partner = o.getString("partner");
                this.status = o.getString("status");
                this.dateAdded = o.getString("dateAdded");
                this.realProfile = o.getBoolean("realProfile");
                this.lastVisited = o.getLong("lastVisited");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public String toString() {
            return "ChatPartner{" +
                    "partner='" + partner + '\'' +
                    ", status='" + status + '\'' +
                    ", dateAdded='" + dateAdded + '\'' +
                    ", realProfile='" + realProfile + '\'' +
                    '}';
        }

        public String toJsonString() {
            try {
                JSONObject o = new JSONObject();
                o.put("partner", partner);
                o.put("status", status);
                o.put("dateAdded", dateAdded);
                o.put("realProfile", realProfile);
                o.put("lastVisited", lastVisited);
                return o.toString();
            } catch (Exception e) {
                return "{error: 'toJsonString failed:" + e.toString() + "' }";
            }
        }

        public View setupChatsItem(final Context context, View item, boolean showMsgCount, final Runnable onFinishTimer, final Profile me) {
            Profile buddy = new Profile(partner);
            if (item == null)
                item = View.inflate(context, R.layout.chats_list_item, null);
            final ChatsItemPlaceHolder holder;
            if (item.getTag() != null)
                holder = (ChatsItemPlaceHolder) item.getTag();
            else {
                holder = new ChatsItemPlaceHolder();
                holder.profileImage = (ImageView) item.findViewById(R.id.profileImage);
                holder.title = (TextView) item.findViewById(R.id.title);
                holder.subtitle1 = (TextView) item.findViewById(R.id.subtitle1);
                holder.subtitle2 = (TextView) item.findViewById(R.id.subtitle2);
                holder.date = (TextView) item.findViewById(R.id.date);
                holder.msgCount = (TextView) item.findViewById(R.id.msgCount);
                holder.timer = (TextView) item.findViewById(R.id.timer);
                item.setTag(holder);
                Utils.setFontToViewGroup(context, (ViewGroup) item, "open");
            }
            ImageLoader.getInstance().displayImage(buddy.imageUrl, holder.profileImage, Utils.displayImageOptions);
            String sub1 = buddy.gender;
            if (buddy.age != null && !buddy.age.isEmpty())
                sub1 += " | " + buddy.age;
            if (buddy.profession != null && !buddy.profession.isEmpty())
                sub1 += " | " + buddy.profession;
            holder.subtitle1.setText(sub1);
            holder.subtitle2.setText(buddy.something);
            holder.date.setText(buddy.lastEntry);

            try {
                if (holder.countDownTimer != null) {
                    holder.countDownTimer.cancel();
                    holder.countDownTimer = null;
                }
                if (dateAdded.equals("")) {
                    holder.timer.setText("");
                } else {
                    long destination = Utils.parseLong(dateAdded) + CHAT_VALIDITY_INTERVAL;
                    long currentTime = Utils.getUnifiedTime();
                    holder.timer.setText(Utils.getTimeForTimer(destination - currentTime));
                    holder.countDownTimer = new CountDownTimer(destination - currentTime, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            holder.timer.setText(Utils.getTimeForTimer(millisUntilFinished));
                        }

                        @Override
                        public void onFinish() {
                            if (onFinishTimer != null)
                                onFinishTimer.run();
                        }
                    };
                    holder.countDownTimer.start();
                }
            } catch (Exception e) {
                e.printStackTrace();
                holder.timer.setText("");
            }

            final String chatId = Utils.getChatId(me.identifier, buddy.identifier);
            holder.msgCount.setVisibility(View.GONE);
            if (showMsgCount) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        FirebaseDatabase.getInstance().getReference("chats").child(chatId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                long messagesSoFar = (long) PreferenceManager.getDefaultSharedPreferences(context).getInt(chatId, 0);
                                long children = dataSnapshot.getChildrenCount();
                                Logs.log("children: " + children + "\nsoFar " + messagesSoFar);
                                final long count = children - messagesSoFar;
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (count > 0) {
                                            holder.msgCount.setText(count + "");
                                            holder.msgCount.setVisibility(View.VISIBLE);
                                            Utils.bubbleUpAnimation(context,
                                                    holder.msgCount,
                                                    1500,
                                                    0);
                                        } else {
                                            holder.msgCount.setVisibility(View.GONE);
                                        }
                                    }
                                });
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                }).start();
            }

            if (!realProfile)
                holder.title.setText(context.getString(R.string.nickname) + " " + buddy.username);
            else
                holder.title.setText(buddy.username);

            return item;
        }

        public class ChatsItemPlaceHolder {
            ImageView profileImage;
            TextView title, subtitle1, subtitle2, date, msgCount, timer;
            CountDownTimer countDownTimer;
        }

        public static Comparator<? super JSONObject> comparator = new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject o1, JSONObject o2) {
                ChatPartner m1 = new ChatPartner(o1.toString());
                ChatPartner m2 = new ChatPartner(o2.toString());
                Long l1 = m1.lastVisited;
                Long l2 = m2.lastVisited;
                Logs.log("compare", l1 + "," + l2);
                return l1.compareTo(l2);
            }
        };
    }
}
