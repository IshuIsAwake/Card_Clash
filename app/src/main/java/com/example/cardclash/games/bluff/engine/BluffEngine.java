package com.example.cardclash.games.bluff.engine;

import com.example.cardclash.core.engine.GameEngine;
import com.example.cardclash.core.models.Action;
import com.example.cardclash.core.models.ActionResult;
import com.example.cardclash.core.models.Card;
import com.example.cardclash.core.models.Player;
import com.example.cardclash.core.models.Rank;
import com.example.cardclash.core.models.RoomConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bluff engine — house-rule "sequence" variant.
 *
 * <p>One sequence = a stretch of plays all claiming the same rank. The first
 * play of a sequence locks the rank for that sequence. From then on, every
 * subsequent play in that sequence must claim the same rank (they can still
 * bluff with mismatched cards). Players may also skip (advance turn) or, if
 * they're the sequence starter, clear (discard the entire pile and end the
 * sequence). When a sequence ends — by clear or by a successful call — the
 * next player to act becomes the new sequence starter.
 *
 * <p>Win: when turn arrives at a player whose hand is empty, they win the round.
 *
 * <p>Call window: same as before. OPEN_CALL = anyone non-claimer; NEXT_ONLY =
 * only the next-in-turn. winner_keeps_turn controls who acts after a call.
 */
public class BluffEngine implements GameEngine {

    public static final String ACTION_PLAY = "play";
    public static final String ACTION_CALL_BLUFF = "call_bluff";
    public static final String ACTION_SKIP = "skip";
    public static final String ACTION_CLEAR = "clear";

    public static final String PHASE_PLAYING = "PLAYING";
    public static final String PHASE_ROUND_OVER = "ROUND_OVER";

    private final List<Listener> listeners = new ArrayList<>();
    private RoomConfig config;
    private List<String> seatOrder = new ArrayList<>();
    private Map<String, Player> players = new LinkedHashMap<>();
    private Map<String, List<Card>> hands = new HashMap<>();
    private List<Card> pile = new ArrayList<>();
    private Claim lastClaim;
    private boolean callable;
    private int turnIndex;
    private long seed;
    private BluffVariation variation;
    private boolean winnerKeepsTurn;
    private int roundsPlayed;
    private String winnerUid;
    private Map<String, Integer> roundsWon = new HashMap<>();
    private String phase = PHASE_PLAYING;
    private String lastResolution;

    // House-rule: sequence state
    private Rank sequenceRank;
    private String sequenceStarterUid;

    @Override
    public void initialize(RoomConfig cfg, List<Player> roster, long seed) {
        this.config = cfg;
        this.seed = seed;
        this.players.clear();
        this.seatOrder.clear();
        this.roundsWon.clear();
        for (Player p : roster) {
            players.put(p.uid, p);
            seatOrder.add(p.uid);
            roundsWon.put(p.uid, 0);
        }
        this.variation = BluffVariation.forId(cfg.strVal("call_rule", "OPEN_CALL"));
        this.winnerKeepsTurn = cfg.boolVal("winner_keeps_turn", true);
        this.roundsPlayed = 0;
        startRound();
    }

    private void startRound() {
        hands.clear();
        pile.clear();
        lastClaim = null;
        callable = false;
        winnerUid = null;
        lastResolution = null;
        sequenceRank = null;
        sequenceStarterUid = null;
        phase = PHASE_PLAYING;

        long roundSeed = seed ^ ((long) roundsPlayed * 0x9E3779B97F4A7C15L);
        List<Card> deck = Card.shuffled(roundSeed);
        for (String uid : seatOrder) hands.put(uid, new ArrayList<>());
        int i = 0;
        while (!deck.isEmpty()) {
            String uid = seatOrder.get(i % seatOrder.size());
            hands.get(uid).add(deck.remove(0));
            i++;
        }
        for (List<Card> h : hands.values()) {
            Collections.sort(h, (a, b) -> Integer.compare(a.rank.value, b.rank.value));
        }
        turnIndex = 0;
        notifyChanged();
    }

    @Override
    public ActionResult submit(Action a) {
        if (a == null || a.actorUid == null) return ActionResult.fail("Bad action");
        if (PHASE_ROUND_OVER.equals(phase)) return ActionResult.fail("Round is over");
        String uid = a.actorUid;
        if (!players.containsKey(uid)) return ActionResult.fail("Not in room");

        switch (a.kind) {
            case ACTION_PLAY: return doPlay(uid, a);
            case ACTION_CALL_BLUFF: return doCallBluff(uid);
            case ACTION_SKIP: return doSkip(uid);
            case ACTION_CLEAR: return doClear(uid);
            default: return ActionResult.fail("Unknown action: " + a.kind);
        }
    }

    private ActionResult doPlay(String uid, Action a) {
        if (!uid.equals(currentTurnUid())) return ActionResult.fail("Not your turn");

        String rankName = (String) a.payload.get("rank");
        Rank claimedRank;
        if (rankName == null) {
            if (sequenceRank == null) return ActionResult.fail("Pick a rank to claim");
            claimedRank = sequenceRank;
        } else {
            try { claimedRank = Rank.valueOf(rankName); }
            catch (IllegalArgumentException e) { return ActionResult.fail("Bad rank"); }
            if (sequenceRank != null && claimedRank != sequenceRank) {
                return ActionResult.fail("Sequence locked to " + sequenceRank.label + "s");
            }
        }

        Object idsRaw = a.payload.get("card_ids");
        if (!(idsRaw instanceof List) || ((List<?>) idsRaw).isEmpty())
            return ActionResult.fail("Select at least one card");
        List<String> cardIds = new ArrayList<>();
        for (Object o : (List<?>) idsRaw) cardIds.add(String.valueOf(o));

        List<Card> hand = hands.get(uid);
        List<Card> playing = new ArrayList<>();
        for (String id : cardIds) {
            Card found = null;
            for (Card c : hand) {
                if (c.id.equals(id) && !playing.contains(c)) { found = c; break; }
            }
            if (found == null) return ActionResult.fail("You don't own card " + id);
            playing.add(found);
        }

        // Apply this play
        hand.removeAll(playing);
        pile.addAll(playing);
        lastClaim = new Claim(uid, claimedRank, playing.size(), playing);
        callable = true;
        if (sequenceRank == null) {
            sequenceRank = claimedRank;
            sequenceStarterUid = uid;
        }
        lastResolution = null;

        advanceTurn();
        return ActionResult.ok();
    }

    private ActionResult doSkip(String uid) {
        if (!uid.equals(currentTurnUid())) return ActionResult.fail("Not your turn");
        if (sequenceRank == null) return ActionResult.fail("You're the starter — play, don't skip");
        // Skip just advances turn. lastClaim and callable unchanged.
        lastResolution = players.get(uid).displayName + " skipped.";
        advanceTurn();
        return ActionResult.ok();
    }

    private ActionResult doClear(String uid) {
        if (!uid.equals(currentTurnUid())) return ActionResult.fail("Not your turn");
        if (sequenceRank == null) return ActionResult.fail("No sequence to clear");
        if (!uid.equals(sequenceStarterUid)) return ActionResult.fail("Only the sequence starter may clear");
        int discarded = pile.size();
        pile.clear();
        Rank wasRank = sequenceRank;
        sequenceRank = null;
        sequenceStarterUid = null;
        lastClaim = null;
        callable = false;
        lastResolution = players.get(uid).displayName + " cleared the pile (" + discarded + " card" +
                (discarded == 1 ? "" : "s") + " of " + wasRank.label + "s discarded).";
        advanceTurn();
        return ActionResult.ok();
    }

    private ActionResult doCallBluff(String caller) {
        if (lastClaim == null || !callable) return ActionResult.fail("Nothing to call");
        if (caller.equals(lastClaim.claimerUid)) return ActionResult.fail("Can't call your own claim");
        if (!variation.openCall()) {
            if (!caller.equals(currentTurnUid())) return ActionResult.fail("Only the next player may call");
        }

        boolean truthful = true;
        for (Card c : lastClaim.playedCards) {
            if (c.rank != lastClaim.rank) { truthful = false; break; }
        }
        String pickerUp = truthful ? caller : lastClaim.claimerUid;
        hands.get(pickerUp).addAll(pile);
        Collections.sort(hands.get(pickerUp), (a, b) -> Integer.compare(a.rank.value, b.rank.value));
        pile.clear();

        lastResolution = (truthful
                ? "Truthful claim — " + players.get(caller).displayName + " picks up the pile."
                : "Bluff! " + players.get(lastClaim.claimerUid).displayName + " picks up the pile.");

        Claim resolved = lastClaim;
        lastClaim = null;
        callable = false;
        // Sequence ends on call resolution.
        sequenceRank = null;
        sequenceStarterUid = null;

        // Turn handoff
        if (winnerKeepsTurn) {
            String winner = truthful ? resolved.claimerUid : caller;
            turnIndex = seatOrder.indexOf(winner);
        } else {
            turnIndex = nextSeatIndex(seatOrder.indexOf(pickerUp));
        }
        // Auto-win check on the new turn holder
        if (handsEmpty(currentTurnUid())) {
            winnerUid = currentTurnUid();
            endRound();
            return ActionResult.ok();
        }
        notifyChanged();
        return ActionResult.ok();
    }

    private void advanceTurn() {
        turnIndex = nextSeatIndex(turnIndex);
        // If sequence still active and turn returns to the claimer of the live claim
        // with their hand empty, they win — no one called in time.
        if (lastClaim != null && callable
                && currentTurnUid().equals(lastClaim.claimerUid)
                && handsEmpty(lastClaim.claimerUid)) {
            winnerUid = lastClaim.claimerUid;
            lastResolution = players.get(winnerUid).displayName +
                    " emptied their hand — no one called in time.";
            endRound();
            return;
        }
        // General: any time the turn arrives at an empty-handed player, they've won.
        if (handsEmpty(currentTurnUid())) {
            winnerUid = currentTurnUid();
            lastResolution = players.get(winnerUid).displayName + " has no cards left — round won.";
            endRound();
            return;
        }
        notifyChanged();
    }

    private boolean handsEmpty(String uid) {
        List<Card> h = hands.get(uid);
        return h != null && h.isEmpty();
    }

    private int nextSeatIndex(int from) { return (from + 1) % seatOrder.size(); }

    private void endRound() {
        if (winnerUid != null) roundsWon.merge(winnerUid, 1, Integer::sum);
        phase = PHASE_ROUND_OVER;
        roundsPlayed++;
        notifyChanged();
    }

    public void nextRound() {
        if (!PHASE_ROUND_OVER.equals(phase)) return;
        startRound();
    }

    @Override public Map<String, Object> snapshot() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("phase", phase);
        s.put("turnIndex", turnIndex);
        s.put("roundsPlayed", roundsPlayed);
        s.put("callable", callable);
        s.put("pileSize", pile.size());
        s.put("winnerUid", winnerUid);
        s.put("variation", variation == null ? null : variation.id());
        s.put("sequenceRank", sequenceRank == null ? null : sequenceRank.name());
        s.put("sequenceStarter", sequenceStarterUid);
        if (lastClaim != null) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("claimerUid", lastClaim.claimerUid);
            c.put("rank", lastClaim.rank.name());
            c.put("count", lastClaim.count);
            s.put("lastClaim", c);
        }
        Map<String, Integer> handCounts = new LinkedHashMap<>();
        for (String uid : seatOrder) handCounts.put(uid, hands.get(uid).size());
        s.put("handCounts", handCounts);
        return s;
    }

    @Override public void restore(Map<String, Object> snapshot) { /* not used in v1 */ }

    @Override public void addListener(Listener l) { listeners.add(l); }
    @Override public void removeListener(Listener l) { listeners.remove(l); }
    private void notifyChanged() { for (Listener l : listeners) l.onStateChanged(); }

    @Override public String currentPhase() { return phase; }
    @Override public String currentTurnUid() {
        return seatOrder.isEmpty() ? null : seatOrder.get(turnIndex);
    }
    @Override public boolean isRoundOver() { return PHASE_ROUND_OVER.equals(phase); }
    @Override public boolean isGameOver() { return false; }

    public List<Card> handOf(String uid) {
        List<Card> h = hands.get(uid);
        return h == null ? Collections.emptyList() : Collections.unmodifiableList(h);
    }
    public int handSize(String uid) {
        List<Card> h = hands.get(uid);
        return h == null ? 0 : h.size();
    }
    public int pileSize() { return pile.size(); }
    public Claim lastClaim() { return lastClaim; }
    public boolean callable() { return callable; }
    public boolean winnerKeepsTurn() { return winnerKeepsTurn; }
    public BluffVariation variation() { return variation; }
    public String winnerUid() { return winnerUid; }
    public String lastResolution() { return lastResolution; }
    public List<String> seatOrder() { return Collections.unmodifiableList(seatOrder); }
    public Player player(String uid) { return players.get(uid); }
    public int roundsWon(String uid) { return roundsWon.getOrDefault(uid, 0); }
    public Rank sequenceRank() { return sequenceRank; }
    public String sequenceStarterUid() { return sequenceStarterUid; }

    public static class Claim {
        public final String claimerUid;
        public final Rank rank;
        public final int count;
        public final List<Card> playedCards;
        public Claim(String claimerUid, Rank rank, int count, List<Card> playedCards) {
            this.claimerUid = claimerUid;
            this.rank = rank;
            this.count = count;
            this.playedCards = new ArrayList<>(playedCards);
        }
    }
}
