package de.gerry.dranbleiben.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.gerry.dranbleiben.data.Habit
import de.gerry.dranbleiben.data.HabitLog
import de.gerry.dranbleiben.data.Store
import de.gerry.dranbleiben.data.fmt
import de.gerry.dranbleiben.data.toNumber
import de.gerry.dranbleiben.ui.theme.habitColor
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Abfrage: geht die heute noch offenen Gewohnheiten nacheinander durch – geöffnet aus der Erinnerung
 * oder über „Abfrage“ auf der Startseite.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(store: Store, onClose: () -> Unit) {
    val today = remember { LocalDate.now() }
    val scope = rememberCoroutineScope()
    val habits by store.habits.collectAsState(initial = null)
    val logs by store.logs.collectAsState(initial = null)
    // Die Liste wird beim Öffnen festgelegt, damit Beantwortetes nicht verschwindet, während man weiterblättert.
    var queue by remember { mutableStateOf<List<Habit>?>(null) }
    var index by remember { mutableIntStateOf(0) }
    var answered by remember { mutableIntStateOf(0) }
    var success by remember { mutableIntStateOf(0) }

    LaunchedEffect(habits, logs) {
        val h = habits ?: return@LaunchedEffect
        val l = logs ?: return@LaunchedEffect
        if (queue == null) queue = h.filter { !it.archived && it.isPlanned(today) && l[it.uid]?.get(today) == null }
    }

    fun next(saved: HabitLog?) {
        val h = queue?.getOrNull(index) ?: return
        if (saved != null) {
            scope.launch { store.saveLog(saved) }
            answered++
            if (h.isSuccess(saved) == true) success++
        }
        index++
    }

    val list = queue
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Abfrage") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, "Schließen") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (list == null) return@Column
            if (list.isNotEmpty()) {
                LinearProgressIndicator(
                    progress = { (index.coerceAtMost(list.size)).toFloat() / list.size },
                    modifier = Modifier.fillMaxWidth(),
                    strokeCap = StrokeCap.Round,
                )
            }
            AnimatedContent(
                targetState = index,
                transitionSpec = { (slideInHorizontally { it / 3 } + fadeIn()) togetherWith fadeOut() },
                label = "abfrage",
                modifier = Modifier.weight(1f),
            ) { i ->
                val h = list.getOrNull(i)
                if (h == null) {
                    Done(list.size, answered, success, onClose)
                } else {
                    Question(h, i, list.size, onAnswer = { next(it) }, onSkip = { next(null) })
                }
            }
        }
    }
}

@Composable
private fun Question(habit: Habit, index: Int, total: Int, onAnswer: (HabitLog) -> Unit, onSkip: () -> Unit) {
    val today = LocalDate.now()
    val color = habitColor(habit.color)
    var text by remember(habit.uid) { mutableStateOf("") }
    val value = text.toNumber()

    Column(
        Modifier.fillMaxSize().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("${index + 1} von $total", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HabitAvatar(habit, size = 88)
        Text(habit.name, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(
            listOfNotNull(habit.question(), habit.targetLabel()?.let { "Ziel: $it" }).joinToString(" · "),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (habit.notes.isNotBlank()) {
            Text("„${habit.notes}“", style = MaterialTheme.typography.bodyMedium, color = color, textAlign = TextAlign.Center)
        }
        Box(Modifier.height(8.dp))

        if (habit.isMeasure) {
            NumberField(text, { text = it }, habit.unit.ifBlank { "Wert" }, Modifier.fillMaxWidth(), suffix = habit.unit.ifBlank { null })
            val steps = quickSteps(habit)
            if (steps.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    steps.forEach { s -> AssistChip(onClick = { text = ((value ?: 0.0) + s).fmt() }, label = { Text("+${s.fmt()}") }) }
                }
            }
            Button(
                onClick = { value?.let { onAnswer(HabitLog(habit.uid, today, done = habit.isSuccess(HabitLog(habit.uid, today, false, it)) == true, value = it)) } },
                enabled = value != null,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color),
            ) { Text("Eintragen") }
        } else {
            Button(
                onClick = { onAnswer(HabitLog(habit.uid, today, done = true)) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color),
            ) {
                Icon(Icons.Outlined.Check, null)
                Text("  Ja, geschafft")
            }
            OutlinedButton(
                onClick = { onAnswer(HabitLog(habit.uid, today, done = false)) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text("Heute nicht") }
        }
        TextButton(onClick = onSkip) { Text("Später") }
    }
}

@Composable
private fun Done(total: Int, answered: Int, success: Int, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(if (total == 0 || success == total) "🎉" else "👍", style = MaterialTheme.typography.displayMedium)
        Text(
            when {
                total == 0 -> "Für heute ist alles eingetragen"
                success == total -> "Alles geschafft!"
                else -> "Danke fürs Eintragen"
            },
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        if (total > 0) {
            Text(
                "$success von $total geschafft" + if (answered < total) " · ${total - answered} später" else "",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = onClose, modifier = Modifier.padding(top = 16.dp)) { Text("Zur Übersicht") }
    }
}
