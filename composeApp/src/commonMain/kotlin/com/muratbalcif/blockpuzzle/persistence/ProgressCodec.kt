package com.muratbalcif.blockpuzzle.persistence

import com.muratbalcif.blockpuzzle.engine.Cell
import com.muratbalcif.blockpuzzle.engine.Difficulty
import com.muratbalcif.blockpuzzle.engine.EndReason
import com.muratbalcif.blockpuzzle.engine.GameConfig
import com.muratbalcif.blockpuzzle.engine.GameSession
import com.muratbalcif.blockpuzzle.engine.GameSnapshot
import com.muratbalcif.blockpuzzle.engine.Tempo
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object ProgressCodec {
    private const val MAX_CHARS = 1_048_576
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(data: LocalProgress): String {
        validate(data)
        return json.encodeToString(
            StoredDocument(
                1,
                StoredPreferences(
                    StoredConfig.from(data.preferences.config),
                    data.preferences.animations,
                    data.preferences.haptics,
                ),
                data.resume?.let(StoredSession::from),
                data.statistics.map(StoredStats::from),
            ),
        ).also { require(it.length <= MAX_CHARS) }
    }

    fun decode(raw: String?): LoadedProgress {
        if (raw == null) return LoadedProgress(LocalProgress())
        if (raw.length > MAX_CHARS) return LoadedProgress(LocalProgress(), LoadProblem.INVALID)
        return try {
            val element = json.parseToJsonElement(raw)
            val format = element.jsonObject["format"]?.jsonPrimitive?.intOrNull
                ?: return LoadedProgress(LocalProgress(), LoadProblem.INVALID)
            if (format != 1) return LoadedProgress(LocalProgress(), LoadProblem.UNSUPPORTED)
            val stored = json.decodeFromJsonElement<StoredDocument>(element)
            val data = LocalProgress(
                PlayerPreferences(
                    stored.preferences.config.domain(),
                    stored.preferences.animations,
                    stored.preferences.haptics,
                ),
                stored.resume?.domain(),
                stored.statistics.map { it.domain() },
            )
            validate(data)
            LoadedProgress(data)
        } catch (_: IllegalArgumentException) {
            LoadedProgress(LocalProgress(), LoadProblem.INVALID)
        }
    }

    private fun validate(data: LocalProgress) {
        require(data.preferences.config.boardSize in listOf(8, 16))
        require(
            data.statistics.size <= 24 &&
                data.statistics.map { it.config }.distinct().size == data.statistics.size,
        )
        var openGames = 0L
        for (stats in data.statistics) {
            require(stats.config.boardSize in listOf(8, 16))
            val counters =
                listOf(
                    stats.started,
                    stats.finished,
                    stats.abandoned,
                    stats.moves,
                    stats.clearedLines,
                    stats.playMillis,
                )
            require(counters.all { it in 0..Long.MAX_VALUE / 1024 })
            require(
                stats.finished <= stats.started &&
                    stats.abandoned <= stats.started - stats.finished,
            )
            require((stats.finished == 0L) == (stats.bestScore == null))
            require(stats.bestScore == null || stats.bestScore >= 0)
            openGames += stats.started - stats.finished - stats.abandoned
        }
        val resume = data.resume
        require(openGames == if (resume == null) 0L else 1L)
        if (resume != null) {
            require(resume.config.boardSize in listOf(8, 16))
            require(resume.occupiedCells.size <= resume.config.boardSize * resume.config.boardSize)
            require(resume.hand.size == 3 && resume.hand.filterNotNull().all { it.size <= 9 })
            require(resume.endReason == null)
            GameSession.restore(resume)
            val stats = data.statistics.singleOrNull { it.config == resume.config }
                ?: throw IllegalArgumentException("Missing resume mode statistics")
            require(stats.started - stats.finished - stats.abandoned == 1L)
        }
    }
}

@Serializable
private data class StoredDocument(
    val format: Int,
    val preferences: StoredPreferences,
    val resume: StoredSession?,
    val statistics: List<StoredStats>,
)

@Serializable
private data class StoredPreferences(
    val config: StoredConfig,
    val animations: Boolean,
    val haptics: Boolean,
)

@Serializable
private data class StoredConfig(val boardSize: Int, val difficulty: String, val tempo: String) {
    fun domain(): GameConfig =
        GameConfig(boardSize, Difficulty.valueOf(difficulty), Tempo.valueOf(tempo))
    companion object {
        fun from(config: GameConfig): StoredConfig =
            StoredConfig(config.boardSize, config.difficulty.name, config.tempo.name)
    }
}

@Serializable
private data class StoredCell(val row: Int, val column: Int) {
    fun domain(): Cell = Cell(row, column)
    companion object {
        fun from(cell: Cell): StoredCell = StoredCell(cell.row, cell.column)
    }
}

@Serializable
private data class StoredSession(
    val rulesVersion: Int,
    val config: StoredConfig,
    val occupiedCells: List<StoredCell>,
    val hand: List<List<StoredCell>?>,
    val generatorState: Long,
    val score: Long,
    val timeouts: Int,
    val remainingMillis: Long?,
    val endReason: String?,
) {
    fun domain(): GameSnapshot = GameSnapshot(
        rulesVersion, config.domain(), occupiedCells.map { it.domain() },
        hand.map { cells -> cells?.map { it.domain() } }, generatorState,
        score, timeouts, remainingMillis, endReason?.let { EndReason.valueOf(it) },
    )
    companion object {
        fun from(snapshot: GameSnapshot): StoredSession = StoredSession(
            snapshot.rulesVersion,
            StoredConfig.from(
                snapshot.config,
            ),
            snapshot.occupiedCells.map(StoredCell::from),
            snapshot.hand.map { cells -> cells?.map(StoredCell::from) }, snapshot.generatorState,
            snapshot.score, snapshot.timeouts, snapshot.remainingMillis, snapshot.endReason?.name,
        )
    }
}

@Serializable
private data class StoredStats(
    val config: StoredConfig,
    val started: Long,
    val finished: Long,
    val abandoned: Long,
    val bestScore: Long?,
    val moves: Long,
    val clearedLines: Long,
    val playMillis: Long,
) {
    fun domain(): ModeStats = ModeStats(
        config.domain(),
        started,
        finished,
        abandoned,
        bestScore,
        moves,
        clearedLines,
        playMillis,
    )
    companion object {
        fun from(stats: ModeStats): StoredStats = StoredStats(
            StoredConfig.from(stats.config),
            stats.started,
            stats.finished,
            stats.abandoned,
            stats.bestScore,
            stats.moves,
            stats.clearedLines,
            stats.playMillis,
        )
    }
}
