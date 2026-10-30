package com.muratbalcif.blockpuzzle.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ProgressUi(controller: GameController) {
    val state = controller.state
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        if (state.saveProblem) {
            Snackbar(
                action = {
                    TextButton(
                        onClick = controller::retrySave,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.inversePrimary,
                        ),
                    ) {
                        Text("Tekrar dene")
                    }
                },
                modifier = Modifier.padding(12.dp).semantics { liveRegion = LiveRegionMode.Polite },
            ) { Text("Son ilerleme kaydedilemedi. Oyun açıkken tekrar deneyebilirsin.") }
        }
    }
    if (state.newGameRequested) {
        AlertDialog(
            onDismissRequest = controller::cancelNewGame,
            title = {
                Text(
                    if (state.loadProblem ==
                        null
                    ) {
                        "Yeni oyun başlatılsın mı?"
                    } else {
                        "Yeni yerel kayıt oluşturulsun mu?"
                    },
                )
            },
            text = {
                Text(
                    if (state.loadProblem ==
                        null
                    ) {
                        "Kayıtlı oyunun değiştirilecek ve bırakılmış sayılacak. Rekorların korunur."
                    } else {
                        "Açılamayan kayıt değiştirilecek; " +
                            "ilerleme ve istatistikler sıfırlanacak. " +
                            "Önce tekrar okumayı deneyebilirsin."
                    },
                )
            },
            confirmButton = {
                Button(onClick = controller::confirmNewGame) { Text("Yeni oyun başlat") }
            },
            dismissButton = { TextButton(onClick = controller::cancelNewGame) { Text("Vazgeç") } },
        )
    }
    if (state.showStatistics) {
        AlertDialog(
            onDismissRequest = { controller.showStatistics(false) },
            title = { Text("İstatistikler") },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val all = state.statistics
                    Text("Başlatılan: ${all.sumOf { it.started }}")
                    Text("Tamamlanan: ${all.sumOf { it.finished }}")
                    Text("Bırakılan: ${all.sumOf { it.abandoned }}")
                    Text(
                        "Hamle: ${all.sumOf {
                            it.moves
                        }} · Temizlenen çizgi: ${all.sumOf { it.clearedLines }}",
                    )
                    Text("Aktif süre: ${all.sumOf { it.playMillis } / 1000} saniye")
                    Text("Mod rekorları", fontWeight = FontWeight.Bold)
                    Text(
                        "Rekorlar bitirilen oyunların son skorudur; süre cezaları dahildir.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (all.isEmpty()) {
                        Text(
                            "İlk oyununu başlatınca istatistikler burada görünecek.",
                        )
                    }
                    for (stats in all.sortedWith(
                        compareBy({
                            it.config.boardSize
                        }, { it.config.difficulty.ordinal }, { it.config.tempo.ordinal }),
                    )) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(
                                "${stats.config.boardSize}×${stats.config.boardSize} · " +
                                    "${stats.config.difficulty.label()} · ${stats.config.tempo.label()}",
                                fontWeight = FontWeight.SemiBold,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Rekor: ${stats.bestScore?.toString() ?: "—"}")
                                Text("Biten: ${stats.finished}")
                            }
                            Text(
                                "Hamle: ${stats.moves} · Çizgi: ${stats.clearedLines}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { controller.showStatistics(false) }) { Text("Kapat") }
            },
        )
    }
}
