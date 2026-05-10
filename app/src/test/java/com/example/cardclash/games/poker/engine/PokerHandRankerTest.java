package com.example.cardclash.games.poker.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.cardclash.core.engine.HandRank;
import com.example.cardclash.core.engine.HandResult;
import com.example.cardclash.core.models.Card;
import com.example.cardclash.core.models.Rank;
import com.example.cardclash.core.models.Suit;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class PokerHandRankerTest {

    private static Card c(Rank r, Suit s) { return new Card(r, s); }

    @Test public void boardPlays_pairOnBoardBeatsHighCardHole() {
        List<Card> hole = Arrays.asList(c(Rank.SEVEN, Suit.SPADES), c(Rank.TWO, Suit.HEARTS));
        List<Card> board = Arrays.asList(
                c(Rank.KING, Suit.DIAMONDS), c(Rank.KING, Suit.HEARTS),
                c(Rank.NINE, Suit.SPADES),   c(Rank.FIVE, Suit.CLUBS),
                c(Rank.THREE, Suit.HEARTS)
        );
        HandResult r = PokerHandRanker.evalHoldem(hole, board);
        assertEquals(HandRank.PAIR, r.rank);
        assertEquals(13 /* K */, r.tiebreakers[0]);
    }

    @Test public void flushOnTwoHearts() {
        // hole AhKh → board has 3 hearts → nut flush
        List<Card> hole = Arrays.asList(c(Rank.ACE, Suit.HEARTS), c(Rank.KING, Suit.HEARTS));
        List<Card> board = Arrays.asList(
                c(Rank.QUEEN, Suit.HEARTS), c(Rank.NINE, Suit.HEARTS),
                c(Rank.FOUR, Suit.HEARTS),  c(Rank.TWO, Suit.SPADES),
                c(Rank.SEVEN, Suit.CLUBS)
        );
        HandResult r = PokerHandRanker.evalHoldem(hole, board);
        assertEquals(HandRank.FLUSH, r.rank);
        assertEquals(14, r.tiebreakers[0]);
    }

    @Test public void wheelStraight_recognized() {
        List<Card> hole = Arrays.asList(c(Rank.ACE, Suit.HEARTS), c(Rank.TWO, Suit.SPADES));
        List<Card> board = Arrays.asList(
                c(Rank.THREE, Suit.DIAMONDS), c(Rank.FOUR, Suit.CLUBS),
                c(Rank.FIVE, Suit.HEARTS),    c(Rank.KING, Suit.HEARTS),
                c(Rank.QUEEN, Suit.HEARTS)
        );
        HandResult r = PokerHandRanker.evalHoldem(hole, board);
        assertEquals(HandRank.STRAIGHT, r.rank);
        assertEquals(5, r.tiebreakers[0]);
    }

    @Test public void straightFlushBeatsQuads() {
        List<Card> sfHole = Arrays.asList(c(Rank.NINE, Suit.SPADES), c(Rank.EIGHT, Suit.SPADES));
        List<Card> sfBoard = Arrays.asList(
                c(Rank.SEVEN, Suit.SPADES), c(Rank.SIX, Suit.SPADES),
                c(Rank.FIVE, Suit.SPADES),  c(Rank.TWO, Suit.HEARTS),
                c(Rank.THREE, Suit.CLUBS)
        );
        HandResult sf = PokerHandRanker.evalHoldem(sfHole, sfBoard);
        List<Card> qHole = Arrays.asList(c(Rank.ACE, Suit.SPADES), c(Rank.ACE, Suit.HEARTS));
        List<Card> qBoard = Arrays.asList(
                c(Rank.ACE, Suit.DIAMONDS), c(Rank.ACE, Suit.CLUBS),
                c(Rank.KING, Suit.SPADES),  c(Rank.QUEEN, Suit.HEARTS),
                c(Rank.JACK, Suit.CLUBS)
        );
        HandResult q = PokerHandRanker.evalHoldem(qHole, qBoard);
        assertEquals(HandRank.STRAIGHT_FLUSH, sf.rank);
        assertEquals(HandRank.FOUR_OF_A_KIND, q.rank);
        assertTrue(sf.compareTo(q) > 0);
    }

    @Test public void higherKickerWins_amongPairs() {
        List<Card> board = Arrays.asList(
                c(Rank.JACK, Suit.SPADES), c(Rank.JACK, Suit.HEARTS),
                c(Rank.NINE, Suit.DIAMONDS), c(Rank.FIVE, Suit.CLUBS),
                c(Rank.TWO, Suit.HEARTS)
        );
        HandResult a = PokerHandRanker.evalHoldem(
                Arrays.asList(c(Rank.ACE, Suit.SPADES), c(Rank.SEVEN, Suit.HEARTS)), board);
        HandResult b = PokerHandRanker.evalHoldem(
                Arrays.asList(c(Rank.KING, Suit.SPADES), c(Rank.SEVEN, Suit.DIAMONDS)), board);
        assertEquals(HandRank.PAIR, a.rank);
        assertEquals(HandRank.PAIR, b.rank);
        assertTrue(a.compareTo(b) > 0);
    }
}
