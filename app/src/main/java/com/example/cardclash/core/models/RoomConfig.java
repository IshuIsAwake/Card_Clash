package com.example.cardclash.core.models;

import java.util.HashMap;
import java.util.Map;

/** Snapshot of host-configured rules for a room. Locks at game start. */
public class RoomConfig {
    public String gameType;            // GameType.name()
    public Map<String, Object> values; // RuleSchema setting key -> value

    public RoomConfig() { this.values = new HashMap<>(); }

    public RoomConfig(GameType type) {
        this.gameType = type.name();
        this.values = new HashMap<>();
    }

    public GameType type() { return GameType.valueOf(gameType); }

    public long longVal(String key, long def) {
        Object v = values.get(key);
        if (v instanceof Number) return ((Number) v).longValue();
        return def;
    }

    public int intVal(String key, int def) {
        Object v = values.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        return def;
    }

    public boolean boolVal(String key, boolean def) {
        Object v = values.get(key);
        return v instanceof Boolean ? (Boolean) v : def;
    }

    public String strVal(String key, String def) {
        Object v = values.get(key);
        return v instanceof String ? (String) v : def;
    }

    public void put(String key, Object value) { values.put(key, value); }
}
