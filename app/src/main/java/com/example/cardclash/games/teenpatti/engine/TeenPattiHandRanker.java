package com.example.cardclash.games.teenpatti.engine;

import com.example.cardclash.core.engine.HandRank;
import com.example.cardclash.core.engine.HandResult;
import com.example.cardclash.core.models.Card;
import com.example.cardclash.core.models.Rank;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Teen Patti uses 3 cards. We map onto a subset of poker categories so the same
 * comparison machinery (HandResult.compareTo) works:
 *
 *   Trail (3 of a kind)   -> ordinal as FOUR_OF_A_KIND  (so it beats pure seq)
 *   Pure Sequence         -> ordinal as STRAIGHT_FLUSH
 *   Sequence              -> ordinal as FLUSH           (we want it to beat color? no)
 *
 * Actually the canonical Teen Patti order, high to low:
 *    Trail > Pure Sequence > Sequence > Color > Pair > High Card
 *
 * We assign ordinals so HandResult.compareTo respects this.
 */
public final class TeenPattiHandRanker {

    public static final int CAT_HIGH = 0;
    public static final int CAT_PAIR = 1;
    public static final int CAT_COLOR = 2;     // flush
    public static final int CAT_SEQUENCE = 3;
    public static final int CAT_PURE_SEQ = 4;  // straight flush
    public static final int CAT_TRAIL = 5;     // 3 of a kind

    private TeenPattiHandRanker() {}

    public static HandResult eval3(List<Card> cards, Set<Rank> wilds) {
        if (cards.size() != 3) throw new IllegalArgumentException("Teen Patti = 3 cards");
        if (wilds == null) wilds = new HashSet<>();

        List<Card> nat = new ArrayList<>();
        int wildCount = 0;
        for (Card c : cards) {
            if (wilds.contains(c.rank)) wildCount++;
            else nat.add(c);
        }
        nat.sort((a, b) -> Integer.compare(b.rank.value, a.rank.value));

        boolean flush = nat.size() == 0
                || nat.stream().map(c -> c.suit).distinct().count() == 1;
        int[] straightHigh = bestStraightHigh(nat, wildCount);
        boolean straight = straightHigh[0] > 0;

        int[] counts = new int[15];
        for (Card c : nat) counts[c.rank.value]++;
        int maxGroup = 0; int maxRank = 0;
        for (int i = 14; i >= 2; i--) {
            if (counts[i] > maxGroup) { maxGroup = counts[i]; maxRank = i; }
        }
        if (wildCount > 0) maxGroup += wildCount;

        // Trail (three of a kind)
        if (maxGroup >= 3) {
            int trailRank = wildCount == 3 ? 14 : maxRank;
            return result(CAT_TRAIL, new int[]{trailRank}, cards, "Trail");
        }
        if (straight && flush) {
            return result(CAT_PURE_SEQ, new int[]{straightHigh[0]}, cards, "Pure Sequence");
        }
        if (straight) {
            return result(CAT_SEQUENCE, new int[]{straightHigh[0]}, cards, "Sequence");
        }
        if (flush) {
            int[] tb = topRanks(nat, 3);
            return result(CAT_COLOR, tb, cards, "Color");
        }
        if (maxGroup >= 2) {
            // Pair: tiebreak by pair rank then kicker
            int pairRank = maxRank;
            int kicker = 0;
            for (int i = 14; i >= 2; i--) if (i != pairRank && counts[i] > 0) { kicker = i; break; }
            if (kicker == 0 && nat.size() == 1) kicker = nat.get(0).rank.value;
            return result(CAT_PAIR, new int[]{pairRank, kicker}, cards, "Pair");
        }
        return result(CAT_HIGH, topRanks(nat, 3), cards, "High Card");
    }

    private static HandResult result(int categoryOrdinal, int[] tb, List<Card> cards, String label) {
        // Encode as a synthetic HandRank index using ordinal*100 + tiebreak via
        // direct construction; we re-use HandResult by carrying category in the
        // first tiebreak slot.
        int[] finalTb = new int[tb.length + 1];
        finalTb[0] = categoryOrdinal;
        System.arraycopy(tb, 0, finalTb, 1, tb.length);
        // Use HIGH_CARD as placeholder; we compare via tiebreakers only.
        return new HandResult(HandRank.HIGH_CARD, finalTb, cards, label);
    }

    private static int[] bestStraightHigh(List<Card> nat, int wildCount) {
        Set<Integer> vals = new HashSet<>();
        for (Card c : nat) vals.add(c.rank.value);
        for (int high = 14; high >= 4; high--) {
            int wildsLeft = wildCount;
            boolean ok = true;
            for (int v = high; v > high - 3; v--) {
                if (!vals.contains(v)) {
                    if (wildsLeft > 0) wildsLeft--;
                    else { ok = false; break; }
                }
            }
            if (ok) return new int[]{high};
        }
        // A-2-3 wheel
        int wildsLeft = wildCount; boolean wheelOk = true;
        for (int v : new int[]{14, 2, 3}) {
            if (!vals.contains(v)) {
                if (wildsLeft > 0) wildsLeft--;
                else { wheelOk = false; break; }
            }
        }
        if (wheelOk) return new int[]{3};
        return new int[]{0};
    }

    private static int[] topRanks(List<Card> nat, int n) {
        int[] vals = new int[Math.min(n, nat.size())];
        for (int i = 0; i < vals.length; i++) vals[i] = nat.get(i).rank.value;
        return vals;
    }
}
