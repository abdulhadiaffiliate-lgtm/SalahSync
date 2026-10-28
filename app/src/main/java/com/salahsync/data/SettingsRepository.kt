package com.salahsync.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.salahsync.core.AsrSchool
import com.salahsync.core.GeoPoint
import com.salahsync.core.HighLatitude
import com.salahsync.core.Method
import com.salahsync.core.Prayer
import com.salahsync.core.RegionDefaults
import com.salahsync.core.SilenceMode
import com.salahsync.core.daily.DailyNoteKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "salahsync")

/** All user settings live on the device only; nothing is sent to a server. */
class SettingsRepository(private val context: Context) {

    private object K {
        val name = stringPreferencesKey("name")
        val onboarded = booleanPreferencesKey("onboarded")
        val lat = doublePreferencesKey("lat")
        val lon = doublePreferencesKey("lon")
        val place = stringPreferencesKey("place")
        val country = stringPreferencesKey("country")
        val methodAuto = booleanPreferencesKey("method_auto")
        val method = stringPreferencesKey("method")
        val asr = stringPreferencesKey("asr")
        val highLat = stringPreferencesKey("high_lat")
        val offsets = stringPreferencesKey("offsets")
        val hijriAuto = booleanPreferencesKey("hijri_auto")
        val hijriAdj = intPreferencesKey("hijri_adj")
        val alerts = stringPreferencesKey("alerts")
        val preReminder = intPreferencesKey("pre_reminder")
        val quranReminder = booleanPreferencesKey("quran_reminder")
        val duaReminder = booleanPreferencesKey("dua_reminder")
        val online = booleanPreferencesKey("online_times")
        val theme = stringPreferencesKey("theme")
        val streak = intPreferencesKey("streak")
        val lastOpen = longPreferencesKey("last_open")
        val prayed = stringSetPreferencesKey("prayed")
        val silentEnabled = booleanPreferencesKey("silent_enabled")
        val silentMode = stringPreferencesKey("silent_mode")
        val silentMinutes = intPreferencesKey("silent_minutes")
        val silentJumuah = intPreferencesKey("silent_jumuah_minutes")
        val silentPrayers = stringSetPreferencesKey("silent_prayers")
        val weeklyReport = booleanPreferencesKey("weekly_report")
        val dailyNote = booleanPreferencesKey("daily_note")
        val dailyNoteKind = stringPreferencesKey("daily_note_kind")
        val dailyNoteMinutes = intPreferencesKey("daily_note_minutes")
    }

    val prefs: Flow<UserPrefs> = context.dataStore.data.map { it.toUserPrefs() }

    suspend fun current(): UserPrefs = prefs.first()

    private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
        this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

    private fun Preferences.toUserPrefs(): UserPrefs {
        val lat = this[K.lat]
        val lon = this[K.lon]
        val location = if (lat != null && lon != null) {
            SavedLocation(GeoPoint(lat, lon), this[K.place] ?: "", this[K.country])
        } else null
        return UserPrefs(
            name = this[K.name] ?: "",
            onboarded = this[K.onboarded] ?: false,
            location = location,
            methodAuto = this[K.methodAuto] ?: true,
            method = this[K.method].toEnum(Method.MUSLIM_WORLD_LEAGUE),
            asrSchool = this[K.asr].toEnum(AsrSchool.SHAFII),
            highLatitude = this[K.highLat].toEnum(HighLatitude.MIDDLE_OF_THE_NIGHT),
            offsets = decodeMap(this[K.offsets]) { it.toIntOrNull() },
            hijriAuto = this[K.hijriAuto] ?: true,
            hijriAdjustment = this[K.hijriAdj] ?: 0,
            alerts = UserPrefs.defaultAlerts + decodeMap(this[K.alerts]) { v ->
                runCatching { AlertStyle.valueOf(v) }.getOrNull()
            },
            preReminderMinutes = this[K.preReminder] ?: 0,
            quranReminder = this[K.quranReminder] ?: true,
            duaReminder = this[K.duaReminder] ?: true,
            useOnlineTimes = this[K.online] ?: true,
            theme = this[K.theme].toEnum(ThemeMode.DARK),
            streakDays = this[K.streak] ?: 0,
            lastOpenEpochDay = this[K.lastOpen] ?: -1,
            silentEnabled = this[K.silentEnabled] ?: false,
            silentMode = this[K.silentMode].toEnum(SilenceMode.DO_NOT_DISTURB),
            silentMinutes = this[K.silentMinutes] ?: 20,
            silentJumuahMinutes = this[K.silentJumuah] ?: 60,
            silentPrayers = this[K.silentPrayers]
                ?.mapNotNull { runCatching { Prayer.valueOf(it) }.getOrNull() }?.filter { it.isFard }?.toSet()
                ?: Prayer.fard.toSet(),
            weeklyReport = this[K.weeklyReport] ?: true,
            dailyNote = this[K.dailyNote] ?: false,
            dailyNoteKind = this[K.dailyNoteKind].toEnum(DailyNoteKind.ALTERNATE),
            dailyNoteMinutes = this[K.dailyNoteMinutes] ?: UserPrefs.DEFAULT_DAILY_NOTE_MINUTES,
        )
    }

    private fun <V> decodeMap(raw: String?, value: (String) -> V?): Map<Prayer, V> =
        raw.orEmpty().split(';').mapNotNull { entry ->
            val (k, v) = entry.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
            val prayer = runCatching { Prayer.valueOf(k) }.getOrNull() ?: return@mapNotNull null
            value(v)?.let { prayer to it }
        }.toMap()

    private fun <V> encodeMap(map: Map<Prayer, V>): String =
        map.entries.joinToString(";") { "${it.key.name}=${it.value}" }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }

    suspend fun setName(name: String) = edit { it[K.name] = name.trim() }

    suspend fun completeOnboarding() = edit { it[K.onboarded] = true }

    /** Saves a location and, where the user has not overridden them, applies regional defaults. */
    suspend fun setLocation(location: SavedLocation) = edit { p ->
        p[K.lat] = location.point.latitude
        p[K.lon] = location.point.longitude
        p[K.place] = location.placeName
        location.countryCode?.let { p[K.country] = it } ?: p.remove(K.country)
        if (p[K.methodAuto] != false) {
            p[K.method] = RegionDefaults.methodFor(location.countryCode).name
            p[K.asr] = RegionDefaults.asrSchoolFor(location.countryCode).name
            p[K.highLat] = RegionDefaults.highLatitudeFor(location.point.latitude).name
        }
        if (p[K.hijriAuto] != false) {
            p[K.hijriAdj] = RegionDefaults.hijriAdjustmentFor(location.countryCode)
        }
    }

    suspend fun setMethod(method: Method) = edit {
        it[K.method] = method.name
        it[K.methodAuto] = false
    }

    suspend fun setMethodAuto(auto: Boolean) = edit { p ->
        p[K.methodAuto] = auto
        if (auto) {
            val country = p[K.country]
            p[K.method] = RegionDefaults.methodFor(country).name
            p[K.asr] = RegionDefaults.asrSchoolFor(country).name
        }
    }

    suspend fun setAsrSchool(school: AsrSchool) = edit { it[K.asr] = school.name }

    suspend fun setHighLatitude(rule: HighLatitude) = edit { it[K.highLat] = rule.name }

    suspend fun setOffset(prayer: Prayer, minutes: Int) = edit { p ->
        val map = decodeMap(p[K.offsets]) { it.toIntOrNull() }.toMutableMap()
        if (minutes == 0) map.remove(prayer) else map[prayer] = minutes
        p[K.offsets] = encodeMap(map)
    }

    suspend fun setHijriAdjustment(days: Int) = edit {
        it[K.hijriAdj] = days.coerceIn(-2, 2)
        it[K.hijriAuto] = false
    }

    suspend fun setHijriAuto(auto: Boolean) = edit { p ->
        p[K.hijriAuto] = auto
        if (auto) p[K.hijriAdj] = RegionDefaults.hijriAdjustmentFor(p[K.country])
    }

    suspend fun setAlert(prayer: Prayer, style: AlertStyle) = edit { p ->
        val map = UserPrefs.defaultAlerts + decodeMap(p[K.alerts]) { v ->
            runCatching { AlertStyle.valueOf(v) }.getOrNull()
        }
        p[K.alerts] = encodeMap(map + (prayer to style))
    }

    suspend fun setAllAlerts(style: AlertStyle) = edit { p ->
        p[K.alerts] = encodeMap(Prayer.fard.associateWith { style } + (Prayer.SUNRISE to AlertStyle.OFF))
    }

    suspend fun setPreReminder(minutes: Int) = edit { it[K.preReminder] = minutes }

    suspend fun setQuranReminder(on: Boolean) = edit { it[K.quranReminder] = on }

    suspend fun setDuaReminder(on: Boolean) = edit { it[K.duaReminder] = on }

    suspend fun setUseOnlineTimes(on: Boolean) = edit { it[K.online] = on }

    suspend fun setTheme(mode: ThemeMode) = edit { it[K.theme] = mode.name }

    suspend fun setSilentEnabled(on: Boolean) = edit { it[K.silentEnabled] = on }

    suspend fun setSilentMode(mode: SilenceMode) = edit { it[K.silentMode] = mode.name }

    suspend fun setSilentMinutes(minutes: Int) = edit { it[K.silentMinutes] = minutes.coerceIn(5, 120) }

    suspend fun setSilentJumuahMinutes(minutes: Int) = edit { it[K.silentJumuah] = minutes.coerceIn(5, 180) }

    suspend fun setSilentFor(prayer: Prayer, on: Boolean) = edit { p ->
        val current = p[K.silentPrayers] ?: Prayer.fard.map { it.name }.toSet()
        p[K.silentPrayers] = if (on) current + prayer.name else current - prayer.name
    }

    suspend fun setWeeklyReport(on: Boolean) = edit { it[K.weeklyReport] = on }

    suspend fun setDailyNote(on: Boolean) = edit { it[K.dailyNote] = on }

    suspend fun setDailyNoteKind(kind: DailyNoteKind) = edit { it[K.dailyNoteKind] = kind.name }

    suspend fun setDailyNoteMinutes(minutes: Int) = edit { it[K.dailyNoteMinutes] = minutes.coerceIn(0, 24 * 60 - 1) }

    /** Counts consecutive days the app was opened. */
    suspend fun recordOpen(today: LocalDate = LocalDate.now()) = edit { p ->
        val day = today.toEpochDay()
        val last = p[K.lastOpen] ?: -1
        p[K.streak] = when (last) {
            day -> p[K.streak] ?: 1
            day - 1 -> (p[K.streak] ?: 0) + 1
            else -> 1
        }
        p[K.lastOpen] = day
    }

    /**
     * Prayer ticks used to live here. They now belong to the tracker's own database;
     * this hands over any that are left, once, and clears them.
     */
    suspend fun takeLegacyPrayed(): Set<String> {
        var taken: Set<String> = emptySet()
        edit { p ->
            taken = p[K.prayed].orEmpty()
            p.remove(K.prayed)
        }
        return taken
    }
}
