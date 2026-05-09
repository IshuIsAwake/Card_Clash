package com.example.cardclash.games.teenpatti.engine;

import com.example.cardclash.core.models.Rank;

import java.util.EnumSet;
import java.util.Set;

public class AK47Variation implements TeenPattiVariation {
    private static final Set<Rank> WILDS =
            EnumSet.of(Rank.ACE, Rank.KING, Rank.FOUR, Rank.SEVEN);

    @Override public String id() { return "ak47"; }
    @Override public String displayName() { return "AK47"; }
    @Override public Set<Rank> wildRanks(long roundSeed) { return WILDS; }
    @Override public String description() { return "Aces, Kings, 4s and 7s are wild."; }
}
