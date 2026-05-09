# CardClash — Implementation Instructions for Claude Code

This document is the authoritative spec for v1. Every decision here is locked unless explicitly marked as delegated to Claude Code or as a future hook. Build against this without re-litigating.

---

## 1. Project Overview

CardClash is an Android app that mediates in-person card games. Friends sit around a table physically; cash settles in real life. The cards, chips, dealing, and rule enforcement live inside the app. The phone is a referee, not a casino backend.

v1 ships three games — Teen Patti, Bluff, and Poker — accessible only through a private-room flow. The host creates a room, others join via QR code or 6-digit room code, and everyone plays at the same virtual table with state synced across phones.

The product differentiator is honesty about how friends actually play together: chips are freely manipulatable by the host (so IRL cash-in/cash-out works), rules lock in stone the moment a room starts (no IRL arguments mid-game), and themes let each player skin their own client without affecting anyone else's view.

Audience: 16+, college students and young adults. The visual language is adult and premium, never cartoony.

---

## 2. Hard Constraints

- **Language**: Java only. No Kotlin.
- **UI**: XML layouts + Java view code. No Jetpack Compose.
- **Allowed libraries (syllabus)**: Retrofit, Firebase Auth, Firebase Realtime Database, SQLite, Material Components, MediaPlayer/ExoPlayer, standard sensor/camera/location APIs, AndroidX.
- **Library exception**: Animation and visual-effect libraries are explicitly permitted (Lottie, AndroidViewAnimations, MotionLayout, similar). Use them where they make the UI feel premium. QR code scanning libraries (zxing) also permitted under this exception.
- **No other third-party libraries** without explicit approval. If a needed effect can't be done within these bounds, escalate.
- **Min SDK**: API 24 (Android 7.0) unless syllabus dictates otherwise.
- **Orientation**: game screens are landscape-locked. Auth, home, lobby, room setup, and settings remain portrait. Never mix orientations within a single screen.
- **Package**: preserve the existing skeleton's package name for continuity.

---

## 3. Core Architectural Principles

The lead developer values modularity above almost everything else. "Adding and swapping things in and out should be trivial" is a hard requirement. Concretely:

1. **Adding a new game** = create a new `GameEngine` implementation, add its UI module, register in a games registry. Zero edits to lobby, theme, networking, or auth code.
2. **Adding a new theme** = create a new `Theme` implementation, ship its drawables/sounds, register in theme registry. Zero edits to game logic or screens.
3. **Adding a new game variation** (Teen Patti variation, Bluff variant, Poker variant) = drop in a new `Variation`/`Rule` strategy, register. Game engine is variation-agnostic.
4. **Settings are data-driven**, not hardcoded. The pre-game settings screen renders from the active game's declared `RuleSchema`. Adding a new setting = add it to the schema; UI updates automatically.
5. **Strategy pattern wherever variants exist.** Hand evaluators, turn ordering, win conditions, joker rules — all pluggable strategies, not switch statements on game type.
6. **UI is dumb, engine is smart.** Game logic never lives in Activity/Fragment code. UI observes engine state and renders. UI sends user actions to the engine for validation. Engine never holds Android references.
7. **State is serializable.** Every piece of game state must round-trip cleanly through JSON for Firebase RTDB sync.

These are not guidelines. This is how the project gets built.

---

## 4. Repository State

A skeleton already exists. Inventory:

- **Auth**: splash, login, register
- **Shell**: home, lobby, profile, shop, leaderboard, game_history
- **Rooms**: create_room, join_room, custom_rules
- **Games**: teen_patti, uno
- **Item layouts**: item_card, item_player, item_opponent_slot, item_leaderboard, item_game_history, item_rule_toggle, item_rule_slider

### Keep
- Activity scaffolding and navigation graph.
- Auth flow shell (will be re-themed and wired to Firebase Auth).
- Room create/join activity skeletons.
- `custom_rules` pattern as a reference for the new schema-driven settings screen.
- Item layouts as templates (will be re-themed and re-wired against new models).

### Cut
- All cartoon/kiddish drawables and color palettes — replaced wholesale.
- Uno game implementation — replaced by Bluff.
- Shop activity wiring — kept as a placeholder file for future, but no nav entry in v1.
- Global leaderboard — replaced by an in-game hidden session leaderboard.
- Friendly/Competitive lobby logic — not in v1.

### Rewrite
- Card and chip rendering — replace with motion-driven custom views.
- Teen Patti game logic — rebuild against the new engine abstraction.
- Theme system — does not yet exist; build from scratch.
- Profile screen — strip down to display name, theme picker, sign-out.

---

## 5. Recommended Project Structure

```
app/src/main/java/com/cardclash/
  auth/                    # splash, login, register
  core/
    engine/                # GameEngine interface, shared types
    theme/                 # Theme interface, registry, applier
    network/               # Firebase RTDB wrapper, room sync, listeners
    models/                # Player, Card, Chip, RoomState, Action, etc.
    settings/              # RuleSchema, Setting, presets
  games/
    teenpatti/
      engine/              # TeenPattiEngine + variation strategies
      ui/
    bluff/
      engine/              # BluffEngine + variation strategies
      ui/
    poker/
      engine/
        holdem/
        fivecarddraw/
        shared/            # HandEvaluator, side-pot logic
      ui/
  ui/
    home/
    room/
      create/
      join/                # QR scan + code entry
      lobby/               # waiting room
      settings/            # pre-game settings (schema-driven)
    common/                # CardView, ChipView, PlayerSlotView, ActionButton, etc.
  themes/
    royaloak/              # Theme impl (resources in res/)
    neonpulse/
  utils/
```

`res/` mirrors with subdirectories per game (layouts), per theme (drawables, raw sounds), and shared values (dimensions, type styles).

This is recommended, not prescribed. Claude Code may adapt internal organization within modules, but the top-level boundaries (core / games / ui / themes) are required for the modularity guarantees.

---

## 6. Theme System

A theme is holistic. It owns:

- **Color palette**: background, surface, primary, accent, text variants, status (win/lose/warning).
- **Typography**: display, heading, body, numeric. Neon Pulse uses monospace numerics specifically.
- **Drawables**: table background, card faces, card backs, chip stack styles, button states (normal/pressed/disabled), scenery overlays, dealer button, pot indicator.
- **Sounds**: card flip, chip clink, shuffle, deal, win, lose, button tap, timer warning.
- **Animations**: win sequence, loss sequence, chip-throw, card-deal, card-reveal.

### Architecture
- A `Theme` interface declares accessors for every resource above (color int / drawable resource ID / raw sound ID / Lottie asset path).
- A `ThemeRegistry` holds all available themes, keyed by ID.
- A `ThemeApplier` re-binds the active screen when the theme changes.
- Per-user, persisted in `SharedPreferences`. Default = host's theme on first room join. Changeable anytime from the in-game settings menu without leaving the room.

### v1 Themes

**Royal Oak** (warm-club lane):
- Wooden table top, deep green felt, brass accents, leather seating in scenery.
- Lamp lighting (warm yellow ambient).
- Serif display type (something like Playfair Display or a similar serif from Material's defaults).
- Warm beige/cream numerics.
- Sound palette: muted, woody, analog (chip clinks, card slaps).

**Neon Pulse** (cyber-lounge lane):
- Dark glass surfaces, magenta/cyan/purple neon accents.
- Glowing edges on cards, buttons, table rim — implement with elevation + colored shadow drawables, layer-list compositions, or a glow library under the animation exception.
- Monospace numerics (JetBrains Mono or similar).
- Futuristic CG backdrop bitmap.
- Sound palette: synth, digital, sharp.

The reference mockup (Royal Oak + Neon Pulse poker table) is a creative target, not a pixel-accurate spec. Match the intent — warmth vs neon, perspective table, opponents around the rim, chip rack at bottom — without trying to recreate photorealistic backgrounds verbatim. Background scenery ships as bitmap assets per theme.

### Build approach
The lead developer builds **one theme as the reference template** (decide which based on which is faster to get right). Teammates implement the second theme and any future themes against the same `Theme` interface. The interface is therefore stable from day one — design it conservatively so it doesn't churn.

---

## 7. Game Engine Architecture

Every game implements a `GameEngine` interface responsible for:

- Initializing state from a `RoomConfig` (player roster, rules, buy-in).
- Validating player actions (returns success / specific failure reason).
- Advancing state on valid actions (turn rotation, dealing, scoring, betting round transitions).
- Emitting state updates that UI observes via listeners/observable streams.
- Detecting end-of-round and end-of-game.
- Delegating variant-specific logic to plugged-in `Variation` strategies.

State must be serializable (Java POJOs / Gson-compatible). Engine never imports Android classes.

Each game module declares:

- A `RuleSchema` describing host-configurable settings (turn timer, blinds for Poker/Teen Patti, buy-in, round count, plus game-specific toggles). The pre-game settings UI renders this schema generically.
- A list of available `Variation` implementations.
- Reference content (hand rankings, rule sheet) for the Reference button.
- Help slides (image + text per slide) for the Help button.

A shared `HandEvaluator` lives in `core/engine` for poker-style hand rankings. It is reused by Teen Patti (modified rankings) and both Poker variants. Bluff does not need it.

---

## 8. Networking — Firebase Realtime Database

Firebase RTDB is the source of truth for room state during a session. Auth-gated reads and writes, scoped by room ID.

**Schema design is delegated to Claude Code.** It must support:

- Room metadata: host UID, game type, full rule config snapshot, status (waiting / in-progress / ended).
- Player roster: UID, display name, seat position, chip stack, connection state.
- Game state: current round number, turn pointer, deck reference / community cards / discard piles / hands as needed per game, action history sufficient for late-joiner sync and dispute resolution.
- Action submissions: any client writes proposed actions; engine validates against the latest state read.
- Rebuy requests: player submits, host approves/denies via a state field.
- Atomic transitions: use Firebase transactions where turn advancement or pot updates require linearization to avoid race conditions.

Pick a schema that minimizes write contention (e.g., separate paths for high-frequency state vs metadata). Document the chosen schema in `docs/firebase-schema.md` once finalized.

QR code carries the room ID. Optionally include a short-lived join token if access control becomes a concern. Use zxing for QR generation and scanning.

---

## 9. Authentication

- Firebase Auth, email/password.
- Splash screen routes to login or home based on auth state.
- Registration captures display name (required, shown at the table).
- Profile screen: display name, theme picker, sign-out. Nothing else in v1.
- No social sign-in (Google, etc.) in v1 unless trivial to add.

---

## 10. Application Flow

### 10.1 Splash → Auth
Standard Firebase auth gate.

### 10.2 Home
Two primary CTAs: **Create Room** and **Join Room**. Profile button. Shop / leaderboard / history nav entries hidden in v1.

### 10.3 Create Room
- Host picks game (Teen Patti / Bluff / Poker).
- Game-specific rules screen renders from the chosen game's `RuleSchema`.
- After rules are set, host lands in a **waiting lobby** with the QR code and 6-digit room code displayed prominently.
- Player slots fill as others join. Host sees a Start button (disabled until min players are present).
- Host can adjust rules until Start is hit.

### 10.4 Join Room
Two paths:
- **Scan QR**: camera permission, decode via zxing, auto-join.
- **Enter code**: 6-digit numeric code field.

On success, lands in the same waiting lobby as the host (without host controls).

### 10.5 Pre-Game Settings (host-only, in waiting lobby)

Each game's `RuleSchema` drives this UI. Required schemas:

**Teen Patti**:
- Boot/ante amount
- Buy-in per player
- Turn timer (10s / 20s / 30s / off)
- Round count (or play-until-stopped)
- Variation queue editor (drag-reorder, add, remove from preset list)
- Variation rotation cadence (every 1 / 2 / 3 / 5 rounds; default 2)

**Bluff**:
- Bluff-call rule: **Open Call** (anyone may call) vs **Next-Only** (only the next player in turn may call)
- Turn timer
- Round count

**Poker**:
- Variant: **Texas Hold'em** vs **5-Card Draw**
- Small blind / big blind
- Buy-in per player
- Turn timer
- Round count or play-until-stopped

Once the host hits Start, settings lock for the room's lifetime.

### 10.6 In-Game Shell (shared across all three games)

- **Top bar**: room ID, blinds (if applicable), settings icon, reference icon, help icon.
- **Reference button**: opens hand rankings overlay (Poker, Teen Patti) or rule sheet (Bluff). Dismissible.
- **Help button**: swipeable rule-slide overlay for the current game and active variation.
- **Settings button** (in-game): theme picker, sound toggle, leave room.
- **Rebuy flow**:
  - When a non-host player's chip stack hits 0 (or any time, depending on game), a Rebuy button appears.
  - Player taps it, enters an amount, submits.
  - Host receives a banner with the request: "Player X requests ₹1,000 buy-in. Approve / Deny."
  - On approve, chips appear in the player's stack. On deny, banner clears.
  - Host can also manually inject chips for any player from the leaderboard panel — this is the "Buy Chips" affordance from the mockup, used for IRL reconciliation ("you gave me ₹500 cash, here are 500 chips").
- **Hidden session leaderboard**: pull-out drawer or modal sheet. Toggleable; off by default.
  - Poker, Teen Patti: chip stack and rounds won.
  - Bluff: rounds won only.
- **Chat/emoji rail** (left edge): leave layout space, but **do not implement** in v1. v1.1 will add Clash Royale–style emoji taunts here.

### 10.7 Game-specific UI

All game screens use a perspective-table layout (matches reference mockup):

- Local player at bottom center: cards face-up to them, chip stack, avatar, action buttons on right.
- Opponents around the rim: avatars, names, chip counts, card backs (face-down to local player).
- Center area: community cards (Hold'em), pile/discard (Bluff), pot indicator above table center.
- Dealer button marker rotates per round in Poker and Teen Patti.
- Chip rack ribbon at the bottom of the screen for tactile "your stack" feel.
- Action buttons on right edge — game-specific labels (Fold/Check/Raise; Pack/Show/Side Show/Chaal; Play/Call Bluff).
- Animations: dealing, chip-throw, win/loss, card-reveal — all theme-driven.

Use Material Components for buttons/dialogs, custom views for the table/cards/chips.

---

## 11. Game Specifications

### 11.1 Teen Patti

Standard 52-card deck. 3 cards per player. Hand rankings (high → low):
1. Trail (Three of a Kind)
2. Pure Sequence (straight flush)
3. Sequence (straight)
4. Color (flush)
5. Pair
6. High Card

**Flow**: ante (boot) → blind/seen choice per player → betting (chaal/raise/fold/show/side-show) → showdown.

**Variations to ship as preset queue**:
1. **Classic** — standard rules.
2. **AK47** — A, K, 4, 7 are jokers (wild).
3. **1942 A Love Story** — 1, 9, 4, 2 are jokers (treat 1 as Ace).
4. **Muflis** — lowest hand wins (rankings inverted).
5. **Joker / Wild Cut** — one rank chosen randomly per round is wild.

Default queue order: Classic → AK47 → 1942 → Muflis → Joker → loop. Host can reorder/add/remove. Default rotation cadence: every 2 rounds.

**Engine notes**:
- `Variation` strategy plugs into hand evaluation (jokers/wild substitution) and win condition (Muflis flips comparison).
- Side show is legal in Classic; engine checks per-variation whether it's allowed.
- `HandEvaluator` is parameterized by joker set.

### 11.2 Bluff (a.k.a. Cheat / I Doubt It)

Standard 52-card deck. All cards dealt evenly across players (any leftover goes to the first players in order).

**Flow**: starting player plays 1 or more cards face-down, claiming a rank (e.g., "three Kings"). The claim can be any rank — no adjacency constraint, keeps the game accessible. Other players decide whether to call based on probability and tells. After a play, turn passes to the next player.

**Variations** (chosen at room creation):
- **Open Call**: any player may call "Bluff!" against the most recent claim at any time before the next play resolves.
- **Next-Only**: only the next player in turn order may call; once they play their own cards, the prior claim is locked.

**Resolution on a call**:
- Played cards are revealed.
- If claim was true (all played cards match the claimed rank): caller picks up the entire pile.
- If claim was false: claimer picks up the pile.

**Win condition**: first player to empty their hand wins the round.

**Engine notes**:
- Action types: `PlayCards(rank, count, cardIds)`, `CallBluff(targetActionId)`, `Pass` (next-only mode, when the next player chooses not to call and proceeds to their own play).
- Validation must ensure the player owns the cards they're playing.
- Hidden information: played cards' true ranks are stored in state but not exposed to non-claimer clients until a call resolves.

**Leaderboard**: rounds won (count of rounds where this player emptied their hand first).

### 11.3 Poker

Two variants chosen at room creation:

**Texas Hold'em**:
- 2 hole cards per player, 5 community cards.
- 4 betting rounds: pre-flop, flop (3 community cards dealt), turn (4th), river (5th).
- Standard hand rankings (High Card → Royal Flush).
- Small/big blinds rotate per round; dealer button rotates.
- Showdown: best 5-card hand from each player's 7 available cards.

**5-Card Draw**:
- 5 cards dealt face-down per player. No community cards.
- Round 1: betting.
- Discard phase: each player may discard 0–4 cards and draw replacements.
- Round 2: betting.
- Showdown: each player's full 5-card hand.
- Use blinds (small/big) for consistency with Hold'em, dealer button rotates.

**Shared Poker mechanics**:
- **Side pots**: when a player goes all-in for less than the current bet, the pot splits. Implement a side-pot algorithm: sort all-in commitments, build pots layer by layer with the eligible-player set decreasing each layer. **Write unit tests for this before integrating** — it's the single highest-risk piece of code in the project.
- Hand rankings shared via `HandEvaluator` in `core/engine`.
- Action types: `Fold`, `Check`, `Call`, `Raise(amount)`, `AllIn`, plus `Discard(cardIds)` for 5-Card Draw.

**Leaderboard**: chip stack and rounds won.

---

## 12. Build Order

1. **Foundation** — auth (Firebase), home, theme system + Royal Oak reference theme, game engine interface, Firebase RTDB sync layer, room creation/join (QR + code), pre-game settings shell, in-game shell skeleton (reference/help/leaderboard/rebuy hooks).
2. **Teen Patti** — full game with Classic + AK47 first; layer in 1942, Muflis, Joker; variation queue editor and rotation logic.
3. **Bluff** — both variations. This validates that the engine abstractions actually generalize beyond Teen Patti.
4. **Poker** — `HandEvaluator` first (with thorough unit tests), then 5-Card Draw (simpler, no community cards), then Texas Hold'em, then side-pot logic.
5. **Polish** — second theme (Neon Pulse), animations, sounds, win/loss sequences, hidden leaderboard polish, help slides written and illustrated.

This is a guideline. The lead developer adjusts based on team velocity.

---

## 13. Out of v1 Scope

Do NOT build any of these in v1, even if they look easy:

- Public lobbies, Friendly mode, Competitive mode, matchmaking.
- Cosmetics shop or any soft-currency system.
- Real-money or convertible currency.
- Inter-player chip transfers (rebuy from host covers this use case).
- In-game text chat.
- Global persistent leaderboard (only the in-game session leaderboard exists).
- UPI or any payment integration.
- Proximity-sensor flip-to-hide, shake-to-shuffle, or any other novel sensor interactions.
- Multi-deck Bluff (single deck only).
- Tournament structures, blind level escalation, or anything else from real online poker.

---

## 14. Future Hooks (Architectural Awareness Only — Do Not Implement)

Leave space for these without building them:

- **Emoji taunts** (Clash Royale–style) — keep the chat/emoji rail in the game UI as a visual placeholder. Wired in v1.1.
- **Shop** — cosmetic theme purchases. The `ThemeRegistry` should already model "owned vs locked" state even though all themes are owned by default in v1.
- **UPI settlement** — a future direction, no backend stubs needed.
- **Additional themes / additional games** — the registry pattern handles these for free.

---

## 15. Decisions Claude Code Owns

These are explicitly delegated. Make a reasonable call and document it:

- **Firebase RTDB schema layout** — see Section 8 for what it must support. Document the chosen schema in `docs/firebase-schema.md`.
- **Specific animation/visual library choices** (Lottie vs alternatives, glow technique). Pick one and stay consistent.
- **File-level organization within each module** (Section 5 is recommended, not prescribed; top-level boundaries are required).
- **Display name uniqueness rules** (allow duplicates? require uniqueness within a room?). Pick a sane default.
- **Min-SDK and Gradle versions** within the constraints.
- **QR payload format** — room ID alone, or room ID + token. Pick based on threat model.

---

## 16. Decisions to Escalate

If any of these come up, surface them rather than guess:

- Anything that materially changes a locked decision in this document.
- Cases where syllabus-allowed libraries cannot achieve a needed effect and a non-allowed library seems necessary.
- Any architectural choice that would make a v1.1+ feature (emoji taunts, shop, additional themes/games) noticeably harder to add later.
- Performance issues that would force a non-trivial change to the modularity guarantees in Section 3.

---

## 17. Definition of Done for v1

The app is v1-complete when:

- A host can sign up, create a room for any of the three games, configure rules, share a QR code or 6-digit code, and start a game.
- Four other players can sign up and join via QR or code, and all five see synchronized state across phones.
- All three games are playable end-to-end with their declared variations.
- Both themes (Royal Oak, Neon Pulse) are selectable per-user and re-skin every relevant screen and animation.
- Reference and Help buttons work for all three games.
- Rebuy flow works (request → host approval → chips appear).
- Hidden session leaderboard works for all three games.
- The faculty can sit down with the team during the demo and play a real round of any of the three games without manual intervention from the developers.

That last bullet is the real bar.
