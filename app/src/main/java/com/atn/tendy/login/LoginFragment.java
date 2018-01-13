package com.atn.tendy.login;


import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.atn.tendy.MainActivity;
import com.atn.tendy.R;
import com.atn.tendy.utils.Dialogs;
import com.atn.tendy.utils.Logs;
import com.atn.tendy.utils.Utils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.crash.FirebaseCrash;

public class LoginFragment extends Fragment {


    public LoginFragment() {
        // Required empty public constructor
    }

    TextView loginTV, registerTv, titleTV, forgotPassword, fb, terms;
    EditText emailET, passwordET, confirmET, nameET;

    TextView phoneVerification;
    Button sendBtn;
    FirebaseAuth firebaseAuth;
    SharedPreferences prefs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_login, container, false);
        setupReferences(v);
        firebaseAuth = FirebaseAuth.getInstance();
        Utils.setFontToViewGroup(getActivity(), (ViewGroup) v, "open");
        return v;
    }

    private void setupReferences(View v) {
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        loginTV = (TextView) v.findViewById(R.id.loginTv);
        registerTv = (TextView) v.findViewById(R.id.registerTv);
        titleTV = (TextView) v.findViewById(R.id.title);
        forgotPassword = (TextView) v.findViewById(R.id.forgotPassword);
        phoneVerification = (TextView) v.findViewById(R.id.phoneVerification);
        phoneVerification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), PhoneVerificationActivity.class));
                getActivity().finish();
            }
        });
        terms = (TextView) v.findViewById(R.id.terms);
        terms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "https://drive.google.com/file/d/0BzgQjgcjMClVdFlxTjRQNnZTUjg/view?usp=sharing";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        });

        emailET = (EditText) v.findViewById(R.id.email);
        passwordET = (EditText) v.findViewById(R.id.password);
        confirmET = (EditText) v.findViewById(R.id.confirm);
        nameET = (EditText) v.findViewById(R.id.name);

        sendBtn = (Button) v.findViewById(R.id.send);
        fb = (TextView) v.findViewById(R.id.fbText);

        loginTV.setOnClickListener(changeListener);
        registerTv.setOnClickListener(changeListener);
        sendBtn.setOnClickListener(loginListener);
        fb.setOnClickListener(fbClickListener);
        forgotPassword.setOnClickListener(forgotPasswordListener);
    }

    View.OnClickListener forgotPasswordListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!Utils.haveInternetConnection(getActivity())) {
                Dialogs.showDialog(getActivity(),
                        getString(R.string.problem),
                        getString(R.string.no_internet));
                return;
            }
            String email = emailET.getText().toString();
            if (!email.matches("[a-zA-Z0-9._-]+@[a-z]+.[a-z]+")) {
                Dialogs.showDialog(getActivity(),
                        getString(R.string.problem),
                        getString(R.string.write_valid_email));
                return;
            }
            firebaseAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Dialogs.showDialog(getActivity(), getString(R.string.success), getString(R.string.email_sent));
                            } else {
                                Logs.log("forgotPassword", task.getException().toString());
                                Dialogs.showDialog(getActivity(),
                                        getString(R.string.problem),
                                        getString(R.string.no_such_user));
                            }
                        }
                    });
            ;

        }
    };

    View.OnClickListener fbClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!Utils.haveInternetConnection(getActivity())) {
                Dialogs.showDialog(getActivity(),
                        getString(R.string.problem),
                        getString(R.string.no_internet));
                return;
            }
            ((LoginActivity) getActivity()).signInWithFacebook();
        }
    };


    View.OnClickListener changeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
                Logs.log("click", v.getTag().toString());
                if (v.getTag().toString().equals("login")) {
                    loginTV.setVisibility(View.VISIBLE);
                    registerTv.setVisibility(View.GONE);
                    confirmET.setVisibility(View.GONE);
                    nameET.setVisibility(View.GONE);
                    forgotPassword.setVisibility(View.VISIBLE);
                    titleTV.setText(getString(R.string.login));
                    sendBtn.setText(getString(R.string.loginBtn));
                } else {
                    loginTV.setVisibility(View.GONE);
                    registerTv.setVisibility(View.VISIBLE);
                    confirmET.setVisibility(View.VISIBLE);
                    nameET.setVisibility(View.VISIBLE);
                    forgotPassword.setVisibility(View.GONE);
                    titleTV.setText(getString(R.string.register));
                    sendBtn.setText(getString(R.string.registerBtn));
                }
            } catch (Exception e) {
                FirebaseCrash.report(e);
                throw e;
            }
        }
    };

    View.OnClickListener loginListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!Utils.haveInternetConnection(getActivity())) {
                Dialogs.showDialog(getActivity(),
                        getString(R.string.problem),
                        getString(R.string.no_internet));
                return;
            }
            try {
                String email = emailET.getText().toString();
                String password = passwordET.getText().toString();
                String confirm = confirmET.getText().toString();
                final String name = nameET.getText().toString();
                if (!email.matches("[a-zA-Z0-9._-]+@[a-z]+.[a-z]+")) {
                    Dialogs.showDialog(getActivity(),
                            getString(R.string.problem),
                            getString(R.string.write_valid_email));
                    return;
                }
                if (password == null || password.length() < 6) {
                    Dialogs.showDialog(getActivity(),
                            getString(R.string.problem),
                            getString(R.string.no_password));
                    return;
                }
                final MaterialDialog dialog = Dialogs.showProgressIndicator(getActivity());
                if (loginTV.getVisibility() == View.VISIBLE) {
                    firebaseAuth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(getActivity(),
                                    new OnCompleteListener<AuthResult>() {
                                        @Override
                                        public void onComplete(@NonNull Task<AuthResult> task) {
                                            if (task.isSuccessful()) {
                                                Logs.log("signed_in", firebaseAuth.getCurrentUser().getUid());
                                                startActivity(new Intent(getActivity(), MainActivity.class));
                                                getActivity().finish();
                                                Dialogs.dismissDialog(dialog);
                                            } else {
                                                Dialogs.dismissDialog(dialog);
                                                Dialogs.showDialog(getActivity(),
                                                        getString(R.string.problem),
                                                        getString(R.string.login_failed));
                                            }
                                        }
                                    });

                } else {
                    if (confirm == null || !confirm.equals(password)) {
                        Dialogs.dismissDialog(dialog);
                        Dialogs.showDialog(getActivity(),
                                getString(R.string.problem),
                                getString(R.string.passwords_dont_match));
                        return;
                    }
                    if (name != null && name.isEmpty()) {
                        Dialogs.dismissDialog(dialog);
                        Dialogs.showDialog(getActivity(),
                                getString(R.string.problem),
                                getString(R.string.gotta_have_a_name));
                        return;
                    }
                    firebaseAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        if (firebaseAuth.getCurrentUser().getDisplayName() == null ||
                                                firebaseAuth.getCurrentUser().getDisplayName().toString().equals("")) {
                                            firebaseAuth.getCurrentUser().updateProfile(
                                                    new UserProfileChangeRequest.Builder()
                                                            .setDisplayName(name)
                                                            .setPhotoUri(Utils.getUriToDrawable(getActivity(), R.drawable.male3))
                                                            .build()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    Intent intent = new Intent(getActivity(), MainActivity.class);
                                                    intent.putExtra("showWelcomeMessage", true);
                                                    startActivity(intent);
                                                    getActivity().finish();
                                                }
                                            });
                                        } else {
                                            startActivity(new Intent(getActivity(), MainActivity.class));
                                            getActivity().finish();
                                        }
                                        Dialogs.dismissDialog(dialog);
                                    } else {
                                        FirebaseCrash.report(task.getException());
                                        Dialogs.dismissDialog(dialog);
                                        Dialogs.showDialog(getActivity(), getString(R.string.problem), getString(R.string.username_taken));
                                    }
                                }
                            });
                }
            } catch (Exception e) {
                FirebaseCrash.report(e);
                Dialogs.showDialog(getActivity(), getString(R.string.problem), getString(R.string.no_internet));
                throw e;
            }
        }
    };

}
