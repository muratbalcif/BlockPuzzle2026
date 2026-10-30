package com.muratbalcif.blockpuzzle.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GameSessionTest {
    @Test
    fun newGameHasThreePiecesAndIndependentModeChoices() {
        for (size in listOf(8, 16)) {
            for (difficulty in Difficulty.entries) {
                val relaxed = GameSession.start(GameConfig(size, difficulty), 42)
                val fast = GameSession.start(GameConfig(size, difficulty, Tempo.FAST), 42)
                assertEquals(relaxed.hand, fast.hand)
                assertEquals(3, fast.hand.size)
                assertTrue(fast.hand.all { it != null })
                assertEquals(20_000L, fast.remainingMillis)
                assertNull(relaxed.remainingMillis)
                assertEquals(GameStatus.ACTIVE, fast.status)
            }
        }
    }

    @Test
    fun successfulPlacementConsumesOneSlotAndResetsTimer() {
        val initial = GameSession.start(GameConfig(16, tempo = Tempo.FAST), 42).advanceTime(7000)
        val piece = assertNotNull(initial.hand[0])
        val move = assertIs<MoveResult.Placed>(initial.place(0, Cell(0, 0)))
        assertEquals(piece.cellCount.toLong(), move.game.score)
        assertNull(move.game.hand[0])
        assertNotNull(move.game.hand[1])
        assertNotNull(move.game.hand[2])
        assertEquals(20_000L, move.game.remainingMillis)
        assertTrue(initial.board.occupiedCells.isEmpty())
        assertNotNull(initial.hand[0])
        assertEquals(13_000L, initial.remainingMillis)
    }

    @Test
    fun handRefillsOnlyAfterAllThreePiecesAreUsed() {
        val config = GameConfig(16)
        val initial = GameSession.start(config, 42)
        val first = assertIs<MoveResult.Placed>(initial.place(0, Cell(0, 0))).game
        val second = assertIs<MoveResult.Placed>(first.place(1, Cell(5, 0))).game
        assertEquals(1, second.hand.count { it != null })
        val third = assertIs<MoveResult.Placed>(second.place(2, Cell(10, 0))).game
        assertEquals(3, third.hand.count { it != null })
        val expectedHand = PieceGenerator.seeded(
            42,
        ).nextHand(config).generator.nextHand(config).pieces
        assertEquals(expectedHand, third.hand)
    }

    @Test
    fun rejectedMovesPreserveEntireSessionAndRandomSequence() {
        val initial = GameSession.start(GameConfig(16, tempo = Tempo.FAST), 42).advanceTime(1234)
        for (slot in listOf(-1, 3, Int.MAX_VALUE)) {
            val rejected = assertIs<MoveResult.Rejected>(initial.place(slot, Cell(0, 0)))
            assertEquals(MoveRejection.INVALID_SLOT, rejected.reason)
            assertSame(initial, rejected.game)
        }
        val outside = assertIs<MoveResult.Rejected>(initial.place(0, Cell(-1, 0)))
        assertEquals(MoveRejection.INVALID_PLACEMENT, outside.reason)
        assertSame(initial, outside.game)
        val placed = assertIs<MoveResult.Placed>(initial.place(0, Cell(0, 0))).game
        val used = assertIs<MoveResult.Rejected>(placed.place(0, Cell(5, 0)))
        assertEquals(MoveRejection.EMPTY_SLOT, used.reason)
        assertSame(placed, used.game)
        val collision = assertIs<MoveResult.Rejected>(placed.place(1, Cell(0, 0)))
        assertEquals(MoveRejection.INVALID_PLACEMENT, collision.reason)
        assertSame(placed, collision.game)
    }

    @Test
    fun timerBoundariesFollowEachSelectedTempo() {
        for ((tempo, limit) in listOf(
            Tempo.SLOW to 90_000L,
            Tempo.MEDIUM to 45_000L,
            Tempo.FAST to 20_000L,
        )) {
            val initial = GameSession.start(GameConfig(tempo = tempo), 42)
            val before = initial.advanceTime(limit - 1)
            assertEquals(0, before.timeouts)
            assertEquals(1L, before.remainingMillis)
            val boundary = before.advanceTime(1)
            assertEquals(1, boundary.timeouts)
            assertEquals(limit, boundary.remainingMillis)
            assertEquals(GameStatus.ACTIVE, boundary.status)
        }
    }

    @Test
    fun timeoutDeductsTenPointsAndFloorsScoreAtZero() {
        val initial = GameSession.start(GameConfig(1, tempo = Tempo.FAST), 42)
        val scored = assertIs<MoveResult.Placed>(initial.place(0, Cell(0, 0))).game
        assertEquals(21L, scored.score)
        val first = scored.advanceTime(20_000)
        assertEquals(11L, first.score)
        assertEquals(1, first.timeouts)
        assertEquals(GameStatus.ACTIVE, first.status)
        val second = first.advanceTime(20_000)
        assertEquals(1L, second.score)
        val third = second.advanceTime(20_000)
        assertEquals(0L, third.score)
        assertEquals(3, third.timeouts)
        assertEquals(EndReason.TIMEOUT_LIMIT, third.endReason)
        assertEquals(GameStatus.FINISHED, third.status)
        assertEquals(0L, third.remainingMillis)
    }

    @Test
    fun placementDoesNotRestoreTimeoutRights() {
        val initial = GameSession.start(GameConfig(1, tempo = Tempo.FAST), 42).advanceTime(20_000)
        val placed = assertIs<MoveResult.Placed>(initial.place(0, Cell(0, 0))).game
        assertEquals(1, placed.timeouts)
        assertEquals(20_000L, placed.remainingMillis)
        assertEquals(GameStatus.FINISHED, placed.advanceTime(40_000).status)
    }

    @Test
    fun delayedUpdateCountsAllCompletePeriodsAndKeepsRemainder() {
        val initial = GameSession.start(GameConfig(tempo = Tempo.FAST), 42)
        val result = initial.advanceTime(45_001)
        assertEquals(2, result.timeouts)
        assertEquals(14_999L, result.remainingMillis)
        assertEquals(GameStatus.ACTIVE, result.status)
        assertEquals(0L, result.score)
    }

    @Test
    fun hugeElapsedTimeEndsAtThirdStrikeWithoutOverflow() {
        val result = GameSession.start(
            GameConfig(tempo = Tempo.FAST),
            42,
        ).advanceTime(Long.MAX_VALUE)
        assertEquals(3, result.timeouts)
        assertEquals(GameStatus.FINISHED, result.status)
        assertEquals(EndReason.TIMEOUT_LIMIT, result.endReason)
        assertEquals(0L, result.score)
    }

    @Test
    fun elapsedTimeIsInvariantToUpdateChunking() {
        val initial = GameSession.start(GameConfig(tempo = Tempo.FAST), 42)
        val one = initial.advanceTime(45_001)
        val chunks = initial.advanceTime(1000).advanceTime(18_999).advanceTime(25_002)
        assertEquals(one.timeouts, chunks.timeouts)
        assertEquals(one.remainingMillis, chunks.remainingMillis)
        assertEquals(one.score, chunks.score)
        assertEquals(one.status, chunks.status)
    }

    @Test
    fun relaxedModeNeverTimesOut() {
        val initial = GameSession.start(GameConfig(), 42)
        assertSame(initial, initial.advanceTime(Long.MAX_VALUE))
        assertEquals(0, initial.timeouts)
        assertNull(initial.remainingMillis)
    }

    @Test
    fun pauseFreezesTimeAndBlocksMovesWithoutResettingRemainingTime() {
        val initial = GameSession.start(GameConfig(tempo = Tempo.FAST), 42).advanceTime(1234)
        val paused = initial.pause()
        assertEquals(GameStatus.PAUSED, paused.status)
        assertSame(paused, paused.advanceTime(Long.MAX_VALUE))
        val rejected = assertIs<MoveResult.Rejected>(paused.place(0, Cell(0, 0)))
        assertEquals(MoveRejection.PAUSED, rejected.reason)
        assertSame(paused, rejected.game)
        val resumed = paused.resume()
        assertEquals(GameStatus.ACTIVE, resumed.status)
        assertEquals(18_766L, resumed.remainingMillis)
        assertIs<MoveResult.Placed>(resumed.place(0, Cell(0, 0)))
    }

    @Test
    fun noRemainingPieceFittingEndsGameDespiteEmptyCells() {
        // Seed 30 starts with a monomino, a Z tetromino and a vertical length-5 line.
        val occupied = (0..7).flatMap { row -> (0..7).map { Cell(row, it) } }.filter {
            (
                it.row +
                    it.column
                ) %
                2 ==
                1
        }
        val initial = GameSession.start(GameConfig(), 30, Board.fromCells(8, occupied))
        assertEquals(1, initial.hand[0]?.cellCount)
        assertEquals(GameStatus.ACTIVE, initial.status)
        val placed = assertIs<MoveResult.Placed>(initial.place(0, Cell(0, 0))).game
        assertEquals(33, placed.board.occupiedCells.size)
        assertEquals(EndReason.NO_MOVES, placed.endReason)
        assertEquals(GameStatus.FINISHED, placed.status)
    }

    @Test
    fun fullInitialBoardIsImmediatelyTerminal() {
        val cells = (0..7).flatMap { row -> (0..7).map { Cell(row, it) } }
        val game = GameSession.start(GameConfig(), 42, Board.fromCells(8, cells))
        assertEquals(EndReason.NO_MOVES, game.endReason)
        assertEquals(GameStatus.FINISHED, game.status)
    }

    @Test
    fun finishedGameCannotChangeThroughAnyAction() {
        val ended = GameSession.start(GameConfig(tempo = Tempo.FAST), 42).advanceTime(60_000)
        assertSame(ended, ended.advanceTime(20_000))
        assertSame(ended, ended.pause())
        assertSame(ended, ended.resume())
        val rejected = assertIs<MoveResult.Rejected>(ended.place(0, Cell(0, 0)))
        assertEquals(MoveRejection.FINISHED, rejected.reason)
        assertSame(ended, rejected.game)
    }

    @Test
    fun invalidElapsedTimeAndMismatchedBoardAreRejected() {
        val game = GameSession.start(GameConfig(), 42)
        assertFailsWith<IllegalArgumentException> { game.advanceTime(-1) }
        assertFailsWith<IllegalArgumentException> {
            GameSession.start(GameConfig(16), 42, Board.empty(8))
        }
    }

    @Test
    fun callersCannotChangeTheAvailableHand() {
        val game = GameSession.start(GameConfig(), 42)
        (game.hand as? MutableList<Piece?>)?.clear()
        assertEquals(3, game.hand.size)
        assertTrue(game.hand.all { it != null })
    }

    @Test
    fun newlyRefilledHandIsCheckedForGameOver() {
        val zCells = listOf(Cell(2, 2), Cell(2, 3), Cell(3, 3), Cell(3, 4))
        val lineCells = (0..4).map { Cell(it, 6) }
        val holes = zCells + lineCells
        val occupied = (0..7).flatMap { row -> (0..7).map { Cell(row, it) } }
            .filter { (it.row + it.column) % 2 == 1 && it !in holes }
        val initial = GameSession.start(GameConfig(), 30, Board.fromCells(8, occupied))
        val first = assertIs<MoveResult.Placed>(initial.place(0, Cell(7, 7))).game
        val second = assertIs<MoveResult.Placed>(first.place(1, Cell(2, 2))).game
        assertEquals(GameStatus.ACTIVE, second.status)
        val third = assertIs<MoveResult.Placed>(second.place(2, Cell(0, 6))).game
        assertEquals(3, third.hand.count { it != null })
        assertEquals(EndReason.NO_MOVES, third.endReason)
        assertEquals(GameStatus.FINISHED, third.status)
    }
}
