# Puzzle engine (F1)

Pure Kotlin `commonMain` rules, shared by JVM/Android and iOS. No platform clock,
UI, network or persistence dependency.

## Boards, pieces and scoring

`Board.empty(size)` supports 8×8, 16×16 and other positive square sizes.
`Board.fromCells(size, cells)` snapshots a custom initial board. `Piece(cells)`
accepts nonempty, unique, nonnegative offsets normalized to top/left zero.
Coordinates are `Cell(row, column)` with `(0,0)` at the top left. Catalog entries
include line pieces (1–5), 2×2/3×3 squares and L/T/Z/S rotations (31 unique shapes).
F1 has no player rotation action.

`canPlace` checks bounds and collisions. `place` returns null on rejection,
otherwise a `Placement` containing a new board, cleared row/column indices,
removed cell count and points. Rows/columns are detected together before any
cells are removed. Crossing cells are removed once; both lines score.

Scoring: **1 per placed cell + 10 per completed row/column**. A single cell
completing a row and a column earns 21. Board, Piece, generated hands and game
sessions snapshot collections defensively; rejected moves preserve state.

## Independent difficulty and tempo

| Difficulty | Small shapes (1–3 cells), weight per shape | Larger shapes, weight per shape |
| --- | ---: | ---: |
| EASY | 4 | 1 |
| MEDIUM | 1 | 1 |
| HARD | 1 | 4 |

These are distribution weights, not promises of a particular hand. Pieces are
filtered to fit an **empty** board of the configured size; the engine does not
rescue a blocked board by choosing only currently playable pieces.

| Tempo | Time per successful move |
| --- | ---: |
| RELAXED | Unlimited |
| SLOW | 90 seconds |
| MEDIUM | 45 seconds |
| FAST | 20 seconds |

Each timeout deducts 10 points, floored at zero. The first two continue play;
the **third timeout ends the game**. Successful placement resets the timer but
never restores timeout rights. Multiple elapsed periods count separately, with
the final partial period retained. Timeouts do not change the board or pieces.

Three pieces are offered together. Used slots stay empty until all three are
used, then a new hand is drawn. After every accepted move (including refill),
none of the remaining pieces fitting anywhere ends play with `NO_MOVES`.
`TIMEOUT_LIMIT` is the other end reason. Finished sessions cannot be resumed.

## API

```kotlin
var game = GameSession.start(
    config = GameConfig(boardSize = 16, difficulty = Difficulty.HARD, tempo = Tempo.SLOW),
    seed = 2026L,
)

// UI supplies monotonic elapsed foreground milliseconds BEFORE processing a move.
game = game.advanceTime(1000L)
when (val move = game.place(slot = 0, origin = Cell(0, 0))) {
    is MoveResult.Placed -> game = move.game
    is MoveResult.Rejected -> { /* inspect move.reason; state is unchanged */ }
}

game = game.pause()
game = game.resume() // Preserves remaining time, score, pieces and strikes.
```

There is no autonomous timer: `advanceTime` is the sole time input. Pass only
elapsed unpaused foreground time, never an absolute timestamp. Negative elapsed
time is rejected. Paused/finished/relaxed sessions ignore positive elapsed time.
The F2 presentation controller advances any pending foreground interval before
a move/pause, pauses on background, and restarts its clock baseline on resume.
These adapters and the playable UI live in composeApp, not in this engine.

`PieceGenerator.seeded(seed)` uses Park–Miller arithmetic with a positive state
in 1..2,147,483,646. Seed normalization is `1 + floorMod(seed, 2,147,483,646)`;
each draw multiplies by 16,807 modulo 2,147,483,647. Catalog order, difficulty
weights and this mapping determine replay; tests pin two full hands and RNG
state. This generator is deterministic, **not cryptographic**. F3 adds transport-neutral
`GameSession.snapshot()` and validated `GameSession.restore(snapshot)`. Restore
preserves generator state/used slots and pauses unfinished games. It rejects
inconsistent versions, coordinates, catalog pieces, strikes, clocks and end reasons.
JSON/platform storage lives in composeApp; trusted server score verification is F4.

## Tests

```sh
./gradlew --configure-on-demand :engine:jvmTest
```

Common tests cover both board sizes, collision and edges, simultaneous clears,
scoring, defensive snapshots, weighted generation, seed replay, hand refill,
no-fit detection before/after refill, timer boundaries, penalties, third strike,
pause and large elapsed values. Test reports: `engine/build/reports/tests/jvmTest`.

On macOS, select an installed simulator with `xcrun simctl list devices available`
and use its UDID:

```sh
# Apple Silicon; Intel Macs use :engine:iosX64Test instead.
./gradlew :engine:iosSimulatorArm64Test -PiosSimulatorId=<simulator-udid>
```

The macOS CI job chooses the host architecture and an available iPhone simulator.
Native compile/test execution remains unverified in this Linux workspace.
