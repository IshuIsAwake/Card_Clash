package com.example.cardclash.ui.room;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cardclash.R;
import com.example.cardclash.core.models.GameType;
import com.example.cardclash.core.models.Player;
import com.example.cardclash.core.models.RoomState;
import com.example.cardclash.core.models.RoomStatus;
import com.example.cardclash.core.network.FirebaseRoomSync;
import com.example.cardclash.games.GameDefinition;
import com.example.cardclash.games.GamesRegistry;
import com.example.cardclash.ui.common.ThemedActivity;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.ArrayList;
import java.util.List;

public class LobbyActivity extends ThemedActivity {

    public static final String EXTRA_ROOM_ID = "room_id";
    public static final String EXTRA_IS_HOST = "is_host";

    private final FirebaseRoomSync sync = new FirebaseRoomSync();
    private String roomId;
    private boolean isHost;
    private ValueEventListener vel;
    private RoomState lastState;

    private TextView roomCodeTv, gameLabelTv;
    private RecyclerView playerList;
    private Button btnStart;
    private final PlayerAdapter adapter = new PlayerAdapter();

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        roomId = getIntent().getStringExtra(EXTRA_ROOM_ID);
        isHost = getIntent().getBooleanExtra(EXTRA_IS_HOST, false);

        roomCodeTv = findViewById(R.id.roomCode);
        gameLabelTv = findViewById(R.id.gameLabel);
        playerList = findViewById(R.id.playerList);
        btnStart = findViewById(R.id.btnStart);

        roomCodeTv.setText(formatCode(roomId));
        playerList.setLayoutManager(new LinearLayoutManager(this));
        playerList.setAdapter(adapter);

        // Render QR
        try {
            BarcodeEncoder enc = new BarcodeEncoder();
            Bitmap bmp = enc.encodeBitmap(roomId, BarcodeFormat.QR_CODE, 480, 480);
            ImageView qr = findViewById(R.id.qrImage);
            qr.setImageBitmap(bmp);
        } catch (Exception ignored) {}

        btnStart.setVisibility(isHost ? View.VISIBLE : View.GONE);
        btnStart.setEnabled(false);
        btnStart.setOnClickListener(v -> startGame());

        findViewById(R.id.btnRules).setOnClickListener(v -> {
            if (lastState == null) return;
            // Read-only summary of locked rules.
            android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this);
            b.setTitle("Rules");
            StringBuilder sb = new StringBuilder();
            if (lastState.config != null) {
                for (var e : lastState.config.values.entrySet())
                    sb.append(e.getKey()).append(": ").append(e.getValue()).append("\n");
            }
            b.setMessage(sb.toString()).setPositiveButton("OK", null).show();
        });
    }

    @Override protected void onStart() {
        super.onStart();
        vel = sync.observeMeta(roomId, new FirebaseRoomSync.MetaListener() {
            @Override public void onMeta(@NonNull RoomState meta) {
                lastState = meta;
                runOnUiThread(() -> render(meta));
            }
            @Override public void onError(String reason) {
                runOnUiThread(() -> Toast.makeText(LobbyActivity.this,
                        "Lobby error: " + reason, Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override protected void onStop() {
        super.onStop();
        sync.detach(roomId, vel);
    }

    private void render(RoomState st) {
        if (st.gameType != null) {
            try {
                GameType gt = GameType.valueOf(st.gameType);
                gameLabelTv.setText(gt.displayName);
            } catch (Exception ignored) {}
        }
        adapter.submit(new ArrayList<>(st.players.values()), st.hostUid);

        boolean canStart = isHost && st.players.size() >= 2 // demo-friendly min
                && RoomStatus.WAITING.name().equals(st.status);
        btnStart.setEnabled(canStart);

        if (RoomStatus.IN_PROGRESS.name().equals(st.status)) {
            // launch table
            try {
                GameType gt = GameType.valueOf(st.gameType);
                GameDefinition def = GamesRegistry.get(gt);
                Intent i = new Intent(this, def.tableActivity);
                i.putExtra("room_id", roomId);
                startActivity(i);
                finish();
            } catch (Exception ignored) {}
        }
    }

    private void startGame() {
        sync.setStatus(roomId, RoomStatus.IN_PROGRESS);
    }

    private static String formatCode(String c) {
        if (c == null || c.length() != 6) return c;
        return c.substring(0, 3) + " " + c.substring(3);
    }

    // -- adapter --

    private static class PlayerAdapter extends RecyclerView.Adapter<PlayerAdapter.VH> {
        private final List<Player> data = new ArrayList<>();
        private String hostUid;

        void submit(List<Player> ps, String host) {
            data.clear();
            data.addAll(ps);
            this.hostUid = host;
            notifyDataSetChanged();
        }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_player, parent, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int position) {
            Player p = data.get(position);
            h.name.setText(p.displayName);
            h.initials.setText(initials(p.displayName));
            h.host.setVisibility(p.uid != null && p.uid.equals(hostUid) ? View.VISIBLE : View.GONE);
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            final TextView initials, name, host;
            VH(@NonNull View v) {
                super(v);
                initials = v.findViewById(R.id.initials);
                name = v.findViewById(R.id.name);
                host = v.findViewById(R.id.hostBadge);
            }
        }

        private static String initials(String s) {
            if (s == null || s.isEmpty()) return "?";
            String[] parts = s.trim().split("\\s+");
            return parts.length == 1 ? parts[0].substring(0, 1).toUpperCase()
                    : ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        }
    }
}
