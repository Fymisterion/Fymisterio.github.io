package de.gerry.dranbleiben.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import de.gerry.dranbleiben.data.Habit
import de.gerry.dranbleiben.data.Store
import de.gerry.dranbleiben.data.hhmm
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Ein Wecker je Erinnerungszeit (nicht je Gewohnheit): Zur Zeit fragt eine Benachrichtigung alle Gewohnheiten ab,
 * die dann noch offen sind. Nach jedem Wecker und jeder Änderung wird neu geplant.
 */
class ReminderScheduler(private val context: Context, private val store: Store) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    suspend fun rescheduleAll() {
        val habits = store.habitsNow().filter { !it.archived }
        val times = habits.flatMap { it.reminders }.toSet()

        prefs.getStringSet(KEY_SCHEDULED, emptySet()).orEmpty()
            .mapNotNull { runCatching { LocalTime.parse(it) }.getOrNull() }
            .filter { it !in times }
            .forEach { alarmManager.cancel(pendingIntent(it)) }

        times.forEach { time -> nextTrigger(time, habits)?.let { schedule(time, it) } }
        prefs.edit().putStringSet(KEY_SCHEDULED, times.map { it.hhmm() }.toSet()).apply()
    }

    /** Nächster Zeitpunkt, an dem mindestens eine Gewohnheit mit dieser Erinnerung geplant ist */
    private fun nextTrigger(time: LocalTime, habits: List<Habit>): Long? {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now()
        val withTime = habits.filter { time in it.reminders }
        for (offset in 0L..7L) {
            val day = LocalDate.now().plusDays(offset)
            val at = day.atTime(time)
            if (!at.isAfter(now)) continue
            if (withTime.any { it.isPlanned(day) }) return at.atZone(zone).toInstant().toEpochMilli()
        }
        return null
    }

    private fun schedule(time: LocalTime, triggerAt: Long) {
        val pending = pendingIntent(time)
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            // Ohne Erlaubnis für exakte Wecker: Android liefert die Erinnerung leicht verzögert.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    private fun pendingIntent(time: LocalTime): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode(time),
            Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_SHOW
                putExtra(ReminderReceiver.EXTRA_TIME, time.hhmm())
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val CHANNEL_ID = "reminders"
        private const val KEY_SCHEDULED = "scheduled"

        /** Minute des Tages – zugleich Nummer der Benachrichtigung */
        fun requestCode(time: LocalTime) = time.hour * 60 + time.minute

        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Erinnerungen",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Fragt zur eingestellten Zeit deine Gewohnheiten ab" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
