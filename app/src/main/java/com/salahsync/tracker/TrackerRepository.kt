package com.salahsync.tracker

import android.content.Context
import androidx.room.withTransaction
import com.salahsync.core.Prayer
import com.salahsync.core.adhkar.AdhkarSession
import com.salahsync.core.tracker.QadaEntry
import com.salahsync.core.tracker.QadaKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Prayer history, Qada and adhkar progress, kept in a local database that is never synced. */
class TrackerRepository(context: Context) {
    private val db = TrackerDatabase.open(context)
    private val dao = db.dao()

    private fun prayerOf(name: String): Prayer? = runCatching { Prayer.valueOf(name) }.getOrNull()

    /** The first day whose unmarked prayers count as Qada. */
    val trackingSince: Flow<LocalDate> = dao.metaFlow(KEY_SINCE).filterNotNull().map { LocalDate.ofEpochDay(it.toLong()) }

    val prayed: Flow<Set<Pair<LocalDate, Prayer>>> = dao.prayed().map { rows ->
        rows.mapNotNull { r -> prayerOf(r.prayer)?.let { LocalDate.ofEpochDay(r.day) to it } }.toSet()
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

    suspend fun togglePrayed(date: LocalDate, prayer: Prayer) = db.withTransaction {
        val day = date.toEpochDay()
        if (dao.isPrayed(day, prayer.name) > 0) dao.deletePrayed(day, prayer.name)
        else dao.insertPrayed(PrayedRow(day, prayer.name))
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
