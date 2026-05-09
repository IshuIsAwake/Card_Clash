package com.example.cardclash.games.bluff.engine;

/**
 * Plug for who, and when, may call "Bluff!" on the most recent claim.
 * Open Call: any player, any time before the next play resolves.
 * Next-Only: only the next-in-turn player, before they play their own cards.
 */
public interface BluffVariation {

    String id();
    String displayName();

    /** True if this variation lets non-next players also call the current claim. */
    boolean openCall();

    /** True if PASS is a meaningful action (only Next-Only mode uses it). */
    boolean allowsPass();

    static BluffVariation forId(String id) {
        if ("NEXT_ONLY".equals(id)) return new NextOnlyBluff();
        return new OpenCallBluff();
    }
}
