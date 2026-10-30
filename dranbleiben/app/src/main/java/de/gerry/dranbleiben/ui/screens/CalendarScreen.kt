package de.gerry.dranbleiben.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.gerry.dranbleiben.data.Habit
import de.gerry.dranbleiben.data.HabitLog
import de.gerry.dranbleiben.data.HabitStats
import de.gerry.dranbleiben.data.Store
import de.gerry.dranbleiben.data.fmt
import de.gerry.dranbleiben.ui.theme.habitColor
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", GERMAN)
private val WEEKDAY_HEADER = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")

/** Monatskalender: alle Gewohnheiten zusammen (Füllung = Anteil geschafft) oder eine einzelne, dazu Serien und Verlauf. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(store: Store, contentPadding: PaddingValues) {
    val habits by store.habits.collectAsState(initial = emptyList())
    val logs by store.logs.collectAsState(initial = emptyMap())
    val active = habits.filter { !it.archived }
    var month by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var selectedUid by rememberSaveable { mutableStateOf<String?>(null) }
    var openDay by remember { mutableStateOf<LocalDate?>(null) }
    val ym = YearMonth.parse(month)
    val selected = active.firstOrNull { it.uid == selectedUid }

    Scaffold(
        modifier = Modifier.padding(contentPadding),
        topBar = { TopAppBar(title = { Text("Kalender") }) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = selected == null, onClick = { selectedUid = null }, label = { Text("Alle") })
                active.forEach { h ->
                    FilterChip(
                        selected = selected?.uid == h.uid,
                        onClick = { selectedUid = h.uid },
                        label = { Text(listOf(h.icon, h.name).filter(String::isNotBlank).joinToString(" ")) },
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { month = ym.minusMonths(1).toString() }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Monat zurück") }
                Text(
                    ym.format(monthFormat).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { month = ym.plusMonths(1).toString() }, enabled = ym < YearMonth.now()) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Monat vor")
                }
            }

            MonthGrid(ym, active, logs, selected, onDay = { openDay = it })
            Legend(selected)

            if (selected != null) {
                HabitStatsCard(selected, logs[selected.uid].orEmpty())
            } else if (active.isNotEmpty()) {
                SectionTitle("Serien")
                active.forEach { h -> StatsRow(h, HabitStats.of(h, logs[h.uid].orEmpty())) { selectedUid = h.uid } }
            } else {
                EmptyHint("Noch nichts zu sehen", "Sobald du Gewohnheiten anlegst und abhakst, füllt sich der Kalender.")
            }
            Box(Modifier.height(24.dp))
        }
    }

    openDay?.let { day ->
        DaySheet(store, day, if (selected != null) listOf(selected) else active, logs, onDismiss = { openDay = null })
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    habits: List<Habit>,
    logs: Map<String, Map<LocalDate, HabitLog>>,
    selected: Habit?,
    onDay: (LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    val first = month.atDay(1)
    val lead = first.dayOfWeek.value - 1
    val cells = (List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }).chunked(7)
    val accent = selected?.let { habitColor(it.color) } ?: MaterialTheme.colorScheme.primary

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            WEEKDAY_HEADER.forEach {
                Text(
                    it, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        cells.forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (0 until 7).forEach { i ->
                    val day = week.getOrNull(i)
                    Box(Modifier.weight(1f).aspectRatio(1f)) {
                        if (day != null) {
                            DayCell(day, today, habits, logs, selected, accent, onDay)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    today: LocalDate,
    habits: List<Habit>,
    logs: Map<String, Map<LocalDate, HabitLog>>,
    selected: Habit?,
    accent: Color,
    onDay: (LocalDate) -> Unit,
) {
    val future = day.isAfter(today)
    val relevant = (if (selected != null) listOf(selected) else habits).filter { day >= minOf(it.createdDate, logs[it.uid]?.keys?.minOrNull() ?: it.createdDate) }
    val planned = relevant.filter { it.isPlanned(day) }
    val done = planned.count { it.isSuccess(logs[it.uid]?.get(day)) == true }
    val failed = selected != null && planned.isNotEmpty() && logs[selected.uid]?.get(day) != null && done == 0
    val fraction = if (planned.isEmpty()) 0f else done.toFloat() / planned.size
    val shape = RoundedCornerShape(12.dp)
    val surface = MaterialTheme.colorScheme.surfaceContainerLow

    Box(
        Modifier
            .fillMaxSize()
            .clip(shape)
            .background(
                when {
                    future || planned.isEmpty() -> Color.Transparent
                    fraction > 0f -> accent.copy(alpha = 0.25f + 0.75f * fraction)
                    else -> surface
                },
            )
            .then(if (day == today) Modifier.border(2.dp, accent, shape) else Modifier)
            .clickable(enabled = !future && relevant.isNotEmpty()) { onDay(day) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            day.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal,
            color = when {
                future -> MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                fraction >= 0.6f -> Color.White
                planned.isEmpty() -> MaterialTheme.colorScheme.outline
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
        if (failed) {
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp).size(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outline))
        }
    }
}

@Composable
private fun Legend(selected: Habit?) {
    Text(
        if (selected == null) "Je kräftiger die Farbe, desto mehr war an dem Tag geschafft. Tippe auf einen Tag zum Nachtragen."
        else "Farbig = geschafft · Punkt = nicht geschafft · blass = nicht geplant. Tippe auf einen Tag zum Nachtragen.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun StatsRow(habit: Habit, stats: HabitStats, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HabitAvatar(habit, size = 36)
        Text(habit.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text("🔥 ${days(stats.current)}", style = MaterialTheme.typography.labelLarge)
            Text(
                stats.rate30?.let { "$it % in 30 Tagen" } ?: "–",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HabitStatsCard(habit: Habit, logs: Map<LocalDate, HabitLog>) {
    val stats = HabitStats.of(habit, logs)
    val color = habitColor(habit.color)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("Serie", days(stats.current), color, Modifier.weight(1f))
        StatTile("Beste Serie", days(stats.best), color, Modifier.weight(1f))
        StatTile("30 Tage", stats.rate30?.let { "$it %" } ?: "–", color, Modifier.weight(1f))
    }
    Text(
        listOfNotNull(habit.scheduleLabel(), habit.targetLabel()?.let { "Ziel $it" }).joinToString(" · "),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (habit.isMeasure) {
        val points = logs.values.filter { it.value != null }.sortedBy { it.date }.takeLast(30)
        if (points.size >= 2) {
            SectionTitle("Verlauf")
            ValueChart(points.map { it.value!! }, habit.target, color)
            val values = points.map { it.value!! }
            Text(
                "Letzte ${points.size} Einträge: Schnitt ${values.average().fmt()} · min ${values.min().fmt()} · max ${values.max().fmt()} ${habit.unit}".trim(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Einfache Linie der letzten Werte, Ziel gestrichelt */
@Composable
private fun ValueChart(values: List<Double>, target: Double?, color: Color) {
    val grid = MaterialTheme.colorScheme.outlineVariant
    val all = values + listOfNotNull(target)
    val min = all.min()
    val max = all.max().let { if (it == min) it + 1 else it }
    Canvas(Modifier.fillMaxWidth().height(140.dp).padding(vertical = 8.dp)) {
        fun y(v: Double) = (size.height * (1 - (v - min) / (max - min))).toFloat()
        val step = size.width / (values.size - 1)
        target?.let {
            drawLine(
                grid, Offset(0f, y(it)), Offset(size.width, y(it)), strokeWidth = 2f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
            )
        }
        val path = Path()
        values.forEachIndexed { i, v -> if (i == 0) path.moveTo(0f, y(v)) else path.lineTo(i * step, y(v)) }
        drawPath(path, color, style = Stroke(width = 6f, cap = StrokeCap.Round))
        values.forEachIndexed { i, v -> drawCircle(color, 7f, Offset(i * step, y(v))) }
    }
}

/** Tag nachtragen: alle (bzw. die gewählte) Gewohnheiten dieses Tages */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaySheet(
    store: Store,
    day: LocalDate,
    habits: List<Habit>,
    logs: Map<String, Map<LocalDate, HabitLog>>,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var measuring by remember { mutableStateOf<Habit?>(null) }
    val list = habits.sortedByDescending { it.isPlanned(day) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(horizontal = 16.dp).navigationBarsPadding().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(dayLabel(day), style = MaterialTheme.typography.titleLarge)
            list.forEach { h ->
                val log = logs[h.uid]?.get(day)
                Column {
                    if (!h.isPlanned(day)) {
                        Text("nicht geplant", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
                    }
                    HabitDayRow(
                        habit = h,
                        log = log,
                        stats = null,
                        onToggle = { scope.toggle(store, h, day, log) },
                        onMeasure = { measuring = h },
                    )
                }
            }
        }
    }
    measuring?.let { h ->
        val log = logs[h.uid]?.get(day)
        MeasureDialog(
            habit = h, log = log, date = day,
            onDismiss = { measuring = null },
            onSave = { value, note ->
                scope.launch { store.saveLog(HabitLog(h.uid, day, done = h.isSuccess(HabitLog(h.uid, day, false, value)) == true, value = value, note = note)) }
                measuring = null
            },
            onClear = {
                scope.launch { store.clearLog(h.uid, day) }
                measuring = null
            },
        )
    }
}
