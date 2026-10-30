package com.muratbalcif.blockpuzzle.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BoardTest {
    private val single = Piece(listOf(Cell(0, 0)))
    private val square = Piece(listOf(Cell(0, 0), Cell(0, 1), Cell(1, 0), Cell(1, 1)))

    @Test
    fun bothBoardSizesAcceptEdgePlacementsWithoutChangingOriginal() {
        for (size in listOf(8, 16)) {
            val board = Board.empty(size)
            val result = assertNotNull(board.place(square, Cell(size - 2, size - 2)))
            assertEquals(4, result.board.occupiedCells.size)
            assertEquals(4L, result.points)
            assertTrue(board.occupiedCells.isEmpty())
            assertTrue(Cell(size - 1, size - 1) in result.board.occupiedCells)
        }
    }

    @Test
    fun outOfBoundsAndOverflowingOriginsAreRejected() {
        for (size in listOf(8, 16)) {
            val board = Board.empty(size)
            for (origin in listOf(
                Cell(-1, 0),
                Cell(0, -1),
                Cell(size - 1, size - 1),
                Cell(Int.MAX_VALUE, 0),
            )) {
                assertFalse(board.canPlace(square, origin))
                assertNull(board.place(square, origin))
            }
        }
    }

    @Test
    fun occupiedCellsRejectCollision() {
        val board = Board.fromCells(8, listOf(Cell(3, 3)))
        assertNull(board.place(square, Cell(2, 2)))
        assertEquals(setOf(Cell(3, 3)), board.occupiedCells)
    }

    @Test
    fun crossingRowAndColumnClearSimultaneously() {
        for (size in listOf(8, 16)) {
            val cells = (1 until size).flatMap { listOf(Cell(0, it), Cell(it, 0)) } + Cell(2, 2)
            val result = assertNotNull(Board.fromCells(size, cells).place(single, Cell(0, 0)))
            assertEquals(setOf(0), result.clearedRows)
            assertEquals(setOf(0), result.clearedColumns)
            assertEquals(2 * size - 1, result.clearedCellCount)
            assertEquals(21L, result.points)
            assertEquals(setOf(Cell(2, 2)), result.board.occupiedCells)
        }
    }

    @Test
    fun multipleRowsClearInOnePlacement() {
        val cells = (0..1).flatMap { row -> (0..5).map { Cell(row, it) } }
        val result = assertNotNull(Board.fromCells(8, cells).place(square, Cell(0, 6)))
        assertEquals(setOf(0, 1), result.clearedRows)
        assertTrue(result.clearedColumns.isEmpty())
        assertEquals(16, result.clearedCellCount)
        assertEquals(24L, result.points)
        assertTrue(result.board.occupiedCells.isEmpty())
    }

    @Test
    fun incompleteLinesStayOccupied() {
        val result = assertNotNull(Board.empty(16).place(square, Cell(5, 5)))
        assertTrue(result.clearedRows.isEmpty())
        assertTrue(result.clearedColumns.isEmpty())
        assertEquals(0, result.clearedCellCount)
        assertEquals(4L, result.points)
    }

    @Test
    fun fitSearchFindsLastAvailableLocation() {
        val board = Board.fromCells(
            8,
            (0..7).flatMap { row ->
                (0..7).map { Cell(row, it) }
            }.filter {
                it !=
                    Cell(7, 7)
            },
        )
        assertTrue(board.canFit(single))
        assertFalse(board.canFit(square))
        assertFalse(Board.empty(1).canFit(square))
    }

    @Test
    fun inputCollectionsCannotMutateModels() {
        val cells = mutableListOf(Cell(0, 0))
        val board = Board.fromCells(8, cells)
        val piece = Piece(cells)
        cells.add(Cell(1, 1))
        assertEquals(setOf(Cell(0, 0)), board.occupiedCells)
        assertEquals(setOf(Cell(0, 0)), piece.cells)
    }

    @Test
    fun returnedCollectionsCannotMutateModels() {
        val board = Board.fromCells(8, listOf(Cell(0, 0), Cell(1, 1)))
        val piece = Piece(listOf(Cell(0, 0), Cell(0, 1)))
        (board.occupiedCells as? MutableSet<Cell>)?.clear()
        (piece.cells as? MutableSet<Cell>)?.clear()
        assertEquals(2, board.occupiedCells.size)
        assertEquals(2, piece.cells.size)
    }

    @Test
    fun invalidModelsAreRejected() {
        assertFailsWith<IllegalArgumentException> { Board.empty(0) }
        assertFailsWith<IllegalArgumentException> { Board.fromCells(8, listOf(Cell(8, 0))) }
        assertFailsWith<IllegalArgumentException> { Piece(emptyList()) }
        assertFailsWith<IllegalArgumentException> { Piece(listOf(Cell(-1, 0))) }
        assertFailsWith<IllegalArgumentException> { Piece(listOf(Cell(0, 0), Cell(0, 0))) }
        assertFailsWith<IllegalArgumentException> { Piece(listOf(Cell(1, 1))) }
    }

    @Test
    fun completingAnOffCenterColumnLeavesOtherCellsAlone() {
        val cells = (0..6).map { Cell(it, 6) } + Cell(2, 2)
        val result = assertNotNull(Board.fromCells(8, cells).place(single, Cell(7, 6)))
        assertTrue(result.clearedRows.isEmpty())
        assertEquals(setOf(6), result.clearedColumns)
        assertEquals(8, result.clearedCellCount)
        assertEquals(11L, result.points)
        assertEquals(setOf(Cell(2, 2)), result.board.occupiedCells)
    }
}
