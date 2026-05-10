# CardClash — Next Session Instructions

Read `memory.md` first for the inventory of what already exists. Read
`instructions.md` for the authoritative spec. This file = what to do next.

Last touched: 2026-05-10. The current build is installable on the user's
device (`SM-S921B`, Android 16). Poker is the flagship and is fully playable
in pass-and-play and demo-vs-bots. Teen Patti is also fully playable. Bluff
is still a stub.

---

## Open issues from the user's last play session

User's latest feedback may include any of these — do NOT assume they are
resolved without verifying in the running app:

1. **Pot-display fit** — pot number font reduced to 26sp, community cards
   shrunk to 50×72dp; should fit, but verify on the actual device.
2. **Raise dialog overflow** — `BetBuilder` is now wrapped in a `ScrollView`
   inside an `AlertDialog`, with RAISE/CANCEL on the dialog's button bar so
   they're always visible. Verify on the device.
3. **System bar overlap** — `PokerActivity` and `PassTheDeviceActivity` now
   use immersive mode (status + nav bars hidden, swipe to reveal transient).
   Verify the screen edges aren't being clipped.
4. **Pass screen continue button missing** — fixed: it had `layout_width="0dp"`
   inside a LinearLayout (ConstraintLayout-only `constraintWidth_percent` was
   silently ignored, so the button was 0px wide). Now `match_parent` with
   `minHeight=56dp`. Verify visible.
5. **Back-press confirmation** — wired via `OnBackPressedDispatcher`. Test
   that pressing back gestures on Android 13+ shows the "Leave game?" dialog.

If user reports any of these are still broken, debug from the screenshot
they provide. Don't take "fixed" on the previous turn as ground truth.

---

## Carried-over goals (still open from prior sessions)

### 1. Bluff engine + UI

User specifically wanted to play Bluff. Engine is still a stub.

- Engine: deal 52 across N players, leftover to first players in seat order.
  State: hands per uid, central pile, last-claim (rank + count + true cards),
  turn pointer.
- Actions: `PlayCards(rank, count, cardIds)`, `CallBluff(targetActionId)`,
  `Pass` (next-only mode only).
- Two variations: `OPEN_CALL` (any player may call before next play resolves)
  and `NEXT_ONLY` (only next player; once they play, prior claim locks). Wire
  via a `BluffVariation` strategy interface.
- Validate the player owns the cards being claimed. Hidden-info: stored true
  ranks must NOT leak through `snapshot()` to non-claimer clients (until a
  call resolves). For pass-and-play this manifests as the pass gate hiding
  the just-played cards.
- UI: pile/discard center, "Play X cards as Y" composer, "Bluff!" call button.
  Mirror the Poker table layout (immersive mode, collapsible left dock,
  bottom action row, AlertDialog for compose). Use the existing
  `CollapsiblePanel` and `ChipRackView` shared components.

### 2. Multi-device Firebase game-state sync

Blocked on Firebase project ownership (project belongs to a friend; user
can't edit DB rules). When unblocked:

- Wire `GameEngine.snapshot()` writes through `FirebaseRoomSync` after each
  state change.
- `restore()` on late-joiners and reconnects (currently a stub for poker).
- Hidden-info filter: opponents' hole cards must be masked per-recipient
  before going on the wire. The `holesPublic` flag is in the snapshot —
  the network layer needs to honor it.

### 3. Variation queue editor drag-reorder

`SchemaRenderer.listEditor()` renders read-only with up/down/remove buttons
already wired but no drag handle. Spec wants drag-reorder. Phase 2.

### 4. Per-denomination chip drawables

ChipRackView and BetBuilder currently draw colored ovals programmatically
(or tint a single theme drawable). For visual polish, ship per-denom
drawables: `bg_chip_{25,100,500,1000,5000}_{dev,balatro}.xml` and have
`Theme.chipBg(denomination)` route to them. Royal Oak / Neon Pulse owned
by other team members — don't touch.

### 5. Apply Poker UX improvements to Teen Patti

Poker now has: immersive mode, OnBackPressedDispatcher confirmation, in-place
theme change (no `recreate()`), action history menu, `CollapsiblePanel`
opponents dock, chip-tap raise dialog. Teen Patti is still on the older
pattern. Port over once stable.

---

## What NOT to do

- Don't touch Royal Oak or Neon Pulse themes — owned by team members.
- Don't implement Firebase multi-device sync until the user migrates the
  project. State so up front if asked.
- Don't add comments for what code does — only for non-obvious *why*.
- Don't add automation: humans tap everything (POST BLIND, RAISE, etc.). The
  default `turn_timer` is 0 and `blind_mode` is `MANUAL` for this reason.
- Don't add cosmetic shop, leaderboard, game history, or chat. Out of v1 scope.

---

## Useful pointers

- **Demo (vs bots)** for Poker: Create Room → Poker (no room id path).
- **Pass and Play**: Home → Pass and Play card → portrait stepper.
- **Hand evaluation** is shared (`core/engine/HandEvaluator.bestOf7`). Poker's
  `PokerHandRanker.evalHoldem` is a thin facade.
- **Side pots** are computed via `SidePotCalculator.compute(commitments, notFolded)`.
  Production-tested. Reuse for any all-in game.
- **Adding a setting** to a game's `RuleSchema` automatically renders a row in
  the pre-game settings UI via `SchemaRenderer`. Don't hand-author per-game settings UI.
- **Adding a game** = create a `GameDefinition`, register in `GamesRegistry`.
- **Theme resource lookups** must go through the `Theme` interface — never
  `R.color.*` / `R.drawable.*` directly outside theme classes.
- **CollapsiblePanel** + **ChipRackView** are reusable; pick them up for any
  new table UI.
- Build commands:
  - `./gradlew assembleDebug` — APK build
  - `./gradlew test` — JVM unit tests
  - `./gradlew :app:testDebugUnitTest --tests "com.example.cardclash.games.poker.engine.*"` — poker tests only
  - `./gradlew installDebug` — install on connected device
