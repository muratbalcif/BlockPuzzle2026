package com.blockpuzzle.game

data class Cell(val row: Int, val column: Int)
data class Piece(val cells: Set<Cell>) {
    init {
        require(cells.isNotEmpty())
        require(cells.all { it.row >= 0 && it.column >= 0 })
    }
}
data class Board(val size: Int = 8, val occupied: Set<Cell> = emptySet()) {
    init {
        require(size > 0)
        require(occupied.all { it.row in 0 until size && it.column in 0 until size })
    }
}
data class PlacementResult(val board: Board, val clearedLines: Int, val points: Int)

object GameEngine {
    fun canPlace(board: Board, piece: Piece, origin: Cell): Boolean =
        piece.cells.all { cell ->
            val target = Cell(origin.row + cell.row, origin.column + cell.column)
            target.row in 0 until board.size &&
                target.column in 0 until board.size &&
                target !in board.occupied
        }

    fun place(board: Board, piece: Piece, origin: Cell): PlacementResult? {
        if (!canPlace(board, piece, origin)) return null
        val placed = board.occupied + piece.cells.map {
            Cell(origin.row + it.row, origin.column + it.column)
        }
        val rows = (0 until board.size).filter { row ->
            (0 until board.size).all { column -> Cell(row, column) in placed }
        }.toSet()
        val columns = (0 until board.size).filter { column ->
            (0 until board.size).all { row -> Cell(row, column) in placed }
        }.toSet()
        val remaining = placed.filterNot { it.row in rows || it.column in columns }.toSet()
        val lines = rows.size + columns.size
        return PlacementResult(Board(board.size, remaining), lines, piece.cells.size + lines * lines * 10)
    }

    fun hasAnyMove(board: Board, pieces: List<Piece>): Boolean =
        pieces.any { piece ->
            (0 until board.size).any { row ->
                (0 until board.size).any { column -> canPlace(board, piece, Cell(row, column)) }
            }
        }
}
