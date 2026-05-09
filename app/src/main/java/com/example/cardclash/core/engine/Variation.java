package com.example.cardclash.core.engine;

/**
 * Marker interface. Each game module defines its own Variation contract that
 * extends this — see {@code teenpatti.engine.TeenPattiVariation},
 * {@code bluff.engine.BluffVariation}, {@code poker.engine.PokerVariant}.
 */
public interface Variation {
    String id();
    String displayName();
}
