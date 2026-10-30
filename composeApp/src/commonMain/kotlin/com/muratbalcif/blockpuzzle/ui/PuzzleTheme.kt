package com.muratbalcif.blockpuzzle.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BoardInk = Color(0xFF168471)
val EmptyCell = Color(0xFFE0EAE5)
val HandColors = listOf(Color(0xFF19877B), Color(0xFF4373BC), Color(0xFFC47726))

private val PuzzleColors = lightColorScheme(
    primary = Color(0xFF126B5B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7EEE3),
    onPrimaryContainer = Color(0xFF173D32),
    secondary = Color(0xFF936023),
    secondaryContainer = Color(0xFFD7EEE3),
    onSecondaryContainer = Color(0xFF173D32),
    background = Color(0xFFF5F7F2),
    onBackground = Color(0xFF203A32),
    surface = Color.White,
    onSurface = Color(0xFF203A32),
    surfaceVariant = Color(0xFFE8EEE6),
    onSurfaceVariant = Color(0xFF495F54),
    error = Color(0xFFB42318),
    inversePrimary = Color(0xFFAADEBD),
)

@Composable
fun PuzzleTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PuzzleColors, content = content)
}
