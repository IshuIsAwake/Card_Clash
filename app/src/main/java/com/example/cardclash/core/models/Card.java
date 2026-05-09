package com.example.cardclash.core.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Card {
    public final Rank rank;
    public final Suit suit;
    public final String id;

    public Card(Rank rank, Suit suit) {
        this.rank = rank;
        this.suit = suit;
        this.id = rank.label + suit.glyph;
    }

    public static List<Card> standardDeck() {
        List<Card> deck = new ArrayList<>(52);
        for (Suit s : Suit.values()) {
            for (Rank r : Rank.values()) {
                deck.add(new Card(r, s));
            }
        }
        return deck;
    }

    public static List<Card> shuffled(long seed) {
        List<Card> deck = standardDeck();
        Collections.shuffle(deck, new Random(seed));
        return deck;
    }

    public String label() { return rank.label + suit.glyph; }

    @Override public String toString() { return label(); }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Card)) return false;
        Card c = (Card) o;
        return c.rank == rank && c.suit == suit;
    }

    @Override public int hashCode() { return rank.ordinal() * 4 + suit.ordinal(); }
}
