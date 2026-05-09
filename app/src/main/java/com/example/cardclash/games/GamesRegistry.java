package com.example.cardclash.games;

import com.example.cardclash.core.models.GameType;
import com.example.cardclash.games.bluff.engine.BluffSchema;
import com.example.cardclash.games.poker.engine.PokerSchema;
import com.example.cardclash.games.teenpatti.engine.TeenPattiSchema;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Registry of every game in the app. Adding a game = register here. */
public final class GamesRegistry {

    private static final Map<GameType, GameDefinition> GAMES = new LinkedHashMap<>();

    static {
        register(TeenPattiSchema.definition());
        register(BluffSchema.definition());
        register(PokerSchema.definition());
    }

    private GamesRegistry() {}

    public static void register(GameDefinition def) { GAMES.put(def.type, def); }
    public static GameDefinition get(GameType t) { return GAMES.get(t); }
    public static Collection<GameDefinition> all() { return GAMES.values(); }
}
