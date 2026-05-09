package com.example.cardclash.ui.room;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.example.cardclash.core.models.RoomConfig;
import com.example.cardclash.core.settings.RuleSchema;
import com.example.cardclash.core.settings.Setting;
import com.example.cardclash.core.theme.Theme;
import com.example.cardclash.core.theme.ThemePrefs;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the host-facing pre-game settings UI directly from a RuleSchema. Adding
 * a setting to a game's schema causes the row to appear here automatically with
 * the right control type — that's the data-driven settings guarantee.
 *
 * <p>Selection state on int/enum pickers uses fill + border swap, never alpha
 * alone — testing which option is active must be unambiguous at a glance.
 */
public final class SchemaRenderer {

    private SchemaRenderer() {}

    public static void renderInto(LinearLayout container, RuleSchema schema, RoomConfig config) {
        Context ctx = container.getContext();
        container.removeAllViews();
        for (Setting s : schema.settings) {
            container.addView(rowFor(ctx, s, config));
        }
    }

    private static View rowFor(Context ctx, Setting s, RoomConfig cfg) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(ctx, 12);
        row.setPadding(0, pad, 0, pad);

        TextView label = new TextView(ctx);
        label.setText(s.label);
        label.setTextSize(14);
        label.setTypeface(label.getTypeface(), Typeface.BOLD);
        label.setTextColor(ThemePrefs.activeTheme(ctx).colorTextPrimary());
        row.addView(label);

        if (s.help != null) {
            TextView help = new TextView(ctx);
            help.setText(s.help);
            help.setTextSize(12);
            help.setTextColor(ThemePrefs.activeTheme(ctx).colorTextSecondary());
            help.setPadding(0, dp(ctx, 2), 0, dp(ctx, 4));
            row.addView(help);
        }

        switch (s.type) {
            case INT_SLIDER:    row.addView(slider(ctx, s, cfg)); break;
            case INT_PICKER:    row.addView(intPicker(ctx, s, cfg)); break;
            case TOGGLE:        row.addView(toggle(ctx, s, cfg)); break;
            case ENUM_PICKER:   row.addView(enumPicker(ctx, s, cfg)); break;
            case LIST_EDITOR:   row.addView(listEditor(ctx, s, cfg)); break;
        }
        return row;
    }

    private static View slider(Context ctx, Setting s, RoomConfig cfg) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);

        long current = cfg.longVal(s.key, ((Number) s.defaultValue).longValue());
        TextView val = new TextView(ctx);
        val.setText(String.valueOf(current));
        val.setMinWidth(dp(ctx, 56));
        val.setTextSize(15);
        val.setTextColor(ThemePrefs.activeTheme(ctx).colorTextPrimary());
        val.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);

        SeekBar bar = new SeekBar(ctx);
        bar.setMax((s.max - s.min) / Math.max(1, s.step));
        bar.setProgress((int) ((current - s.min) / Math.max(1, s.step)));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int p, boolean fromUser) {
                long v = (long) s.min + (long) p * s.step;
                val.setText(String.valueOf(v));
                cfg.put(s.key, v);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        box.addView(bar, blp);
        box.addView(val);
        cfg.put(s.key, current);
        return box;
    }

    private static View intPicker(Context ctx, Setting s, RoomConfig cfg) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.HORIZONTAL);
        Object curRaw = cfg.values.get(s.key);
        if (curRaw == null) curRaw = s.defaultValue;
        final int[] active = { ((Number) curRaw).intValue() };
        cfg.put(s.key, active[0]);

        for (Integer opt : s.intOptions) {
            Button b = pickerButton(ctx, opt == 0 ? "Off" : String.valueOf(opt));
            b.setTag(opt);
            b.setOnClickListener(v -> {
                cfg.put(s.key, opt);
                active[0] = opt;
                refreshPickerSelection(box, opt);
            });
            box.addView(b, pickerLp(ctx));
        }
        refreshPickerSelection(box, active[0]);
        return box;
    }

    private static View enumPicker(Context ctx, Setting s, RoomConfig cfg) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.HORIZONTAL);
        String cur = cfg.strVal(s.key, (String) s.defaultValue);
        cfg.put(s.key, cur);

        for (String opt : s.options) {
            Button b = pickerButton(ctx, opt);
            b.setTag(opt);
            b.setOnClickListener(v -> {
                cfg.put(s.key, opt);
                refreshPickerSelection(box, opt);
            });
            box.addView(b, pickerLp(ctx));
        }
        refreshPickerSelection(box, cur);
        return box;
    }

    private static Button pickerButton(Context ctx, String text) {
        Button b = new Button(ctx);
        b.setText(text);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(dp(ctx, 44));
        b.setMinimumHeight(dp(ctx, 44));
        b.setPadding(dp(ctx, 14), dp(ctx, 8), dp(ctx, 14), dp(ctx, 8));
        b.setStateListAnimator(null);
        b.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        return b;
    }

    private static LinearLayout.LayoutParams pickerLp(Context ctx) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, dp(ctx, 8), 0);
        return lp;
    }

    /** Apply fill + border + text-color swap across a row of picker buttons. */
    private static void refreshPickerSelection(LinearLayout box, Object selected) {
        Theme t = ThemePrefs.activeTheme(box.getContext());
        for (int i = 0; i < box.getChildCount(); i++) {
            View v = box.getChildAt(i);
            if (!(v instanceof Button)) continue;
            Button b = (Button) v;
            boolean on = b.getTag() != null && b.getTag().equals(selected);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setColor(on ? t.colorTextPrimary() : t.colorBackground());
            bg.setStroke(dp(box.getContext(), 2), t.colorTextPrimary());
            b.setBackground(bg);
            b.setTextColor(on ? t.colorBackground() : t.colorTextPrimary());
            b.setAlpha(1f);
        }
    }

    private static View toggle(Context ctx, Setting s, RoomConfig cfg) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);

        boolean cur = cfg.boolVal(s.key, (Boolean) s.defaultValue);
        cfg.put(s.key, cur);

        Theme t = ThemePrefs.activeTheme(ctx);
        TextView state = new TextView(ctx);
        state.setTextSize(14);
        state.setTypeface(state.getTypeface(), Typeface.BOLD);
        state.setMinWidth(dp(ctx, 60));
        state.setPadding(dp(ctx, 12), dp(ctx, 6), dp(ctx, 12), dp(ctx, 6));

        Switch sw = new Switch(ctx);
        sw.setChecked(cur);
        sw.setOnCheckedChangeListener((b, checked) -> {
            cfg.put(s.key, checked);
            applyToggleState(state, checked, t);
        });
        applyToggleState(state, cur, t);

        box.addView(sw);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.setMargins(dp(ctx, 12), 0, 0, 0);
        box.addView(state, slp);
        return box;
    }

    private static void applyToggleState(TextView state, boolean on, Theme t) {
        state.setText(on ? "ON" : "OFF");
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setColor(on ? t.colorTextPrimary() : t.colorBackground());
        bg.setStroke(dp(state.getContext(), 2), t.colorTextPrimary());
        state.setBackground(bg);
        state.setTextColor(on ? t.colorBackground() : t.colorTextPrimary());
    }

    @SuppressWarnings("unchecked")
    private static View listEditor(Context ctx, Setting s, RoomConfig cfg) {
        Theme t = ThemePrefs.activeTheme(ctx);
        Object cur = cfg.values.get(s.key);
        if (cur == null) cur = s.defaultValue;
        final List<String> ids = new ArrayList<>((List<String>) cur);
        cfg.put(s.key, ids);

        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);

        renderListRows(ctx, col, ids, cfg, s, t);
        return col;
    }

    private static void renderListRows(Context ctx, LinearLayout col, List<String> ids,
                                       RoomConfig cfg, Setting s, Theme t) {
        col.removeAllViews();
        for (int i = 0; i < ids.size(); i++) {
            final int idx = i;
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(ctx, 8), dp(ctx, 6), dp(ctx, 8), dp(ctx, 6));
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setColor(t.colorBackground());
            bg.setStroke(dp(ctx, 2), t.colorTextPrimary());
            row.setBackground(bg);

            TextView pos = new TextView(ctx);
            pos.setText((idx + 1) + ".");
            pos.setTextSize(13);
            pos.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            pos.setTextColor(t.colorTextPrimary());
            pos.setMinWidth(dp(ctx, 28));
            row.addView(pos);

            TextView name = new TextView(ctx);
            name.setText(ids.get(idx));
            name.setTextSize(14);
            name.setTypeface(name.getTypeface(), Typeface.BOLD);
            name.setTextColor(t.colorTextPrimary());
            LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            row.addView(name, nlp);

            Button up = arrowButton(ctx, "▲", t);
            up.setEnabled(idx > 0);
            up.setOnClickListener(v -> {
                if (idx > 0) {
                    String tmp = ids.remove(idx);
                    ids.add(idx - 1, tmp);
                    cfg.put(s.key, ids);
                    renderListRows(ctx, col, ids, cfg, s, t);
                }
            });
            row.addView(up);

            Button down = arrowButton(ctx, "▼", t);
            down.setEnabled(idx < ids.size() - 1);
            down.setOnClickListener(v -> {
                if (idx < ids.size() - 1) {
                    String tmp = ids.remove(idx);
                    ids.add(idx + 1, tmp);
                    cfg.put(s.key, ids);
                    renderListRows(ctx, col, ids, cfg, s, t);
                }
            });
            row.addView(down);

            Button rm = arrowButton(ctx, "✕", t);
            rm.setEnabled(ids.size() > 1);
            rm.setOnClickListener(v -> {
                ids.remove(idx);
                cfg.put(s.key, ids);
                renderListRows(ctx, col, ids, cfg, s, t);
            });
            row.addView(rm);

            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rlp.bottomMargin = dp(ctx, 6);
            col.addView(row, rlp);
        }

        // Add row: lets host re-add removed presets
        List<String> remaining = new ArrayList<>(s.options);
        remaining.removeAll(ids);
        if (!remaining.isEmpty()) {
            LinearLayout addRow = new LinearLayout(ctx);
            addRow.setOrientation(LinearLayout.HORIZONTAL);
            addRow.setPadding(0, dp(ctx, 4), 0, 0);
            TextView addLabel = new TextView(ctx);
            addLabel.setText("Add:");
            addLabel.setTextColor(t.colorTextSecondary());
            addLabel.setTextSize(12);
            addLabel.setPadding(0, dp(ctx, 10), dp(ctx, 8), 0);
            addRow.addView(addLabel);
            LinearLayout chips = new LinearLayout(ctx);
            chips.setOrientation(LinearLayout.HORIZONTAL);
            for (String opt : remaining) {
                Button b = arrowButton(ctx, "+ " + opt, t);
                b.setEnabled(true);
                b.setOnClickListener(v -> {
                    ids.add(opt);
                    cfg.put(s.key, ids);
                    renderListRows(ctx, col, ids, cfg, s, t);
                });
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                clp.setMargins(0, 0, dp(ctx, 6), dp(ctx, 6));
                chips.addView(b, clp);
            }
            addRow.addView(chips);
            col.addView(addRow);
        }
    }

    private static Button arrowButton(Context ctx, String text, Theme t) {
        Button b = new Button(ctx);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setMinWidth(0);
        b.setMinimumWidth(dp(ctx, 36));
        b.setMinHeight(dp(ctx, 36));
        b.setMinimumHeight(dp(ctx, 36));
        b.setPadding(dp(ctx, 8), 0, dp(ctx, 8), 0);
        b.setStateListAnimator(null);
        b.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setColor(t.colorTextPrimary());
        bg.setStroke(dp(ctx, 2), t.colorTextPrimary());
        b.setBackground(bg);
        b.setTextColor(t.colorBackground());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, dp(ctx, 4), 0);
        b.setLayoutParams(lp);
        return b;
    }

    private static int dp(Context c, int v) {
        return (int) (v * c.getResources().getDisplayMetrics().density);
    }
}
