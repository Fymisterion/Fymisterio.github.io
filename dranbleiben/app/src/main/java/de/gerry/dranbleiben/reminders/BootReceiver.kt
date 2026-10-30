package de.gerry.dranbleiben.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.gerry.dranbleiben.DranbleibenApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Nach Neustart, Update oder geänderter Uhrzeit/Zeitzone: Erinnerungen neu planen. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as DranbleibenApp
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.reminders.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
