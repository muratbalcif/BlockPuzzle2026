package com.muratbalcif.blockpuzzle

import com.muratbalcif.blockpuzzle.engine.Difficulty
import com.muratbalcif.blockpuzzle.engine.GameConfig
import com.muratbalcif.blockpuzzle.persistence.LocalProgress
import com.muratbalcif.blockpuzzle.persistence.PlayerPreferences
import com.muratbalcif.blockpuzzle.persistence.ProgressCodec
import com.muratbalcif.blockpuzzle.persistence.ProgressStore
import com.muratbalcif.blockpuzzle.persistence.ProgressWriter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressWriterTest {
    @Test
    fun inFlightWriteCompletesBeforeNewestConflatedDocument() = runTest {
        val firstGate = CompletableDeferred<Unit>()
        val written = mutableListOf<String>()
        val saved = mutableListOf<LocalProgress>()
        val store = object : ProgressStore {
            override suspend fun read(): String? = written.lastOrNull()
            override suspend fun write(value: String) {
                if (written.isEmpty()) firstGate.await()
                written += value
            }
        }
        val writer =
            ProgressWriter(backgroundScope, store, saved::add, {
                error("Unexpected failure")
            }, StandardTestDispatcher(testScheduler))
        val first = LocalProgress()
        val intermediate = LocalProgress(PlayerPreferences(GameConfig(16)))
        val last = LocalProgress(PlayerPreferences(GameConfig(16, Difficulty.HARD)))
        writer.submit(first)
        runCurrent()
        writer.submit(intermediate)
        writer.submit(last)
        firstGate.complete(Unit)
        runCurrent()
        assertEquals(listOf(first, last), written.map { ProgressCodec.decode(it).data })
        assertEquals(listOf(first, last), saved)
    }

    @Test
    fun failedWriteCanBeRetriedWithoutLosingLatestDocument() = runTest {
        var attempts = 0
        var stored: String? = null
        val failures = mutableListOf<LocalProgress>()
        val saved = mutableListOf<LocalProgress>()
        val store = object : ProgressStore {
            override suspend fun read(): String? = stored
            override suspend fun write(value: String) {
                attempts++
                if (attempts == 1) error("Disk unavailable")
                stored = value
            }
        }
        val writer =
            ProgressWriter(
                backgroundScope,
                store,
                saved::add,
                failures::add,
                StandardTestDispatcher(testScheduler),
            )
        val data = LocalProgress()
        writer.submit(data)
        runCurrent()
        assertEquals(listOf(data), failures)
        writer.submit(data)
        runCurrent()
        assertEquals(2, attempts)
        assertEquals(listOf(data), saved)
        assertEquals(data, ProgressCodec.decode(stored).data)
    }
}
