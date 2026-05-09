package com.example.cardclash.ui.hotseat;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.cardclash.core.theme.Theme;
import com.example.cardclash.core.theme.ThemePrefs;

/**
 * Full-screen "Pass to {next} — tap to continue" gate. Hides whatever was on
 * screen so the previous player can hand the device over without revealing
 * the next player's hand.
 */
public final class PassGate {

    private PassGate() {}

    public static void show(Activity activity, String nextDisplayName, String subtitle, Runnable onContinue) {
        Theme t = ThemePrefs.activeTheme(activity);
        Dialog d = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.black);
        }
        d.setCancelable(false);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(t.colorBackground());
        int pad = (int) (24 * activity.getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        TextView pre = new TextView(activity);
        pre.setText("PASS THE DEVICE");
        pre.setLetterSpacing(0.2f);
        pre.setTextColor(t.colorTextSecondary());
        pre.setTextSize(13);
        pre.setGravity(Gravity.CENTER);
        root.addView(pre);

        TextView name = new TextView(activity);
        name.setText(nextDisplayName.toUpperCase());
        name.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        name.setTextColor(t.colorTextPrimary());
        name.setTextSize(40);
        name.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nlp.topMargin = pad / 2;
        nlp.bottomMargin = pad / 2;
        root.addView(name, nlp);

        if (subtitle != null) {
            TextView sub = new TextView(activity);
            sub.setText(subtitle);
            sub.setTextColor(t.colorTextSecondary());
            sub.setTextSize(14);
            sub.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            slp.bottomMargin = pad;
            root.addView(sub, slp);
        }

        Button go = new Button(activity);
        go.setText("I'M " + nextDisplayName.toUpperCase() + " — CONTINUE");
        go.setAllCaps(false);
        go.setTextSize(14);
        go.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setColor(t.colorTextPrimary());
        bg.setStroke((int) (2 * activity.getResources().getDisplayMetrics().density), t.colorTextPrimary());
        go.setBackground(bg);
        go.setTextColor(t.colorBackground());
        go.setPadding(pad, pad / 2, pad, pad / 2);
        go.setOnClickListener(v -> {
            d.dismiss();
            if (onContinue != null) onContinue.run();
        });
        root.addView(go);

        d.setContentView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        d.show();
    }
}
