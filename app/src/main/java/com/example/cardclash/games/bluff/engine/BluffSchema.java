package com.example.cardclash.games.bluff.engine;

import com.example.cardclash.core.models.GameType;
import com.example.cardclash.core.settings.RuleSchema;
import com.example.cardclash.core.settings.Setting;
import com.example.cardclash.games.GameDefinition;
import com.example.cardclash.games.bluff.ui.BluffActivity;

import java.util.Arrays;
import java.util.List;

public final class BluffSchema {
    private BluffSchema() {}

    public static GameDefinition definition() {
        RuleSchema schema = RuleSchema.builder()
                .add(Setting.enumPicker("call_rule", "Bluff-Call Rule", "OPEN_CALL",
                        Arrays.asList("OPEN_CALL", "NEXT_ONLY"))
                        .help("Open Call: anyone may call. Next-Only: only the next player."))
                .add(Setting.toggle("winner_keeps_turn", "Winner Keeps Turn", true)
                        .help("After a call, whoever was right keeps the turn. Off = next-in-seat rotation."))
                .add(Setting.picker("turn_timer", "Turn Timer (s)", 30,
                        Arrays.asList(0, 10, 20, 30)))
                .add(Setting.slider("round_count", "Rounds", 5, 1, 30, 1))
                .build();

        List<String> reference = Arrays.asList(
                "Play any number of cards face-down, claiming a rank.",
                "Anyone (or only next, depending on rule) may call Bluff.",
                "If the claim was a lie, claimer takes the pile.",
                "If the claim was true, caller takes the pile.",
                "First to empty their hand wins the round."
        );

        List<GameDefinition.HelpSlide> help = Arrays.asList(
                new GameDefinition.HelpSlide("Goal",
                        "Be the first to empty your hand. Lie freely — but if you're caught, you pick up the pile."),
                new GameDefinition.HelpSlide("Your turn",
                        "Pick one or more cards from your hand and a rank to claim. The cards go face-down to the center pile."),
                new GameDefinition.HelpSlide("Calling Bluff",
                        "Open Call: anyone may call before the next play resolves. Next-Only: only the next player, before they play."),
                new GameDefinition.HelpSlide("Resolution",
                        "Reveal the played cards. If they all match the claim, the caller picks up the pile. If any don't, the claimer picks it up.")
        );

        return new GameDefinition(
                GameType.BLUFF, schema, BluffEngine::new,
                BluffActivity.class, reference, help
        );
    }
}
