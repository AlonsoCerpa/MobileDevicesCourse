package com.example.gamerz;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private Toolbar mainToolbar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firebase_firestore;
    private FloatingActionButton add_post_button;
    private String current_user_id;
    private BottomNavigationView main_bottom_nav;
    private HomeFragment home_fragment;
    private NotificationFragment notification_fragment;
    private AccountFragment account_fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        firebase_firestore = FirebaseFirestore.getInstance();

        mainToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(mainToolbar);

        getSupportActionBar().setTitle("GamerZ Forum");

        if (mAuth.getCurrentUser() != null) {

            main_bottom_nav = (BottomNavigationView) findViewById(R.id.mainBottomNav);

            home_fragment = new HomeFragment();
            notification_fragment = new NotificationFragment();
            account_fragment = new AccountFragment();

            replace_fragment(home_fragment);

            main_bottom_nav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.bottom_action_home:
                            replace_fragment(home_fragment);
                            return true;
                        case R.id.bottom_action_account:
                            replace_fragment(account_fragment);
                            return true;
                        case R.id.bottom_action_notification:
                            replace_fragment(notification_fragment);
                            return true;
                        default:
                            return false;
                    }
                }
            });

            add_post_button = (FloatingActionButton) findViewById(R.id.addPostButton);
            add_post_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent new_post_intent = new Intent(MainActivity.this, NewPostActivity.class);
                    startActivity(new_post_intent);
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            current_user_id = mAuth.getCurrentUser().getUid();
            firebase_firestore.collection("Users").document(current_user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        if (!task.getResult().exists()) {
                            Intent setup_intent = new Intent(MainActivity.this, SetupActivity.class);
                            startActivity(setup_intent);
                            finish();
                        }
                    } else {
                        String error_message = task.getException().getMessage();
                        Toast.makeText(MainActivity.this, "Error : " + error_message, Toast.LENGTH_LONG).show();
                    }
                }
            });
        } else {
            sendToLogin();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {

            case R.id.action_logout_button:
                logOut();
                return true;
            case R.id.action_settings_button:
                Intent settings_intent = new Intent(MainActivity.this, SetupActivity.class);
                startActivity(settings_intent);
                return true;
            default:
                return false;
        }
    }

    private void logOut() {

        mAuth.signOut();
        sendToLogin();

    }

    private void sendToLogin() {
        Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(loginIntent);
        finish();
    }

    private void replace_fragment(Fragment fragment) {
        FragmentTransaction fragment_transaction = getSupportFragmentManager().beginTransaction();
        fragment_transaction.replace(R.id.main_container, fragment);
        fragment_transaction.commit();
    }
}