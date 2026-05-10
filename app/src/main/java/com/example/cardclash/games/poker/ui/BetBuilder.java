package com.example.cardclash.games.poker.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.cardclash.core.theme.Theme;
import com.example.cardclash.core.theme.ThemePrefs;
import com.example.cardclash.ui.common.ChipRackView;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-row chip-tap raise builder. Each denomination has its own row with [-]
 * count [+] controls; quick-fills snap to common targets; CANCEL / RAISE close
 * the dialog. Designed to live inside an {@link android.app.AlertDialog} so the
 * commit buttons are always visible regardless of felt size.
 */
public class BetBuilder extends LinearLayout {

    public interface OnCommitListener { void onCommit(long amount); }
    public interface OnCancelListener { void onCancel(); }

    private final Map<Long, Integer> staged = new LinkedHashMap<>();
    private long minimum, owedToCall, potForQuick, ownStack;
    private TextView totalText, hintText;
    private final TextView[] countLabels = new TextView[ChipRackView.DENOMS.length];
    private OnCommitListener onCommit;
    private OnCancelListener onCancel;

    public BetBuilder(Context ctx) { this(ctx, null); }
    public BetBuilder(Context ctx, @Nullable AttributeSet attrs) { this(ctx, attrs, 0); }
    public BetBuilder(Context ctx, @Nullable AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle);
        setOrientation(VERTICAL);
        build();
    }

    public void configure(long minimum, long owedToCall, long potTotal, long ownStack) {
        this.minimum = minimum;
        this.owedToCall = owedToCall;
        this.potForQuick = potTotal;
        this.ownStack = ownStack;
        staged.clear();
        renderTotals();
        renderCounts();
    }

    public void setOnCommitListener(OnCommitListener l) { this.onCommit = l; }
    public void setOnCancelListener(OnCancelListener l) { this.onCancel = l; }

    private void build() {
        Theme t = ThemePrefs.activeTheme(getContext());
        int pad = dp(14);
        setPadding(pad, dp(14), pad, dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(t.colorSurface());
        bg.setStroke(dp(t.borderWidthDp()), t.colorAccent());
        bg.setCornerRadius(dp(10));
        setBackground(bg);

        TextView eyebrow = new TextView(getContext());
        eyebrow.setText("RAISE TO ADD");
        eyebrow.setLetterSpacing(0.20f);
        eyebrow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        eyebrow.setTextColor(t.colorFg2());
        eyebrow.setTypeface(eyebrow.getTypeface(), Typeface.BOLD);
        addView(eyebrow);

        totalText = new TextView(getContext());
        totalText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        totalText.setTextColor(t.colorFg1());
        totalText.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        addView(totalText);

        hintText = new TextView(getContext());
        hintText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        hintText.setTextColor(t.colorFg3());
        hintText.setLetterSpacing(0.10f);
        hintText.setTypeface(Typeface.MONOSPACE);
        LayoutParams hLp = lpMatch();
        hLp.bottomMargin = dp(10);
        addView(hintText, hLp);

        // Denomination rows
        for (int i = 0; i < ChipRackView.DENOMS.length; i++) {
            addView(buildDenomRow(t, i));
        }

        // Quick fill row
        LinearLayout quick = new LinearLayout(getContext());
        quick.setOrientation(HORIZONTAL);
        LayoutParams qlp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        qlp.topMargin = dp(8);
        qlp.bottomMargin = dp(10);
        addView(quick, qlp);
        quick.addView(quickButton(t, "MIN",    v -> setStaged(minimum)));
        quick.addView(quickButton(t, "POT",    v -> setStaged(owedToCall + potForQuick)));
        quick.addView(quickButton(t, "ALL-IN", v -> setStaged(ownStack)));
        quick.addView(quickButton(t, "CLEAR",  v -> { staged.clear(); renderTotals(); renderCounts(); }));

    }

    /** Total chips currently staged in the tray. */
    public long getStagedTotal() { return stagedTotal(); }
    public long getMinimum() { return minimum; }
    public long getOwnStack() { return ownStack; }

    private LinearLayout buildDenomRow(Theme t, int idx) {
        long denom = ChipRackView.DENOMS[idx];
        int colour = ChipRackColors.colourFor(denom);

        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LayoutParams rlp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        rlp.topMargin = dp(2);
        rlp.bottomMargin = dp(2);
        row.setLayoutParams(rlp);

        // chip disc
        View disc = new View(getContext());
        int tile = dp(28);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(colour);
        g.setStroke(dp(t.borderWidthDp()), Color.WHITE);
        disc.setBackground(g);
        LayoutParams dlp = new LayoutParams(tile, tile);
        dlp.rightMargin = dp(10);
        row.addView(disc, dlp);

        TextView denomText = new TextView(getContext());
        denomText.setText(String.valueOf(denom));
        denomText.setTextColor(t.colorFg1());
        denomText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        denomText.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        LayoutParams dtxLp = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        row.addView(denomText, dtxLp);

        Button minus = stepButton(t, "−");
        minus.setOnClickListener(v -> {
            int c = staged.getOrDefault(denom, 0);
            if (c > 0) {
                staged.put(denom, c - 1);
                renderTotals(); renderCounts();
            }
        });
        row.addView(minus);

        TextView count = new TextView(getContext());
        countLabels[idx] = count;
        count.setText("0");
        count.setTextColor(t.colorFg1());
        count.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        count.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        count.setMinWidth(dp(34));
        count.setGravity(Gravity.CENTER);
        row.addView(count);

        Button plus = stepButton(t, "+");
        plus.setOnClickListener(v -> {
            int c = staged.getOrDefault(denom, 0);
            staged.put(denom, c + 1);
            renderTotals(); renderCounts();
        });
        row.addView(plus);

        return row;
    }

    private Button stepButton(Theme t, String label) {
        Button b = new Button(getContext());
        b.setText(label);
        b.setStateListAnimator(null);
        b.setAllCaps(false);
        b.setMinWidth(dp(40));
        b.setMinimumWidth(dp(40));
        b.setMinHeight(dp(38));
        b.setMinimumHeight(dp(38));
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        b.setTextColor(t.colorFg1());
        b.setTypeface(b.getTypeface(), Typeface.BOLD);
        b.setPadding(0, 0, 0, 0);
        GradientDrawable g = new GradientDrawable();
        g.setColor(t.colorBg());
        g.setStroke(dp(t.borderWidthDp()), t.colorFg3());
        g.setCornerRadius(dp((int) t.radiusBtnDp()));
        b.setBackground(g);
        return b;
    }

    private Button pillButton(Theme t, String label, boolean accent) {
        Button b = new Button(getContext());
        b.setText(label);
        b.setStateListAnimator(null);
        b.setAllCaps(true);
        b.setLetterSpacing(0.12f);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        b.setTextColor(accent ? t.colorAccentOn() : t.colorFg1());
        b.setTypeface(b.getTypeface(), Typeface.BOLD);
        GradientDrawable g = new GradientDrawable();
        g.setColor(accent ? t.colorAccent() : t.colorBg());
        g.setStroke(dp(t.borderWidthDp()), accent ? t.colorAccent() : t.colorFg3());
        g.setCornerRadius(dp((int) t.radiusBtnDp() + 2));
        b.setBackground(g);
        return b;
    }

    private Button quickButton(Theme t, String label, View.OnClickListener listener) {
        Button b = new Button(getContext());
        b.setText(label);
        b.setStateListAnimator(null);
        b.setAllCaps(true);
        b.setLetterSpacing(0.10f);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        b.setTextColor(t.colorFg1());
        b.setTypeface(b.getTypeface(), Typeface.BOLD);
        b.setPadding(dp(4), 0, dp(4), 0);
        b.setMinHeight(dp(34));
        GradientDrawable g = new GradientDrawable();
        g.setColor(t.colorBg());
        g.setStroke(dp(t.borderWidthDp()), t.colorFg3());
        g.setCornerRadius(dp((int) t.radiusBtnDp() + 2));
        b.setBackground(g);
        b.setOnClickListener(listener);
        LayoutParams lp = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = dp(4);
        b.setLayoutParams(lp);
        return b;
    }

    private void setStaged(long amountToAdd) {
        if (amountToAdd < 0) amountToAdd = 0;
        if (amountToAdd > ownStack) amountToAdd = ownStack;
        staged.clear();
        long left = amountToAdd;
        for (int i = ChipRackView.DENOMS.length - 1; i >= 0; i--) {
            long d = ChipRackView.DENOMS[i];
            int n = (int) (left / d);
            if (n > 0) {
                staged.put(d, n);
                left -= (long) n * d;
            }
        }
        renderTotals();
        renderCounts();
    }

    private long stagedTotal() {
        long sum = 0;
        for (Map.Entry<Long, Integer> e : staged.entrySet()) {
            sum += (long) e.getKey() * e.getValue();
        }
        return sum;
    }

    private void renderTotals() {
        long s = stagedTotal();
        totalText.setText(ChipRackView.formatAmount(s));
        StringBuilder hint = new StringBuilder();
        hint.append("MIN ").append(ChipRackView.formatAmount(minimum));
        if (owedToCall > 0) hint.append("   CALL ").append(ChipRackView.formatAmount(owedToCall));
        hint.append("   STACK ").append(ChipRackView.formatAmount(ownStack));
        hintText.setText(hint.toString());
    }

    private void renderCounts() {
        for (int i = 0; i < ChipRackView.DENOMS.length; i++) {
            int c = staged.getOrDefault(ChipRackView.DENOMS[i], 0);
            if (countLabels[i] != null) countLabels[i].setText(String.valueOf(c));
        }
    }

    private LayoutParams lpMatch() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    public static final class ChipRackColors {
        private ChipRackColors() {}
        public static int colourFor(long denom) {
            for (int i = 0; i < ChipRackView.DENOMS.length; i++) {
                if (ChipRackView.DENOMS[i] == denom) return COLORS[i];
            }
            return 0xFFFFFFFF;
        }
        private static final int[] COLORS = {
                0xFF5DBE6B, 0xFF4D9DE0, 0xFFE15554, 0xFF2A2A2A, 0xFFE9C46A
        };
    }
}
