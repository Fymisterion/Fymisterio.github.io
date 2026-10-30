package de.gerry.dranbleiben.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.gerry.dranbleiben.data.HABIT_COLORS
import de.gerry.dranbleiben.data.Habit
import de.gerry.dranbleiben.data.HabitType
import de.gerry.dranbleiben.data.Store
import de.gerry.dranbleiben.data.TEMPLATES
import de.gerry.dranbleiben.data.TargetMode
import de.gerry.dranbleiben.data.UNIT_SUGGESTIONS
import de.gerry.dranbleiben.data.fmt
import de.gerry.dranbleiben.data.hhmm
import de.gerry.dranbleiben.data.toNumber
import de.gerry.dranbleiben.ui.theme.habitColor
import kotlinx.coroutines.launch
import java.time.LocalTime

/** Alle Gewohnheiten verwalten: Reihenfolge, Archiv, Anlegen */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(store: Store, contentPadding: PaddingValues, onOpen: (String) -> Unit, onNew: () -> Unit) {
    val habits by store.habits.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showArchived by remember { mutableStateOf(false) }
    val active = habits.filter { !it.archived }
    val archived = habits.filter { it.archived }

    fun move(h: Habit, by: Int) {
        val list = active.toMutableList()
        val i = list.indexOf(h)
        val j = i + by
        if (i < 0 || j !in list.indices) return
        list.add(j, list.removeAt(i))
        scope.launch { list.forEachIndexed { index, item -> if (item.sortOrder != index) store.saveHabit(item.copy(sortOrder = index)) } }
    }

    Scaffold(
        modifier = Modifier.padding(contentPadding),
        topBar = { TopAppBar(title = { Text("Gewohnheiten") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onNew, icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("Neu") })
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (habits.isEmpty()) {
                item { EmptyHint("Noch leer", "Tippe auf „Neu“ und wähle eine Vorlage oder leg eine eigene Gewohnheit an.") }
            }
            items(active, key = { it.uid }) { h ->
                ManageRow(h, onClick = { onOpen(h.uid) }) {
                    IconButton(onClick = { move(h, -1) }, enabled = active.first() != h) { Icon(Icons.Outlined.KeyboardArrowUp, "Nach oben") }
                    IconButton(onClick = { move(h, 1) }, enabled = active.last() != h) { Icon(Icons.Outlined.KeyboardArrowDown, "Nach unten") }
                }
            }
            if (archived.isNotEmpty()) {
                item {
                    TextButton(onClick = { showArchived = !showArchived }, modifier = Modifier.padding(top = 8.dp)) {
                        Text(if (showArchived) "Archiv ausblenden" else "Archiv (${archived.size})")
                    }
                }
                if (showArchived) {
                    items(archived, key = { it.uid }) { h -> ManageRow(h, onClick = { onOpen(h.uid) }, muted = true) {} }
                }
            }
        }
    }
}

@Composable
private fun ManageRow(habit: Habit, onClick: () -> Unit, muted: Boolean = false, actions: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HabitAvatar(habit, size = 40)
        Column(Modifier.weight(1f)) {
            Text(
                habit.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                listOfNotNull(
                    habit.type.label,
                    habit.scheduleLabel(),
                    habit.targetLabel(),
                    habit.reminders.takeIf { it.isNotEmpty() }?.joinToString(", ") { "⏰ ${it.hhmm()}" },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
        actions()
    }
}

/** Anlegen und Bearbeiten. [uid] null = neu (mit Vorlagen). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitEditScreen(store: Store, uid: String?, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val existing by remember(uid) { uid?.let { store.habit(it) } ?: kotlinx.coroutines.flow.flowOf(null) }.collectAsState(initial = null)
    var loaded by remember { mutableStateOf(uid == null) }

    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(HabitType.CHECK) }
    var unit by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(TargetMode.MIN) }
    var days by remember { mutableStateOf(emptySet<Int>()) }
    var reminders by remember { mutableStateOf(emptyList<LocalTime>()) }
    var color by remember { mutableStateOf(0) }
    var notes by remember { mutableStateOf("") }
    var pickTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun fill(h: Habit) {
        name = h.name; icon = h.icon; type = h.type; unit = h.unit; target = h.target?.fmt().orEmpty()
        mode = h.targetMode; days = h.days; reminders = h.reminders; color = h.color; notes = h.notes
    }
    LaunchedEffect(existing) {
        if (!loaded) existing?.let { fill(it); loaded = true }
    }

    fun build(base: Habit) = base.copy(
        name = name.trim(), icon = icon.trim(), type = type,
        unit = if (type == HabitType.MEASURE) unit.trim() else "",
        target = if (type == HabitType.MEASURE) target.toNumber() else null,
        targetMode = mode, days = if (days.size == 7) emptySet() else days, reminders = reminders.sorted(),
        color = color, notes = notes.trim(),
    )

    fun save() = scope.launch {
        val base = existing ?: Habit(Store.newUid(), "", sortOrder = store.nextSortOrder())
        store.saveHabit(build(base))
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uid == null) "Neue Gewohnheit" else "Bearbeiten") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurück") } },
                actions = {
                    TextButton(onClick = { save() }, enabled = loaded && name.isNotBlank()) {
                        Icon(Icons.Outlined.Check, null)
                        Text(" Speichern")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (uid == null) {
                Text("Vorlagen", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TEMPLATES.forEach { t -> SuggestionChip(onClick = { fill(t) }, label = { Text("${t.icon} ${t.name}") }) }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    icon, { icon = it.take(4) }, label = { Text("Emoji") }, singleLine = true,
                    modifier = Modifier.width(88.dp), placeholder = { Text("🙂") },
                )
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.weight(1f))
            }

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                HabitType.entries.forEachIndexed { i, t ->
                    SegmentedButton(
                        selected = type == t,
                        onClick = { type = t },
                        shape = SegmentedButtonDefaults.itemShape(i, HabitType.entries.size),
                    ) { Text(t.label) }
                }
            }
            Text(
                if (type == HabitType.CHECK) "Einfach abhaken: geschafft oder nicht." else "Mit Zahl und Einheit, z. B. Gewicht, Minuten oder Schritte.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (type == HabitType.MEASURE) {
                OutlinedTextField(unit, { unit = it.take(16) }, label = { Text("Einheit") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UNIT_SUGGESTIONS.forEach { u -> FilterChip(selected = unit == u, onClick = { unit = u }, label = { Text(u) }) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                        TargetMode.entries.forEachIndexed { i, m ->
                            SegmentedButton(selected = mode == m, onClick = { mode = m }, shape = SegmentedButtonDefaults.itemShape(i, 2)) { Text(m.label) }
                        }
                    }
                    NumberField(target, { target = it }, "Tagesziel", Modifier.weight(1f), suffix = unit.ifBlank { null })
                }
                Text(
                    "Ohne Tagesziel wird der Wert nur erfasst (z. B. Gewicht) – jeder Eintrag zählt dann als geschafft.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionTitle("An welchen Tagen?")
            val all = days.isEmpty() || days.size == 7
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So").forEachIndexed { i, label ->
                    val d = i + 1
                    FilterChip(
                        selected = all || d in days,
                        onClick = {
                            val current = if (all) (1..7).toSet() else days
                            val next = if (d in current) current - d else current + d
                            days = if (next.isEmpty() || next.size == 7) emptySet() else next
                        },
                        label = { Text(label) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { days = emptySet() }, label = { Text("Täglich") })
                AssistChip(onClick = { days = setOf(1, 2, 3, 4, 5) }, label = { Text("Werktags") })
                AssistChip(onClick = { days = setOf(6, 7) }, label = { Text("Wochenende") })
            }

            SectionTitle("Erinnerungen")
            Text(
                "Zur eingestellten Zeit fragt dich Dranbleiben per Benachrichtigung ab – aber nur, wenn noch nichts eingetragen ist.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                reminders.sorted().forEach { t ->
                    InputChip(
                        selected = false,
                        onClick = { reminders = reminders - t },
                        label = { Text(t.hhmm()) },
                        leadingIcon = { Icon(Icons.Outlined.NotificationsNone, null, Modifier.size(18.dp)) },
                        trailingIcon = { Icon(Icons.Outlined.Close, "Entfernen", Modifier.size(18.dp)) },
                    )
                }
                AssistChip(onClick = { pickTime = true }, label = { Text("Erinnerung") }, leadingIcon = { Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)) })
            }

            SectionTitle("Farbe")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HABIT_COLORS.indices.forEach { i ->
                    val c = habitColor(i)
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(3.dp, if (i == color) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                            .clickable { color = i },
                    )
                }
            }

            OutlinedTextField(
                notes, { notes = it }, label = { Text("Notiz – warum ist dir das wichtig? (optional)") },
                modifier = Modifier.fillMaxWidth(), minLines = 2,
            )

            existing?.let { h ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedButton(onClick = {
                        scope.launch { store.saveHabit(h.copy(archived = !h.archived)) }
                        onBack()
                    }) {
                        Icon(if (h.archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive, null)
                        Text(if (h.archived) "  Zurückholen" else "  Archivieren")
                    }
                    TextButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error)
                        Text("  Löschen", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Box(Modifier.height(32.dp))
        }
    }

    if (pickTime) {
        val state = rememberTimePickerState(initialHour = 20, initialMinute = 0, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            title = { Text("Erinnerung um") },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton(onClick = {
                    reminders = (reminders + LocalTime.of(state.hour, state.minute)).distinct()
                    pickTime = false
                }) { Text("Hinzufügen") }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text("Abbrechen") } },
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Gewohnheit löschen?",
            text = "„$name“ und der ganze Verlauf werden gelöscht. Archivieren blendet sie nur aus.",
            confirmLabel = "Löschen",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                uid?.let { scope.launch { store.deleteHabit(it); onBack() } }
            },
        )
    }
}
