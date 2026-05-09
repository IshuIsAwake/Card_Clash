package com.example.cardclash.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.example.cardclash.R;
import com.example.cardclash.ui.common.ThemedActivity;
import com.example.cardclash.ui.home.HomeActivity;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends ThemedActivity {
    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        getWindow().getDecorView().postDelayed(this::route, 700);
    }

    private void route() {
        Intent next = FirebaseAuth.getInstance().getCurrentUser() != null
                ? new Intent(this, HomeActivity.class)
                : new Intent(this, LoginActivity.class);
        startActivity(next);
        finish();
    }
}
