package com.muratbalcif.blockpuzzle

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.muratbalcif.blockpuzzle.engine.Cell
import com.muratbalcif.blockpuzzle.engine.GameConfig
import com.muratbalcif.blockpuzzle.engine.GameSession
import com.muratbalcif.blockpuzzle.engine.GameStatus
import com.muratbalcif.blockpuzzle.engine.MoveResult
import com.muratbalcif.blockpuzzle.persistence.AndroidProgressStore
import com.muratbalcif.blockpuzzle.persistence.LocalProgress
import com.muratbalcif.blockpuzzle.persistence.ModeStats
import com.muratbalcif.blockpuzzle.persistence.ProgressCodec
import com.muratbalcif.blockpuzzle.ui.GameController
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidProgressStoreTest {
    @Test
    fun freshStoreAndControllerReadSameCommittedGameAndUsedSlots() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "f3_roundtrip_test"
        context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
        val store = AndroidProgressStore(context, name)
        assertNull(store.read())
        val config = GameConfig()
        val game = (GameSession.start(config, 30).place(0, Cell(0, 0)) as MoveResult.Placed).game
        val progress =
            LocalProgress(
                resume = game.snapshot(),
                statistics = listOf(ModeStats(config, started = 1, moves = 1)),
            )
        store.write(ProgressCodec.encode(progress))
        val loaded = ProgressCodec.decode(AndroidProgressStore(context, name).read())
        assertNull(loaded.problem)
        assertEquals(progress, loaded.data)
        val controller = GameController(initialProgress = loaded.data)
        controller.continueGame()
        assertEquals(GameStatus.PAUSED, controller.state.game!!.status)
        assertNull(controller.state.game!!.hand[0])
        assertEquals(1L, controller.state.game!!.score)
    }

    @Test
    fun wholeDocumentReplacementRemovesOldResumeAlongsideFinalStatistics() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = AndroidProgressStore(context, "f3_replacement_test")
        val config = GameConfig()
        val active =
            LocalProgress(
                resume = GameSession.start(config, 30).snapshot(),
                statistics = listOf(ModeStats(config, started = 1)),
            )
        val completed =
            LocalProgress(
                statistics = listOf(ModeStats(config, started = 1, finished = 1, bestScore = 123)),
            )
        store.write(ProgressCodec.encode(active))
        store.write(ProgressCodec.encode(completed))
        val loaded = ProgressCodec.decode(
            AndroidProgressStore(context, "f3_replacement_test").read(),
        ).data
        assertNull(loaded.resume)
        assertEquals(1L, loaded.statistics.single().finished)
        assertEquals(123L, loaded.statistics.single().bestScore)
    }
}
