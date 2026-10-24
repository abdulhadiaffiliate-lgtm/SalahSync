package com.salahsync.core

/**
 * Picks sensible defaults from the user's country (ISO 3166-1 alpha-2), so a new
 * user gets locally familiar times without opening settings.
 */
object RegionDefaults {

    private val southAsia = setOf("PK", "IN", "BD", "AF", "NP", "LK")

    fun methodFor(countryCode: String?): Method = when (countryCode?.uppercase()) {
        "PK", "IN", "BD", "AF", "NP", "LK" -> Method.KARACHI
        "SA", "YE", "BH", "OM" -> Method.UMM_AL_QURA
        "AE" -> Method.DUBAI
        "KW" -> Method.KUWAIT
        "QA" -> Method.QATAR
        "EG", "SD", "LY", "SY", "LB", "IQ", "JO", "PS" -> Method.EGYPTIAN
        "US", "CA" -> Method.NORTH_AMERICA
        "SG", "MY", "ID", "BN" -> Method.SINGAPORE
        "GB", "IE" -> Method.MOON_SIGHTING_COMMITTEE
        else -> Method.MUSLIM_WORLD_LEAGUE
    }

    /** Hanafi Asr is the common practice across South Asia, Turkey and Central Asia. */
    fun asrSchoolFor(countryCode: String?): AsrSchool = when (countryCode?.uppercase()) {
        in southAsia, "TR", "UZ", "TJ", "KZ", "KG", "TM", "AZ" -> AsrSchool.HANAFI
        else -> AsrSchool.SHAFII
    }

    /**
     * Days to add to the Umm al-Qura Hijri date. Moon sighting in South Asia usually
     * runs one day behind Makkah.
     */
    fun hijriAdjustmentFor(countryCode: String?): Int =
        if (countryCode?.uppercase() in southAsia) -1 else 0

    /** Places far from the equator need a high-latitude rule for Fajr and Isha. */
    fun highLatitudeFor(latitude: Double): HighLatitude =
        if (kotlin.math.abs(latitude) >= 48.0) HighLatitude.SEVENTH_OF_THE_NIGHT
        else HighLatitude.MIDDLE_OF_THE_NIGHT
}
