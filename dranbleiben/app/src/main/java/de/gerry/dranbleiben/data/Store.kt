package de.gerry.dranbleiben.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

/** Alles, was die Oberfläche liest und schreibt. Jede Änderung wird für den Abgleich vorgemerkt. */
class Store(private val dao: RecordDao) {

    private val _localChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Meldet lokale Änderungen (Auslöser für den Abgleich) */
    val localChanges: SharedFlow<Unit> = _localChanges

    private val _habitChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Meldet geänderte Gewohnheiten, lokal oder vom Server (Auslöser für die Erinnerungen) */
    val habitChanges: SharedFlow<Unit> = _habitChanges

    // ---------- Lesen ----------

    /** Alle Gewohnheiten, auch archivierte, in ihrer Reihenfolge */
    val habits: Flow<List<Habit>> = dao.observeKind(Kind.HABIT).map { list -> list.map(::toHabit).sorted() }

    /** Alle Einträge je Gewohnheit und Tag */
    val logs: Flow<Map<String, Map<LocalDate, HabitLog>>> = dao.observeKind(Kind.LOG).map { list ->
        list.mapNotNull(::toLog).groupBy { it.habitUid }.mapValues { (_, l) -> l.associateBy { it.date } }
    }

    fun habit(uid: String): Flow<Habit?> = habits.map { list -> list.firstOrNull { it.uid == uid } }

    suspend fun habitsNow(): List<Habit> = dao.kind(Kind.HABIT).map(::toHabit).sorted()

    suspend fun logsOn(date: LocalDate): Map<String, HabitLog> =
        dao.onDay(date.toString()).filter { it.kind == Kind.LOG }.mapNotNull(::toLog).associateBy { it.habitUid }

    private fun toHabit(r: RecordEntity) = Habit.from(r.uid, JSONObject(r.data))
    private fun toLog(r: RecordEntity) = runCatching { HabitLog.from(JSONObject(r.data)) }.getOrNull()
    private fun List<Habit>.sorted() = sortedWith(compareBy({ it.sortOrder }, { it.createdAt }))

    // ---------- Schreiben ----------

    suspend fun saveHabit(h: Habit) {
        save(h.uid, Kind.HABIT, null, h.toJson())
        _habitChanges.tryEmit(Unit)
    }

    suspend fun saveLog(l: HabitLog) = save(l.uid, Kind.LOG, l.date.toString(), l.toJson())

    /** Eintrag eines Tages entfernen (wieder „offen“) */
    suspend fun clearLog(habitUid: String, date: LocalDate) = delete(HabitLog.uid(habitUid, date))

    /** Gewohnheit samt Verlauf löschen */
    suspend fun deleteHabit(uid: String) {
        dao.kind(Kind.LOG).filter { toLog(it)?.habitUid == uid }.forEach { delete(it.uid) }
        delete(uid)
        _habitChanges.tryEmit(Unit)
    }

    suspend fun nextSortOrder(): Int = (habitsNow().maxOfOrNull { it.sortOrder } ?: -1) + 1

    private suspend fun delete(uid: String) {
        val old = dao.get(uid) ?: return
        dao.upsert(old.copy(deleted = true, dirty = true, updatedAt = stamp(old.updatedAt), data = JSONObject().toString()))
        _localChanges.tryEmit(Unit)
    }

    private suspend fun save(uid: String, kind: String, day: String?, data: JSONObject) {
        val old = dao.get(uid)
        dao.upsert(RecordEntity(uid, kind, day, stamp(old?.updatedAt), false, true, data.toString()))
        _localChanges.tryEmit(Unit)
    }

    /** Nie kleiner als der bisherige Stand – sonst gewänne beim Abgleich die ältere Fassung. */
    private fun stamp(previous: Long?) = maxOf(System.currentTimeMillis(), (previous ?: 0) + 1)

    // ---------- Abgleich ----------

    suspend fun pendingChanges(): List<SyncRecord> = dao.dirty().map {
        SyncRecord(it.uid, it.kind, it.updatedAt, it.deleted, JSONObject(it.data))
    }

    suspend fun markPushed(records: List<SyncRecord>) = records.forEach { dao.markClean(it.uid, it.updatedAt) }

    suspend fun markAllForUpload() = dao.markAllDirty()

    /** Übernimmt Änderungen vom Server, wenn sie jünger sind als der Stand auf dem Handy. */
    suspend fun applyRemote(records: List<SyncRecord>) {
        val apply = records.filter { it.kind in Kind.ALL }.mapNotNull { r ->
            val local = dao.get(r.uid)
            if (local != null && r.updatedAt <= local.updatedAt) return@mapNotNull null
            val day = if (r.kind == Kind.LOG) r.data.optStringOrNull("date") ?: local?.day else null
            RecordEntity(r.uid, r.kind, day, r.updatedAt, r.deleted, false, r.data.toString())
        }
        if (apply.isNotEmpty()) dao.upsertAll(apply)
        if (apply.any { it.kind == Kind.HABIT }) _habitChanges.tryEmit(Unit)
    }

    companion object {
        fun newUid(): String = UUID.randomUUID().toString().replace("-", "")
    }
}

/** Ein Datensatz im Abgleich (Format siehe kopffrei-cloud/src/habits.js) */
data class SyncRecord(val uid: String, val kind: String, val updatedAt: Long, val deleted: Boolean, val data: JSONObject) {
    fun toJson(): JSONObject = JSONObject()
        .put("uid", uid).put("kind", kind).put("updatedAt", updatedAt).put("deleted", deleted).put("data", data)

    companion object {
        fun fromJson(o: JSONObject) = SyncRecord(
            o.getString("uid"), o.getString("kind"), o.getLong("updatedAt"), o.optBoolean("deleted"),
            o.optJSONObject("data") ?: JSONObject(),
        )
    }
}
