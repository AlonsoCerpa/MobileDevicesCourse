package com.example.gamerz;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import org.w3c.dom.Text;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import de.hdodenhof.circleimageview.CircleImageView;

public class BlogRecyclerAdapter extends RecyclerView.Adapter<BlogRecyclerAdapter.ViewHolder> {

    public List<BlogPost> blog_list;
    public Context context;

    private FirebaseFirestore firebase_firestore;
    private FirebaseAuth firebase_auth;

    public BlogRecyclerAdapter(List<BlogPost> blog_list) {
        this.blog_list = blog_list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.blog_list_item, parent, false);
        context = parent.getContext();
        firebase_firestore = FirebaseFirestore.getInstance();
        firebase_auth = FirebaseAuth.getInstance();
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        holder.setIsRecyclable(false);

        final String blog_post_id = blog_list.get(position).BlogPostId;
        final String current_user_id = firebase_auth.getCurrentUser().getUid();

        String desc_data = blog_list.get(position).getDesc();
        holder.set_desc_text(desc_data);

        String image_url = blog_list.get(position).getImage_url();
        String thumb_uri = blog_list.get(position).getImage_thumb();
        holder.set_blog_image(image_url, thumb_uri);

        String user_id = blog_list.get(position).getUser_id();
        firebase_firestore.collection("Users").document(user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    String user_name = task.getResult().getString("name");
                    String user_image = task.getResult().getString("image");

                    holder.set_user_data(user_name, user_image);
                } else {

                }
            }
        });

        long millisecond = blog_list.get(position).getTimestamp().getTime();
        String date_string = DateFormat.format("MM/dd/yyyy", new Date(millisecond)).toString();
        holder.set_time(date_string);

        //Get Likes Count
        firebase_firestore.collection("Posts/" + blog_post_id + "/Likes").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                    int count = queryDocumentSnapshots.size();
                    holder.update_likes_count(count);
                } else {
                    holder.update_likes_count(0);
                }
            }
        });

        //Get Comments Count
        firebase_firestore.collection("Posts/" + blog_post_id + "/Comments").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                    int count = queryDocumentSnapshots.size();
                    holder.update_comments_count(count);
                } else {
                    holder.update_comments_count(0);
                }
            }
        });

        //Get Likes
        firebase_firestore.collection("Posts/" + blog_post_id + "/Likes").document(current_user_id).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    holder.blog_like_button.setImageDrawable(context.getDrawable(R.mipmap.action_like_accent));
                } else {
                    holder.blog_like_button.setImageDrawable(context.getDrawable(R.mipmap.action_like_gray));
                }
            }
        });

        //Likes feature
        holder.blog_like_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                firebase_firestore.collection("Posts/" + blog_post_id + "/Likes").document(current_user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (!task.getResult().exists()) {
                            Map<String, Object> likes_map = new HashMap<>();
                            likes_map.put("timestamp", FieldValue.serverTimestamp());

                            firebase_firestore.collection("Posts/" + blog_post_id + "/Likes").document(current_user_id).set(likes_map);
                        } else {
                            firebase_firestore.collection("Posts/" + blog_post_id + "/Likes").document(current_user_id).delete();
                        }
                    }
                });
            }
        });

        holder.blog_comment_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent comment_intent = new Intent(context, CommentsActivity.class);
                comment_intent.putExtra("blog_post_id", blog_post_id);
                context.startActivity(comment_intent);
            }
        });

    }

    @Override
    public int getItemCount() {
        return blog_list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private View m_view;
        private TextView desc_view;
        private ImageView blog_image_view;
        private TextView blog_date;
        private TextView blog_user_name;
        private CircleImageView blog_user_image;
        private ImageView blog_like_button;
        private TextView blog_like_count;
        private ImageView blog_comment_button;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            m_view = itemView;

            blog_like_button = m_view.findViewById(R.id.blogLikeButton);
            blog_comment_button = m_view.findViewById(R.id.blogCommentIcon);
        }

        public void set_desc_text(String desc_text) {
            desc_view = m_view.findViewById(R.id.blogDesc);
            desc_view.setText(desc_text);
        }

        public void set_blog_image(String download_uri, String thumb_uri) {
            blog_image_view = m_view.findViewById(R.id.blogImage);

            RequestOptions request_options = new RequestOptions();
            request_options.placeholder(R.drawable.gray_rect);

            Glide.with(context).applyDefaultRequestOptions(request_options).load(download_uri).thumbnail(
                    Glide.with(context).load(thumb_uri)
            ) .into(blog_image_view);
        }

        public void set_time(String date) {
            blog_date = m_view.findViewById(R.id.blogDate);
            blog_date.setText(date);
        }

        public void set_user_data(String name, String image) {
            blog_user_image = m_view.findViewById(R.id.blogUserImage);
            blog_user_name = m_view.findViewById(R.id.blogUserName);

            blog_user_name.setText(name);

            RequestOptions placeholder_options = new RequestOptions();
            placeholder_options.placeholder(R.drawable.grey_circle);

            Glide.with(context).applyDefaultRequestOptions(placeholder_options).load(image).into(blog_user_image);
        }

        public void update_likes_count(int count) {
            blog_like_count = m_view.findViewById(R.id.blogLikeCount);
            blog_like_count.setText(count + " Likes");
        }

        public void update_comments_count(int count) {
            blog_like_count = m_view.findViewById(R.id.blogCommentCount);
            blog_like_count.setText(count + " Comments");
        }
    }
}
