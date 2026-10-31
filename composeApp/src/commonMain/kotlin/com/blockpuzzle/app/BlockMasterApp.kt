package com.blockpuzzle.app

import androidx.compose.foundation.background
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

private val pieces = listOf(
    Piece(setOf(Cell(0, 0))),
    Piece(setOf(Cell(0, 0), Cell(0, 1), Cell(1, 0))),
    Piece(setOf(Cell(0, 0), Cell(0, 1), Cell(1, 0), Cell(1, 1)))
)

@Composable
fun BlockMasterApp() {
    var board by remember { mutableStateOf(Board()) }
    var score by remember { mutableIntStateOf(0) }
    var selected by remember { mutableIntStateOf(0) }
    var available by remember { mutableStateOf(listOf(0, 1, 2)) }
    val gameOver = !GameEngine.hasAnyMove(board, available.map { pieces[it] })
    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFF101B35)).padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("BLOCK MASTER", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text("Score: $score", color = Color.White)
            Spacer(Modifier.height(24.dp))
            for (row in 0 until 8) {
                Row {
                    for (col in 0 until 8) {
                        val cell = Cell(row, col)
                        Box(
                            Modifier.padding(2.dp).size(34.dp)
                                .background(if (cell in board.occupied) Color(0xFF67C8FF) else Color(0xFF293854), RoundedCornerShape(5.dp))
                                .clickable(enabled = !gameOver && available.isNotEmpty()) {
                                    val result = GameEngine.place(board, pieces[selected], cell)
                                    if (result != null) {
                                        board = result.board
                                        score += result.points
                                        available = available.filterNot { it == selected }
                                        if (available.isEmpty()) available = listOf(0, 1, 2)
                                        selected = available.first()
                                    }
                                }
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(if (gameOver) "Game over" else "Choose a piece, then tap the board", color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                available.forEach { index ->
                    Button(onClick = { selected = index }, enabled = !gameOver) {
                        Text(if (selected == index) "● ${index + 1}" else "${index + 1}")
                    }
                }
            }
            Button(onClick = {
                board = Board()
                score = 0
                selected = 0
                available = listOf(0, 1, 2)
            }) { Text("New Game") }
        }
    }
}
