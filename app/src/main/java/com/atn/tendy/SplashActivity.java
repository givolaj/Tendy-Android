package com.atn.tendy;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.atn.tendy.login.LoginActivity;
import com.atn.tendy.utils.Dialogs;
import com.atn.tendy.utils.Utils;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    private static final int COARSE_LOCATION_PERMISSION_REQUEST = 2;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int NOTIFICATION_POLICY_CODE = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Log.i("sha",Utils.getSha1(this));
//        Log.i("hash", Utils.getHashKey(this));
        //Utils.requestDisableDoze(this);
        checkPermission();
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION},
                    COARSE_LOCATION_PERMISSION_REQUEST);
        } else {
            CheckBlueToothState();
        }
    }

    private void CheckBlueToothState() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            Dialogs.showDialog(this, getString(R.string.problem), getString(R.string.no_bluetooth), new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            });
        } else if (!adapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            checkServices();
        }
    }

    void checkServices() {
        Dialogs.showNoInternetDialog(this, new Runnable() {
            @Override
            public void run() {
                if (!Utils.checkGooglePlayServiceAvailability(SplashActivity.this, 11200000)) {
                    int responseCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(SplashActivity.this);
                    Dialog dialog = GooglePlayServicesUtil.getErrorDialog(responseCode, SplashActivity.this, 0);
                    dialog.show();
                } else {
                    if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                        FirebaseAuth.getInstance().getCurrentUser().reload().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful() && FirebaseAuth.getInstance().getCurrentUser() != null) {
                                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                                    finish();
                                } else{
                                    Utils.runWithDelay(splashCode, 1500);
                                }
                            }
                        });
                    }
                    else{
                        Utils.runWithDelay(splashCode, 1500);
                    }
                }
            }
        });
    }

    Runnable splashCode = new Runnable() {
        @Override
        public void run() {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SplashActivity.this);
            if (prefs.getBoolean("shouldShowSlides", true)) {
                startActivity(new Intent(SplashActivity.this, SlidesActivity.class));
                finish();
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish();
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == COARSE_LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                CheckBlueToothState();
            } else {
                Dialogs.showDialog(this, getString(R.string.problem), getString(R.string.cant_go_on_without_permission), new Runnable() {
                    @Override
                    public void run() {
                        checkPermission();
                    }
                });
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK)
            CheckBlueToothState();
        else {
            Dialogs.showDialog(this, getString(R.string.problem), getString(R.string.no_bluetooth), new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            });
        }
    }
}
