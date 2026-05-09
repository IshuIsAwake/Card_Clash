package com.example.cardclash.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.cardclash.R;
import com.example.cardclash.ui.common.ThemedActivity;
import com.example.cardclash.ui.home.HomeActivity;
import com.example.cardclash.ui.hotseat.HotSeatSetupActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends ThemedActivity {

    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(v -> doLogin());
        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
        findViewById(R.id.btnHotSeat).setOnClickListener(v ->
                startActivity(new Intent(this, HotSeatSetupActivity.class)));
    }

    private void doLogin() {
        String email = String.valueOf(etEmail.getText()).trim();
        String pass = String.valueOf(etPassword.getText());
        if (email.isEmpty() || pass.length() < 6) {
            Toast.makeText(this, "Enter a valid email and password", Toast.LENGTH_SHORT).show();
            return;
        }
        btnLogin.setEnabled(false);
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(t -> {
                    btnLogin.setEnabled(true);
                    if (t.isSuccessful()) {
                        startActivity(new Intent(this, HomeActivity.class));
                        finish();
                    } else {
                        String msg = t.getException() != null ? t.getException().getMessage() : "Login failed";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
