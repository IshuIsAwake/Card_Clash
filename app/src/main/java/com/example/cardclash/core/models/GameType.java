package com.example.cardclash.core.models;

public enum GameType {
    TEEN_PATTI("Teen Patti", 3, 8),
    BLUFF("Bluff", 3, 8),
    POKER("Poker", 2, 8);

    public final String displayName;
    public final int minPlayers;
    public final int maxPlayers;

    GameType(String displayName, int minPlayers, int maxPlayers) {
        this.displayName = displayName;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
    }
}
