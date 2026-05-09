package com.example.cardclash.ui.room;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;

import com.example.cardclash.R;
import com.example.cardclash.core.models.Player;
import com.example.cardclash.core.network.FirebaseRoomSync;
import com.example.cardclash.ui.common.ThemedActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class JoinRoomActivity extends ThemedActivity {

    private final FirebaseRoomSync sync = new FirebaseRoomSync();
    private final ActivityResultLauncher<ScanOptions> scanLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result == null || result.getContents() == null) return;
                attemptJoin(result.getContents().trim());
            });

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_room);

        TextInputEditText et = findViewById(R.id.etRoomCode);
        Button btnJoin = findViewById(R.id.btnJoin);
        Button btnScan = findViewById(R.id.btnScanQR);

        btnJoin.setOnClickListener(v -> {
            String code = String.valueOf(et.getText()).trim();
            if (code.length() != 6) {
                Toast.makeText(this, "Enter the 6-digit room code", Toast.LENGTH_SHORT).show();
                return;
            }
            attemptJoin(code);
        });

        btnScan.setOnClickListener(v -> {
            ScanOptions opts = new ScanOptions();
            opts.setPrompt("Point at the room's QR code");
            opts.setBeepEnabled(false);
            opts.setOrientationLocked(false);
            scanLauncher.launch(opts);
        });
    }

    private void attemptJoin(String roomId) {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) { Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show(); return; }
        // Seat assigned later by host UI; for v1 assume sequential by join order.
        Player p = new Player(u.getUid(),
                u.getDisplayName() == null ? "Player" : u.getDisplayName(),
                -1, 1000, false);
        sync.joinRoom(roomId, p, new FirebaseRoomSync.RoomCallback() {
            @Override public void onSuccess(com.example.cardclash.core.models.RoomState state) {
                Intent i = new Intent(JoinRoomActivity.this, LobbyActivity.class);
                i.putExtra(LobbyActivity.EXTRA_ROOM_ID, roomId);
                i.putExtra(LobbyActivity.EXTRA_IS_HOST, false);
                startActivity(i);
                finish();
            }
            @Override public void onError(String reason) {
                Toast.makeText(JoinRoomActivity.this, reason, Toast.LENGTH_LONG).show();
            }
        });
    }
}
