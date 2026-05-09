package com.example.cardclash.themes.dev;

import com.example.cardclash.R;
import com.example.cardclash.core.theme.Theme;
import com.example.cardclash.core.theme.ThemeId;

/**
 * Developer theme. Dark background, white-on-black loud controls. Selection
 * state is communicated by fill + border swap (never alpha alone) so testing
 * which option is active is unambiguous at a glance.
 *
 * Why: lead dev's own testing surface. Looks don't matter — clarity does.
 */
public class DevTheme implements Theme {
    @Override public ThemeId id() { return ThemeId.DEVELOPER; }
    @Override public String displayName() { return "Developer"; }
    @Override public boolean owned() { return true; }
    @Override public int appStyle() { return R.style.Theme_CardClash_Dev; }

    @Override public int colorBackground()      { return 0xFF000000; }
    @Override public int colorSurface()         { return 0xFF111111; }
    @Override public int colorTablefelt()       { return 0xFF1A1A1A; }
    @Override public int colorPrimary()         { return 0xFFFFFFFF; }
    @Override public int colorAccent()          { return 0xFFFFFFFF; }
    @Override public int colorTextPrimary()     { return 0xFFFFFFFF; }
    @Override public int colorTextSecondary()   { return 0xFFB0B0B0; }
    @Override public int colorWin()             { return 0xFFFFFFFF; }
    @Override public int colorLose()            { return 0xFF888888; }
    @Override public int colorWarning()         { return 0xFFFFFFFF; }

    @Override public int drawableTableBackground() { return R.drawable.bg_table_dev; }
    @Override public int drawableCardBack()        { return R.drawable.bg_card_back_dev; }
    @Override public int drawableChipStack()       { return R.drawable.bg_chip_stack_dev; }
    @Override public int drawableButtonPrimary()   { return R.drawable.bg_btn_primary_dev; }
    @Override public int drawablePotIndicator()    { return R.drawable.bg_pot_indicator_dev; }
    @Override public int drawableLogo()            { return R.drawable.ic_logo_cardclash; }

    @Override public int soundCardFlip()    { return 0; }
    @Override public int soundChipClink()   { return 0; }
    @Override public int soundShuffle()     { return 0; }
    @Override public int soundDeal()        { return 0; }
    @Override public int soundWin()         { return 0; }
    @Override public int soundLose()        { return 0; }
    @Override public int soundButtonTap()   { return 0; }

    @Override public String lottieWinSequence()  { return null; }
    @Override public String lottieLossSequence() { return null; }
}
