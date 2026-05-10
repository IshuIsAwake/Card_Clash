package com.example.cardclash.games.poker.engine;

import com.example.cardclash.core.engine.HandRank;
import com.example.cardclash.core.engine.HandResult;
import com.example.cardclash.core.models.Action;
import com.example.cardclash.core.models.Card;
import com.example.cardclash.core.models.Player;
import com.example.cardclash.core.models.Rank;

import java.util.List;
import java.util.Random;

/**
 * Heuristic poker bot. Pre-flop uses a Chen-style score on hole cards; post-flop
 * uses the current best-hand category to set aggression. Pot-odds gate calls.
 *
 * <p>Not strong, but plays a coherent game and demonstrates the engine end-to-end.
 */
public final class PokerBotPolicy {

    private PokerBotPolicy() {}

    /** Build a single action for {@code uid}. The caller submits it. */
    public static Action chooseAction(PokerEngine engine, String uid, Random rng) {
        Player p = engine.player(uid);
        if (p == null) return new Action(uid, PokerEngine.ACTION_FOLD);
        long owed = engine.currentBet() - engine.committedThisStreet(uid);
        long pot  = engine.totalPot();
        boolean canCheck = (owed <= 0);

        double strength = handStrength(engine, uid);

        // pot odds — what fraction of the new pot does the call cost?
        double potOdds = owed > 0 ? (double) owed / Math.max(1.0, owed + pot) : 0.0;

        // Aggression jitter so games aren't deterministic
        double aggression = strength + (rng.nextDouble() - 0.5) * 0.1;

        if (canCheck) {
            if (aggression > 0.65 && p.chips >= engine.bigBlind() * 2) {
                long target = engine.currentBet() + engine.bigBlind() * (1 + rng.nextInt(3));
                target = Math.min(target, p.chips + engine.committedThisStreet(uid));
                return new Action(uid, PokerEngine.ACTION_RAISE).with("amount", target);
            }
            return new Action(uid, PokerEngine.ACTION_CHECK);
        }

        // Facing a bet
        if (aggression < 0.25 && potOdds > 0.2) {
            return new Action(uid, PokerEngine.ACTION_FOLD);
        }
        if (aggression > 0.80 && p.chips > owed * 2) {
            long minTarget = engine.minRaiseTarget();
            long target = Math.max(minTarget, engine.currentBet() + engine.bigBlind() * (2 + rng.nextInt(3)));
            target = Math.min(target, engine.committedThisStreet(uid) + p.chips);
            return new Action(uid, PokerEngine.ACTION_RAISE).with("amount", target);
        }
        // default: call (clamps to all-in if short)
        return new Action(uid, PokerEngine.ACTION_CALL);
    }

    /** 0.0 (trash) → 1.0 (monster). */
    private static double handStrength(PokerEngine engine, String uid) {
        List<Card> hole = engine.holeOf(uid);
        List<Card> board = engine.community();
        if (hole.isEmpty()) return 0.5;

        if (board.isEmpty()) {
            return chen(hole) / 20.0; // chen score caps near 20 for AA
        }
        HandResult r = PokerHandRanker.evalHoldem(hole, board);
        return categoryStrength(r);
    }

    /** Bill Chen formula, normalized so high-end hands sit near 1.0 after dividing by 20. */
    private static double chen(List<Card> hole) {
        Card a = hole.get(0), b = hole.get(1);
        Rank hi = a.rank.value >= b.rank.value ? a.rank : b.rank;
        Rank lo = a.rank.value >= b.rank.value ? b.rank : a.rank;
        double score;
        if (hi == Rank.ACE)        score = 10;
        else if (hi == Rank.KING)  score = 8;
        else if (hi == Rank.QUEEN) score = 7;
        else if (hi == Rank.JACK)  score = 6;
        else                        score = hi.value / 2.0;

        if (hi == lo) {
            score *= 2;
            if (score < 5) score = 5;
        } else {
            int gap = hi.value - lo.value - 1;
            if (gap == 1) score -= 1;
            else if (gap == 2) score -= 2;
            else if (gap == 3) score -= 4;
            else if (gap >= 4) score -= 5;
            if (gap <= 1 && hi.value < 12) score += 1;
        }
        if (a.suit == b.suit) score += 2;
        return Math.max(0, score);
    }

    private static double categoryStrength(HandResult r) {
        switch (r.rank) {
            case HIGH_CARD:        return 0.18;
            case PAIR:             return 0.42;
            case TWO_PAIR:         return 0.65;
            case THREE_OF_A_KIND:  return 0.78;
            case STRAIGHT:         return 0.85;
            case FLUSH:            return 0.90;
            case FULL_HOUSE:       return 0.94;
            case FOUR_OF_A_KIND:   return 0.98;
            case STRAIGHT_FLUSH:
            case ROYAL_FLUSH:      return 1.0;
        }
        return 0.5;
    }
}
