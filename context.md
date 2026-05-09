# CardClash — Next Session Instructions

Read `memory.md` first for the full inventory of what already exists. Read
`instructions.md` for the authoritative spec. This file = what to do next.

---

## Immediate goals (in order)

### 1. Developer theme (B&W, plain, ease-of-use focused)

**Why**: User cannot test the app effectively right now. On the launched APK,
some buttons were not selectable / did not respond. State of toggles, sliders,
and selected options is not obvious enough at a glance. User needs a theme
optimized purely for *testing every button and feature*, not for looks.

**Constraints from the user**:
- Pure black-and-white. No gradients, no decorative drawables, no ornament.
- Buttons, sliders, toggles must be visually loud — large hit-targets, hard
  borders, obvious pressed/disabled states.
- It must be **immediately obvious** when a setting is toggled / a value is
  selected. Currently `SchemaRenderer`'s `intPicker` and `enumPicker` use only
  alpha (1.0 vs 0.5) to show selection — that is too subtle. Use background
  fill + border swap.
- Skeleton (layouts, view IDs, structure) stays the same so teammates can
  swap in their own themes without breaking. Other themes (Royal Oak,
  Neon Pulse) untouched — those are the team's polish work.
- "Looks don't matter much to me. I just want ease of use." — design accordingly.

**Implementation outline** (do not start coding until user confirms):
- Add `ThemeId.DEVELOPER`, `themes/dev/DevTheme.java`, `Theme.CardClash.Dev[.Splash|.Game]` styles.
- Plain white background, black text, black 2dp borders on every interactive
  element. No drawable shadows, no gradients. Sans-serif default.
- Custom selectors for buttons/toggles/etc. with very loud pressed and
  selected states (e.g. selected = solid black bg + white text; unselected =
  white bg + black text + border).
- Set as the default theme if user opens the app for the first time? Probably
  yes for now — easier testing. Confirm with user.
- Audit `SchemaRenderer` and the in-game action buttons for unreachable /
  unstyled controls. Fix any "I couldn't tap that" issues uncovered.

### 2. Hot seat mode (single-device pass-and-play)

**Why**: User wants to test rule logic with multiple human players without
Firebase, since they can't configure Firebase yet (project is owned by a
friend). Also a useful demo fallback if Firebase is flaky on demo day.

**Approach**:
- Add an entry point on the Home screen — e.g. a "Hot Seat" button — that
  bypasses the room/Firebase flow.
- Hot-seat-launch lands directly on a player-count picker (2/3/4/5) +
  display-name entry per player, then enters the chosen game's table
  activity in hot-seat mode.
- Pass via Intent extras or a small `HotSeatConfig` singleton: list of
  `Player` POJOs with synthesized uids (`p1`, `p2`, ...) and the active
  player-uid pointer.
- Modify `TeenPattiActivity` so it accepts a hot-seat mode flag. When set:
  - The "local player" identity rotates after each action — i.e. `localUid`
    becomes `engine.currentTurnUid()` after each turn submit.
  - Between turns, show a full-screen "Pass to {nextName}, then tap" gate so
    the previous player can't see the next player's hand.
  - At showdown, reveal everyone's cards face-up.
- Bots stay available as a separate "Demo (vs bots)" entry point, kept for
  quick smoke-testing.

### 3. Bluff engine + UI

User specifically wants to play Bluff in this round of testing.

- Engine: deal full 52 across N players, leftover to first players in seat
  order. State: hands per uid, central pile, last-claim (rank + count + true
  cards), turn pointer.
- Actions: `PlayCards(rank, count, cardIds)`, `CallBluff(targetActionId)`,
  `Pass` (next-only mode only).
- Two variations: `OPEN_CALL` (any player may call any time before next play
  resolves) and `NEXT_ONLY` (only next player; once they play, prior claim
  locks). Wire via `BluffVariation` strategy interface.
- Validate the player owns the cards being claimed. Hidden-info: stored true
  ranks must not leak through the snapshot to non-claimer clients (until a
  call resolves). For hot-seat mode this manifests as the "Pass to next" gate
  hiding the just-played cards.
- UI: spec calls for pile/discard center, "Play X cards as Y" composer,
  "Bluff!" call button. Mirror the Teen Patti table layout style for
  consistency.

### 4. Polish from this round

- The `SchemaRenderer.listEditor()` currently displays the variation queue
  read-only. Make it reorderable (drag handle or up/down arrows). Adding /
  removing variations is also part of the spec (instructions.md §10.5).
- Add a "Demo (vs bots)" button on Home for quick Teen Patti smoke test
  (bypasses Firebase entirely — same code path as the activity's existing
  demo mode but reachable from the UI).

---

## What NOT to do this round

- Do not touch Royal Oak or Neon Pulse themes. Those belong to the team.
- Do not implement multi-device Firebase game-state sync. User does not own
  the Firebase project. Wait until they have access or migrate to their own.
- Do not implement Poker. Phase 4 in the spec; not on the user's near-term
  list.
- Do not build the cosmetic shop, leaderboard, game history, or chat. Out of
  v1 scope per spec §13.
- Do not add comments for what code does — only for non-obvious *why* (per
  the project's commenting bar).

---

## Useful pointers when picking up

- Demo mode launch path: `TeenPattiActivity` with no `EXTRA_ROOM_ID` extra =
  bots-only run.
- The engine never imports Android. Keep it that way — UI observes via
  `GameEngine.Listener.onStateChanged()`.
- Adding a setting to a game's `RuleSchema` automatically renders a row via
  `SchemaRenderer`. Don't hand-author settings UI per game.
- All theme resource lookups go through `Theme` interface methods — don't
  read `R.color.*` directly outside theme classes.
- Build commands the user has been running:
  - `./gradlew assembleDebug` — APK build
  - `./gradlew test` — JVM unit tests
  - `./gradlew installDebug` — install on a connected device
