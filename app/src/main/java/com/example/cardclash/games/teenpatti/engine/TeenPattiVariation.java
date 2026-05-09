package com.example.cardclash.games.teenpatti.engine;

import com.example.cardclash.core.engine.HandResult;
import com.example.cardclash.core.engine.Variation;
import com.example.cardclash.core.models.Card;
import com.example.cardclash.core.models.Rank;

import java.util.List;
import java.util.Set;

/**
 * Per-round Teen Patti variation. Plug-points:
 *  - {@link #wildRanks} returns a set of ranks treated as jokers/wild for hand
 *    evaluation. Each round can return a different set (Joker Wild Cut).
 *  - {@link #compare} decides who wins between two evaluated hands. Default is
 *    "highest hand wins"; Muflis flips that.
 *  - {@link #sideShowAllowed} controls whether the side-show action is legal.
 */
public interface TeenPattiVariation extends Variation {

    /** Wild ranks effective for the round about to be played. May be empty. */
    Set<Rank> wildRanks(long roundSeed);

    /** Negative if a is worse, positive if a is better, 0 tie. */
    default int compare(HandResult a, HandResult b) { return a.compareTo(b); }

    default boolean sideShowAllowed() { return true; }

    /** Convenience for activity reference text. */
    String description();

    /** Built-in queue presets. Order matters — also default rotation order. */
    static List<TeenPattiVariation> defaultQueue() {
        return List.of(
                new ClassicVariation(),
                new AK47Variation(),
                new Variation1942(),
                new MuflisVariation(),
                new JokerWildVariation()
        );
    }
}
