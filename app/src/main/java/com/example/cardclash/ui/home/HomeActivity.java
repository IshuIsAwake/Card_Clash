package com.example.cardclash.ui.home;

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
import com.example.cardclash.ui.hotseat.HotSeatSetupActivity;
import com.example.cardclash.ui.room.CreateRoomActivity;
import com.example.cardclash.ui.room.JoinRoomActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends ThemedActivity {

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        applyThemeChrome();

        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        TextView footer = findViewById(R.id.tvFooter);
        if (u != null) {
            String email = u.getEmail();
            String name = u.getDisplayName();
            footer.setText("Signed in as " + (name != null && !name.isEmpty() ? name :
                    email != null ? email : "—"));
            footer.setVisibility(View.VISIBLE);
        } else {
            footer.setVisibility(View.GONE);
        }

        findViewById(R.id.cardCreate).setOnClickListener(v ->
                startActivity(new Intent(this, CreateRoomActivity.class)));
        findViewById(R.id.cardJoin).setOnClickListener(v ->
                startActivity(new Intent(this, JoinRoomActivity.class)));
        findViewById(R.id.cardHotSeat).setOnClickListener(v ->
                startActivity(new Intent(this, HotSeatSetupActivity.class)));
        findViewById(R.id.btnProfile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }

    private void applyThemeChrome() {
        Theme t = ThemePrefs.activeTheme(this);
        Typeface display = safeFont(t.fontDisplay());
        Typeface heading = safeFont(t.fontHeading());
        Typeface body = safeFont(t.fontBody());
        Typeface mono = safeFont(t.fontMono());

        TextView welcome = findViewById(R.id.tvWelcome);
        welcome.setTypeface(display, Typeface.BOLD);
        welcome.setTextColor(t.colorFg1());

        TextView subtitle = findViewById(R.id.tvSubtitle);
        subtitle.setTypeface(body);
        subtitle.setTextColor(t.colorFg2());

        TextView session = findViewById(R.id.tvSessionLabel);
        session.setTypeface(heading, Typeface.BOLD);
        session.setTextColor(t.colorFg2());

        TextView topbar = findViewById(R.id.topbarLabel);
        topbar.setTypeface(heading, Typeface.BOLD);
        topbar.setTextColor(t.colorFg2());

        TextView footer = findViewById(R.id.tvFooter);
        footer.setTypeface(mono);
        footer.setTextColor(t.colorFg3());

        styleSecondaryButton((Button) findViewById(R.id.btnProfile), t);
        styleCard(findViewById(R.id.cardCreate),
                  findViewById(R.id.cardCreateTitle),
                  findViewById(R.id.cardCreateBody), t);
        styleCard(findViewById(R.id.cardJoin),
                  findViewById(R.id.cardJoinTitle),
                  findViewById(R.id.cardJoinBody), t);
        styleCard(findViewById(R.id.cardHotSeat),
                  findViewById(R.id.cardHotSeatTitle),
                  findViewById(R.id.cardHotSeatBody), t);
    }

    private void styleCard(View card, TextView title, TextView body, Theme t) {
        Typeface heading = safeFont(t.fontHeading());
        Typeface bodyTf = safeFont(t.fontBody());
        title.setTypeface(heading, Typeface.BOLD);
        title.setTextColor(t.colorFg1());
        body.setTypeface(bodyTf);
        body.setTextColor(t.colorFg2());
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp((int) t.radiusSurfaceDp()));
        bg.setColor(t.colorSurface());
        bg.setStroke(dp(t.borderWidthDp()), t.colorFg3());
        card.setBackground(bg);
    }

    private void styleSecondaryButton(Button b, Theme t) {
        b.setAllCaps(true);
        b.setLetterSpacing(0.12f);
        b.setTypeface(safeFont(t.fontHeading()), Typeface.BOLD);
        b.setStateListAnimator(null);
        b.setTextColor(t.colorFg1());
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp((int) t.radiusBtnDp()));
        bg.setColor(t.colorBg());
        bg.setStroke(dp(t.borderWidthDp()), t.colorFg3());
        b.setBackground(bg);
        b.setMinHeight(dp(40));
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
