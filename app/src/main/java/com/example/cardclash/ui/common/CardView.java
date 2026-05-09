package com.example.cardclash.ui.common;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.cardclash.core.models.Card;
import com.example.cardclash.core.theme.Theme;
import com.example.cardclash.core.theme.ThemePrefs;

/**
 * Lightweight playing-card view. Front shows rank glyph + suit; back uses the
 * active theme's card back drawable.
 */
public class CardView extends FrameLayout {

    private final TextView rankTop;
    private final TextView suitCenter;
    private boolean faceUp = true;
    private Card card;

    public CardView(Context context) { this(context, null); }
    public CardView(Context context, @Nullable AttributeSet attrs) { this(context, attrs, 0); }
    public CardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setClipToOutline(true);
        rankTop = new TextView(context);
        rankTop.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        rankTop.setTypeface(rankTop.getTypeface(), android.graphics.Typeface.BOLD);
        FrameLayout.LayoutParams lpTop = new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lpTop.gravity = Gravity.TOP | Gravity.START;
        lpTop.setMargins(dp(6), dp(4), dp(6), dp(4));
        addView(rankTop, lpTop);

        suitCenter = new TextView(context);
        suitCenter.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        FrameLayout.LayoutParams lpC = new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lpC.gravity = Gravity.CENTER;
        addView(suitCenter, lpC);
        applyTheme();
    }

    public void bind(Card c, boolean faceUp) {
        this.card = c;
        this.faceUp = faceUp;
        applyTheme();
    }

    public void setFaceUp(boolean up) {
        if (this.faceUp == up) return;
        this.faceUp = up;
        animate().rotationY(90).setDuration(120).withEndAction(() -> {
            applyTheme();
            setRotationY(-90);
            animate().rotationY(0).setDuration(120).start();
        }).start();
    }

    private void applyTheme() {
        Theme t = ThemePrefs.activeTheme(getContext());
        if (faceUp && card != null) {
            setBackgroundResource(com.example.cardclash.R.drawable.bg_card_face);
            int color = card.suit.black ? 0xFF15110B : 0xFF9E2A1F;
            rankTop.setText(card.rank.label);
            rankTop.setTextColor(color);
            suitCenter.setText(card.suit.glyph);
            suitCenter.setTextColor(color);
        } else {
            setBackgroundResource(t.drawableCardBack());
            rankTop.setText("");
            suitCenter.setText("");
        }
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }
}
