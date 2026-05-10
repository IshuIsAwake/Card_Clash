# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

CardClash is an Android (Java, viewBinding, minSdk 26 / compileSdk 36) multiplayer card-game app. Firebase Realtime Database + Firebase Auth back the online flow; Google Sign-In, ZXing (QR), Glide, Lottie are the other notable deps. Package root: `com.example.cardclash`.

The user does not own the Firebase project (a friend does). Multi-device Firebase sync work is out of scope until they migrate. Use **hot-seat** (single-device pass-and-play) and **demo (vs bots)** code paths for local testing.

## Build / test commands

```
./gradlew assembleDebug      # build debug APK
./gradlew installDebug       # install on connected device
./gradlew test               # JVM unit tests (engine logic)
./gradlew :app:testDebugUnitTest --tests "com.example.cardclash.core.engine.HandEvaluatorTest"  # single test
./gradlew connectedAndroidTest   # instrumented tests
./gradlew lint
```

There is no `assemble` wrapper script — use Gradle directly. `local.properties` holds the SDK path; `app/google-services.json` is required for Firebase to link.

## Architecture

### Game engine layer (headless, Android-free)

`core/engine/GameEngine` is the contract every game implements. **The engine never imports Android.** UI talks to it only through `submit(Action) -> ActionResult` and observes via `GameEngine.Listener.onStateChanged()`. Engines must be **deterministic** given the same `(seed, action sequence)` so that Firebase replay reconstructs identical state via `snapshot()` / `restore()`.

Per-game engines live under `games/<game>/engine/` (Teen Patti is the reference implementation; Poker and Bluff are stubs). Each game directory mirrors the same shape:

- `<Game>Engine.java` — implements `GameEngine`
- `<Game>Schema.java` — `RuleSchema` describing tunable settings
- `<Game>HandRanker.java` — pure hand-eval helpers
- `<Game>Variation.java` interface + concrete strategy classes (e.g. `ClassicVariation`, `MuflisVariation`, `AK47Variation`, `Variation1942`, `JokerWildVariation`) — variations are queueable; the queue is round-rotated.

Models in `core/models/` (`Action`, `ActionResult`, `Card`, `Player`, `RoomConfig`, `RoomState`, etc.) are POJOs shared across engine + network layers.

### Settings / `RuleSchema` + `SchemaRenderer`

Adding a setting to a game's `RuleSchema` (`core/settings/`) automatically renders a row in the pre-game screen via `ui/room/SchemaRenderer`. **Do not hand-author per-game settings UI.** Setting types include int picker, enum picker, toggle, and a list editor for the variation queue.

### Theme system

Themes are full skins (colors, typography, drawables, sounds, Lottie). Active theme lives behind the `core/theme/Theme` interface. Concrete themes are under `themes/<id>/` (`balatro`, `royaloak`, `neonpulse`, `dev`) and registered in `ThemeRegistry`. `ThemePrefs` persists the user's choice; `ThemeApplier` + `ui/common/ThemedActivity` apply the chosen `appStyle()` and themed views resolve colors/fonts via the `Theme` interface.

**Rule:** No `R.color.*` / `R.drawable.*` direct lookups outside the active `Theme` implementation. UI code asks the theme.

The "dev" theme exists specifically for testing — pure black/white, loud selected/disabled states. Royal Oak and Neon Pulse are owned by other team members; **do not touch them**.

### Networking

`core/network/FirebaseRoomSync` is the only Firebase RTDB writer/reader the UI uses; `RoomCodeGenerator` produces room codes. Schema lives in `docs/firebase-schema.md`. The engine writes snapshots; clients restore from them. Hidden information (e.g. opponents' hidden cards, true ranks of bluff plays) must not leak through `snapshot()` to non-owner clients.

### UI flow

- `ui/auth/SplashActivity` → Login/Register (Firebase + Google Sign-In).
- `ui/home/HomeActivity` is the hub: online room create/join, **Hot Seat** entry point, demo-vs-bots entry point.
- `ui/room/`: create/join/lobby + `PreGameSettingsActivity` (which hosts `SchemaRenderer`).
- `ui/hotseat/`: `HotSeatSetupActivity` (player count + name entry), `PassTheDeviceActivity` / `PassGate` (full-screen privacy gate between turns), backed by `core/hotseat/HotSeatConfig` (singleton holding player roster + active uid pointer).
- Game tables live with their game, e.g. `games/teenpatti/ui/TeenPattiActivity`. A table activity launched without `EXTRA_ROOM_ID` runs in **demo (vs bots)** mode. In hot-seat mode it accepts a flag and rotates `localUid` to `engine.currentTurnUid()` after each submitted action, gating with `PassGate`.
- `ui/common/`: shared themed views (`CardView`, `ChipStackView`, `PlayerSlotView`, `ThemedActivity`).

## Project-specific conventions

- **Comments:** default to none. Only write a comment for non-obvious *why* (constraint, invariant, workaround). Don't explain what the code does.
- **Engine purity:** anything under `core/engine/` or `games/*/engine/` must compile without the Android SDK on the classpath.
- **Determinism:** never use `Math.random()` or wall-clock time inside engine code; use the seeded `Random` initialized in `initialize(...)`.
- **Hidden info:** when adding game state that includes hidden cards/claims, audit the `snapshot()` output to make sure it's filtered per-recipient before reaching clients.

## Source-of-truth docs in this repo

- `instructions.md` — authoritative spec (read first when scope is unclear)
- `memory.md` — running inventory of what's built
- `context.md` — current near-term goals and what is explicitly out of scope
- `docs/firebase-schema.md` — RTDB layout
- `CardClash Design System/` — design tokens / handoff for themed UI
