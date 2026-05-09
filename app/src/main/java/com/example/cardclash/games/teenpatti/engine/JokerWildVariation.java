package com.example.cardclash.games.teenpatti.engine;

import com.example.cardclash.core.models.Rank;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

public class JokerWildVariation implements TeenPattiVariation {
    @Override public String id() { return "joker_wild"; }
    @Override public String displayName() { return "Joker / Wild Cut"; }

    @Override public Set<Rank> wildRanks(long roundSeed) {
        Rank[] all = Rank.values();
        Rank wild = all[new Random(roundSeed).nextInt(all.length)];
        return EnumSet.of(wild);
    }

    @Override public String description() {
        return "One rank is randomly chosen as wild at the start of each round.";
    }
}
