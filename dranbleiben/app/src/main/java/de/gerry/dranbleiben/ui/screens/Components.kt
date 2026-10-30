package de.gerry.dranbleiben.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.gerry.dranbleiben.data.Habit
import de.gerry.dranbleiben.data.HabitLog
import de.gerry.dranbleiben.data.HabitStats
import de.gerry.dranbleiben.data.fmt
import de.gerry.dranbleiben.data.toNumber
import de.gerry.dranbleiben.ui.theme.habitColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val GERMAN: Locale = Locale.GERMAN
private val dayFormat = DateTimeFormatter.ofPattern("EEEE, d. MMMM", GERMAN)

fun dayLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Heute"
        today.minusDays(1) -> "Gestern"
        else -> date.format(dayFormat)
    }
}

fun days(n: Int) = if (n == 1) "1 Tag" else "$n Tage"

/** Runder Kreis mit Emoji bzw. Anfangsbuchstabe in der Farbe der Gewohnheit */
@Composable
fun HabitAvatar(habit: Habit, size: Int = 44) {
    val color = habitColor(habit.color)
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        if (habit.icon.isNotBlank()) {
            Text(habit.icon, fontSize = (size * 0.48).sp)
        } else {
            Text(habit.name.take(1).uppercase(), color = color, fontWeight = FontWeight.Bold, fontSize = (size * 0.42).sp)
        }
    }
}

/**
 * Eine Gewohnheit an einem Tag: Ja/Nein mit rundem Haken zum Antippen, Messbar mit Wert-Knopf.
 * [onToggle] schaltet Ja/Nein zwischen offen und geschafft, [onMeasure] öffnet die Eingabe.
 */
@Composable
fun HabitDayRow(
    habit: Habit,
    log: HabitLog?,
    stats: HabitStats?,
    onToggle: () -> Unit,
    onMeasure: () -> Unit,
    modifier: Modifier = Modifier,
    onOpen: (() -> Unit)? = null,
) {
    val success = habit.isSuccess(log)
    val color = habitColor(habit.color)
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable { if (onOpen != null) onOpen() else if (habit.isMeasure) onMeasure() else onToggle() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HabitAvatar(habit)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(habit.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sub = buildList {
                habit.targetLabel()?.let { add(it) }
                if (log?.note?.isNotBlank() == true) add(log.note)
            }.joinToString(" · ")
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (stats != null && stats.current > 0) {
                    Icon(Icons.Outlined.LocalFireDepartment, null, Modifier.size(16.dp), tint = color)
                    Text(
                        " ${days(stats.current)}" + if (sub.isNotEmpty()) " · " else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = color,
                    )
                }
                if (sub.isNotEmpty()) {
                    Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (habit.isMeasure) {
            ValueButton(habit, log, color, onMeasure)
        } else {
            CheckButton(success, color, onToggle)
        }
    }
}

@Composable
fun CheckButton(success: Boolean?, color: Color, onClick: () -> Unit, size: Int = 40) {
    val background by animateColorAsState(if (success == true) color else Color.Transparent, label = "check")
    val outline = when (success) {
        true -> color
        false -> MaterialTheme.colorScheme.outline
        null -> color.copy(alpha = 0.5f)
    }
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(background)
            .border(2.dp, outline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when (success) {
            true -> Icon(Icons.Outlined.Check, "Geschafft", tint = Color.White)
            false -> Icon(Icons.Outlined.Close, "Nicht geschafft", tint = MaterialTheme.colorScheme.outline)
            null -> {}
        }
    }
}

@Composable
private fun ValueButton(habit: Habit, log: HabitLog?, color: Color, onClick: () -> Unit) {
    val value = log?.value
    val success = habit.isSuccess(log) == true
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (success) color else if (value != null) color.copy(alpha = 0.16f) else Color.Transparent)
            .border(2.dp, if (value == null) color.copy(alpha = 0.5f) else color, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            if (value == null) "+ ${habit.unit.ifBlank { "Wert" }}" else "${value.fmt()} ${habit.unit}".trim(),
            style = MaterialTheme.typography.labelLarge,
            color = if (success) Color.White else color,
        )
    }
}

/** Wert für eine messbare Gewohnheit eingeben; mit Schnell-Knöpfen für Zähl-Einheiten */
@Composable
fun MeasureDialog(
    habit: Habit,
    log: HabitLog?,
    date: LocalDate,
    onDismiss: () -> Unit,
    onSave: (Double, String) -> Unit,
    onClear: () -> Unit,
) {
    var text by remember { mutableStateOf(log?.value?.fmt().orEmpty()) }
    var note by remember { mutableStateOf(log?.note.orEmpty()) }
    val value = text.toNumber()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(listOf(habit.icon, habit.name).filter(String::isNotBlank).joinToString(" ")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    dayLabel(date) + (habit.targetLabel()?.let { " · Ziel: $it" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NumberField(text, { text = it }, "Wert", Modifier.fillMaxWidth(), suffix = habit.unit.ifBlank { null })
                val steps = quickSteps(habit)
                if (steps.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        steps.forEach { step ->
                            AssistChip(onClick = { text = ((value ?: 0.0) + step).coerceAtLeast(0.0).fmt() }, label = { Text("+${step.fmt()}") })
                        }
                    }
                }
                OutlinedTextField(note, { note = it }, label = { Text("Notiz (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { value?.let { onSave(it, note.trim()) } }, enabled = value != null) { Text("Speichern") } },
        dismissButton = {
            Row {
                if (log != null) TextButton(onClick = onClear) { Text("Entfernen") }
                TextButton(onClick = onDismiss) { Text("Abbrechen") }
            }
        },
    )
}

/** Sinnvolle „+“-Knöpfe je nach Einheit (Gewicht braucht keine) */
fun quickSteps(habit: Habit): List<Double> {
    val unit = habit.unit.lowercase()
    val target = habit.target ?: 0.0
    return when {
        unit in setOf("kg", "°c", "bpm") -> emptyList()
        unit.startsWith("min") -> listOf(5.0, 10.0, 15.0)
        unit.startsWith("std") || unit == "h" -> listOf(0.5, 1.0)
        unit == "schritte" || target >= 1000 -> listOf(1000.0, 2500.0, 5000.0)
        unit == "km" || unit == "l" -> listOf(0.5, 1.0, 5.0)
        else -> listOf(1.0, 2.0, 5.0)
    }
}

/** Eingabefeld für Zahlen (Komma oder Punkt) */
@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onValueChange(v.filter { it.isDigit() || it == ',' || it == '.' }.take(9)) },
        label = { Text(label) },
        suffix = suffix?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirmLabel: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = modifier.padding(top = 8.dp))
}

/** Leerer Zustand mit kurzer Erklärung */
@Composable
fun EmptyHint(title: String, text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        action?.invoke()
    }
}
