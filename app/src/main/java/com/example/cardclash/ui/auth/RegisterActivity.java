package com.example.cardclash.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.cardclash.R;
import com.example.cardclash.ui.common.ThemedActivity;
import com.example.cardclash.ui.home.HomeActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends ThemedActivity {

    private TextInputEditText etDisplayName, etEmail, etPassword;
    private Button btnRegister;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etDisplayName = findViewById(R.id.etDisplayName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        TextView tvLogin = findViewById(R.id.tvLogin);

        btnRegister.setOnClickListener(v -> doRegister());
        tvLogin.setOnClickListener(v -> finish());
    }

    private void doRegister() {
        String name = String.valueOf(etDisplayName.getText()).trim();
        String email = String.valueOf(etEmail.getText()).trim();
        String pass = String.valueOf(etPassword.getText());
        if (name.isEmpty()) { Toast.makeText(this, "Display name is required", Toast.LENGTH_SHORT).show(); return; }
        if (email.isEmpty()) { Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show(); return; }
        if (pass.length() < 6) { Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show(); return; }

        btnRegister.setEnabled(false);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(t -> {
                    if (!t.isSuccessful()) {
                        btnRegister.setEnabled(true);
                        String msg = t.getException() != null ? t.getException().getMessage() : "Registration failed";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        return;
                    }
                    UserProfileChangeRequest req = new UserProfileChangeRequest.Builder()
                            .setDisplayName(name).build();
                    auth.getCurrentUser().updateProfile(req);
                    FirebaseDatabase.getInstance().getReference("users")
                            .child(auth.getCurrentUser().getUid())
                            .child("displayName").setValue(name);
                    startActivity(new Intent(this, HomeActivity.class));
                    finishAffinity();
                });
    }
}
