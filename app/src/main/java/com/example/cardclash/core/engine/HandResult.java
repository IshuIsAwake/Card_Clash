package com.example.cardclash.core.engine;

import java.util.Arrays;
import java.util.List;

import com.example.cardclash.core.models.Card;

/**
 * Hand category + tiebreaker key. Compare by category ordinal, then by tiebreakers
 * lexicographically (high to low).
 */
public class HandResult implements Comparable<HandResult> {
    public final HandRank rank;
    /** High-to-low ranked values driving tiebreak (e.g. quads-rank then kicker). */
    public final int[] tiebreakers;
    public final List<Card> cards;
    public final String description;

    public HandResult(HandRank rank, int[] tiebreakers, List<Card> cards, String description) {
        this.rank = rank;
        this.tiebreakers = tiebreakers;
        this.cards = cards;
        this.description = description;
    }

    @Override public int compareTo(HandResult other) {
        int c = Integer.compare(this.rank.ordinal(), other.rank.ordinal());
        if (c != 0) return c;
        int n = Math.min(tiebreakers.length, other.tiebreakers.length);
        for (int i = 0; i < n; i++) {
            int d = Integer.compare(tiebreakers[i], other.tiebreakers[i]);
            if (d != 0) return d;
        }
        return 0;
    }

    @Override public String toString() {
        return rank + Arrays.toString(tiebreakers);
    }
}
