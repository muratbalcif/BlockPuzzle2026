package com.muratbalcif.blockpuzzle.engine

/** Park–Miller PRNG. Long arithmetic is identical on JVM and Kotlin/Native. */
class PieceGenerator private constructor(val state: Long) {
    fun nextHand(config: GameConfig): GeneratedHand {
        val candidates = PieceCatalog.all.filter {
            it.height <= config.boardSize &&
                it.width <= config.boardSize
        }
        val weights = candidates.map { piece ->
            when (config.difficulty) {
                Difficulty.EASY -> if (piece.cellCount <= 3) 4 else 1
                Difficulty.MEDIUM -> 1
                Difficulty.HARD -> if (piece.cellCount <= 3) 1 else 4
            }
        }
        val totalWeight = weights.sum()
        var nextState = state
        val pieces = List(3) {
            nextState = (nextState * 16_807L) % MODULUS
            var draw = (nextState % totalWeight).toInt()
            var index = 0
            while (draw >= weights[index]) {
                draw -= weights[index]
                index++
            }
            candidates[index]
        }
        return GeneratedHand(pieces, PieceGenerator(nextState))
    }

    companion object {
        private const val MODULUS = 2_147_483_647L

        internal fun fromState(state: Long): PieceGenerator {
            require(state in 1 until MODULUS) { "Invalid generator state" }
            return PieceGenerator(state)
        }

        /** Any Long seed, including negatives, maps safely to 1..MODULUS-1. */
        fun seeded(seed: Long): PieceGenerator {
            val range = MODULUS - 1
            val state = ((seed % range) + range) % range + 1
            return PieceGenerator(state)
        }
    }
}

class GeneratedHand internal constructor(pieces: List<Piece>, val generator: PieceGenerator) {
    private val hand = pieces.toList()
    val pieces: List<Piece> get() = hand.toList()
}
