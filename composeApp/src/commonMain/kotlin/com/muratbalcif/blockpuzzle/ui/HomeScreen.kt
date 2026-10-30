package com.muratbalcif.blockpuzzle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.muratbalcif.blockpuzzle.engine.Difficulty
import com.muratbalcif.blockpuzzle.engine.Tempo

fun Difficulty.label(): String = when (this) {
    Difficulty.EASY -> "Kolay"
    Difficulty.MEDIUM -> "Orta"
    Difficulty.HARD -> "Zor"
}

fun Tempo.label(): String = when (this) {
    Tempo.RELAXED -> "Rahat"
    Tempo.SLOW -> "Yavaş"
    Tempo.MEDIUM -> "Orta"
    Tempo.FAST -> "Hızlı"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(controller: GameController) {
    val state = controller.state
    val config = state.config
    val scroll = rememberScrollState()
    LaunchedEffect(state.loading) {
        if (!state.loading) {
            withFrameNanos { }
            scroll.scrollTo(0)
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 560.dp).fillMaxWidth()
                .verticalScroll(scroll).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(2) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            repeat(2) { column ->
                                Box(
                                    Modifier.size(18.dp).background(
                                        if (row ==
                                            column
                                        ) {
                                            BoardInk
                                        } else {
                                            HandColors[2]
                                        },
                                        RoundedCornerShape(4.dp),
                                    ),
                                )
                            }
                        }
                    }
                }
                Text(
                    "BlockPuzzle2026",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                "Kendi ritminde oyna.",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Parçaları yerleştir, dolan satır ve sütunları temizle.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.loading) Text("Kayıt yükleniyor…")
            if (state.loadProblem != null) {
                SetupSection("Yerel kayıt açılamadı") {
                    Text(
                        "Mevcut kayıt değiştirilmedi. Tekrar deneyebilir " +
                            "veya onaylayarak yeni oyun başlatabilirsin.",
                    )
                    androidx.compose.material3.TextButton(
                        onClick = controller::retrySave,
                        enabled = !state.loading,
                    ) {
                        Text("Tekrar oku")
                    }
                }
            }
            state.savedGame?.let { saved ->
                SetupSection("Kayıtlı oyun") {
                    Text(
                        "${saved.config.boardSize}×${saved.config.boardSize} · " +
                            "${saved.config.difficulty.label()} · ${saved.config.tempo.label()}",
                    )
                    Text("Skor: ${saved.score}. Devam ederken süre duraklatılmış kalır.")
                    when (state.saveStatus) {
                        SaveStatus.SAVING -> Text(
                            "Kaydediliyor…",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        SaveStatus.SAVED -> Text(
                            "Cihazda kayıtlı.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        SaveStatus.ERROR -> Text(
                            "Son kayıt tamamlanamadı.",
                            color = MaterialTheme.colorScheme.error,
                        )
                        SaveStatus.IDLE -> Unit
                    }
                    Button(
                        onClick = controller::continueGame,
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth().testTag("continue-game"),
                    ) {
                        Text("Kayıtlı oyuna devam")
                    }
                }
            }
            SetupSection("Tahta boyutu") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (size in listOf(8, 16)) {
                        FilterChip(
                            selected = config.boardSize == size,
                            enabled = !state.loading && state.loadProblem == null,
                            onClick = {
                                controller.configure(size, config.difficulty, config.tempo)
                            },
                            label = { Text("$size × $size") },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("size-$size"),
                        )
                    }
                }
            }
            SetupSection("Zorluk") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (difficulty in Difficulty.entries) {
                        FilterChip(
                            selected = config.difficulty == difficulty,
                            enabled = !state.loading && state.loadProblem == null,
                            onClick = {
                                controller.configure(config.boardSize, difficulty, config.tempo)
                            },
                            label = { Text(difficulty.label()) },
                            modifier = Modifier.heightIn(
                                min = 48.dp,
                            ).testTag("difficulty-${difficulty.name}"),
                        )
                    }
                }
                Text(
                    when (config.difficulty) {
                        Difficulty.EASY -> "Küçük ve basit parçalar daha sık gelir."
                        Difficulty.MEDIUM -> "Tüm parçalardan dengeli bir karışım."
                        Difficulty.HARD -> "Büyük ve zor şekiller daha sık gelir."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            SetupSection("Tempo") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (tempo in Tempo.entries) {
                        FilterChip(
                            selected = config.tempo == tempo,
                            enabled = !state.loading && state.loadProblem == null,
                            onClick = {
                                controller.configure(config.boardSize, config.difficulty, tempo)
                            },
                            label = { Text(tempo.label()) },
                            modifier = Modifier.heightIn(
                                min = 48.dp,
                            ).testTag("tempo-${tempo.name}"),
                        )
                    }
                }
                val limit = config.tempo.moveLimitMillis
                Text(
                    if (limit ==
                        null
                    ) {
                        "Süre sınırı yok. Düşünmek için zamanın var."
                    } else {
                        "Hamle başına ${limit / 1000} saniye. " +
                            "Süre aşımı −10 puan; üçüncüde oyun biter."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            SetupSection("Bu moddaki rekorun") {
                val stats = state.statistics.firstOrNull { it.config == config }
                Text(
                    stats?.bestScore?.toString() ?: "Henüz tamamlanmış oyun yok.",
                    style = MaterialTheme.typography.titleLarge,
                )
                androidx.compose.material3.TextButton(onClick = {
                    controller.showStatistics(true)
                }, modifier = Modifier.testTag("statistics")) {
                    Text("Tüm istatistikler")
                }
            }
            SetupSection("Görünüm ve geri bildirim") {
                Preference(
                    "Animasyonlar",
                    state.animationsEnabled,
                    !state.loading && state.loadProblem == null,
                ) {
                    controller.effects(it, state.hapticsEnabled)
                }
                Preference(
                    "Titreşim",
                    state.hapticsEnabled,
                    !state.loading && state.loadProblem == null,
                ) {
                    controller.effects(state.animationsEnabled, it)
                }
            }
            Button(
                onClick = controller::startGame,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("start-game"),
                shape = RoundedCornerShape(18.dp),
            ) { Text("Oyuna başla", style = MaterialTheme.typography.titleMedium) }
        }
    }
}

@Composable
private fun SetupSection(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun Preference(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
