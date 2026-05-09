package com.example.cardclash.core.models;

import java.util.HashMap;
import java.util.Map;

/** Top-level Firebase RTDB shape per room. */
public class RoomState {
    public String roomId;             // 6-digit code
    public String hostUid;
    public String gameType;           // GameType.name()
    public RoomConfig config;
    public String status;             // RoomStatus.name()
    public long createdAt;
    public Map<String, Player> players = new HashMap<>();
    public Map<String, RebuyRequest> rebuyRequests = new HashMap<>();
    /** Opaque per-game state blob, written by the engine. */
    public Map<String, Object> gameState = new HashMap<>();

    public RoomState() {}

    public RoomStatus statusEnum() {
        return status == null ? RoomStatus.WAITING : RoomStatus.valueOf(status);
    }
}
