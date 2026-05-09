package com.example.cardclash.core.settings;

import java.util.ArrayList;
import java.util.List;

import com.example.cardclash.core.models.RoomConfig;

/** Ordered list of {@link Setting}s a game declares. Drives the PreGameSettings UI. */
public class RuleSchema {
    public final List<Setting> settings;

    private RuleSchema(List<Setting> settings) {
        this.settings = settings;
    }

    public static Builder builder() { return new Builder(); }

    /** Apply all defaults onto a RoomConfig in-place. */
    public void applyDefaults(RoomConfig cfg) {
        for (Setting s : settings) {
            if (!cfg.values.containsKey(s.key)) cfg.put(s.key, s.defaultValue);
        }
    }

    public static class Builder {
        private final List<Setting> settings = new ArrayList<>();
        public Builder add(Setting s) { settings.add(s); return this; }
        public Builder add(Setting.Builder b) { settings.add(b.build()); return this; }
        public RuleSchema build() { return new RuleSchema(settings); }
    }
}
