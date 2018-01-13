package com.atn.tendy.profile;


import android.app.Dialog;
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
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.ScrollView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.atn.tendy.Dtos;
import com.atn.tendy.MainActivity;
import com.atn.tendy.R;
import com.atn.tendy.utils.Dialogs;
import com.atn.tendy.utils.Logs;
import com.atn.tendy.utils.Utils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.app.Activity.RESULT_OK;
import static com.atn.tendy.chat.ChatActivity.RESIZE_BITMAP_SIZE;
import static com.atn.tendy.utils.Utils.countSubstring;


/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment {

    static final int PICK_IMAGE = 1;
    static final int PICK_IMAGE_PERMISSION_REQUEST = 2;

    public ProfileFragment() {
        // Required empty public constructor
    }

    FirebaseDatabase database;
    FirebaseUser user;
    FirebaseStorage storage;
    de.hdodenhof.circleimageview.CircleImageView image;
    RadioButton man, woman, other;
    EditText username, age, profession, something;
    Button save;
    ScrollView scrollView;
    boolean imageChanged = false;
    String imageUrl = null;
    ImageLoader imageLoader = ImageLoader.getInstance();
    SharedPreferences prefs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);
        setupReferences(v);
        refresh();
        return v;
    }

    public void refresh() {
        final MaterialDialog dialog = Dialogs.showProgressIndicator(getActivity());
        String profileJson = prefs.getString("profile", "");
        Log.i("profile", profileJson);
        if (!Dtos.Profile.isValidProfile(profileJson)) {
            database.getReference("profiles")
                    .child(user.getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Dtos.Profile profile = dataSnapshot.getValue(Dtos.Profile.class);
                            if (profile == null) {
                                imageChanged = true;
                                try {
                                    String rpString = prefs.getString("realProfile", "");
                                    if(Dtos.Profile.isValidProfile(rpString)) {
                                        Dtos.Profile rp = new Dtos.Profile(rpString);
                                        age.setText(Utils.getAgeFromDate(rp.age));
                                        if (rp.gender.equals("man")) man.setChecked(true);
                                        else if(rp.gender.equals("woman")) woman.setChecked(true);
                                        else other.setChecked(true);
                                    }
                                } catch (Exception e){e.printStackTrace();}
                                Dialogs.dismissDialog(dialog);
                                return;
                            }
                            updateScreenWithProfile(profile, dialog);
                            try {
                                profile.pushToken = FirebaseInstanceId.getInstance().getToken();
                                prefs.edit().putString("profile", profile.toJsonString()).commit();
                                database.getReference("profiles")
                                        .child(user.getUid())
                                        .setValue(profile);
                                ((MainActivity) getActivity()).setupDrawer();
                            } catch (Exception e){ e.printStackTrace(); }

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Dialogs.dismissDialog(dialog);
                        }
                    });
        } else {
            updateScreenWithProfile(new Dtos.Profile(profileJson), dialog);
        }
    }

    synchronized void updateScreenWithProfile(Dtos.Profile profile, final Dialog dialog) {
        username.setText(profile.username);
        age.setText(profile.age);
        profession.setText(profile.profession);
        something.setText(profile.something);
        if (profile.gender.equals("man")) man.setChecked(true);
        else if(profile.gender.equals("woman")) woman.setChecked(true);
        else other.setChecked(true);
        if (imageUrl == null ||
                profile.imageUrl.equals(imageUrl) == false) { //refresh only if the images are different
            imageUrl = profile.imageUrl;
            imageLoader.loadImage(imageUrl, Utils.displayImageOptions, new ImageLoadingListener() {
                @Override
                public void onLoadingStarted(String imageUri, View view) {

                }

                @Override
                public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                    Dialogs.dismissDialog(dialog);
                    image.setImageResource(R.drawable.male1whitebg);
                }

                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    Dialogs.dismissDialog(dialog);
                    image.setImageBitmap(loadedImage);
                }

                @Override
                public void onLoadingCancelled(String imageUri, View view) {
                    Dialogs.dismissDialog(dialog);
                    image.setImageResource(R.drawable.male1whitebg);
                }
            });
        } else {
            Dialogs.dismissDialog(dialog);
        }
    }

    private void setupReferences(View v) {
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        scrollView = (ScrollView) v.findViewById(R.id.scrollView);
        image = (de.hdodenhof.circleimageview.CircleImageView) v.findViewById(R.id.profileFragmentImage);
        View.OnClickListener galleryClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        };
        image.setOnClickListener(galleryClick);
        v.findViewById(R.id.imageSubtext).setOnClickListener(galleryClick);

        man = (RadioButton) v.findViewById(R.id.man);
        man.setOnCheckedChangeListener(genderChangeListener);
        woman = (RadioButton) v.findViewById(R.id.woman);
        woman.setOnCheckedChangeListener(genderChangeListener);
        other = (RadioButton) v.findViewById(R.id.other);
        other.setOnCheckedChangeListener(genderChangeListener);

        username = (EditText) v.findViewById(R.id.username);
        age = (EditText) v.findViewById(R.id.age);
        profession = (EditText) v.findViewById(R.id.profession);
        something = (EditText) v.findViewById(R.id.something);
        something.addTextChangedListener(textWatcher);
        something.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus)
                    something.setSelection(something.getText().length());
            }
        });
        save = (Button) v.findViewById(R.id.save);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveData();
            }
        });

        Utils.setFontToViewGroup(getActivity(), (ViewGroup) v, "open");
    }

    void openGallery() {
        cameraUri = null;
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(getActivity(),
                android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
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
        File photo = Utils.createImageFile(getActivity());
        if(photo != null){
            cameraUri = Uri.fromFile(photo);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
        }
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});
        startActivityForResult(chooserIntent, PICK_IMAGE);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            try {
                if (data != null && data.getData() != null) {
                    Uri selectedImage = data.getData();
                    String url = data.getData().toString();
                    if (url.startsWith("content://com.google.android.apps.photos.content")) {
                        try {
                            InputStream is = getActivity().getContentResolver().openInputStream(data.getData());
                            if (is != null) {
                                Bitmap pictureBitmap = BitmapFactory.decodeStream(is);
                                image.setImageBitmap(pictureBitmap);
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    } else {
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};
                        Cursor cursor = getActivity().getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                        if(cursor == null) {
                            image.setImageResource(R.drawable.male1whitebg);
                            Dialogs.showDialog(getActivity(),
                                    getString(R.string.problem),
                                    getString(R.string.issue_with_image));
                            return;
                        }
                        cursor.moveToFirst();
                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        String picturePath = cursor.getString(columnIndex);
                        cursor.close();
                        image.setImageBitmap(BitmapFactory.decodeFile(picturePath));
                    }
                } else {
                    if(cameraUri == null) return;
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    options.inScaled = false;
                    File file = new File(cameraUri.getPath());
                    Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(file), null, options);
                    bitmap = Utils.getImageWithPortraitOrientation(bitmap, cameraUri);
                    image.setImageBitmap(bitmap);
                }
                imageChanged = true;
            } catch (Exception e) {
                FirebaseCrash.report(e);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == ProfileFragment.PICK_IMAGE_PERMISSION_REQUEST) {
            Logs.log("in", "in");
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                _openGallery();
            }
        }
    }

    void saveData() {
        if (!Utils.haveInternetConnection(getActivity())) {
            Dialogs.showDialog(getActivity(),
                    getString(R.string.problem),
                    getString(R.string.no_internet));
            return;
        }
        String _username = username.getText().toString();
        String _age = age.getText().toString();
        String _profession = profession.getText().toString();
        String _something = something.getText().toString();
        String _gender = man.isChecked() ? "man" : (woman.isChecked() ? "woman" : "other");
        if (_username.length() == 0) {
            Dialogs.showDialog(getActivity(), getString(R.string.missing_fields), getString(R.string.cant_without_username));
            return;
        }
        final MaterialDialog dialog = Dialogs.showProgressIndicator(getActivity());
        final Dtos.Profile profile = new Dtos.Profile(_username,
                _age, _profession, _something, _gender,
                "", "", new SimpleDateFormat("dd/MM/yy").format(new Date()),
                user.getUid(), FirebaseInstanceId.getInstance().getToken());

        if (imageChanged || prefs.getString("profile", "").equals("")) {
            imageChanged = false;
            Bitmap bitmap = Utils.resizeBitmap(((BitmapDrawable) image.getDrawable()).getBitmap(), RESIZE_BITMAP_SIZE);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] data = baos.toByteArray();
            StorageReference imgRef = storage.getReference().child("profileImages").child(user.getUid());
            UploadTask uploadTask = imgRef.putBytes(data);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    saveProfile(profile, dialog);
                    Dialogs.showDialog(getActivity(),
                            getString(R.string.issue),
                            getString(R.string.cant_image_right_now));
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                @SuppressWarnings("VisibleForTests")
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    imageUrl = taskSnapshot.getDownloadUrl().toString();
                    profile.imageUrl = imageUrl;
                    saveProfile(profile, dialog);
                }
            });
        } else { //image not changed
            profile.imageUrl = imageUrl == null ? "" : imageUrl;
            saveProfile(profile, dialog);
        }
    }

    private void saveProfile(Dtos.Profile profile, MaterialDialog dialog) {
        database.getReference("profiles")
                .child(profile.identifier)
                .setValue(profile);
        prefs.edit().putString("profile", profile.toJsonString()).commit();
        Dialogs.dismissDialog(dialog);
        Dialogs.showSuccessfullySaved(getActivity(), new Runnable() {
            @Override
            public void run() {
                ((MainActivity) getActivity()).goToDiscovery();
            }
        });
        ((MainActivity) getActivity()).setupDrawer();
        Logs.log("profile", profile.toJsonString());
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if(countSubstring(something.getText().toString(), "\n") == 2){
                something.setText(
                        something.getText().subSequence(0, something.length()-1)
                );
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(something.getWindowToken(), 0);
                something.clearFocus();
                scrollView.fullScroll(View.FOCUS_DOWN);

            }
        }

        @Override
        public void afterTextChanged(Editable s) {}
    };

    CompoundButton.OnCheckedChangeListener genderChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(imageUrl == null || imageUrl.equals("")) {
                if (woman.isChecked()) {
                    image.setImageResource(R.drawable.female1whitebg);
                } else{
                    image.setImageResource(R.drawable.male1whitebg);
                }
            }
        }
    };
}
