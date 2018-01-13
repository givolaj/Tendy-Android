package com.atn.tendy.chat;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.atn.tendy.Dtos;
import com.atn.tendy.R;
import com.atn.tendy.notifications.TheFirebaseMessagingService;
import com.atn.tendy.utils.Dialogs;
import com.atn.tendy.utils.Logs;
import com.atn.tendy.utils.PushServer;
import com.atn.tendy.utils.Utils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static com.atn.tendy.Dtos.ChatMessage.wantsToStayInTouch1;
import static com.atn.tendy.Dtos.ChatMessage.wantsToStayInTouch2;
import static com.atn.tendy.Dtos.ChatPartner.CHAT_VALIDITY_INTERVAL;
import static com.atn.tendy.Dtos.ChatPartner.STATE_BLOCKED;
import static com.atn.tendy.Dtos.ChatPartner.STATE_CONNECTED;
import static com.atn.tendy.Dtos.ChatPartner.STATE_FOREVER;
import static com.atn.tendy.Dtos.ChatPartner.STATE_HAVE_INVITED;
import static com.atn.tendy.Dtos.ChatPartner.STATE_INVITED;

public class ChatActivity extends AppCompatActivity {
    public static final String CHAT_ID_SEPARATOR = "____";
    public static final int RESIZE_BITMAP_SIZE = 400;
    Dtos.Profile buddyProfile, myProfile;
    Dtos.ChatPartner partner, buddyPartner;
    RelativeLayout header, listViewContainer;
    ListView listView;
    ChatMessagesAdapter adapter;
    ArrayList<JSONObject> messages = new ArrayList<>();
    EditText messageEditText;
    LinearLayout invitedContainer;
    FrameLayout topButtons;
    TextView inviteeName, barText;
    ImageView actions;
    ProgressBar bar;
    String stayInTouchString;
    DatabaseReference partnerReference, buddyPartnerReference;
    SharedPreferences prefs;

    FirebaseStorage storage;
    FirebaseDatabase database;
    String chatId;
    boolean isInviting = false, isConnected = false, isForever = false;
    static final int PICK_IMAGE = 1;
    static final int PICK_IMAGE_PERMISSION_REQUEST = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        setupReferences();
        setupProfiles();
        Dialogs.showNoInternetDialog(this, new Runnable() {
            @Override
            public void run() {
                setupListView();
            }
        });
        cancelRelevantNotifications();
    }

    private void setupReferences() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        storage = FirebaseStorage.getInstance();
        database = FirebaseDatabase.getInstance();

        barText = (TextView) findViewById(R.id.barText);
        bar = (ProgressBar) findViewById(R.id.progressBar);
        actions = (ImageView) findViewById(R.id.actions);
        header = (RelativeLayout) findViewById(R.id.header);
        listViewContainer = (RelativeLayout) findViewById(R.id.listViewContainer);
        topButtons = (FrameLayout) findViewById(R.id.topButtons);
        listView = (ListView) findViewById(R.id.listView);
        invitedContainer = (LinearLayout) findViewById(R.id.invitedContainer);
        inviteeName = (TextView) findViewById(R.id.inviteeName);
        messageEditText = (EditText) findViewById(R.id.messageEditText);
        stayInTouchString = getString(R.string.stay_in_touch);
    }

    private void setupProfiles() {
        isInviting = getIntent().getBooleanExtra("isInviting", false);
        myProfile = new Dtos.Profile(prefs.getString("profile", ""));
        buddyProfile = new Dtos.Profile(getIntent().getStringExtra("chatBuddy"));
        if (getIntent().hasExtra("partner")) {
            partner = new Dtos.ChatPartner(getIntent().getStringExtra("partner"));
        } else {
            partner = new Dtos.ChatPartner(buddyProfile.toJsonString(), STATE_HAVE_INVITED, Utils.getUnifiedTime() + "");
        }
        chatId = Utils.getChatId(myProfile.identifier, buddyProfile.identifier);
        partnerReference = database.getReference("chatPartners")
                .child(myProfile.identifier)
                .child(buddyProfile.identifier);
        buddyPartnerReference = database.getReference("chatPartners")
                .child(buddyProfile.identifier)
                .child(myProfile.identifier);
    }

    MaterialDialog _dialog = null;

    private void setupListView() {
        listView.setOnItemClickListener(onItemClickListener);
        listView.setOnItemLongClickListener(onItemLongClickListener);
        adapter = new ChatMessagesAdapter(this, messages);
        listView.setAdapter(adapter);
        Utils.setFontToViewGroup(this, invitedContainer, "open");
        messageEditText.clearFocus();
        Dialogs.dismissDialog(_dialog);
        _dialog = Dialogs.showProgressIndicator(ChatActivity.this);
        partnerReference.addValueEventListener(partnersValueListener);
    }

    private ValueEventListener partnersValueListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            partner = dataSnapshot.getValue(Dtos.ChatPartner.class);
            if(partner == null)
                partner = new Dtos.ChatPartner(buddyProfile.toJsonString(), STATE_HAVE_INVITED, Utils.getUnifiedTime() + "");
            try {
                buddyPartnerReference.removeEventListener(partnersBuddyValueListener);
            } catch (Exception e) {
                e.printStackTrace();
            }
            buddyPartnerReference.addValueEventListener(partnersBuddyValueListener);
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    private ValueEventListener partnersBuddyValueListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            Dtos.ChatPartner p = dataSnapshot.getValue(Dtos.ChatPartner.class);
            buddyPartner = p;
            handleStateChange(_dialog);
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            Dialogs.dismissDialog(_dialog);
        }

    };

    private void handleStateChange(MaterialDialog dialog) {
        if (partner == null || buddyPartner == null) {
            Dialogs.dismissDialog(dialog);
            return;
        }
        isInviting = false;
        setupHeader();
        if (partner.status.equals(STATE_BLOCKED) || buddyPartner.status.equals(STATE_BLOCKED)) {
            Dialogs.showDialog(this, getString(R.string.problem), getString(R.string.youve_been_blocked), new Runnable() {
                @Override
                public void run() {
                    shouldNotSaveLastVisited = true;
                    finish();
                }
            });
        }
        if (partner.status.equals(STATE_INVITED)) {
            topButtons.setVisibility(View.VISIBLE);
            isConnected = false;
            listViewContainer.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
            boolean isMale = true;
            try {
                isMale = buddyProfile.gender.equals("man");
            } catch (Exception e) {
                e.printStackTrace();
            }
            inviteeName.setText(String.format(getString(R.string.wants_to_chat), buddyProfile.username,
                    isMale ? getString(R.string.wants_male) : getString(R.string.wants_female)));
            invitedContainer.setVisibility(View.VISIBLE);
            messageEditText.setHint("");
            messageEditText.setEnabled(false);
            Dialogs.dismissDialog(dialog);
        } else {
            isConnected = true;
            messageEditText.setEnabled(true);
            listViewContainer.setVisibility(View.VISIBLE);
            invitedContainer.setVisibility(View.GONE);
            if (partner.status.equals(STATE_HAVE_INVITED)) {
                topButtons.setVisibility(View.VISIBLE);
                messageEditText.setHint(getString(R.string.waiting_for_invitation));
            } else {
                messageEditText.setHint(getString(R.string.write_a_msg));
                if (partner.status.equals(Dtos.ChatPartner.STATE_FOREVER)) {
                    if (buddyPartner != null && buddyPartner.status.equals(STATE_FOREVER)) {
                        isForever = true;
                        topButtons.setVisibility(View.GONE);
                    } else{
                        topButtons.setVisibility(View.VISIBLE);
                    }
                } else {
                    topButtons.setVisibility(View.VISIBLE);
                    if (buddyPartner.status.equals(STATE_FOREVER)) {
                        stayInTouchString = getString(R.string.stay_in_touch_asked);
                    }
                }
            }
            getChats();
        }
    }

    private void getChats() {
        listView.setVisibility(View.VISIBLE);
        invitedContainer.setVisibility(View.GONE);
        listViewContainer.setVisibility(View.VISIBLE);
        database.getReference("chats").child(chatId).addValueEventListener(chatsValueListener);
    }

    private ValueEventListener chatsValueListener = new ValueEventListener() {
        @Override
        public void onDataChange(final DataSnapshot dataSnapshot) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        Map<String, Dtos.ChatMessage> map = dataSnapshot.getValue(new GenericTypeIndicator<Map<String, Dtos.ChatMessage>>() {
                        });
                        if (map == null) map = new HashMap<String, Dtos.ChatMessage>();
                        prefs.edit().putInt(chatId, map.size()).commit();
                        if (messages.size() == map.size()) {
                            Dialogs.dismissDialog(_dialog);
                            return; //if i sent message than no update
                        }
                        messages.clear();

                        for (String key : map.keySet()) {
                            if(!map.get(key).deleted.contains(myProfile.identifier))
                                messages.add(new JSONObject(map.get(key).toJsonString()));
                        }
                        Collections.sort(messages, Dtos.ChatMessage.comparator);
                        adapter.notifyDataSetChanged();
                        Dialogs.dismissDialog(_dialog);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Dialogs.dismissDialog(_dialog);
                    }
                }
            });
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            Dialogs.dismissDialog(_dialog);
        }
    };


    CountDownTimer countDownTimer;
    private void setupHeader() {
        partner.setupChatsItem(this, header, false, null, myProfile);
        Utils.setTextColor(header, getResources().getColor(R.color.white));
        if (partner.realProfile)
            header.setBackgroundColor(getResources().getColor(R.color.darkGreen));
        else
            header.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        header.findViewById(R.id.timer).setVisibility(View.GONE);
        header.findViewById(R.id.date).setVisibility(View.GONE);
        header.findViewById(R.id.profileImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChatActivity.this, ImageActivity.class);
                intent.putExtra("imageUrl", buddyProfile.imageUrl);
                startActivity(intent);
            }
        });
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        if (partner.dateAdded.equals("") || partner.status.equals(STATE_FOREVER)) {
            return;
        }
        try {
            long destination = Utils.parseLong(partner.dateAdded) + CHAT_VALIDITY_INTERVAL;
            long currentTime = Utils.getUnifiedTime();
            final long totalTime = destination - currentTime;
            Logs.log("totalTime", totalTime + "");
            bar.setMax((int) CHAT_VALIDITY_INTERVAL);
            bar.setProgress((int) totalTime);
            countDownTimer = new CountDownTimer(totalTime, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int color = 0;
                    if (millisUntilFinished > 45 * 60 * 1000) {
                        color = getResources().getColor(R.color.green);
                    } else if (millisUntilFinished > 20 * 60 * 1000) {
                        color = getResources().getColor(R.color.orange);
                    } else {
                        color = getResources().getColor(R.color.red);
                    }
                    bar.setProgressTintList(ColorStateList.valueOf(color));
                    bar.setProgress((int) (millisUntilFinished));
                    Logs.log("minutes", (millisUntilFinished / 60000 + 1) + "");
                    barText.setText(String.format(stayInTouchString, (millisUntilFinished / 60000 + 1) + ""));
                }

                @Override
                public void onFinish() {
                    deleteOnFinish.run();
                }
            };
            countDownTimer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void openGallery(View v) {
        if ((partner.status.equals(STATE_INVITED) || partner.status.equals(STATE_HAVE_INVITED)) &&
                prefs.getInt(chatId, 0) >= 5) {
            Dialogs.showDialog(ChatActivity.this,
                    getString(R.string.problem),
                    getString(R.string.cant_until_invitation_accepted));
            return;
        }
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                    PICK_IMAGE_PERMISSION_REQUEST);
        } else {
            _openGallery();
        }
    }


    Uri cameraUri;

    void _openGallery() {
        shouldNotSaveLastVisited = true;
        Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/*");
        Intent chooserIntent = Intent.createChooser(pickIntent, getString(R.string.select_image));
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photo = Utils.createImageFile(this);
        if (photo != null) {
            cameraUri = Uri.fromFile(photo);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
        }
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});
        startActivityForResult(chooserIntent, PICK_IMAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PICK_IMAGE_PERMISSION_REQUEST) {
            Logs.log("in", "in");
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                _openGallery();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            try {
                shouldNotSaveLastVisited = false;
                if (data != null && data.getData() != null) {
                    Uri selectedImage = data.getData();
                    String url = data.getData().toString();
                    if (url.startsWith("content://com.google.android.apps.photos.content")) {
                        try {
                            InputStream is = getContentResolver().openInputStream(data.getData());
                            if (is != null) {
                                Bitmap pictureBitmap = BitmapFactory.decodeStream(is);
                                AddImageMessage(Utils.resizeBitmap(pictureBitmap, RESIZE_BITMAP_SIZE));
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            String[] filePathColumn = {MediaStore.Images.Media.DATA};
                            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                            cursor.moveToFirst();
                            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                            String picturePath = cursor.getString(columnIndex);
                            cursor.close();
                            AddImageMessage(Utils.resizeBitmap(BitmapFactory.decodeFile(picturePath), RESIZE_BITMAP_SIZE));
                        } catch (Exception e) {
                            e.printStackTrace();
                            FirebaseCrash.report(e);
                        }
                    }
                } else {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    options.inScaled = false;
                    File file = new File(cameraUri.getPath());
                    Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(file), null, options);
                    bitmap = Utils.getImageWithPortraitOrientation(bitmap, cameraUri);
                    AddImageMessage(Utils.resizeBitmap(bitmap, RESIZE_BITMAP_SIZE));
                }
            } catch (Exception e) {
                FirebaseCrash.report(e);
                e.printStackTrace();
            }
        }
    }


    private void AddImageMessage(final Bitmap bitmap) {
        Dialogs.showImageConfirmationDialog(this, bitmap, new Runnable() {
            @Override
            public void run() {
                _AddImageMessage(bitmap);
            }
        });
    }

    private void _AddImageMessage(final Bitmap bitmap) {
        if (!isConnected && !isInviting) return;
        Dialogs.showNoInternetDialog(this, new Runnable() {
            @Override
            public void run() {
                final MaterialDialog dialog = Dialogs.showProgressIndicator(ChatActivity.this);
                final Dtos.ChatMessage msg = new Dtos.ChatMessage();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                final byte[] data = baos.toByteArray();
                StorageReference imgRef = storage.getReference().child("chatImages").child(chatId).child(new Random().nextInt() + "");
                UploadTask uploadTask = imgRef.putBytes(data);
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Dialogs.dismissDialog(dialog);
                        Dialogs.showDialog(ChatActivity.this,
                                getString(R.string.issue),
                                getString(R.string.cant_image_right_now));
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    @SuppressWarnings("VisibleForTests")
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        msg.imageUrl = taskSnapshot.getDownloadUrl().toString();
                        String key = database.getReference("chats").child(chatId).push().getKey();
                        msg.key = key;
                        msg.sender = myProfile.identifier;
                        database.getReference("chats").child(chatId).child(key).setValue(msg);
                        Dialogs.dismissDialog(dialog);
                        try {
                            messages.add(new JSONObject(msg.toJsonString()));
                            adapter.notifyDataSetChanged();
                            FirebaseDatabase.getInstance().getReference("profiles").child(buddyProfile.identifier)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            Dtos.Profile p = dataSnapshot.getValue(Dtos.Profile.class);
                                            if(p != null  && myProfile != null){
                                                PushServer.sendPush(p, myProfile.username, myProfile.username + getString(R.string.sent_an_image), msg.toJsonString());
                                            }
                                        }
                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (isInviting) {
                            invite();
                        }
                        saveLastVisited();
                    }
                });
            }
        });
    }


    public void sendMessage(View view) {
        if (!isConnected && !isInviting) return;
        Dialogs.showNoInternetDialog(this, new Runnable() {
            @Override
            public void run() {
                String text = messageEditText.getText().toString().trim();
                if (text != null && text.isEmpty()) return;
                sendTextMessage(text);
                if (isInviting) {
                    invite();
                }
                saveLastVisited();
            }
        });
    }

    private void sendTextMessage(String text) {
        if ((partner.status.equals(STATE_INVITED) || partner.status.equals(STATE_HAVE_INVITED)) &&
                prefs.getInt(chatId, 0) >= 5) {
            Dialogs.showDialog(ChatActivity.this,
                    getString(R.string.problem),
                    getString(R.string.cant_until_invitation_accepted));
            return;
        }
        final Dtos.ChatMessage msg = new Dtos.ChatMessage();
        msg.text = text;
        messageEditText.setText("");
        String key = database.getReference("chats").child(chatId).push().getKey();
        msg.key = key;
        msg.sender = myProfile.identifier;
        database.getReference("chats").child(chatId).child(key).setValue(msg);
        try {
            messages.add(new JSONObject(msg.toJsonString()));
            adapter.notifyDataSetChanged();
            FirebaseDatabase.getInstance().getReference("profiles").child(buddyProfile.identifier)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Dtos.Profile p = dataSnapshot.getValue(Dtos.Profile.class);
                            if (p != null && myProfile != null) {
                                if (partner.status.equals(STATE_INVITED) || partner.status.equals(STATE_HAVE_INVITED)){
                                    PushServer.sendPush(p, myProfile.username, buddyProfile.username + " " + getString(R.string.invites_you_to_chat), msg.toJsonString());
                                } else {
                                    PushServer.sendPush(p, myProfile.username, msg.text, msg.toJsonString());
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (msg.text.contains(wantsToStayInTouch1) || msg.text.contains(wantsToStayInTouch2)) {
            stayInTouch(null);
        }
    }

    private void invite() {
        isInviting = false;
        isConnected = true;
        partner = new Dtos.ChatPartner(buddyProfile.toJsonString(), STATE_HAVE_INVITED, Utils.getUnifiedTime() + "");
        buddyPartner = new Dtos.ChatPartner(myProfile.toJsonString(), STATE_INVITED, Utils.getUnifiedTime() + "");
        partnerReference.setValue(partner);
        buddyPartnerReference.setValue(buddyPartner);
        setupHeader();
    }

    private class ChatMessagesAdapter extends ArrayAdapter<JSONObject> {
        public ChatMessagesAdapter(Context context, ArrayList<JSONObject> arr) {
            super(context, 0, arr);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Dtos.ChatMessage m = new Dtos.ChatMessage(getItem(position).toString());
            return m.setupMessageItem(getContext(), convertView, myProfile);
        }
    }

    private AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Dtos.ChatMessage m = new Dtos.ChatMessage(parent.getItemAtPosition(position).toString());
            if (m.imageUrl != null && !m.imageUrl.isEmpty() && (m.deleted == null || !m.deleted.contains(myProfile.identifier))) {
                Intent intent = new Intent(ChatActivity.this, ImageActivity.class);
                intent.putExtra("imageUrl", m.imageUrl);
                String transitionName = getString(R.string.image_transition_name);
                View viewStart = view.findViewById(R.id.image);
                ActivityOptionsCompat options =
                        ActivityOptionsCompat.makeSceneTransitionAnimation(
                                ChatActivity.this,
                                viewStart,
                                transitionName
                        );
                ActivityCompat.startActivity(ChatActivity.this, intent, options.toBundle());
            }
        }
    };

    private AdapterView.OnItemLongClickListener onItemLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            final Dtos.ChatMessage m = new Dtos.ChatMessage(parent.getItemAtPosition(position).toString());
            if (m.deleted != null && !m.deleted.contains(myProfile.identifier) && !m.text.contains(wantsToStayInTouch1) && !m.text.contains(wantsToStayInTouch2)) {
                Dialogs.showYesNoDialog(ChatActivity.this,
                        getString(R.string.warning),
                        getString(R.string.sure_you_want_to_delete_message),
                        getString(R.string.yes),
                        getString(R.string.no),
                        new Runnable() {
                            @Override
                            public void run() {
                                m.deleted += "___" + myProfile.identifier;
                                database.getReference("chats").child(chatId).child(m.key).setValue(m);
                                updateMessage(m);
                            }
                        }, null);
            }
            return true;
        }
    };

    private void updateMessage(Dtos.ChatMessage m) {
        try {
            View v = null;
            for (int i = 0; i < messages.size(); i++) {
                try {
                    if (messages.get(i).getString("key").equals(m.key)) {
                        messages.remove(i);
                        v = listView.getChildAt(i - listView.getFirstVisiblePosition());
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            messages.add(new JSONObject(m.toJsonString()));
            Collections.sort(messages, Dtos.ChatMessage.comparator);
            TextView tv = (TextView) v.findViewById(R.id.text);
            tv.setText(getString(R.string.deleted_message));
            v.findViewById(R.id.image).setVisibility(View.GONE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean shouldNotSaveLastVisited = false;
    public void saveLastVisited(){
        if (!isInviting && !shouldNotSaveLastVisited) {
            try {
                buddyPartner.lastVisited = Utils.getUnifiedTime();
                buddyPartnerReference.setValue(buddyPartner);
                partner.lastVisited = Utils.getUnifiedTime();
                partnerReference.setValue(partner);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onPause() {
        prefs.edit().putBoolean("isActive", false).commit();
        prefs.edit().putString("currentChatPartner", "").commit();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        partnerReference.removeEventListener(partnersValueListener);
        buddyPartnerReference.removeEventListener(partnersBuddyValueListener);
        database.getReference("chats").child(chatId).removeEventListener(chatsValueListener);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        prefs.edit().putBoolean("isActive", true).commit();
        try {
            if (buddyProfile != null)
                prefs.edit().putString("currentChatPartner", buddyProfile.identifier).commit();
        } catch (Exception e){}
        setupHeader();
    }

    public void acceptInvitation(View view) {
        if (!Utils.haveInternetConnection(this)) {
            Dialogs.showDialog(this, getString(R.string.problem), getString(R.string.no_internet));
            return;
        }
        Dialogs.dismissDialog(_dialog);
        _dialog = Dialogs.showProgressIndicator(this);
        partner.status = buddyPartner.status = STATE_CONNECTED;
        partnerReference.setValue(partner);
        buddyPartnerReference.setValue(buddyPartner);
        getChats();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onStop() {
        Logs.log("onStop");
        super.onStop();
    }

    boolean stayImTouchEnabled = true;

    public void stayInTouch(View view) {
        if (!stayImTouchEnabled) return;
        stayImTouchEnabled = false;
        if (partner.status.equals(STATE_INVITED) || partner.status.equals(STATE_HAVE_INVITED)) {
            Dialogs.showDialog(ChatActivity.this,
                    getString(R.string.problem),
                    getString(R.string.cant_until_accepted_invitation),
                    new Runnable() {
                        @Override
                        public void run() {
                            stayImTouchEnabled = true;
                        }
                    });
            return;
        }
        Dialogs.showYesNoDialog(this,
                getString(R.string.requestFriendshipTitle),
                String.format(getString(R.string.requestFriendshipText), buddyProfile.username),
                getString(R.string.yes),
                getString(R.string.no),
                new Runnable() {
                    @Override
                    public void run() {
                        sendTextMessage(String.format(getString(R.string.wants_to_stay_in_touch), myProfile.username));
                        partner.status = Dtos.ChatPartner.STATE_FOREVER;
                        if (buddyPartner.status.equals(STATE_FOREVER)) {
                            partner.dateAdded = "";
                            buddyPartner.dateAdded = "";
                            buddyPartnerReference.setValue(buddyPartner);
                        }
                        partnerReference.setValue(partner);
                        stayImTouchEnabled = true;
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        stayImTouchEnabled = true;
                    }
                });
    }

    public void block(View view) {
        if (isInviting) {
            Dialogs.showDialog(this, getString(R.string.problem), getString(R.string.cant_block_before_inviting));
            return;
        }
        Dialogs.showYesNoDialog(this,
                getString(R.string.warning),
                String.format(getString(R.string.sure_you_want_to_delete), buddyProfile.username),
                getString(R.string.yes),
                getString(R.string.no),
                new Runnable() {
                    @Override
                    public void run() {
                        buddyPartner.status = partner.status = STATE_BLOCKED;
                        buddyPartner.dateAdded = partner.dateAdded = "";
                        partnerReference.setValue(partner);
                        buddyPartnerReference.setValue(buddyPartner);
                        finish();
                    }
                }, null);
    }


    public void openActions(View view) {
        PopupMenu popup = new PopupMenu(this, actions);
        popup.getMenuInflater()
                .inflate(R.menu.chat_activity_actions, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.share_real_identity) {
                    shareRealIdentity(null);
                } else if (item.getItemId() == R.id.block) {
                    block(null);
                } else if (item.getItemId() == R.id.delete) {
                    deleteChat();
                } else {
                    showMyCurrentProfile();
                }
                return true;
            }
        });
        popup.show();
    }

    private void showMyCurrentProfile() {
        buddyPartnerReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Dtos.ChatPartner partner = dataSnapshot.getValue(Dtos.ChatPartner.class);
                if (partner == null) {
                    Dialogs.showDialog(ChatActivity.this,
                            getString(R.string.problem),
                            getString(R.string.temp_problem));
                    return;
                }
                Dialogs.showProfileDialog(ChatActivity.this, new Dtos.Profile(partner.partner));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void shareRealIdentity(View v) {
        if (!Utils.haveInternetConnection(this)) {
            Dialogs.showDialog(this, getString(R.string.problem), getString(R.string.no_internet));
            return;
        }
        if (isInviting) {
            Dialogs.showDialog(this, getString(R.string.problem), getString(R.string.cant_share_real_before_writing_a_message));
            return;
        }
        String rp = prefs.getString("realProfile", "");
        if (!Dtos.Profile.isValidProfile(rp)) {
            Dialogs.showDialog(ChatActivity.this,
                    getString(R.string.problem),
                    getString(R.string.dont_have_real_profile));
            return;
        }
        final Dtos.Profile realProfile = new Dtos.Profile(rp);
        Dialogs.showYesNoDialog(ChatActivity.this,
                getString(R.string.real_details_title),
                getString(R.string.real_details_text),
                getString(R.string.yes),
                getString(R.string.no),
                new Runnable() {
                    @Override
                    public void run() {
                        buddyPartner.realProfile = true;
                        buddyPartner.partner = realProfile.toJsonString();
                        buddyPartnerReference.setValue(buddyPartner);
                        Dialogs.showDialog(ChatActivity.this,
                                getString(R.string.success),
                                getString(R.string.real_profile_shared));
                    }
                }, null);
    }

    private Runnable deleteOnFinish = new Runnable() {
        @Override
        public void run() {
            if (isForever) return;
            if (buddyPartner != null && buddyPartner.status.equals(STATE_FOREVER) &&
                    partner != null && partner.status.equals(STATE_FOREVER)) {
                return;
            }
            try {
                topButtons.setVisibility(View.GONE);
                Dialogs.showDialog(ChatActivity.this, getString(R.string.time_over_title), getString(R.string.time_over_text), new Runnable() {
                    @Override
                    public void run() {
                        try {
                            deleteChat();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    };

    private void deleteChat() {
        shouldNotSaveLastVisited = true;
        cancelRelevantNotifications();
        partnerReference.removeEventListener(partnersValueListener);
        buddyPartnerReference.removeEventListener(partnersBuddyValueListener);
        partnerReference.removeValue();
        buddyPartnerReference.removeValue();
        database.getReference("chats").child(chatId).removeValue();
        finish();
    }

    private void cancelRelevantNotifications() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(TheFirebaseMessagingService.INVITE_NOTIFICATION_ID);
        notificationManager.cancel(Utils.uniqueNumberFromString(buddyProfile.identifier));
    }
}
