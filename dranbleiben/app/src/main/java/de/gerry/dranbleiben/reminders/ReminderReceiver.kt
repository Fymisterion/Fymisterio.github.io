package de.gerry.dranbleiben.reminders

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import de.gerry.dranbleiben.DranbleibenApp
import de.gerry.dranbleiben.MainActivity
import de.gerry.dranbleiben.R
import de.gerry.dranbleiben.data.HabitLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/**
 * Zur Erinnerungszeit: Benachrichtigung mit den noch offenen Gewohnheiten. Antippen öffnet die Abfrage.
 * Ist nur eine Ja/Nein-Gewohnheit offen, lässt sie sich direkt mit „Ja“ oder „Heute nicht“ beantworten.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as DranbleibenApp
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_SHOW -> show(context, app, intent.getStringExtra(EXTRA_TIME))
                    ACTION_ANSWER -> answer(context, app, intent)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun show(context: Context, app: DranbleibenApp, time: String?) {
        app.reminders.rescheduleAll()
        val at = runCatching { LocalTime.parse(time) }.getOrNull() ?: return
        val today = LocalDate.now()
        val logs = app.store.logsOn(today)
        val habits = app.store.habitsNow().filter { !it.archived && it.isPlanned(today) }
        val open = habits.filter { at in it.reminders && logs[it.uid] == null }
        if (open.isEmpty()) return
        val id = ReminderScheduler.requestCode(at)

        val openIntent = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_CHECK_IN, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val names = open.map { listOf(it.icon, it.name).filter(String::isNotBlank).joinToString(" ") }
        val builder = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openIntent)
            .setAutoCancel(true)

        val single = open.singleOrNull()
        if (single != null) {
            builder.setContentTitle(names.first()).setContentText(single.question())
            if (!single.isMeasure) {
                builder.addAction(0, "Ja", answerIntent(context, id, single.uid, true))
                builder.addAction(0, "Heute nicht", answerIntent(context, id, single.uid, false))
            } else {
                builder.addAction(0, "Eintragen", openIntent)
            }
        } else {
            builder.setContentTitle("Zeit für deine Gewohnheiten")
                .setContentText(names.joinToString(" · "))
                .setStyle(NotificationCompat.InboxStyle().also { s -> names.forEach { s.addLine(it) } })
                .addAction(0, "Abfrage starten", openIntent)
        }
        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (_: SecurityException) {
            // Benachrichtigungen nicht erlaubt – in „Mehr“ wird darauf hingewiesen
        }
    }

    private suspend fun answer(context: Context, app: DranbleibenApp, intent: Intent) {
        val uid = intent.getStringExtra(EXTRA_HABIT) ?: return
        app.store.saveLog(HabitLog(uid, LocalDate.now(), done = intent.getBooleanExtra(EXTRA_DONE, true)))
        NotificationManagerCompat.from(context).cancel(intent.getIntExtra(EXTRA_NOTIFICATION, 0))
    }

    private fun answerIntent(context: Context, id: Int, habitUid: String, done: Boolean): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            id * 2 + if (done) 1 else 0,
            Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_ANSWER
                putExtra(EXTRA_HABIT, habitUid)
                putExtra(EXTRA_DONE, done)
                putExtra(EXTRA_NOTIFICATION, id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val ACTION_SHOW = "de.gerry.dranbleiben.action.SHOW_REMINDER"
        const val ACTION_ANSWER = "de.gerry.dranbleiben.action.ANSWER"
        const val EXTRA_TIME = "time"
        const val EXTRA_HABIT = "habit"
        const val EXTRA_DONE = "done"
        const val EXTRA_NOTIFICATION = "notification"
    }
}
