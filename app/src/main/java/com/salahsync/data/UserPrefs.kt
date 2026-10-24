package com.salahsync.data

import com.salahsync.core.AsrSchool
import com.salahsync.core.GeoPoint
import com.salahsync.core.HighLatitude
import com.salahsync.core.Method
import com.salahsync.core.Prayer
import com.salahsync.core.PrayerSettings
import com.salahsync.core.SilenceMode

enum class ThemeMode(val displayName: String) {
    DARK("Dark"), LIGHT("Light"), SYSTEM("Follow system")
}

/** How a prayer announces itself. */
enum class AlertStyle(val displayName: String) {
    ADHAN("Adhan"),
    NOTIFICATION("Notification sound"),
    SILENT("Silent notification"),
    OFF("Off");

    fun next(): AlertStyle = entries[(ordinal + 1) % entries.size]
}

data class SavedLocation(
    val point: GeoPoint,
    val placeName: String,
    val countryCode: String?,
)

data class UserPrefs(
    val name: String = "",
    val onboarded: Boolean = false,
    val location: SavedLocation? = null,
    val methodAuto: Boolean = true,
    val method: Method = Method.MUSLIM_WORLD_LEAGUE,
    val asrSchool: AsrSchool = AsrSchool.SHAFII,
    val highLatitude: HighLatitude = HighLatitude.MIDDLE_OF_THE_NIGHT,
    val offsets: Map<Prayer, Int> = emptyMap(),
    val hijriAuto: Boolean = true,
    val hijriAdjustment: Int = 0,
    val alerts: Map<Prayer, AlertStyle> = defaultAlerts,
    val preReminderMinutes: Int = 0,
    val quranReminder: Boolean = true,
    val duaReminder: Boolean = true,
    val useOnlineTimes: Boolean = true,
    val theme: ThemeMode = ThemeMode.DARK,
    val streakDays: Int = 0,
    val lastOpenEpochDay: Long = -1,
    val silentEnabled: Boolean = false,
    val silentMode: SilenceMode = SilenceMode.DO_NOT_DISTURB,
    val silentMinutes: Int = 20,
    val silentJumuahMinutes: Int = 60,
    val silentPrayers: Set<Prayer> = Prayer.fard.toSet(),
    val weeklyReport: Boolean = true,
) {
    val prayerSettings: PrayerSettings
        get() = PrayerSettings(method, asrSchool, highLatitude, offsets)

    fun alertFor(prayer: Prayer): AlertStyle = alerts[prayer] ?: AlertStyle.OFF

    companion object {
        val silentMinuteOptions = listOf(10, 15, 20, 30, 45, 60)
        val jumuahMinuteOptions = listOf(30, 45, 60, 75, 90)

        val defaultAlerts: Map<Prayer, AlertStyle> = Prayer.entries.associateWith {
            if (it == Prayer.SUNRISE) AlertStyle.OFF else AlertStyle.ADHAN
        }
    }
}
