package com.blockpuzzle.game

/**
 * Pure, seeded piece selection. The next seed is returned so a saved game
 * can reproduce subsequent piece batches across Android and iOS.
 */
object PieceGenerator {
    val catalog: List<Piece> = listOf(
        piece(0 to 0),
        piece(0 to 0, 0 to 1),
        piece(0 to 0, 1 to 0),
        piece(0 to 0, 0 to 1, 0 to 2),
        piece(0 to 0, 1 to 0, 2 to 0),
        piece(0 to 0, 0 to 1, 1 to 0, 1 to 1),
        piece(0 to 0, 1 to 0, 2 to 0, 2 to 1),
        piece(0 to 0, 0 to 1, 1 to 1, 1 to 2),
        piece(0 to 0, 0 to 1, 0 to 2, 1 to 1),
        piece(0 to 0, 1 to 0, 2 to 0, 3 to 0)
    )

    data class Batch(val pieces: List<Piece>, val nextSeed: Long)

    fun next(seed: Long, board: Board, count: Int = 3): Batch {
        require(count > 0)
        var state = seed
        val result = ArrayList<Piece>(count)
        repeat(count) {
            state = (state * 1664525L + 1013904223L) and 0xFFFFFFFFL
            result += catalog[(state % catalog.size).toInt()]
        }
        // Guarantee at least one playable option when the board has any legal move.
        if (!GameEngine.hasAnyMove(board, result)) {
            val playable = catalog.firstOrNull { GameEngine.hasAnyMove(board, listOf(it)) }
            if (playable != null) result[0] = playable
        }
        return Batch(result, state)
    }

    private fun piece(vararg positions: Pair<Int, Int>): Piece =
        Piece(positions.map { Cell(it.first, it.second) }.toSet())
}
