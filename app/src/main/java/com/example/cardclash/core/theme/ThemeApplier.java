package com.example.cardclash.core.theme;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

/**
 * Re-binds an activity's view hierarchy to the active theme. Call from every
 * activity's {@code onCreate} after {@code setContentView}, and again whenever
 * the user changes themes from the in-game settings menu.
 *
 * <p>Strategy: walk the view tree, and any view tagged with R.id.theme_role gets
 * its color/drawable resolved from the Theme. We deliberately keep this simple
 * for v1 — the bulk of theming lives in resource styles selected by activity
 * theme attribute.
 */
public final class ThemeApplier {

    private ThemeApplier() {}

    /** Set the activity's AppCompat style. Must be called BEFORE setContentView. */
    public static void applyToActivity(Activity activity) {
        Theme t = ThemePrefs.activeTheme(activity);
        activity.setTheme(t.appStyle());
    }

    /** Re-skin the existing hierarchy in place (used after a theme switch). */
    public static void rebind(Activity activity) {
        // For v1 we simply recreate(); future versions can rebind in place to
        // avoid the activity flash.
        activity.recreate();
    }

    /** Walk a view tree, ignoring nulls. Reserved for future granular re-skin. */
    public static void walk(View v, java.util.function.Consumer<View> fn) {
        if (v == null) return;
        fn.accept(v);
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) walk(g.getChildAt(i), fn);
        }
    }
}
