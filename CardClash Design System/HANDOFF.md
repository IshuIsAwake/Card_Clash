# CardClash — Implementation Handoff for Claude Code

## How to use this handoff

1. Open `ui_kits/cardclash_app/index.html` in a browser. Click through every screen × every theme using the toolbar at the top. **Take a manual screenshot (Cmd/Win + Shift + 4)** of each phone shell — crop to just the device, drop the toolbar chrome.
2. Save them under `mocks/` in your repo: `mocks/login-royaloak.png`, `mocks/bluff-table-balatro.png`, etc. The 17 captures worth grabbing:
   - **Login** (portrait) × 4 themes
   - **Home** (portrait) × 4 themes
   - **Hot Seat Setup** (landscape) × 4 themes
   - **Pass Gate** × 1 (theme-agnostic)
   - **Bluff Table** (landscape) × 4 themes ← most important
3. Also include the originals: `uploads/balatro.png` and the WhatsApp screenshots of the current build, so Claude Code sees both target and current state.
4. In Claude Code, when you assign each section below, drag the matching screenshot(s) into the chat alongside the prompt.

> **Note on auto-screenshots**: I tried generating these from this environment and the preview tooling kept disposing the iframe before the captures resolved. Manual captures from a real browser will be cleaner anyway — full-resolution, with proper font rendering.

---

This document is the bridge from this design system to the Android app at `IshuIsAwake/Card_Clash`. Read `README.md` for the design rationale; this file tells you exactly what to build.

It assumes you already have:
- the engine (`core/`), `Theme` interface, `ThemeRegistry`, `ThemePrefs`, `ThemedActivity`
- working Royal Oak + Neon Pulse `Theme` impls
- working Teen Patti engine + activity, Bluff engine + activity
- the screens listed in `memory.md`

It tells you what to **change**, **add**, and **delete**.

---

## 0. Hard rules (locked, do not re-litigate)

1. **Orientation**:
   - **Portrait only**: `SplashActivity`, `LoginActivity`, `RegisterActivity`, `HomeActivity` (game-mode selection / Create-Join-HotSeat).
   - **Landscape locked**: every other screen — Hot Seat setup, pass-the-device gate, Lobby (QR), `PreGameSettingsActivity`, `CreateRoomActivity`, `JoinRoomActivity`, `ProfileActivity`, all game table activities, all in-game dialogs.
   - In `AndroidManifest.xml`: set `android:screenOrientation="portrait"` on the four portrait activities, `="sensorLandscape"` on everything else.
2. **No emoji anywhere.** No Unicode suit chars in copy. Suits are vector drawables (`ic_suit_spade.xml` etc.) tinted per theme.
3. **Selection states** must be border + background swap, never alpha. Audit every `RadioButton` / `ToggleButton` / `Selector` in `res/drawable/`.
4. **Primary CTAs sit at the horizontal middle of the screen**, not bottom-anchored. On landscape game tables this means the action column is centered vertically on the right edge, NOT bottom row. On landscape setup screens the primary button is `layout_gravity="center"` below the form.
5. **No hand-rolled colors in layouts.** Every color reference goes through the `Theme` interface, e.g. `theme.colorAccent()` → wrapped in a `ThemeApplier.tint(view, ColorRole.ACCENT)` call. No `@color/np_cyan` literal in layout XML — it must be theme-routed.

---

## 1. Add the four themes (Developer + Balatro are new)

**Files to add:**

```
app/src/main/java/com/example/cardclash/themes/dev/DevTheme.java
app/src/main/java/com/example/cardclash/themes/balatro/BalatroTheme.java
app/src/main/java/com/example/cardclash/core/theme/ThemeId.java     (add DEVELOPER, BALATRO)
app/src/main/java/com/example/cardclash/core/theme/ThemeRegistry.java  (register both)
```

**Resource additions** (mirror Royal Oak's structure):

```
res/values/colors.xml                  -> add dev_*, bal_* palettes
res/values/themes.xml                  -> Theme.CardClash.Dev[.Splash|.Game], Theme.CardClash.Balatro[...]
res/font/                              -> press_start_2p.ttf, vt323.ttf, jetbrains_mono.ttf, inter.ttf, lora.ttf, playfair_display.ttf, orbitron.ttf
res/drawable/                          -> bg_table_balatro.xml (radial pickled-green gradient + scan-line tile),
                                          bg_table_dev.xml (solid #000),
                                          bg_card_back_balatro.xml (45deg red+black diagonal stripes, 2px hard border),
                                          bg_card_back_dev.xml (solid #000 + 2px white border),
                                          bg_card_face_balatro.xml (#F5EFE2 with 2px black border, 2px black offset shadow),
                                          bg_chip_*_balatro.xml (per denomination),
                                          bg_btn_primary_balatro.xml (solid #E69020 fill, 2px #1B1C20 border, 2px #000 offset shadow, 0 corner-radius),
                                          bg_btn_primary_dev.xml (solid #FFF, 0 radius, no shadow),
                                          bg_pot_indicator_*.xml,
                                          bg_surface_card_balatro.xml,
                                          selector_btn_balatro.xml + selector_btn_dev.xml (state list: pressed = inverted, disabled = 40% alpha + grey),
                                          selector_pill_*.xml.
```

**Token source of truth**: copy hexes verbatim from this skill's `colors_and_type.css`. Do NOT re-pick.

**Default theme on first launch**: set to `DEVELOPER` so the user can finish testing every button. Switch the launch-default to `ROYAL_OAK` once the team's polish round lands.

---

## 2. SchemaRenderer: kill the alpha-only selection bug

`app/src/main/java/com/example/cardclash/ui/room/SchemaRenderer.java`

In `intPicker` and `enumPicker`, replace alpha-toggle code with **background drawable swap**:

- Selected → solid accent fill, accent-on text, 2dp accent border.
- Unselected → transparent fill, fg-1 text, 2dp fg-3 border.
- Disabled → 40% alpha + filled disabled-bg.

Add `selector_pill.xml` per theme; reference via `theme.pillSelector()` rather than inline `setAlpha(0.5f)`.

`listEditor` is currently read-only. Add up/down chevrons on each row (no drag yet — drag is Phase 2). Each row gets `▲ ▼ ✕` controls right-aligned. Use Lucide-style stroked vectors at 24dp; **don't use emoji**.

---

## 3. New screens (build in this order)

### 3.1 HotSeatSetupActivity (landscape)

Path: `app/src/main/java/com/example/cardclash/ui/hotseat/HotSeatSetupActivity.java` + `res/layout/activity_hot_seat_setup.xml`

Layout (landscape, 16:9):

```
┌─────────────────────────────────────────────────────────────┐
│  TOPBAR  HOT SEAT  ·  BLUFF · OPEN CALL          ⓘ ❓ ⚙   │
│                                                              │
│   ┌──────────────────────┐    ┌──────────────────────┐     │
│   │ Single device.       │    │ PLAYERS              │     │
│   │ Pass and play.       │    │ ┌──┐┌──┐┌──┐┌──┐    │     │
│   │ No Firebase needed.  │    │ │ 2││ 3││ 4││ 5│    │     │
│   │                      │    │ └──┘└──┘└──┘└──┘    │     │
│   │ GAME · BLUFF [v]     │    │                      │     │
│   │ MODE · OPEN CALL [v] │    │ NAMES                │     │
│   │                      │    │ P1 [Ishu___________] │     │
│   │                      │    │ P2 [Ria____________] │     │
│   │                      │    │ P3 [Ari____________] │     │
│   └──────────────────────┘    └──────────────────────┘     │
│                                                              │
│              [  DEAL · PASS TO PLAYER 1  ]                  │
│                  (centered horizontally)                     │
└─────────────────────────────────────────────────────────────┘
```

- Two-column form. Left col = description + game/mode dropdowns. Right col = player count pills + name editors.
- Primary CTA centered horizontally, vertically positioned ~70% down. **Not** bottom-pinned.
- Player count pills: 2/3/4/5. Selected = accent-filled; unselected = bordered. Border/fill swap, no alpha.
- Name rows: P{n} label + EditText. RecyclerView so it grows/shrinks with the count.
- On Continue: build `HotSeatConfig` (already exists), launch the relevant table activity with `EXTRA_HOT_SEAT_CONFIG` extra.

### 3.2 PassTheDeviceActivity (landscape, transparent)

Full-screen modal-style activity launched between every player's turn. Owns the device-handoff gate.

```
┌─────────────────────────────────────────────────────────────┐
│                                                              │
│                  PASS THE DEVICE                             │
│                                                              │
│              Player 1 — your turn                            │
│                                                              │
│   Hand will appear after you continue. Hide the screen       │
│   from other players first.                                  │
│                                                              │
│         [  I'M PLAYER 1 — CONTINUE  ]                        │
│              (centered, ~midline)                            │
│                                                              │
│              Player 1 of 3                                   │
└─────────────────────────────────────────────────────────────┘
```

Theme-tinted opaque background — **no blur, no transparency** (must hide previously-visible cards). Killable only by the named player's Continue tap. Back button suppressed (`onBackPressed` no-op).

Implementation: `Intent` extras `EXTRA_PLAYER_NAME`, `EXTRA_PLAYER_INDEX`, `EXTRA_PLAYER_COUNT`. Returns `RESULT_OK` on Continue.

`TeenPattiActivity` and `BluffActivity` start this activity via `startActivityForResult` between turns when in hot-seat mode. After `RESULT_OK`, they reveal the hand via `localUid = engine.currentTurnUid()`.

### 3.3 BluffActivity polish (landscape, already exists per memory.md)

Refer to `ui_kits/cardclash_app/screens.jsx` `BluffTableScreen` for the layout intent.

- Opponents along top, two-line slot (avatar + name + card-count + 4-card mini-back row).
- Center pile: claim chip ("RIA CLAIMED · 3 KINGS") above a 3-card stack of card-backs, "14 IN PILE · ROUND 3" caption below.
- Local player hand strip horizontally centered along the bottom — cards are draggable up to "select" (translateY -12dp).
- Action column **anchored to the right edge, vertically centered**. PLAY / CALL BLUFF / PASS / EXIT. PLAY is disabled until ≥1 card is selected and the rank chooser is filled.
- Show local player's name + selected count above the hand: `PLAYER 1 — YOUR TURN · 0 SEL`.

### 3.4 TeenPattiActivity reskin

Same intent as Bluff but with the Teen Patti table. Existing impl is mostly correct — verify:
- Action column on the right edge, vertically centered.
- Side-show button surfaced when legal (currently buried).
- Variation chip in the top bar showing the active variation (`AK47` etc.).
- Hot-seat mode: between each player's action, finish the turn → show pass-gate → re-enter the activity with the next `localUid`.

---

## 4. Asset import (do this first, it unblocks everything)

Copy from this skill into `app/src/main/res/`:

| From skill | To `res/` | Purpose |
|---|---|---|
| `assets/logo_cardclash.svg` | `drawable/ic_logo_cardclash.xml` | Use Android Studio's "New > Vector Asset" to convert SVG → VectorDrawable |
| `assets/suits/spade.svg` etc. | `drawable/ic_suit_*.xml` | Per-theme tints applied via `theme.colorCardBlack()` / `colorCardRed()` |
| `fonts/PressStart2P-Regular.ttf` | `font/press_start_2p.ttf` | Balatro display |
| `fonts/VT323-Regular.ttf` | `font/vt323.ttf` | Balatro body |
| `fonts/Inter-*.ttf` | `font/inter*.ttf` | Dev + Neon Pulse body |
| `fonts/PlayfairDisplay-*.ttf` | `font/playfair_display*.ttf` | Royal Oak display |
| `fonts/Lora-*.ttf` | `font/lora*.ttf` | Royal Oak body |
| `fonts/Orbitron-*.ttf` | `font/orbitron*.ttf` | Neon Pulse display |
| `fonts/JetBrainsMono-*.ttf` | `font/jetbrains_mono*.ttf` | Numerics |

Define these in `res/font/` as font families; reference via `theme.fontDisplay()` / `theme.fontBody()` / `theme.fontMono()` accessors on the `Theme` interface (add these methods if not present).

---

## 5. Token mapping (Theme interface ↔ CSS vars)

Add these accessors to the `Theme` interface (Java) so layouts can route everything through the active theme:

```java
@ColorInt int colorBg();          // --bg
@ColorInt int colorSurface();     // --surface
@ColorInt int colorSurfaceAlt();  // --surface-alt
@ColorInt int colorFg1();         // --fg-1
@ColorInt int colorFg2();         // --fg-2
@ColorInt int colorFg3();         // --fg-3
@ColorInt int colorAccent();      // --accent
@ColorInt int colorAccentOn();    // --accent-on
@ColorInt int colorBorder();      // --border
@ColorInt int colorWin();         // --win
@ColorInt int colorLose();        // --lose
@ColorInt int colorWarn();        // --warn
@ColorInt int colorCardFace();
@ColorInt int colorCardRed();
@ColorInt int colorCardBlack();

@FontRes int fontDisplay();
@FontRes int fontHeading();
@FontRes int fontBody();
@FontRes int fontMono();

@DrawableRes int btnPrimaryBg();
@DrawableRes int btnSecondaryBg();
@DrawableRes int cardFaceBg();
@DrawableRes int cardBackBg();
@DrawableRes int chipBg(int denomination);
@DrawableRes int tableBg();
@DrawableRes int pillSelector();   // for SchemaRenderer fixes

float radiusBtnDp();
float radiusCardDp();
float radiusSurfaceDp();
int   borderWidthDp();
```

Each theme class returns the values from `colors_and_type.css`. For the four themes the canonical hexes are:

- **Developer** — bg `#000`, fg-1 `#FFF`, accent `#FFF`, accent-on `#000`, border `#FFF`, radius-btn `0`, border-w `2dp`.
- **Royal Oak** — bg `#1A0F08`, surface `#26170D`, fg-1 `#F4E9D6`, accent `#C9A24B`, accent-on `#1A0F08`, win `#7BB07A`, lose `#9E2A1F`, radius-btn `10dp`, border-w `1dp`.
- **Neon Pulse** — bg `#0A0512`, surface `#160A2A`, fg-1 `#F0EAFF`, accent `#00D4FF`, accent-on `#0A0512`, win `#00D4FF`, lose `#FF1E8E`, radius-btn `14dp`, border-w `1dp`.
- **Balatro** — bg `#1B2D24`, surface `#2D2E33`, fg-1 `#F5EFE2`, accent `#E69020`, accent-on `#FFF`, win `#7AC080`, lose `#D85A3E`, radius-btn `2dp`, border-w `2dp`, hard-shadow-offset `2dp`.

---

## 6. Dialog + system-element polish

Apply theme tokens to:

- **AlertDialog** (Reference, Help, Theme picker): wrap with `ContextThemeWrapper` + `Theme.CardClash.{Theme}.Dialog` style. Title in `fontDisplay`, body in `fontBody`, buttons in `fontHeading` ALL CAPS. Background = `colorSurface`.
- **Snackbars** (rebuy events, errors): tinted background = `colorAccent` for info, `colorLose` for errors. Action button text = `colorAccentOn`.
- **EditText**: underline removed; replaced by 2dp border with corner radius `radiusBtnDp`.
- **Spinner** dropdowns: replace stock arrow with the suit-glyph chevron, `fontMono` 13sp items.

---

## 7. Build order for this round

1. Import fonts + assets (Section 4) — 15 min, unblocks everything.
2. Add `DevTheme` (Section 1) + flip default to it (Section 1 last paragraph).
3. Fix `SchemaRenderer` selection bug (Section 2). After this you can actually test the app.
4. Build `HotSeatSetupActivity` (Section 3.1) — landscape, two-column.
5. Build `PassTheDeviceActivity` (Section 3.2).
6. Wire `BluffActivity` to use both, fix layout per Section 3.3.
7. Wire `TeenPattiActivity` to use both, fix layout per Section 3.4.
8. Add `BalatroTheme` (Section 1) — last because it depends on every drawable being tokenized.

Royal Oak and Neon Pulse stay frozen — those are the team's polish work. Don't touch their classes.

---

## 8. Out of scope for this round (don't build)

- Variation queue drag-and-drop. Up/down buttons only (Section 2 last paragraph).
- Poker engine.
- Multi-device Firebase game-state sync.
- Sounds.
- Win/loss animation sequences.
- Cosmetic shop, leaderboards, in-game chat.

---

## 9. Acceptance checklist

The PR is mergeable when:

- [ ] All four themes selectable from `ProfileActivity`. Each re-skins every screen and dialog without crash.
- [ ] First-run default = Developer.
- [ ] Hot Seat: pick game + mode, count, names → enter game; every turn passes through `PassTheDeviceActivity`; Bluff round playable end-to-end on a single device.
- [ ] Bluff and Teen Patti tables in landscape with action column right-edge centered.
- [ ] No `@color/` or `@font/` references in layout XML for tokens that should be theme-routed.
- [ ] No emoji in any string resource. No Unicode `♠♥♦♣`.
- [ ] Every selection state communicated by border + fill, not alpha.
- [ ] All landscape activities locked via manifest, all portrait activities locked via manifest, none left at `unspecified`.

When all boxes tick, drop the tested APK in the chat and we'll do a screen-by-screen design review against the mocks in `ui_kits/cardclash_app/`.
