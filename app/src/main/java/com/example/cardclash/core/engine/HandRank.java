package com.example.cardclash.core.engine;

/**
 * Poker-style hand categories, high-to-low. Used by both Poker and (mapped subset)
 * Teen Patti. Ordinal compares directly: higher ordinal = stronger hand.
 */
public enum HandRank {
    HIGH_CARD,
    PAIR,
    TWO_PAIR,
    THREE_OF_A_KIND,
    STRAIGHT,
    FLUSH,
    FULL_HOUSE,
    FOUR_OF_A_KIND,
    STRAIGHT_FLUSH,
    ROYAL_FLUSH;
}
