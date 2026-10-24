package com.salahsync.hadith

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * One hadith of a downloaded book. [seq] is its place in the dataset, [number] the
 * dataset's hadithnumber (to join the three language editions), and [ref] and [base]
 * how it is cited (see HadithNumber). [grades] holds "scholar<TAB>grade" lines.
 */
@Entity(
    tableName = "hadith",
    indices = [
        Index(value = ["book", "seq"], unique = true),
        Index(value = ["book", "number"]),
        Index(value = ["book", "chapter", "seq"]),
        Index(value = ["ref"]),
        Index(value = ["base"]),
    ],
)
data class HadithRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val book: String,
    val seq: Int,
    val number: String,
    val ref: String,
    val base: String,
    val chapter: Int,
    val arabic: String,
    val english: String,
    val urdu: String,
    val grades: String,
)

/** Full-text index over the translations, kept in step with [HadithRow] by Room. */
@Fts4(contentEntity = HadithRow::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "hadith_fts")
data class HadithFts(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowId: Long,
    val english: String,
    val urdu: String,
)

/** A chapter (kitab) title, as the dataset names it. */
@Entity(tableName = "chapter", primaryKeys = ["book", "number"])
data class ChapterRow(val book: String, val number: Int, val title: String)

/** A book whose import finished, and the dataset commit it came from. */
@Entity(tableName = "installed")
data class InstalledRow(@PrimaryKey val book: String, val commit: String, val installedAt: Long)

data class ChapterSummary(val number: Int, val title: String, val count: Int, val firstSeq: Int, val lastSeq: Int)

data class SeqRef(val seq: Int, val ref: String)

@Dao
interface HadithDao {
    @Query("SELECT book FROM installed")
    fun installed(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setInstalled(row: InstalledRow)

    @Insert
    suspend fun insertHadith(rows: List<HadithRow>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(rows: List<ChapterRow>)

    @Query("UPDATE hadith SET arabic = :text WHERE book = :book AND number = :number")
    suspend fun setArabic(book: String, number: String, text: String)

    @Query("UPDATE hadith SET urdu = :text WHERE book = :book AND number = :number")
    suspend fun setUrdu(book: String, number: String, text: String)

    /** Entries the dataset keeps only as numbering, with no text in any language. */
    @Query("DELETE FROM hadith WHERE book = :book AND arabic = '' AND english = '' AND urdu = ''")
    suspend fun deleteEmpty(book: String)

    @Query("DELETE FROM hadith WHERE book = :book")
    suspend fun deleteHadith(book: String)

    @Query("DELETE FROM chapter WHERE book = :book")
    suspend fun deleteChapters(book: String)

    @Query("DELETE FROM installed WHERE book = :book")
    suspend fun deleteInstalled(book: String)

    @Query(
        """SELECT c.number AS number, c.title AS title, COUNT(h.id) AS count, MIN(h.seq) AS firstSeq, MAX(h.seq) AS lastSeq
        FROM chapter c JOIN hadith h ON h.book = c.book AND h.chapter = c.number
        WHERE c.book = :book GROUP BY c.number ORDER BY c.number""",
    )
    suspend fun chapters(book: String): List<ChapterSummary>

    @Query("SELECT seq, ref FROM hadith WHERE book = :book AND seq IN (:seqs)")
    suspend fun refsAt(book: String, seqs: List<Int>): List<SeqRef>

    @Query("SELECT title FROM chapter WHERE book = :book AND number = :number")
    suspend fun chapterTitle(book: String, number: Int): String?

    @Query("SELECT * FROM hadith WHERE book = :book AND chapter = :chapter ORDER BY seq")
    suspend fun inChapter(book: String, chapter: Int): List<HadithRow>

    @Query("SELECT * FROM hadith WHERE book = :book AND ref = :ref LIMIT 1")
    suspend fun byRef(book: String, ref: String): HadithRow?

    @Query("SELECT * FROM hadith WHERE (ref = :ref OR base = :ref) ORDER BY book, seq LIMIT :limit")
    suspend fun byNumber(ref: String, limit: Int): List<HadithRow>

    @Query(
        """SELECT h.* FROM hadith h JOIN hadith_fts ON h.id = hadith_fts.rowid
        WHERE hadith_fts MATCH :match ORDER BY h.book, h.seq LIMIT :limit""",
    )
    suspend fun matching(match: String, limit: Int): List<HadithRow>
}

/**
 * Downloaded hadith books. Everything in it can be downloaded again, so it is left out of
 * backups (res/xml/backup_rules.xml).
 */
@Database(
    entities = [HadithRow::class, HadithFts::class, ChapterRow::class, InstalledRow::class],
    version = 1,
    exportSchema = false,
)
abstract class HadithDatabase : RoomDatabase() {
    abstract fun dao(): HadithDao

    companion object {
        const val FILE = "hadith.db"

        fun open(context: Context): HadithDatabase =
            Room.databaseBuilder(context, HadithDatabase::class.java, FILE).build()
    }
}
