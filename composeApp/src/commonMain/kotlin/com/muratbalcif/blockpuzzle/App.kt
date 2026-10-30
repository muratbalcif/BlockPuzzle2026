package com.muratbalcif.blockpuzzle

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.muratbalcif.blockpuzzle.persistence.rememberProgressStore
import com.muratbalcif.blockpuzzle.ui.GameController
import com.muratbalcif.blockpuzzle.ui.GameScreen
import com.muratbalcif.blockpuzzle.ui.HomeScreen
import com.muratbalcif.blockpuzzle.ui.ProgressUi
import com.muratbalcif.blockpuzzle.ui.PuzzleTheme
import com.muratbalcif.blockpuzzle.ui.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun App(controller: GameController? = null, observeLifecycle: Boolean = true) {
    val store = if (controller == null) rememberProgressStore() else null
    val active = controller ?: viewModel { PlayViewModel(checkNotNull(store)) }.controller
    val state = active.state
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    if (observeLifecycle) {
        DisposableEffect(lifecycle, active) {
            active.setForeground(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> active.setForeground(true)
                    Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> active.setForeground(false)
                    else -> Unit
                }
            }
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer) }
        }
    }
    LaunchedEffect(active) {
        while (isActive) {
            delay(100)
            active.tick()
        }
    }
    val haptic = LocalHapticFeedback.current
    val feedback = state.feedback
    LaunchedEffect(feedback?.id) {
        if (feedback != null) {
            if (state.hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(if (state.animationsEnabled) 350 else 0)
            active.dismissFeedback(feedback.id)
        }
    }
    AppBackHandler(enabled = state.screen == Screen.GAME) {
        if (state.exitRequested) active.cancelExit() else active.requestExit()
    }
    PuzzleTheme {
        Surface(
            Modifier.fillMaxSize().safeDrawingPadding(),
            color = MaterialTheme.colorScheme.background,
        ) {
            androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
                when (state.screen) {
                    Screen.HOME -> HomeScreen(active)
                    Screen.GAME -> GameScreen(active)
                }
                ProgressUi(active)
            }
        }
    }
}
