package com.atn.tendy.utils;

import com.atn.tendy.Dtos;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import cz.msebera.android.httpclient.Header;

/**
 * Created by Admin on 23/07/2017.
 */

public class PushServer {
    private static final String API_KEY = "abcd1234";
    private static final String SERVER_KEY = "AAAAn0yYjrE:APA91bEkPJx2sKPFhHb5syj-lYWBR7aP1DaXWyc_rjZEdhqXS5L5OlZPrUzhaQ7lwbJ0M6Njmgr_kvbqQ3nRAAW98ZKSL2XzonBT2q62m5QQhsJmj_VBeZ_HNSmXdZEGboDYqchPYlKZ";
    private static final String SENDER_ID = "684184866481";
    private static final String BASE_SERVER_URL = "http://pushserver.atnisrael.com/";
    public static long timeDiff = 0;

    public static void test() {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(BASE_SERVER_URL + "test", new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Logs.log("test", responseString);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                Logs.log("test", responseString);
            }
        });
    }

    public static void getCurrentMillis() {
        AsyncHttpClient client = new AsyncHttpClient();
        client.setConnectTimeout(60 * 1000);
        client.get(BASE_SERVER_URL + "getCurrentMillis", new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Logs.log("getCurrentMillis", responseString);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                Logs.log("getCurrentMillis", responseString);
                Logs.log("System.currentTimeMillis()", System.currentTimeMillis() + "");
                try {
                    timeDiff = System.currentTimeMillis() - Utils.parseLong(responseString);
                } catch (Exception e) {
                    timeDiff = 0;
                    FirebaseCrash.report(e);
                }
            }
        });
    }

    public static void sendPush(final Dtos.Profile profile, final String title, final String message, final String data) {
        sendPushTries=0;
        FirebaseDatabase.getInstance().getReference("profiles").child(profile.identifier).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Dtos.Profile p = dataSnapshot.getValue(Dtos.Profile.class);
                if(p != null)
                    try {
                        _sendPush(p.pushToken,
                                (profile.deviceType == null || profile.deviceType.equals("android")) ? "" : title,
                                (profile.deviceType == null || profile.deviceType.equals("android")) ? "" : message, data);
                    } catch (Exception e){
                        _sendPush(p.pushToken,
                                "", "", data);
                    }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private static int sendPushTries = 0;
    public static void _sendPush(final String deviceToken, final String title, final String message, final String data) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.setConnectTimeout(60 * 1000);
        RequestParams params = new RequestParams();
        params.put("apiKey", API_KEY);
        params.put("serverKey", SERVER_KEY);
        params.put("senderId", SENDER_ID);
        params.put("deviceToken", deviceToken);
        params.put("title", title);
        params.put("message", message);
        params.put("data", data);
        client.post(BASE_SERVER_URL + "push", params, new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Logs.log("push failed", throwable == null ? responseString : throwable.getMessage());
                if(sendPushTries++ < 2)
                    _sendPush(deviceToken, title, message, data);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                 Logs.log("sendPush", responseString);
            }
        });
    }


}
