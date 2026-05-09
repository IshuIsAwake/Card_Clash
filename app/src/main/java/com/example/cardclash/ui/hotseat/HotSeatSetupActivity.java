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
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.cardclash.R;
import com.example.cardclash.core.hotseat.HotSeatConfig;
import com.example.cardclash.core.models.GameType;
import com.example.cardclash.core.models.Player;
import com.example.cardclash.core.theme.Theme;
import com.example.cardclash.core.theme.ThemePrefs;
import com.example.cardclash.games.GameDefinition;
import com.example.cardclash.games.GamesRegistry;
import com.example.cardclash.ui.common.ThemedActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pick game, player count, names. No Firebase. */
public class HotSeatSetupActivity extends ThemedActivity {

    private static final String[] DEFAULT_NAMES = {"Player 1", "Player 2", "Player 3", "Player 4", "Player 5", "Player 6"};

    private GameType selectedGame = GameType.BLUFF;
    private int playerCount = 3;
    private String callRule = "OPEN_CALL";
    private boolean winnerKeepsTurn = true;
    private final List<EditText> nameEdits = new ArrayList<>();

    @Override protected void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_hotseat_setup);

        renderGamePicker();
        renderCountPicker();
        renderCallRulePicker();
        renderWinnerKeepsTurn();
        rebuildNameRows();

        findViewById(R.id.btnStart).setOnClickListener(v -> start());
    }

    private void renderGamePicker() {
        LinearLayout box = findViewById(R.id.gamePicker);
        box.removeAllViews();
        // Bluff and Teen Patti only — Poker not built.
        for (GameType g : Arrays.asList(GameType.BLUFF, GameType.TEEN_PATTI)) {
            Button b = pickerButton(g.displayName);
            b.setTag(g);
            b.setOnClickListener(v -> {
                selectedGame = g;
                refreshSelection(box, g);
                findViewById(R.id.bluffRulePanel)
                        .setVisibility(g == GameType.BLUFF ? View.VISIBLE : View.GONE);
                clampPlayerCount();
                rebuildNameRows();
            });
            box.addView(b, picklp());
        }
        refreshSelection(box, selectedGame);
        findViewById(R.id.bluffRulePanel)
                .setVisibility(selectedGame == GameType.BLUFF ? View.VISIBLE : View.GONE);
    }

    private void renderCallRulePicker() {
        LinearLayout box = findViewById(R.id.callRulePicker);
        box.removeAllViews();
        for (String[] pair : new String[][] {{"OPEN_CALL", "Open Call"}, {"NEXT_ONLY", "Next-Only"}}) {
            Button b = pickerButton(pair[1]);
            b.setTag(pair[0]);
            b.setOnClickListener(v -> {
                callRule = pair[0];
                refreshSelection(box, callRule);
            });
            box.addView(b, picklp());
        }
        refreshSelection(box, callRule);
    }

    private void renderWinnerKeepsTurn() {
        LinearLayout box = findViewById(R.id.winnerTurnPicker);
        box.removeAllViews();
        for (Object[] pair : new Object[][] {{Boolean.TRUE, "On"}, {Boolean.FALSE, "Off"}}) {
            Button b = pickerButton((String) pair[1]);
            b.setTag(pair[0]);
            b.setOnClickListener(v -> {
                winnerKeepsTurn = (Boolean) pair[0];
                refreshSelection(box, winnerKeepsTurn);
            });
            box.addView(b, picklp());
        }
        refreshSelection(box, winnerKeepsTurn);
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

    private void rebuildNameRows() {
        Theme t = ThemePrefs.activeTheme(this);
        LinearLayout rows = findViewById(R.id.nameRows);
        // Preserve existing name input where possible
        List<String> existing = new ArrayList<>();
        for (EditText e : nameEdits) existing.add(e.getText().toString());
        rows.removeAllViews();
        nameEdits.clear();
        for (int i = 0; i < playerCount; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(6), 0, dp(6));

            TextView seat = new TextView(this);
            seat.setText("P" + (i + 1));
            seat.setTextSize(14);
            seat.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            seat.setTextColor(t.colorTextPrimary());
            seat.setMinWidth(dp(40));
            row.addView(seat);

            EditText e = new EditText(this);
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            e.setHint("Name");
            e.setHintTextColor(t.colorTextSecondary());
            e.setTextColor(t.colorTextPrimary());
            e.setTextSize(15);
            e.setSingleLine(true);
            String preset = (i < existing.size() && !existing.get(i).isEmpty())
                    ? existing.get(i) : DEFAULT_NAMES[i];
            e.setText(preset);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setColor(t.colorBackground());
            bg.setStroke(dp(2), t.colorTextPrimary());
            e.setBackground(bg);
            e.setPadding(dp(10), dp(8), dp(10), dp(8));
            LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            elp.setMargins(dp(8), 0, 0, 0);
            row.addView(e, elp);
            nameEdits.add(e);
            rows.addView(row);
        }
    }

    private Button pickerButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(dp(44));
        b.setMinimumHeight(dp(44));
        b.setPadding(dp(14), dp(8), dp(14), dp(8));
        b.setStateListAnimator(null);
        b.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
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
            bg.setColor(on ? t.colorTextPrimary() : t.colorBackground());
            bg.setStroke(dp(2), t.colorTextPrimary());
            b.setBackground(bg);
            b.setTextColor(on ? t.colorBackground() : t.colorTextPrimary());
        }
    }

    private void start() {
        List<Player> roster = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            String n = nameEdits.get(i).getText().toString().trim();
            if (n.isEmpty()) n = DEFAULT_NAMES[i];
            roster.add(new Player("p" + (i + 1), n, i, 1000, i == 0));
        }
        Map<String, Object> overrides = new HashMap<>();
        if (selectedGame == GameType.BLUFF) {
            overrides.put("call_rule", callRule);
            overrides.put("winner_keeps_turn", winnerKeepsTurn);
        }
        HotSeatConfig.set(new HotSeatConfig(selectedGame, roster, overrides));
        GameDefinition def = GamesRegistry.get(selectedGame);
        Intent i = new Intent(this, def.tableActivity);
        i.putExtra("hotseat", true);
        startActivity(i);
        finish();
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }
}
