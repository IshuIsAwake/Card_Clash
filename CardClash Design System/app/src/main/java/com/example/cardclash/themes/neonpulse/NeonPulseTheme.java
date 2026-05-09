package com.example.cardclash.themes.neonpulse;

import com.example.cardclash.R;
import com.example.cardclash.core.theme.Theme;
import com.example.cardclash.core.theme.ThemeId;

public class NeonPulseTheme implements Theme {
    @Override public ThemeId id() { return ThemeId.NEON_PULSE; }
    @Override public String displayName() { return "Neon Pulse"; }
    @Override public boolean owned() { return true; }
    @Override public int appStyle() { return R.style.Theme_CardClash_NeonPulse; }

    @Override public int colorBackground()      { return 0xFF0A0512; }
    @Override public int colorSurface()         { return 0xFF160A2A; }
    @Override public int colorTablefelt()       { return 0xFF1A0F33; }
    @Override public int colorPrimary()         { return 0xFF00D4FF; }
    @Override public int colorAccent()          { return 0xFFFF1E8E; }
    @Override public int colorTextPrimary()     { return 0xFFF0EAFF; }
    @Override public int colorTextSecondary()   { return 0xFFA89AC9; }
    @Override public int colorWin()             { return 0xFF3FFF99; }
    @Override public int colorLose()            { return 0xFFFF4D6D; }
    @Override public int colorWarning()         { return 0xFFFFCB3F; }

    @Override public int drawableTableBackground() { return R.drawable.bg_table_neonpulse; }
    @Override public int drawableCardBack()        { return R.drawable.bg_card_back_neonpulse; }
    @Override public int drawableChipStack()       { return R.drawable.bg_chip_stack_neonpulse; }
    @Override public int drawableButtonPrimary()   { return R.drawable.bg_btn_primary_neonpulse; }
    @Override public int drawablePotIndicator()    { return R.drawable.bg_pot_indicator_neonpulse; }
    @Override public int drawableLogo()            { return R.drawable.ic_logo_cardclash; }

    @Override public int soundCardFlip()  { return 0; }
    @Override public int soundChipClink() { return 0; }
    @Override public int soundShuffle()   { return 0; }
    @Override public int soundDeal()      { return 0; }
    @Override public int soundWin()       { return 0; }
    @Override public int soundLose()      { return 0; }
    @Override public int soundButtonTap() { return 0; }

    @Override public String lottieWinSequence()  { return null; }
    @Override public String lottieLossSequence() { return null; }
}
