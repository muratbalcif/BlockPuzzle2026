package com.muratbalcif.blockpuzzle.engine

/** Normalized cell offsets; rotations are distinct pieces, not player actions. */
class Piece(cells: Collection<Cell>) {
    private val offsets = cells.toSet()
    val cells: Set<Cell> get() = offsets.toSet()
    val cellCount: Int get() = offsets.size
    val height: Int
    val width: Int

    init {
        require(offsets.isNotEmpty()) { "A piece must contain at least one cell" }
        require(offsets.size == cells.size) { "Duplicate piece cells" }
        require(
            offsets.all {
                it.row in 0 until Int.MAX_VALUE && it.column in 0 until Int.MAX_VALUE
            },
        )
        require(
            offsets.minOf {
                it.row
            } == 0 &&
                offsets.minOf { it.column } == 0,
        ) { "Offsets must be normalized" }
        height = offsets.maxOf { it.row } + 1
        width = offsets.maxOf { it.column } + 1
    }

    override fun equals(other: Any?): Boolean = other is Piece && offsets == other.offsets
    override fun hashCode(): Int = offsets.hashCode()
    override fun toString(): String = "Piece($offsets)"
}
