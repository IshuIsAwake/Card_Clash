package com.example.cardclash.core.theme;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.example.cardclash.themes.dev.DevTheme;
import com.example.cardclash.themes.neonpulse.NeonPulseTheme;
import com.example.cardclash.themes.royaloak.RoyalOakTheme;

/**
 * Registry of every available theme, keyed by id. Adding a theme = ship its
 * resources, register it here, done. UI code never branches on id — it always
 * resolves the active Theme through here.
 */
public final class ThemeRegistry {

    private static final Map<ThemeId, Theme> THEMES = new LinkedHashMap<>();

    static {
        register(new DevTheme());
        register(new RoyalOakTheme());
        register(new NeonPulseTheme());
    }

    private ThemeRegistry() {}

    public static void register(Theme t) { THEMES.put(t.id(), t); }

    public static Theme get(ThemeId id) {
        Theme t = THEMES.get(id);
        if (t == null) throw new IllegalStateException("No theme registered: " + id);
        return t;
    }

    public static Collection<Theme> all() { return THEMES.values(); }

    public static Theme defaultTheme() { return THEMES.get(ThemeId.DEVELOPER); }
}
