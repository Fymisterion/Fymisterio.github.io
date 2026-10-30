package de.gerry.dranbleiben.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Die Datensätze von Dranbleiben. Gespeichert und abgeglichen werden sie als JSON (gleiches Format auf dem Server,
 * siehe kopffrei-cloud/src/habits.js):
 *
 * habit: { name, icon, type ("check" | "measure"), unit, target, targetMode ("min" | "max"),
 *          days [1–7, Mo = 1; leer = jeden Tag], reminders ["HH:MM"], color (0–7), notes, sortOrder, archived, createdAt }
 * log:   { habitUid, date, done, value, note }   (uid "log-<habitUid>-<YYYY-MM-DD>", ein Eintrag je Gewohnheit und Tag)
 */
object Kind {
    const val HABIT = "habit"
    const val LOG = "log"
    val ALL = setOf(HABIT, LOG)
}

enum class HabitType(val key: String, val label: String) {
    CHECK("check", "Ja/Nein"),
    MEASURE("measure", "Messbar");

    companion object {
        fun of(key: String?) = entries.firstOrNull { it.key == key } ?: CHECK
    }
}

enum class TargetMode(val key: String, val label: String) {
    MIN("min", "mindestens"),
    MAX("max", "höchstens");

    companion object {
        fun of(key: String?) = entries.firstOrNull { it.key == key } ?: MIN
    }
}

private val TIME = DateTimeFormatter.ofPattern("HH:mm")
private val WEEKDAYS = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")

fun LocalTime.hhmm(): String = format(TIME)

data class Habit(
    val uid: String,
    val name: String,
    val icon: String = "",
    val type: HabitType = HabitType.CHECK,
    val unit: String = "",
    val target: Double? = null,
    val targetMode: TargetMode = TargetMode.MIN,
    /** 1 = Montag … 7 = Sonntag; leer = jeden Tag */
    val days: Set<Int> = emptySet(),
    val reminders: List<LocalTime> = emptyList(),
    val color: Int = 0,
    val notes: String = "",
    val sortOrder: Int = 0,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val isMeasure get() = type == HabitType.MEASURE

    fun isPlanned(date: LocalDate) = days.isEmpty() || date.dayOfWeek.value in days

    /** null = nichts eingetragen, sonst geschafft oder nicht */
    fun isSuccess(log: HabitLog?): Boolean? {
        if (log == null) return null
        if (!isMeasure) return log.done
        val v = log.value ?: return null
        val t = target ?: return true
        return if (targetMode == TargetMode.MAX) v <= t else v >= t
    }

    val createdDate: LocalDate get() = Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()

    fun targetLabel(): String? = target?.let { "${targetMode.label} ${it.fmt()}${if (unit.isBlank()) "" else " $unit"}" }

    fun scheduleLabel(): String = when {
        days.isEmpty() -> "Täglich"
        days == setOf(1, 2, 3, 4, 5) -> "Werktags"
        days == setOf(6, 7) -> "Am Wochenende"
        else -> days.sorted().joinToString(", ") { WEEKDAYS[it - 1] }
    }

    /** Frage für die Abfrage */
    fun question(): String = if (isMeasure) "Wie viel heute?" else "Heute geschafft?"

    fun toJson(): JSONObject = JSONObject()
        .put("name", name).put("icon", icon).put("type", type.key).put("unit", unit)
        .put("target", target ?: JSONObject.NULL).put("targetMode", targetMode.key)
        .put("days", JSONArray(days.sorted()))
        .put("reminders", JSONArray(reminders.sorted().map { it.hhmm() }))
        .put("color", color).put("notes", notes).put("sortOrder", sortOrder).put("archived", archived)
        .put("createdAt", createdAt)

    companion object {
        fun from(uid: String, o: JSONObject) = Habit(
            uid = uid,
            name = o.optString("name"),
            icon = o.optString("icon"),
            type = HabitType.of(o.optString("type")),
            unit = o.optString("unit"),
            target = o.optDoubleOrNull("target"),
            targetMode = TargetMode.of(o.optString("targetMode")),
            days = o.optJSONArray("days")?.let { a -> List(a.length()) { a.optInt(it) }.filter { it in 1..7 }.toSet() }.orEmpty(),
            reminders = o.optJSONArray("reminders")?.let { a ->
                List(a.length()) { runCatching { LocalTime.parse(a.optString(it), TIME) }.getOrNull() }.filterNotNull()
            }.orEmpty(),
            color = o.optInt("color", 0).coerceIn(0, 7),
            notes = o.optString("notes"),
            sortOrder = o.optInt("sortOrder", 0),
            archived = o.optBoolean("archived"),
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
        )
    }
}

data class HabitLog(
    val habitUid: String,
    val date: LocalDate,
    val done: Boolean,
    val value: Double? = null,
    val note: String = "",
) {
    val uid get() = uid(habitUid, date)

    fun toJson(): JSONObject = JSONObject()
        .put("habitUid", habitUid).put("date", date.toString()).put("done", done)
        .put("value", value ?: JSONObject.NULL).put("note", note)

    companion object {
        fun uid(habitUid: String, date: LocalDate) = "log-$habitUid-$date"

        fun from(o: JSONObject) = HabitLog(
            habitUid = o.optString("habitUid"),
            date = LocalDate.parse(o.optString("date")),
            done = o.optBoolean("done"),
            value = o.optDoubleOrNull("value"),
            note = o.optString("note"),
        )
    }
}

/** Serien und Quote – gleiche Regeln wie auf dem Server (habits.js) */
data class HabitStats(val current: Int, val best: Int, val planned30: Int, val done30: Int) {
    val rate30: Int? get() = if (planned30 == 0) null else Math.round(done30 * 100f / planned30)

    companion object {
        fun of(habit: Habit, logs: Map<LocalDate, HabitLog>, today: LocalDate = LocalDate.now()): HabitStats {
            val start = listOfNotNull(habit.createdDate, logs.keys.minOrNull()).min()

            var current = 0
            var d = today
            if (habit.isPlanned(d) && habit.isSuccess(logs[d]) != true) d = d.minusDays(1)
            while (!d.isBefore(start)) {
                if (habit.isPlanned(d)) {
                    if (habit.isSuccess(logs[d]) == true) current++ else break
                }
                d = d.minusDays(1)
            }

            var best = 0
            var run = 0
            logs.keys.minOrNull()?.let { first ->
                var day = first
                while (!day.isAfter(today)) {
                    if (habit.isPlanned(day)) {
                        if (habit.isSuccess(logs[day]) == true) best = maxOf(best, ++run) else if (day != today) run = 0
                    }
                    day = day.plusDays(1)
                }
            }

            var planned = 0
            var done = 0
            var day = maxOf(today.minusDays(29), start)
            while (!day.isAfter(today)) {
                if (habit.isPlanned(day)) {
                    planned++
                    if (habit.isSuccess(logs[day]) == true) done++
                }
                day = day.plusDays(1)
            }
            return HabitStats(current, maxOf(best, current), planned, done)
        }
    }
}

/** Farben der Gewohnheiten (Index 0–7) */
val HABIT_COLORS = listOf(0xFFB4235A, 0xFF4F46E5, 0xFF0E7C86, 0xFF3F7A2E, 0xFFC27A12, 0xFFC2410C, 0xFF7C3AED, 0xFF475569)

/** Vorlagen beim Anlegen */
val TEMPLATES = listOf(
    Habit("", "Wasser trinken", "💧", HabitType.MEASURE, "Gläser", 8.0, color = 2),
    Habit("", "Meditieren", "🧘", HabitType.CHECK, color = 6),
    Habit("", "Lesen", "📖", HabitType.MEASURE, "Min.", 20.0, color = 4),
    Habit("", "Gewicht", "⚖️", HabitType.MEASURE, "kg", color = 7),
    Habit("", "Schritte", "🚶", HabitType.MEASURE, "Schritte", 8000.0, color = 3),
    Habit("", "Schlafen", "😴", HabitType.MEASURE, "Std.", 7.5, color = 1),
    Habit("", "Training", "💪", HabitType.CHECK, days = setOf(1, 3, 5), color = 5),
    Habit("", "Kein Alkohol", "🚫", HabitType.CHECK, color = 0),
)

val UNIT_SUGGESTIONS = listOf("kg", "Min.", "Std.", "km", "Schritte", "Seiten", "Gläser", "l", "Mal")

fun JSONObject.optDoubleOrNull(key: String): Double? = if (isNull(key) || !has(key)) null else optDouble(key).takeIf { !it.isNaN() }

fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key) || !has(key)) null else optString(key)

/** 81,4 · 20 · 7,5 */
fun Double.fmt(): String {
    val rounded = Math.round(this * 100) / 100.0
    return if (rounded == Math.floor(rounded) && Math.abs(rounded) < 1e12) rounded.toLong().toString()
    else String.format(Locale.GERMAN, "%.2f", rounded).trimEnd('0').trimEnd(',')
}

fun String.toNumber(): Double? = replace(',', '.').trim().toDoubleOrNull()
