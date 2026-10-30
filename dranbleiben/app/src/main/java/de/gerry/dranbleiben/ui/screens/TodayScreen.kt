package de.gerry.dranbleiben.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.gerry.dranbleiben.data.Habit
import de.gerry.dranbleiben.data.HabitLog
import de.gerry.dranbleiben.data.HabitStats
import de.gerry.dranbleiben.data.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    store: Store,
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    onCheckIn: () -> Unit,
    onNewHabit: () -> Unit,
    contentPadding: PaddingValues,
) {
    val habits by store.habits.collectAsState(initial = null)
    val logs by store.logs.collectAsState(initial = emptyMap())
    val scope = rememberCoroutineScope()
    var measuring by remember { mutableStateOf<Habit?>(null) }

    val active = habits.orEmpty().filter { !it.archived }
    val planned = active.filter { it.isPlanned(date) }
    val free = active.filter { !it.isPlanned(date) }
    val dayLogs = active.associate { it.uid to logs[it.uid]?.get(date) }
    val openToday = date == LocalDate.now() && planned.any { dayLogs[it.uid] == null }

    Scaffold(
        modifier = Modifier.padding(contentPadding),
        topBar = {
            TopAppBar(
                title = { Text(dayLabel(date)) },
                actions = {
                    if (openToday) {
                        TextButton(onClick = onCheckIn) {
                            Icon(Icons.Outlined.PlayArrow, null)
                            Text(" Abfrage")
                        }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { WeekStrip(date, active, logs, onDateChange) }
            if (habits != null && active.isEmpty()) {
                item {
                    EmptyHint("Noch keine Gewohnheiten", "Leg deine erste an – Ja/Nein wie „Meditieren“ oder messbar wie „Gewicht“.") {
                        Button(onClick = onNewHabit, modifier = Modifier.padding(top = 8.dp)) {
                            Icon(Icons.Outlined.Add, null)
                            Text(" Gewohnheit anlegen")
                        }
                    }
                }
                return@LazyColumn
            }
            if (planned.isNotEmpty()) {
                item { DaySummary(planned.count { it.isSuccess(dayLogs[it.uid]) == true }, planned.size) }
            }
            items(planned, key = { it.uid }) { h ->
                HabitDayRow(
                    habit = h,
                    log = dayLogs[h.uid],
                    stats = HabitStats.of(h, logs[h.uid].orEmpty()),
                    onToggle = { scope.toggle(store, h, date, dayLogs[h.uid]) },
                    onMeasure = { measuring = h },
                )
            }
            if (free.isNotEmpty()) {
                item {
                    Text(
                        if (date == LocalDate.now()) "Heute nicht geplant" else "An diesem Tag nicht geplant",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp, start = 4.dp),
                    )
                }
                items(free, key = { "frei-" + it.uid }) { h ->
                    HabitDayRow(
                        habit = h,
                        log = dayLogs[h.uid],
                        stats = null,
                        onToggle = { scope.toggle(store, h, date, dayLogs[h.uid]) },
                        onMeasure = { measuring = h },
                    )
                }
            }
        }
    }

    measuring?.let { h ->
        MeasureDialog(
            habit = h,
            log = dayLogs[h.uid],
            date = date,
            onDismiss = { measuring = null },
            onSave = { value, note ->
                scope.launch { store.saveLog(HabitLog(h.uid, date, done = h.isSuccess(HabitLog(h.uid, date, false, value)) == true, value = value, note = note)) }
                measuring = null
            },
            onClear = {
                scope.launch { store.clearLog(h.uid, date) }
                measuring = null
            },
        )
    }
}

/** Ja/Nein antippen: offen → geschafft → offen (ein „Nein“ wird auch wieder offen) */
fun CoroutineScope.toggle(store: Store, habit: Habit, date: LocalDate, log: HabitLog?) {
    launch {
        if (log != null) store.clearLog(habit.uid, date)
        else store.saveLog(HabitLog(habit.uid, date, done = true))
    }
}

@Composable
private fun DaySummary(done: Int, planned: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { if (planned == 0) 0f else done.toFloat() / planned },
                modifier = Modifier.size(56.dp),
                strokeWidth = 6.dp,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                strokeCap = StrokeCap.Round,
            )
            Text("$done/$planned", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        Column {
            Text(
                when {
                    done == planned -> "Alles geschafft 🎉"
                    done == 0 -> "Los geht's"
                    else -> "Noch ${planned - done} offen"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "$done von $planned geschafft",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
            )
        }
    }
}

/** Woche mit Fortschritt je Tag; Pfeile blättern wochenweise */
@Composable
private fun WeekStrip(
    date: LocalDate,
    habits: List<Habit>,
    logs: Map<String, Map<LocalDate, HabitLog>>,
    onDateChange: (LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    val monday = date.with(DayOfWeek.MONDAY)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onDateChange(date.minusWeeks(1)) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Woche zurück")
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
            (0L..6L).map { monday.plusDays(it) }.forEach { day ->
                val planned = habits.filter { it.isPlanned(day) }
                val done = planned.count { it.isSuccess(logs[it.uid]?.get(day)) == true }
                val selected = day == date
                val future = day.isAfter(today)
                Column(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                        .clickable(enabled = !future) { onDateChange(day) }
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        day.dayOfWeek.getDisplayName(TextStyle.SHORT, GERMAN).take(2),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { if (planned.isEmpty() || future) 0f else done.toFloat() / planned.size },
                            modifier = Modifier.size(34.dp),
                            strokeWidth = 3.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            strokeCap = StrokeCap.Round,
                        )
                        Text(
                            day.dayOfMonth.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal,
                            color = if (future) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
        IconButton(onClick = { onDateChange(minOf(today, date.plusWeeks(1))) }, enabled = monday.plusWeeks(1) <= today, modifier = Modifier.size(32.dp)) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Woche vor")
        }
    }
    Box(Modifier.height(4.dp))
}
