package com.example.cardclash.ui.hotseat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.example.cardclash.R;
import com.example.cardclash.core.theme.Theme;
import com.example.cardclash.core.theme.ThemePrefs;
import com.example.cardclash.ui.common.ThemedActivity;

/**
 * Full-screen device-handoff gate. Opaque themed background — no blur — to
 * fully hide whatever was on the previous turn's screen. The named player taps
 * Continue, the activity returns RESULT_OK, and the calling table re-binds the
 * UI to the new local player.
 *
 * <p>Back button is suppressed: a misclick must not skip the handoff.
 */
public class PassTheDeviceActivity extends ThemedActivity {

    public static final String EXTRA_PLAYER_NAME  = "player_name";
    public static final String EXTRA_PLAYER_INDEX = "player_index";
    public static final String EXTRA_PLAYER_COUNT = "player_count";

    public static Intent intent(Context ctx, String name, int index, int count) {
        Intent i = new Intent(ctx, PassTheDeviceActivity.class);
        i.putExtra(EXTRA_PLAYER_NAME, name);
        i.putExtra(EXTRA_PLAYER_INDEX, index);
        i.putExtra(EXTRA_PLAYER_COUNT, count);
        return i;
    }

    @Override protected void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_pass_the_device);

        Theme t = ThemePrefs.activeTheme(this);
        String name  = getIntent().getStringExtra(EXTRA_PLAYER_NAME);
        int    index = getIntent().getIntExtra(EXTRA_PLAYER_INDEX, 1);
        int    count = getIntent().getIntExtra(EXTRA_PLAYER_COUNT, 0);
        if (name == null) name = "Player " + index;

        // Opaque themed background — must hide previous content fully.
        findViewById(R.id.passRoot).setBackgroundColor(t.colorBg());

        TextView eyebrow = findViewById(R.id.eyebrow);
        eyebrow.setTextColor(t.colorFg2());
        eyebrow.setTypeface(safeFont(t.fontHeading()), Typeface.BOLD);

        TextView playerName = findViewById(R.id.playerName);
        playerName.setText(name.toUpperCase());
        playerName.setTextColor(t.colorFg1());
        playerName.setTypeface(safeFont(t.fontDisplay()), Typeface.BOLD);

        TextView subtitle = findViewById(R.id.subtitle);
        subtitle.setTextColor(t.colorFg2());
        subtitle.setTypeface(safeFont(t.fontBody()));

        TextView footer = findViewById(R.id.footer);
        if (count > 0) {
            footer.setText("PLAYER " + index + " OF " + count);
            footer.setTextColor(t.colorFg3());
            footer.setTypeface(safeFont(t.fontMono()));
            footer.setVisibility(View.VISIBLE);
        } else {
            footer.setVisibility(View.GONE);
        }

        Button go = findViewById(R.id.btnContinue);
        go.setText("I'M " + name.toUpperCase() + "  —  CONTINUE");
        go.setAllCaps(false);
        go.setLetterSpacing(0.10f);
        go.setTypeface(safeFont(t.fontHeading()), Typeface.BOLD);
        go.setStateListAnimator(null);
        go.setTextColor(t.colorAccentOn());
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp((int) t.radiusBtnDp()));
        bg.setColor(t.colorAccent());
        bg.setStroke(dp(t.borderWidthDp()), t.colorAccent());
        go.setBackground(bg);
        go.setMinHeight(dp(56));
        go.setOnClickListener(v -> {
            setResult(Activity.RESULT_OK);
            finish();
        });
    }

    /** Suppress: misclicking back must not skip the handoff. */
    @Override public void onBackPressed() { /* no-op */ }

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
