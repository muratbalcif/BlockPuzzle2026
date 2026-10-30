package com.muratbalcif.blockpuzzle.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PieceGeneratorTest {
    @Test
    fun sameSeedReproducesWholeSequence() {
        val config = GameConfig()
        var first = PieceGenerator.seeded(1234)
        var second = PieceGenerator.seeded(1234)
        repeat(30) {
            val a = first.nextHand(config)
            val b = second.nextHand(config)
            assertEquals(a.pieces, b.pieces)
            assertEquals(3, a.pieces.size)
            first = a.generator
            second = b.generator
        }
    }

    @Test
    fun generatorHasStableCrossPlatformGoldenState() {
        val generator = PieceGenerator.seeded(0)
        val hand = generator.nextHand(GameConfig())
        assertEquals(1622650073L, hand.generator.state)
        assertEquals(1L, generator.state)
    }

    @Test
    fun differentSeedsAndSubsequentHandsVary() {
        val first = PieceGenerator.seeded(1).nextHand(GameConfig())
        val second = PieceGenerator.seeded(2).nextHand(GameConfig())
        assertNotEquals(first.pieces, second.pieces)
        assertNotEquals(first.pieces, first.generator.nextHand(GameConfig()).pieces)
    }

    @Test
    fun everyDifficultyGeneratesPiecesFittingEverySupportedSize() {
        for (size in listOf(1, 2, 8, 16)) {
            for (difficulty in Difficulty.entries) {
                var generator = PieceGenerator.seeded(Long.MIN_VALUE)
                repeat(20) {
                    val hand = generator.nextHand(GameConfig(size, difficulty))
                    assertTrue(hand.pieces.all { Board.empty(size).canFit(it) })
                    generator = hand.generator
                }
            }
        }
    }

    @Test
    fun difficultyChangesActualPieceDistribution() {
        fun smallCount(difficulty: Difficulty): Int {
            var generator = PieceGenerator.seeded(7)
            var count = 0
            repeat(2000) {
                val hand = generator.nextHand(GameConfig(difficulty = difficulty))
                count += hand.pieces.count { it.cellCount <= 3 }
                generator = hand.generator
            }
            return count
        }
        val easy = smallCount(Difficulty.EASY)
        val medium = smallCount(Difficulty.MEDIUM)
        val hard = smallCount(Difficulty.HARD)
        assertTrue(easy > medium + 1000)
        assertTrue(medium > hard + 500)
    }

    @Test
    fun catalogHasUniqueConnectedNormalizedShapes() {
        val pieces = PieceCatalog.all
        assertEquals(pieces.size, pieces.toSet().size)
        assertTrue(pieces.any { it.cellCount == 1 })
        assertTrue(pieces.any { it.cellCount == 9 })
        assertTrue(pieces.any { it.height == 5 && it.width == 1 })
        assertTrue(pieces.any { it.width == 5 && it.height == 1 })
        for (piece in pieces) {
            val visited = mutableSetOf(piece.cells.first())
            do {
                val before = visited.size
                visited += piece.cells.filter { cell ->
                    visited.any {
                        kotlin.math.abs(it.row - cell.row) +
                            kotlin.math.abs(it.column - cell.column) ==
                            1
                    }
                }
            } while (visited.size != before)
            assertEquals(piece.cellCount, visited.size)
        }
    }

    @Test
    fun gameConfigRejectsInvalidBoardSizes() {
        assertFailsWith<IllegalArgumentException> { GameConfig(boardSize = 0) }
        assertFailsWith<IllegalArgumentException> { GameConfig(boardSize = -8) }
    }

    @Test
    fun seedZeroProducesStablePieceSequencesAcrossTwoHands() {
        val first = PieceGenerator.seeded(0).nextHand(GameConfig())
        assertEquals(
            listOf(
                setOf(Cell(0, 0), Cell(0, 1), Cell(0, 2), Cell(0, 3)),
                setOf(Cell(0, 1), Cell(1, 0), Cell(1, 1), Cell(1, 2)),
                setOf(Cell(0, 0), Cell(0, 1), Cell(0, 2), Cell(1, 1)),
            ),
            first.pieces.map { it.cells },
        )
        val second = first.generator.nextHand(GameConfig())
        assertEquals(
            listOf(
                setOf(Cell(0, 0), Cell(1, 0)),
                setOf(Cell(0, 0), Cell(0, 1), Cell(1, 0), Cell(2, 0)),
                setOf(Cell(0, 0), Cell(0, 1), Cell(1, 1), Cell(2, 1)),
            ),
            second.pieces.map { it.cells },
        )
    }
}
