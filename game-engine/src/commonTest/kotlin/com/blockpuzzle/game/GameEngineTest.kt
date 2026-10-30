package com.blockpuzzle.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameEngineTest {
    private val single = Piece(setOf(Cell(0, 0)))

    @Test fun placesPiece() {
        val result = assertNotNull(GameEngine.place(Board(), single, Cell(2, 3)))
        assertTrue(Cell(2, 3) in result.board.occupied)
        assertEquals(1, result.points)
    }

    @Test fun rejectsOverlapAndOutOfBounds() {
        val board = Board(8, setOf(Cell(2, 3)))
        assertNull(GameEngine.place(board, single, Cell(2, 3)))
        assertFalse(GameEngine.canPlace(board, single, Cell(8, 0)))
    }

    @Test fun clearsFullRow() {
        val board = Board(8, (0..6).map { Cell(0, it) }.toSet())
        val result = assertNotNull(GameEngine.place(board, single, Cell(0, 7)))
        assertEquals(1, result.clearedLines)
        assertTrue(result.board.occupied.isEmpty())
    }

    @Test fun detectsNoMoves() {
        val full = Board(8, (0..7).flatMap { r -> (0..7).map { c -> Cell(r, c) } }.toSet())
        assertFalse(GameEngine.hasAnyMove(full, listOf(single)))
    }
}
