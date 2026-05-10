package com.example.cardclash.games.poker.engine;

public class HoldemVariation implements PokerVariation {
    @Override public String id() { return "HOLDEM"; }
    @Override public String displayName() { return "Texas Hold'em"; }
    @Override public String description() {
        return "Two hole cards each, five community cards revealed across flop, turn, and river. "
                + "Best 5-card hand from any combination wins.";
    }
    @Override public int holeCardCount() { return 2; }
    @Override public int streetCount() { return 4; }
    @Override public boolean usesCommunityCards() { return true; }
}
