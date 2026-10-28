package com.salahsync.tracker

import android.content.Context
import androidx.room.withTransaction
import com.salahsync.core.Prayer
import com.salahsync.core.adhkar.AdhkarSession
import com.salahsync.core.tracker.QadaEntry
import com.salahsync.core.tracker.QadaKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

data class Marks(val prayed: Set<Pair<LocalDate, Prayer>>, val late: Set<Pair<LocalDate, Prayer>>)

data class Snapshot(val since: LocalDate, val marks: Marks, val qada: List<QadaEntry>)

/** Prayer history, Qada and adhkar progress, kept in a local database that is never synced. */
class TrackerRepository(context: Context) {
    private val db = TrackerDatabase.open(context)
    private val dao = db.dao()

    private fun prayerOf(name: String): Prayer? = runCatching { Prayer.valueOf(name) }.getOrNull()

    /** The first day whose unmarked prayers count as Qada. */
    val trackingSince: Flow<LocalDate> = dao.metaFlow(KEY_SINCE).filterNotNull().map { LocalDate.ofEpochDay(it.toLong()) }

    /** Every tick, with whether it was late. */
    val marks: Flow<Marks> = dao.prayed().map { rows ->
        val all = rows.mapNotNull { r -> prayerOf(r.prayer)?.let { Triple(LocalDate.ofEpochDay(r.day), it, r.late) } }
        Marks(
            prayed = all.map { (d, p, _) -> d to p }.toSet(),
            late = all.filter { it.third }.map { (d, p, _) -> d to p }.toSet(),
        )
    }

    /** The tracker as it stands now, for work done outside the app's screens. */
    suspend fun snapshot(): Snapshot? {
        val since = dao.meta(KEY_SINCE)?.toLongOrNull()?.let(LocalDate::ofEpochDay) ?: return null
        return Snapshot(since, marks.first(), qadaEntries.first())
    }

    val qadaEntries: Flow<List<QadaEntry>> = dao.qada().map { rows ->
        rows.mapNotNull { r ->
            val prayer = prayerOf(r.prayer) ?: return@mapNotNull null
            val kind = runCatching { QadaKind.valueOf(r.kind) }.getOrNull() ?: return@mapNotNull null
            QadaEntry(prayer, kind, r.count)
        }
    }

    /**
     * Starts tracking today on first run, and moves ticks from the old settings store
     * into this database so nothing marked before this version is lost.
     */
    suspend fun start(today: LocalDate, legacyPrayed: Set<String>) = db.withTransaction {
        if (dao.meta(KEY_SINCE) == null) dao.setMeta(MetaRow(KEY_SINCE, today.toEpochDay().toString()))
        val rows = legacyPrayed.mapNotNull { key ->
            val date = runCatching { LocalDate.parse(key.substringBefore(':')) }.getOrNull() ?: return@mapNotNull null
            val prayer = prayerOf(key.substringAfter(':')) ?: return@mapNotNull null
            PrayedRow(date.toEpochDay(), prayer.name)
        }
        if (rows.isNotEmpty()) dao.insertPrayedAll(rows)
    }

    /** Ticks or unticks [prayer]. A new tick is stored as [lateIfMarked]. */
    suspend fun togglePrayed(date: LocalDate, prayer: Prayer, lateIfMarked: Boolean) = db.withTransaction {
        val day = date.toEpochDay()
        if (dao.isPrayed(day, prayer.name) > 0) dao.deletePrayed(day, prayer.name)
        else dao.insertPrayed(PrayedRow(day, prayer.name, late = lateIfMarked, markedAt = System.currentTimeMillis()))
    }

    /** Switches a ticked prayer between on time and late. Does nothing if it is not ticked. */
    suspend fun toggleLate(date: LocalDate, prayer: Prayer) = db.withTransaction {
        val day = date.toEpochDay()
        dao.prayedRow(day, prayer.name)?.let { dao.setLate(day, prayer.name, !it.late) }
    }

    suspend fun logMadeUp(prayer: Prayer, today: LocalDate) = addEntry(prayer, QadaKind.MADE_UP, 1, today)

    /** Removes the most recent made-up entry for [prayer], to correct a mistaken tap. */
    suspend fun undoMadeUp(prayer: Prayer) = db.withTransaction {
        dao.lastQada(prayer.name, QadaKind.MADE_UP.name)?.let { dao.deleteQada(it.id) }
    }

    /** Records [delta] more (or fewer) missed prayers from before tracking began. */
    suspend fun adjustEarlier(prayer: Prayer, delta: Int, today: LocalDate) {
        if (delta != 0) addEntry(prayer, QadaKind.ADDED, delta, today)
    }

    private suspend fun addEntry(prayer: Prayer, kind: QadaKind, count: Int, today: LocalDate) =
        dao.insertQada(QadaRow(prayer = prayer.name, kind = kind.name, count = count, day = today.toEpochDay(), createdAt = System.currentTimeMillis()))

    /** How many Qada prayers were logged as made up from [from] on. */
    fun madeUpSince(from: LocalDate): Flow<Int> = dao.qadaSince(QadaKind.MADE_UP.name, from.toEpochDay())

    fun adhkarProgress(date: LocalDate, session: AdhkarSession): Flow<Map<String, Int>> =
        dao.adhkarProgress(date.toEpochDay(), session.name).map { rows -> rows.associate { it.dhikrId to it.count } }

    suspend fun setAdhkarProgress(date: LocalDate, session: AdhkarSession, dhikrId: String, count: Int) {
        dao.pruneAdhkarProgress(date.toEpochDay() - 1)
        dao.setAdhkarProgress(AdhkarProgressRow(date.toEpochDay(), session.name, dhikrId, count))
    }

    private companion object {
        const val KEY_SINCE = "tracking_since"
    }
}
