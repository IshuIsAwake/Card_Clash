package com.example.cardclash.games.poker.engine;

/** Strategy hook for poker variants. v1 only ships {@link HoldemVariation}; the
 *  interface exists so 5-Card Draw and house-rule variants can slot in later
 *  without touching {@link PokerEngine}. */
public interface PokerVariation {
    String id();
    String displayName();
    String description();

    /** Hole cards dealt face-down to each player at the start of the round. */
    int holeCardCount();

    /** Number of betting streets including pre-flop. */
    int streetCount();

    /** Whether the variation uses community cards (Hold'em yes, draw no). */
    boolean usesCommunityCards();
}
