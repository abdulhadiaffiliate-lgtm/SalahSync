package com.salahsync

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.salahsync.core.AsrSchool
import com.salahsync.core.DayTimes
import com.salahsync.core.HighLatitude
import com.salahsync.core.Hijri
import com.salahsync.core.HijriDate
import com.salahsync.core.Method
import com.salahsync.core.Prayer
import com.salahsync.data.AlertStyle
import com.salahsync.data.SavedLocation
import com.salahsync.data.ThemeMode
import com.salahsync.data.UserPrefs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val salah = app as SalahSyncApp
    private val settings = salah.settings

    /** Null until the first read from disk, so the UI can avoid flashing onboarding. */
    val prefs: StateFlow<UserPrefs?> = settings.prefs
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _now = MutableStateFlow(ZonedDateTime.now())
    val now: StateFlow<ZonedDateTime> = _now.asStateFlow()

    private val _today = MutableStateFlow<DayTimes?>(null)
    val today: StateFlow<DayTimes?> = _today.asStateFlow()

    private val _tomorrow = MutableStateFlow<DayTimes?>(null)
    val tomorrow: StateFlow<DayTimes?> = _tomorrow.asStateFlow()

    private val _locating = MutableStateFlow(false)
    val locating: StateFlow<Boolean> = _locating.asStateFlow()

    private var recalcJob: Job? = null

    init {
        viewModelScope.launch {
            while (isActive) {
                val n = ZonedDateTime.now()
                val dayChanged = n.toLocalDate() != _now.value.toLocalDate()
                _now.value = n
                if (dayChanged) prefs.value?.let { recalc(it) }
                delay(1_000L - n.nano / 1_000_000)
            }
        }
        viewModelScope.launch {
            // Recalculate and reschedule only when settings that affect times or alerts change.
            settings.prefs
                .map { it.copy(streakDays = 0, lastOpenEpochDay = 0, prayed = emptySet(), name = "", theme = ThemeMode.DARK) }
                .distinctUntilChanged()
                .collect { recalc(settings.current()) }
        }
        viewModelScope.launch { settings.recordOpen() }
    }

    private fun recalc(p: UserPrefs) {
        recalcJob?.cancel()
        recalcJob = viewModelScope.launch {
            val zone = ZonedDateTime.now().zone
            val date = LocalDate.now(zone)
            _today.value = salah.prayers.timesFor(p, date, zone)
            _tomorrow.value = salah.prayers.timesFor(p, date.plusDays(1), zone)
            salah.scheduler.reschedule(p)
            // Online times arrive in the background; refresh the screen once they land.
            salah.prayers.refreshOnline(p, date)
            _today.value = salah.prayers.timesFor(p, date, zone)
            _tomorrow.value = salah.prayers.timesFor(p, date.plusDays(1), zone)
            salah.scheduler.reschedule(p)
        }
    }

    suspend fun timesFor(date: LocalDate): DayTimes? = prefs.value?.let { salah.prayers.timesFor(it, date) }

    fun hijriFor(date: LocalDate): HijriDate = Hijri.of(date, prefs.value?.hijriAdjustment ?: 0)

    fun canScheduleExact(): Boolean = salah.scheduler.canScheduleExact()

    private fun io(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    fun setName(name: String) = io { settings.setName(name) }
    fun completeOnboarding() = io { settings.completeOnboarding() }
    fun setLocation(loc: SavedLocation) = io { settings.setLocation(loc) }
    fun setMethod(m: Method) = io { settings.setMethod(m) }
    fun setMethodAuto(on: Boolean) = io { settings.setMethodAuto(on) }
    fun setAsrSchool(s: AsrSchool) = io { settings.setAsrSchool(s) }
    fun setHighLatitude(r: HighLatitude) = io { settings.setHighLatitude(r) }
    fun setOffset(p: Prayer, minutes: Int) = io { settings.setOffset(p, minutes) }
    fun setHijriAdjustment(d: Int) = io { settings.setHijriAdjustment(d) }
    fun setHijriAuto(on: Boolean) = io { settings.setHijriAuto(on) }
    fun setAlert(p: Prayer, s: AlertStyle) = io { settings.setAlert(p, s) }
    fun setAllAlerts(s: AlertStyle) = io { settings.setAllAlerts(s) }
    fun setPreReminder(m: Int) = io { settings.setPreReminder(m) }
    fun setQuranReminder(on: Boolean) = io { settings.setQuranReminder(on) }
    fun setDuaReminder(on: Boolean) = io { settings.setDuaReminder(on) }
    fun setUseOnlineTimes(on: Boolean) = io { settings.setUseOnlineTimes(on) }
    fun setTheme(t: ThemeMode) = io { settings.setTheme(t) }
    fun togglePrayed(date: LocalDate, p: Prayer) = io { settings.togglePrayed(date, p) }

    /** Rebuilds alarms, e.g. after the user grants the exact-alarm permission. */
    fun rescheduleNow() = io { prefs.value?.let { salah.scheduler.reschedule(it) } }

    fun locate(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _locating.value = true
            val loc = salah.location.currentLocation()
            _locating.value = false
            if (loc != null) settings.setLocation(loc)
            onResult(loc != null)
        }
    }
}
