package com.example.cardclash.games.poker.engine;

import com.example.cardclash.core.engine.GameEngine;
import com.example.cardclash.core.models.Action;
import com.example.cardclash.core.models.ActionResult;
import com.example.cardclash.core.models.Player;
import com.example.cardclash.core.models.RoomConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Stub Poker engine. HandEvaluator + SidePotCalculator are tested separately;
 *  full Hold'em / 5-Card-Draw integration lands in Phase 4. */
public class PokerEngine implements GameEngine {

    private final List<Listener> listeners = new ArrayList<>();
    private RoomConfig config;
    private List<Player> players = new ArrayList<>();
    private String phase = "PRE_FLOP";

    @Override public void initialize(RoomConfig config, List<Player> players, long seed) {
        this.config = config;
        this.players = new ArrayList<>(players);
        notifyChanged();
    }

    @Override public ActionResult submit(Action action) { return ActionResult.fail("Poker engine not implemented yet"); }
    @Override public Map<String, Object> snapshot() { return new LinkedHashMap<>(); }
    @Override public void restore(Map<String, Object> snapshot) {}
    @Override public void addListener(Listener l) { listeners.add(l); }
    @Override public void removeListener(Listener l) { listeners.remove(l); }
    @Override public String currentPhase() { return phase; }
    @Override public String currentTurnUid() { return players.isEmpty() ? null : players.get(0).uid; }
    @Override public boolean isRoundOver() { return false; }
    @Override public boolean isGameOver() { return false; }

    private void notifyChanged() { for (Listener l : listeners) l.onStateChanged(); }
}
