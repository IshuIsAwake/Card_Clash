package com.example.cardclash.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.cardclash.R;
import com.example.cardclash.ui.common.ThemedActivity;
import com.example.cardclash.ui.hotseat.HotSeatSetupActivity;
import com.example.cardclash.ui.room.CreateRoomActivity;
import com.example.cardclash.ui.room.JoinRoomActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends ThemedActivity {

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        TextView tvWelcome = findViewById(R.id.tvWelcome);
        if (u != null && u.getDisplayName() != null && !u.getDisplayName().isEmpty()) {
            tvWelcome.setText("Welcome, " + u.getDisplayName());
        }

        findViewById(R.id.btnCreateRoom).setOnClickListener(v ->
                startActivity(new Intent(this, CreateRoomActivity.class)));
        findViewById(R.id.btnJoinRoom).setOnClickListener(v ->
                startActivity(new Intent(this, JoinRoomActivity.class)));
        findViewById(R.id.btnProfile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
        findViewById(R.id.btnHotSeat).setOnClickListener(v ->
                startActivity(new Intent(this, HotSeatSetupActivity.class)));
    }
}
