package com.example.cardclash.ui.common;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.cardclash.core.theme.Theme;
import com.example.cardclash.core.theme.ThemePrefs;

/** Tiny chip pill: themed dot + numeric. */
public class ChipStackView extends LinearLayout {
    private final TextView amountText;
    private final TextView dot;

    public ChipStackView(Context context) { this(context, null); }
    public ChipStackView(Context context, @Nullable AttributeSet attrs) { this(context, attrs, 0); }
    public ChipStackView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);

        Theme t = ThemePrefs.activeTheme(context);

        dot = new TextView(context);
        dot.setBackgroundResource(t.drawableChipStack());
        dot.setText("");
        LayoutParams ldp = new LayoutParams(dp(18), dp(18));
        ldp.setMargins(0, 0, dp(6), 0);
        addView(dot, ldp);

        amountText = new TextView(context);
        amountText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        amountText.setTextColor(t.colorTextPrimary());
        amountText.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        addView(amountText);
    }

    public void setAmount(long n) { amountText.setText(format(n)); }

    private static String format(long n) {
        if (n >= 1_000_000) return (n / 1000) / 1000.0 + "M";
        if (n >= 10_000) return (n / 1000) + "k";
        return String.valueOf(n);
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }
}
