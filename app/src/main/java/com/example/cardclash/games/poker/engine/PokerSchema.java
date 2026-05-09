package com.example.cardclash.games.poker.engine;

import com.example.cardclash.core.models.GameType;
import com.example.cardclash.core.settings.RuleSchema;
import com.example.cardclash.core.settings.Setting;
import com.example.cardclash.games.GameDefinition;
import com.example.cardclash.games.poker.ui.PokerActivity;

import java.util.Arrays;
import java.util.List;

public final class PokerSchema {
    private PokerSchema() {}

    public static GameDefinition definition() {
        RuleSchema schema = RuleSchema.builder()
                .add(Setting.enumPicker("variant", "Variant", "HOLDEM",
                        Arrays.asList("HOLDEM", "FIVE_CARD_DRAW")))
                .add(Setting.slider("small_blind", "Small Blind", 5, 1, 100, 1))
                .add(Setting.slider("big_blind", "Big Blind", 10, 2, 200, 1))
                .add(Setting.slider("buy_in", "Buy-In", 1000, 100, 100000, 100))
                .add(Setting.picker("turn_timer", "Turn Timer (s)", 30,
                        Arrays.asList(0, 10, 20, 30)))
                .add(Setting.slider("round_count", "Rounds", 0, 0, 50, 1))
                .build();

        List<String> reference = Arrays.asList(
                "Royal Flush", "Straight Flush", "Four of a Kind", "Full House",
                "Flush", "Straight", "Three of a Kind", "Two Pair", "Pair", "High Card"
        );

        return new GameDefinition(
                GameType.POKER, schema, PokerEngine::new,
                PokerActivity.class, reference,
                Arrays.asList(new GameDefinition.HelpSlide("Coming soon",
                        "Poker implementation lands in Phase 4."))
        );
    }
}
