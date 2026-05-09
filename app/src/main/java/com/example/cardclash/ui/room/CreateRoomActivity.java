package com.example.cardclash.ui.room;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.example.cardclash.R;
import com.example.cardclash.core.models.GameType;
import com.example.cardclash.ui.common.ThemedActivity;

public class CreateRoomActivity extends ThemedActivity {

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_room);

        findViewById(R.id.btnTeenPatti).setOnClickListener(v -> next(GameType.TEEN_PATTI));
        findViewById(R.id.btnBluff).setOnClickListener(v -> next(GameType.BLUFF));
        findViewById(R.id.btnPoker).setOnClickListener(v -> next(GameType.POKER));
    }

    private void next(GameType type) {
        Intent i = new Intent(this, PreGameSettingsActivity.class);
        i.putExtra(PreGameSettingsActivity.EXTRA_GAME_TYPE, type.name());
        startActivity(i);
    }
}
