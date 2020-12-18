package com.example.gamerz;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

public class SetupActivity extends AppCompatActivity {

    private CircleImageView setup_image;
    private Uri main_image_URI = null;
    private EditText setup_name;
    private Button setup_button;
    private StorageReference storage_reference;
    private FirebaseAuth firebase_auth;
    private ProgressBar setup_progress;
    private FirebaseFirestore firebase_firestore;
    private String user_id;
    private boolean is_changed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        Toolbar setup_toolbar = (Toolbar) findViewById(R.id.setupToolbar);
        setSupportActionBar(setup_toolbar);
        getSupportActionBar().setTitle("Account Settings");

        firebase_auth = FirebaseAuth.getInstance();

        user_id = firebase_auth.getCurrentUser().getUid();

        firebase_firestore = FirebaseFirestore.getInstance();
        storage_reference = FirebaseStorage.getInstance().getReference();

        setup_image = (CircleImageView) findViewById(R.id.setupImage);
        setup_name = (EditText) findViewById(R.id.setupName);
        setup_button = (Button) findViewById(R.id.setupButton);
        setup_progress = (ProgressBar) findViewById(R.id.setupProgress);

        setup_progress.setVisibility(View.VISIBLE);
        setup_button.setEnabled(false);

        firebase_firestore.collection("Users").document(user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    if (task.getResult().exists()) {
                        String name = task.getResult().getString("name");
                        String image = task.getResult().getString("image");

                        main_image_URI = Uri.parse(image);

                        setup_name.setText(name);

                        RequestOptions placeholder_request = new RequestOptions();
                        placeholder_request.placeholder(R.drawable.profile);
                        Glide.with(SetupActivity.this).setDefaultRequestOptions(placeholder_request).load(image).into(setup_image);
                    }
                } else {
                    String error = task.getException().getMessage();
                    Toast.makeText(SetupActivity.this, "Error reading Account Settings : " + error, Toast.LENGTH_LONG).show();
                }
                setup_progress.setVisibility(View.INVISIBLE);
                setup_button.setEnabled(true);
            }
        });

        setup_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String user_name = setup_name.getText().toString();
                if (!TextUtils.isEmpty(user_name) && main_image_URI != null) {
                    setup_progress.setVisibility(View.VISIBLE);
                    if (is_changed) {
                        final StorageReference image_path = storage_reference.child("profile_images").child(user_id + ".jpg");
                        image_path.putFile(main_image_URI).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                if (task.isSuccessful()) {
                                    store_firestore(image_path, user_name);
                                } else {
                                    String error = task.getException().getMessage();
                                    Toast.makeText(SetupActivity.this, "Error on Image to Firebase Storage : " + error, Toast.LENGTH_LONG).show();
                                    setup_progress.setVisibility(View.INVISIBLE);
                                }
                            }
                        });
                    } else {
                        store_firestore(null, user_name);
                    }
                }
            }
        });

        setup_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(SetupActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(SetupActivity.this, "Permission denied", Toast.LENGTH_LONG).show();
                        ActivityCompat.requestPermissions(SetupActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                    } else {
                        bring_image_picker();
                    }
                } else {
                    bring_image_picker();
                }
            }
        });
    }

    private void store_firestore(StorageReference image_path, final String user_name) {
        if (image_path != null) {
            image_path.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    Map<String, String> user_map = new HashMap<>();
                    user_map.put("name", user_name);
                    user_map.put("image", uri.toString());
                    firebase_firestore.collection("Users").document(user_id).set(user_map).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(SetupActivity.this, "The user settings are updated", Toast.LENGTH_LONG).show();
                                Intent main_intent = new Intent(SetupActivity.this, MainActivity.class);
                                startActivity(main_intent);
                                finish();
                            } else {
                                String error = task.getException().getMessage();
                                Toast.makeText(SetupActivity.this, "Error on Firestore : " + error, Toast.LENGTH_LONG).show();
                            }
                            setup_progress.setVisibility(View.INVISIBLE);
                        }
                    });
                }
            });
        } else {
            Map<String, String> user_map = new HashMap<>();
            user_map.put("name", user_name);
            user_map.put("image", main_image_URI.toString());
            firebase_firestore.collection("Users").document(user_id).set(user_map).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(SetupActivity.this, "The user settings are updated", Toast.LENGTH_LONG).show();
                        Intent main_intent = new Intent(SetupActivity.this, MainActivity.class);
                        startActivity(main_intent);
                        finish();
                    } else {
                        String error = task.getException().getMessage();
                        Toast.makeText(SetupActivity.this, "Error on Firestore : " + error, Toast.LENGTH_LONG).show();
                    }
                    setup_progress.setVisibility(View.INVISIBLE);
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                main_image_URI = result.getUri();
                setup_image.setImageURI(main_image_URI);
                is_changed = true;
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    private void bring_image_picker() {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1, 1)
                .start(SetupActivity.this);
    }
}