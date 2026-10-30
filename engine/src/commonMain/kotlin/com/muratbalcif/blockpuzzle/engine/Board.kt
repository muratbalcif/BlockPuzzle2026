package com.muratbalcif.blockpuzzle.engine

/** Immutable square board. Coordinates use row/column, with (0,0) at top left. */
class Board private constructor(val size: Int, cells: Collection<Cell>) {
    private val occupied = cells.toSet()
    val occupiedCells: Set<Cell> get() = occupied.toSet()

    init {
        require(size > 0) { "Board size must be positive" }
        require(
            occupied.all {
                it.row in 0 until size && it.column in 0 until size
            },
        ) { "Cell outside board" }
    }

    fun canPlace(piece: Piece, origin: Cell): Boolean {
        if (origin.row !in 0 until size || origin.column !in 0 until size) return false
        // Subtraction avoids overflow from an adversarial origin + piece offset.
        if (piece.height > size - origin.row || piece.width > size - origin.column) return false
        return piece.cells.none { Cell(origin.row + it.row, origin.column + it.column) in occupied }
    }

    fun place(piece: Piece, origin: Cell): Placement? {
        if (!canPlace(piece, origin)) return null
        val placed =
            occupied + piece.cells.map { Cell(origin.row + it.row, origin.column + it.column) }
        val rows = placed.groupingBy { it.row }.eachCount().filterValues { it == size }.keys
        val columns = placed.groupingBy { it.column }.eachCount().filterValues { it == size }.keys
        val remaining = placed.filterNot { it.row in rows || it.column in columns }
        return Placement(
            board = Board(size, remaining),
            clearedRows = rows.toSet(),
            clearedColumns = columns.toSet(),
            clearedCellCount = placed.size - remaining.size,
            points = piece.cellCount.toLong() + 10L * (rows.size + columns.size),
        )
    }

    fun canFit(piece: Piece): Boolean {
        if (piece.height > size || piece.width > size) return false
        for (row in 0..size - piece.height) {
            for (column in 0..size - piece.width) {
                if (canPlace(piece, Cell(row, column))) return true
            }
        }
        return false
    }

    companion object {
        fun empty(size: Int = 8): Board = Board(size, emptySet())
        fun fromCells(size: Int, cells: Collection<Cell>): Board = Board(size, cells)
    }
}

class Placement internal constructor(
    val board: Board,
    clearedRows: Set<Int>,
    clearedColumns: Set<Int>,
    val clearedCellCount: Int,
    val points: Long,
) {
    private val rows = clearedRows.toSet()
    private val columns = clearedColumns.toSet()
    val clearedRows: Set<Int> get() = rows.toSet()
    val clearedColumns: Set<Int> get() = columns.toSet()
}
