package com.salahsync.tracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.salahsync.SalahSyncApp
import com.salahsync.core.Prayer
import com.salahsync.core.tracker.PrayerWindows
import com.salahsync.core.tracker.QadaCount
import com.salahsync.core.tracker.QadaLedger
import com.salahsync.core.tracker.WeeklyReport
import com.salahsync.core.tracker.WeeklyReports
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime

data class TrackerState(
    val today: LocalDate,
    val since: LocalDate,
    val prayed: Set<Pair<LocalDate, Prayer>>,
    val late: Set<Pair<LocalDate, Prayer>>,
    val qada: Map<Prayer, QadaCount>,
) {
    val totalOwed: Int get() = qada.values.sumOf { it.owed }

    fun isPrayed(date: LocalDate, prayer: Prayer): Boolean = (date to prayer) in prayed

    fun isLate(date: LocalDate, prayer: Prayer): Boolean = (date to prayer) in late

    fun prayedOn(date: LocalDate): Int = Prayer.fard.count { isPrayed(date, it) }

    /** The Friday-to-Thursday report for the week starting [weekStart]. */
    fun report(weekStart: LocalDate): WeeklyReport = WeeklyReports.build(weekStart, today, since, prayed, late, totalOwed)
}

class TrackerViewModel(app: Application) : AndroidViewModel(app) {
    private val salah = app as SalahSyncApp
    private val repo = salah.tracker

    private val today = MutableStateFlow(LocalDate.now())

    /** Null until the database has been read once. */
    val state: StateFlow<TrackerState?> = combine(today, repo.trackingSince, repo.marks, repo.qadaEntries) { day, since, marks, entries ->
        TrackerState(day, since, marks.prayed, marks.late, QadaLedger.counts(since, day, marks.prayed, entries))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch { repo.start(LocalDate.now(), salah.settings.takeLegacyPrayed()) }
        viewModelScope.launch {
            // Yesterday's unmarked prayers become Qada when the day turns.
            while (isActive) {
                today.value = LocalDate.now()
                delay(30_000)
            }
        }
    }

    fun togglePrayed(date: LocalDate, prayer: Prayer) {
        if (date.isAfter(LocalDate.now())) return
        viewModelScope.launch { repo.togglePrayed(date, prayer, lateIfMarked = wouldBeLate(date, prayer)) }
    }

    /** Corrects a tick between on time and late. */
    fun toggleLate(date: LocalDate, prayer: Prayer) = viewModelScope.launch { repo.toggleLate(date, prayer) }

    /**
     * A tick counts as late when it is made after the prayer's time has ended. Without
     * prayer times (no location yet) it is taken as on time; the user can correct it.
     */
    private suspend fun wouldBeLate(date: LocalDate, prayer: Prayer): Boolean {
        val prefs = salah.settings.current()
        val day = salah.prayers.timesFor(prefs, date) ?: return false
        val next = salah.prayers.timesFor(prefs, date.plusDays(1))
        return PrayerWindows.isLate(prayer, day, next, ZonedDateTime.now(day.zone))
    }

    fun logMadeUp(prayer: Prayer) = viewModelScope.launch { repo.logMadeUp(prayer, LocalDate.now()) }

    fun undoMadeUp(prayer: Prayer) = viewModelScope.launch { repo.undoMadeUp(prayer) }

    /** Sets the number of missed prayers from before tracking began to [total]. */
    fun setEarlier(prayer: Prayer, total: Int) = viewModelScope.launch {
        val current = state.value?.qada?.get(prayer)?.added ?: 0
        repo.adjustEarlier(prayer, total.coerceAtLeast(0) - current, LocalDate.now())
    }
}
