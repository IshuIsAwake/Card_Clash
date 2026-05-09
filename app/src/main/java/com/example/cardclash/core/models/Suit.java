package com.example.cardclash.core.models;

public enum Suit {
    SPADES("♠", true),
    HEARTS("♥", false),
    DIAMONDS("♦", false),
    CLUBS("♣", true);

    public final String glyph;
    public final boolean black;

    Suit(String glyph, boolean black) {
        this.glyph = glyph;
        this.black = black;
    }
}
