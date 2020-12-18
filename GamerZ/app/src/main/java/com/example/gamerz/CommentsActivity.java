package com.example.gamerz;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public class CommentsActivity extends AppCompatActivity {

    private Toolbar comment_toolbar;
    private EditText comment_field;
    private ImageView comment_post_button;

    private RecyclerView comment_list;
    private CommentsRecyclerAdapter commentsRecyclerAdapter;
    private List<Comments> commentsList;
    private DividerItemDecoration dividerItemDecoration;
    private LinearLayoutManager linearLayoutManager;

    private FirebaseFirestore firebase_firestore;
    private FirebaseAuth firebase_auth;

    private String blog_post_id;
    private String current_user_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        comment_toolbar = findViewById(R.id.commentToolbar);
        setSupportActionBar(comment_toolbar);
        getSupportActionBar().setTitle("Comments");

        firebase_auth = FirebaseAuth.getInstance();
        firebase_firestore = FirebaseFirestore.getInstance();

        current_user_id = firebase_auth.getCurrentUser().getUid();
        blog_post_id = getIntent().getStringExtra("blog_post_id");

        comment_field = findViewById(R.id.commentField);
        comment_post_button = findViewById(R.id.commentPostButton);
        comment_list = findViewById(R.id.commentList);

        //RecyclerView Firebase List
        commentsList = new ArrayList<>();
        commentsRecyclerAdapter = new CommentsRecyclerAdapter(commentsList);
        comment_list.setHasFixedSize(true);
        comment_list.setLayoutManager(new LinearLayoutManager(this));
        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        dividerItemDecoration = new DividerItemDecoration(comment_list.getContext(), linearLayoutManager.getOrientation());
        comment_list.setAdapter(commentsRecyclerAdapter);

        //RecyclerView Firebase List
        firebase_firestore.collection("Posts/" + blog_post_id + "/Comments").addSnapshotListener(CommentsActivity.this, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {

                if (queryDocumentSnapshots != null) {
                    for (DocumentChange doc: queryDocumentSnapshots.getDocumentChanges()) {
                        if (doc.getType() == DocumentChange.Type.ADDED) {
                            String comment_id = doc.getDocument().getId();
                            Comments comments = doc.getDocument().toObject(Comments.class);
                            commentsList.add(comments);
                            commentsRecyclerAdapter.notifyDataSetChanged();
                        }
                    }
                }
            }
        });

        comment_post_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String comment_message = comment_field.getText().toString();

                if (!comment_message.isEmpty()) {
                    Map<String, Object> comments_map = new HashMap<>();
                    comments_map.put("message", comment_message);
                    comments_map.put("user_id", current_user_id);
                    comments_map.put("timestamp", FieldValue.serverTimestamp());
                    firebase_firestore.collection("Posts/" + blog_post_id + "/Comments").add(comments_map).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentReference> task) {
                            if (!task.isSuccessful()) {
                                Toast.makeText(CommentsActivity.this, "Error Posting Comment: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            } else {
                                comment_field.setText("");
                            }
                        }
                    });
                }
            }
        });
    }
}