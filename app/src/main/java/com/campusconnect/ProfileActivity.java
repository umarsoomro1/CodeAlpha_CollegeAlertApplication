package com.campusconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class ProfileActivity extends AppCompatActivity {
    private TextView emailTextView;
    private SwitchMaterial notificationsSwitch;
    private MaterialCheckBox seminarCheck, examCheck, festCheck, noticeCheck;
    private Button logoutButton;
    private SharedPreferences sharedPreferences;
    private FirebaseHelper firebaseHelper;
    private static final String PREFS_NAME = "CampusConnectPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        emailTextView = findViewById(R.id.email);
        notificationsSwitch = findViewById(R.id.notification_toggle);
        seminarCheck = findViewById(R.id.seminar_check);
        examCheck = findViewById(R.id.exam_check);
        festCheck = findViewById(R.id.fest_check);
        noticeCheck = findViewById(R.id.notice_check);
        logoutButton = findViewById(R.id.logout_button);

        firebaseHelper = new FirebaseHelper();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        if (firebaseHelper.getCurrentUser() != null) {
            emailTextView.setText(firebaseHelper.getCurrentUser().getEmail());
        } else {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }

        notificationsSwitch.setChecked(sharedPreferences.getBoolean("notifications_enabled", true));
        seminarCheck.setChecked(sharedPreferences.getBoolean("category_seminar", true));
        examCheck.setChecked(sharedPreferences.getBoolean("category_exam", true));
        festCheck.setChecked(sharedPreferences.getBoolean("category_fest", true));
        noticeCheck.setChecked(sharedPreferences.getBoolean("category_notice", true));

        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("notifications_enabled", isChecked);
            editor.apply();
        });

        seminarCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("category_seminar", isChecked);
            editor.apply();
        });

        examCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("category_exam", isChecked);
            editor.apply();
        });

        festCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("category_fest", isChecked);
            editor.apply();
        });

        noticeCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("category_notice", isChecked);
            editor.apply();
        });

        logoutButton.setOnClickListener(v -> {
            firebaseHelper.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}