package com.example.cardclash.ui.room;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.cardclash.R;
import com.example.cardclash.core.models.GameType;
import com.example.cardclash.core.models.Player;
import com.example.cardclash.core.models.RoomConfig;
import com.example.cardclash.core.network.FirebaseRoomSync;
import com.example.cardclash.core.network.RoomCodeGenerator;
import com.example.cardclash.games.GameDefinition;
import com.example.cardclash.games.GamesRegistry;
import com.example.cardclash.ui.common.ThemedActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class PreGameSettingsActivity extends ThemedActivity {

    public static final String EXTRA_GAME_TYPE = "game_type";

    private GameType type;
    private RoomConfig config;
    private final FirebaseRoomSync sync = new FirebaseRoomSync();

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pregame_settings);

        type = GameType.valueOf(getIntent().getStringExtra(EXTRA_GAME_TYPE));
        GameDefinition def = GamesRegistry.get(type);

        TextView title = findViewById(R.id.title);
        title.setText(type.displayName + " · Rules");

        config = new RoomConfig(type);
        def.ruleSchema.applyDefaults(config);
        SchemaRenderer.renderInto(findViewById(R.id.settingsContainer), def.ruleSchema, config);

        Button btnContinue = findViewById(R.id.btnContinue);
        btnContinue.setOnClickListener(v -> createRoom());
    }

    private void createRoom() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) { Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show(); return; }
        String roomId = RoomCodeGenerator.next();
        long buyIn = config.longVal("buy_in", 1000);
        Player host = new Player(u.getUid(),
                u.getDisplayName() == null ? "Host" : u.getDisplayName(),
                0, buyIn, true);

        sync.createRoom(roomId, u.getUid(), config, host, new FirebaseRoomSync.RoomCallback() {
            @Override public void onSuccess(com.example.cardclash.core.models.RoomState state) {
                Intent i = new Intent(PreGameSettingsActivity.this, LobbyActivity.class);
                i.putExtra(LobbyActivity.EXTRA_ROOM_ID, roomId);
                i.putExtra(LobbyActivity.EXTRA_IS_HOST, true);
                startActivity(i);
                finish();
            }
            @Override public void onError(String reason) {
                Toast.makeText(PreGameSettingsActivity.this,
                        "Couldn't create room: " + reason, Toast.LENGTH_LONG).show();
            }
        });
    }
}
