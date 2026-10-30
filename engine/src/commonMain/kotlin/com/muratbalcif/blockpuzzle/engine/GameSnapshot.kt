package com.muratbalcif.blockpuzzle.engine

/** Transport-neutral snapshot; restore validates all fields before constructing a session. */
data class GameSnapshot(
    val rulesVersion: Int = 1,
    val config: GameConfig,
    val occupiedCells: List<Cell>,
    val hand: List<List<Cell>?>,
    val generatorState: Long,
    val score: Long,
    val timeouts: Int,
    val remainingMillis: Long?,
    val endReason: EndReason?,
)
