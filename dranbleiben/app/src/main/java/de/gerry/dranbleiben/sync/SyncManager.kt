package de.gerry.dranbleiben.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import de.gerry.dranbleiben.DranbleibenApp
import de.gerry.dranbleiben.data.Store
import de.gerry.dranbleiben.data.SyncRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Claude-Verbindung: gleicht Gewohnheiten und Einträge mit dem eigenen Server ab
 * (kopffrei-cloud, /api/habits/sync), auf dem Claude sie als Connector liest und ändert.
 * Abgleich kurz nach jeder Änderung, beim Öffnen der App und alle 15 Minuten.
 */
class SyncManager(
    private val context: Context,
    private val store: Store,
    private val settings: Settings,
) {
    data class Status(val running: Boolean, val lastSync: Long, val error: String?)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val _status = MutableStateFlow(Status(false, settings.lastSync, settings.lastError))
    val status: StateFlow<Status> = _status

    @OptIn(FlowPreview::class)
    fun start() {
        scope.launch { store.localChanges.debounce(2_000).collect { sync() } }
        if (settings.isConnected) schedulePeriodic()
    }

    fun requestSync() {
        scope.launch { sync() }
    }

    /** Neu verbinden: prüft Server und Schlüssel mit einem ersten Abgleich. */
    suspend fun connect(server: String, key: String): String? {
        settings.connect(server, key)
        store.markAllForUpload()
        val error = sync()
        if (error == null) schedulePeriodic() else settings.disconnect()
        _status.value = Status(false, settings.lastSync, error)
        return error
    }

    fun disconnect() {
        settings.disconnect()
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        _status.value = Status(false, 0, null)
    }

    /** Gibt null zurück, wenn alles geklappt hat, sonst eine Fehlermeldung. */
    suspend fun sync(): String? = mutex.withLock {
        val server = settings.serverUrl ?: return null
        val key = settings.key ?: return null
        _status.value = _status.value.copy(running = true)
        val error = try {
            var more: Boolean
            do {
                val pushed = store.pendingChanges()
                val response = post(
                    "$server/api/habits/sync",
                    key,
                    JSONObject()
                        .put("cursor", settings.cursor)
                        .put("tz", ZoneId.systemDefault().id)
                        .put("changes", JSONArray(pushed.map { it.toJson() })),
                )
                store.markPushed(pushed)
                val incoming = response.getJSONArray("changes").let { a ->
                    List(a.length()) { SyncRecord.fromJson(a.getJSONObject(it)) }
                }
                store.applyRemote(incoming)
                settings.cursor = response.getLong("cursor")
                more = response.optBoolean("more", false)
            } while (more)
            settings.lastSync = System.currentTimeMillis()
            null
        } catch (e: HttpError) {
            when (e.code) {
                401 -> "Der Schlüssel passt nicht zum Server."
                404 -> "Der Server kennt Dranbleiben noch nicht – bitte den Server aktualisieren."
                else -> "Server meldet Fehler ${e.code}."
            }
        } catch (e: IOException) {
            "Server nicht erreichbar – keine Internetverbindung?"
        } catch (e: Exception) {
            "Abgleich fehlgeschlagen: ${e.message}"
        }
        settings.lastError = error
        _status.value = Status(false, settings.lastSync, error)
        error
    }

    private class HttpError(val code: Int) : IOException("HTTP $code")

    private suspend fun post(url: String, key: String, body: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Authorization", "Bearer $key")
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode != 200) throw HttpError(conn.responseCode)
            JSONObject(conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
        } finally {
            conn.disconnect()
        }
    }

    private fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    companion object {
        private const val WORK_NAME = "claude-sync"
    }
}

/** Regelmäßiger Abgleich, damit Einträge von Claude auch ohne Öffnen der App ankommen. */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val error = (applicationContext as DranbleibenApp).sync.sync()
        return if (error == null) Result.success() else Result.retry()
    }
}
