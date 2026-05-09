package com.example.cardclash.games.teenpatti.engine;

import com.example.cardclash.core.engine.GameEngine;
import com.example.cardclash.core.engine.HandResult;
import com.example.cardclash.core.models.Action;
import com.example.cardclash.core.models.ActionResult;
import com.example.cardclash.core.models.Card;
import com.example.cardclash.core.models.Player;
import com.example.cardclash.core.models.RoomConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Teen Patti engine — Classic ruleset wired end-to-end. Variation hooks for
 * AK47/1942/Muflis/JokerWild route through {@link TeenPattiVariation}.
 *
 * <p>v1 first-iteration scope: ante → blind/seen → betting (chaal/raise/fold) →
 *    showdown. Side-show is recognized but resolves automatically as a
 *    high-vs-high comparison between requester and target.
 */
public class TeenPattiEngine implements GameEngine {

    public static final String ACTION_TOGGLE_SEEN = "toggle_seen";
    public static final String ACTION_CHAAL       = "chaal";
    public static final String ACTION_RAISE       = "raise";
    public static final String ACTION_FOLD        = "fold";
    public static final String ACTION_SHOW        = "show";          // 2 players left
    public static final String ACTION_SIDE_SHOW   = "side_show";

    public static final String PHASE_DEAL     = "DEAL";
    public static final String PHASE_BETTING  = "BETTING";
    public static final String PHASE_SHOWDOWN = "SHOWDOWN";
    public static final String PHASE_ROUND_OVER = "ROUND_OVER";

    // -- state ---------------------------------------------------------------
    private final List<Listener> listeners = new ArrayList<>();
    private RoomConfig config;
    private List<String> seatOrder = new ArrayList<>(); // uids in seat order
    private Map<String, Player> players = new LinkedHashMap<>();
    private Map<String, List<Card>> hands = new HashMap<>();
    private Set<String> seen = new HashSet<>();
    private Set<String> folded = new HashSet<>();
    private long pot;
    private long currentStake;            // unit stake: blind player pays this, seen pays 2x
    private int turnIndex;                // index into seatOrder
    private int dealerIndex;
    private int roundsPlayed;
    private long seed;
    private List<TeenPattiVariation> queue = new ArrayList<>();
    private int variationCadence = 2;
    private TeenPattiVariation currentVariation;
    private String winnerUid;
    private String phase = PHASE_DEAL;

    @Override
    public void initialize(RoomConfig config, List<Player> players, long seed) {
        this.config = config;
        this.seed = seed;
        this.players.clear();
        this.seatOrder.clear();
        for (Player p : players) {
            this.players.put(p.uid, p);
            this.seatOrder.add(p.uid);
        }
        this.dealerIndex = 0;
        this.roundsPlayed = 0;

        // variation queue from config (list of ids). If unset, default queue.
        Object rawQueue = config.values.get("variation_queue");
        if (rawQueue instanceof List) {
            queue.clear();
            for (Object id : (List<?>) rawQueue) {
                TeenPattiVariation v = lookupVariation(String.valueOf(id));
                if (v != null) queue.add(v);
            }
        }
        if (queue.isEmpty()) queue.addAll(TeenPattiVariation.defaultQueue());
        variationCadence = config.intVal("variation_cadence", 2);

        startRound();
    }

    private static TeenPattiVariation lookupVariation(String id) {
        for (TeenPattiVariation v : TeenPattiVariation.defaultQueue())
            if (v.id().equals(id)) return v;
        return null;
    }

    private void startRound() {
        folded.clear();
        seen.clear();
        hands.clear();
        winnerUid = null;
        pot = 0;

        // pick variation by rotation
        int idx = roundsPlayed / Math.max(1, variationCadence);
        currentVariation = queue.get(idx % queue.size());

        // collect ante/boot
        long ante = config.longVal("boot_amount", 10);
        for (String uid : seatOrder) {
            Player p = players.get(uid);
            long take = Math.min(ante, p.chips);
            p.chips -= take;
            pot += take;
        }
        currentStake = ante;

        // shuffle and deal 3 cards each
        long roundSeed = seed ^ ((long) roundsPlayed * 0x9E3779B97F4A7C15L);
        List<Card> deck = Card.shuffled(roundSeed);
        int n = seatOrder.size();
        for (int i = 0; i < n; i++) hands.put(seatOrder.get(i), new ArrayList<>(3));
        for (int round = 0; round < 3; round++) {
            for (int i = 0; i < n; i++) {
                hands.get(seatOrder.get(i)).add(deck.remove(0));
            }
        }
        // first to act: left of dealer
        turnIndex = (dealerIndex + 1) % n;
        phase = PHASE_BETTING;
        notifyChanged();
    }

    @Override
    public ActionResult submit(Action a) {
        if (a == null || a.actorUid == null) return ActionResult.fail("Bad action");
        if (PHASE_ROUND_OVER.equals(phase)) return ActionResult.fail("Round over");

        String uid = a.actorUid;
        if (!players.containsKey(uid)) return ActionResult.fail("Not in room");
        if (folded.contains(uid)) return ActionResult.fail("You're folded");
        boolean myTurn = uid.equals(currentTurnUid());

        switch (a.kind) {
            case ACTION_TOGGLE_SEEN:
                // free action, can be done anytime before next bet
                seen.add(uid);
                notifyChanged();
                return ActionResult.ok();

            case ACTION_FOLD:
                if (!myTurn) return ActionResult.fail("Not your turn");
                folded.add(uid);
                advanceOrShowdown();
                return ActionResult.ok();

            case ACTION_CHAAL: {
                if (!myTurn) return ActionResult.fail("Not your turn");
                long owe = seen.contains(uid) ? currentStake * 2 : currentStake;
                Player p = players.get(uid);
                if (p.chips < owe) return ActionResult.fail("Not enough chips");
                p.chips -= owe;
                pot += owe;
                advanceOrShowdown();
                return ActionResult.ok();
            }

            case ACTION_RAISE: {
                if (!myTurn) return ActionResult.fail("Not your turn");
                long newStake = ((Number) a.payload.getOrDefault("stake", currentStake * 2L)).longValue();
                if (newStake < currentStake) return ActionResult.fail("Raise must exceed current stake");
                long owe = seen.contains(uid) ? newStake * 2 : newStake;
                Player p = players.get(uid);
                if (p.chips < owe) return ActionResult.fail("Not enough chips");
                p.chips -= owe;
                pot += owe;
                currentStake = newStake;
                advanceOrShowdown();
                return ActionResult.ok();
            }

            case ACTION_SHOW: {
                if (activePlayers().size() != 2) return ActionResult.fail("Show needs 2 players");
                if (!myTurn) return ActionResult.fail("Not your turn");
                long owe = seen.contains(uid) ? currentStake * 2 : currentStake;
                Player p = players.get(uid);
                if (p.chips < owe) return ActionResult.fail("Not enough chips");
                p.chips -= owe; pot += owe;
                showdown();
                return ActionResult.ok();
            }

            case ACTION_SIDE_SHOW: {
                if (!currentVariation.sideShowAllowed()) return ActionResult.fail("Side-show not allowed in this variation");
                String targetUid = (String) a.payload.get("target");
                if (targetUid == null || !seen.contains(uid) || !seen.contains(targetUid))
                    return ActionResult.fail("Both players must be seen");
                HandResult mine = TeenPattiHandRanker.eval3(hands.get(uid),
                        currentVariation.wildRanks(roundSeed()));
                HandResult their = TeenPattiHandRanker.eval3(hands.get(targetUid),
                        currentVariation.wildRanks(roundSeed()));
                int cmp = currentVariation.compare(mine, their);
                String loser = cmp < 0 ? uid : targetUid;
                folded.add(loser);
                advanceOrShowdown();
                return ActionResult.ok();
            }

            default:
                return ActionResult.fail("Unknown action: " + a.kind);
        }
    }

    private void advanceOrShowdown() {
        List<String> active = activePlayers();
        if (active.size() <= 1) {
            winnerUid = active.isEmpty() ? null : active.get(0);
            endRound();
            return;
        }
        // advance turn to next active
        int n = seatOrder.size();
        for (int i = 1; i <= n; i++) {
            int idx = (turnIndex + i) % n;
            if (!folded.contains(seatOrder.get(idx))) { turnIndex = idx; break; }
        }
        notifyChanged();
    }

    private void showdown() {
        phase = PHASE_SHOWDOWN;
        List<String> active = activePlayers();
        Set<com.example.cardclash.core.models.Rank> wilds =
                currentVariation.wildRanks(roundSeed());
        String best = null;
        HandResult bestHand = null;
        for (String uid : active) {
            HandResult h = TeenPattiHandRanker.eval3(hands.get(uid), wilds);
            if (best == null || currentVariation.compare(h, bestHand) > 0) {
                best = uid; bestHand = h;
            }
        }
        winnerUid = best;
        endRound();
    }

    private void endRound() {
        if (winnerUid != null) players.get(winnerUid).chips += pot;
        pot = 0;
        phase = PHASE_ROUND_OVER;
        roundsPlayed++;
        dealerIndex = (dealerIndex + 1) % seatOrder.size();
        notifyChanged();
    }

    private List<String> activePlayers() {
        List<String> out = new ArrayList<>();
        for (String uid : seatOrder) if (!folded.contains(uid)) out.add(uid);
        return out;
    }

    private long roundSeed() { return seed ^ ((long) roundsPlayed * 0x9E3779B97F4A7C15L); }

    /** Called by UI to start the next round (host-only in network mode). */
    public void nextRound() {
        if (!PHASE_ROUND_OVER.equals(phase)) return;
        startRound();
    }

    // -- snapshots & listeners -------------------------------------------------

    @Override public Map<String, Object> snapshot() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("phase", phase);
        s.put("pot", pot);
        s.put("currentStake", currentStake);
        s.put("turnIndex", turnIndex);
        s.put("dealerIndex", dealerIndex);
        s.put("roundsPlayed", roundsPlayed);
        s.put("variationId", currentVariation == null ? null : currentVariation.id());
        s.put("seen", new ArrayList<>(seen));
        s.put("folded", new ArrayList<>(folded));
        s.put("winnerUid", winnerUid);
        // Hands serialized as lists of "Rs" pairs; UI only renders own hand
        // unless phase is SHOWDOWN/ROUND_OVER.
        Map<String, List<String>> handsOut = new LinkedHashMap<>();
        for (Map.Entry<String, List<Card>> e : hands.entrySet()) {
            List<String> ids = new ArrayList<>();
            for (Card c : e.getValue()) ids.add(c.rank.label + ":" + c.suit.name());
            handsOut.put(e.getKey(), ids);
        }
        s.put("hands", handsOut);
        return s;
    }

    @Override public void restore(Map<String, Object> snap) {
        // v1: not used in single-device demo. Implemented for future sync.
    }

    @Override public void addListener(Listener l) { listeners.add(l); }
    @Override public void removeListener(Listener l) { listeners.remove(l); }
    private void notifyChanged() { for (Listener l : listeners) l.onStateChanged(); }

    @Override public String currentPhase() { return phase; }
    @Override public String currentTurnUid() {
        if (seatOrder.isEmpty()) return null;
        return seatOrder.get(turnIndex);
    }
    @Override public boolean isRoundOver() { return PHASE_ROUND_OVER.equals(phase); }
    @Override public boolean isGameOver() {
        // Host-controlled in v1: ends when explicitly stopped.
        return false;
    }

    // -- UI accessors (read-only) ---------------------------------------------
    public List<Card> handOf(String uid) {
        List<Card> h = hands.get(uid);
        return h == null ? Collections.emptyList() : Collections.unmodifiableList(h);
    }
    public boolean isSeen(String uid) { return seen.contains(uid); }
    public boolean isFolded(String uid) { return folded.contains(uid); }
    public long pot() { return pot; }
    public long currentStake() { return currentStake; }
    public TeenPattiVariation variation() { return currentVariation; }
    public String winnerUid() { return winnerUid; }
    public List<String> seatOrder() { return Collections.unmodifiableList(seatOrder); }
    public Player player(String uid) { return players.get(uid); }
    public int roundsPlayed() { return roundsPlayed; }
}
