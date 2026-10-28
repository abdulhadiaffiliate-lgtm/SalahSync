package com.salahsync.tracker

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/**
 * A fard prayer marked as prayed on [day] (epoch day). [late] is set when it was prayed
 * after its time had ended; [markedAt] is when it was ticked, or 0 if not known.
 */
@Entity(tableName = "prayed", primaryKeys = ["day", "prayer"])
data class PrayedRow(
    val day: Long,
    val prayer: String,
    @ColumnInfo(defaultValue = "0") val late: Boolean = false,
    @ColumnInfo(defaultValue = "0") val markedAt: Long = 0,
)

/** One line in the Qada ledger: earlier missed prayers added by hand, or a prayer made up. */
@Entity(tableName = "qada_entry")
data class QadaRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val prayer: String,
    val kind: String,
    val count: Int,
    val day: Long,
    val createdAt: Long,
)

/** How far through each dhikr the user is, per day and session. */
@Entity(tableName = "adhkar_progress", primaryKeys = ["day", "session", "dhikrId"])
data class AdhkarProgressRow(val day: Long, val session: String, val dhikrId: String, val count: Int)

@Entity(tableName = "tracker_meta")
data class MetaRow(@PrimaryKey val key: String, val value: String)

@Dao
interface TrackerDao {
    @Query("SELECT * FROM prayed")
    fun prayed(): Flow<List<PrayedRow>>

    @Query("SELECT COUNT(*) FROM prayed WHERE day = :day AND prayer = :prayer")
    suspend fun isPrayed(day: Long, prayer: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPrayed(row: PrayedRow)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPrayedAll(rows: List<PrayedRow>)

    @Query("SELECT * FROM prayed WHERE day = :day AND prayer = :prayer")
    suspend fun prayedRow(day: Long, prayer: String): PrayedRow?

    @Query("UPDATE prayed SET late = :late WHERE day = :day AND prayer = :prayer")
    suspend fun setLate(day: Long, prayer: String, late: Boolean)

    @Query("DELETE FROM prayed WHERE day = :day AND prayer = :prayer")
    suspend fun deletePrayed(day: Long, prayer: String)

    @Query("SELECT * FROM qada_entry ORDER BY id")
    fun qada(): Flow<List<QadaRow>>

    @Insert
    suspend fun insertQada(row: QadaRow)

    @Query("SELECT * FROM qada_entry WHERE prayer = :prayer AND kind = :kind ORDER BY id DESC LIMIT 1")
    suspend fun lastQada(prayer: String, kind: String): QadaRow?

    @Query("DELETE FROM qada_entry WHERE id = :id")
    suspend fun deleteQada(id: Long)

    @Query("SELECT COALESCE(SUM(`count`), 0) FROM qada_entry WHERE kind = :kind AND day >= :from")
    fun qadaSince(kind: String, from: Long): Flow<Int>

    @Query("SELECT * FROM adhkar_progress WHERE day = :day AND session = :session")
    fun adhkarProgress(day: Long, session: String): Flow<List<AdhkarProgressRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setAdhkarProgress(row: AdhkarProgressRow)

    @Query("DELETE FROM adhkar_progress WHERE day < :day")
    suspend fun pruneAdhkarProgress(day: Long)

    @Query("SELECT value FROM tracker_meta WHERE `key` = :key")
    suspend fun meta(key: String): String?

    @Query("SELECT value FROM tracker_meta WHERE `key` = :key")
    fun metaFlow(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setMeta(row: MetaRow)
}

/**
 * The prayer tracker's own database. It lives only on this phone: the file is excluded
 * from cloud backup (see res/xml/backup_rules.xml), and the app never uploads it.
 */
@Database(
    entities = [PrayedRow::class, QadaRow::class, AdhkarProgressRow::class, MetaRow::class],
    version = 2,
    exportSchema = false,
)
abstract class TrackerDatabase : RoomDatabase() {
    abstract fun dao(): TrackerDao

    companion object {
        const val FILE = "salah_tracker.db"

        /** Version 2 records whether each prayer was on time. Older ticks count as on time. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE prayed ADD COLUMN late INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE prayed ADD COLUMN markedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun open(context: Context): TrackerDatabase =
            Room.databaseBuilder(context, TrackerDatabase::class.java, FILE).addMigrations(MIGRATION_1_2).build()
    }
}
