package com.muratbalcif.blockpuzzle

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.muratbalcif.blockpuzzle.engine.Cell
import com.muratbalcif.blockpuzzle.engine.GameConfig
import com.muratbalcif.blockpuzzle.engine.GameSession
import com.muratbalcif.blockpuzzle.engine.MoveResult
import com.muratbalcif.blockpuzzle.persistence.AndroidProgressStore
import com.muratbalcif.blockpuzzle.persistence.LocalProgress
import com.muratbalcif.blockpuzzle.persistence.ModeStats
import com.muratbalcif.blockpuzzle.persistence.PlayerPreferences
import com.muratbalcif.blockpuzzle.persistence.ProgressCodec
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ColdStartProgressTest {
    @get:Rule val rule = createEmptyComposeRule()

    @Test
    fun freshActivitiesLoadCommittedGameThroughRealViewModel() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = AndroidProgressStore(context)
        val config = GameConfig()
        val game = (GameSession.start(config, 30).place(0, Cell(0, 0)) as MoveResult.Placed).game
        val data =
            LocalProgress(
                PlayerPreferences(config),
                game.snapshot(),
                listOf(ModeStats(config, started = 1, moves = 1)),
            )
        runBlocking { store.write(ProgressCodec.encode(data)) }
        repeat(2) {
            ActivityScenario.launch(MainActivity::class.java).use {
                rule.waitUntil(5000) {
                    rule.onAllNodesWithTag("continue-game").fetchSemanticsNodes().isNotEmpty()
                }
                rule.onNodeWithTag("continue-game").performScrollTo().performClick()
                rule.onNodeWithText("Oyun duraklatıldı").assertIsDisplayed()
                rule.onNodeWithText("Devam et").performClick()
                rule.onNodeWithTag("piece-0").assertDoesNotExist()
                rule.onNodeWithText("1").assertIsDisplayed()
            }
        }
        val final = runBlocking { ProgressCodec.decode(store.read()) }.data
        assertEquals(1L, final.statistics.single().started)
        assertEquals(0L, final.statistics.single().finished)
    }

    @Test
    fun malformedPlatformSaveIsNotOverwrittenByLaunchOrCancel() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = AndroidProgressStore(context)
        runBlocking { store.write("{broken") }
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.waitUntil(5000) {
                rule.onAllNodesWithText("Yerel kayıt açılamadı").fetchSemanticsNodes().isNotEmpty()
            }
            assertEquals("{broken", runBlocking { store.read() })
            rule.onNodeWithTag("start-game").performScrollTo().performClick()
            rule.onNodeWithText("Vazgeç").performClick()
            assertEquals("{broken", runBlocking { store.read() })
        }
    }
}
