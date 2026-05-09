# CardClash — Design System

> **CardClash** is a mobile card game for friends sitting around a real table. The phone is the **referee** — it deals, tracks chips, and enforces rules. The friendship, banter, and IRL cash-settlement stay in the room. v1 ships **Teen Patti**, **Bluff**, and **Poker**, with **Uno** on the roadmap. Think *Ludo King for cards*: pass-and-play on one device, or scan a QR to join a private room.

This repo is the design system that the team — and any AI agents helping the team — should reference when building or restyling any CardClash surface. It exists because the current UI (a placeholder dev build with hard-bordered B&W boxes) is **functional but not tasteful**, and the team has the engine working long before the look-and-feel.

---

## Sources

| Source | Path / Link | What it is |
|---|---|---|
| Android codebase | `IshuIsAwake/Card_Clash` (GitHub) | Java + XML Android Studio project. Engine, themes, screens. |
| `instructions.md` | repo root | Authoritative v1 spec. |
| `memory.md` | repo root | What's already implemented. |
| `context.md` | repo root | Next-session work plan. |
| Current UI screenshots | `uploads/WhatsApp Image*.jpeg`, `uploads/Screenshot*.png` | Login, Hot Seat setup, pass-the-device gate, Bluff table — all in the placeholder dev theme. |
| Balatro reference | `uploads/balatro.png` | Visual target for the **Balatro** theme (pixel cards, lo-fi UI panels, painterly green felt). |
| Existing themes | `app/src/main/java/com/example/cardclash/themes/{dev,royaloak,neonpulse}/*.java` | `Theme` interface implementations. |
| Existing colors | `app/src/main/res/values/colors.xml` | `ro_*` `np_*` `dev_*` palettes. |
| Existing styles | `app/src/main/res/values/themes.xml` | Material theme overlays, button styles. |

The reader is not assumed to have access to the GitHub repo — every value referenced here is also encoded in `colors_and_type.css`, `assets/`, or the JSX UI kit.

---

## Index

| File / folder | Purpose |
|---|---|
| `README.md` | This file. Brand, voice, visual foundations, iconography. |
| `SKILL.md` | Agent-Skill manifest. Drop into Claude Code's skills folder to invoke as `cardclash-design`. |
| `colors_and_type.css` | All four themes' colors + type as CSS variables. Semantic tokens (`--bg`, `--fg`, `--accent`, `--h1`, `--mono`, etc.) plus per-theme primitives. |
| `fonts/` | Google Fonts substitutes for the four themes, plus a manifest noting which weights are used. |
| `assets/` | Logo SVG, card-back patterns, chip glyphs, table-felt textures, suit icons. |
| `preview/` | One HTML card per atomic concept. Renders into the Design System tab. |
| `ui_kits/cardclash_app/` | Pixel-faithful JSX recreation of the Android app. Login → Hot Seat setup → pass-the-device → Bluff table. |

---

## Content fundamentals

### Voice

Direct. Short. Confident. **No exclamation points, no emoji, no marketing hype.** The product knows it's a referee, not a casino — copy never tries to gamify or reward; it just tells you whose turn it is and what you can do.

### Casing

- **ALL-CAPS LIGHTLY-TRACKED** for screen labels and section headers. Tracking ≈ `0.16em`. e.g. `HOT SEAT`, `BLUFF · HOT SEAT`, `PASS THE DEVICE`, `NO ACTIVE SEQUENCE`.
- **Title Case** for primary CTAs that are full sentences (`Hot Seat (Offline)`, `No account? Register`).
- **UPPERCASE** for primary action buttons (`I'M PLAYER 1 — CONTINUE`, `CALL BLUFF`, `PLAY`, `SKIP`, `DEAL`).
- **sentence case** for body copy and helper text (`Hand will appear after you continue.`, `Single device. Pass and play.`).

### Pronouns

Second-person `you` for instructions to the active player. Neutral nouns (`Player 1`, `the next player`, `the host`) for the referee's voice. **Never** `we` — the app isn't a brand mascot.

### Punctuation

- The em-dash `—` is the house separator for compound CTAs and labels: `I'M PLAYER 1 — CONTINUE`, `Player 1 — your turn`.
- The middle-dot `·` separates inline metadata: `BLUFF · HOT SEAT`, `Open Call · WKT`, `17 cards · 0W`.
- A trailing period closes full helper sentences. Buttons and labels never get a period.

### Numerals

Always Arabic numerals, never spelled out: `3 players`, `6-digit code`, `0/5`, `52/52`, `0 sel`.

### Emoji

**No emoji anywhere.** This includes ✨ 🎉 🎴 ♥ ♠ ♦ ♣ in copy. The four card suits are drawn as **vector glyphs** (or theme-specific pixel sprites for Balatro) — never as Unicode emoji.

### Vibe

A poker-night refit of a transit app. Calm, legible, premium. The product respects players' attention because IRL friends are sitting across from them — the screen should never demand the room.

#### Examples actually shipped in the build

> `BLUFF · HOT SEAT` &nbsp;·&nbsp; `Single device. Pass and play.` &nbsp;·&nbsp; `Hand will appear after you continue.` &nbsp;·&nbsp; `Welcome back` / `Sign in to your table` &nbsp;·&nbsp; `No Firebase needed` &nbsp;·&nbsp; `Player 1 — your turn` &nbsp;·&nbsp; `17 cards · 0W` &nbsp;·&nbsp; `0 sel` &nbsp;·&nbsp; `NO ACTIVE SEQUENCE / PILE` &nbsp;·&nbsp; `Show hand` / `Hide hand` &nbsp;·&nbsp; `Hot Seat (Offline)`

---

## Visual foundations

### The four themes

CardClash ships with **four themes** that re-skin every screen without changing layout. Every theme implements the same `Theme` interface, so layouts stay constant; only colors, drawables, type, and sound change.

| Theme | Lane | Vibe |
|---|---|---|
| **Developer** | Default during dev | Dark mode, very high contrast, white borders. Built for testing — selection state must be unambiguous. No ornament. |
| **Royal Oak** | Warm club | Walnut wood, deep green felt, brass accents, ivory cards. Serif type, warm amber numerics. The "premium living-room poker night" lane. |
| **Neon Pulse** | Cyber lounge | Dark glass, magenta + cyan + purple. Glow on every edge. Monospace numerics. The "bar-arcade" lane. |
| **Balatro** | Pixel / CRT | Pixel-art card faces, painterly green felt with subtle scan-lines, lo-fi UI panels with bevel borders, blocky display type. The "rogue-like deckbuilder" lane. |

### Color usage philosophy

- **Surfaces are dark.** All four themes default to a dark canvas (Developer is `#000`, Royal Oak is `#1A0F08` walnut, Neon Pulse is `#0A0512`, Balatro is `#1B2D24` pickled green). Light mode is **not** in v1.
- **One accent per screen.** Royal Oak = brass `#C9A24B`. Neon Pulse alternates cyan + magenta. Balatro uses a desaturated tan-and-red. Developer uses pure white.
- **Win = green-leaning, Lose = red-leaning, Warning = amber.** Never use pure `#0F0` / `#F00` — every status color is tinted to fit the theme.
- **Card faces stay near-ivory in every theme**, never black-on-white pure. The card surface itself is a near-white that reads as warm in Royal Oak, cool in Neon Pulse, and pixelated/posterized in Balatro.

### Typography

| Role | Royal Oak | Neon Pulse | Balatro | Developer |
|---|---|---|---|---|
| Display | **Playfair Display 700/800** | **Orbitron 700** | **Press Start 2P 400** | **Inter 700** |
| Heading | Playfair Display 600 | Orbitron 600 | VT323 400 (faux-pixel for runs of UI text) | Inter 600 |
| Body | Lora 400 | Inter 400 | VT323 400 | Inter 400 |
| Numeric | Lora 600 | **JetBrains Mono 700** | Press Start 2P 400 | JetBrains Mono 600 |

- The Balatro theme actually ships m6x11 in the game — we substitute **Press Start 2P** (display) and **VT323** (running text) on Google Fonts. *Flagged: replace with the licensed pixel face if/when you secure rights.*
- Numerics are always either monospace or set with tabular figures. Chip stacks, scores, and timers must never reflow when digits change.
- Tracking on ALL-CAPS headers ranges from `0.12em` (Royal Oak serif) to `0.20em` (Neon Pulse / Balatro display).

### Spacing and layout

- 4-pt grid. Token scale: `4 8 12 16 20 24 32 40 56 72 96`.
- Touch targets: never below **44pt** square (matches the existing themes' button min-height).
- Chip rack and action buttons stay anchored to screen edges — they're tactile, not flowing content.
- **Orientation rule (locked)**: only **Login** and **Home / game-mode selection** are portrait. Every other screen — Hot Seat setup, name entry, Pass-the-device gate, all game tables (Bluff / Teen Patti / Poker), Lobby with QR, Settings — is **landscape-locked**. Don't mix orientations within a screen.
- **Primary action placement**: primary CTAs sit at horizontal **middle of the screen**, not pinned to the bottom edge. On landscape gameplay screens, the action column is centered vertically on the right edge; on landscape setup screens, the primary CTA centers horizontally below the form. The thumb reaches the middle naturally when the device is held in two hands for landscape — bottom-anchored CTAs were a portrait habit and don't apply here.
- The action column always lives on the **right edge** of game screens, the player's hand always on the bottom edge, opponents always along the top.

### Backgrounds

- **Royal Oak**: Tabletop perspective. Deep walnut periphery, oval green-felt center, soft warm vignette. Felt has a subtle radial gradient (lighter center) — no busy patterns.
- **Neon Pulse**: Dark glass with a faint violet radial. A few thin neon "circuit" lines along the rim of the table. No pattern; lighting carries the mood.
- **Balatro**: Painterly green felt, slightly warped/wobbled, with smoke or playing-card silhouettes barely visible in the background — references the Balatro screenshot's organic blob backdrop. Optional CRT scan-line overlay at low opacity.
- **Developer**: Pure black. No texture. No gradient. Visible 1px white grid lines for layout debugging *only* in tweak/preview mode, off by default.

### Animation

- **Easings**: `cubic-bezier(0.2, 0.8, 0.2, 1)` for enters, `cubic-bezier(0.4, 0, 1, 1)` for exits. Royal Oak uses slightly slower curves (220–280ms) than Neon Pulse (140–180ms) — wood is heavier than neon.
- **Card deal**: 220ms slide-in from deck origin, 12° tilt → 0°, slight overshoot.
- **Chip throw**: 180ms parabolic arc from player slot to pot. Stops with a tiny bounce.
- **Card flip**: 320ms Y-axis flip. Royal Oak has a soft brass shimmer at the midpoint; Neon Pulse has a cyan flash; Balatro is two-frame instant snap (no easing — pixel-art convention).
- **Win**: 600ms vignette of the winner's slot brightening + chip-rain animation. **No confetti.** No emoji.
- **No bouncy springy "fun" animations.** This is a poker app, not a kids' game.

### Hover / press / disabled

- Hover (where applicable, e.g. desktop emulation): +6% lighten on background, no scale.
- Press: 95% scale on the touched element (Royal Oak / Neon Pulse / Balatro). Developer uses a fill-swap (white → grey) and **no scale**.
- Disabled: 40% alpha + slightly darker fill. Always pair with cursor change on web mocks.

### Borders and shadows

- **Royal Oak**: Hairline brass border (`1px @ #C9A24B`) on cards and buttons. Soft warm shadow `0 4px 16px rgba(0,0,0,0.4)`. Inner shadow on the felt to suggest the table rim.
- **Neon Pulse**: 1px outer glow (cyan or magenta), then a softer 8px outer glow at lower opacity. No solid drop shadows.
- **Balatro**: 2px hard pixel-style border, 2px hard offset shadow (always pure black, no blur). Pixel-art convention.
- **Developer**: 2px solid white border, no shadow. Selected state = inverted (black text on white fill); unselected = white text on black fill **with the border still visible**. **Never communicate selection by alpha alone** — that was the bug in the dev build before this rewrite.

### Transparency & blur

- Modals and sheets: 80% opacity black scrim behind, 12px backdrop-blur on the sheet container itself.
- The "Pass the device" gate is **opaque black** — no blur — because it must hide every previously-visible card.
- Avoid translucent UI on game screens; chip stacks and pot indicators are always solid.

### Corner radii

| Element | Radius |
|---|---|
| Buttons (Dev) | `0` (intentional sharp) |
| Buttons (Royal Oak) | `10pt` |
| Buttons (Neon Pulse) | `14pt` |
| Buttons (Balatro) | `2pt` (faux-pixel) |
| Cards (the playing kind) | `8pt` (Royal Oak/Neon Pulse), `2pt` (Balatro), `4pt` (Dev) |
| Cards (the surface kind) | `14pt` everywhere except Dev (`0`) and Balatro (`2pt`) |
| Inputs | `10pt` Royal Oak, `12pt` Neon Pulse, `2pt` Balatro, `0` Dev |
| Pills / chips | full pill (`9999pt`) — except Balatro which uses a square chip with a beveled stripe |

### Card faces

The face design is theme-driven. Every face has the same anatomy:

- Top-left + bottom-right rank+suit corner pip
- Center pip (numbered cards) **or** centered face-card portrait (J/Q/K)
- 8pt corner radius (Royal Oak / Neon Pulse / Dev), 2pt (Balatro)
- Subtle inner shadow on Royal Oak; hairline cyan outline on Neon Pulse; hard pixel border on Balatro

Royal Oak portraits are stylized line illustrations (placeholder vectors in `assets/face_portraits/`). **Balatro portraits are pixel-art** — we ship simplified, recolored pixel sprites that don't infringe on Balatro's actual artwork. **Flagged: ship final portraits with the artist.**

### Chips

Chips are **circular, edge-striped**, and stack vertically. Denominations carry color, not text:

- `1` white · `5` red · `10` blue · `25` green · `100` black w/ gold rim · `500` purple · `1000` brass

Royal Oak chips have a hammered metal rim. Neon Pulse chips glow on the stripe. Balatro chips are flat-colored circles with a 2-pixel highlight ring. Dev chips are plain white-on-black circles with the denomination written across the face.

### Layout fixed elements

- Top bar (room ID, blinds, settings/help/reference): always anchored top, never scrolls.
- Action column: anchored right edge of game screens.
- Hand strip: anchored bottom of game screens.
- "Pass the device" gate: full-screen modal, dismissed only by the named player tapping continue.

---

## Iconography

CardClash uses three families of icons, used in different contexts:

1. **Suit glyphs** (♠ ♥ ♦ ♣ as drawn vectors, never Unicode). Stored in `assets/suits/{spade,heart,diamond,club}.svg`. Each theme tints these — Royal Oak red is `#9E2A1F`, Balatro red is `#D85A3E`, Neon Pulse "red" is actually magenta `#FF1E8E`. **Black** suits are `#15110B` Royal Oak / `#F0EAFF` Neon Pulse (suits glow on the dark glass) / `#1A1A1A` Balatro / `#FFFFFF` Dev.
2. **UI icons**: from **[Lucide](https://lucide.dev)** at 24×24 with `1.75` stroke. Loaded via CDN script tag. Used for `settings`, `help`, `chevron-right`, `qr-code`, `eye`, `eye-off`, `arrow-up-right`, `info`, `users`, `gear`, `circle-help`. The Balatro theme rasterizes these to a 16×16 pixel grid via a `image-rendering: pixelated` CSS rule applied to a downscaled SVG.
3. **Logo / brandmark**: two stylized cards (gold + dark green) overlapping with an upward arrow in the green card. Stored at `assets/logo_cardclash.svg`. The login screen logo (uploads/WhatsApp Image 4.22.46 PM(1).jpeg) is the canonical reference. **Never** redraw — copy this asset.

### Emoji

**No emoji.** Anywhere. Including in haptic feedback, push notifications, or ASCII placeholders. The roadmap calls for a Clash-Royale–style **emoji-taunt rail** on the chat edge in v1.1; those will be **bespoke pixel-stickers shipped per theme**, not Unicode emoji.

### Unicode

The middle-dot `·`, em-dash `—`, and the Indian-rupee `₹` are the only non-ASCII characters routinely used in copy. Suit characters (♠♥♦♣) are **forbidden** in copy — they look weak rendered as text.

---

## Caveats & substitutions

- **Pixel font**: Balatro uses **m6x11**; we substitute **Press Start 2P** (display) and **VT323** (body) from Google Fonts. Both are open-source. Final shipping APK can keep the m6x11 license check pending and use VT323 in the meantime.
- **Card portraits** (face cards J/Q/K) are placeholder vector silhouettes — the licensed artist cut for Royal Oak hasn't been delivered yet.
- **Sounds** are not bundled in this design system; only documented expectations (woody chip-clink for Royal Oak, synth blip for Neon Pulse, 8-bit beep for Balatro, no sound for Developer).
- **Balatro theme drawables** are not yet in the Android codebase — this design system *introduces* them. The team needs to add `bg_table_balatro.xml`, etc., when implementing.
- **Logo SVG**: the in-repo `ic_logo_cardclash.xml` is a placeholder vector. The polished login mark from the screenshot has not been exported to SVG; we recreate it in `assets/logo_cardclash.svg` as a near-match.
