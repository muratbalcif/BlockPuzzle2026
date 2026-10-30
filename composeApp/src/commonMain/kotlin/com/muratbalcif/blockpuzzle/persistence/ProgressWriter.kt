package com.muratbalcif.blockpuzzle.persistence

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One in-flight write; newer pending documents replace older pending documents. */
class ProgressWriter(
    scope: CoroutineScope,
    store: ProgressStore,
    onSaved: (LocalProgress) -> Unit,
    onFailure: (LocalProgress) -> Unit,
    encodingDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val pending = Channel<LocalProgress>(Channel.CONFLATED)
    init {
        scope.launch {
            for (data in pending) {
                try {
                    val encoded = withContext(encodingDispatcher) { ProgressCodec.encode(data) }
                    store.write(encoded)
                    onSaved(data)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    onFailure(data)
                }
            }
        }
    }
    fun submit(data: LocalProgress) {
        pending.trySend(data)
    }
}
