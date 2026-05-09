package com.example.cardclash.games.teenpatti.engine;

import com.example.cardclash.core.models.Rank;

import java.util.Collections;
import java.util.Set;

public class ClassicVariation implements TeenPattiVariation {
    @Override public String id() { return "classic"; }
    @Override public String displayName() { return "Classic"; }
    @Override public Set<Rank> wildRanks(long roundSeed) { return Collections.emptySet(); }
    @Override public String description() { return "Standard Teen Patti rules. No wild cards."; }
}
