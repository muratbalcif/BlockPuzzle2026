package com.muratbalcif.blockpuzzle.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.muratbalcif.blockpuzzle.engine.Cell
import com.muratbalcif.blockpuzzle.engine.EndReason
import com.muratbalcif.blockpuzzle.engine.GameStatus

@Composable
fun GameScreen(controller: GameController) {
    val state = controller.state
    val game = state.game ?: return
    var enlarged by remember(game.config) { mutableStateOf(false) }
    var grid by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var viewport by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val dragAt: (Offset, Cell) -> Unit = { point, anchor ->
        val coordinates = grid
        val window = viewport
        if (coordinates != null && window != null && coordinates.isAttached && window.isAttached) {
            val topLeft = coordinates.positionInRoot()
            val bounds = Rect(
                topLeft.x,
                topLeft.y,
                topLeft.x + coordinates.size.width,
                topLeft.y + coordinates.size.height,
            )
            val geometry = BoardGeometry(bounds, window.boundsInRoot(), game.board.size)
            val cell = geometry.originAt(point, anchor)
            if (cell == null) controller.clearPreview() else controller.preview(cell)
        } else {
            controller.clearPreview()
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 600.dp && maxWidth > maxHeight
        val edge = if (wide) {
            minOf(maxHeight - 48.dp, maxWidth * 0.55f - 32.dp)
        } else {
            minOf(
                maxWidth - 32.dp,
                maxOf(120.dp, maxHeight - 300.dp),
                520.dp,
            )
        }
        val boardContent: @Composable () -> Unit = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BoardView(controller, edge.coerceAtLeast(120.dp), enlarged, { grid = it }, {
                    viewport =
                        it
                })
            }
        }
        val controls: @Composable () -> Unit = {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                GameHeader(controller, enlarged) {
                    enlarged = !enlarged
                    controller.clearPreview()
                }
                GameStats(controller)
                if (!wide) boardContent()
                Text(
                    if (state.previewCell != null) {
                        if (state.previewValid) {
                            "Yerleştirilebilir — bırakabilirsin."
                        } else {
                            "Buraya sığmıyor."
                        }
                    } else {
                        state.message
                    },
                    Modifier.fillMaxWidth().heightIn(min = 44.dp).semantics {
                        liveRegion =
                            LiveRegionMode.Polite
                    },
                    minLines = 2,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.previewCell != null &&
                        !state.previewValid
                    ) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                PieceTray(controller, dragAt, {
                    controller.commitPreview()
                }, controller::clearPreview)
            }
        }
        if (wide) {
            Row(
                Modifier.fillMaxSize().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Box(
                    Modifier.weight(0.55f).verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    boardContent()
                }
                Box(Modifier.weight(0.45f).verticalScroll(rememberScrollState())) { controls() }
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier.widthIn(
                        max = 600.dp,
                    ).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                ) {
                    controls()
                }
            }
        }
    }
    when {
        state.exitRequested -> AlertDialog(
            onDismissRequest = controller::cancelExit,
            title = { Text("Ana ekrana dönülsün mü?") },
            text = { Text("Oyunun kaydedilecek. Daha sonra kaldığın yerden devam edebilirsin.") },
            confirmButton = {
                TextButton(onClick = controller::confirmExit) { Text("Kaydet ve dön") }
            },
            dismissButton = { TextButton(onClick = controller::cancelExit) { Text("Oyuna dön") } },
        )
        game.status == GameStatus.PAUSED -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Oyun duraklatıldı") },
            text = { Text("Süre durdu. Hazır olduğunda devam edebilirsin.") },
            confirmButton = { Button(onClick = controller::resume) { Text("Devam et") } },
            dismissButton = { TextButton(onClick = controller::requestExit) { Text("Ana ekran") } },
        )
        game.status == GameStatus.FINISHED -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Oyun bitti") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (game.endReason ==
                            EndReason.TIMEOUT_LIMIT
                        ) {
                            "Üç süre hakkı da doldu."
                        } else {
                            "Kalan parçaların hiçbiri tahtaya sığmıyor."
                        },
                    )
                    Text("Skorun: ${game.score}", fontWeight = FontWeight.Bold)
                    if (state.newRecord) {
                        Text(
                            "Bu modda yeni rekor!",
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            },
            confirmButton = { Button(onClick = controller::startGame) { Text("Tekrar oyna") } },
            dismissButton = { TextButton(onClick = controller::confirmExit) { Text("Ana ekran") } },
        )
    }
}

@Composable
private fun GameHeader(controller: GameController, enlarged: Boolean, onZoom: () -> Unit) {
    val game = controller.state.game ?: return
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = controller::requestExit) { Text("Ana ekran") }
        TextButton(onClick = onZoom, modifier = Modifier.weight(1f)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${game.board.size}×${game.board.size} · ${game.config.difficulty.label()}",
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(if (enlarged) "Sığdır" else "Büyüt")
            }
        }
        OutlinedButton(onClick = controller::pause) { Text("Duraklat") }
    }
}

@Composable
private fun GameStats(controller: GameController) {
    val game = controller.state.game ?: return
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text("Skor", style = MaterialTheme.typography.labelMedium)
                Text(
                    "${game.score}",
                    Modifier.testTag("score"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(Modifier.weight(1f)) {
                Text("Süre", style = MaterialTheme.typography.labelMedium)
                Text(
                    game.remainingMillis?.let {
                        "${(it + 999) / 1000} sn"
                    } ?: "Süresiz",
                    Modifier.testTag("remaining-time"),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            if (game.remainingMillis != null) {
                Column(Modifier.weight(1f)) {
                    Text("Kalan hak", style = MaterialTheme.typography.labelMedium)
                    Text("${3 - game.timeouts} / 3", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}
