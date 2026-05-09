package com.example.cardclash.games.poker.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Builds Poker side pots from per-player commitments.
 *
 * <p>Algorithm: for each distinct commitment level (sorted ascending), peel off
 * a layer equal to (level - previousLevel). The pot for that layer is
 * (layerSize × eligible-player-count) and its eligible set is everyone whose
 * commitment ≥ level.
 */
public final class SidePotCalculator {

    private SidePotCalculator() {}

    public static class Pot {
        public final long amount;
        public final Set<String> eligibleUids;
        public Pot(long amount, Set<String> eligibleUids) {
            this.amount = amount;
            this.eligibleUids = Collections.unmodifiableSet(eligibleUids);
        }
        @Override public String toString() { return "Pot(" + amount + ", " + eligibleUids + ")"; }
    }

    /** @param commitments map of uid -> total chips committed this hand
     *  @param notFolded   uids of players still in the hand (eligible to win) */
    public static List<Pot> compute(java.util.Map<String, Long> commitments,
                                    Set<String> notFolded) {
        List<Pot> out = new ArrayList<>();
        TreeSet<Long> levels = new TreeSet<>();
        for (long v : commitments.values()) if (v > 0) levels.add(v);

        long prev = 0;
        for (long level : levels) {
            long delta = level - prev;
            // contributors: anyone whose commitment >= level
            int contributorCount = 0;
            Set<String> eligible = new HashSet<>();
            for (var e : commitments.entrySet()) {
                if (e.getValue() >= level) {
                    contributorCount++;
                    if (notFolded.contains(e.getKey())) eligible.add(e.getKey());
                }
            }
            long potAmount = delta * contributorCount;
            if (potAmount > 0 && !eligible.isEmpty()) out.add(new Pot(potAmount, eligible));
            prev = level;
        }
        return out;
    }
}
