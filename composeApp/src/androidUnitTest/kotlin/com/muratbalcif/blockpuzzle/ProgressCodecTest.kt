package com.muratbalcif.blockpuzzle

import com.muratbalcif.blockpuzzle.engine.Cell
import com.muratbalcif.blockpuzzle.engine.Difficulty
import com.muratbalcif.blockpuzzle.engine.GameConfig
import com.muratbalcif.blockpuzzle.engine.GameSession
import com.muratbalcif.blockpuzzle.engine.MoveResult
import com.muratbalcif.blockpuzzle.engine.Tempo
import com.muratbalcif.blockpuzzle.persistence.LoadProblem
import com.muratbalcif.blockpuzzle.persistence.LocalProgress
import com.muratbalcif.blockpuzzle.persistence.ModeStats
import com.muratbalcif.blockpuzzle.persistence.PlayerPreferences
import com.muratbalcif.blockpuzzle.persistence.ProgressCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressCodecTest {
    @Test
    fun roundTripPreservesSettingsResumeAndPerModeStatistics() {
        val config = GameConfig(16, Difficulty.HARD, Tempo.FAST)
        val started = GameSession.start(config, 42)
        val moved = (started.place(0, Cell(0, 0)) as MoveResult.Placed).game.advanceTime(1234)
        val data = LocalProgress(
            PlayerPreferences(config, animations = false, haptics = false),
            moved.snapshot(),
            listOf(ModeStats(config, started = 1, moves = 1, playMillis = 1234)),
        )
        val decoded = ProgressCodec.decode(ProgressCodec.encode(data))
        assertNull(decoded.problem)
        assertEquals(data, decoded.data)
    }

    @Test
    fun absentSaveIsFreshProgressWithoutWarning() {
        val loaded = ProgressCodec.decode(null)
        assertNull(loaded.problem)
        assertEquals(LocalProgress(), loaded.data)
    }

    @Test
    fun malformedAndOversizedPayloadsAreRejected() {
        for (raw in listOf("{", "[]", "", "x".repeat(1_048_577))) {
            assertEquals(LoadProblem.INVALID, ProgressCodec.decode(raw).problem)
        }
    }

    @Test
    fun unsupportedVersionCannotBeMistakenForEmptyProgress() {
        val raw = ProgressCodec.encode(LocalProgress()).replace("\"format\":1", "\"format\":99")
        assertEquals(LoadProblem.UNSUPPORTED, ProgressCodec.decode(raw).problem)
    }

    @Test
    fun malformedEngineStateIsRejectedBeforeResume() {
        val config = GameConfig()
        val data =
            LocalProgress(
                resume = GameSession.start(config, 42).snapshot(),
                statistics = listOf(ModeStats(config, started = 1)),
            )
        val raw = ProgressCodec.encode(
            data,
        ).replace("\"generatorState\":", "\"generatorState\":0,\"ignored\":")
        assertEquals(LoadProblem.INVALID, ProgressCodec.decode(raw).problem)
    }

    @Test
    fun duplicateModesAndInconsistentCountsAreRejected() {
        val config = GameConfig()
        val valid =
            LocalProgress(
                statistics = listOf(ModeStats(config, started = 1, finished = 1, bestScore = 12)),
            )
        val encoded = ProgressCodec.encode(valid)
        assertEquals(
            LoadProblem.INVALID,
            ProgressCodec.decode(encoded.replace("\"finished\":1", "\"finished\":2")).problem,
        )
        val duplicate = encoded.replace(
            "\"statistics\":[",
            "\"statistics\":[" + encoded.substringAfter(
                "\"statistics\":[",
            ).substringBeforeLast("]") +
                ",",
        )
        assertEquals(LoadProblem.INVALID, ProgressCodec.decode(duplicate).problem)
    }

    @Test
    fun unknownFieldsDoNotBreakCurrentFormat() {
        val raw = ProgressCodec.encode(LocalProgress()).replaceFirst("{", "{\"futureNote\":true,")
        assertNull(ProgressCodec.decode(raw).problem)
    }

    @Test
    fun completedResultHasNoResumableTerminalGame() {
        val config = GameConfig(tempo = Tempo.FAST)
        val ended = GameSession.start(config, 42).advanceTime(60_000)
        val valid =
            LocalProgress(
                statistics = listOf(ModeStats(config, started = 1, finished = 1, bestScore = 0)),
            )
        assertNull(ProgressCodec.decode(ProgressCodec.encode(valid)).data.resume)
        assertNotNull(ended.endReason)
    }

    @Test
    fun resumeWithMissingModeStatisticsIsRejectedWithoutThrowing() {
        val config = GameConfig()
        val valid =
            LocalProgress(
                resume = GameSession.start(config, 30).snapshot(),
                statistics = listOf(ModeStats(config, started = 1)),
            )
        val raw = ProgressCodec.encode(valid)
        // Change only the statistics mode; the resume still belongs to an8x8 board.
        val altered = raw.substringBefore("\"statistics\":") + "\"statistics\":" +
            raw.substringAfter("\"statistics\":").replace("\"boardSize\":8", "\"boardSize\":16")
        assertEquals(LoadProblem.INVALID, ProgressCodec.decode(altered).problem)
    }
}
