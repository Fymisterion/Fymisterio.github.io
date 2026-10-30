package de.gerry.dranbleiben.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Alle Daten liegen als JSON-Datensätze in einer Tabelle – genau so, wie sie mit dem Server abgeglichen werden.
 * [day] ist nur für Tageseinträge gesetzt, damit ein Zeitraum schnell geladen ist.
 */
@Entity(tableName = "records", indices = [Index("kind"), Index("day"), Index("dirty")])
data class RecordEntity(
    @PrimaryKey val uid: String,
    val kind: String,
    val day: String?,
    val updatedAt: Long,
    val deleted: Boolean,
    /** Noch nicht zum Server übertragen */
    val dirty: Boolean,
    val data: String,
)

@Dao
interface RecordDao {
    @Query("SELECT * FROM records WHERE kind = :kind AND deleted = 0")
    fun observeKind(kind: String): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE kind = :kind AND deleted = 0")
    suspend fun kind(kind: String): List<RecordEntity>

    @Query("SELECT * FROM records WHERE day = :day AND deleted = 0")
    suspend fun onDay(day: String): List<RecordEntity>

    @Query("SELECT * FROM records WHERE uid = :uid")
    suspend fun get(uid: String): RecordEntity?

    @Upsert
    suspend fun upsert(record: RecordEntity)

    @Upsert
    suspend fun upsertAll(records: List<RecordEntity>)

    @Query("SELECT * FROM records WHERE dirty = 1 LIMIT 2000")
    suspend fun dirty(): List<RecordEntity>

    @Query("UPDATE records SET dirty = 0 WHERE uid = :uid AND updatedAt = :updatedAt")
    suspend fun markClean(uid: String, updatedAt: Long)

    @Query("UPDATE records SET dirty = 1")
    suspend fun markAllDirty()
}

@Database(entities = [RecordEntity::class], version = 1)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun records(): RecordDao

    companion object {
        fun create(context: Context) = Room.databaseBuilder(context, HabitDatabase::class.java, "dranbleiben.db").build()
    }
}
