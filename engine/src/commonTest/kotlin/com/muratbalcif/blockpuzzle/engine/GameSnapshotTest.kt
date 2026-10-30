package com.muratbalcif.blockpuzzle.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class GameSnapshotTest {
    @Test
    fun restorePreservesUsedSlotsScoreClockStrikesAndGeneratorContinuation() {
        val config = GameConfig(16, tempo = Tempo.FAST)
        var original = GameSession.start(config, 42)
        original = assertIs<MoveResult.Placed>(original.place(0, Cell(0, 0))).game
        original = original.advanceTime(21_234)
        val snapshot = original.snapshot()
        var restored = GameSession.restore(snapshot)
        assertEquals(GameStatus.PAUSED, restored.status)
        assertEquals(snapshot, restored.snapshot())
        assertNull(restored.hand[0])
        restored = restored.resume()
        for ((slot, row) in listOf(1 to 5, 2 to 10)) {
            original = assertIs<MoveResult.Placed>(original.place(slot, Cell(row, 0))).game
            restored = assertIs<MoveResult.Placed>(restored.place(slot, Cell(row, 0))).game
        }
        assertEquals(original.snapshot(), restored.snapshot())
    }

    @Test
    fun restoredPausedGameIgnoresOfflineTime() {
        val before = GameSession.start(GameConfig(tempo = Tempo.SLOW), 42).advanceTime(1234)
        val restored = GameSession.restore(before.snapshot()).advanceTime(Long.MAX_VALUE)
        assertEquals(88_766L, restored.remainingMillis)
        assertEquals(0, restored.timeouts)
        assertEquals(before.snapshot(), restored.snapshot())
    }

    @Test
    fun finishedSnapshotStaysFinished() {
        val ended = GameSession.start(GameConfig(tempo = Tempo.FAST), 42).advanceTime(60_000)
        val restored = GameSession.restore(ended.snapshot()).resume()
        assertEquals(GameStatus.FINISHED, restored.status)
        assertEquals(EndReason.TIMEOUT_LIMIT, restored.endReason)
    }

    @Test
    fun malformedSnapshotsAreRejected() {
        val timed = GameSession.start(GameConfig(tempo = Tempo.FAST), 42).snapshot()
        val invalid = listOf(
            timed.copy(rulesVersion = 99),
            timed.copy(score = -1),
            timed.copy(timeouts = -1),
            timed.copy(timeouts = 3),
            timed.copy(remainingMillis = 0),
            timed.copy(remainingMillis = 20_001),
            timed.copy(remainingMillis = null),
            timed.copy(generatorState = 0),
            timed.copy(generatorState = 2_147_483_647),
            timed.copy(hand = emptyList()),
            timed.copy(hand = listOf(null, null, null)),
            timed.copy(hand = listOf(listOf(Cell(0, 0), Cell(0, 2)), null, null)),
            timed.copy(occupiedCells = listOf(Cell(8, 0))),
            timed.copy(occupiedCells = listOf(Cell(1, 1), Cell(1, 1))),
            timed.copy(endReason = EndReason.TIMEOUT_LIMIT),
            timed.copy(endReason = EndReason.NO_MOVES),
        )
        for (snapshot in invalid) {
            assertFailsWith<IllegalArgumentException> { GameSession.restore(snapshot) }
        }
        val relaxed = GameSession.start(GameConfig(), 42).snapshot()
        assertFailsWith<IllegalArgumentException> {
            GameSession.restore(relaxed.copy(timeouts = 1))
        }
        assertFailsWith<IllegalArgumentException> {
            GameSession.restore(relaxed.copy(remainingMillis = 10))
        }
    }

    @Test
    fun noMovesSnapshotMustAgreeWithBoardAndHand() {
        val full = (0..7).flatMap { row -> (0..7).map { Cell(row, it) } }
        val ended = GameSession.start(GameConfig(), 42, Board.fromCells(8, full))
        assertEquals(GameStatus.FINISHED, GameSession.restore(ended.snapshot()).status)
        assertFailsWith<IllegalArgumentException> {
            GameSession.restore(ended.snapshot().copy(endReason = null))
        }
    }

    @Test
    fun restoredExtremeScoreDoesNotOverflowOnPlacement() {
        val config = GameConfig(1)
        val snapshot = GameSession.start(config, 42).snapshot().copy(score = Long.MAX_VALUE)
        val game = GameSession.restore(snapshot).resume()
        val result = assertIs<MoveResult.Placed>(game.place(0, Cell(0, 0))).game
        assertEquals(Long.MAX_VALUE, result.score)
    }

    @Test
    fun exportedAndImportedCollectionsCannotMutateLiveSessions() {
        val game = GameSession.start(GameConfig(), 42)
        val exported = game.snapshot()
        (exported.hand[0] as? MutableList<Cell>)?.set(0, Cell(7, 7))
        kotlin.test.assertFalse(Cell(7, 7) in game.hand[0]!!.cells)
        assertEquals(4, game.hand[0]?.cellCount)
        val snapshot = game.snapshot()
        val cells = snapshot.occupiedCells.toMutableList()
        val hand = snapshot.hand.map { it?.toMutableList() }.toMutableList()
        val restored = GameSession.restore(snapshot.copy(occupiedCells = cells, hand = hand))
        cells.add(Cell(7, 7))
        hand[0]?.clear()
        assertEquals(0, restored.board.occupiedCells.size)
        assertEquals(4, restored.hand[0]?.cellCount)
    }
}
