package com.muratbalcif.blockpuzzle.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.muratbalcif.blockpuzzle.engine.Cell
import com.muratbalcif.blockpuzzle.engine.GameStatus
import kotlin.math.floor

@Composable
fun BoardView(
    controller: GameController,
    edge: Dp,
    enlarged: Boolean,
    onGrid: (LayoutCoordinates) -> Unit,
    onViewport: (LayoutCoordinates) -> Unit,
) {
    val state = controller.state
    val game = state.game ?: return
    val active = game.status == GameStatus.ACTIVE && !state.exitRequested
    val boardSize = game.board.size
    val occupied = game.board.occupiedCells
    val previewOrigin = state.previewCell
    val selectedPiece = state.selectedSlot?.let { game.hand[it] }
    val previewCells = if (previewOrigin != null && selectedPiece != null) {
        selectedPiece.cells.map {
            Cell(it.row + previewOrigin.row, it.column + previewOrigin.column)
        }.toSet()
    } else {
        emptySet()
    }
    val gridEdge = if (enlarged) maxOf(edge, (48 * boardSize).dp) else edge
    val cellEdge = gridEdge / boardSize
    Box(
        Modifier.size(edge).clip(RoundedCornerShape(12.dp)).background(Color(0xFFD0DED5))
            .onGloballyPositioned(onViewport).testTag("board-viewport"),
    ) {
        Box(
            Modifier.horizontalScroll(
                rememberScrollState(),
                enabled = enlarged,
            ).testTag("board-horizontal-scroll"),
        ) {
            Box(
                Modifier.verticalScroll(
                    rememberScrollState(),
                    enabled = enlarged,
                ).testTag("board-vertical-scroll"),
            ) {
                Column(
                    Modifier.size(gridEdge).onGloballyPositioned(onGrid).testTag("board-grid")
                        .semantics { collectionInfo = CollectionInfo(boardSize, boardSize) }
                        .pointerInput(boardSize, active, gridEdge) {
                            if (active) {
                                detectTapGestures { point ->
                                    val row = floor(
                                        point.y / (size.height.toFloat() / boardSize),
                                    ).toInt()
                                    val column = floor(
                                        point.x / (size.width.toFloat() / boardSize),
                                    ).toInt()
                                    controller.placeAt(Cell(row, column))
                                }
                            }
                        },
                ) {
                    repeat(boardSize) { row ->
                        Row {
                            repeat(boardSize) { column ->
                                val cell = Cell(row, column)
                                val inPreview = cell in previewCells
                                val target = when {
                                    inPreview && state.previewValid -> Color(0xFFAADEBD)
                                    inPreview -> Color(0xFFF0B5AF)
                                    cell in (state.feedback?.clearedCells ?: emptySet()) -> Color(
                                        0xFFF3CD74,
                                    )
                                    cell in occupied -> BoardInk
                                    else -> EmptyCell
                                }
                                val color by animateColorAsState(
                                    target,
                                    animationSpec = tween(if (state.animationsEnabled) 160 else 0),
                                    label = "cell-color",
                                )
                                val border = if (inPreview) {
                                    if (state.previewValid) Color(0xFF126B5B) else Color(0xFFB42318)
                                } else {
                                    Color.Transparent
                                }
                                Box(
                                    Modifier.size(cellEdge).testTag("cell-$row-$column")
                                        .semantics {
                                            contentDescription =
                                                "Satır ${row + 1}, sütun ${column + 1}, " +
                                                if (cell in occupied) "dolu" else "boş"
                                            collectionItemInfo =
                                                CollectionItemInfo(row, 1, column, 1)
                                            role = Role.Button
                                            if (active) {
                                                onClick(label = "Seçili parçayı yerleştir") {
                                                    controller.placeAt(cell)
                                                    true
                                                }
                                            }
                                        }
                                        .padding(if (boardSize == 16 && !enlarged) 1.dp else 2.dp)
                                        .background(color, RoundedCornerShape(4.dp))
                                        .border(
                                            if (inPreview) 2.dp else 0.dp,
                                            border,
                                            RoundedCornerShape(4.dp),
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
