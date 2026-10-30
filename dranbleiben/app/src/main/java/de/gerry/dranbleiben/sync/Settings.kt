package de.gerry.dranbleiben.sync

import android.content.Context

/** Einstellungen der Claude-Verbindung (Abgleich mit dem eigenen Server, siehe kopffrei-cloud). */
class Settings(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** z. B. https://kopffrei-cloud.name.workers.dev – null = nicht verbunden */
    val serverUrl: String? get() = prefs.getString(KEY_SERVER, null)
    val key: String? get() = prefs.getString(KEY_KEY, null)
    val isConnected: Boolean get() = serverUrl != null && key != null

    /** Adresse, die in Claude als Connector eingetragen wird (dieselbe wie für Kopffrei). */
    val connectorUrl: String? get() = if (isConnected) "$serverUrl/mcp/$key" else null

    fun connect(server: String, key: String) = prefs.edit()
        .putString(KEY_SERVER, server.trimEnd('/'))
        .putString(KEY_KEY, key)
        .putLong(KEY_CURSOR, 0)
        .remove(KEY_LAST_SYNC)
        .remove(KEY_LAST_ERROR)
        .apply()

    fun disconnect() = prefs.edit()
        .remove(KEY_SERVER).remove(KEY_KEY).remove(KEY_CURSOR).remove(KEY_LAST_SYNC).remove(KEY_LAST_ERROR)
        .apply()

    var cursor: Long
        get() = prefs.getLong(KEY_CURSOR, 0)
        set(value) = prefs.edit().putLong(KEY_CURSOR, value).apply()

    var lastSync: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC, value).apply()

    var lastError: String?
        get() = prefs.getString(KEY_LAST_ERROR, null)
        set(value) = prefs.edit().putString(KEY_LAST_ERROR, value).apply()

    companion object {
        /** https, nur zum Testen auch http auf dem PC (Emulator: 10.0.2.2) – siehe network_security_config.xml */
        fun isAllowedServer(url: String): Boolean =
            url.startsWith("https://") || Regex("""^http://(10\.0\.2\.2|localhost|127\.0\.0\.1)(:\d+)?/?$""").matches(url)

        private const val KEY_SERVER = "cloud_server"
        private const val KEY_KEY = "cloud_key"
        private const val KEY_CURSOR = "cloud_cursor"
        private const val KEY_LAST_SYNC = "cloud_last_sync"
        private const val KEY_LAST_ERROR = "cloud_last_error"
    }
}
