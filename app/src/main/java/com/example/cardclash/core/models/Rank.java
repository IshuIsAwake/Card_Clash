package com.example.cardclash.core.models;

public enum Rank {
    TWO(2, "2"), THREE(3, "3"), FOUR(4, "4"), FIVE(5, "5"),
    SIX(6, "6"), SEVEN(7, "7"), EIGHT(8, "8"), NINE(9, "9"),
    TEN(10, "10"), JACK(11, "J"), QUEEN(12, "Q"), KING(13, "K"), ACE(14, "A");

    public final int value;
    public final String label;

    Rank(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public static Rank fromValue(int v) {
        for (Rank r : values()) if (r.value == v) return r;
        throw new IllegalArgumentException("No rank with value " + v);
    }
}
