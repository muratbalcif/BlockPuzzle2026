package com.muratbalcif.blockpuzzle

import com.muratbalcif.blockpuzzle.engine.Board
import com.muratbalcif.blockpuzzle.engine.Cell
import com.muratbalcif.blockpuzzle.engine.Difficulty
import com.muratbalcif.blockpuzzle.engine.GameConfig
import com.muratbalcif.blockpuzzle.engine.GameSession
import com.muratbalcif.blockpuzzle.engine.GameStatus
import com.muratbalcif.blockpuzzle.engine.Tempo
import com.muratbalcif.blockpuzzle.persistence.LoadProblem
import com.muratbalcif.blockpuzzle.persistence.LoadedProgress
import com.muratbalcif.blockpuzzle.persistence.LocalProgress
import com.muratbalcif.blockpuzzle.persistence.ModeStats
import com.muratbalcif.blockpuzzle.persistence.ProgressCodec
import com.muratbalcif.blockpuzzle.ui.GameController
import com.muratbalcif.blockpuzzle.ui.SaveStatus
import com.muratbalcif.blockpuzzle.ui.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressControllerTest {
    private var now = 0L
    private val saved = mutableListOf<LocalProgress>()
    private fun controller(initial: LocalProgress = LocalProgress()) = GameController(
        clockMillis = { now },
        nextSeed = { 30L },
        initialProgress = initial,
        onSave = saved::add,
    )
    private fun roundTrip(data: LocalProgress): LocalProgress =
        ProgressCodec.decode(ProgressCodec.encode(data)).data

    @Test
    fun freshControllerRestoresExactProgressPausedAndIgnoresOfflineTime() {
        val first = controller()
        first.configure(8, Difficulty.MEDIUM, Tempo.FAST)
        first.startGame()
        now = 1234
        first.selectPiece(0)
        first.placeAt(Cell(0, 0))
        first.pause()
        val document = roundTrip(saved.last())
        now = 900_000
        val restored = controller(document)
        assertEquals(Screen.HOME, restored.state.screen)
        assertNotNull(restored.state.savedGame)
        restored.continueGame()
        assertEquals(GameStatus.PAUSED, restored.state.game!!.status)
        assertNull(restored.state.game!!.hand[0])
        assertEquals(1L, restored.state.game!!.score)
        // Successful placement reset the timer before pause.
        assertEquals(20_000L, restored.state.game!!.remainingMillis)
        restored.tick()
        assertEquals(1234L, restored.progress.statistics.single().playMillis)
        restored.resume()
        now += 1000
        restored.tick()
        assertEquals(19_000L, restored.state.game!!.remainingMillis)
        assertEquals(2234L, restored.progress.statistics.single().playMillis)
    }

    @Test
    fun relaxedPlayTimeCountsForegroundOnly() {
        val controller = controller()
        controller.startGame()
        now = 10_000
        controller.tick()
        controller.setForeground(false)
        now = 800_000
        controller.tick()
        controller.setForeground(true)
        controller.resume()
        now += 1000
        controller.tick()
        assertEquals(11_000L, controller.progress.statistics.single().playMillis)
    }

    @Test
    fun terminalResultAndResumeRemovalPersistTogetherOnlyOnce() {
        val controller = controller()
        controller.configure(8, Difficulty.MEDIUM, Tempo.FAST)
        controller.startGame()
        controller.selectPiece(0)
        controller.placeAt(Cell(0, 0))
        now = 90_000
        controller.tick()
        val result = roundTrip(saved.last())
        assertNull(result.resume)
        val stats = result.statistics.single()
        assertEquals(1L, stats.started)
        assertEquals(1L, stats.finished)
        assertEquals(0L, stats.bestScore)
        assertEquals(1L, stats.moves)
        assertEquals(60_000L, stats.playMillis)
        controller.tick()
        controller.pause()
        controller.confirmExit()
        val restored = controller(roundTrip(saved.last()))
        assertNull(restored.state.savedGame)
        assertEquals(1L, restored.progress.statistics.single().finished)
    }

    @Test
    fun recordsUseFinalScoresAndRemainSeparateByMode() {
        val config = GameConfig()
        val occupied = (0..7).flatMap { row -> (0..7).map { Cell(row, it) } }.filter {
            (
                it.row +
                    it.column
                ) %
                2 ==
                1
        }
        val initial =
            LocalProgress(
                resume = GameSession.start(config, 30, Board.fromCells(8, occupied)).snapshot(),
                statistics = listOf(ModeStats(config, started = 1)),
            )
        val controller = controller(initial)
        controller.continueGame()
        controller.resume()
        controller.selectPiece(0)
        controller.placeAt(Cell(0, 0))
        assertEquals(1L, controller.progress.statistics.single().bestScore)
        controller.confirmExit()
        controller.configure(16, Difficulty.HARD, Tempo.FAST)
        controller.startGame()
        now = 60_000
        controller.tick()
        val bySize = roundTrip(saved.last()).statistics.associateBy { it.config.boardSize }
        assertEquals(1L, bySize.getValue(8).bestScore)
        assertEquals(0L, bySize.getValue(16).bestScore)
    }

    @Test
    fun homeExitSavesAndReplacementRequiresConfirmation() {
        val controller = controller()
        controller.startGame()
        controller.requestExit()
        controller.confirmExit()
        assertNotNull(controller.state.savedGame)
        assertEquals(0L, controller.progress.statistics.single().abandoned)
        controller.configure(16, Difficulty.HARD, Tempo.SLOW)
        controller.startGame()
        assertTrue(controller.state.newGameRequested)
        assertEquals(8, controller.state.savedGame!!.config.boardSize)
        controller.cancelNewGame()
        assertEquals(0L, controller.progress.statistics.single().abandoned)
        controller.startGame()
        controller.confirmNewGame()
        assertEquals(
            1L,
            controller.progress.statistics.single {
                it.config.boardSize == 8
            }.abandoned,
        )
        assertEquals(
            1L,
            controller.progress.statistics.single {
                it.config.boardSize == 16
            }.started,
        )
        assertEquals(16, roundTrip(saved.last()).resume!!.config.boardSize)
    }

    @Test
    fun settingsAndEffectsSurviveFreshController() {
        val controller = controller()
        controller.configure(16, Difficulty.HARD, Tempo.MEDIUM)
        controller.effects(false, false)
        val restored = controller(roundTrip(saved.last()))
        assertEquals(GameConfig(16, Difficulty.HARD, Tempo.MEDIUM), restored.state.config)
        assertFalse(restored.state.animationsEnabled)
        assertFalse(restored.state.hapticsEnabled)
    }

    @Test
    fun staleSuccessCannotHideNewestWriteFailureAndRetryDoesNotCountAgain() {
        val controller = controller()
        controller.startGame()
        val old = saved.last()
        controller.selectPiece(0)
        controller.placeAt(Cell(0, 0))
        val newest = saved.last()
        controller.saveFailed(newest)
        assertEquals(SaveStatus.ERROR, controller.state.saveStatus)
        controller.saved(old)
        assertEquals(SaveStatus.ERROR, controller.state.saveStatus)
        controller.retrySave()
        assertSame(newest, saved.last())
        controller.saved(newest)
        assertEquals(SaveStatus.SAVED, controller.state.saveStatus)
        assertEquals(1L, controller.progress.statistics.single().started)
        assertEquals(1L, controller.progress.statistics.single().moves)
    }

    @Test
    fun timerCheckpointsAreThrottledButPauseFlushesRemainder() {
        val controller = controller()
        controller.startGame()
        repeat(9) {
            now += 100
            controller.tick()
        }
        assertEquals(1, saved.size)
        now = 1000
        controller.tick()
        assertEquals(2, saved.size)
        now = 1100
        controller.pause()
        assertEquals(3, saved.size)
        assertEquals(1100L, roundTrip(saved.last()).statistics.single().playMillis)
    }

    @Test
    fun invalidLoadIsNotOverwrittenUntilExplicitNewGameConfirmation() {
        val controller = controller()
        controller.loadProgress(LoadedProgress(LocalProgress(), LoadProblem.UNSUPPORTED))
        controller.configure(16, Difficulty.HARD, Tempo.FAST)
        assertTrue(saved.isEmpty())
        controller.startGame()
        assertTrue(controller.state.newGameRequested)
        assertTrue(saved.isEmpty())
        controller.cancelNewGame()
        assertNotNull(controller.state.loadProblem)
        controller.startGame()
        controller.confirmNewGame()
        assertNull(controller.state.loadProblem)
        assertEquals(1L, roundTrip(saved.last()).statistics.single().started)
    }

    @Test
    fun slowOlderWriteFailureRemainsVisibleWhileNewerCheckpointIsPending() {
        val controller = controller()
        controller.startGame()
        val older = saved.last()
        now = 1000
        controller.tick()
        val newest = saved.last()
        controller.saveFailed(older)
        assertTrue(controller.state.saveProblem)
        controller.saved(older)
        assertTrue(controller.state.saveProblem)
        controller.saved(newest)
        assertFalse(controller.state.saveProblem)
        // A hypothetical delayed old callback cannot undo acknowledged latest success.
        controller.saveFailed(older)
        assertFalse(controller.state.saveProblem)
    }
}
