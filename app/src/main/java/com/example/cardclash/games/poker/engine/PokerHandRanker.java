package com.example.cardclash.games.poker.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.example.cardclash.core.engine.HandEvaluator;
import com.example.cardclash.core.engine.HandResult;
import com.example.cardclash.core.models.Card;

/** Thin facade over {@link HandEvaluator#bestOf7} for Texas Hold'em best-5-of-7
 *  selection. Standard rankings, no wilds. */
public final class PokerHandRanker {
    private PokerHandRanker() {}

    public static HandResult evalHoldem(List<Card> hole, List<Card> community) {
        List<Card> seven = new ArrayList<>(hole.size() + community.size());
        seven.addAll(hole);
        seven.addAll(community);
        return HandEvaluator.bestOf7(seven, Collections.emptySet());
    }
}
