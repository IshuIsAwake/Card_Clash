package com.example.cardclash.games.teenpatti.engine;

import com.example.cardclash.core.models.Rank;

import java.util.EnumSet;
import java.util.Set;

public class Variation1942 implements TeenPattiVariation {
    // 1 (Ace), 9, 4, 2 are wild.
    private static final Set<Rank> WILDS =
            EnumSet.of(Rank.ACE, Rank.NINE, Rank.FOUR, Rank.TWO);

    @Override public String id() { return "1942"; }
    @Override public String displayName() { return "1942 A Love Story"; }
    @Override public Set<Rank> wildRanks(long roundSeed) { return WILDS; }
    @Override public String description() { return "Aces (1), 9s, 4s and 2s are wild."; }
}
