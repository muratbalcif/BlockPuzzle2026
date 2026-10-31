package com.blockpuzzle.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.blockpuzzle.game.*

private val background = Color(0xFF101B35)
private val emptyCell = Color(0xFF293854)
private val filledCell = Color(0xFF67C8FF)

@Composable
fun BlockMasterApp() {
    var board by remember { mutableStateOf(Board()) }
    var score by remember { mutableIntStateOf(0) }
    var seed by remember { mutableLongStateOf(2026L) }
    val initial = remember { PieceGenerator.next(seed, Board()) }
    var available by remember { mutableStateOf(initial.pieces) }
    var selected by remember { mutableIntStateOf(0) }
    val gameOver = available.isNotEmpty() && !GameEngine.hasAnyMove(board, available)
    MaterialTheme {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().background(background).padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            val tileSize = ((maxWidth - 24.dp) / 8).coerceAtMost(42.dp)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("BLOCK MASTER", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                Text("Score: $score", color = Color.White)
                Spacer(Modifier.height(16.dp))
                repeat(board.size) { row ->
                    Row {
                        repeat(board.size) { column ->
                            val cell = Cell(row, column)
                            Box(
                                Modifier.padding(2.dp).size(tileSize)
                                    .background(
                                        if (cell in board.occupied) filledCell else emptyCell,
                                        RoundedCornerShape(5.dp)
                                    )
                                    .clickable(enabled = !gameOver && available.isNotEmpty()) {
                                        val piece = available.getOrNull(selected) ?: return@clickable
                                        val placed = GameEngine.place(board, piece, cell) ?: return@clickable
                                        board = placed.board
                                        score += placed.points
                                        available = available.filterIndexed { index, _ -> index != selected }
                                        if (available.isEmpty()) {
                                            val batch = PieceGenerator.next(seed, board)
                                            available = batch.pieces
                                            seed = batch.nextSeed
                                        }
                                        selected = 0
                                    }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    if (gameOver) "Game over" else "Select a piece and tap a board cell",
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    available.forEachIndexed { index, piece ->
                        val isSelected = index == selected
                        Column(
                            modifier = Modifier
                                .border(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) filledCell else emptyCell,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable(enabled = !gameOver) { selected = index }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Piece ${index + 1}", color = Color.White)
                            repeat((piece.cells.maxOf { it.row } + 1)) { row ->
                                Row {
                                    repeat((piece.cells.maxOf { it.column } + 1)) { column ->
                                        Box(
                                            Modifier.padding(1.dp).size(12.dp)
                                                .background(
                                                    if (Cell(row, column) in piece.cells) filledCell else emptyCell,
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
                }) { Text("New Game") }
            }
        }
    }
}
