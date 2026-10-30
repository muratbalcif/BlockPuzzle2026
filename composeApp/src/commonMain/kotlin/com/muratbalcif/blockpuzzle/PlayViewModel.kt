package com.muratbalcif.blockpuzzle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muratbalcif.blockpuzzle.persistence.LoadProblem
import com.muratbalcif.blockpuzzle.persistence.LoadedProgress
import com.muratbalcif.blockpuzzle.persistence.LocalProgress
import com.muratbalcif.blockpuzzle.persistence.ProgressCodec
import com.muratbalcif.blockpuzzle.persistence.ProgressStore
import com.muratbalcif.blockpuzzle.persistence.ProgressWriter
import com.muratbalcif.blockpuzzle.ui.GameController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayViewModel(private val store: ProgressStore) : ViewModel() {
    val controller: GameController =
        GameController(onSave = { writer.submit(it) }, onReload = ::reload)
    private val writer: ProgressWriter =
        ProgressWriter(viewModelScope, store, controller::saved, controller::saveFailed)
    init {
        reload()
    }

    private fun reload() {
        controller.beginLoading()
        viewModelScope.launch {
            val loaded = try {
                val raw = store.read()
                withContext(Dispatchers.Default) { ProgressCodec.decode(raw) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                LoadedProgress(LocalProgress(), LoadProblem.READ_ERROR)
            }
            controller.loadProgress(loaded)
        }
    }
}
