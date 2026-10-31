package com.blockpuzzle.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.blockpuzzle.game.*

private val navy = Color(0xFF101B35)
private val empty = Color(0xFF293854)
private val filled = Color(0xFF67C8FF)
private val previewColor = Color(0xFF3BAF87)

@Composable
fun BlockMasterApp() {
    var board by remember { mutableStateOf(Board()) }
    var score by remember { mutableIntStateOf(0) }
    var seed by remember { mutableLongStateOf(2026L) }
    var available by remember { mutableStateOf(PieceGenerator.next(2026L, Board()).pieces) }
    var selected by remember { mutableIntStateOf(0) }
    var preview by remember { mutableStateOf<Cell?>(null) }
    var boardPosition by remember { mutableStateOf(Offset.Zero) }
    var piecePosition by remember { mutableStateOf(Offset.Zero) }
    var pieceSize by remember { mutableStateOf(IntSize.Zero) }
    var drag by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }
    val gameOver = available.isNotEmpty() && !GameEngine.hasAnyMove(board, available)
    val density = LocalDensity.current

    fun commit(pieceIndex: Int, origin: Cell) {
        val piece = available.getOrNull(pieceIndex) ?: return
        val result = GameEngine.place(board, piece, origin) ?: return
        board = result.board
        score += result.points
        available = available.filterIndexed { index, _ -> index != pieceIndex }
        if (available.isEmpty()) {
            val batch = PieceGenerator.next(seed, board)
            available = batch.pieces
            seed = batch.nextSeed
        }
        selected = 0
        preview = null
    }

    MaterialTheme {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().background(navy).padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            val tile = ((maxWidth - 24.dp) / 8).coerceAtMost(42.dp)
            val tilePx = with(density) { (tile + 4.dp).toPx() }
            fun originAt(point: Offset): Cell? {
                val relative = point - boardPosition
                val row = (relative.y / tilePx).toInt()
                val col = (relative.x / tilePx).toInt()
                return Cell(row, col).takeIf { row in 0..7 && col in 0..7 }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("BLOCK MASTER", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                Text("Score: $score", color = Color.White)
                Spacer(Modifier.height(16.dp))
                Column(modifier = Modifier.onGloballyPositioned { boardPosition = it.positionInRoot() }) {
                    repeat(8) { row ->
                        Row {
                            repeat(8) { column ->
                                val cell = Cell(row, column)
                                val validPreview = preview?.let { origin ->
                                    available.getOrNull(selected)?.let { GameEngine.canPlace(board, it, origin) }
                                } == true
                                val previewCells = if (validPreview) {
                                    available[selected].cells.map { Cell(it.row + preview!!.row, it.column + preview!!.column) }
                                } else emptyList()
                                val target = when {
                                    cell in board.occupied -> filled
                                    cell in previewCells -> previewColor
                                    else -> empty
                                }
                                val color by animateColorAsState(targetValue = target, label = "board-cell")
                                Box(
                                    Modifier.padding(2.dp).size(tile)
                                        .background(color, RoundedCornerShape(5.dp))
                                        .clickable(enabled = !gameOver && !dragging) { commit(selected, cell) }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    if (gameOver) "Game over" else "Drag a piece onto the board, or tap a cell",
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    available.forEachIndexed { index, piece ->
                        val isSelected = index == selected
                        Column(
                            modifier = Modifier
                                .onGloballyPositioned {
                                    if (isSelected) {
                                        piecePosition = it.positionInRoot()
                                        pieceSize = it.size
                                    }
                                }
                                .graphicsLayer {
                                    if (dragging && isSelected) {
                                        translationX = drag.x
                                        translationY = drag.y
                                    }
                                }
                                .border(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) filled else empty,
                                    RoundedCornerShape(8.dp)
                                )
                                .pointerInput(index, board, available, boardPosition, piecePosition, pieceSize) {
                                    detectDragGestures(
                                        onDragStart = {
                                            selected = index
                                            dragging = true
                                            drag = Offset.Zero
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            drag += amount
                                            val drop = piecePosition + Offset(pieceSize.width / 2f, pieceSize.height / 2f) + drag
                                            preview = originAt(drop)?.takeIf { GameEngine.canPlace(board, piece, it) }
                                        },
                                        onDragEnd = {
                                            val drop = piecePosition + Offset(pieceSize.width / 2f, pieceSize.height / 2f) + drag
                                            val origin = originAt(drop)
                                            if (origin != null) commit(index, origin)
                                            dragging = false
                                            drag = Offset.Zero
                                            preview = null
                                        },
                                        onDragCancel = {
                                            dragging = false
                                            drag = Offset.Zero
                                            preview = null
                                        }
                                    )
                                }
                                .clickable(enabled = !gameOver) { selected = index }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Piece ${index + 1}", color = Color.White)
                            repeat(piece.cells.maxOf { it.row } + 1) { row ->
                                Row {
                                    repeat(piece.cells.maxOf { it.column } + 1) { column ->
                                        Box(
                                            Modifier.padding(1.dp).size(12.dp)
                                                .background(
                                                    if (Cell(row, column) in piece.cells) filled else empty,
                                                    RoundedCornerShape(2.dp)
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    board = Board()
                    score = 0
                    seed = 2026L
                    available = PieceGenerator.next(seed, board).pieces
                    selected = 0
                    preview = null
                }) { Text("New Game") }
            }
        }
    }
}
