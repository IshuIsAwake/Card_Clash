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
                .add(Setting.slider("small_blind", "Small Blind", 25, 5, 500, 5))
                .add(Setting.slider("big_blind", "Big Blind", 50, 10, 1000, 10))
                .add(Setting.slider("buy_in", "Buy-In", 5000, 500, 100000, 500))
                .add(Setting.enumPicker("blind_mode", "Blind Posting", "MANUAL",
                        Arrays.asList("MANUAL", "HOST_DEAL"))
                        .help("MANUAL: small/big blind each press POST BLIND on their turn. "
                                + "HOST_DEAL: the host taps DEAL once and blinds auto-post."))
                .add(Setting.enumPicker("min_raise_mode", "Minimum Raise", "NONE",
                        Arrays.asList("NONE", "BB", "2BB", "3BB", "LAST"))
                        .help("NONE: any raise above the current bet is legal — raise by 25, 75, or anything you like (default, casual). "
                                + "LAST: standard poker, a raise must be at least the size of the previous raise (resets each street). "
                                + "BB / 2BB / 3BB lock the minimum to a fixed multiple of the big blind."))
                .add(Setting.picker("turn_timer", "Turn Timer (s)", 0,
                        Arrays.asList(0, 15, 30, 45, 60))
                        .help("0 disables the timer (default — no auto-action). "
                                + "Otherwise, idle players auto-check or auto-fold."))
                .add(Setting.slider("round_count", "Rounds", 0, 0, 50, 1)
                        .help("0 plays open-ended until the host ends the game."))
                .add(Setting.toggle("host_can_end_round", "Host can end round", true))
                .add(Setting.toggle("host_can_buy_in", "Host approves buy-ins", true))
                .build();

        List<String> reference = Arrays.asList(
                "Royal Flush", "Straight Flush", "Four of a Kind", "Full House",
                "Flush", "Straight", "Three of a Kind", "Two Pair", "Pair", "High Card"
        );

        List<GameDefinition.HelpSlide> help = Arrays.asList(
                new GameDefinition.HelpSlide("Blinds & Dealer Button",
                        "Each round, the dealer button rotates left. Small blind sits left of the button, "
                                + "big blind left of small blind. Heads-up: the dealer is small blind."),
                new GameDefinition.HelpSlide("Streets & Burn Cards",
                        "Pre-flop you have your two hole cards. Then a burn card is discarded and the FLOP "
                                + "(3 community) deals. Burn → TURN (1 community). Burn → RIVER (1 community). "
                                + "A betting round runs after each deal."),
                new GameDefinition.HelpSlide("Bets, Calls, Raises",
                        "Check when no bet is owed; call to match the current bet; raise must be at least the "
                                + "size of the previous raise. All-in is allowed any time."),
                new GameDefinition.HelpSlide("Side Pots",
                        "When a player goes all-in for less than the current bet, a side pot forms. Players who "
                                + "matched only the all-in level can win only the main pot; deeper stacks contest the side."),
                new GameDefinition.HelpSlide("Host Controls",
                        "The host can end the round early (remaining streets deal out and the best hand wins) "
                                + "and approve buy-ins requested by short-stacked players.")
        );

        return new GameDefinition(
                GameType.POKER, schema, PokerEngine::new,
                PokerActivity.class, reference, help
        );
    }
}
