package com.example.cardclash.ui.auth;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.example.cardclash.R;
import com.example.cardclash.core.theme.Theme;
import com.example.cardclash.core.theme.ThemePrefs;
import com.example.cardclash.ui.common.ThemedActivity;
import com.example.cardclash.ui.home.HomeActivity;
import com.example.cardclash.ui.hotseat.HotSeatSetupActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends ThemedActivity {

    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        applyThemeChrome();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(v -> doLogin());
        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        findViewById(R.id.btnHotSeat).setOnClickListener(v ->
                startActivity(new Intent(this, HotSeatSetupActivity.class)));
    }

    private void applyThemeChrome() {
        Theme t = ThemePrefs.activeTheme(this);
        Typeface display = safeFont(t.fontDisplay());
        Typeface body = safeFont(t.fontBody());

        TextView welcome = findViewById(R.id.tvWelcome);
        welcome.setTypeface(display, Typeface.BOLD);
        welcome.setTextColor(t.colorFg1());

        TextView subtitle = findViewById(R.id.tvSubtitle);
        subtitle.setTypeface(body);
        subtitle.setTextColor(t.colorFg2());

        TextView register = findViewById(R.id.tvRegister);
        register.setTypeface(body);
        register.setTextColor(t.colorAccent());

        View divider = findViewById(R.id.divider);
        divider.setBackgroundColor(t.colorFg3());

        Button signIn = findViewById(R.id.btnLogin);
        styleAccentButton(signIn, t);

        Button hot = findViewById(R.id.btnHotSeat);
        styleSecondaryButton(hot, t);
    }

    private void styleAccentButton(Button b, Theme t) {
        b.setAllCaps(true);
        b.setLetterSpacing(0.12f);
        b.setTypeface(safeFont(t.fontHeading()), Typeface.BOLD);
        b.setStateListAnimator(null);
        b.setTextColor(t.colorAccentOn());
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp((int) t.radiusBtnDp()));
        bg.setColor(t.colorAccent());
        bg.setStroke(dp(t.borderWidthDp()), t.colorAccent());
        b.setBackground(bg);
        b.setMinHeight(dp(52));
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
        b.setMinHeight(dp(48));
    }

    private void doLogin() {
        String email = String.valueOf(etEmail.getText()).trim();
        String pass = String.valueOf(etPassword.getText());
        if (email.isEmpty() || pass.length() < 6) {
            Toast.makeText(this, "Enter a valid email and password", Toast.LENGTH_SHORT).show();
            return;
        }
        btnLogin.setEnabled(false);
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(t -> {
                    btnLogin.setEnabled(true);
                    if (t.isSuccessful()) {
                        startActivity(new Intent(this, HomeActivity.class));
                        finish();
                    } else {
                        String msg = t.getException() != null ? t.getException().getMessage() : "Login failed";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                });
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
