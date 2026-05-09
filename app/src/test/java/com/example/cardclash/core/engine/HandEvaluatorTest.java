package com.example.cardclash.core.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.cardclash.core.models.Card;
import com.example.cardclash.core.models.Rank;
import com.example.cardclash.core.models.Suit;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public class HandEvaluatorTest {

    private static Card c(Rank r, Suit s) { return new Card(r, s); }

    @Test public void royalFlush_beatsStraightFlush() {
        HandResult royal = HandEvaluator.eval5(Arrays.asList(
                c(Rank.ACE, Suit.SPADES), c(Rank.KING, Suit.SPADES),
                c(Rank.QUEEN, Suit.SPADES), c(Rank.JACK, Suit.SPADES),
                c(Rank.TEN, Suit.SPADES)));
        HandResult sf = HandEvaluator.eval5(Arrays.asList(
                c(Rank.NINE, Suit.HEARTS), c(Rank.EIGHT, Suit.HEARTS),
                c(Rank.SEVEN, Suit.HEARTS), c(Rank.SIX, Suit.HEARTS),
                c(Rank.FIVE, Suit.HEARTS)));
        assertEquals(HandRank.ROYAL_FLUSH, royal.rank);
        assertEquals(HandRank.STRAIGHT_FLUSH, sf.rank);
        assertTrue(royal.compareTo(sf) > 0);
    }

    @Test public void quads_beatsFullHouse() {
        HandResult quads = HandEvaluator.eval5(Arrays.asList(
                c(Rank.SEVEN, Suit.SPADES), c(Rank.SEVEN, Suit.HEARTS),
                c(Rank.SEVEN, Suit.DIAMONDS), c(Rank.SEVEN, Suit.CLUBS),
                c(Rank.TWO, Suit.SPADES)));
        HandResult full = HandEvaluator.eval5(Arrays.asList(
                c(Rank.KING, Suit.SPADES), c(Rank.KING, Suit.HEARTS),
                c(Rank.KING, Suit.DIAMONDS), c(Rank.QUEEN, Suit.CLUBS),
                c(Rank.QUEEN, Suit.SPADES)));
        assertEquals(HandRank.FOUR_OF_A_KIND, quads.rank);
        assertEquals(HandRank.FULL_HOUSE, full.rank);
        assertTrue(quads.compareTo(full) > 0);
    }

    @Test public void wheelStraight_recognized() {
        HandResult wheel = HandEvaluator.eval5(Arrays.asList(
                c(Rank.ACE, Suit.SPADES), c(Rank.TWO, Suit.HEARTS),
                c(Rank.THREE, Suit.DIAMONDS), c(Rank.FOUR, Suit.CLUBS),
                c(Rank.FIVE, Suit.SPADES)));
        assertEquals(HandRank.STRAIGHT, wheel.rank);
        // High should be 5, not 14
        assertEquals(5, wheel.tiebreakers[0]);
    }

    @Test public void higherKickerWinsAtHighCard() {
        HandResult a = HandEvaluator.eval5(Arrays.asList(
                c(Rank.ACE, Suit.SPADES), c(Rank.KING, Suit.HEARTS),
                c(Rank.NINE, Suit.DIAMONDS), c(Rank.SEVEN, Suit.CLUBS),
                c(Rank.FIVE, Suit.SPADES)));
        HandResult b = HandEvaluator.eval5(Arrays.asList(
                c(Rank.ACE, Suit.HEARTS), c(Rank.KING, Suit.DIAMONDS),
                c(Rank.NINE, Suit.HEARTS), c(Rank.SEVEN, Suit.SPADES),
                c(Rank.FOUR, Suit.SPADES)));
        assertEquals(HandRank.HIGH_CARD, a.rank);
        assertEquals(HandRank.HIGH_CARD, b.rank);
        assertTrue(a.compareTo(b) > 0);
    }

    @Test public void wildAcePromotesPairToTrips() {
        HandResult r = HandEvaluator.eval5(Arrays.asList(
                c(Rank.KING, Suit.SPADES), c(Rank.KING, Suit.HEARTS),
                c(Rank.ACE, Suit.DIAMONDS), // wild
                c(Rank.SEVEN, Suit.CLUBS), c(Rank.FIVE, Suit.SPADES)),
                EnumSet.of(Rank.ACE));
        assertEquals(HandRank.THREE_OF_A_KIND, r.rank);
    }

    @Test public void bestOf7_findsCorrectFiveCardHand() {
        // Hole: K♠ K♥ ; Board: K♦ Q♣ Q♠ J♥ 2♠  — full house Kings full of Queens
        List<Card> seven = Arrays.asList(
                c(Rank.KING, Suit.SPADES), c(Rank.KING, Suit.HEARTS),
                c(Rank.KING, Suit.DIAMONDS), c(Rank.QUEEN, Suit.CLUBS),
                c(Rank.QUEEN, Suit.SPADES), c(Rank.JACK, Suit.HEARTS),
                c(Rank.TWO, Suit.SPADES));
        HandResult r = HandEvaluator.bestOf7(seven, Collections.emptySet());
        assertEquals(HandRank.FULL_HOUSE, r.rank);
        assertEquals(13 /* K */, r.tiebreakers[0]);
    }
}
