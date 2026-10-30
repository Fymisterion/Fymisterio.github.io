package de.gerry.dranbleiben

import android.app.Application
import de.gerry.dranbleiben.data.HabitDatabase
import de.gerry.dranbleiben.data.Store
import de.gerry.dranbleiben.reminders.ReminderScheduler
import de.gerry.dranbleiben.sync.Settings
import de.gerry.dranbleiben.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

class DranbleibenApp : Application() {

    lateinit var store: Store
        private set
    lateinit var settings: Settings
        private set
    lateinit var sync: SyncManager
        private set
    lateinit var reminders: ReminderScheduler
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        store = Store(HabitDatabase.create(this).records())
        settings = Settings(this)
        reminders = ReminderScheduler(this, store)
        ReminderScheduler.createChannel(this)
        sync = SyncManager(this, store, settings).also { it.start() }
        // Erinnerungen neu planen, wenn sich Gewohnheiten ändern – auf dem Handy oder über Claude
        scope.launch { store.habitChanges.debounce(500).collect { reminders.rescheduleAll() } }
        scope.launch { reminders.rescheduleAll() }
    }
}
