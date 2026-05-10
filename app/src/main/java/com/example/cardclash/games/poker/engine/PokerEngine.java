package com.example.cardclash.games.poker.engine;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.example.cardclash.core.engine.GameEngine;
import com.example.cardclash.core.engine.HandResult;
import com.example.cardclash.core.models.Action;
import com.example.cardclash.core.models.ActionResult;
import com.example.cardclash.core.models.Card;
import com.example.cardclash.core.models.Player;
import com.example.cardclash.core.models.RoomConfig;

/**
 * Texas Hold'em engine. Authoritative state lives here; UI submits {@link Action}s
 * and reads via accessors / {@link #snapshot()}.
 *
 * <p>Hand flow per round:
 *   POSTING_BLINDS (transient) → PRE_FLOP → FLOP → TURN → RIVER → SHOWDOWN → ROUND_OVER.
 *   A burn card precedes each post-flop deal. Side pots are computed via
 *   {@link SidePotCalculator} at showdown.
 */
public class PokerEngine implements GameEngine {

    public static final String ACTION_FOLD            = "fold";
    public static final String ACTION_CHECK           = "check";
    public static final String ACTION_CALL            = "call";
    public static final String ACTION_RAISE           = "raise";   // payload "amount" = total this-street commitment target
    public static final String ACTION_ALL_IN          = "all_in";
    public static final String ACTION_POST_BLIND     = "post_blind";  // manual mode only
    public static final String ACTION_DEAL            = "deal";        // host_deal mode (host triggers blind post + first deal)
    public static final String ACTION_NEXT_ROUND      = "next_round";
    public static final String ACTION_END_ROUND       = "end_round";       // host
    public static final String ACTION_BUY_IN          = "buy_in";          // host, payload target_uid, amount
    public static final String ACTION_REQUEST_BUY_IN  = "request_buy_in";  // any player, payload amount

    public static final String PHASE_POSTING_BLINDS = "POSTING_BLINDS";
    public static final String PHASE_PRE_FLOP   = "PRE_FLOP";
    public static final String PHASE_FLOP       = "FLOP";
    public static final String PHASE_TURN       = "TURN";
    public static final String PHASE_RIVER      = "RIVER";
    public static final String PHASE_SHOWDOWN   = "SHOWDOWN";
    public static final String PHASE_ROUND_OVER = "ROUND_OVER";

    public static final String BLIND_MODE_MANUAL     = "MANUAL";
    public static final String BLIND_MODE_HOST_DEAL  = "HOST_DEAL";

    public static final String MIN_RAISE_NONE         = "NONE";
    public static final String MIN_RAISE_BB           = "BB";
    public static final String MIN_RAISE_2BB          = "2BB";
    public static final String MIN_RAISE_3BB          = "3BB";
    public static final String MIN_RAISE_LAST         = "LAST";

    public static final String EVENT_BURN         = "burn";
    public static final String EVENT_DEAL_STREET  = "deal_street";
    public static final String EVENT_SHOWDOWN     = "showdown";
    public static final String EVENT_ROUND_AWARD  = "round_award";
    public static final String EVENT_BUY_IN_REQ   = "buy_in_request";

    // -- listeners ----------------------------------------------------------
    private final List<Listener> listeners = new ArrayList<>();
    private void notifyChanged() { for (Listener l : listeners) l.onStateChanged(); }
    private void emit(String kind, Map<String, Object> p) { for (Listener l : listeners) l.onEvent(kind, p); }

    // -- config / immutable per-game --------------------------------------
    private RoomConfig config;
    private long seed;
    private long sbAmount, bbAmount;
    private int  roundCountLimit;             // 0 = open-ended
    private boolean hostCanEndRound;
    private boolean hostCanBuyIn;
    private String blindMode;
    private String minRaiseMode;
    private final PokerVariation variation = new HoldemVariation();

    // -- per-round blind tracking (for MANUAL mode) ----------------------
    private int sbSeat = -1, bbSeat = -1;
    private boolean sbPosted, bbPosted;

    // -- action log (last 3 rounds; oldest dropped) ----------------------
    public static class LoggedAction {
        public final int roundIndex;
        public final String phase;
        public final String actorUid, actorName;
        public final String kind;
        public final long amount;       // 0 if not chip-related
        public final long timestamp;
        public LoggedAction(int round, String phase, String uid, String name,
                            String kind, long amount) {
            this.roundIndex = round; this.phase = phase;
            this.actorUid = uid; this.actorName = name;
            this.kind = kind; this.amount = amount;
            this.timestamp = System.currentTimeMillis();
        }
    }
    private final java.util.LinkedList<LoggedAction> actionLog = new java.util.LinkedList<>();
    private static final int LOG_RETAIN_ROUNDS = 3;

    // -- roster -----------------------------------------------------------
    private final List<String> seatOrder = new ArrayList<>();
    private final Map<String, Player> players = new LinkedHashMap<>();

    // -- per-round state --------------------------------------------------
    private int dealerIndex;
    private int turnIndex;
    private int roundsPlayed;
    private String phase = PHASE_ROUND_OVER;
    private Deque<Card> deck = new ArrayDeque<>();
    private final Map<String, List<Card>> holeCards = new HashMap<>();
    private final List<Card> community = new ArrayList<>();
    private final List<Card> burned = new ArrayList<>();
    private final Set<String> folded = new HashSet<>();
    private final Set<String> allIn = new HashSet<>();
    private final Map<String, Long> committedThisStreet = new HashMap<>();
    private final Map<String, Long> committedTotal = new HashMap<>();
    private final Set<String> actedThisStreet = new HashSet<>();
    private long currentBet;
    private long minRaiseSize;
    private String lastAggressorUid;

    // -- showdown / award state ------------------------------------------
    private final List<SidePotCalculator.Pot> showdownPots = new ArrayList<>();
    private final Map<String, HandResult> showdownHands = new LinkedHashMap<>();
    private final Map<String, Long> potWinnings = new LinkedHashMap<>();
    private String soleWinnerUid; // set when everyone else folded

    // -- queued buy-ins (applied at next startRound) ----------------------
    private final List<String> pendingBuyInUids = new ArrayList<>();
    private final List<Long> pendingBuyInAmounts = new ArrayList<>();

    // ====================================================================
    // INITIALIZATION
    // ====================================================================

    @Override public void initialize(RoomConfig config, List<Player> playerList, long seed) {
        this.config = config;
        this.seed = seed;
        this.sbAmount = config.longVal("small_blind", 25);
        this.bbAmount = config.longVal("big_blind", 50);
        if (this.bbAmount < this.sbAmount) this.bbAmount = this.sbAmount;
        this.roundCountLimit = config.intVal("round_count", 0);
        this.hostCanEndRound = config.boolVal("host_can_end_round", true);
        this.hostCanBuyIn = config.boolVal("host_can_buy_in", true);
        this.blindMode = config.strVal("blind_mode", BLIND_MODE_MANUAL);
        this.minRaiseMode = config.strVal("min_raise_mode", MIN_RAISE_LAST);

        seatOrder.clear();
        players.clear();
        for (Player p : playerList) {
            players.put(p.uid, p);
            seatOrder.add(p.uid);
        }
        dealerIndex = 0;
        roundsPlayed = 0;
        startRound();
    }

    private void startRound() {
        // apply queued buy-ins
        for (int i = 0; i < pendingBuyInUids.size(); i++) {
            Player p = players.get(pendingBuyInUids.get(i));
            if (p != null) p.chips += pendingBuyInAmounts.get(i);
        }
        pendingBuyInUids.clear();
        pendingBuyInAmounts.clear();

        // drop disconnected/zero-chip players from rotation? v1 keeps them but they auto-fold by short stacks.
        folded.clear();
        allIn.clear();
        holeCards.clear();
        community.clear();
        burned.clear();
        committedThisStreet.clear();
        committedTotal.clear();
        actedThisStreet.clear();
        showdownPots.clear();
        showdownHands.clear();
        potWinnings.clear();
        soleWinnerUid = null;
        currentBet = 0;
        minRaiseSize = openingMinRaiseSize();
        lastAggressorUid = null;

        long roundSeed = seed ^ ((long) roundsPlayed * 0x9E3779B97F4A7C15L);
        deck = new ArrayDeque<>(Card.shuffled(roundSeed));

        // anyone with 0 chips sits this hand out
        for (String uid : seatOrder) {
            if (players.get(uid).chips <= 0) folded.add(uid);
        }

        if (activePlayerCount() < 2) {
            phase = PHASE_ROUND_OVER;
            notifyChanged();
            return;
        }

        // rotate dealer to a player who's still in
        int n = seatOrder.size();
        if (roundsPlayed > 0) {
            for (int i = 1; i <= n; i++) {
                int idx = (dealerIndex + i) % n;
                if (!folded.contains(seatOrder.get(idx))) { dealerIndex = idx; break; }
            }
        } else {
            // first round: ensure dealer is an active seat
            if (folded.contains(seatOrder.get(dealerIndex))) {
                for (int i = 0; i < n; i++) {
                    if (!folded.contains(seatOrder.get(i))) { dealerIndex = i; break; }
                }
            }
        }

        // deal hole cards
        for (String uid : seatOrder) {
            if (!folded.contains(uid)) holeCards.put(uid, new ArrayList<>(variation.holeCardCount()));
        }
        for (int round = 0; round < variation.holeCardCount(); round++) {
            for (int i = 1; i <= n; i++) {
                String uid = seatOrder.get((dealerIndex + i) % n);
                if (folded.contains(uid)) continue;
                holeCards.get(uid).add(deck.pollFirst());
            }
        }

        // resolve seat positions — heads-up rule: dealer is SB
        int active = activePlayerCount();
        if (active == 2) {
            sbSeat = dealerIndex;
            bbSeat = nextActiveSeat(dealerIndex);
        } else {
            sbSeat = nextActiveSeat(dealerIndex);
            bbSeat = nextActiveSeat(sbSeat);
        }
        sbPosted = false;
        bbPosted = false;

        if (BLIND_MODE_HOST_DEAL.equals(blindMode)) {
            // host hasn't tapped DEAL yet — sit on POSTING_BLINDS until they do
            phase = PHASE_POSTING_BLINDS;
            turnIndex = hostSeat();
            notifyChanged();
            return;
        }

        // MANUAL: SB acts first by tapping POST BLIND, then BB.
        phase = PHASE_POSTING_BLINDS;
        turnIndex = sbSeat;
        notifyChanged();
    }

    /** Auto-post both blinds and transition to PRE_FLOP. Called from HOST_DEAL flow. */
    private void autoPostBlindsAndStart() {
        postBlind(seatOrder.get(sbSeat), sbAmount);
        postBlind(seatOrder.get(bbSeat), bbAmount);
        sbPosted = bbPosted = true;
        finishBlindsAndOpenPreflop();
    }

    private void finishBlindsAndOpenPreflop() {
        currentBet = bbAmount;
        minRaiseSize = openingMinRaiseSize();
        lastAggressorUid = seatOrder.get(bbSeat); // BB closes preflop action when checked-around

        int firstToActSeat = (activePlayerCount() == 2) ? sbSeat : nextActiveSeat(bbSeat);
        turnIndex = firstToActSeat;
        phase = PHASE_PRE_FLOP;
        skipFoldedAndAllInForTurn();
        notifyChanged();
    }

    private int hostSeat() {
        for (int i = 0; i < seatOrder.size(); i++) {
            Player p = players.get(seatOrder.get(i));
            if (p != null && p.host) return i;
        }
        return 0;
    }

    private void postBlind(String uid, long amount) {
        Player p = players.get(uid);
        long take = Math.min(amount, p.chips);
        p.chips -= take;
        committedThisStreet.merge(uid, take, Long::sum);
        committedTotal.merge(uid, take, Long::sum);
        if (p.chips == 0) allIn.add(uid);
    }

    // ====================================================================
    // SUBMIT
    // ====================================================================

    @Override public ActionResult submit(Action a) {
        if (a == null || a.actorUid == null || a.kind == null) return ActionResult.fail("Bad action");
        String uid = a.actorUid;
        Player actor = players.get(uid);
        if (actor == null) return ActionResult.fail("Not in room");
        boolean isHost = actor.host;

        // Always-available actions first
        switch (a.kind) {
            case ACTION_NEXT_ROUND:
                if (!PHASE_ROUND_OVER.equals(phase)) return ActionResult.fail("Round not over");
                if (roundCountLimit > 0 && roundsPlayed >= roundCountLimit)
                    return ActionResult.fail("Round limit reached");
                roundsPlayed++;
                startRound();
                return ActionResult.ok();
            case ACTION_REQUEST_BUY_IN: {
                long amt = ((Number) a.payload.getOrDefault("amount", 0L)).longValue();
                if (amt <= 0) return ActionResult.fail("Invalid amount");
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("uid", uid); p.put("amount", amt);
                emit(EVENT_BUY_IN_REQ, p);
                return ActionResult.ok();
            }
            case ACTION_BUY_IN: {
                if (!isHost) return ActionResult.fail("Host only");
                if (!hostCanBuyIn) return ActionResult.fail("Host buy-in disabled");
                String target = (String) a.payload.get("target_uid");
                long amt = ((Number) a.payload.getOrDefault("amount", 0L)).longValue();
                if (target == null || !players.containsKey(target)) return ActionResult.fail("Bad target");
                if (amt <= 0) return ActionResult.fail("Invalid amount");
                pendingBuyInUids.add(target);
                pendingBuyInAmounts.add(amt);
                if (PHASE_ROUND_OVER.equals(phase)) {
                    // apply immediately so the player rejoins the next deal
                    Player tp = players.get(target);
                    tp.chips += amt;
                    pendingBuyInUids.remove(pendingBuyInUids.size() - 1);
                    pendingBuyInAmounts.remove(pendingBuyInAmounts.size() - 1);
                }
                notifyChanged();
                return ActionResult.ok();
            }
            case ACTION_END_ROUND: {
                if (!isHost) return ActionResult.fail("Host only");
                if (!hostCanEndRound) return ActionResult.fail("End round disabled");
                if (PHASE_ROUND_OVER.equals(phase)) return ActionResult.fail("Already over");
                forceShowdown();
                return ActionResult.ok();
            }
            case ACTION_DEAL: {
                if (!PHASE_POSTING_BLINDS.equals(phase)) return ActionResult.fail("Not awaiting deal");
                if (!BLIND_MODE_HOST_DEAL.equals(blindMode)) return ActionResult.fail("Manual blind mode");
                if (!isHost) return ActionResult.fail("Host only");
                logAction(uid, ACTION_DEAL, 0);
                autoPostBlindsAndStart();
                return ActionResult.ok();
            }
            case ACTION_POST_BLIND: {
                if (!PHASE_POSTING_BLINDS.equals(phase)) return ActionResult.fail("Blinds already posted");
                if (!BLIND_MODE_MANUAL.equals(blindMode)) return ActionResult.fail("Manual blind mode disabled");
                if (!uid.equals(currentTurnUid())) return ActionResult.fail("Not your turn to post");
                if (!sbPosted) {
                    if (!seatOrder.get(sbSeat).equals(uid)) return ActionResult.fail("Small blind first");
                    postBlind(uid, sbAmount);
                    sbPosted = true;
                    logAction(uid, ACTION_POST_BLIND, Math.min(sbAmount, players.get(uid).chips + sbAmount));
                    turnIndex = bbSeat;
                    notifyChanged();
                    return ActionResult.ok();
                }
                if (!bbPosted) {
                    if (!seatOrder.get(bbSeat).equals(uid)) return ActionResult.fail("Big blind next");
                    postBlind(uid, bbAmount);
                    bbPosted = true;
                    logAction(uid, ACTION_POST_BLIND, Math.min(bbAmount, players.get(uid).chips + bbAmount));
                    finishBlindsAndOpenPreflop();
                    return ActionResult.ok();
                }
                return ActionResult.fail("Blinds already posted");
            }
            default:
                break;
        }

        // Player turn-based actions
        if (PHASE_ROUND_OVER.equals(phase) || PHASE_SHOWDOWN.equals(phase))
            return ActionResult.fail("Round over");
        if (PHASE_POSTING_BLINDS.equals(phase))
            return ActionResult.fail("Post blinds first");
        if (folded.contains(uid)) return ActionResult.fail("Folded");
        if (allIn.contains(uid)) return ActionResult.fail("All-in");
        if (!uid.equals(currentTurnUid())) return ActionResult.fail("Not your turn");

        switch (a.kind) {
            case ACTION_FOLD:
                folded.add(uid);
                actedThisStreet.add(uid);
                logAction(uid, ACTION_FOLD, 0);
                postAction();
                return ActionResult.ok();

            case ACTION_CHECK: {
                long owed = currentBet - committedThisStreet.getOrDefault(uid, 0L);
                if (owed > 0) return ActionResult.fail("Cannot check; bet to call");
                actedThisStreet.add(uid);
                logAction(uid, ACTION_CHECK, 0);
                postAction();
                return ActionResult.ok();
            }

            case ACTION_CALL: {
                long owed = currentBet - committedThisStreet.getOrDefault(uid, 0L);
                if (owed <= 0) return ActionResult.fail("Nothing to call; check");
                long take = Math.min(owed, actor.chips);
                actor.chips -= take;
                committedThisStreet.merge(uid, take, Long::sum);
                committedTotal.merge(uid, take, Long::sum);
                if (actor.chips == 0) allIn.add(uid);
                actedThisStreet.add(uid);
                logAction(uid, ACTION_CALL, take);
                postAction();
                return ActionResult.ok();
            }

            case ACTION_RAISE: {
                long target = ((Number) a.payload.getOrDefault("amount", 0L)).longValue();
                long curMine = committedThisStreet.getOrDefault(uid, 0L);
                long delta = target - curMine;
                if (delta <= 0) return ActionResult.fail("Raise must add chips");
                if (delta > actor.chips) return ActionResult.fail("Not enough chips");
                long minTarget = currentBet + minRaiseSize;
                boolean isAllIn = (delta == actor.chips);
                if (!isMinRaiseRelaxed() && target < minTarget && !isAllIn)
                    return ActionResult.fail("Min raise to " + minTarget);
                if (target <= currentBet && !isAllIn)
                    return ActionResult.fail("Raise must exceed current bet");
                actor.chips -= delta;
                committedThisStreet.put(uid, target);
                committedTotal.merge(uid, delta, Long::sum);
                if (actor.chips == 0) allIn.add(uid);

                long increment = target - currentBet;
                if (increment >= minRaiseSize) {
                    minRaiseSize = nextMinRaiseSize(increment);
                    // re-open action: only this player has acted
                    actedThisStreet.clear();
                    actedThisStreet.add(uid);
                    lastAggressorUid = uid;
                    currentBet = target;
                } else {
                    // short all-in below minRaiseSize: per casino rules this doesn't
                    // reopen action for players who already acted (they don't owe more).
                    if (target > currentBet) currentBet = target;
                    actedThisStreet.add(uid);
                }
                logAction(uid, ACTION_RAISE, delta);
                postAction();
                return ActionResult.ok();
            }

            case ACTION_ALL_IN: {
                long curMine = committedThisStreet.getOrDefault(uid, 0L);
                long target = curMine + actor.chips;
                Action proxy = new Action(uid, ACTION_RAISE).with("amount", target);
                if (target > currentBet) {
                    return submit(proxy); // re-enter as raise / short raise
                }
                // pure call all-in
                long take = actor.chips;
                actor.chips = 0;
                committedThisStreet.merge(uid, take, Long::sum);
                committedTotal.merge(uid, take, Long::sum);
                allIn.add(uid);
                actedThisStreet.add(uid);
                logAction(uid, ACTION_ALL_IN, take);
                postAction();
                return ActionResult.ok();
            }

            default:
                return ActionResult.fail("Unknown action: " + a.kind);
        }
    }

    // ====================================================================
    // STREET ADVANCE / SHOWDOWN
    // ====================================================================

    private void postAction() {
        // sole survivor wins immediately
        int notFolded = activePlayerCount();
        if (notFolded == 1) {
            soleWinnerUid = firstNotFolded();
            awardSoleWinner();
            phase = PHASE_ROUND_OVER;
            notifyChanged();
            return;
        }

        // street complete?
        if (isStreetComplete()) {
            advanceStreet();
        } else {
            advanceTurn();
        }
        notifyChanged();
    }

    private boolean isStreetComplete() {
        for (String uid : seatOrder) {
            if (folded.contains(uid)) continue;
            if (allIn.contains(uid)) continue;
            if (!actedThisStreet.contains(uid)) return false;
            long c = committedThisStreet.getOrDefault(uid, 0L);
            if (c != currentBet) return false;
        }
        return true;
    }

    private void advanceTurn() {
        int n = seatOrder.size();
        for (int i = 1; i <= n; i++) {
            int idx = (turnIndex + i) % n;
            String uid = seatOrder.get(idx);
            if (folded.contains(uid) || allIn.contains(uid)) continue;
            turnIndex = idx;
            return;
        }
        // nobody left to act — shouldn't happen here (caller checks isStreetComplete first)
    }

    private void advanceStreet() {
        // reset street betting state
        committedThisStreet.clear();
        actedThisStreet.clear();
        currentBet = 0;
        minRaiseSize = openingMinRaiseSize();
        lastAggressorUid = null;

        switch (phase) {
            case PHASE_PRE_FLOP:
                burnAndDealCommunity(3);
                phase = PHASE_FLOP;
                break;
            case PHASE_FLOP:
                burnAndDealCommunity(1);
                phase = PHASE_TURN;
                break;
            case PHASE_TURN:
                burnAndDealCommunity(1);
                phase = PHASE_RIVER;
                break;
            case PHASE_RIVER:
                runShowdown();
                return;
            default:
                return;
        }

        // first to act post-flop: first non-folded non-allin left of dealer
        turnIndex = nextActiveSeat(dealerIndex);
        skipFoldedAndAllInForTurn();

        // if 1 or 0 players can still act, run remaining streets without betting
        if (countCanAct() < 2) {
            advanceStreet();
        }
    }

    private void burnAndDealCommunity(int n) {
        if (deck.size() >= 1) {
            burned.add(deck.pollFirst());
            Map<String, Object> p = new LinkedHashMap<>();
            emit(EVENT_BURN, p);
        }
        for (int i = 0; i < n && !deck.isEmpty(); i++) community.add(deck.pollFirst());
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("phase", phase);
        emit(EVENT_DEAL_STREET, p);
    }

    private void runShowdown() {
        phase = PHASE_SHOWDOWN;
        Set<String> live = new HashSet<>();
        for (String uid : seatOrder) if (!folded.contains(uid)) live.add(uid);

        // evaluate hands
        for (String uid : live) {
            HandResult r = PokerHandRanker.evalHoldem(holeCards.get(uid), community);
            showdownHands.put(uid, r);
        }

        // build pots and award
        showdownPots.clear();
        showdownPots.addAll(SidePotCalculator.compute(committedTotal, live));
        for (SidePotCalculator.Pot pot : showdownPots) {
            List<String> winners = new ArrayList<>();
            HandResult best = null;
            for (String uid : pot.eligibleUids) {
                HandResult h = showdownHands.get(uid);
                if (h == null) continue;
                if (best == null || h.compareTo(best) > 0) {
                    best = h; winners.clear(); winners.add(uid);
                } else if (h.compareTo(best) == 0) {
                    winners.add(uid);
                }
            }
            if (winners.isEmpty()) continue;
            long share = pot.amount / winners.size();
            long remainder = pot.amount - share * winners.size();
            for (int i = 0; i < winners.size(); i++) {
                String w = winners.get(i);
                long got = share + (i == 0 ? remainder : 0);
                players.get(w).chips += got;
                potWinnings.merge(w, got, Long::sum);
            }
        }

        Map<String, Object> p = new LinkedHashMap<>();
        emit(EVENT_SHOWDOWN, p);
        emit(EVENT_ROUND_AWARD, p);
        phase = PHASE_ROUND_OVER;
    }

    private void awardSoleWinner() {
        long total = totalPot();
        Player p = players.get(soleWinnerUid);
        p.chips += total;
        potWinnings.merge(soleWinnerUid, total, Long::sum);
        Map<String, Object> ev = new LinkedHashMap<>();
        ev.put("winner", soleWinnerUid);
        emit(EVENT_ROUND_AWARD, ev);
    }

    /** Host-driven: deal remaining community cards face-up, then resolve as showdown. */
    private void forceShowdown() {
        // deal remaining streets without betting
        while (!PHASE_RIVER.equals(phase) && !PHASE_SHOWDOWN.equals(phase)) {
            committedThisStreet.clear();
            actedThisStreet.clear();
            currentBet = 0;
            switch (phase) {
                case PHASE_PRE_FLOP:
                    burnAndDealCommunity(3); phase = PHASE_FLOP; break;
                case PHASE_FLOP:
                    burnAndDealCommunity(1); phase = PHASE_TURN; break;
                case PHASE_TURN:
                    burnAndDealCommunity(1); phase = PHASE_RIVER; break;
                default: break;
            }
        }
        runShowdown();
        notifyChanged();
    }

    // ====================================================================
    // HELPERS
    // ====================================================================

    /** Initial min-raise size for a fresh street (no raise yet). */
    private long openingMinRaiseSize() {
        switch (minRaiseMode == null ? MIN_RAISE_LAST : minRaiseMode) {
            case MIN_RAISE_NONE: return 1;
            case MIN_RAISE_2BB:  return bbAmount * 2;
            case MIN_RAISE_3BB:  return bbAmount * 3;
            case MIN_RAISE_BB:
            case MIN_RAISE_LAST:
            default: return bbAmount;
        }
    }

    /** Min-raise size after a player just raised by {@code increment}. Standard
     *  poker is "match the last raise" but the host can fix it to a multiple of BB,
     *  or NONE for casual no-min play (any raise above the current bet is legal). */
    private long nextMinRaiseSize(long increment) {
        switch (minRaiseMode == null ? MIN_RAISE_LAST : minRaiseMode) {
            case MIN_RAISE_NONE: return 1;
            case MIN_RAISE_BB:   return bbAmount;
            case MIN_RAISE_2BB:  return bbAmount * 2;
            case MIN_RAISE_3BB:  return bbAmount * 3;
            case MIN_RAISE_LAST:
            default: return Math.max(bbAmount, increment);
        }
    }

    /** Whether the engine allows arbitrary raise sizes (above current bet). */
    private boolean isMinRaiseRelaxed() { return MIN_RAISE_NONE.equals(minRaiseMode); }

    private void logAction(String uid, String kind, long amount) {
        Player p = players.get(uid);
        actionLog.addLast(new LoggedAction(roundsPlayed, phase, uid,
                p == null ? uid : p.displayName, kind, amount));
        // drop entries from any round older than (current - LOG_RETAIN_ROUNDS + 1)
        int cutoff = roundsPlayed - LOG_RETAIN_ROUNDS + 1;
        while (!actionLog.isEmpty() && actionLog.peekFirst().roundIndex < cutoff) {
            actionLog.removeFirst();
        }
    }

    private int activePlayerCount() {
        int n = 0;
        for (String uid : seatOrder) if (!folded.contains(uid)) n++;
        return n;
    }

    private int countCanAct() {
        int n = 0;
        for (String uid : seatOrder) if (!folded.contains(uid) && !allIn.contains(uid)) n++;
        return n;
    }

    private String firstNotFolded() {
        for (String uid : seatOrder) if (!folded.contains(uid)) return uid;
        return null;
    }

    private int nextActiveSeat(int from) {
        int n = seatOrder.size();
        for (int i = 1; i <= n; i++) {
            int idx = (from + i) % n;
            if (!folded.contains(seatOrder.get(idx))) return idx;
        }
        return from;
    }

    private void skipFoldedAndAllInForTurn() {
        int n = seatOrder.size();
        for (int i = 0; i < n; i++) {
            int idx = (turnIndex + i) % n;
            String uid = seatOrder.get(idx);
            if (!folded.contains(uid) && !allIn.contains(uid)) { turnIndex = idx; return; }
        }
    }

    public long totalPot() {
        long sum = 0;
        for (long v : committedTotal.values()) sum += v;
        return sum;
    }

    // ====================================================================
    // GameEngine plumbing
    // ====================================================================

    @Override public void addListener(Listener l) { listeners.add(l); }
    @Override public void removeListener(Listener l) { listeners.remove(l); }

    @Override public String currentPhase() { return phase; }

    @Override public String currentTurnUid() {
        if (PHASE_ROUND_OVER.equals(phase) || PHASE_SHOWDOWN.equals(phase)) return null;
        if (seatOrder.isEmpty()) return null;
        return seatOrder.get(turnIndex);
    }

    @Override public boolean isRoundOver() { return PHASE_ROUND_OVER.equals(phase); }

    @Override public boolean isGameOver() {
        if (roundCountLimit > 0 && roundsPlayed >= roundCountLimit && PHASE_ROUND_OVER.equals(phase))
            return true;
        // Or only one player has chips left
        int withChips = 0;
        for (Player p : players.values()) if (p.chips > 0) withChips++;
        return withChips < 2;
    }

    @Override public Map<String, Object> snapshot() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("phase", phase);
        s.put("dealerIndex", dealerIndex);
        s.put("turnIndex", turnIndex);
        s.put("currentBet", currentBet);
        s.put("minRaiseSize", minRaiseSize);
        s.put("pot", totalPot());
        s.put("roundsPlayed", roundsPlayed);
        s.put("folded", new ArrayList<>(folded));
        s.put("allIn", new ArrayList<>(allIn));
        s.put("committedThisStreet", new LinkedHashMap<>(committedThisStreet));
        s.put("committedTotal", new LinkedHashMap<>(committedTotal));
        s.put("acted", new ArrayList<>(actedThisStreet));

        // Community cards always public (post-flop only)
        List<String> commIds = new ArrayList<>();
        for (Card c : community) commIds.add(c.rank.label + ":" + c.suit.name());
        s.put("community", commIds);
        s.put("burnedCount", burned.size());

        // Hole cards: filtered per-recipient by the network layer; engine emits all,
        // but we tag with revealAt so the network sync can mask. Same pattern as Teen Patti.
        Map<String, List<String>> holes = new LinkedHashMap<>();
        for (Map.Entry<String, List<Card>> e : holeCards.entrySet()) {
            List<String> ids = new ArrayList<>();
            for (Card c : e.getValue()) ids.add(c.rank.label + ":" + c.suit.name());
            holes.put(e.getKey(), ids);
        }
        s.put("holeCards", holes);
        s.put("holesPublic", PHASE_SHOWDOWN.equals(phase) || PHASE_ROUND_OVER.equals(phase));

        // Showdown payload
        if (PHASE_ROUND_OVER.equals(phase) || PHASE_SHOWDOWN.equals(phase)) {
            s.put("potWinnings", new LinkedHashMap<>(potWinnings));
            s.put("soleWinnerUid", soleWinnerUid);
            List<Map<String, Object>> potsOut = new ArrayList<>();
            for (SidePotCalculator.Pot p : showdownPots) {
                Map<String, Object> po = new LinkedHashMap<>();
                po.put("amount", p.amount);
                po.put("eligible", new ArrayList<>(p.eligibleUids));
                potsOut.add(po);
            }
            s.put("pots", potsOut);
        }
        return s;
    }

    @Override public void restore(Map<String, Object> snapshot) {
        // v1: Firebase replay not used for poker (single-device only). Stub for future.
    }

    // ====================================================================
    // UI accessors
    // ====================================================================

    public List<String> seatOrder() { return Collections.unmodifiableList(seatOrder); }
    public Player player(String uid) { return players.get(uid); }
    public int dealerIndex() { return dealerIndex; }
    public List<Card> community() { return Collections.unmodifiableList(community); }
    public int burnedCount() { return burned.size(); }
    public List<Card> holeOf(String uid) {
        List<Card> h = holeCards.get(uid);
        return h == null ? Collections.emptyList() : Collections.unmodifiableList(h);
    }
    public boolean isFolded(String uid) { return folded.contains(uid); }
    public boolean isAllIn(String uid) { return allIn.contains(uid); }
    public long committedThisStreet(String uid) { return committedThisStreet.getOrDefault(uid, 0L); }
    public long committedTotal(String uid) { return committedTotal.getOrDefault(uid, 0L); }
    public long currentBet() { return currentBet; }
    public long minRaiseTarget() { return currentBet + minRaiseSize; }
    public long smallBlind() { return sbAmount; }
    public long bigBlind() { return bbAmount; }
    public PokerVariation variation() { return variation; }
    public int roundsPlayed() { return roundsPlayed; }
    public Map<String, HandResult> showdownHands() { return Collections.unmodifiableMap(showdownHands); }
    public List<SidePotCalculator.Pot> showdownPots() { return Collections.unmodifiableList(showdownPots); }
    public Map<String, Long> potWinnings() { return Collections.unmodifiableMap(potWinnings); }
    public String soleWinnerUid() { return soleWinnerUid; }
    public boolean hostCanEndRound() { return hostCanEndRound; }
    public boolean hostCanBuyIn() { return hostCanBuyIn; }
    public String blindMode() { return blindMode; }
    public boolean isManualBlinds() { return BLIND_MODE_MANUAL.equals(blindMode); }
    public int sbSeat() { return sbSeat; }
    public int bbSeat() { return bbSeat; }
    public boolean sbPosted() { return sbPosted; }
    public boolean bbPosted() { return bbPosted; }
    public List<LoggedAction> actionLog() { return Collections.unmodifiableList(actionLog); }
}
