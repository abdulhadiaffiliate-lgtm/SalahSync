package com.salahsync.goals

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.salahsync.core.goals.GoalKind
import com.salahsync.core.goals.GoalSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.goalsStore: DataStore<Preferences> by preferencesDataStore(name = "goals")

data class GoalsData(
    val settings: GoalSettings = GoalSettings(),
    /** The day goals began; null until the first run has set it. */
    val since: LocalDate? = null,
    val met: Set<LocalDate> = emptySet(),
    val quranSeconds: Map<LocalDate, Int> = emptyMap(),
)

/**
 * Goal settings, the days they were met and the minutes read, kept on this phone only.
 * The file (datastore/goals.preferences_pb) is left out of cloud backup and device
 * transfer; prayer and adhkar progress is read from the tracker, not copied here.
 */
class GoalsStore(private val context: Context) {
    private object K {
        val enabled = stringSetPreferencesKey("enabled")
        val quranMinutes = intPreferencesKey("quran_minutes")
        val goodDays = intPreferencesKey("good_days")
        val qadaPerWeek = intPreferencesKey("qada_per_week")
        val since = longPreferencesKey("since")
        val met = stringSetPreferencesKey("met_days")
        const val QURAN_PREFIX = "quran_day_"
        fun quranDay(date: LocalDate) = intPreferencesKey(QURAN_PREFIX + date.toEpochDay())
    }

    val data: Flow<GoalsData> = context.goalsStore.data.map { p ->
        GoalsData(
            settings = GoalSettings(
                enabled = p[K.enabled]?.mapNotNull { runCatching { GoalKind.valueOf(it) }.getOrNull() }?.toSet() ?: GoalSettings.DEFAULT_ENABLED,
                quranMinutes = (p[K.quranMinutes] ?: GoalSettings().quranMinutes).coerceIn(GoalSettings.quranMinuteRange),
                goodDays = (p[K.goodDays] ?: GoalSettings().goodDays).coerceIn(GoalSettings.goodDayRange),
                qadaPerWeek = (p[K.qadaPerWeek] ?: GoalSettings().qadaPerWeek).coerceIn(GoalSettings.qadaRange),
            ),
            since = p[K.since]?.let(LocalDate::ofEpochDay),
            met = p[K.met].orEmpty().mapNotNull { it.toLongOrNull()?.let(LocalDate::ofEpochDay) }.toSet(),
            quranSeconds = p.asMap().entries.mapNotNull { (key, value) ->
                val day = key.name.removePrefix(K.QURAN_PREFIX).takeIf { key.name.startsWith(K.QURAN_PREFIX) }?.toLongOrNull()
                if (day == null || value !is Int) null else LocalDate.ofEpochDay(day) to value
            }.toMap(),
        )
    }

    /** Goals begin today on first run. */
    suspend fun start(today: LocalDate) = context.goalsStore.edit { p ->
        if (p[K.since] == null) p[K.since] = today.toEpochDay()
    }

    suspend fun setEnabled(kind: GoalKind, on: Boolean) = context.goalsStore.edit { p ->
        val current = p[K.enabled] ?: GoalSettings.DEFAULT_ENABLED.map { it.name }.toSet()
        p[K.enabled] = if (on) current + kind.name else current - kind.name
    }

    suspend fun setQuranMinutes(minutes: Int) = context.goalsStore.edit { it[K.quranMinutes] = minutes.coerceIn(GoalSettings.quranMinuteRange) }
    suspend fun setGoodDays(days: Int) = context.goalsStore.edit { it[K.goodDays] = days.coerceIn(GoalSettings.goodDayRange) }
    suspend fun setQadaPerWeek(n: Int) = context.goalsStore.edit { it[K.qadaPerWeek] = n.coerceIn(GoalSettings.qadaRange) }

    /** Adds (or, if negative, takes off) reading time on [date]. Never goes below zero. */
    suspend fun addQuranSeconds(date: LocalDate, seconds: Int) = context.goalsStore.edit { p ->
        val key = K.quranDay(date)
        p[key] = ((p[key] ?: 0) + seconds).coerceIn(0, MAX_SECONDS_A_DAY)
        prune(p, date)
    }

    suspend fun setMet(date: LocalDate, met: Boolean) = context.goalsStore.edit { p ->
        val current = p[K.met].orEmpty()
        val day = date.toEpochDay().toString()
        p[K.met] = if (met) current + day else current - day
    }

    /** Only today and yesterday are ever judged from reading time, so older days are dropped. */
    private fun prune(p: MutablePreferences, today: LocalDate) {
        val oldest = today.toEpochDay() - KEEP_READING_DAYS
        p.asMap().keys.map { it.name }.filter { it.startsWith(K.QURAN_PREFIX) }
            .filter { (it.removePrefix(K.QURAN_PREFIX).toLongOrNull() ?: Long.MIN_VALUE) < oldest }
            .forEach { p.remove(intPreferencesKey(it)) }
    }

    private companion object {
        const val MAX_SECONDS_A_DAY = 24 * 60 * 60
        const val KEEP_READING_DAYS = 14L
    }
}
