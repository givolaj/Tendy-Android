package com.atn.tendy.chat;


import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.atn.tendy.Dtos;
import com.atn.tendy.R;
import com.atn.tendy.notifications.TheFirebaseMessagingService;
import com.atn.tendy.utils.Dialogs;
import com.atn.tendy.utils.Logs;
import com.atn.tendy.utils.Utils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 */
public class ChatsFragment extends Fragment {

    public ChatsFragment() {
        // Required empty public constructor
    }

    ImageView noChats;
    ListView listView;
    ChatsAdapter adapter;
    ArrayList<JSONObject> data = new ArrayList<>();
    FirebaseDatabase database;
    Dtos.Profile profile;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_chats, container, false);
        noChats = (ImageView) v.findViewById(R.id.noChats);
        listView = (ListView) v.findViewById(R.id.listView);
        adapter = new ChatsAdapter(getActivity(), data);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Dtos.ChatPartner partner = new Dtos.ChatPartner(parent.getItemAtPosition(position).toString());
                Intent intent = new Intent(getActivity(), ChatActivity.class);
                intent.putExtra("partner", partner.toJsonString());
                intent.putExtra("chatBuddy", partner.partner);
                intent.putExtra("isInviting", false);
                String transitionName = getString(R.string.header_transition_name);
                View viewStart = view;
                ActivityOptionsCompat options =
                        ActivityOptionsCompat.makeSceneTransitionAnimation(
                                getActivity(),
                                viewStart,
                                transitionName
                        );
                ActivityCompat.startActivity(getActivity(), intent, options.toBundle());
            }
        });
        database = FirebaseDatabase.getInstance();
        refresh(true);
        return v;
    }

    MaterialDialog dialog = null;
    public void refresh(boolean showDialog) {
        try {
            Dialogs.dismissDialog(dialog);
            if(showDialog)
                dialog = Dialogs.showProgressIndicator(getActivity());
            String profileJson = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("profile", "");
            if (!Dtos.Profile.isValidProfile(profileJson)) {
                noChats.setVisibility(View.VISIBLE);
                listView.setVisibility(View.GONE);
                Dialogs.dismissDialog(dialog);
                return;
            }
            profile = new Dtos.Profile(profileJson);
            Dialogs.showNoInternetDialog(getActivity(), new Runnable(){
                @Override
                public void run() {
                    database.getReference("chatPartners").child(profile.identifier).removeEventListener(refreshListener);
                    database.getReference("chatPartners").child(profile.identifier).addValueEventListener(refreshListener);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            noChats.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
            Dialogs.dismissDialog(dialog);
        }
    }

    private void deleteItems(ArrayList<Dtos.ChatPartner> toDelete) {
        for(Dtos.ChatPartner p: toDelete){
            Dtos.Profile buddy = new Dtos.Profile(p.partner);
            NotificationManager notificationManager =
                    (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(TheFirebaseMessagingService.INVITE_NOTIFICATION_ID);
            notificationManager.cancel(Utils.uniqueNumberFromString(buddy.identifier));
            database.getReference("chatPartners").child(profile.identifier).child(buddy.identifier).removeValue();
            database.getReference("chatPartners").child(buddy.identifier).child(profile.identifier).removeValue();
            String chatId = Utils.getChatId(profile.identifier, buddy.identifier);
            database.getReference("chats").child(chatId).removeValue();
        }
        if(toDelete.size() > 0) refresh(false);
    }


    private class ChatsAdapter extends ArrayAdapter<JSONObject> {
        public ChatsAdapter(Context context, ArrayList<JSONObject> arr) {
            super(context, 0, arr);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Dtos.ChatPartner partner = new Dtos.ChatPartner(getItem(position).toString());
            return partner.setupChatsItem(getContext(), convertView, true, new Runnable() {
                @Override
                public void run() {
                    refresh(false);
                }
            }, profile);
        }
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    @Override
    public void onPause(){
        database.getReference("chatPartners").child(profile.identifier).removeEventListener(refreshListener);
        super.onPause();
    }

    private ValueEventListener refreshListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            try {
                Map<String, Dtos.ChatPartner> map = dataSnapshot.getValue(new GenericTypeIndicator<Map<String, Dtos.ChatPartner>>() {
                });
                if (map != null && map.size() > 0) {
                    data.clear();
                    ArrayList<Dtos.ChatPartner> toDelete = new ArrayList<Dtos.ChatPartner>();
                    for (String key : map.keySet()) {
                        try {
                            Dtos.ChatPartner cp = map.get(key);
                            Logs.log("partner", cp.toJsonString());
                            if(!cp.status.equals(Dtos.ChatPartner.STATE_BLOCKED)) {
                                data.add(new JSONObject(map.get(key).toJsonString()));
                                if(!cp.status.equals(Dtos.ChatPartner.STATE_FOREVER)){
                                    long destination = Utils.parseLong(map.get(key).dateAdded) + Dtos.ChatPartner.CHAT_VALIDITY_INTERVAL;
                                    long currentTime = Utils.getUnifiedTime();
                                    if(currentTime >= destination){
                                        toDelete.add(map.get(key));
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    data.removeAll(toDelete);
                    Collections.sort(data, Dtos.ChatPartner.comparator);
                    Collections.reverse(data);
                    adapter.notifyDataSetChanged();
                    if(data.size() == 0){
                        noChats.setVisibility(View.VISIBLE);
                        listView.setVisibility(View.GONE);
                    } else{
                        noChats.setVisibility(View.GONE);
                        listView.setVisibility(View.VISIBLE);
                    }
                    deleteItems(toDelete);
                } else {
                    noChats.setVisibility(View.VISIBLE);
                    listView.setVisibility(View.GONE);
                }
                Dialogs.dismissDialog(dialog);
            } catch (Exception e) {
                e.printStackTrace();
                noChats.setVisibility(View.VISIBLE);
                listView.setVisibility(View.GONE);
                Dialogs.dismissDialog(dialog);
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            noChats.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
            Dialogs.dismissDialog(dialog);
        }
    };
}
