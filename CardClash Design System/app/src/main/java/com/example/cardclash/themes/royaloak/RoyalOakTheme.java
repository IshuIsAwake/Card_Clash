package com.example.cardclash.themes.royaloak;

import com.example.cardclash.R;
import com.example.cardclash.core.theme.Theme;
import com.example.cardclash.core.theme.ThemeId;

public class RoyalOakTheme implements Theme {
    @Override public ThemeId id() { return ThemeId.ROYAL_OAK; }
    @Override public String displayName() { return "Royal Oak"; }
    @Override public boolean owned() { return true; }
    @Override public int appStyle() { return R.style.Theme_CardClash_RoyalOak; }

    @Override public int colorBackground()      { return 0xFF1A0F08; }
    @Override public int colorSurface()         { return 0xFF26170D; }
    @Override public int colorTablefelt()       { return 0xFF0E3D24; }
    @Override public int colorPrimary()         { return 0xFFC9A24B; }
    @Override public int colorAccent()          { return 0xFFE9B97A; }
    @Override public int colorTextPrimary()     { return 0xFFF4E9D6; }
    @Override public int colorTextSecondary()   { return 0xFFB8A07C; }
    @Override public int colorWin()             { return 0xFF7BC97F; }
    @Override public int colorLose()            { return 0xFFC24A3D; }
    @Override public int colorWarning()         { return 0xFFE0A23A; }

    @Override public int drawableTableBackground() { return R.drawable.bg_table_royaloak; }
    @Override public int drawableCardBack()        { return R.drawable.bg_card_back_royaloak; }
    @Override public int drawableChipStack()       { return R.drawable.bg_chip_stack_royaloak; }
    @Override public int drawableButtonPrimary()   { return R.drawable.bg_btn_primary_royaloak; }
    @Override public int drawablePotIndicator()    { return R.drawable.bg_pot_indicator_royaloak; }
    @Override public int drawableLogo()            { return R.drawable.ic_logo_cardclash; }

    // No raw sound assets bundled in v1 reference theme. 0 = unset; sound code
    // must skip playback when the resource id is 0.
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
