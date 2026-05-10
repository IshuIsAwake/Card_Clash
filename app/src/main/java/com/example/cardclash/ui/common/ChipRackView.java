package com.example.cardclash.ui.common;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.cardclash.core.theme.Theme;
import com.example.cardclash.core.theme.ThemePrefs;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Visual stack of denominated chips. Tournament-style denominations are 25 / 100
 * / 500 / 1000 / 5000. Each denom renders as a coloured disc with a count label.
 *
 * <p>If the active theme returns the same drawable for every denomination, we
 * fall back to tinting a programmatic disc by denom-specific colour so the chip
 * tray is still readable.
 */
public class ChipRackView extends LinearLayout {

    public static final long[] DENOMS = { 25L, 100L, 500L, 1000L, 5000L };
    private static final int[] DENOM_COLORS = {
            0xFF5DBE6B, // 25 — green
            0xFF4D9DE0, // 100 — blue
            0xFFE15554, // 500 — red
            0xFF2A2A2A, // 1000 — black
            0xFFE9C46A  // 5000 — gold
    };

    private TextView totalLabel;

    public ChipRackView(Context context) { this(context, null); }
    public ChipRackView(Context context, @Nullable AttributeSet attrs) { this(context, attrs, 0); }
    public ChipRackView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
    }

    /** Render the chip breakdown of {@code amount}. Greedy: largest denom first. */
    public void setAmount(long amount) {
        Theme t = ThemePrefs.activeTheme(getContext());
        removeAllViews();

        Map<Long, Integer> breakdown = breakdown(amount);

        for (int i = DENOMS.length - 1; i >= 0; i--) {
            long denom = DENOMS[i];
            int count = breakdown.getOrDefault(denom, 0);
            if (count == 0) continue;
            addView(buildChipColumn(t, denom, DENOM_COLORS[i], count));
        }

        totalLabel = new TextView(getContext());
        totalLabel.setText(formatAmount(amount));
        totalLabel.setTextColor(t.colorFg1());
        totalLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        totalLabel.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(8), 0, 0, 0);
        addView(totalLabel, lp);
    }

    private View buildChipColumn(Theme t, long denom, int colour, int count) {
        LinearLayout col = new LinearLayout(getContext());
        col.setOrientation(VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        LayoutParams clp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        clp.setMargins(0, 0, dp(6), 0);
        col.setLayoutParams(clp);

        // chip disc — try theme drawable, fall back to programmatic tinted oval
        View disc = new View(getContext());
        int tile = dp(22);
        int dDr = t.chipBg((int) denom);
        if (dDr != 0) {
            disc.setBackgroundResource(dDr);
            // additive tint by colour for readability across themes
            disc.getBackground().setTint(colour);
        } else {
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(colour);
            g.setStroke(dp(t.borderWidthDp()), Color.WHITE);
            disc.setBackground(g);
        }
        col.addView(disc, new LayoutParams(tile, tile));

        TextView lbl = new TextView(getContext());
        lbl.setText(String.valueOf(denom) + (count > 1 ? "  ×" + count : ""));
        lbl.setTextColor(t.colorFg2());
        lbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        lbl.setTypeface(Typeface.MONOSPACE);
        lbl.setLetterSpacing(0.04f);
        col.addView(lbl);

        return col;
    }

    /** Greedy denomination breakdown (largest first). */
    public static Map<Long, Integer> breakdown(long amount) {
        Map<Long, Integer> out = new LinkedHashMap<>();
        long left = amount;
        for (int i = DENOMS.length - 1; i >= 0; i--) {
            long d = DENOMS[i];
            int n = (int) (left / d);
            if (n > 0) {
                out.put(d, n);
                left -= (long) n * d;
            }
        }
        return out;
    }

    public static String formatAmount(long n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 10_000) return (n / 1000) + "k";
        return String.valueOf(n);
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }
}
