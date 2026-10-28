package com.salahsync.goals

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.salahsync.SalahSyncApp
import com.salahsync.core.Prayer
import com.salahsync.core.adhkar.AdhkarSession
import com.salahsync.core.goals.DayRecord
import com.salahsync.core.goals.GoalKind
import com.salahsync.core.goals.Goals
import com.salahsync.core.goals.GoalsDay
import com.salahsync.core.tracker.WeeklyReports
import com.salahsync.tracker.Marks
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Daily and weekly goals. Prayers, adhkar and Qada are read from the prayer tracker's
 * database rather than recorded twice; only reading time and the days met are stored here.
 */
class GoalsViewModel(app: Application) : AndroidViewModel(app) {
    private val tracker = (app as SalahSyncApp).tracker
    private val store = GoalsStore(app)

    private val today = MutableStateFlow(LocalDate.now())

    private class Computed(val day: GoalsDay, val todayMet: Boolean, val yesterdayMet: Boolean, val recorded: Set<LocalDate>)

    private fun adhkarDone(date: LocalDate, session: AdhkarSession): Flow<Boolean> =
        tracker.adhkarProgress(date, session).map { Goals.adhkarDone(session, it) }.distinctUntilChanged()

    /** Null until the stores have been read once. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<GoalsDay?> = today.flatMapLatest { day ->
        val yesterday = day.minusDays(1)
        val adhkar = combine(
            adhkarDone(day, AdhkarSession.MORNING), adhkarDone(day, AdhkarSession.EVENING),
            adhkarDone(yesterday, AdhkarSession.MORNING), adhkarDone(yesterday, AdhkarSession.EVENING),
        ) { m, e, ym, ye -> listOf(m, e, ym, ye) }
        combine(store.data, tracker.marks, adhkar, tracker.madeUpSince(WeeklyReports.weekStart(day))) { data, marks, a, madeUp ->
            val since = data.since ?: day
            val settings = data.settings
            fun record(date: LocalDate, morning: Boolean, evening: Boolean) =
                DayRecord(marks.prayedOn(date), marks.onTimeOn(date), data.quranSeconds[date] ?: 0, morning, evening)
            val todayRecord = record(day, a[0], a[1])
            Computed(
                day = Goals.build(day, since, settings, todayRecord, data.met, madeUp),
                todayMet = Goals.dayMet(settings, todayRecord),
                yesterdayMet = !yesterday.isBefore(since) && Goals.dayMet(settings, record(yesterday, a[2], a[3])),
                recorded = data.met,
            )
        }
    }.onEach { c ->
        // Today follows what happens, both ways. Yesterday can still become met, for a
        // prayer ticked late after midnight, but is never taken back, so changing goals
        // today does not undo a day already done.
        val day = c.day.today
        if (c.todayMet != (day in c.recorded)) store.setMet(day, c.todayMet)
        val yesterday = day.minusDays(1)
        if (c.yesterdayMet && yesterday !in c.recorded) store.setMet(yesterday, true)
    }.map { it.day }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch { store.start(LocalDate.now()) }
        viewModelScope.launch {
            while (isActive) {
                today.value = LocalDate.now()
                delay(30_000)
            }
        }
    }

    private fun Marks.prayedOn(date: LocalDate): Int = Prayer.fard.count { (date to it) in prayed }

    private fun Marks.onTimeOn(date: LocalDate): Int = Prayer.fard.count { (date to it) in prayed && (date to it) !in late }

    fun setEnabled(kind: GoalKind, on: Boolean) = viewModelScope.launch { store.setEnabled(kind, on) }
    fun setQuranMinutes(minutes: Int) = viewModelScope.launch { store.setQuranMinutes(minutes) }
    fun setGoodDays(days: Int) = viewModelScope.launch { store.setGoodDays(days) }
    fun setQadaPerWeek(n: Int) = viewModelScope.launch { store.setQadaPerWeek(n) }

    /** Time spent in the Quran reader. */
    fun addReading(seconds: Int) {
        if (seconds > 0) viewModelScope.launch { store.addQuranSeconds(LocalDate.now(), seconds) }
    }

    /** For reading from a printed mushaf, or to correct the count. */
    fun adjustReadingMinutes(minutes: Int) = viewModelScope.launch { store.addQuranSeconds(LocalDate.now(), minutes * 60) }
}
