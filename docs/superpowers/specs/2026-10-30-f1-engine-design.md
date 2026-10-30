# F1 — Deterministic game engine

## Approved rules

One square Board model supports 8×8, 16×16 and other positive sizes; Piece uses
relative cell offsets independent of board size. There are three available
pieces; after all three are used, generate a new hand. Full rows and columns
clear simultaneously. No remaining piece fitting anywhere ends the game.

Difficulty affects piece distribution, independently of size and tempo:
Easy favors small/simple shapes, Medium samples the catalog evenly, Hard favors
large/awkward shapes. No rotation action in F1; rotated shapes exist in the catalog.

Tempo: Relaxed has no timer; Slow 90 seconds, Medium 45, Fast 20 per move.
A successful placement resets the move timer; invalid actions do not. Pause and
background time do not advance the engine. Timed games have three timeout strikes:
each timeout deducts 10 points (floor zero); the third ends the game. Placement
does not restore strikes. Multiple complete periods in an explicit elapsed-time
input count separately, so a delayed update cannot grant unlimited time.

## Implementation choices

Pure Kotlin commonMain/commonTest, no Compose/Android/clock/network dependency.
Immutable board/state snapshots and explicit action results. A rejected move
leaves board, score, timer, hand and deterministic generator state unchanged.
Inject elapsed milliseconds and a seed; use a documented stable PRNG so equal
seeds/action sequences reproduce on JVM and iOS. Do not use global Random/time.

Default scoring (builder choice): one point per placed cell plus 10 per completed
line; intersecting row/column counts as two lines, shared cells removed once.
Scores use Long; timeout deduction is exactly 10. Piece catalog: monomino,
lines of lengths 2–5, 2×2/3×3 squares, L triominoes and L/T/Z tetromino rotations.
Generated pieces must fit an empty selected board. Distribution weights small
(1–3 cells) / large (4+): Easy 4/1, Medium 1/1, Hard 1/4.

Alternative approaches: size-specific board classes duplicate rules; a mutable
clock-driven engine couples tests to platform lifecycle. Chosen: one immutable
engine with explicit actions/time, suitable for future UI and save/restore.

## Scope and verification

Tests cover both board sizes, boundaries/collision, simultaneous clears and
scoring, invalid actions, seeded generation/weights, hand refill, terminal states,
pause/resume, exact time boundaries, multiple timeouts, penalty floor and third
strike. F2 supplies UI controls and platform lifecycle calls; F1 exposes pause
but does not add menus, gameplay UI or lifecycle wiring. iOS compilation requires
macOS; common tests run on JVM here and are wired to an Apple simulator CI task.
