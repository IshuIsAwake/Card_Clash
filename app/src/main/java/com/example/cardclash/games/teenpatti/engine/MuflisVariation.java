package com.example.cardclash.games.teenpatti.engine;

import com.example.cardclash.core.engine.HandResult;
import com.example.cardclash.core.models.Rank;

import java.util.Collections;
import java.util.Set;

public class MuflisVariation implements TeenPattiVariation {
    @Override public String id() { return "muflis"; }
    @Override public String displayName() { return "Muflis"; }
    @Override public Set<Rank> wildRanks(long roundSeed) { return Collections.emptySet(); }
    @Override public int compare(HandResult a, HandResult b) {
        // Lowest hand wins — flip the comparator.
        return -a.compareTo(b);
    }
    @Override public boolean sideShowAllowed() { return false; }
    @Override public String description() { return "Lowest hand wins. Side-show disabled."; }
}
