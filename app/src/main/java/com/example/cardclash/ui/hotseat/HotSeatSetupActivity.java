package com.example.cardclash.ui.hotseat;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.example.cardclash.R;
import com.example.cardclash.core.hotseat.HotSeatConfig;
import com.example.cardclash.core.models.GameType;
import com.example.cardclash.core.models.Player;
import com.example.cardclash.core.models.RoomConfig;
import com.example.cardclash.core.theme.Theme;
import com.example.cardclash.core.theme.ThemePrefs;
import com.example.cardclash.games.GameDefinition;
import com.example.cardclash.games.GamesRegistry;
import com.example.cardclash.ui.common.ThemedActivity;
import com.example.cardclash.ui.room.SchemaRenderer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HotSeatSetupActivity extends ThemedActivity {

    private static final String[] DEFAULT_NAMES = {"Player 1", "Player 2", "Player 3", "Player 4", "Player 5", "Player 6"};

    private GameType selectedGame = GameType.BLUFF;
    private int playerCount = 3;
    private RoomConfig schemaCfg;
    private final List<EditText> nameEdits = new ArrayList<>();

    @Override protected void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_hotseat_setup);
        applyThemeToStaticText();
        renderGamePicker();
        renderCountPicker();
        rebuildNameRows();
        renderSchema();
        styleStartButton();
        refreshSubtitle();
        refreshStartLabel();

        findViewById(R.id.btnStart).setOnClickListener(v -> start());
    }

    private void applyThemeToStaticText() {
        Theme t = ThemePrefs.activeTheme(this);
        Typeface display = safeFont(t.fontDisplay());
        Typeface heading = safeFont(t.fontHeading());
        applyFont(R.id.topbarTitle, display, t.colorFg1());
        applyFont(R.id.topbarDot, heading, t.colorFg3());
        applyFont(R.id.topbarSubtitle, heading, t.colorFg2());
        applyFont(R.id.heroTitle, display, t.colorFg1());
        applyFont(R.id.rulesHeader, heading, t.colorFg2());
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

    private void applyFont(int id, Typeface tf, int color) {
        View v = findViewById(id);
        if (v instanceof TextView) {
            ((TextView) v).setTypeface(tf, Typeface.BOLD);
            ((TextView) v).setTextColor(color);
        }
    }

    private void renderGamePicker() {
        LinearLayout box = findViewById(R.id.gamePicker);
        box.removeAllViews();
        for (GameType g : Arrays.asList(GameType.BLUFF, GameType.TEEN_PATTI)) {
            Button b = pickerButton(g.displayName);
            b.setTag(g);
            b.setOnClickListener(v -> {
                if (selectedGame == g) return;
                selectedGame = g;
                refreshSelection(box, g);
                clampPlayerCount();
                rebuildNameRows();
                renderSchema();
                refreshSubtitle();
            });
            box.addView(b, picklp());
        }
        refreshSelection(box, selectedGame);
    }

    private void renderCountPicker() {
        LinearLayout box = findViewById(R.id.countPicker);
        box.removeAllViews();
        int min = selectedGame.minPlayers;
        int max = Math.min(selectedGame.maxPlayers, 6);
        if (playerCount < min) playerCount = min;
        if (playerCount > max) playerCount = max;
        for (int i = min; i <= max; i++) {
            final int n = i;
            Button b = pickerButton(String.valueOf(i));
            b.setTag(i);
            b.setOnClickListener(v -> {
                playerCount = n;
                refreshSelection(box, n);
                rebuildNameRows();
                refreshStartLabel();
            });
            box.addView(b, picklp());
        }
        refreshSelection(box, playerCount);
    }

    private void clampPlayerCount() {
        int min = selectedGame.minPlayers;
        int max = Math.min(selectedGame.maxPlayers, 6);
        if (playerCount < min) playerCount = min;
        if (playerCount > max) playerCount = max;
        renderCountPicker();
    }

    private void renderSchema() {
        GameDefinition def = GamesRegistry.get(selectedGame);
        schemaCfg = new RoomConfig(selectedGame);
        def.ruleSchema.applyDefaults(schemaCfg);
        LinearLayout container = findViewById(R.id.schemaContainer);
        SchemaRenderer.renderInto(container, def.ruleSchema, schemaCfg);
    }

    private void rebuildNameRows() {
        Theme t = ThemePrefs.activeTheme(this);
        Typeface body = safeFont(t.fontBody());
        Typeface mono = safeFont(t.fontMono());
        LinearLayout rows = findViewById(R.id.nameRows);
        List<String> existing = new ArrayList<>();
        for (EditText e : nameEdits) existing.add(e.getText().toString());
        rows.removeAllViews();
        nameEdits.clear();
        for (int i = 0; i < playerCount; i++) {
            final int seatIdx = i;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(6), 0, dp(6));

            TextView seat = new TextView(this);
            seat.setText("P" + (i + 1));
            seat.setTextSize(13);
            seat.setTypeface(mono, Typeface.BOLD);
            seat.setTextColor(t.colorFg2());
            seat.setLetterSpacing(0.12f);
            seat.setMinWidth(dp(40));
            row.addView(seat);

            EditText e = new EditText(this);
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            e.setHint("Name");
            e.setHintTextColor(t.colorFg3());
            e.setTextColor(t.colorFg1());
            e.setTypeface(body);
            e.setTextSize(15);
            e.setSingleLine(true);
            String preset = (i < existing.size() && !existing.get(i).isEmpty())
                    ? existing.get(i) : DEFAULT_NAMES[i];
            e.setText(preset);
            e.addTextChangedListener(new SimpleWatcher(() -> { if (seatIdx == 0) refreshStartLabel(); }));
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setColor(t.colorBg());
            bg.setStroke(dp(t.borderWidthDp()), t.colorFg3());
            bg.setCornerRadius(dp((int) t.radiusBtnDp()));
            e.setBackground(bg);
            e.setPadding(dp(12), dp(10), dp(12), dp(10));
            LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            elp.setMargins(dp(8), 0, 0, 0);
            row.addView(e, elp);
            nameEdits.add(e);
            rows.addView(row);
        }
    }

    private Button pickerButton(String text) {
        Theme t = ThemePrefs.activeTheme(this);
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setMinWidth(0);
        b.setMinimumWidth(dp(48));
        b.setMinHeight(dp(44));
        b.setMinimumHeight(dp(44));
        b.setPadding(dp(14), dp(8), dp(14), dp(8));
        b.setStateListAnimator(null);
        b.setTypeface(safeFont(t.fontHeading()), Typeface.BOLD);
        return b;
    }

    private LinearLayout.LayoutParams picklp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, dp(8), 0);
        return lp;
    }

    private void refreshSelection(LinearLayout box, Object selected) {
        Theme t = ThemePrefs.activeTheme(this);
        for (int i = 0; i < box.getChildCount(); i++) {
            View v = box.getChildAt(i);
            if (!(v instanceof Button)) continue;
            Button b = (Button) v;
            boolean on = b.getTag() != null && b.getTag().equals(selected);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(dp((int) t.radiusBtnDp()));
            bg.setColor(on ? t.colorAccent() : t.colorBg());
            bg.setStroke(dp(t.borderWidthDp()), on ? t.colorAccent() : t.colorFg3());
            b.setBackground(bg);
            b.setTextColor(on ? t.colorAccentOn() : t.colorFg1());
        }
    }

    private void styleStartButton() {
        Theme t = ThemePrefs.activeTheme(this);
        Button b = findViewById(R.id.btnStart);
        b.setAllCaps(true);
        b.setLetterSpacing(0.12f);
        b.setTypeface(safeFont(t.fontHeading()), Typeface.BOLD);
        b.setTextColor(t.colorAccentOn());
        b.setStateListAnimator(null);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp((int) t.radiusBtnDp()));
        bg.setColor(t.colorAccent());
        bg.setStroke(dp(t.borderWidthDp()), t.colorAccent());
        b.setBackground(bg);
        b.setMinHeight(dp(52));
    }

    private void refreshSubtitle() {
        TextView sub = findViewById(R.id.topbarSubtitle);
        sub.setText(selectedGame.displayName.toUpperCase());
    }

    private void refreshStartLabel() {
        Button b = findViewById(R.id.btnStart);
        String first = nameEdits.isEmpty() ? DEFAULT_NAMES[0]
                : nameEdits.get(0).getText().toString().trim();
        if (first.isEmpty()) first = DEFAULT_NAMES[0];
        b.setText("DEAL  —  PASS TO " + first.toUpperCase());
    }

    private void start() {
        List<Player> roster = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            String n = nameEdits.get(i).getText().toString().trim();
            if (n.isEmpty()) n = DEFAULT_NAMES[i];
            roster.add(new Player("p" + (i + 1), n, i, 1000, i == 0));
        }
        Map<String, Object> overrides = new HashMap<>();
        if (schemaCfg != null) overrides.putAll(schemaCfg.values);
        HotSeatConfig.set(new HotSeatConfig(selectedGame, roster, overrides));
        GameDefinition def = GamesRegistry.get(selectedGame);
        Intent i = new Intent(this, def.tableActivity);
        i.putExtra("hotseat", true);
        startActivity(i);
        finish();
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    private static class SimpleWatcher implements android.text.TextWatcher {
        private final Runnable onChange;
        SimpleWatcher(Runnable r) { this.onChange = r; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(android.text.Editable s) { onChange.run(); }
    }
}
