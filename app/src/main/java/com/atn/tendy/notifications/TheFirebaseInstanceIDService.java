package com.atn.tendy.notifications;

import com.atn.tendy.Dtos;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;


public class TheFirebaseInstanceIDService extends FirebaseInstanceIdService {
    @Override
    public void onTokenRefresh() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        sendRegistrationToServer(refreshedToken);
    }

    private void sendRegistrationToServer(final String token) {
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if(user == null) return;
            FirebaseDatabase.getInstance().getReference("profiles")
                    .child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Dtos.Profile p = dataSnapshot.getValue(Dtos.Profile.class);
                    if(p != null) {
                        p.pushToken = token;
                        FirebaseDatabase.getInstance().getReference("profiles").child(p.identifier).setValue(p);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        } catch (Exception e){ e.printStackTrace();}
    }
}
