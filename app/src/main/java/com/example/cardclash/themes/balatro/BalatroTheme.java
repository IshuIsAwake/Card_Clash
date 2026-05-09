package com.example.cardclash.themes.balatro;

import com.example.cardclash.R;
import com.example.cardclash.core.theme.Theme;
import com.example.cardclash.core.theme.ThemeId;

/**
 * Balatro theme — pixel / CRT lane. Deep pickled-green felt, bevelled panels,
 * orange + cream accents, hard 2px offset shadows under every interactive
 * surface. Display: Press Start 2P; running text: VT323.
 */
public class BalatroTheme implements Theme {
    @Override public ThemeId id() { return ThemeId.BALATRO; }
    @Override public String displayName() { return "Balatro"; }
    @Override public boolean owned() { return true; }
    @Override public int appStyle() { return R.style.Theme_CardClash_Balatro; }

    @Override public int colorBackground()      { return 0xFF1B2D24; }
    @Override public int colorSurface()         { return 0xFF2D2E33; }
    @Override public int colorTablefelt()       { return 0xFF2A553C; }
    @Override public int colorPrimary()         { return 0xFFE69020; }
    @Override public int colorAccent()          { return 0xFFE69020; }
    @Override public int colorTextPrimary()     { return 0xFFFFFFFF; }
    @Override public int colorTextSecondary()   { return 0xFFC9C9C9; }
    @Override public int colorWin()             { return 0xFF7AC080; }
    @Override public int colorLose()            { return 0xFFD85A3E; }
    @Override public int colorWarning()         { return 0xFFF0BD3A; }

    @Override public int drawableTableBackground() { return R.drawable.bg_table_balatro; }
    @Override public int drawableCardBack()        { return R.drawable.bg_card_back_balatro; }
    @Override public int drawableChipStack()       { return R.drawable.bg_chip_stack_balatro; }
    @Override public int drawableButtonPrimary()   { return R.drawable.bg_btn_primary_balatro; }
    @Override public int drawablePotIndicator()    { return R.drawable.bg_pot_indicator_balatro; }
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

    @Override public int colorFg3()         { return 0xFF777777; }
    @Override public int colorBorder()      { return 0xFF1B1C20; }
    @Override public int colorAccentOn()    { return 0xFFFFFFFF; }
    @Override public int colorCardFace()    { return 0xFFF5EFE2; }
    @Override public int colorCardRed()     { return 0xFFD85A3E; }
    @Override public int colorCardBlack()   { return 0xFF1A1A1A; }

    @Override public int fontDisplay()  { return R.font.press_start_2p; }
    @Override public int fontHeading()  { return R.font.press_start_2p; }
    @Override public int fontBody()     { return R.font.vt323; }
    @Override public int fontMono()     { return R.font.vt323; }

    @Override public int btnSecondaryBg() { return R.drawable.bg_btn_secondary_balatro; }
    @Override public int cardFaceBg()     { return R.drawable.bg_card_face_balatro; }
    @Override public int chipBg(int d)    { return R.drawable.bg_chip_stack_balatro; }
    @Override public int pillSelector()   { return R.drawable.selector_pill_balatro; }

    @Override public float radiusBtnDp()     { return 2f; }
    @Override public float radiusCardDp()    { return 2f; }
    @Override public float radiusSurfaceDp() { return 2f; }
    @Override public int   borderWidthDp()   { return 2; }
}
