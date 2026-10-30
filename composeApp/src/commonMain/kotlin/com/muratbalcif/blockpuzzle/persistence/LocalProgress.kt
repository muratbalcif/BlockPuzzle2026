package com.muratbalcif.blockpuzzle.persistence

import com.muratbalcif.blockpuzzle.engine.Difficulty
import com.muratbalcif.blockpuzzle.engine.GameConfig
import com.muratbalcif.blockpuzzle.engine.GameSnapshot

data class PlayerPreferences(
    val config: GameConfig = GameConfig(difficulty = Difficulty.EASY),
    val animations: Boolean = true,
    val haptics: Boolean = true,
)

data class ModeStats(
    val config: GameConfig,
    val started: Long = 0,
    val finished: Long = 0,
    val abandoned: Long = 0,
    val bestScore: Long? = null,
    val moves: Long = 0,
    val clearedLines: Long = 0,
    val playMillis: Long = 0,
)

data class LocalProgress(
    val preferences: PlayerPreferences = PlayerPreferences(),
    val resume: GameSnapshot? = null,
    val statistics: List<ModeStats> = emptyList(),
)

enum class LoadProblem { INVALID, UNSUPPORTED, READ_ERROR }
data class LoadedProgress(val data: LocalProgress, val problem: LoadProblem? = null)
