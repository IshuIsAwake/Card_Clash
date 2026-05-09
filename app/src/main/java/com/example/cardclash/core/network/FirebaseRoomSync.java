package com.example.cardclash.core.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.cardclash.core.models.Player;
import com.example.cardclash.core.models.RebuyRequest;
import com.example.cardclash.core.models.RoomConfig;
import com.example.cardclash.core.models.RoomState;
import com.example.cardclash.core.models.RoomStatus;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Thin wrapper over Firebase Realtime Database for room-scoped operations.
 *
 * <p>Schema (see docs/firebase-schema.md):
 * <pre>
 * /rooms/{roomId}
 *   /meta              { hostUid, gameType, status, createdAt, config: {...} }
 *   /players/{uid}     Player
 *   /gameState         engine snapshot map
 *   /rebuyRequests/{r} RebuyRequest
 *   /events/{eventId}  append-only action log (size-bounded)
 * </pre>
 *
 * Reads are split per node so high-frequency game-state writes don't trigger
 * roster listeners and vice versa.
 */
public class FirebaseRoomSync {

    public interface RoomCallback {
        void onSuccess(RoomState state);
        void onError(String reason);
    }

    public interface MetaListener {
        void onMeta(@NonNull RoomState meta);
        default void onError(String reason) {}
    }

    private final DatabaseReference root;

    public FirebaseRoomSync() {
        this.root = FirebaseDatabase.getInstance().getReference("rooms");
    }

    public DatabaseReference room(String roomId) { return root.child(roomId); }

    // -- create / join ----------------------------------------------------

    public void createRoom(String roomId, String hostUid, RoomConfig config,
                           Player hostPlayer, RoomCallback cb) {
        DatabaseReference r = root.child(roomId);
        Map<String, Object> meta = new HashMap<>();
        meta.put("hostUid", hostUid);
        meta.put("gameType", config.gameType);
        meta.put("status", RoomStatus.WAITING.name());
        meta.put("createdAt", System.currentTimeMillis());
        meta.put("config", config);

        Map<String, Object> tx = new HashMap<>();
        tx.put("meta", meta);
        tx.put("players/" + hostUid, hostPlayer);

        r.updateChildren(tx).addOnCompleteListener(t -> {
            if (t.isSuccessful()) {
                RoomState st = new RoomState();
                st.roomId = roomId;
                st.hostUid = hostUid;
                st.gameType = config.gameType;
                st.config = config;
                st.status = RoomStatus.WAITING.name();
                st.createdAt = (long) meta.get("createdAt");
                st.players.put(hostUid, hostPlayer);
                cb.onSuccess(st);
            } else {
                cb.onError(t.getException() != null ? t.getException().getMessage() : "Create failed");
            }
        });
    }

    public void joinRoom(String roomId, Player player, RoomCallback cb) {
        DatabaseReference r = root.child(roomId);
        r.child("meta").get().addOnCompleteListener(t -> {
            if (!t.isSuccessful() || !t.getResult().exists()) {
                cb.onError("Room not found"); return;
            }
            RoomState st = readMetaSnapshot(roomId, t.getResult());
            if (RoomStatus.IN_PROGRESS.name().equals(st.status)) {
                cb.onError("Game already in progress"); return;
            }
            r.child("players").child(player.uid).setValue(player)
                    .addOnCompleteListener(t2 -> {
                        if (t2.isSuccessful()) cb.onSuccess(st);
                        else cb.onError("Join failed");
                    });
        });
    }

    public void leaveRoom(String roomId, String uid) {
        root.child(roomId).child("players").child(uid).removeValue();
    }

    public void setStatus(String roomId, RoomStatus status) {
        root.child(roomId).child("meta").child("status").setValue(status.name());
    }

    // -- listeners --------------------------------------------------------

    public ValueEventListener observeMeta(String roomId, MetaListener listener) {
        DatabaseReference r = root.child(roomId);
        ValueEventListener vel = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) { listener.onError("Room gone"); return; }
                RoomState st = new RoomState();
                st.roomId = roomId;
                DataSnapshot meta = snapshot.child("meta");
                st.hostUid = meta.child("hostUid").getValue(String.class);
                st.gameType = meta.child("gameType").getValue(String.class);
                st.status = meta.child("status").getValue(String.class);
                Long ts = meta.child("createdAt").getValue(Long.class);
                st.createdAt = ts == null ? 0 : ts;
                st.config = meta.child("config").getValue(RoomConfig.class);
                for (DataSnapshot p : snapshot.child("players").getChildren()) {
                    Player pl = p.getValue(Player.class);
                    if (pl != null) st.players.put(p.getKey(), pl);
                }
                listener.onMeta(st);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.getMessage());
            }
        };
        r.addValueEventListener(vel);
        return vel;
    }

    public void detach(String roomId, ValueEventListener vel) {
        if (vel != null) root.child(roomId).removeEventListener(vel);
    }

    // -- rebuy / chip injection ------------------------------------------

    public void requestRebuy(String roomId, RebuyRequest req) {
        root.child(roomId).child("rebuyRequests").child(req.requestId).setValue(req);
    }

    public void resolveRebuy(String roomId, String requestId, boolean approve, long deltaChips,
                             String playerUid) {
        DatabaseReference req = root.child(roomId).child("rebuyRequests").child(requestId);
        req.child("status").setValue(approve ? "APPROVED" : "DENIED");
        if (approve) addChips(roomId, playerUid, deltaChips);
    }

    /** Atomically add (or subtract) chips to a player's stack. */
    public void addChips(String roomId, String playerUid, long delta) {
        DatabaseReference c = root.child(roomId).child("players").child(playerUid).child("chips");
        c.runTransaction(new Transaction.Handler() {
            @NonNull @Override public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Long cur = currentData.getValue(Long.class);
                long base = cur == null ? 0 : cur;
                currentData.setValue(Math.max(0, base + delta));
                return Transaction.success(currentData);
            }
            @Override public void onComplete(@Nullable DatabaseError error, boolean committed,
                                             @Nullable DataSnapshot snap) {}
        });
    }

    // -- helpers ---------------------------------------------------------

    private static RoomState readMetaSnapshot(String roomId, DataSnapshot snap) {
        RoomState st = new RoomState();
        st.roomId = roomId;
        st.hostUid = snap.child("hostUid").getValue(String.class);
        st.gameType = snap.child("gameType").getValue(String.class);
        st.status = snap.child("status").getValue(String.class);
        Long ts = snap.child("createdAt").getValue(Long.class);
        st.createdAt = ts == null ? 0 : ts;
        st.config = snap.child("config").getValue(RoomConfig.class);
        return st;
    }
}
