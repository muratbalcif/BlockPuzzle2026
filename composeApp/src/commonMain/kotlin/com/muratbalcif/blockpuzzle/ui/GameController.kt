package com.muratbalcif.blockpuzzle.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.muratbalcif.blockpuzzle.engine.Cell
import com.muratbalcif.blockpuzzle.engine.Difficulty
import com.muratbalcif.blockpuzzle.engine.GameConfig
import com.muratbalcif.blockpuzzle.engine.GameSession
import com.muratbalcif.blockpuzzle.engine.GameSnapshot
import com.muratbalcif.blockpuzzle.engine.GameStatus
import com.muratbalcif.blockpuzzle.engine.MoveResult
import com.muratbalcif.blockpuzzle.engine.Tempo
import com.muratbalcif.blockpuzzle.persistence.LoadProblem
import com.muratbalcif.blockpuzzle.persistence.LoadedProgress
import com.muratbalcif.blockpuzzle.persistence.LocalProgress
import com.muratbalcif.blockpuzzle.persistence.ModeStats
import com.muratbalcif.blockpuzzle.persistence.PlayerPreferences
import kotlin.random.Random
import kotlin.time.TimeSource

enum class Screen { HOME, GAME }
enum class SaveStatus { IDLE, SAVING, SAVED, ERROR }
data class PlacementFeedback(val id: Long, val points: Long, val clearedCells: Set<Cell>)
data class PlayState(
    val config: GameConfig = GameConfig(difficulty = Difficulty.EASY),
    val screen: Screen = Screen.HOME,
    val game: GameSession? = null,
    val selectedSlot: Int? = null,
    val previewCell: Cell? = null,
    val previewValid: Boolean = false,
    val exitRequested: Boolean = false,
    val message: String = "",
    val feedback: PlacementFeedback? = null,
    val animationsEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val statistics: List<ModeStats> = emptyList(),
    val savedGame: GameSnapshot? = null,
    val saveStatus: SaveStatus = SaveStatus.IDLE,
    val saveProblem: Boolean = false,
    val loadProblem: LoadProblem? = null,
    val loading: Boolean = false,
    val newGameRequested: Boolean = false,
    val showStatistics: Boolean = false,
    val newRecord: Boolean = false,
)

/** UI transitions and checkpoints; F1 remains the sole authority for play rules. */
class GameController(
    private val clockMillis: () -> Long = monotonicMillis(),
    private val nextSeed: () -> Long = { Random.nextLong() },
    initialProgress: LocalProgress = LocalProgress(),
    private val onSave: ((LocalProgress) -> Unit)? = null,
    private val onReload: (() -> Unit)? = null,
) {
    var progress: LocalProgress = initialProgress
        private set
    var state by mutableStateOf(
        PlayState(
            config = initialProgress.preferences.config,
            animationsEnabled = initialProgress.preferences.animations,
            hapticsEnabled = initialProgress.preferences.haptics,
            statistics = initialProgress.statistics,
            savedGame = initialProgress.resume,
        ),
    )
        private set
    private var lastTick: Long? = null
    private var lastCheckpoint = clockMillis()
    private var pendingPlayMillis = 0L
    private var foreground = true
    private var exitWasActive = false
    private var newGameWasActive = false
    private var completionRecorded = false
    private var feedbackId = 0L

    fun beginLoading() {
        state = state.copy(loading = true)
    }

    fun loadProgress(loaded: LoadedProgress) {
        progress = loaded.data
        val preferences = progress.preferences
        state = PlayState(
            config = preferences.config,
            animationsEnabled = preferences.animations,
            hapticsEnabled = preferences.haptics,
            statistics = progress.statistics,
            savedGame = progress.resume,
            loadProblem = loaded.problem,
            saveStatus = if (loaded.problem == null &&
                onSave != null
            ) {
                SaveStatus.SAVED
            } else {
                SaveStatus.IDLE
            },
        )
        pendingPlayMillis = 0
        lastTick = null
        completionRecorded = false
    }

    fun configure(size: Int, difficulty: Difficulty, tempo: Tempo) {
        if (state.screen == Screen.HOME && !state.loading && state.loadProblem == null) {
            state = state.copy(config = GameConfig(size, difficulty, tempo))
            checkpoint()
        }
    }

    fun effects(animations: Boolean, haptics: Boolean) {
        if (state.loading || state.loadProblem != null) return
        state = state.copy(animationsEnabled = animations, hapticsEnabled = haptics)
        checkpoint()
    }

    fun startGame() {
        if (state.loading) return
        tick()
        if (state.savedGame != null || state.loadProblem != null) {
            newGameWasActive = state.game?.status == GameStatus.ACTIVE
            if (state.game != null) pause()
            state = state.copy(newGameRequested = true)
            return
        }
        startFresh()
    }

    fun confirmNewGame() {
        if (!state.newGameRequested) return
        if (state.loadProblem != null) {
            progress = LocalProgress()
            state = state.copy(loadProblem = null, statistics = emptyList(), savedGame = null)
        }
        state.savedGame?.let { snapshot ->
            updateStats(snapshot.config) { it.copy(abandoned = it.abandoned + 1) }
        }
        startFresh()
    }

    fun cancelNewGame() {
        state = state.copy(newGameRequested = false)
        if (newGameWasActive && foreground) resume()
        newGameWasActive = false
    }

    private fun startFresh() {
        val game = GameSession.start(state.config, nextSeed()).let {
            if (foreground) it else it.pause()
        }
        updateStats(game.config) { it.copy(started = it.started + 1) }
        completionRecorded = false
        pendingPlayMillis = 0
        state = state.copy(
            screen = Screen.GAME, game = game, selectedSlot = null, previewCell = null,
            previewValid = false, exitRequested = false, feedback = null,
            newGameRequested = false, newRecord = false,
            message = "Parça seçip hücreye dokun veya sürükle.",
        )
        exitWasActive = false
        lastTick = if (game.status == GameStatus.ACTIVE) clockMillis() else null
        checkpoint()
    }

    fun continueGame() {
        if (state.loading || state.loadProblem != null) return
        val snapshot = state.savedGame ?: return
        val game = try {
            GameSession.restore(snapshot)
        } catch (_: IllegalArgumentException) {
            state = state.copy(loadProblem = LoadProblem.INVALID)
            return
        }
        state = state.copy(
            config = game.config, screen = Screen.GAME, game = game, selectedSlot = null,
            previewCell = null, previewValid = false, exitRequested = false, feedback = null,
            newGameRequested = false, newRecord = false, message = "Kayıtlı oyunun hazır.",
        )
        completionRecorded = false
        pendingPlayMillis = 0
        lastTick = null
        checkpoint()
    }

    fun selectPiece(slot: Int) {
        tick()
        val game = state.game ?: return
        if (game.status != GameStatus.ACTIVE ||
            state.exitRequested ||
            state.newGameRequested
        ) {
            return
        }
        if (slot !in game.hand.indices || game.hand[slot] == null) return
        state = state.copy(
            selectedSlot = slot,
            previewCell = null,
            previewValid = false,
            message = "Parça ${slot + 1} seçildi. Bir hücreye dokun.",
        )
    }

    fun placeAt(cell: Cell): Boolean {
        tick()
        val game = state.game ?: return false
        if (game.status != GameStatus.ACTIVE ||
            state.exitRequested ||
            state.newGameRequested
        ) {
            return false
        }
        val slot = state.selectedSlot ?: run {
            state = state.copy(message = "Önce alttan bir parça seç.")
            return false
        }
        val piece = game.hand[slot] ?: return false
        when (val move = game.place(slot, cell)) {
            is MoveResult.Rejected -> {
                state = state.copy(message = "Buraya sığmıyor. Başka bir hücre dene.")
                return false
            }
            is MoveResult.Placed -> {
                val rows = move.placement.clearedRows
                val columns = move.placement.clearedColumns
                val cleared = (
                    game.board.occupiedCells + piece.cells.map {
                        Cell(cell.row + it.row, cell.column + it.column)
                    }
                    ).filter { it.row in rows || it.column in columns }.toSet()
                updateStats(game.config) {
                    it.copy(
                        moves = it.moves + 1,
                        clearedLines =
                        it.clearedLines + rows.size + columns.size,
                    )
                }
                state = state.copy(
                    game = move.game,
                    selectedSlot = null,
                    previewCell = null,
                    previewValid = false,
                    message = if (cleared.isEmpty()) {
                        "Parça yerleştirildi. +${move.placement.points} puan."
                    } else {
                        "${rows.size + columns.size} çizgi temizlendi. " +
                            "+${move.placement.points} puan!"
                    },
                    feedback = PlacementFeedback(++feedbackId, move.placement.points, cleared),
                )
                checkpoint()
                return true
            }
        }
    }

    fun preview(cell: Cell) {
        val game = state.game ?: return
        val slot = state.selectedSlot ?: return
        val piece = game.hand[slot] ?: return
        if (game.status == GameStatus.ACTIVE && !state.exitRequested && !state.newGameRequested) {
            state = state.copy(previewCell = cell, previewValid = game.board.canPlace(piece, cell))
        }
    }
    fun clearPreview() {
        state = state.copy(previewCell = null, previewValid = false)
    }
    fun commitPreview(): Boolean {
        val cell = state.previewCell
        val accepted = if (cell == null) false else placeAt(cell)
        clearPreview()
        return accepted
    }
    fun dismissFeedback(id: Long) {
        if (state.feedback?.id ==
            id
        ) {
            state = state.copy(feedback = null)
        }
    }

    fun tick() {
        val game = state.game ?: return
        if (!foreground ||
            game.status != GameStatus.ACTIVE ||
            state.exitRequested ||
            state.newGameRequested
        ) {
            return
        }
        val now = clockMillis()
        val previous = lastTick ?: now
        lastTick = now
        val elapsed = if (now >= previous) (now - previous).coerceAtLeast(0) else 0L
        val updated = game.advanceTime(elapsed)
        val ended = updated.status == GameStatus.FINISHED
        val limit = game.config.tempo.moveLimitMillis
        val played = if (ended &&
            limit != null
        ) {
            minOf(
                elapsed,
                checkNotNull(game.remainingMillis) + (2 - game.timeouts) * limit,
            )
        } else {
            elapsed
        }
        pendingPlayMillis = cappedAdd(pendingPlayMillis, played)
        if (updated !== game) {
            state = state.copy(
                game = updated,
                selectedSlot = if (ended) null else state.selectedSlot,
                previewCell = if (ended) null else state.previewCell,
                previewValid = if (ended) false else state.previewValid,
                message = if (updated.timeouts >
                    game.timeouts
                ) {
                    "Süre doldu. 10 puan ceza. Kalan hak: ${3 - updated.timeouts}."
                } else {
                    state.message
                },
            )
        }
        if (ended || updated.timeouts != game.timeouts || now - lastCheckpoint >= 1000) checkpoint()
    }

    fun pause() {
        tick()
        val game = state.game ?: return
        state =
            state.copy(
                game = game.pause(),
                selectedSlot = null,
                previewCell = null,
                previewValid = false,
                feedback = null,
            )
        lastTick = null
        checkpoint()
    }
    fun resume() {
        val game = state.game ?: return
        if (!foreground ||
            state.exitRequested ||
            state.newGameRequested ||
            game.status != GameStatus.PAUSED
        ) {
            return
        }
        state = state.copy(game = game.resume())
        lastTick = clockMillis()
        lastCheckpoint = clockMillis()
    }
    fun setForeground(value: Boolean) {
        if (foreground == value) return
        if (!value) {
            pause()
            exitWasActive = false
            newGameWasActive = false
        }
        foreground = value
        lastTick = null
    }
    fun requestExit() {
        tick()
        val game = state.game ?: return
        exitWasActive = game.status == GameStatus.ACTIVE
        pause()
        state = state.copy(exitRequested = true)
    }
    fun cancelExit() {
        if (!state.exitRequested) return
        val game = state.game ?: return
        val restored = if (exitWasActive && foreground) game.resume() else game
        state = state.copy(game = restored, exitRequested = false)
        lastTick = if (restored.status == GameStatus.ACTIVE) clockMillis() else null
    }
    fun confirmExit() {
        if (state.game != null) checkpoint()
        state = state.copy(
            screen = Screen.HOME, game = null, selectedSlot = null,
            previewCell = null,
            previewValid = false,
            exitRequested = false,
            feedback = null,
            message = "",
            newRecord = false,
        )
        lastTick = null
    }
    fun showStatistics(value: Boolean) {
        state = state.copy(showStatistics = value)
    }
    fun saved(document: LocalProgress) {
        if (document ===
            progress
        ) {
            state = state.copy(saveStatus = SaveStatus.SAVED, saveProblem = false)
        }
    }
    fun saveFailed(document: LocalProgress) {
        if (document === progress ||
            state.saveStatus != SaveStatus.SAVED
        ) {
            state =
                state.copy(saveStatus = SaveStatus.ERROR, saveProblem = true)
        }
    }
    fun retrySave() {
        if (state.loading) return
        if (state.loadProblem != null) {
            onReload?.invoke()
            return
        }
        val sink = onSave ?: return
        state = state.copy(saveStatus = SaveStatus.SAVING)
        sink(progress)
    }
    private fun updateStats(config: GameConfig, transform: (ModeStats) -> ModeStats) {
        val existing = progress.statistics.firstOrNull { it.config == config }
        val updated = transform(existing ?: ModeStats(config))
        progress =
            progress.copy(
                statistics = if (existing ==
                    null
                ) {
                    progress.statistics + updated
                } else {
                    progress.statistics.map {
                        if (it.config ==
                            config
                        ) {
                            updated
                        } else {
                            it
                        }
                    }
                },
            )
    }
    private fun checkpoint() {
        if (state.loading || state.loadProblem != null) return
        val game = state.game
        if (game != null && pendingPlayMillis > 0) {
            updateStats(game.config) {
                it.copy(playMillis = cappedAdd(it.playMillis, pendingPlayMillis))
            }
            pendingPlayMillis = 0
        }
        if (game?.status == GameStatus.FINISHED && !completionRecorded) {
            var record = false
            updateStats(game.config) {
                record = game.score > 0 && (it.bestScore == null || game.score > it.bestScore)
                it.copy(
                    finished = it.finished + 1,
                    bestScore = maxOf(it.bestScore ?: 0, game.score),
                )
            }
            state = state.copy(newRecord = record)
            completionRecorded = true
        }
        val resume = if (game ==
            null
        ) {
            state.savedGame
        } else if (game.status ==
            GameStatus.FINISHED
        ) {
            null
        } else {
            game.snapshot()
        }
        progress = progress.copy(
            preferences = PlayerPreferences(
                state.config,
                state.animationsEnabled,
                state.hapticsEnabled,
            ),
            resume = resume,
        )
        state = state.copy(
            statistics = progress.statistics,
            savedGame = resume,
            saveStatus = if (onSave == null) SaveStatus.IDLE else SaveStatus.SAVING,
        )
        lastCheckpoint = clockMillis()
        onSave?.invoke(progress)
    }
}

private fun cappedAdd(a: Long, b: Long): Long = if (a >
    Long.MAX_VALUE - b
) {
    Long.MAX_VALUE
} else {
    a + b
}
private fun monotonicMillis(): () -> Long {
    val origin = TimeSource.Monotonic.markNow()
    return { origin.elapsedNow().inWholeMilliseconds }
}
