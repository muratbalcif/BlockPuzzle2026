package com.muratbalcif.blockpuzzle

import com.muratbalcif.blockpuzzle.engine.Cell
import com.muratbalcif.blockpuzzle.engine.Difficulty
import com.muratbalcif.blockpuzzle.engine.GameStatus
import com.muratbalcif.blockpuzzle.engine.Tempo
import com.muratbalcif.blockpuzzle.ui.GameController
import com.muratbalcif.blockpuzzle.ui.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GameControllerTest {
    private var now = 0L
    private fun controller() = GameController(clockMillis = { now }, nextSeed = { 30L })

    @Test
    fun setupChoicesReachEngine() {
        val controller = controller()
        controller.configure(16, Difficulty.HARD, Tempo.SLOW)
        controller.startGame()
        val state = controller.state
        assertEquals(Screen.GAME, state.screen)
        assertEquals(16, state.game!!.board.size)
        assertEquals(Difficulty.HARD, state.game.config.difficulty)
        assertEquals(90_000L, state.game.remainingMillis)
    }

    @Test
    fun acceptedTapConsumesPieceAndRejectedTapPreservesIt() {
        val controller = controller()
        controller.startGame()
        controller.selectPiece(0)
        val old = controller.state.game!!
        assertFalse(controller.placeAt(Cell(-1, 0)))
        assertSame(old, controller.state.game)
        assertEquals(0, controller.state.selectedSlot)
        assertTrue(controller.placeAt(Cell(0, 0)))
        assertNull(controller.state.game!!.hand[0])
        assertNull(controller.state.selectedSlot)
        assertTrue(controller.state.game!!.score > 0)
    }

    @Test
    fun timeoutIsAppliedBeforePlacementNotAvoidedByLateTap() {
        val controller = controller()
        controller.configure(8, Difficulty.MEDIUM, Tempo.FAST)
        controller.startGame()
        controller.selectPiece(0)
        now = 60_000
        assertFalse(controller.placeAt(Cell(0, 0)))
        assertEquals(GameStatus.FINISHED, controller.state.game!!.status)
        assertTrue(controller.state.game!!.board.occupiedCells.isEmpty())
        assertEquals(3, controller.state.game!!.timeouts)
    }

    @Test
    fun tickingIsIncrementalAndSuccessfulMoveResetsBaseline() {
        val controller = controller()
        controller.configure(8, Difficulty.MEDIUM, Tempo.FAST)
        controller.startGame()
        now = 5000
        controller.tick()
        assertEquals(15_000L, controller.state.game!!.remainingMillis)
        controller.selectPiece(0)
        assertTrue(controller.placeAt(Cell(0, 0)))
        now = 6000
        controller.tick()
        assertEquals(19_000L, controller.state.game!!.remainingMillis)
    }

    @Test
    fun backgroundFreezesTimerAndRequiresManualResume() {
        val controller = controller()
        controller.configure(8, Difficulty.MEDIUM, Tempo.FAST)
        controller.startGame()
        controller.selectPiece(0)
        controller.preview(Cell(0, 0))
        now = 5000
        controller.setForeground(false)
        assertEquals(15_000L, controller.state.game!!.remainingMillis)
        assertEquals(GameStatus.PAUSED, controller.state.game!!.status)
        assertNull(controller.state.selectedSlot)
        assertNull(controller.state.previewCell)
        now = 1_000_000
        controller.tick()
        controller.setForeground(true)
        controller.tick()
        assertEquals(15_000L, controller.state.game!!.remainingMillis)
        assertEquals(GameStatus.PAUSED, controller.state.game!!.status)
        controller.resume()
        now += 1000
        controller.tick()
        assertEquals(14_000L, controller.state.game!!.remainingMillis)
    }

    @Test
    fun pauseConsumesPendingTimeAndCancelsGesture() {
        val controller = controller()
        controller.configure(8, Difficulty.MEDIUM, Tempo.FAST)
        controller.startGame()
        controller.selectPiece(0)
        controller.preview(Cell(0, 0))
        now = 2000
        controller.pause()
        assertEquals(18_000L, controller.state.game!!.remainingMillis)
        assertNull(controller.state.previewCell)
        assertFalse(controller.placeAt(Cell(0, 0)))
        now += 300_000
        controller.resume()
        controller.tick()
        assertEquals(18_000L, controller.state.game!!.remainingMillis)
    }

    @Test
    fun cancelledOrOutsideDragDoesNotPlaceAnything() {
        val controller = controller()
        controller.startGame()
        controller.selectPiece(0)
        val original = controller.state.game
        controller.preview(Cell(0, 0))
        controller.clearPreview()
        assertFalse(controller.commitPreview())
        assertSame(original, controller.state.game)
        controller.preview(Cell(-1, 0))
        assertFalse(controller.commitPreview())
        assertSame(original, controller.state.game)
    }

    @Test
    fun previewAndCommitSharePlacementRules() {
        val controller = controller()
        controller.startGame()
        controller.selectPiece(0)
        controller.preview(Cell(0, 0))
        assertTrue(controller.state.previewValid)
        assertTrue(controller.commitPreview())
        assertNull(controller.state.previewCell)
        assertNull(controller.state.selectedSlot)
    }

    @Test
    fun cancelExitRestoresActiveButNotManuallyPausedGames() {
        val active = controller()
        active.startGame()
        active.requestExit()
        assertTrue(active.state.exitRequested)
        assertEquals(GameStatus.PAUSED, active.state.game!!.status)
        active.cancelExit()
        assertEquals(GameStatus.ACTIVE, active.state.game!!.status)
        active.pause()
        active.requestExit()
        active.cancelExit()
        assertEquals(GameStatus.PAUSED, active.state.game!!.status)
    }

    @Test
    fun backgroundDuringExitCannotAutomaticallyResumeOnCancel() {
        val controller = controller()
        controller.startGame()
        controller.requestExit()
        controller.setForeground(false)
        controller.setForeground(true)
        controller.cancelExit()
        assertEquals(GameStatus.PAUSED, controller.state.game!!.status)
    }

    @Test
    fun exitAndReplayKeepSettingsButStartFreshGame() {
        val controller = controller()
        controller.configure(16, Difficulty.HARD, Tempo.MEDIUM)
        controller.startGame()
        controller.selectPiece(0)
        controller.placeAt(Cell(0, 0))
        controller.startGame()
        controller.confirmNewGame()
        assertEquals(0L, controller.state.game!!.score)
        controller.requestExit()
        controller.confirmExit()
        assertEquals(Screen.HOME, controller.state.screen)
        assertNull(controller.state.game)
        assertEquals(16, controller.state.config.boardSize)
    }

    @Test
    fun homeCannotRunGameTimerOrAcceptMoves() {
        val controller = controller()
        now = Long.MAX_VALUE
        controller.tick()
        assertFalse(controller.placeAt(Cell(0, 0)))
        assertEquals(Screen.HOME, controller.state.screen)
    }
}
