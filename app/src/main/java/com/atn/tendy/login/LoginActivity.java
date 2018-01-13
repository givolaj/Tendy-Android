package com.atn.tendy.login;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.afollestad.materialdialogs.MaterialDialog;
import com.atn.tendy.Dtos;
import com.atn.tendy.MainActivity;
import com.atn.tendy.R;
import com.atn.tendy.utils.Dialogs;
import com.atn.tendy.utils.Logs;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    CallbackManager fbCallbackManager;
    LoginButton fbLoginButton;
    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();
        fbCallbackManager = CallbackManager.Factory.create();
        fbLoginButton = (LoginButton) findViewById(R.id.login_button);
        fbLoginButton.setReadPermissions("email", "public_profile", "user_birthday");
        fbLoginButton.registerCallback(fbCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Logs.log("login", "facebook:onSuccess:" + loginResult);
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Logs.log("login", "facebook:onCancel");
            }

            @Override
            public void onError(FacebookException error) {
                Logs.log("login", "facebook:onError");
                FirebaseCrash.report(error);
            }
        });
    }

    public void signInWithFacebook() {
        LoginManager.getInstance().logOut();
        fbLoginButton.callOnClick();
    }

    private void handleFacebookAccessToken(AccessToken token) {
        try {
            Logs.log("login", "handleFacebookAccessToken:" + token);
            final AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
            final MaterialDialog dialog = Dialogs.showProgressIndicator(this);
            firebaseAuth.signInWithCredential(credential)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                GraphRequest request = GraphRequest.newMeRequest(
                                        AccessToken.getCurrentAccessToken(),
                                        new GraphRequest.GraphJSONObjectCallback() {
                                            @Override
                                            public void onCompleted(final JSONObject object, GraphResponse response) {
                                                try {
                                                    PreferenceManager.getDefaultSharedPreferences(LoginActivity.this)
                                                            .edit()
                                                            .putString("fbobject", object.toString())
                                                            .commit();
                                                    final Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                                    if(firebaseAuth.getCurrentUser().getDisplayName() == null ||
                                                            firebaseAuth.getCurrentUser().getDisplayName().toString().equals("")){
                                                        intent.putExtra("showWelcomeMessage", true);
                                                    }
                                                    final Dtos.Profile p = new Dtos.Profile();
                                                    p.identifier = firebaseAuth.getCurrentUser().getUid();
                                                    p.username = object.getString("name");
                                                    p.imageUrl = String.format(getString(R.string.fb_photo), object.getString("id"));
                                                    p.age = "";
                                                    try{
                                                        String[] bd = object.getString("birthday").split("/");
                                                        p.age = bd[1] + "/" + bd[0] + "/" + bd[2];
                                                    }catch (Exception e){}
                                                    p.gender = "other";
                                                    try {
                                                        if (object.getString("gender").equals("male"))
                                                            p.gender = "man";
                                                        else if (object.getString("gender").equals("female"))
                                                            p.gender = "woman";
                                                    }catch (Exception e){}
                                                    p.profession = "";
                                                    p.something = "";
                                                    p.pushToken = "";
                                                    p.lastEntry = "";

                                                    PreferenceManager.getDefaultSharedPreferences(LoginActivity.this)
                                                            .edit().putString("realProfile", p.toJsonString()).commit();
                                                    FirebaseDatabase.getInstance().getReference("realProfiles")
                                                            .child(firebaseAuth.getCurrentUser().getUid()).setValue(p);

                                                    firebaseAuth.getCurrentUser().updateProfile(
                                                            new UserProfileChangeRequest.Builder()
                                                                    .setDisplayName(p.username)
                                                                    .setPhotoUri(Uri.parse(p.imageUrl))
                                                                    .build()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {

                                                            startActivity(intent);
                                                            finish();
                                                        }
                                                    });
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        });
                                Bundle parameters = new Bundle();
                                parameters.putString("fields", "id,name,email,gender,birthday");
                                request.setParameters(parameters);
                                request.executeAsync();
                                Dialogs.dismissDialog(dialog);
                            } else {
                                Logs.log("login", "signInWithCredential:failure");
                                if (task.getException().toString().contains("An account already exists with the same email address but different sign-in credentials")) {
                                    Dialogs.showDialog(LoginActivity.this,
                                            getString(R.string.login),
                                            getString(R.string.cant_use_facebook_already_has_user));
                                }
                                Dialogs.dismissDialog(dialog);
                            }
                        }
                    });
        } catch (Exception e) {
            FirebaseCrash.report(e);
            Dialogs.showDialog(LoginActivity.this, getString(R.string.problem), getString(R.string.no_internet));
            throw e;
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            fbCallbackManager.onActivityResult(requestCode, resultCode, data);
        }catch (Exception e){}
    }
}
