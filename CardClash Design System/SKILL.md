---
name: cardclash-design
description: Use this skill to generate well-branded interfaces and assets for CardClash, a mobile card-game referee app (Teen Patti, Bluff, Poker — pass-and-play and private-room). Contains essential design guidelines, colors, type, fonts, assets, and UI kit components for prototyping. Four built-in themes — Developer (B&W testing), Royal Oak (warm club), Neon Pulse (cyber lounge), Balatro (pixel/CRT).
user-invocable: true
---

Read the README.md file within this skill, and explore the other available files. Key entry points:

- `README.md` — brand, voice, visual foundations, iconography. Start here.
- `colors_and_type.css` — drop-in CSS variables for all four themes. Add `data-theme="royal-oak|neon-pulse|balatro|dev"` to a wrapper to switch.
- `assets/` — logo SVG, suit glyphs, card-back patterns.
- `preview/` — small specimen cards demonstrating each foundation token in isolation.
- `ui_kits/cardclash_app/` — pixel-faithful JSX recreation of five core screens: Login, Home, Hot Seat setup, Pass-the-device gate, Bluff table.

If creating visual artifacts (slides, mocks, throwaway prototypes, etc), copy assets out of this skill and create static HTML files for the user to view. Pull in `colors_and_type.css` directly so theme tokens stay consistent.

If working on production Android code, copy assets and use the README and primitive token tables as authoritative — the system maps onto the existing `Theme` interface in `app/src/main/java/com/example/cardclash/core/theme/`.

If the user invokes this skill without any other guidance, ask them what they want to build or design (which surface, which theme, what level of fidelity), then act as an expert designer and output HTML artifacts or production code, depending on the need.

House rules to follow when designing for CardClash:

- No emoji. Anywhere. Suits are drawn vectors, not Unicode `♠♥♦♣`.
- ALL-CAPS lightly-tracked for screen labels. Title Case for compound CTAs. UPPERCASE for action buttons. sentence case for body.
- Em-dash `—` separates compound CTAs. Middle-dot `·` separates inline metadata. No exclamation points.
- Selection states must be unambiguous — never communicate selected-vs-unselected by alpha alone (this was the bug in the dev build).
- Every screen lives inside one of the four themes. Don't invent new palettes or type pairings.
