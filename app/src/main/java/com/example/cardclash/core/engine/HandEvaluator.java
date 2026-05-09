package com.example.cardclash.core.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.cardclash.core.models.Card;
import com.example.cardclash.core.models.Rank;

/**
 * 5-card poker hand evaluator. Parameterized by a joker / wild rank set so the same
 * code handles vanilla Poker, AK47 Teen Patti, "1942 A Love Story", and the
 * per-round Joker variation.
 *
 * For Teen Patti the caller passes only 3 cards; we treat that as a 3-card eval and
 * map the subset of categories appropriate to Teen Patti.
 */
public final class HandEvaluator {

    private HandEvaluator() {}

    public static HandResult eval5(List<Card> cards) {
        return eval5(cards, Collections.emptySet());
    }

    /** Evaluate 5 cards with optional wild ranks. */
    public static HandResult eval5(List<Card> cards, Set<Rank> wilds) {
        if (cards.size() != 5) throw new IllegalArgumentException("eval5 expects 5 cards");
        if (wilds == null) wilds = Collections.emptySet();

        List<Card> nat = new ArrayList<>();
        int wildCount = 0;
        for (Card c : cards) {
            if (wilds.contains(c.rank)) wildCount++;
            else nat.add(c);
        }

        // Sort natural cards descending by rank value
        nat.sort((a, b) -> Integer.compare(b.rank.value, a.rank.value));

        boolean flushPossible = isFlushPossible(nat);
        int[] straightHigh = bestStraightHigh(nat, wildCount);

        int[] counts = rankCounts(nat); // index 2..14
        int[] groups = groupSizesDesc(counts); // descending group sizes

        // Apply wilds to the largest group
        if (wildCount > 0 && groups.length > 0) {
            groups[0] += wildCount;
            Arrays.sort(groups);
            // re-descend
            int[] desc = new int[groups.length];
            for (int i = 0; i < groups.length; i++) desc[i] = groups[groups.length - 1 - i];
            groups = desc;
        } else if (wildCount > 0) {
            groups = new int[]{wildCount};
        }

        // Determine best category
        boolean isStraight = straightHigh[0] > 0;
        boolean isFlush = flushPossible; // wilds always assignable to needed suit when flush is possible

        if (isStraight && isFlush) {
            int high = straightHigh[0];
            HandRank rank = (high == 14) ? HandRank.ROYAL_FLUSH : HandRank.STRAIGHT_FLUSH;
            return new HandResult(rank, new int[]{high}, cards, rank.name());
        }
        if (groups.length > 0 && groups[0] >= 4) {
            return new HandResult(HandRank.FOUR_OF_A_KIND, kickers(counts, 4, 1, wildCount),
                    cards, "Four of a Kind");
        }
        if (groups.length >= 2 && groups[0] >= 3 && groups[1] >= 2) {
            return new HandResult(HandRank.FULL_HOUSE, kickers(counts, 3, 2, wildCount),
                    cards, "Full House");
        }
        if (isFlush) {
            int[] tb = topRanks(nat, 5);
            return new HandResult(HandRank.FLUSH, tb, cards, "Flush");
        }
        if (isStraight) {
            return new HandResult(HandRank.STRAIGHT, new int[]{straightHigh[0]}, cards, "Straight");
        }
        if (groups.length > 0 && groups[0] >= 3) {
            return new HandResult(HandRank.THREE_OF_A_KIND, kickers(counts, 3, 0, wildCount),
                    cards, "Three of a Kind");
        }
        if (groups.length >= 2 && groups[0] >= 2 && groups[1] >= 2) {
            return new HandResult(HandRank.TWO_PAIR, kickers(counts, 2, 2, wildCount),
                    cards, "Two Pair");
        }
        if (groups.length > 0 && groups[0] >= 2) {
            return new HandResult(HandRank.PAIR, kickers(counts, 2, 0, wildCount),
                    cards, "Pair");
        }
        return new HandResult(HandRank.HIGH_CARD, topRanks(nat, 5), cards, "High Card");
    }

    /** Evaluate the best 5-card hand from 7 (Hold'em). */
    public static HandResult bestOf7(List<Card> seven, Set<Rank> wilds) {
        if (seven.size() < 5) throw new IllegalArgumentException("Need >= 5 cards");
        HandResult best = null;
        int n = seven.size();
        for (int a = 0; a < n; a++)
        for (int b = a + 1; b < n; b++)
        for (int c = b + 1; c < n; c++)
        for (int d = c + 1; d < n; d++)
        for (int e = d + 1; e < n; e++) {
            List<Card> hand = Arrays.asList(seven.get(a), seven.get(b), seven.get(c), seven.get(d), seven.get(e));
            HandResult r = eval5(hand, wilds);
            if (best == null || r.compareTo(best) > 0) best = r;
        }
        return best;
    }

    // -- helpers ----------------------------------------------------------

    private static boolean isFlushPossible(List<Card> nat) {
        if (nat.isEmpty()) return true; // all wild
        var suit = nat.get(0).suit;
        for (Card c : nat) if (c.suit != suit) return false;
        return true;
    }

    /** Returns {high,0} or {0,0} if no straight. Handles wheel A-2-3-4-5 (high=5). */
    private static int[] bestStraightHigh(List<Card> nat, int wildCount) {
        Set<Integer> vals = new HashSet<>();
        for (Card c : nat) vals.add(c.rank.value);
        // Try high-to-low windows of 5 consecutive ranks
        for (int high = 14; high >= 5; high--) {
            int wildsLeft = wildCount;
            boolean ok = true;
            for (int v = high; v > high - 5; v--) {
                if (!vals.contains(v)) {
                    if (wildsLeft > 0) wildsLeft--;
                    else { ok = false; break; }
                }
            }
            if (ok) return new int[]{high, 0};
        }
        // Wheel: A-2-3-4-5 (treat A as 1)
        int wildsLeft = wildCount;
        boolean wheelOk = true;
        int[] wheel = {14, 2, 3, 4, 5};
        for (int v : wheel) {
            if (!vals.contains(v)) {
                if (wildsLeft > 0) wildsLeft--;
                else { wheelOk = false; break; }
            }
        }
        if (wheelOk) return new int[]{5, 0};
        return new int[]{0, 0};
    }

    private static int[] rankCounts(List<Card> nat) {
        int[] counts = new int[15]; // 2..14
        for (Card c : nat) counts[c.rank.value]++;
        return counts;
    }

    private static int[] groupSizesDesc(int[] counts) {
        List<Integer> sizes = new ArrayList<>();
        for (int i = 2; i <= 14; i++) if (counts[i] > 0) sizes.add(counts[i]);
        sizes.sort(Collections.reverseOrder());
        int[] out = new int[sizes.size()];
        for (int i = 0; i < sizes.size(); i++) out[i] = sizes.get(i);
        return out;
    }

    /** First-N tiebreak ranks: rank of size>=primary, then size>=secondary, then kickers. */
    private static int[] kickers(int[] counts, int primary, int secondary, int wildCount) {
        List<Integer> tb = new ArrayList<>();
        Integer primaryRank = topRankWithCount(counts, primary);
        if (primaryRank != null) tb.add(primaryRank);
        if (secondary > 0) {
            Integer secondaryRank = topRankWithCount(counts, secondary, primaryRank);
            if (secondaryRank != null) tb.add(secondaryRank);
        }
        // Add remaining cards as kickers, descending
        for (int i = 14; i >= 2 && tb.size() < 5; i--) {
            if (counts[i] > 0 && (primaryRank == null || i != primaryRank)
                    && (tb.size() < 2 || i != tb.get(1))) {
                tb.add(i);
            }
        }
        // Pad with wild "ace-high" assumption if needed
        while (tb.size() < 1 && wildCount > 0) { tb.add(14); break; }
        int[] out = new int[tb.size()];
        for (int i = 0; i < tb.size(); i++) out[i] = tb.get(i);
        return out;
    }

    private static Integer topRankWithCount(int[] counts, int min, Integer... exclude) {
        Set<Integer> ex = new HashSet<>();
        for (Integer e : exclude) if (e != null) ex.add(e);
        for (int i = 14; i >= 2; i--) if (counts[i] >= min && !ex.contains(i)) return i;
        return null;
    }

    private static int[] topRanks(List<Card> nat, int n) {
        int[] vals = new int[Math.min(n, nat.size())];
        for (int i = 0; i < vals.length; i++) vals[i] = nat.get(i).rank.value;
        return vals;
    }
}
