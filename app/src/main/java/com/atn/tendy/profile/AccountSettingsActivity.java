package com.atn.tendy.profile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.ScrollView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.atn.tendy.Dtos;
import com.atn.tendy.R;
import com.atn.tendy.chat.ChatActivity;
import com.atn.tendy.utils.Dialogs;
import com.atn.tendy.utils.Logs;
import com.atn.tendy.utils.Utils;
import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.codetroopers.betterpickers.calendardatepicker.MonthAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static com.atn.tendy.profile.ProfileFragment.PICK_IMAGE;
import static com.atn.tendy.profile.ProfileFragment.PICK_IMAGE_PERMISSION_REQUEST;
import static com.atn.tendy.utils.Utils.countSubstring;

public class AccountSettingsActivity extends AppCompatActivity {

    de.hdodenhof.circleimageview.CircleImageView realImage;
    FirebaseUser user;
    FirebaseStorage storage;
    ScrollView scrollView;
    RadioButton man, woman, other;
    EditText name, birthday, profession, something;
    Button save;
    SharedPreferences prefs;
    Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);

        setupReferences();
        setupProfile();
        if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("firstTimeInSettings", true)){
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("firstTimeInSettings", false).commit();
            Dialogs.showCustomDialogOneButton(this, R.layout.real_profile_intro, null);
        }
    }

    private void setupProfile() {
        final MaterialDialog dialog = Dialogs.showProgressIndicator(this);
        try {
            if (user.getPhotoUrl() == null)
                realImage.setImageResource(R.drawable.male3);
            else if (user.getPhotoUrl().toString().contains("android.resource:")) {
                realImage.setImageURI(user.getPhotoUrl());
            } else {
                ImageLoader.getInstance().displayImage(
                        user.getPhotoUrl().toString(),
                        realImage,
                        Utils.displayImageOptions);
            }
        } catch (Exception e){e.printStackTrace();}
        FirebaseDatabase.getInstance().getReference("realProfiles").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Dtos.Profile realProfile = dataSnapshot.getValue(Dtos.Profile.class);
                if (realProfile == null) {
                    try {
                        JSONObject object = new JSONObject(prefs.getString("fbobject", "{}"));
                        try{
                            String[] bd = object.getString("birthday").split("/");
                            birthday.setText(bd[1] + "/" + bd[0] + "/" + bd[2]);
                        } catch (Exception e){e.printStackTrace();}
                        name.setText(user.getDisplayName());
                        if (object.getString("gender").equals("male")) man.setChecked(true);
                        else if (object.getString("gender").equals("female")) woman.setChecked(true);
                        else other.setChecked(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Dialogs.dismissDialog(dialog);
                    return;
                }
                findViewById(R.id.message).setVisibility(View.VISIBLE);
                prefs.edit().putString("realProfile", realProfile.toJsonString()).commit();
                name.setText(realProfile.username);
                birthday.setText(realProfile.age);
                profession.setText(realProfile.profession);
                something.setText(realProfile.something);
                if (realProfile.gender.equals("man")) man.setChecked(true);
                else if (realProfile.gender.equals("woman")) woman.setChecked(true);
                else other.setChecked(true);
                name.setEnabled(false);
                //birthday.setEnabled(false);
                //man.setEnabled(false);
                //woman.setEnabled(false);
                //other.setEnabled(false);
                Dialogs.dismissDialog(dialog);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void setupReferences() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        user = FirebaseAuth.getInstance().getCurrentUser();
        storage = FirebaseStorage.getInstance();
        realImage = (de.hdodenhof.circleimageview.CircleImageView) findViewById(R.id.realProfileImage);
        realImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        scrollView = (ScrollView) findViewById(R.id.scrollView);

        man = (RadioButton) findViewById(R.id.man);
        man.setOnCheckedChangeListener(genderChangeListener);
        woman = (RadioButton) findViewById(R.id.woman);
        woman.setOnCheckedChangeListener(genderChangeListener);
        other = (RadioButton) findViewById(R.id.other);
        other.setOnCheckedChangeListener(genderChangeListener);

        name = (EditText) findViewById(R.id.name);
        birthday = (EditText) findViewById(R.id.birthday);
        birthday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] dateParts = { "01", "01", "1990"};
                if(birthday.getText().toString().length() == 10){
                    String[] _dateParts = birthday.getText().toString().split("/");
                    if(_dateParts.length == 3)
                        dateParts = _dateParts;
                }
                CalendarDatePickerDialogFragment cdp = new CalendarDatePickerDialogFragment()
                        .setOnDateSetListener(new CalendarDatePickerDialogFragment.OnDateSetListener() {
                            @Override
                            public void onDateSet(CalendarDatePickerDialogFragment dialog, int year, int monthOfYear, int dayOfMonth) {
                                calendar.set(Calendar.YEAR, year);
                                calendar.set(Calendar.MONTH, monthOfYear);
                                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                                birthday.setText(new SimpleDateFormat("dd/MM/yyyy").format(calendar.getTime()));
                            }
                        })
                        .setFirstDayOfWeek(Calendar.SUNDAY)
                        .setPreselectedDate(Utils.parseInt(dateParts[2]), Utils.parseInt(dateParts[1])-1, Utils.parseInt(dateParts[0]))
                        .setDateRange(null, new MonthAdapter.CalendarDay())
                        .setDoneText(getString(R.string.yes))
                        .setCancelText(getString(R.string.no));
                cdp.show(getSupportFragmentManager(), "");
            }
        });
        profession = (EditText) findViewById(R.id.profession);
        something = (EditText) findViewById(R.id.something);
        something.addTextChangedListener(textWatcher);
        something.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    something.setSelection(something.getText().length());
            }
        });
        save = (Button) findViewById(R.id.save);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveData();
            }
        });
    }

    void openGallery() {
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Logs.log("onActivityResult", (resultCode == RESULT_OK) + "");
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            try {
                if (data != null && data.getData() != null) {
                    Uri selectedImage = data.getData();
                    String url = data.getData().toString();
                    if (url.startsWith("content://com.google.android.apps.photos.content")) {
                        try {
                            InputStream is = getContentResolver().openInputStream(selectedImage);
                            if (is != null) {
                                Bitmap pictureBitmap = BitmapFactory.decodeStream(is);
                                realImage.setImageBitmap(pictureBitmap);
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    } else {
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};
                        Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                        cursor.moveToFirst();
                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        String picturePath = cursor.getString(columnIndex);
                        cursor.close();
                        Bitmap pictureBitmap = BitmapFactory.decodeFile(picturePath);
                        realImage.setImageBitmap(pictureBitmap);
                    }
                } else {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    options.inScaled = false;
                    File file = new File(cameraUri.getPath());
                    Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(file), null, options);
                    bitmap = Utils.getImageWithPortraitOrientation(bitmap, cameraUri);
                    realImage.setImageBitmap(bitmap);
                }
            } catch (Exception e) {
                FirebaseCrash.report(e);
            }
        }
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

    private void saveData() {
        if (!Utils.haveInternetConnection(this)) {
            Dialogs.showDialog(this,
                    getString(R.string.problem),
                    getString(R.string.no_internet));
            return;
        }
        String _name = name.getText().toString();
        String _age = birthday.getText().toString();
        String _profession = profession.getText().toString();
        String _something = something.getText().toString();
        String _gender = man.isChecked() ? "man" : (woman.isChecked() ? "woman" : "other");
        if (_name.length() == 0 || _age.length() == 0) {
            Dialogs.showDialog(this, getString(R.string.missing_fields), getString(R.string.cant_without_name));
            return;
        }
        final MaterialDialog dialog = Dialogs.showProgressIndicator(this);
        final Dtos.Profile profile = new Dtos.Profile(_name,
                _age, _profession, _something, _gender,
                "", "", "0",
                user.getUid(), FirebaseInstanceId.getInstance().getToken());

        Bitmap bitmap = Utils.resizeBitmap(((BitmapDrawable) realImage.getDrawable()).getBitmap(), ChatActivity.RESIZE_BITMAP_SIZE);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();
        StorageReference imgRef = storage.getReference().child("profileImages").child(user.getUid() + "real");
        UploadTask uploadTask = imgRef.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Dialogs.showDialog(AccountSettingsActivity.this,
                        getString(R.string.issue),
                        getString(R.string.cant_image_right_now));
                saveProfile(profile, dialog);
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            @SuppressWarnings("VisibleForTests")
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                String imageUrl = taskSnapshot.getDownloadUrl().toString();
                profile.imageUrl = imageUrl;
                saveProfile(profile, dialog);
            }
        });
    }

    private void saveProfile(Dtos.Profile profile, MaterialDialog dialog) {
        Logs.log("realProfile", profile.toJsonString());
        prefs.edit().putString("realProfile", profile.toJsonString()).commit();
        FirebaseDatabase.getInstance().getReference("realProfiles").child(user.getUid()).setValue(profile);
        user.updateProfile(new UserProfileChangeRequest.Builder()
                .setPhotoUri(Uri.parse(profile.imageUrl))
                .setDisplayName(profile.username)
                .build()).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Logs.log("updateProfile", "User profile updated.");
                } else {
                    Logs.log("updateProfile", task.getException().toString());
                }
            }
        });
        Dialogs.dismissDialog(dialog);
        Dialogs.showSuccessfullySaved(this, new Runnable() {
            @Override
            public void run() {
                finish();
            }
        });
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (countSubstring(something.getText().toString(), "\n") == 2) {
                something.setText(
                        something.getText().subSequence(0, something.length() - 1)
                );
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(something.getWindowToken(), 0);
                something.clearFocus();
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    CompoundButton.OnCheckedChangeListener genderChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (user.getPhotoUrl() == null || user.getPhotoUrl().equals("") ||
                    user.getPhotoUrl().toString().contains("android.resource:")) {
                if (woman.isChecked()) {
                    realImage.setImageResource(R.drawable.female3);
                } else {
                    realImage.setImageResource(R.drawable.male3);
                }
            }
        }
    };

//    @Override
//    public void onBackPressed() {
//        if (Dtos.Profile.isValidProfile(prefs.getString("realProfile", ""))) {
//            super.onBackPressed();
//        } else {
//            Dialogs.showDialog(this,
//                    getString(R.string.problem),
//                    getString(R.string.cant_leave_before_saving));
//        }
//    }
}
