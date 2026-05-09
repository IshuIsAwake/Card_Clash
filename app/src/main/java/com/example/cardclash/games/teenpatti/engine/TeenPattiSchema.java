package com.example.cardclash.games.teenpatti.engine;

import com.example.cardclash.core.models.GameType;
import com.example.cardclash.core.settings.RuleSchema;
import com.example.cardclash.core.settings.Setting;
import com.example.cardclash.games.GameDefinition;
import com.example.cardclash.games.teenpatti.ui.TeenPattiActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class TeenPattiSchema {

    private TeenPattiSchema() {}

    public static GameDefinition definition() {
        List<String> variationIds = new ArrayList<>();
        for (TeenPattiVariation v : TeenPattiVariation.defaultQueue()) variationIds.add(v.id());

        RuleSchema schema = RuleSchema.builder()
                .add(Setting.slider("boot_amount", "Boot / Ante", 10, 1, 200, 1)
                        .help("Initial pot contribution per player each round."))
                .add(Setting.slider("buy_in", "Buy-In", 1000, 100, 100000, 100)
                        .help("Starting chip stack per player."))
                .add(Setting.picker("turn_timer", "Turn Timer (s)", 30,
                        Arrays.asList(0, 10, 20, 30))
                        .help("0 = no timer."))
                .add(Setting.slider("round_count", "Rounds", 0, 0, 50, 1)
                        .help("0 = play until host stops."))
                .add(Setting.listEditor("variation_queue", "Variation Queue",
                        variationIds, variationIds)
                        .help("Cycles in this order."))
                .add(Setting.picker("variation_cadence", "Cycle every (rounds)", 2,
                        Arrays.asList(1, 2, 3, 5)))
                .build();

        List<String> reference = Arrays.asList(
                "Trail (Three of a Kind) — strongest",
                "Pure Sequence (Straight Flush)",
                "Sequence (Straight)",
                "Color (Flush)",
                "Pair",
                "High Card — weakest"
        );

        List<GameDefinition.HelpSlide> help = Arrays.asList(
                new GameDefinition.HelpSlide("Boot",
                        "Every player puts in the boot at the start of each round."),
                new GameDefinition.HelpSlide("Blind vs Seen",
                        "Until you tap to view, you're blind and pay 1× the stake. " +
                        "Seen players pay 2× — but you've seen your cards."),
                new GameDefinition.HelpSlide("Chaal / Raise / Fold",
                        "On your turn: match the stake (Chaal), raise it, or fold (Pack)."),
                new GameDefinition.HelpSlide("Show",
                        "When two players remain, either can call Show to settle.")
        );

        return new GameDefinition(
                GameType.TEEN_PATTI,
                schema,
                TeenPattiEngine::new,
                TeenPattiActivity.class,
                reference,
                help
        );
    }
}
