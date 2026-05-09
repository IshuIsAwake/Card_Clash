package com.example.cardclash.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.cardclash.R;
import com.example.cardclash.core.theme.Theme;
import com.example.cardclash.core.theme.ThemeId;
import com.example.cardclash.core.theme.ThemePrefs;
import com.example.cardclash.core.theme.ThemeRegistry;
import com.example.cardclash.ui.auth.LoginActivity;
import com.example.cardclash.ui.common.ThemedActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;

public class ProfileActivity extends ThemedActivity {

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        TextInputEditText etDisplayName = findViewById(R.id.etDisplayName);
        Button btnSaveName = findViewById(R.id.btnSaveName);
        RadioGroup themeGroup = findViewById(R.id.themeGroup);
        Button btnSignOut = findViewById(R.id.btnSignOut);

        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u != null) etDisplayName.setText(u.getDisplayName());

        // Theme radio buttons
        ThemeId active = ThemePrefs.active(this);
        for (Theme t : ThemeRegistry.all()) {
            RadioButton rb = new RadioButton(this);
            rb.setText(t.displayName());
            rb.setChecked(t.id() == active);
            rb.setTag(t.id());
            rb.setPadding(0, 16, 0, 16);
            themeGroup.addView(rb);
        }
        themeGroup.setOnCheckedChangeListener((g, checkedId) -> {
            RadioButton rb = findViewById(checkedId);
            if (rb == null) return;
            ThemePrefs.setActive(this, (ThemeId) rb.getTag());
            recreate();
        });

        btnSaveName.setOnClickListener(v -> {
            String n = String.valueOf(etDisplayName.getText()).trim();
            if (n.isEmpty() || u == null) return;
            u.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(n).build());
            FirebaseDatabase.getInstance().getReference("users")
                    .child(u.getUid()).child("displayName").setValue(n);
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        });

        btnSignOut.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        });
    }
}
