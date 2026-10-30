package com.muratbalcif.blockpuzzle.engine

/** Catalog order is part of the seeded generator's reproducibility contract. */
object PieceCatalog {
    private val shapes: List<Piece> = buildList {
        add(Piece(listOf(Cell(0, 0))))
        for (length in 2..5) {
            add(Piece((0 until length).map { Cell(0, it) }))
            add(Piece((0 until length).map { Cell(it, 0) }))
        }
        for (size in 2..3) {
            add(Piece((0 until size).flatMap { row -> (0 until size).map { Cell(row, it) } }))
        }
        val bases = listOf(
            listOf(Cell(0, 0), Cell(1, 0), Cell(1, 1)),
            listOf(Cell(0, 0), Cell(1, 0), Cell(2, 0), Cell(2, 1)),
            listOf(Cell(0, 1), Cell(1, 1), Cell(2, 1), Cell(2, 0)),
            listOf(Cell(0, 0), Cell(0, 1), Cell(0, 2), Cell(1, 1)),
            listOf(Cell(0, 0), Cell(0, 1), Cell(1, 1), Cell(1, 2)),
            listOf(Cell(0, 1), Cell(0, 2), Cell(1, 0), Cell(1, 1)),
        )
        for (base in bases) {
            var cells = base
            repeat(4) {
                add(Piece(cells))
                val rotated = cells.map { Cell(it.column, -it.row) }
                val minRow = rotated.minOf { it.row }
                val minColumn = rotated.minOf { it.column }
                cells = rotated.map { Cell(it.row - minRow, it.column - minColumn) }
            }
        }
    }.distinct()

    val all: List<Piece> get() = shapes.toList()
}
