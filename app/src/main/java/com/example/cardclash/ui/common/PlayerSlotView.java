package com.example.cardclash.ui.common;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.cardclash.core.models.Player;
import com.example.cardclash.core.theme.Theme;
import com.example.cardclash.core.theme.ThemePrefs;

/** Avatar + name + chip count for an opponent slot. */
public class PlayerSlotView extends LinearLayout {

    private final TextView avatar;
    private final TextView name;
    private final ChipStackView chips;

    public PlayerSlotView(Context context) { this(context, null); }
    public PlayerSlotView(Context context, @Nullable AttributeSet attrs) { this(context, attrs, 0); }
    public PlayerSlotView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);

        Theme t = ThemePrefs.activeTheme(context);

        avatar = new TextView(context);
        avatar.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        avatar.setTextColor(t.colorBackground());
        avatar.setGravity(Gravity.CENTER);
        avatar.setTypeface(avatar.getTypeface(), android.graphics.Typeface.BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(t.colorPrimary());
        bg.setStroke(dp(2), t.colorAccent());
        avatar.setBackground(bg);
        LayoutParams alp = new LayoutParams(dp(56), dp(56));
        addView(avatar, alp);

        name = new TextView(context);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        name.setTextColor(t.colorTextPrimary());
        name.setMaxLines(1);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LayoutParams nlp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        nlp.topMargin = dp(4);
        addView(name, nlp);

        chips = new ChipStackView(context);
        addView(chips);
    }

    public void bind(Player p, boolean active) {
        avatar.setText(initials(p.displayName));
        name.setText(p.displayName);
        chips.setAmount(p.chips);
        setAlpha(active ? 1f : 0.4f);
    }

    public void setActive(boolean active) {
        Theme t = ThemePrefs.activeTheme(getContext());
        GradientDrawable bg = (GradientDrawable) avatar.getBackground();
        bg.setStroke(dp(active ? 3 : 2), active ? t.colorWarning() : t.colorAccent());
    }

    private static String initials(String s) {
        if (s == null || s.isEmpty()) return "?";
        String[] parts = s.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }
}
