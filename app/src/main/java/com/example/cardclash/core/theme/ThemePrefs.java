package com.example.cardclash.core.theme;

import android.content.Context;
import android.content.SharedPreferences;

/** Per-user persistence of the active theme id. */
public final class ThemePrefs {

    private static final String FILE = "cardclash_theme";
    private static final String KEY_ID = "active_theme_id";

    private ThemePrefs() {}

    public static ThemeId active(Context ctx) {
        SharedPreferences sp = ctx.getApplicationContext()
                .getSharedPreferences(FILE, Context.MODE_PRIVATE);
        String s = sp.getString(KEY_ID, null);
        if (s == null) return ThemeRegistry.defaultTheme().id();
        try { return ThemeId.valueOf(s); }
        catch (IllegalArgumentException e) { return ThemeRegistry.defaultTheme().id(); }
    }

    public static void setActive(Context ctx, ThemeId id) {
        ctx.getApplicationContext()
                .getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .edit().putString(KEY_ID, id.name()).apply();
    }

    public static Theme activeTheme(Context ctx) {
        return ThemeRegistry.get(active(ctx));
    }
}
