package com.atn.tendy.discovery;


import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.atn.tendy.Dtos;
import com.atn.tendy.MainActivity;
import com.atn.tendy.R;
import com.atn.tendy.chat.ChatActivity;
import com.atn.tendy.notifications.TheFirebaseMessagingService;
import com.atn.tendy.utils.Dialogs;
import com.atn.tendy.utils.Logs;
import com.atn.tendy.utils.Utils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;


public class DiscoveryFragment extends Fragment {

    private static long REMOVE_PARTNERS_INTERVAL = 5*1000*60;

    public DiscoveryFragment() {
        // Required empty public constructor
    }

    pl.droidsonroids.gif.GifImageView searching;
    DiscoveryAdapter adapter;
    ArrayList<JSONObject> data = new ArrayList<>();
    ListView listView;
    TextView discoveryStateOff;
    Dtos.Profile me;
    SharedPreferences prefs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_discovery, container, false);
        setupReferences(v);
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            Dialogs.showDialog(getActivity(), getString(R.string.problem), getString(R.string.no_bluetooth));
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                refresh();
            }
        }, 0);
        handleNotifications();
        return v;
    }

    private void setupReferences(View v){
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Utils.setFontToViewGroup(getActivity(), (ViewGroup) v, "open");
        me = new Dtos.Profile(prefs.getString("profile", ""));
        searching = (pl.droidsonroids.gif.GifImageView) v.findViewById(R.id.searching);
        listView = (ListView) v.findViewById(R.id.listView);
        adapter = new DiscoveryAdapter(getActivity(), data);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(onListItemClick);
        discoveryStateOff = (TextView) v.findViewById(R.id.discoveryStateOff);
        discoveryStateOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.edit().putBoolean("discoveryState", true).commit();
                ((MainActivity) getActivity()).setupDrawer();
                refresh();
            }
        });
    }

    public void refresh() {
        Logs.log("refresh");
        boolean state = prefs.getBoolean("discoveryState", true);
        if(!state){
            searching.setVisibility(View.GONE);
            listView.setVisibility(View.GONE);
            discoveryStateOff.setVisibility(View.VISIBLE);
            return;
        } else{
            discoveryStateOff.setVisibility(View.GONE);
        }
        try {
            JSONArray arr = new JSONArray(getDefaultSharedPreferences(getActivity()).getString("discoveryArray", "[]"));
            Logs.log("arrItems", arr.toString(4));
            JSONArray newArr = new JSONArray();
            for(int i=0;i<arr.length();i++){
                JSONObject o = arr.getJSONObject(i);
                Logs.log("time1", o.getLong("dateAdded") + REMOVE_PARTNERS_INTERVAL + "");
                Logs.log("time2", System.currentTimeMillis() + "");
                if(o.getLong("dateAdded") + REMOVE_PARTNERS_INTERVAL > Utils.getUnifiedTime()){
                    newArr.put(o);
                }
            }
            getDefaultSharedPreferences(getActivity()).edit().putString("discoveryArray", newArr.toString()).commit();
            deployList(newArr);
        }
        catch (Exception e){ e.printStackTrace(); }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    private void deployList(final JSONArray arr) {
        try {
            data.clear();
            data.addAll(Utils.listFromJSONArray(arr));
            FirebaseDatabase.getInstance().getReference("chats").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    ArrayList<JSONObject> toRemove = new ArrayList<JSONObject>();
                    for(int i=0;i<data.size();i++){
                        Dtos.Profile buddy = new Dtos.Profile(data.get(i).toString());
                        String chatId = Utils.getChatId(me.identifier, buddy.identifier);
                        if(dataSnapshot.hasChild(chatId)){
                            toRemove.add(data.get(i));
                        }
                    }
                    data.removeAll(toRemove);
                    Collections.sort(data, Dtos.Profile.comparator);
                    if(data.size() == 0){
                        listView.setVisibility(View.GONE);
                        searching.setVisibility(View.VISIBLE);
                    } else{
                        searching.setVisibility(View.GONE);
                        listView.setVisibility(View.VISIBLE);
                    }
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    searching.setVisibility(View.VISIBLE);
                    listView.setVisibility(View.GONE);
                    adapter.notifyDataSetChanged();
                }
            });
        }
        catch (Exception e){e.printStackTrace();}
    }

    private class DiscoveryAdapter extends ArrayAdapter<JSONObject> {
        public DiscoveryAdapter(Context context, ArrayList<JSONObject> arr) {
            super(context, 0, arr);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Dtos.Profile p = new Dtos.Profile(getItem(position).toString());
            return p.setupDiscoveryItem(getContext(), convertView);
        }
    }

    boolean inviting = false;
    AdapterView.OnItemClickListener onListItemClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            try {
                if(inviting) return;
                inviting = true;
                final JSONObject buddyObject = (JSONObject) parent.getItemAtPosition(position);
                final Dtos.Profile buddy = new Dtos.Profile(buddyObject.toString());
                final String chatId = Utils.getChatId(me.identifier, buddy.identifier);
                FirebaseDatabase.getInstance().getReference("chats").child(chatId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.getValue() == null){
                            inviteToChat(buddyObject);
                        } else{
                            Dialogs.showDialog(getActivity(),
                                    getString(R.string.already_invited_title),
                                    String.format(getString(R.string.already_invited_text), buddy.username));
                        }
                        inviting = false;
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
            catch (Exception e){e.printStackTrace();}
        }
    };

    private void inviteToChat(final JSONObject o) {
        String name = "";
        try{ name = o.getString("username"); } catch (Exception e){e.printStackTrace();}
        try {
            Dialogs.showYesNoDialog(getActivity(),
                    getString(R.string.inviteTitle),
                    String.format(getString(R.string.inviteText), name),
                    getString(R.string.inviteYes),
                    getString(R.string.inviteNo),
                    new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(getActivity(), ChatActivity.class);
                            intent.putExtra("chatBuddy", o.toString());
                            intent.putExtra("isInviting", true);
                            startActivity(intent);
                        }
                    },
                    null);
        } catch (Exception e){e.printStackTrace();}
    }

    private void handleNotifications() {
        NotificationManager notificationManager =
                (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(TheFirebaseMessagingService.DISCOVERY_NOTIFICATION_ID);
    }
}
