package com.muratbalcif.blockpuzzle.engine

enum class Difficulty { EASY, MEDIUM, HARD }

enum class Tempo(val moveLimitMillis: Long?) {
    RELAXED(null),
    SLOW(90_000L),
    MEDIUM(45_000L),
    FAST(20_000L),
}

data class GameConfig(
    val boardSize: Int = 8,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val tempo: Tempo = Tempo.RELAXED,
) {
    init {
        require(boardSize > 0) { "Board size must be positive" }
    }
}
