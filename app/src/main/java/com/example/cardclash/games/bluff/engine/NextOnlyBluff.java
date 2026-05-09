package com.example.cardclash.games.bluff.engine;

public class NextOnlyBluff implements BluffVariation {
    @Override public String id() { return "NEXT_ONLY"; }
    @Override public String displayName() { return "Next-Only"; }
    @Override public boolean openCall() { return false; }
    @Override public boolean allowsPass() { return true; }
}
