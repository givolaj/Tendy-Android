package com.atn.tendy;

import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.atn.tendy.chat.ChatActivity;
import com.atn.tendy.chat.ChatsFragment;
import com.atn.tendy.discovery.BackgroundDiscoveryService;
import com.atn.tendy.discovery.DiscoveryFragment;
import com.atn.tendy.login.LoginActivity;
import com.atn.tendy.notifications.TheFirebaseMessagingService;
import com.atn.tendy.profile.AccountSettingsActivity;
import com.atn.tendy.profile.ProfileFragment;
import com.atn.tendy.utils.Dialogs;
import com.atn.tendy.utils.Logs;
import com.atn.tendy.utils.PushServer;
import com.atn.tendy.utils.Utils;
import com.facebook.login.LoginManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth firebaseAuth;
    FirebaseDatabase database;
    RelativeLayout profile, chat, discovery;
    ImageView profileImage, chatImage, discoveryImage;
    TextView profileText, chatText, discoveryText;
    View profileLine, chatLine, discoveryLine;
    android.support.v4.widget.DrawerLayout drawer;
    boolean isMoving = false;

    int darkGray, yellow, light_gray;

    SharedPreferences prefs;
    private boolean isResumed = false;

    FirebaseUser user;
    Dtos.Profile myProfile;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = FirebaseDatabase.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
        setContentView(R.layout.activity_main);
        Utils.enableWifiIfNeeded();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        setupDrawer();
        setupTabs();

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        Utils.setFontToViewGroup(this, drawer, "open");

        LocalBroadcastManager.getInstance(this).registerReceiver(DiscoveryReceiver,
                new IntentFilter("discovery"));
        LocalBroadcastManager.getInstance(this).registerReceiver(ChatMessageReceiver,
                new IntentFilter("chatMessage"));

        if (!Utils.haveInternetConnection(this)) {
            Dialogs.showDialog(this, getString(R.string.problem), getString(R.string.no_internet), new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            });
        }

        String p = prefs.getString("profile", "");
        if (!Dtos.Profile.isValidProfile(p)) {
            profile.callOnClick();
            if(prefs.getBoolean("firstUse", true)) {
                prefs.edit().putBoolean("firstUse", false).commit();
                Dialogs.showCustomDialogOneButton(this, R.layout.login_success, null);
            }
        } else {
            myProfile = new Dtos.Profile(p);
            if (getIntent().hasExtra("chatMessage")) {
                chat.callOnClick();
                handleChatIntent();
            } else {
                discovery.callOnClick();
            }
        }
       // checkRealProfile();
        checkToken();
        checkPartners();

        prefs.edit().putBoolean("isAppActive", true).commit();
        startService(new Intent(getApplicationContext(), BackgroundDiscoveryService.class));
    }

    private void checkToken() {
        if (user != null) {
            FirebaseDatabase.getInstance().getReference("profiles")
                    .child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Dtos.Profile p = dataSnapshot.getValue(Dtos.Profile.class);
                    if (p != null) {
                        p.pushToken = FirebaseInstanceId.getInstance().getToken();
                        FirebaseDatabase.getInstance().getReference("profiles").child(p.identifier).setValue(p);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    public void setupDrawer() {
        final LinearLayout drawer = (LinearLayout) findViewById(R.id.left_drawer);
        Utils.setFontToViewGroup(this, drawer, "open");
        final TextView logout = (TextView) drawer.findViewById(R.id.logout);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialogs.showYesNoDialog(MainActivity.this,
                        getString(R.string.log_out),
                        getString(R.string.sure_you_want_to_logout),
                        getString(R.string.yes),
                        getString(R.string.no),
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    prefs.edit().remove("profile").commit();
                                    prefs.edit().remove("realProfile").commit();
                                    prefs.edit().remove("discoveryArray").commit();
                                    prefs.edit().remove("fbobject").commit();
                                    NotificationManager notificationManager =
                                            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                                    notificationManager.cancelAll();
                                    firebaseAuth.signOut();
                                    LoginManager.getInstance().logOut();
                                    startActivity(new Intent(MainActivity.this, SplashActivity.class));
                                    finish();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    startActivity(new Intent(MainActivity.this, SplashActivity.class));
                                    finish();
                                }
                            }
                        },
                        null);
            }
        });
        TextView email = (TextView) drawer.findViewById(R.id.email);
        email.setText(firebaseAuth.getCurrentUser().getEmail());
        TextView realName = (TextView) drawer.findViewById(R.id.realName);
        realName.setText(firebaseAuth.getCurrentUser().getDisplayName());
        de.hdodenhof.circleimageview.CircleImageView realImage =
                (de.hdodenhof.circleimageview.CircleImageView) drawer.findViewById(R.id.realProfileImage);
        if (firebaseAuth.getCurrentUser().getPhotoUrl() == null)
            realImage.setImageResource(R.drawable.male3);
        else if (firebaseAuth.getCurrentUser().getPhotoUrl().toString().contains("android.resource:")) {
            realImage.setImageURI(firebaseAuth.getCurrentUser().getPhotoUrl());
        } else {
            ImageLoader.getInstance().displayImage(
                    firebaseAuth.getCurrentUser().getPhotoUrl().toString(),
                    realImage,
                    Utils.displayImageOptions);
        }

        TextView accountSettings = (TextView) drawer.findViewById(R.id.account_settings);
        accountSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AccountSettingsActivity.class);
                String transitionName = getString(R.string.header_transition_name);
                View viewStart = drawer.findViewById(R.id.profileContainer);
                ActivityOptionsCompat options =
                        ActivityOptionsCompat.makeSceneTransitionAnimation(
                                MainActivity.this,
                                viewStart,
                                transitionName
                        );
                ActivityCompat.startActivity(MainActivity.this, intent, options.toBundle());
            }
        });

        final TextView discoveryStateSubtitle = drawer.findViewById(R.id.discoveryStateSubtitle);
        boolean state = prefs.getBoolean("discoveryState", true);
        setupDiscoveryState(discoveryStateSubtitle, state);
        drawer.findViewById(R.id.discoveryState).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean state = !prefs.getBoolean("discoveryState", true);
                prefs.edit().putBoolean("discoveryState", state).commit();
                refreshIfNeeded(OPTION_DISCOVERY);
                setupDiscoveryState(discoveryStateSubtitle, state);
            }
        });

        drawer.findViewById(R.id.support).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", "atn.tendi@gmail.com", null));
                intent.putExtra(Intent.EXTRA_SUBJECT, "Support request - " + user.getUid());
                startActivity(Intent.createChooser(intent, ""));
            }
        });

        drawer.findViewById(R.id.deleteProfiles).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseDatabase.getInstance().getReference("profiles").child(user.getUid()).removeValue();
                FirebaseDatabase.getInstance().getReference("realProfiles").child(user.getUid()).removeValue();
                firebaseAuth.getCurrentUser().delete();
                logout.callOnClick();
            }
        });
        if(!Logs.DEBUG_MODE)
            drawer.findViewById(R.id.deleteProfiles).setVisibility(View.GONE);

        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            int build = pInfo.versionCode;
            ((TextView) drawer.findViewById(R.id.version)).setText(version + " (" + build + ")");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void setupDiscoveryState(TextView discoveryStateSubtitle, boolean state) {
        discoveryStateSubtitle.setText(getString(state ? R.string.discoveryStateOn : R.string.discoveryStateOff));
        discoveryStateSubtitle.setTextColor(getResources().getColor(state ? R.color.green : R.color.red));
    }

    private void setupTabs() {
        darkGray = ContextCompat.getColor(this, R.color.darkGray);
        light_gray = ContextCompat.getColor(this, R.color.light_gray);
        yellow = ContextCompat.getColor(this, R.color.yellow);

        profile = (RelativeLayout) findViewById(R.id.profile);
        profileImage = (ImageView) profile.findViewById(R.id.profileImage);
        profileText = (TextView) profile.findViewById(R.id.profileText);
        profileLine = profile.findViewById(R.id.profileLine);
        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isMoving) return;
                setAllMenuGray();
                goToFragment(new ProfileFragment());
                profileImage.setImageResource(R.drawable.ic_profile_on);
                profileText.setTextColor(yellow);
                profileLine.setBackgroundColor(yellow);
            }
        });

        chat = (RelativeLayout) findViewById(R.id.chat);
        chatImage = (ImageView) chat.findViewById(R.id.chatImage);
        chatText = (TextView) chat.findViewById(R.id.chatText);
        chatLine = chat.findViewById(R.id.chatLine);
        chat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isMoving) return;
                if (!Dtos.Profile.isValidProfile(prefs.getString("profile", ""))) {
                    Dialogs.showDialog(MainActivity.this,
                            getString(R.string.problem),
                            getString(R.string.fill_profile_first));
                    return;
                }
                setAllMenuGray();
                goToFragment(new ChatsFragment());
                chatImage.setImageResource(R.drawable.ic_chat_on);
                chatText.setTextColor(yellow);
                chatLine.setBackgroundColor(yellow);
            }
        });

        discovery = (RelativeLayout) findViewById(R.id.discovery);
        discoveryImage = (ImageView) discovery.findViewById(R.id.discoveryImage);
        discoveryText = (TextView) discovery.findViewById(R.id.discoveryText);
        discoveryLine = discovery.findViewById(R.id.discoveryLine);
        discovery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isMoving) return;
                if (!Dtos.Profile.isValidProfile(prefs.getString("profile", ""))) {
                    Dialogs.showDialog(MainActivity.this,
                            getString(R.string.problem),
                            getString(R.string.fill_profile_first));
                    return;
                }
                setAllMenuGray();
                goToFragment(new DiscoveryFragment());
                discoveryImage.setImageResource(R.drawable.ic_discovery_on);
                discoveryText.setTextColor(yellow);
                discoveryLine.setBackgroundColor(yellow);
            }
        });
    }

    void setAllMenuGray() {
        profileImage.setImageResource(R.drawable.ic_profile);
        profileText.setTextColor(light_gray);
        profileLine.setBackgroundColor(darkGray);
        chatImage.setImageResource(R.drawable.ic_chat);
        chatText.setTextColor(light_gray);
        chatLine.setBackgroundColor(darkGray);
        discoveryImage.setImageResource(R.drawable.ic_discovery);
        discoveryText.setTextColor(light_gray);
        discoveryLine.setBackgroundColor(darkGray);
        Utils.closeKeyboard(this);
    }

    Fragment currentFragment = null;

    void goToFragment(Fragment fragment) {
       try {
           if (currentFragment != null &&
                   currentFragment.getClass().getName().equals(fragment.getClass().getName()))
               return;
           isMoving = true;
           boolean goingLeft = true;
           if ((fragment instanceof ProfileFragment) ||
                   currentFragment instanceof DiscoveryFragment)
               goingLeft = false;
           currentFragment = fragment;
           FragmentTransaction tr = getSupportFragmentManager()
                   .beginTransaction();
           if (!goingLeft)
               tr.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right);
           else
               tr.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_right, R.anim.slide_out_left);
           tr.replace(R.id.container, fragment);
           tr.commit();
           Utils.runWithDelay(new Runnable() {
               @Override
               public void run() {
                   isMoving = false;
               }
           }, 750);
       } catch (Exception e){}
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        currentFragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 100 && resultCode == Activity.RESULT_CANCELED) {
            Dialogs.showDialog(this, getString(R.string.problem), getString(R.string.bluetooth_warning));
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    final int OPTION_ALL = 0;
    final int OPTION_DISCOVERY = 1;
    final int OPTION_CHATS = 2;
    final int OPTION_PROFILE = 3;

    private void refreshIfNeeded(int options) {
        Logs.log("options", options + "");
        if (!isResumed) {
            return;
        }
        if (options == OPTION_ALL || options == OPTION_CHATS) {
            if (currentFragment != null &&
                    currentFragment instanceof ChatsFragment) {
                ((ChatsFragment) currentFragment).refresh(false);
            }
        }

        if (options == OPTION_ALL || options == OPTION_DISCOVERY) {
            if (currentFragment != null &&
                    currentFragment instanceof DiscoveryFragment) {
                ((DiscoveryFragment) currentFragment).refresh();
            }
        }

        if (options == OPTION_ALL || options == OPTION_PROFILE) {
            if (currentFragment != null &&
                    currentFragment instanceof ProfileFragment) {
                ((ProfileFragment) currentFragment).refresh();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(Gravity.START)) {
            drawer.closeDrawer(Gravity.START);
            return;
        }
        super.onBackPressed();
    }

    public void openMenu(View view) {
        if (drawer.isDrawerOpen(Gravity.START)) {
            drawer.closeDrawer(Gravity.START);
        } else {
            drawer.openDrawer(Gravity.START);
        }
    }

    private BroadcastReceiver DiscoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshIfNeeded(OPTION_DISCOVERY);
        }
    };

    private BroadcastReceiver ChatMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshIfNeeded(OPTION_ALL);
        }
    };

    private void handleChatIntent() {
        chat.callOnClick();
        Dtos.Profile buddy = new Dtos.Profile(getIntent().getStringExtra("buddy"));
        Dtos.Profile me = new Dtos.Profile(prefs.getString("profile", ""));
        FirebaseDatabase.getInstance().getReference("chatPartners")
                .child(me.identifier)
                .child(buddy.identifier)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Dtos.ChatPartner p = dataSnapshot.getValue(Dtos.ChatPartner.class);
                        if (p == null) return;
                        else {
                            Dtos.ChatMessage msg = new Dtos.ChatMessage(getIntent().getStringExtra("msg"));
                            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                            intent.putExtra("chatBuddy", getIntent().getStringExtra("buddy"));
                            intent.putExtra("msg", msg.toJsonString());
                            intent.putExtra("isInviting", false);
                            intent.putExtra("partner", getIntent().getStringExtra("partner"));
                            startActivity(intent);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        PushServer.getCurrentMillis();
        isResumed = true;
        prefs.edit().putBoolean("isAppActive", true).commit();
        if (!Utils.haveInternetConnection(this)) {
            Dialogs.showDialog(this, getString(R.string.problem), getString(R.string.no_internet), new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            });
            return;
        }
        refreshIfNeeded(OPTION_ALL);
        setupDrawer();
    }

    protected synchronized void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(DiscoveryReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(ChatMessageReceiver);
        prefs.edit().putBoolean("isAppActive", false).commit();
        //stopService(new Intent(getApplicationContext(), BackgroundDiscoveryService.class));
        super.onDestroy();
    }

    @Override
    public void onPause() {
        isResumed = false;
        prefs.edit().putBoolean("isAppActive", false).commit();
        super.onPause();
    }

    private void checkRealProfile() {
        final Dialog d = Dialogs.showProgressIndicator(this);
        FirebaseDatabase.getInstance().getReference("realProfiles").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Logs.log("onDataChange");
                Dialogs.dismissDialog(d);
                Dtos.Profile p = dataSnapshot.getValue(Dtos.Profile.class);
                if (p == null) {
                    Intent intent = new Intent(MainActivity.this, AccountSettingsActivity.class);
                    intent.putExtra("showWelcomeMessage", true);
                    startActivity(intent);
                } else {
                    prefs.edit().putString("realProfile", p.toJsonString()).commit();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Logs.log("error!");
                Dialogs.dismissDialog(d);
            }
        });
    }

    ValueEventListener checkPartnersListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            try {
                Map<String, Dtos.ChatPartner> map = dataSnapshot.getValue(new GenericTypeIndicator<Map<String, Dtos.ChatPartner>>() {
                });
                if (map != null && map.size() > 0) {
                    ArrayList<Dtos.ChatPartner> toDelete = new ArrayList<Dtos.ChatPartner>();
                    for (String key : map.keySet()) {
                        try {
                            Dtos.ChatPartner cp = map.get(key);
                            if (!cp.status.equals(Dtos.ChatPartner.STATE_BLOCKED) && !cp.status.equals(Dtos.ChatPartner.STATE_FOREVER)) {
                                long destination = Utils.parseLong(map.get(key).dateAdded) + Dtos.ChatPartner.CHAT_VALIDITY_INTERVAL;
                                long currentTime = Utils.getUnifiedTime();
                                if (currentTime >= destination) {
                                    toDelete.add(map.get(key));
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    deleteItems(toDelete);
                }
            } catch (Exception e) {

            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    public void checkPartners() {
        if(myProfile == null) return;
        database.getReference("chatPartners").child(myProfile.identifier).addListenerForSingleValueEvent(checkPartnersListener);
    }

    private void deleteItems(ArrayList<Dtos.ChatPartner> toDelete) {
        if(myProfile == null) return;
        for (Dtos.ChatPartner p : toDelete) {
            Dtos.Profile buddy = new Dtos.Profile(p.partner);
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(TheFirebaseMessagingService.INVITE_NOTIFICATION_ID);
            notificationManager.cancel(Utils.uniqueNumberFromString(buddy.identifier));
            database.getReference("chatPartners").child(myProfile.identifier).child(buddy.identifier).removeValue();
            database.getReference("chatPartners").child(buddy.identifier).child(myProfile.identifier).removeValue();
            String chatId = Utils.getChatId(myProfile.identifier, buddy.identifier);
            database.getReference("chats").child(chatId).removeValue();
        }
    }


    public void goToDiscovery(){
        discovery.callOnClick();
    }
}
