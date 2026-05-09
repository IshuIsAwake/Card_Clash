package com.example.cardclash.games;

import android.app.Activity;

import com.example.cardclash.core.engine.GameEngine;
import com.example.cardclash.core.models.GameType;
import com.example.cardclash.core.settings.RuleSchema;

import java.util.List;

/** Bundles everything the rest of the app needs to know about a game.
 *  Adding a game = create one of these and register it. */
public class GameDefinition {

    public final GameType type;
    public final RuleSchema ruleSchema;
    public final EngineFactory engineFactory;
    public final Class<? extends Activity> tableActivity;
    public final List<String> handRankingsReference;
    public final List<HelpSlide> helpSlides;

    public GameDefinition(GameType type, RuleSchema schema,
                          EngineFactory factory,
                          Class<? extends Activity> tableActivity,
                          List<String> handRankingsReference,
                          List<HelpSlide> helpSlides) {
        this.type = type;
        this.ruleSchema = schema;
        this.engineFactory = factory;
        this.tableActivity = tableActivity;
        this.handRankingsReference = handRankingsReference;
        this.helpSlides = helpSlides;
    }

    public interface EngineFactory {
        GameEngine create();
    }

    public static class HelpSlide {
        public final String title;
        public final String body;
        public HelpSlide(String title, String body) { this.title = title; this.body = body; }
    }
}
