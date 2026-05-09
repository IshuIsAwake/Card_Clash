package com.example.cardclash.core.engine;

import java.util.List;

import com.example.cardclash.core.models.Action;
import com.example.cardclash.core.models.ActionResult;
import com.example.cardclash.core.models.Player;
import com.example.cardclash.core.models.RoomConfig;

/**
 * Headless game engine. Holds authoritative state for one room. UI never touches
 * fields directly; it submits {@link Action}s and observes via {@link Listener}.
 *
 * <p>Engines must be deterministic given the same seed and action sequence so that
 * Firebase replay reconstructs identical state.
 */
public interface GameEngine {

    /** Initialize from a room config and player roster. Resets all state. */
    void initialize(RoomConfig config, List<Player> players, long seed);

    /** Submit an action. Returns success / specific failure reason. */
    ActionResult submit(Action action);

    /** Snapshot current state as a JSON-friendly map for Firebase sync. */
    java.util.Map<String, Object> snapshot();

    /** Restore state from a snapshot (for late-joiners / reconnects). */
    void restore(java.util.Map<String, Object> snapshot);

    /** Subscribe to state changes. */
    void addListener(Listener l);
    void removeListener(Listener l);

    /** Game-defined "phase" string for the UI to render against (e.g. BETTING, SHOWDOWN). */
    String currentPhase();

    /** UID of the player whose turn it is, or null if not applicable. */
    String currentTurnUid();

    boolean isRoundOver();
    boolean isGameOver();

    interface Listener {
        void onStateChanged();
        default void onEvent(String kind, java.util.Map<String, Object> payload) {}
    }
}
