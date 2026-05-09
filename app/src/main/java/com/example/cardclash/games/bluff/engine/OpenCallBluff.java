package com.example.cardclash.games.bluff.engine;

public class OpenCallBluff implements BluffVariation {
    @Override public String id() { return "OPEN_CALL"; }
    @Override public String displayName() { return "Open Call"; }
    @Override public boolean openCall() { return true; }
    @Override public boolean allowsPass() { return false; }
}
