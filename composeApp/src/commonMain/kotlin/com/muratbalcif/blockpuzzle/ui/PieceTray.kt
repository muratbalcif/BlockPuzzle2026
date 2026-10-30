package com.muratbalcif.blockpuzzle.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.muratbalcif.blockpuzzle.engine.Cell
import com.muratbalcif.blockpuzzle.engine.GameStatus
import com.muratbalcif.blockpuzzle.engine.Piece
import kotlin.math.floor

@Composable
fun PieceTray(
    controller: GameController,
    onDrag: (Offset, Cell) -> Unit,
    onDrop: () -> Unit,
    onCancel: () -> Unit,
) {
    val state = controller.state
    val game = state.game ?: return
    val active = game.status == GameStatus.ACTIVE && !state.exitRequested
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        game.hand.forEachIndexed { slot, piece ->
            if (piece == null) {
                Box(Modifier.weight(1f).height(88.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Yerleştirildi",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                PieceCard(
                    piece = piece,
                    slot = slot,
                    selected = state.selectedSlot == slot,
                    active = active,
                    animations = state.animationsEnabled,
                    modifier = Modifier.weight(1f),
                    onSelect = { controller.selectPiece(slot) },
                    onDrag = onDrag,
                    onDrop = onDrop,
                    onCancel = onCancel,
                )
            }
        }
    }
}

@Composable
private fun PieceCard(
    piece: Piece,
    slot: Int,
    selected: Boolean,
    active: Boolean,
    animations: Boolean,
    modifier: Modifier,
    onSelect: () -> Unit,
    onDrag: (Offset, Cell) -> Unit,
    onDrop: () -> Unit,
    onCancel: () -> Unit,
) {
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val maxCell = with(LocalDensity.current) { 22.dp.toPx() }
    val scale by animateFloatAsState(
        if (selected) 1.03f else 1f,
        tween(if (animations) 120 else 0),
        label = "piece-selection",
    )
    val color = HandColors[slot]
    Box(
        modifier.height(88.dp).graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) color else Color(0xFFCFDDD3),
                RoundedCornerShape(16.dp),
            )
            .testTag("piece-$slot")
            .semantics(mergeDescendants = true) {
                this.selected = selected
                contentDescription =
                    "Parça ${slot + 1}, ${piece.cellCount} hücre, " +
                    "${piece.height} satır ${piece.width} sütun. " +
                    piece.cells.sortedWith(
                        compareBy({
                            it.row
                        }, {
                            it.column
                        }),
                    ).joinToString { "${it.row + 1}. satır ${it.column + 1}. sütun" }
            }
            .clickable(
                enabled = active,
                role = Role.Button,
                onClickLabel = "Parçayı seç",
                onClick = onSelect,
            ),
    ) {
        Canvas(
            Modifier.fillMaxSize().padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 24.dp)
                .onGloballyPositioned { coordinates = it }
                .pointerInput(piece, active, maxCell) {
                    if (active) {
                        var anchor = Cell(0, 0)
                        fun at(point: Offset) {
                            val local = coordinates ?: return
                            if (local.isAttached) onDrag(local.localToRoot(point), anchor)
                        }
                        detectDragGestures(
                            onDragStart = { point ->
                                onSelect()
                                val cell = minOf(
                                    maxCell,
                                    size.width.toFloat() / piece.width,
                                    size.height.toFloat() / piece.height,
                                )
                                val left = (size.width - piece.width * cell) / 2
                                val top = (size.height - piece.height * cell) / 2
                                anchor = Cell(
                                    floor((point.y - top) / cell).toInt().coerceIn(
                                        0,
                                        piece.height - 1,
                                    ),
                                    floor((point.x - left) / cell).toInt().coerceIn(
                                        0,
                                        piece.width - 1,
                                    ),
                                )
                                at(point)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                at(change.position)
                            },
                            onDragEnd = onDrop,
                            onDragCancel = onCancel,
                        )
                    }
                },
        ) {
            val cell = minOf(maxCell, size.width / piece.width, size.height / piece.height)
            val left = (size.width - piece.width * cell) / 2
            val top = (size.height - piece.height * cell) / 2
            for (offset in piece.cells) {
                drawRoundRect(
                    color,
                    topLeft = Offset(left + offset.column * cell + 1, top + offset.row * cell + 1),
                    size = Size((cell - 2).coerceAtLeast(1f), (cell - 2).coerceAtLeast(1f)),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }
        }
        Text(
            if (selected) "Seçili" else "Parça ${slot + 1}",
            Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}
