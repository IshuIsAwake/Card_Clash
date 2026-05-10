package com.example.cardclash.ui.hotseat;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.example.cardclash.core.models.GameType;
import com.example.cardclash.core.theme.Theme;
import com.example.cardclash.core.theme.ThemePrefs;
import com.example.cardclash.games.GameDefinition;
import com.example.cardclash.games.GamesRegistry;
import com.example.cardclash.ui.common.ThemedActivity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Step 1+2 of the Pass-and-Play flow (portrait). User picks a game; tapping a
 * card expands it to a description with a SELECT button. SELECT launches
 * {@link HotSeatSetupActivity} for player count + names + rules.
 */
public class PassAndPlayPickerActivity extends ThemedActivity {

    private static final List<GameType> GAMES = Arrays.asList(
            GameType.POKER, GameType.TEEN_PATTI, GameType.BLUFF
    );

    private static final Map<GameType, String> SUBTITLES = new HashMap<>();
    private static final Map<GameType, String> DESCRIPTIONS = new HashMap<>();
    static {
        SUBTITLES.put(GameType.POKER,
                "Texas Hold'em. Two hole cards, five community.");
        SUBTITLES.put(GameType.TEEN_PATTI,
                "Three-card Indian classic. Blind or seen, chaal or pack.");
        SUBTITLES.put(GameType.BLUFF,
                "Lay cards face-down and lie about them. Call the bluff or pay.");

        DESCRIPTIONS.put(GameType.POKER,
                "Each player gets two hole cards. Small and big blinds post automatically or manually "
                        + "(your choice in rules). Three burn cards reveal the FLOP, TURN, and RIVER. "
                        + "Best 5-card hand from any combination of hole + community wins.\n\n"
                        + "Actions: FOLD · CHECK · CALL · RAISE (build a chip stack to bet) · ALL-IN. "
                        + "Side pots are computed automatically when a player goes all-in for less.\n\n"
                        + "Host can end a round early, approve mid-game buy-ins, and review the last "
                        + "three rounds of action history from the table menu.");
        DESCRIPTIONS.put(GameType.TEEN_PATTI,
                "Three cards each, dealt face-down. Players ante a 'boot' to seed the pot. "
                        + "Each turn you can SEE your cards (raises your stake) or stay BLIND (cheaper). "
                        + "Bet by Chaal-ing the current stake or RAISING it. The last unfolded player "
                        + "wins, or two players reach a SHOW to compare hands.\n\n"
                        + "Variations rotate every few rounds: Classic, AK47 (jokers wild), 1942 (random "
                        + "wild ranks), Muflis (lowest hand wins), Joker Wild.");
        DESCRIPTIONS.put(GameType.BLUFF,
                "All cards are dealt out. Players take turns playing 1–4 cards face-down, claiming "
                        + "they are a specific rank. The next player can either accept the claim and "
                        + "play their own, or call BLUFF. If the bluff was called and the cards don't "
                        + "match the claim, the bluffer takes the discard pile. If the call was wrong, "
                        + "the caller takes the pile. First to empty their hand wins.");
    }

    @Override protected void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        Theme t = ThemePrefs.activeTheme(this);

        ScrollView root = new ScrollView(this);
        root.setBackgroundColor(t.colorBg());
        setContentView(root);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(20), dp(28), dp(20), dp(28));
        root.addView(col, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Header
        TextView eyebrow = label("PASS AND PLAY", 13, t.colorFg2(), t.fontHeading(), 0.18f);
        col.addView(eyebrow);

        TextView title = label("Pick a game", 28, t.colorFg1(), t.fontDisplay(), 0.0f);
        title.setTypeface(safeFont(t.fontDisplay()), Typeface.BOLD);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = dp(2);
        tlp.bottomMargin = dp(20);
        col.addView(title, tlp);

        // Cards
        for (GameType g : GAMES) col.addView(buildCard(t, g));
    }

    private View buildCard(Theme t, GameType g) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(t.colorSurface());
        bg.setStroke(dp(t.borderWidthDp()), t.colorFg3());
        bg.setCornerRadius(dp((int) t.radiusSurfaceDp() + 4));
        card.setBackground(bg);

        // Header row
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView name = new TextView(this);
        name.setText(g.displayName.toUpperCase());
        name.setTextSize(18);
        name.setTextColor(t.colorFg1());
        name.setLetterSpacing(0.10f);
        name.setTypeface(safeFont(t.fontHeading()), Typeface.BOLD);
        LinearLayout.LayoutParams nLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        headerRow.addView(name, nLp);

        TextView chevron = new TextView(this);
        chevron.setText("▾");
        chevron.setTextSize(18);
        chevron.setTextColor(t.colorFg2());
        chevron.setTypeface(chevron.getTypeface(), Typeface.BOLD);
        headerRow.addView(chevron);
        card.addView(headerRow);

        TextView subtitle = new TextView(this);
        subtitle.setText(SUBTITLES.getOrDefault(g, ""));
        subtitle.setTextSize(12);
        subtitle.setTextColor(t.colorFg2());
        subtitle.setTypeface(safeFont(t.fontBody()));
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sLp.topMargin = dp(4);
        card.addView(subtitle, sLp);

        // Expandable section: description + SELECT button
        LinearLayout expanded = new LinearLayout(this);
        expanded.setOrientation(LinearLayout.VERTICAL);
        expanded.setVisibility(View.GONE);
        LinearLayout.LayoutParams eLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        eLp.topMargin = dp(12);
        card.addView(expanded, eLp);

        // separator
        View sep = new View(this);
        sep.setBackgroundColor(t.colorFg3());
        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        spLp.bottomMargin = dp(10);
        expanded.addView(sep, spLp);

        TextView desc = new TextView(this);
        desc.setText(DESCRIPTIONS.getOrDefault(g, ""));
        desc.setTextSize(13);
        desc.setLineSpacing(dp(2), 1f);
        desc.setTextColor(t.colorFg1());
        desc.setTypeface(safeFont(t.fontBody()));
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        dLp.bottomMargin = dp(12);
        expanded.addView(desc, dLp);

        // hand rankings reference (compact)
        GameDefinition def = GamesRegistry.get(g);
        if (def != null && !def.handRankingsReference.isEmpty()) {
            TextView refTitle = new TextView(this);
            refTitle.setText("HAND RANKINGS");
            refTitle.setTextSize(10);
            refTitle.setLetterSpacing(0.20f);
            refTitle.setTextColor(t.colorFg3());
            refTitle.setTypeface(safeFont(t.fontHeading()), Typeface.BOLD);
            expanded.addView(refTitle);

            TextView refList = new TextView(this);
            refList.setText(TextUtils.join("  ·  ", def.handRankingsReference));
            refList.setTextSize(11);
            refList.setTextColor(t.colorFg2());
            refList.setTypeface(safeFont(t.fontMono()));
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rlp.bottomMargin = dp(12);
            expanded.addView(refList, rlp);
        }

        Button select = new Button(this);
        select.setText("SELECT  " + g.displayName.toUpperCase());
        select.setAllCaps(true);
        select.setLetterSpacing(0.12f);
        select.setStateListAnimator(null);
        select.setTextColor(t.colorAccentOn());
        select.setTypeface(safeFont(t.fontHeading()), Typeface.BOLD);
        GradientDrawable selectBg = new GradientDrawable();
        selectBg.setColor(t.colorAccent());
        selectBg.setStroke(dp(t.borderWidthDp()), t.colorAccent());
        selectBg.setCornerRadius(dp((int) t.radiusBtnDp() + 2));
        select.setBackground(selectBg);
        select.setMinHeight(dp(48));
        select.setOnClickListener(v -> {
            Intent i = new Intent(this, HotSeatSetupActivity.class);
            i.putExtra(HotSeatSetupActivity.EXTRA_PRESELECTED_GAME, g.name());
            startActivity(i);
        });
        expanded.addView(select, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Toggle expand/collapse
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> {
            boolean visible = expanded.getVisibility() == View.VISIBLE;
            expanded.setVisibility(visible ? View.GONE : View.VISIBLE);
            chevron.animate().rotation(visible ? 0f : 180f).setDuration(160).start();
        });

        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.bottomMargin = dp(12);
        card.setLayoutParams(clp);
        return card;
    }

    private TextView label(String text, int sp, int color, int font, float spacing) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setLetterSpacing(spacing);
        v.setTypeface(safeFont(font), Typeface.BOLD);
        return v;
    }

    private Typeface safeFont(int fontRes) {
        if (fontRes == 0) return Typeface.DEFAULT;
        try {
            Typeface tf = ResourcesCompat.getFont(this, fontRes);
            return tf != null ? tf : Typeface.DEFAULT;
        } catch (Throwable ignored) {
            return Typeface.DEFAULT;
        }
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }
}
