package com.muratbalcif.blockpuzzle

import androidx.activity.compose.setContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.Lifecycle
import com.muratbalcif.blockpuzzle.engine.Difficulty
import com.muratbalcif.blockpuzzle.engine.GameStatus
import com.muratbalcif.blockpuzzle.engine.Tempo
import com.muratbalcif.blockpuzzle.ui.GameController
import com.muratbalcif.blockpuzzle.ui.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlayUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun setup(clock: () -> Long = { 0L }): GameController {
        val controller = GameController(clockMillis = clock, nextSeed = { 30L })
        controller.configure(8, Difficulty.MEDIUM, Tempo.RELAXED)
        rule.runOnUiThread { rule.activity.setContent { App(controller) } }
        rule.waitForIdle()
        return controller
    }

    private fun start() {
        rule.waitUntil(5000) {
            rule.onAllNodes(
                androidx.compose.ui.test.hasTestTag("start-game") and
                    androidx.compose.ui.test.isEnabled(),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("start-game").performScrollTo().performClick()
        rule.onNodeWithTag("board-grid").assertIsDisplayed()
    }

    @Test
    fun setupStartsSelectedBoardDifficultyAndTempo() {
        val controller = setup()
        rule.onNodeWithTag("size-16").performScrollTo().performClick()
        rule.onNodeWithTag("difficulty-HARD").performScrollTo().performClick()
        rule.onNodeWithTag("tempo-FAST").performScrollTo().performClick()
        start()
        assertEquals(16, controller.state.game!!.board.size)
        assertEquals(Difficulty.HARD, controller.state.game!!.config.difficulty)
        rule.onNodeWithTag("remaining-time").assertIsDisplayed()
    }

    @Test
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun selectingPieceThenTappingBoardPlacesIt() {
        val controller = setup()
        captureScreen("home")
        start()
        rule.onNodeWithTag("piece-0").performClick()
        rule.onNodeWithTag("cell-0-0").performTouchInput { click() }
        assertEquals(1L, controller.state.game!!.score)
        assertNull(controller.state.game!!.hand[0])
        rule.onNodeWithText("Parça yerleştirildi. +1 puan.").assertIsDisplayed()
        captureScreen("game")
    }

    @Test
    fun edgeCollisionExplainsFailureWithoutConsumingPiece() {
        val controller = setup()
        start()
        rule.onNodeWithTag("piece-1").performClick()
        rule.onNodeWithTag("cell-7-7").performClick()
        assertEquals(0L, controller.state.game!!.score)
        assertNotNull(controller.state.game!!.hand[1])
        rule.onNodeWithText("Buraya sığmıyor. Başka bir hücre dene.").assertIsDisplayed()
    }

    @Test
    fun pointerDragFromTrayToBoardPlacesPiece() {
        val controller = setup()
        start()
        val target = rule.onNodeWithTag("cell-0-0").fetchSemanticsNode().boundsInRoot.center
        val piece = rule.onNodeWithTag("piece-0")
        val pieceOrigin = piece.fetchSemanticsNode().boundsInRoot.topLeft
        piece.performTouchInput {
            down(center)
            moveBy(Offset(0f, -30f))
            moveTo(target - pieceOrigin, delayMillis = 500)
            up()
        }
        rule.waitForIdle()
        assertEquals(1L, controller.state.game!!.score)
        assertNull(controller.state.game!!.hand[0])
    }

    @Test
    fun outsideDropPreservesHandAndBoard() {
        val controller = setup()
        start()
        rule.onNodeWithTag("piece-0").performTouchInput {
            down(center)
            moveBy(Offset(0f, 40f))
            up()
        }
        rule.waitForIdle()
        assertEquals(0L, controller.state.game!!.score)
        assertNotNull(controller.state.game!!.hand[0])
        assertNull(controller.state.previewCell)
    }

    @Test
    fun pauseResumeAndExitConfirmationWork() {
        val controller = setup()
        start()
        rule.onNodeWithText("Duraklat").performClick()
        rule.onNodeWithText("Oyun duraklatıldı").assertIsDisplayed()
        rule.onNodeWithText("Devam et").performClick()
        assertEquals(GameStatus.ACTIVE, controller.state.game!!.status)
        rule.onNodeWithText("Ana ekran").performClick()
        rule.onNodeWithText("Oyuna dön").performClick()
        assertEquals(GameStatus.ACTIVE, controller.state.game!!.status)
        rule.onNodeWithText("Ana ekran").performClick()
        rule.onNodeWithText("Kaydet ve dön").performClick()
        assertEquals(Screen.HOME, controller.state.screen)
        rule.onNodeWithTag("start-game").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun realLifecyclePauseSurvivesForegroundReturn() {
        val controller = setup()
        start()
        rule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        rule.waitForIdle()
        assertEquals(GameStatus.PAUSED, controller.state.game!!.status)
        rule.onNodeWithText("Devam et").assertIsDisplayed()
    }

    @Test
    fun enlargedBoardCanPanToLastCell() {
        val controller = setup()
        rule.onNodeWithTag("size-16").performScrollTo().performClick()
        start()
        rule.onNodeWithText("Büyüt").performClick()
        rule.onNodeWithTag("piece-0").performScrollTo().performClick()
        assertEquals(0, controller.state.selectedSlot)
        rule.onNodeWithTag(
            "board-horizontal-scroll",
        ).performSemanticsAction(SemanticsActions.ScrollBy) {
            it(10_000f, 0f)
        }
        rule.onNodeWithTag(
            "board-vertical-scroll",
        ).performSemanticsAction(SemanticsActions.ScrollBy) {
            it(0f, 10_000f)
        }
        rule.onNodeWithTag("cell-15-15").assertIsDisplayed().performClick()
        assertEquals(
            "${controller.state.message}; selected=${controller.state.selectedSlot}; " +
                "piece=${controller.state.game!!.hand[0]}",
            1L,
            controller.state.game!!.score,
        )
    }

    @Test
    fun activityRecreationKeepsGameThroughViewModel() {
        // Use the real App/ViewModel created by MainActivity, not an injected controller.
        start()
        rule.onNodeWithTag("piece-0").performClick()
        rule.onNodeWithTag("cell-0-0").performClick()
        rule.activityRule.scenario.recreate()
        rule.onNodeWithText("Oyun duraklatıldı").assertIsDisplayed()
        rule.onNodeWithText("Devam et").performClick()
        rule.onNodeWithTag("board-grid").assertIsDisplayed()
        rule.onNodeWithTag("piece-0").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w800dp-h360dp-land")
    fun landscapeKeepsBoardAndHandVisible() {
        setup()
        start()
        rule.onNodeWithTag("piece-0").assertIsDisplayed()
        rule.onNodeWithTag("cell-0-0").assertIsDisplayed()
    }

    @Test
    fun timedPenaltyContinuesThenShowsGameOverAndReplay() {
        var now = 0L
        val controller = setup { now }
        rule.onNodeWithTag("tempo-FAST").performScrollTo().performClick()
        start()
        rule.runOnIdle {
            now = 20_000
            controller.tick()
        }
        rule.onNodeWithText("2 / 3").assertIsDisplayed()
        assertEquals(GameStatus.ACTIVE, controller.state.game!!.status)
        rule.runOnIdle {
            now = 60_000
            controller.tick()
        }
        rule.onNodeWithText("Oyun bitti").assertIsDisplayed()
        rule.onNodeWithText("Üç süre hakkı da doldu.").assertIsDisplayed()
        rule.onNodeWithText("Tekrar oyna").performClick()
        assertEquals(0, controller.state.game!!.timeouts)
        assertEquals(GameStatus.ACTIVE, controller.state.game!!.status)
    }

    @Test
    fun backRequestsConfirmationInsteadOfDiscardingGame() {
        val controller = setup()
        start()
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithText("Ana ekrana dönülsün mü?").assertIsDisplayed()
        rule.onNodeWithText("Oyuna dön").performClick()
        assertEquals(Screen.GAME, controller.state.screen)
        assertEquals(GameStatus.ACTIVE, controller.state.game!!.status)
    }

    @Test
    fun doubleFontScaleStillAllowsSetupAndAccessiblePlacement() {
        val controller = GameController(clockMillis = { 0L }, nextSeed = { 30L })
        controller.configure(8, Difficulty.MEDIUM, Tempo.RELAXED)
        val density = rule.activity.resources.displayMetrics.density
        rule.runOnUiThread {
            rule.activity.setContent {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides
                        androidx.compose.ui.unit.Density(density, 2f),
                ) { App(controller) }
            }
        }
        start()
        rule.onNodeWithTag("piece-0").performScrollTo().performClick()
        rule.onNodeWithTag("board-viewport").performScrollTo()
        rule.onNodeWithTag("cell-0-0").assertIsDisplayed().performClick()
        assertEquals(1L, controller.state.game!!.score)
    }

    @Test
    fun draggingAfterEnlargedBoardPanUsesScrolledCoordinates() {
        val controller = setup()
        rule.onNodeWithTag("size-16").performScrollTo().performClick()
        start()
        rule.onNodeWithText("Büyüt").performClick()
        rule.onNodeWithTag("piece-0").performScrollTo()
        rule.onNodeWithTag(
            "board-horizontal-scroll",
        ).performSemanticsAction(SemanticsActions.ScrollBy) {
            it(10_000f, 0f)
        }
        rule.onNodeWithTag(
            "board-vertical-scroll",
        ).performSemanticsAction(SemanticsActions.ScrollBy) {
            it(0f, 10_000f)
        }
        val target = rule.onNodeWithTag("cell-15-15").fetchSemanticsNode().boundsInRoot.center
        val piece = rule.onNodeWithTag("piece-0")
        val origin = piece.fetchSemanticsNode().boundsInRoot.topLeft
        piece.performTouchInput {
            down(center)
            moveBy(Offset(0f, -30f))
            moveTo(target - origin, delayMillis = 500)
            up()
        }
        rule.waitForIdle()
        assertEquals(1L, controller.state.game!!.score)
    }

    @Test
    fun clippedPartOfEnlargedBoardCannotReceiveDrop() {
        val controller = setup()
        rule.onNodeWithTag("size-16").performScrollTo().performClick()
        start()
        rule.onNodeWithText("Büyüt").performClick()
        rule.onNodeWithTag("piece-0").performScrollTo()
        rule.onNodeWithTag(
            "board-horizontal-scroll",
        ).performSemanticsAction(SemanticsActions.ScrollBy) {
            it(10_000f, 0f)
        }
        rule.onNodeWithTag(
            "board-vertical-scroll",
        ).performSemanticsAction(SemanticsActions.ScrollBy) {
            it(0f, 10_000f)
        }
        val target =
            rule.onNodeWithTag("board-viewport").fetchSemanticsNode().boundsInRoot.topLeft +
                Offset(10f, -10f)
        val piece = rule.onNodeWithTag("piece-0")
        val origin = piece.fetchSemanticsNode().boundsInRoot.topLeft
        piece.performTouchInput {
            down(center)
            moveBy(Offset(0f, -30f))
            moveTo(target - origin, delayMillis = 500)
            up()
        }
        rule.waitForIdle()
        assertEquals(0L, controller.state.game!!.score)
        assertNotNull(controller.state.game!!.hand[0])
    }

    private fun captureScreen(name: String) {
        rule.waitForIdle()
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val bitmap = android.graphics.Bitmap.createBitmap(
                view.width,
                view.height,
                android.graphics.Bitmap.Config.ARGB_8888,
            )
            view.draw(android.graphics.Canvas(bitmap))
            val directory = java.io.File("build/reports/ui-snapshots").apply { mkdirs() }
            java.io.File(directory, "$name.png").outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
        }
    }
}
