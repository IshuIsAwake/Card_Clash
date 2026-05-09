package com.example.cardclash.core.theme;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.FontRes;
import androidx.annotation.RawRes;
import androidx.annotation.StyleRes;

/**
 * Holistic skin for the app. A theme owns colors, typography, drawables, sounds,
 * and animation references. Every screen / view that renders themed content
 * resolves its inputs via this interface — no direct R.color.* lookups outside
 * the active Theme.
 */
public interface Theme {

    ThemeId id();
    String displayName();
    boolean owned();

    @StyleRes int appStyle();

    // -- Legacy palette (kept for back-compat with existing call sites) ---
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

    // -- Design-system semantic palette (HANDOFF §5) ---------------------
    @ColorInt default int colorBg()           { return colorBackground(); }
    @ColorInt default int colorSurfaceAlt()   { return colorSurface(); }
    @ColorInt default int colorFg1()          { return colorTextPrimary(); }
    @ColorInt default int colorFg2()          { return colorTextSecondary(); }
    @ColorInt int colorFg3();
    @ColorInt int colorBorder();
    @ColorInt int colorAccentOn();
    @ColorInt int colorCardFace();
    @ColorInt int colorCardRed();
    @ColorInt int colorCardBlack();

    // -- Typography (Downloadable Fonts via Google Fonts provider) -------
    @FontRes int fontDisplay();
    @FontRes int fontHeading();
    @FontRes int fontBody();
    @FontRes int fontMono();

    // -- Drawables -------------------------------------------------------
    @DrawableRes int drawableTableBackground();
    @DrawableRes int drawableCardBack();
    @DrawableRes int drawableChipStack();
    @DrawableRes int drawableButtonPrimary();
    @DrawableRes int drawablePotIndicator();
    @DrawableRes int drawableLogo();

    @DrawableRes default int btnPrimaryBg()   { return drawableButtonPrimary(); }
    @DrawableRes int btnSecondaryBg();
    @DrawableRes int cardFaceBg();
    @DrawableRes default int cardBackBg()     { return drawableCardBack(); }
    @DrawableRes int chipBg(int denomination);
    @DrawableRes default int tableBg()        { return drawableTableBackground(); }
    @DrawableRes int pillSelector();

    // -- Geometry --------------------------------------------------------
    float radiusBtnDp();
    float radiusCardDp();
    float radiusSurfaceDp();
    int   borderWidthDp();

    // -- Sounds ----------------------------------------------------------
    @RawRes int soundCardFlip();
    @RawRes int soundChipClink();
    @RawRes int soundShuffle();
    @RawRes int soundDeal();
    @RawRes int soundWin();
    @RawRes int soundLose();
    @RawRes int soundButtonTap();

    String lottieWinSequence();
    String lottieLossSequence();
}
