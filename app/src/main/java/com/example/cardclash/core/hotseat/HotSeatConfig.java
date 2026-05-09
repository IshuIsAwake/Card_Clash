package com.example.cardclash.core.hotseat;

import com.example.cardclash.core.models.GameType;
import com.example.cardclash.core.models.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Single-device pass-and-play config. Bypasses Firebase entirely. Lives as a
 * process-scoped singleton because Intent-marshalling a list of POJOs is more
 * ceremony than this one in-memory hop deserves.
 */
public final class HotSeatConfig {

    private static HotSeatConfig active;

    public final GameType gameType;
    public final List<Player> players;
    /** Game-specific rule overrides (RuleSchema keys → values). */
    public final Map<String, Object> ruleOverrides;

    public HotSeatConfig(GameType gameType, List<Player> players, Map<String, Object> ruleOverrides) {
        this.gameType = gameType;
        this.players = new ArrayList<>(players);
        this.ruleOverrides = new HashMap<>(ruleOverrides);
    }

    public static void set(HotSeatConfig cfg) { active = cfg; }
    public static HotSeatConfig get() { return active; }
    public static void clear() { active = null; }
    public static boolean isActive() { return active != null; }
}
