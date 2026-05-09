# CardClash — Implementation Memory (state at end of session)

This is a snapshot of what has been built. Pair with `context.md` for next steps.
Spec lives in `instructions.md` (authoritative).

---

## Build state

- `./gradlew assembleDebug` — passes
- `./gradlew test` — passes (HandEvaluatorTest, SidePotCalculatorTest)
- Min SDK 26, Java 11, package `com.example.cardclash` (preserved from skeleton)

## Repository structure

```
app/src/main/java/com/example/cardclash/
  CardClashApp.java                   # Application class (Firebase init)
  core/
    models/        Card, Suit, Rank, Player, RoomConfig, RoomState, RoomStatus,
                   Action, ActionResult, RebuyRequest, GameType
    engine/        GameEngine, Variation, HandEvaluator, HandRank, HandResult
    settings/      RuleSchema, Setting, SettingType
    theme/         Theme, ThemeId, ThemeRegistry, ThemePrefs, ThemeApplier
    network/       FirebaseRoomSync, RoomCodeGenerator
  games/
    GameDefinition, GamesRegistry
    teenpatti/
      engine/      TeenPattiEngine (FULL), TeenPattiSchema, TeenPattiHandRanker,
                   TeenPattiVariation + 5 impls (Classic, AK47, 1942, Muflis, JokerWild)
      ui/          TeenPattiActivity (FULL, demo mode w/ 3 bots)
    bluff/
      engine/      BluffEngine (STUB — submit() returns fail), BluffSchema
      ui/          BluffActivity (placeholder "Coming in Phase 3")
    poker/
      engine/      PokerEngine (STUB), PokerSchema, SidePotCalculator (FULL + tested)
      ui/          PokerActivity (placeholder)
  themes/
    royaloak/      RoyalOakTheme (reference; warm wood + green felt + brass + serif)
    neonpulse/     NeonPulseTheme (cyan/magenta/purple, monospace numerics)
  ui/
    auth/          SplashActivity, LoginActivity, RegisterActivity
    home/          HomeActivity, ProfileActivity
    room/          CreateRoomActivity, PreGameSettingsActivity, JoinRoomActivity,
                   LobbyActivity, SchemaRenderer
    common/        ThemedActivity (base), CardView, ChipStackView, PlayerSlotView

app/src/main/res/
  layout/          activity_splash, _login, _register, _home, _profile,
                   _create_room, _pregame_settings, _join_room, _lobby,
                   _teen_patti, item_player
  values/          colors.xml (ro_* + np_* prefixed palettes), strings.xml (minimal),
                   themes.xml (Theme.CardClash.RoyalOak[.Splash|.Game], Theme.CardClash.NeonPulse[...])
  drawable/        bg_table_{royaloak,neonpulse}, bg_card_back_{royaloak,neonpulse},
                   bg_card_face, bg_chip_stack_{royaloak,neonpulse},
                   bg_btn_primary_{royaloak,neonpulse},
                   bg_pot_indicator_{royaloak,neonpulse}, bg_surface_card,
                   ic_logo_cardclash

app/src/test/java/com/example/cardclash/core/engine/
  HandEvaluatorTest.java               # 6 cases inc. wheel, wilds, bestOf7
  SidePotCalculatorTest.java           # main pot, side pot, folded eligibility

docs/firebase-schema.md                # RTDB schema + suggested rules
```

## What works end-to-end right now

1. **Auth** — Firebase Email/Password sign-up & sign-in (Splash → Login/Register → Home).
   Display name stored on FirebaseUser + at `/users/{uid}/displayName`.
2. **Home** — Create Room / Join Room / Profile buttons. No Shop/Leaderboard nav (per spec).
3. **Profile** — display name editor, theme picker (radio group), sign out.
4. **Create Room** flow:
   - Pick game (Teen Patti / Bluff / Poker)
   - PreGameSettings: data-driven from each game's `RuleSchema` (slider/picker/toggle/enum/list rows)
   - On Continue: writes `/rooms/{6-digit-code}/{meta,players}` to Firebase RTDB.
5. **Lobby** — observes `/rooms/{id}` via `FirebaseRoomSync.observeMeta`. Renders QR
   (zxing `BarcodeEncoder`), 6-digit code, player list (RecyclerView). Host-only Start
   button. When status flips to `IN_PROGRESS`, all clients launch the game's
   `tableActivity` from `GameDefinition`.
6. **Join Room** — 6-digit code entry OR QR scan via zxing `ScanContract`. Creates a
   Player under `/rooms/{id}/players/{uid}`.
7. **Teen Patti table** — `TeenPattiActivity`. Renders the perspective table, opponents
   along the top, center pot, action column, local cards + chip strip.
   - **Demo mode**: launching the activity with no `EXTRA_ROOM_ID` extra spawns 3 bot
     opponents (uids `bot1`/`bot2`/`bot3`) that act on a 60% chaal / 25% raise / 15% fold
     heuristic. Pure on-device, no Firebase.
   - Reference (hand rankings) and Help (rule slides) dialogs work.
   - Settings dialog switches between registered themes; calls `recreate()`.
8. **Theme system** — `ThemeRegistry` holds RoyalOak + NeonPulse. `ThemePrefs` persists
   the active id in SharedPreferences (`cardclash_theme.active_theme_id`).
   `ThemedActivity` base class applies the active style before `setContentView`.
9. **Engine modularity** — `GamesRegistry.register(GameDefinition)` is the single
   add-a-game surface. `RuleSchema` drives settings UI generically.

## What is stub / not done

- **Bluff** engine — only `initialize`/listener stubs; `submit()` returns
  `ActionResult.fail("Bluff engine not implemented yet")`. UI is a placeholder TextView.
- **Poker** engine — same stub state. SidePotCalculator + HandEvaluator are real and
  tested; no game loop yet.
- **Multi-device game-state sync** — `FirebaseRoomSync` syncs lobby (meta + players)
  but the engine state is host-local. `GameEngine.snapshot()/restore()` exists but
  isn't wired to RTDB writes/reads inside `TeenPattiEngine` event flow.
- **Hot seat (single-device pass-and-play)** — not built.
- **Developer / B&W theme** — not built. Only Royal Oak (reference) and Neon Pulse
  exist. Per Theme interface, adding a 3rd is: implement `Theme`, register in
  `ThemeRegistry`, ship the theme's drawables/styles.
- **In-game shell extras** — hidden session leaderboard drawer, rebuy banner,
  chat/emoji rail placeholder are *not* in the table activity yet. Engine + sync
  hooks for rebuy do exist (`FirebaseRoomSync.requestRebuy / resolveRebuy /
  addChips` with a Firebase Transaction).
- **Variation queue editor** — `Setting.LIST_EDITOR` renders read-only in
  `SchemaRenderer.listEditor()`. Drag-reorder is Phase 2.
- **Side-show is partially wired** in Teen Patti engine but not exposed as a button.
- **Sounds + Lottie assets** — Theme interface declares hooks, both themes return
  `0` / `null`. Nothing in `res/raw/`.
- **Round-count enforcement** — `RoomConfig` has `round_count` but the engine plays
  open-ended.

## Decisions taken (delegated by spec section 15)

- Package name: kept `com.example.cardclash`.
- QR payload: room ID alone (6 digits, ~1M space; collision check is a future
  hardening if abuse is observed).
- Min SDK: 26.
- RTDB schema: see `docs/firebase-schema.md`. `meta` / `players` / `gameState` /
  `rebuyRequests` / `events` split so high-frequency game writes don't fan out
  to roster listeners.
- Theme persistence: SharedPreferences (`cardclash_theme` file).
- ThemeApplier strategy: `recreate()` on theme change for v1 (no in-place rebind).
- Bluff/Poker engines stubbed deliberately; HandEvaluator + SidePotCalculator
  written first with unit tests because they're the highest-risk pieces (per spec).

## Firebase

- `app/google-services.json` points at project `cardclash-c6bab`. The user does
  NOT own this project — owned by a friend. **Cannot configure Auth / Database
  rules** until the user creates their own Firebase project or gets access.
- Until Firebase is sorted, the only playable surface is the Teen Patti demo mode,
  and even that requires going through Login → Home → Create Room flow that hits
  Firebase. **No UI path currently bypasses Firebase to reach the table.**

## Auto-memory (separate from this file)

- `~/.claude/projects/-home-ishu-Projects-University-CardClash/memory/MEMORY.md`
  has `project_cardclash.md` (high-level vision; from a prior session, dated
  2026-05-06). Treat as background context.
