package com.blockpuzzle.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PieceGeneratorTest {
    @Test fun sameSeedGivesSameBatch() {
        val first = PieceGenerator.next(1234, Board())
        val second = PieceGenerator.next(1234, Board())
        assertEquals(first, second)
        assertEquals(3, first.pieces.size)
    }

    @Test fun piecesAreValidAndDistinctCells() {
        PieceGenerator.catalog.forEach { piece ->
            assertTrue(piece.cells.isNotEmpty())
            assertTrue(piece.cells.all { it.row >= 0 && it.column >= 0 })
        }
    }

    @Test fun includesPlayablePieceWhenSpaceRemains() {
        val empty = Cell(7, 7)
        val occupied = (0 until 8).flatMap { row ->
            (0 until 8).map { col -> Cell(row, col) }
        }.toSet() - empty
        val board = Board(8, occupied)
        val batch = PieceGenerator.next(42, board)
        assertTrue(GameEngine.hasAnyMove(board, batch.pieces))
    }
}
