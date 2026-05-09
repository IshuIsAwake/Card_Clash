# CardClash — App UI Kit

Interactive recreation of the CardClash Android app. Five screens, fully theme-aware (Developer, Royal Oak, Neon Pulse, Balatro). Click through Login → Home → Hot Seat setup → Pass-the-device gate → Bluff table.

Open `index.html` in your browser. Switch themes from the top-right pill.

## Files

- `index.html` — entry. Loads React, Babel, the design system CSS, and the JSX modules.
- `components.jsx` — primitives: `<Phone>`, `<Btn>`, `<Field>`, `<TopBar>`, `<Card>`, `<Chip>`, `<HandStrip>`, `<PlayerSlot>`.
- `screens.jsx` — five screens: `LoginScreen`, `HomeScreen`, `HotSeatSetupScreen`, `PassGateScreen`, `BluffTableScreen`.
- `app.jsx` — minimal client-side router + theme switcher.

## Coverage

These are visual recreations — game logic is mocked. The point is to give designers and the team a high-fidelity reference for layout, type, color, and component anatomy. Real game state lives in the Java engine.
