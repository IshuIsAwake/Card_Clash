# CardClash — Implementation Memory (state at end of session)

This is a snapshot of what has been built. Pair with `context.md` for next steps.
Spec lives in `instructions.md` (authoritative).

Date of this snapshot: 2026-05-10.

---

## Build state

- `./gradlew assembleDebug` — passes
- `./gradlew test` — passes (HandEvaluatorTest, SidePotCalculatorTest, **PokerHandRankerTest**, **PokerEngineTest** — 21 cases total)
- `./gradlew installDebug` — installs on `SM-S921B` (Android 16) cleanly
- Min SDK 26, Java 11, package `com.example.cardclash`

## Repository structure

```
app/src/main/java/com/example/cardclash/
  CardClashApp.java
  core/
    models/        Card, Suit, Rank, Player, RoomConfig, RoomState, RoomStatus,
                   Action, ActionResult, RebuyRequest, GameType
    engine/        GameEngine, Variation, HandEvaluator, HandRank, HandResult
    settings/      RuleSchema, Setting, SettingType
    theme/         Theme, ThemeId, ThemeRegistry (default = BALATRO),
                   ThemePrefs, ThemeApplier
    network/       FirebaseRoomSync, RoomCodeGenerator
    hotseat/       HotSeatConfig (process-scoped roster + rule overrides)
  games/
    GameDefinition, GamesRegistry
    teenpatti/     engine + ui — FULL
    bluff/         engine + ui — STUB (submit() returns fail; placeholder UI)
    poker/         engine + ui — FULL  (see "Poker" section below)
  themes/
    dev/           DevTheme (B&W testing skin)
    royaloak/      RoyalOakTheme
    neonpulse/     NeonPulseTheme
    balatro/       BalatroTheme (DEFAULT; pickled-green felt + orange accents)
  ui/
    auth/          SplashActivity, LoginActivity, RegisterActivity
    home/          HomeActivity, ProfileActivity
    room/          CreateRoomActivity, PreGameSettingsActivity, JoinRoomActivity,
                   LobbyActivity, SchemaRenderer
    hotseat/       HotSeatSetupActivity, PassTheDeviceActivity, PassGate,
                   PassAndPlayPickerActivity (new — portrait stepper Step 1+2)
    common/        ThemedActivity, CardView, ChipStackView, PlayerSlotView,
                   CollapsiblePanel (reusable), ChipRackView (reusable)

app/src/test/java/com/example/cardclash/
  core/engine/     HandEvaluatorTest, SidePotCalculatorTest
  games/poker/engine/  PokerHandRankerTest, PokerEngineTest
```

## Poker — FULL (this is the flagship game)

Engine: `games/poker/engine/PokerEngine.java`. Texas Hold'em, deterministic, no
Android imports. Side-pot calculation via `SidePotCalculator` (production +
tested). Hidden-info filtering on `snapshot()`.

### Phases
`POSTING_BLINDS → PRE_FLOP → FLOP → TURN → RIVER → SHOWDOWN → ROUND_OVER`

### Actions
- `FOLD`, `CHECK`, `CALL`, `RAISE` (payload `amount` = total this-street commitment), `ALL_IN`
- `POST_BLIND` — used in `MANUAL` blind mode; SB then BB tap it on their seat
- `DEAL` — used in `HOST_DEAL` blind mode; host taps to auto-post both blinds
- `NEXT_ROUND` — start the next hand once the round is over
- `END_ROUND` (host) — deals out remaining streets and runs showdown
- `BUY_IN` (host) — adds chips to a player; queued mid-round, applied immediately if round over
- `REQUEST_BUY_IN` — any player; surfaces a `buy_in_request` event for the host

### Settings (`PokerSchema.java`)
- `small_blind` slider (default 25)
- `big_blind` slider (default 50)
- `buy_in` slider (default 5000) — also drives starting chips in pass-and-play
- `blind_mode` enum picker — `MANUAL` (default) | `HOST_DEAL`
- `min_raise_mode` enum picker — `NONE` (default — any raise above current bet legal),
  `BB`, `BB_2X`, `BB_3X`, `MATCH_LAST`
- `turn_timer` int picker (0/15/30/45/60s; **default 0 = no auto-action**)
- `round_count` slider (0 = open-ended)
- `host_can_end_round` toggle
- `host_can_buy_in` toggle

### Action log
Engine retains the last 3 rounds of actions in a `LinkedList<LoggedAction>`.
Surfaced via `engine.actionLog()`; rendered in the in-game host menu under
"Action history".

### UI: `games/poker/ui/PokerActivity.java`
Programmatic landscape layout. Immersive mode (status + nav bars hidden) so
system bars don't overlap the felt.

- **Top bar (always visible)**: ROOM code · PHASE · BET · STACK info chips · timer pill · MENU.
- **Left dock**: slim collapsible opponents column (`CollapsiblePanel` edge=LEFT, 120dp).
  Each row is name + dealer/SB/BB tag + chips + this-street bet + state (FOLD/ALL-IN).
  At showdown, reveals the eligible players' hole cards inline.
- **Felt**: 5 community cards (50×72dp) over a centered POT box (26sp number).
  Showdown banner appears below the pot.
- **Bottom strip**: 2 hole cards · player name · best-hand label · ChipRackView (denominated stacks).
- **Action row**: FOLD · CHECK/CALL · RAISE · ALL-IN.
  In POSTING_BLINDS phase, this row swaps to POST BLIND or DEAL HAND on the active seat.
- **Raise dialog** (`BetBuilder` inside an AlertDialog): per-row chip controls
  `[chip] [denom] [-] [count] [+]` for 25/100/500/1000/5000. Quick-fills: MIN /
  POT / ALL-IN / CLEAR. RAISE / CANCEL are the dialog's positive/negative buttons
  (always visible regardless of screen height).
- **Host menu** (gear/MENU button): Action history · Hand rankings · How to play ·
  Theme · End round now · Approve buy-ins · Grant chips · Toggle opponents dock · Exit.
- **Back button**: confirmation dialog ("Leave game?" → Leave / Stay) wired via
  `OnBackPressedDispatcher` (works on Android 13+ back gestures).
- **Demo (vs bots)**: launch with no `EXTRA_ROOM_ID`. Bots in MANUAL mode also
  submit `POST_BLIND` for their seat. Heuristic: Chen on hole cards preflop,
  hand-rank percentile postflop, pot-odds gating.
- **Hot-seat (Pass and Play)**: rotates `localUid` between turns through
  `PassTheDeviceActivity`.

### Bot policy: `PokerBotPolicy.java`
Chen formula on hole cards preflop; hand-strength category postflop; pot-odds
for calls. 60% call · 25% raise · 15% fold under pressure, with stack/odds
adjustment.

### Tests
- `PokerHandRankerTest` — 5 cases (board plays, flush, wheel, SF > quads, kicker tiebreak)
- `PokerEngineTest` — 10 cases inc. blind posting, fold-around, full check-down
  to showdown, raise-rejected-below-min, raise reopens action, all-in side pot,
  host END_ROUND, host BUY_IN at round-over, dealer rotation, non-host blocked

## Pass and Play (renamed from "Hot Seat")

Stepper flow:
1. **Step 1+2 (portrait)** — `PassAndPlayPickerActivity`: list of game cards.
   Tap to expand description + hand-rankings reference + SELECT button.
2. **Step 3 (portrait)** — `HotSeatSetupActivity` with `EXTRA_PRESELECTED_GAME`:
   player count picker + per-player names + game's `RuleSchema` rules editor.
   Initial chip stack honors `buy_in` from the schema.
3. **Step 4 (landscape)** — `PassTheDeviceActivity` between turns:
   "I'M {NAME} — CONTINUE" button. Back swallowed via OnBackPressedDispatcher
   so misclicks/gestures can't skip past the gate.

User-facing "Hot Seat" labels were renamed to "Pass and Play" everywhere
(Login, Home card, Bluff label, setup top bar, error toasts).

## Themes

- Default: **Balatro** (`ThemeRegistry.defaultTheme()` returns BALATRO; was DEVELOPER).
- Theme picker mid-game in PokerActivity re-skins the layout in place — no
  `recreate()`, no state loss. Other tables still call `recreate()` (TODO if needed).

## Reusable UI components

- `ui/common/CollapsiblePanel.java` — wraps content with a chevron handle on a
  configurable edge (LEFT/RIGHT/TOP/BOTTOM); `LayoutTransition` slide animation.
- `ui/common/ChipRackView.java` — denominated chip stacks (25/100/500/1000/5000).
  Greedy breakdown; programmatic colored discs (with theme drawable + tint
  fallback); abbreviated total label.

## What works end-to-end right now

1. Auth (Email/Password) → Home.
2. Home → "Pass and Play" card → portrait game picker → portrait setup →
   landscape pass screen → game table.
3. Demo (vs bots) reachable via Create Room → game flow with no room id, OR via
   Pass and Play with bot players (manual setup).
4. Teen Patti table — 4-player demo + hot-seat both work.
5. **Poker** table — full Hold'em hand flow, manual blind posting, side pots,
   host menu, action history, chip-tap raise dialog, immersive mode.

## What is stub / not done

- **Bluff** engine + UI — still STUB. `submit()` returns fail; placeholder UI.
  Spec for it lives in `context.md` (carried forward).
- **5-Card Draw** poker variant — `PokerVariation` interface exists; only
  `HoldemVariation` is implemented. The schema's `variant` setting was removed
  (only Hold'em ships).
- **Multi-device Firebase game-state sync** — engine state stays host-local.
  `GameEngine.snapshot()` exists but isn't wired through `FirebaseRoomSync`
  for poker (or any game) yet. Blocked on Firebase project ownership.
- **Per-denomination chip drawables** — themes still use a single chip drawable;
  ChipRackView/BetBuilder render programmatic colored discs as a fallback.
  Add `bg_chip_{25,100,500,1000,5000}_<theme>.xml` if you want themed chips.
- **Variation queue editor drag-reorder** — still read-only.
- **Sounds + Lottie assets** — Theme interface declares hooks, all themes
  return 0/null.
- **Other tables (TeenPatti, Bluff)** still use `recreate()` for theme change
  and don't have immersive mode or back-press confirmation. Port from poker
  if desired.

## Decisions taken

- Default theme: BALATRO (was DEVELOPER).
- `min_raise_mode` default: `NONE` (casual — any raise above current bet legal).
  Standard poker `MATCH_LAST` is available as a picker option.
- `turn_timer` default: `0` (no auto-action). Manual blind posting is the
  default, so automation is off across the board unless the host opts in.
- `blind_mode` default: `MANUAL` (each blind player taps POST BLIND).
- Pass-and-Play setup screens: portrait. Game tables: landscape.
- Theme change in poker: re-skin in place via inline `buildUi(...)` rebuild.
  No `recreate()`.
- Raise UI: per-row chip controls with +/- buttons; commit via the AlertDialog's
  positive button so it's always visible regardless of screen height.
- Reverted from "tap-disc-to-add" chip layout (cluttered) to row-based +/-.

## Firebase

- `app/google-services.json` points at project owned by a friend. **No write
  access to Database / Auth rules.** Multi-device sync work is parked until
  the user migrates to their own Firebase project.
- Pass-and-Play is the primary playable surface; Demo-vs-bots is the secondary.

## Auto-memory (separate, for the AI)

- `~/.claude/projects/-home-ishu-Projects-Playground-CardClash/memory/MEMORY.md`
  may contain background context from prior sessions.
