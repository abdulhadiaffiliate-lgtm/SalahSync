package com.salahsync.core

/** Calculation methods offered in settings, mapped onto the Adhan library. */
enum class Method(val displayName: String) {
    MUSLIM_WORLD_LEAGUE("Muslim World League"),
    KARACHI("University of Islamic Sciences, Karachi"),
    NORTH_AMERICA("ISNA (North America)"),
    EGYPTIAN("Egyptian General Authority"),
    UMM_AL_QURA("Umm al-Qura, Makkah"),
    DUBAI("Dubai"),
    KUWAIT("Kuwait"),
    QATAR("Qatar"),
    SINGAPORE("Singapore, Malaysia, Indonesia"),
    MOON_SIGHTING_COMMITTEE("Moonsighting Committee"),
}

enum class AsrSchool(val displayName: String) {
    /** Shafi'i, Maliki and Hanbali: shadow equals object length. */
    SHAFII("Shafi'i, Maliki, Hanbali"),

    /** Hanafi: shadow twice the object length, so Asr comes later. */
    HANAFI("Hanafi"),
}

enum class HighLatitude(val displayName: String) {
    MIDDLE_OF_THE_NIGHT("Middle of the night"),
    SEVENTH_OF_THE_NIGHT("One-seventh of the night"),
    TWILIGHT_ANGLE("Twilight angle"),
}

data class PrayerSettings(
    val method: Method = Method.MUSLIM_WORLD_LEAGUE,
    val asrSchool: AsrSchool = AsrSchool.SHAFII,
    val highLatitude: HighLatitude = HighLatitude.MIDDLE_OF_THE_NIGHT,
    /** Per-prayer offsets in minutes, to match a local mosque timetable. */
    val offsetsMinutes: Map<Prayer, Int> = emptyMap(),
)

data class GeoPoint(val latitude: Double, val longitude: Double) {
    init {
        require(latitude in -90.0..90.0) { "latitude out of range: $latitude" }
        require(longitude in -180.0..180.0) { "longitude out of range: $longitude" }
    }
}
