package com.atn.tendy.login;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.atn.tendy.MainActivity;
import com.atn.tendy.R;
import com.atn.tendy.utils.Dialogs;
import com.atn.tendy.utils.Logs;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class PhoneVerificationActivity extends AppCompatActivity {

    EditText phoneNumberET;
    TextView phoneVerificationTimer;
    MaterialDialog dialog;
    CountDownTimer timer;
    boolean canSendCode;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_verification);

        setupReferences();
    }

    private void setupReferences() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        phoneNumberET = (EditText) findViewById(R.id.phoneNumber);
        phoneVerificationTimer = (TextView) findViewById(R.id.phoneVerificationTimer);
        startTimer(prefs.getLong("verificationTime", 0));
    }

    private void startTimer(long time) {
        canSendCode = false;
        if(timer != null){
            timer.cancel();
            timer = null;
        }
        timer = new CountDownTimer(time, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                prefs.edit().putLong("verificationTime",millisUntilFinished).commit();
                phoneVerificationTimer.setText(String.format(getString(R.string.can_resend_code_in), (millisUntilFinished/1000) + ""));
            }

            @Override
            public void onFinish() {
                canSendCode = true;
                phoneVerificationTimer.setText(getString(R.string.click_on_this_button_to_get_a_verification_code));
            }
        };
        timer.start();
    }

    public void verifyPhoneNumber(View view) {
        if(canSendCode) {
            dialog = Dialogs.showProgressIndicator(this);
            String phoneNumber = phoneNumberET.getText().toString();
            if (phoneNumber.length() < 10 || !PhoneNumberUtils.isGlobalPhoneNumber(phoneNumber)) {
                Dialogs.showDialog(this,
                        getString(R.string.problem),
                        getString(R.string.not_valid_phone));
                Dialogs.dismissDialog(dialog);
                return;
            }

            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    phoneNumber,        // Phone number to verify
                    60,                 // Timeout duration
                    TimeUnit.SECONDS,   // Unit of timeout
                    this,               // Activity (for callback binding)
                    mCallbacks);        // OnVerificationStateChangedCallbacks
            startTimer(60000);
        } else{
            //Dialogs.showDialog(this, getString(R.string.problem), getString(R.string.wait_for_the_counter_to_zero_out));
            if(mVerificationId == null) return;
            Dialogs.dismissDialog(codeDialog);
            codeDialog = Dialogs.showInputAndButtonDialog(PhoneVerificationActivity.this,
                    getString(R.string.code_sent_title),
                    getString(R.string.code_sent_text),
                    new Dialogs.InputAndButtonDialogClickListener(){
                        @Override
                        public void onClick(String inputString) {
                            if(inputString.length() < 6){
                                Dialogs.showDialog(PhoneVerificationActivity.this,
                                        getString(R.string.problem),
                                        getString(R.string.wrong_code));
                                return;
                            }
                            dialog = Dialogs.showProgressIndicator(PhoneVerificationActivity.this);
                            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, inputString);
                            signInWithPhoneAuthCredential(credential);
                            Dialogs.dismissDialog(codeDialog);
                        }
                    });
        }
    }

    Dialog codeDialog = null;
    public String mVerificationId;
    public PhoneAuthProvider.ForceResendingToken mResendToken;
    PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(PhoneAuthCredential credential) {
            Log.d("verification", "onVerificationCompleted:" + credential);
            signInWithPhoneAuthCredential(credential);
        }

        @Override
        public void onVerificationFailed(FirebaseException e) {
            Log.w("verification", "onVerificationFailed", e);
            if (e instanceof FirebaseAuthInvalidCredentialsException) {
//                Dialogs.showDialog(PhoneVerificationActivity.this,
//                        getString(R.string.problem),
//                        getString(R.string.code_invalid));
            } else if (e instanceof FirebaseTooManyRequestsException) {

            }
            Dialogs.dismissDialog(dialog);
            Dialogs.showDialog(PhoneVerificationActivity.this,
                    getString(R.string.problem),
                    getString(R.string.cant_verify));
        }

        @Override
        public void onCodeSent(String verificationId,
                PhoneAuthProvider.ForceResendingToken token) {
            Log.d("verification", "onCodeSent:" + verificationId);
            mVerificationId = verificationId;
            mResendToken = token;
            Dialogs.dismissDialog(dialog);
            codeDialog = Dialogs.showInputAndButtonDialog(PhoneVerificationActivity.this,
                    getString(R.string.code_sent_title),
                    getString(R.string.code_sent_text),
                    new Dialogs.InputAndButtonDialogClickListener(){
                        @Override
                        public void onClick(String inputString) {
                            if(inputString.length() < 6){
                                Dialogs.showDialog(PhoneVerificationActivity.this,
                                        getString(R.string.problem),
                                        getString(R.string.wrong_code));
                                return;
                            }
                            dialog = Dialogs.showProgressIndicator(PhoneVerificationActivity.this);
                            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, inputString);
                            signInWithPhoneAuthCredential(credential);
                            Dialogs.dismissDialog(codeDialog);
                        }
                    });
        }
    };

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Logs.log("signed_in", FirebaseAuth.getInstance().getCurrentUser().getUid());
                    startActivity(new Intent(PhoneVerificationActivity.this, MainActivity.class));
                    finish();
                    Dialogs.dismissDialog(dialog);
                } else {
                    Dialogs.dismissDialog(dialog);
                    if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                        Dialogs.showDialog(PhoneVerificationActivity.this,
                                getString(R.string.problem),
                                getString(R.string.code_invalid));
                    } else {
                        Dialogs.showDialog(PhoneVerificationActivity.this,
                                getString(R.string.problem),
                                getString(R.string.login_failed_general));
                    }
                }
            }
        });
    }

    @Override
    public void onBackPressed(){
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
