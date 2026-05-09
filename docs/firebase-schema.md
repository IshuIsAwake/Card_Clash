# Firebase Realtime Database Schema — CardClash v1

This is the authoritative schema. Code-side wrapper: `core/network/FirebaseRoomSync.java`.

## Top-level

```
/users/{uid}
  displayName: string

/rooms/{roomId}
  meta:
    hostUid: string
    gameType: "TEEN_PATTI" | "BLUFF" | "POKER"
    status:   "WAITING" | "IN_PROGRESS" | "ENDED"
    createdAt: long (epoch ms)
    config:
      gameType: string
      values:
        <ruleSchemaKey>: <int|long|bool|string|list>

  players:
    {playerUid}:
      uid:         string
      displayName: string
      seat:        int
      chips:       long
      connected:   bool
      host:        bool

  rebuyRequests:
    {requestId}:
      requestId:   string
      playerUid:   string
      amount:      long
      requestedAt: long
      status:      "PENDING" | "APPROVED" | "DENIED"

  gameState:
    <opaque per-game blob, written by the engine>

  events:
    {pushId}:
      actorUid:  string
      kind:      string
      payload:   map
      timestamp: long
```

## Why split `meta`, `players`, `gameState`?

High-frequency game-state writes (turn pointer, pot, hands) would otherwise
trigger every roster listener — an unnecessary fanout on every chip flick.
By scoping listeners per node, each client subscribes to exactly the nodes it
re-renders against.

- Lobby UI: subscribes to `meta` and `players`.
- In-game UI: subscribes to `players` (chips, connected) and `gameState`.
- Host rebuy banner: subscribes to `rebuyRequests`.

## Atomic operations

- **Chip add/sub**: `players/{uid}/chips` is mutated via Firebase
  `Transaction` to avoid lost updates from concurrent rebuys / host injections.
- **Turn advancement & pot updates**: when full multiplayer game-state sync is
  wired, these go through transactions on `gameState/turnIndex` and
  `gameState/pot`.

## QR payload

The QR encodes only the 6-digit room code. The 6-digit space (1M values) is
sufficient for friend-group concurrency; collisions are checked at create time
(future enhancement: a short-lived join token if abuse is observed).

## Auth scoping (rules — enforce in Firebase console)

```
"rooms/$roomId": {
  ".read":  "auth != null && data.child('players').child(auth.uid).exists()
                              || !data.exists()",
  ".write": "auth != null"
}
```

Tighten for production: writes to `meta/status` should require
`auth.uid == data.child('hostUid').val()`; writes to `players/{uid}/chips`
should be host-only OR via the rebuy-approval flow.
