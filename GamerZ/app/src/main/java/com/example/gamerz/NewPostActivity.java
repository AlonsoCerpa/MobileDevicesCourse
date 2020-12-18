package com.example.gamerz;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import id.zelory.compressor.Compressor;

public class NewPostActivity extends AppCompatActivity {

    private Toolbar new_post_toolbar;
    private ImageView new_post_image;
    private EditText new_post_desc;
    private Button new_post_button;
    private Uri post_image_uri = null;
    private ProgressBar new_post_progress;
    private StorageReference storage_reference;
    private FirebaseFirestore firebase_firestore;
    private FirebaseAuth firebase_auth;
    private String current_user_id;
    private Bitmap compressed_image_file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);

        storage_reference = FirebaseStorage.getInstance().getReference();
        firebase_firestore = FirebaseFirestore.getInstance();
        firebase_auth = FirebaseAuth.getInstance();

        current_user_id = firebase_auth.getCurrentUser().getUid();

        new_post_toolbar = (Toolbar) findViewById(R.id.newPostToolbar);
        setSupportActionBar(new_post_toolbar);
        getSupportActionBar().setTitle("Add New Post");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        new_post_image = (ImageView) findViewById(R.id.newPostImage);
        new_post_desc = (EditText) findViewById(R.id.newPostDesc);
        new_post_button = (Button) findViewById(R.id.postButton);
        new_post_progress = (ProgressBar) findViewById(R.id.newPostProgress);

        new_post_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setMinCropResultSize(512, 512)
                        .setAspectRatio(1, 1)
                        .start(NewPostActivity.this);
            }
        });

        new_post_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String desc = new_post_desc.getText().toString();
                if (!TextUtils.isEmpty(desc) && post_image_uri != null) {
                    new_post_progress.setVisibility(View.VISIBLE);
                    final String random_name = UUID.randomUUID().toString();
                    final StorageReference file_path = storage_reference.child("post_images").child(random_name + ".jpg");
                    file_path.putFile(post_image_uri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            if (task.isSuccessful()) {
                                file_path.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                    @Override
                                    public void onComplete(@NonNull final Task<Uri> task) {
                                        final String download_uri = task.getResult().toString();
                                        if (task.isSuccessful()) {
                                            File new_image_file = new File(post_image_uri.getPath());
                                            try {
                                                compressed_image_file = new Compressor(NewPostActivity.this)
                                                        .setMaxHeight(100)
                                                        .setMaxWidth(100)
                                                        .setQuality(2)
                                                        .compressToBitmap(new_image_file);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }

                                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                            compressed_image_file.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                            byte[] thumb_data = baos.toByteArray();

                                            UploadTask upload_task = storage_reference.child("post_images/thumbs")
                                                    .child(random_name + ".jpg").putBytes(thumb_data);
                                            upload_task.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                                @Override
                                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                                    Task<Uri> task_download_thumb_uri = taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                        @Override
                                                        public void onSuccess(Uri uri) {
                                                            String download_thumb_uri = uri.toString();
                                                            Map<String, Object> post_map = new HashMap<>();
                                                            post_map.put("image_url", download_uri);
                                                            post_map.put("image_thumb", download_thumb_uri);
                                                            post_map.put("desc", desc);
                                                            post_map.put("user_id", current_user_id);
                                                            post_map.put("timestamp", FieldValue.serverTimestamp());
                                                            firebase_firestore.collection(  "Posts").add(post_map).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<DocumentReference> task) {
                                                                    if (task.isSuccessful()) {
                                                                        Toast.makeText(NewPostActivity.this, "Post was added", Toast.LENGTH_LONG).show();
                                                                        Intent main_intent = new Intent(NewPostActivity.this, MainActivity.class);
                                                                        startActivity(main_intent);
                                                                        finish();
                                                                    } else {

                                                                    }
                                                                    new_post_progress.setVisibility(View.INVISIBLE);
                                                                }
                                                            });
                                                        }
                                                    });
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {

                                                }
                                            });
                                        } else {
                                            new_post_progress.setVisibility(View.INVISIBLE);
                                        }
                                    }
                                });
                            } else {
                                new_post_progress.setVisibility(View.INVISIBLE);
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                post_image_uri = result.getUri();
                new_post_image.setImageURI(post_image_uri);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }
}