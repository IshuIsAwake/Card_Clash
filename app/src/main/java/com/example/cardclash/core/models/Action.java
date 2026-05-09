package com.example.cardclash.core.models;

import java.util.HashMap;
import java.util.Map;

/** Generic action envelope. Game engines interpret `kind` and `payload`. */
public class Action {
    public String actorUid;
    public String kind;                       // engine-defined
    public Map<String, Object> payload = new HashMap<>();
    public long timestamp = System.currentTimeMillis();

    public Action() {}

    public Action(String actorUid, String kind) {
        this.actorUid = actorUid;
        this.kind = kind;
    }

    public Action with(String key, Object val) { payload.put(key, val); return this; }
}
