package com.example.cardclash.core.theme;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.RawRes;
import androidx.annotation.StyleRes;

/**
 * Holistic skin for the app. A theme owns colors, typography, drawables, sounds,
 * and animation references. Every screen / view that renders themed content
 * resolves its inputs via this interface — no direct R.color.* lookups outside
 * the active Theme.
 *
 * Future themes register in {@link ThemeRegistry} and ship their own resources.
 * The interface is intentionally narrow at v1 — extend deliberately.
 */
public interface Theme {

    ThemeId id();
    String displayName();
    /** True if owned by every user in v1; future shop will gate this. */
    boolean owned();

    /** AppCompat style applied at activity {@code setTheme()}. */
    @StyleRes int appStyle();

    // -- Palette ----------------------------------------------------------
    @ColorInt int colorBackground();
    @ColorInt int colorSurface();
    @ColorInt int colorTablefelt();
    @ColorInt int colorPrimary();
    @ColorInt int colorAccent();
    @ColorInt int colorTextPrimary();
    @ColorInt int colorTextSecondary();
    @ColorInt int colorWin();
    @ColorInt int colorLose();
    @ColorInt int colorWarning();

    // -- Drawables --------------------------------------------------------
    @DrawableRes int drawableTableBackground();
    @DrawableRes int drawableCardBack();
    @DrawableRes int drawableChipStack();
    @DrawableRes int drawableButtonPrimary();
    @DrawableRes int drawablePotIndicator();
    @DrawableRes int drawableLogo();

    // -- Sounds (raw) ----------------------------------------------------
    @RawRes int soundCardFlip();
    @RawRes int soundChipClink();
    @RawRes int soundShuffle();
    @RawRes int soundDeal();
    @RawRes int soundWin();
    @RawRes int soundLose();
    @RawRes int soundButtonTap();

    // -- Lottie / animation asset paths -----------------------------------
    String lottieWinSequence();
    String lottieLossSequence();
}
