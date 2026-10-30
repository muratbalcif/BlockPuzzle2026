package com.muratbalcif.blockpuzzle

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.muratbalcif.blockpuzzle.engine.Cell
import com.muratbalcif.blockpuzzle.engine.GameConfig
import com.muratbalcif.blockpuzzle.engine.GameSession
import com.muratbalcif.blockpuzzle.engine.GameStatus
import com.muratbalcif.blockpuzzle.engine.MoveResult
import com.muratbalcif.blockpuzzle.persistence.LoadProblem
import com.muratbalcif.blockpuzzle.persistence.LoadedProgress
import com.muratbalcif.blockpuzzle.persistence.LocalProgress
import com.muratbalcif.blockpuzzle.persistence.ModeStats
import com.muratbalcif.blockpuzzle.persistence.PlayerPreferences
import com.muratbalcif.blockpuzzle.ui.GameController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProgressUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun setup(
        data: LocalProgress = LocalProgress(),
        onSave: ((LocalProgress) -> Unit)? = null,
    ): GameController {
        val controller =
            GameController(clockMillis = {
                0L
            }, nextSeed = { 30L }, initialProgress = data, onSave = onSave)
        rule.runOnUiThread { rule.activity.setContent { App(controller) } }
        rule.waitForIdle()
        return controller
    }
    private fun savedGame(): LocalProgress {
        val config = GameConfig()
        val game = (GameSession.start(config, 30).place(0, Cell(0, 0)) as MoveResult.Placed).game
        return LocalProgress(
            PlayerPreferences(config),
            game.snapshot(),
            listOf(ModeStats(config, started = 1, moves = 1)),
        )
    }

    @Test
    fun continueShowsPausedGameWithOriginalScoreAndConsumedPiece() {
        val controller = setup(savedGame())
        rule.onNodeWithTag("continue-game").performScrollTo().performClick()
        rule.onNodeWithText("Oyun duraklatıldı").assertIsDisplayed()
        assertEquals(GameStatus.PAUSED, controller.state.game!!.status)
        rule.onNodeWithText("Devam et").performClick()
        rule.onNodeWithTag("piece-0").assertDoesNotExist()
        rule.onNodeWithText("1").assertIsDisplayed()
    }

    @Test
    fun newGameCannotSilentlyReplaceExistingSave() {
        val controller = setup(savedGame())
        rule.onNodeWithTag("start-game").performScrollTo().performClick()
        rule.onNodeWithText("Yeni oyun başlatılsın mı?").assertIsDisplayed()
        rule.onNodeWithText("Vazgeç").performClick()
        assertNotNull(controller.state.savedGame)
        assertEquals(0L, controller.progress.statistics.single().abandoned)
        rule.onNodeWithTag("start-game").performClick()
        rule.onNodeWithText("Yeni oyun başlat").performClick()
        assertEquals(1L, controller.progress.statistics.single().abandoned)
        assertEquals(0L, controller.state.game!!.score)
    }

    @Test
    fun modeRecordAndStatisticsAreVisible() {
        val config = GameConfig()
        setup(
            LocalProgress(
                PlayerPreferences(config),
                statistics = listOf(
                    ModeStats(
                        config,
                        started = 3,
                        finished = 2,
                        abandoned = 1,
                        bestScore = 123,
                        moves = 24,
                        clearedLines = 5,
                        playMillis = 90_000,
                    ),
                ),
            ),
        )
        rule.onNodeWithText("123").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("statistics").performScrollTo().performClick()
        rule.onNodeWithText("Tamamlanan: 2").assertIsDisplayed()
        rule.onNodeWithText("Rekor: 123").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Kapat").performClick()
        rule.onNodeWithTag("size-16").performScrollTo().performClick()
        rule.onNodeWithText("Henüz tamamlanmış oyun yok.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun corruptedSaveRequiresExplicitRecoveryConfirmation() {
        var writes = 0
        val controller = setup(onSave = { writes++ })
        rule.runOnIdle {
            controller.loadProgress(LoadedProgress(LocalProgress(), LoadProblem.INVALID))
        }
        rule.onNodeWithText("Yerel kayıt açılamadı").assertIsDisplayed()
        rule.onNodeWithTag("start-game").performScrollTo().performClick()
        rule.onNodeWithText("Yeni yerel kayıt oluşturulsun mu?").assertIsDisplayed()
        assertEquals(0, writes)
        rule.onNodeWithText("Vazgeç").performClick()
        assertEquals(0, writes)
    }

    @Test
    fun writeFailureShowsRetryWithoutChangingStatistics() {
        var writes = 0
        val controller = setup(onSave = { writes++ })
        rule.onNodeWithTag("start-game").performScrollTo().performClick()
        rule.runOnIdle { controller.saveFailed(controller.progress) }
        rule.onNodeWithText("Tekrar dene").assertIsDisplayed().performClick()
        assertEquals(2, writes)
        assertEquals(1L, controller.progress.statistics.single().started)
        rule.runOnIdle { controller.saved(controller.progress) }
        rule.onNodeWithText("Tekrar dene").assertDoesNotExist()
    }
}
