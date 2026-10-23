package com.salahsync.core

/** The daily times SalahSync shows. Sunrise is informational and never gets an adhan. */
enum class Prayer(val displayName: String, val arabicName: String, val isFard: Boolean) {
    FAJR("Fajr", "الفجر", true),
    SUNRISE("Sunrise", "الشروق", false),
    DHUHR("Dhuhr", "الظهر", true),
    ASR("Asr", "العصر", true),
    MAGHRIB("Maghrib", "المغرب", true),
    ISHA("Isha", "العشاء", true);

    /** On Fridays Dhuhr is shown as Jumu'ah. */
    fun labelOn(isFriday: Boolean): String =
        if (this == DHUHR && isFriday) "Jumu'ah" else displayName

    fun arabicLabelOn(isFriday: Boolean): String =
        if (this == DHUHR && isFriday) "الجمعة" else arabicName

    companion object {
        val fard: List<Prayer> = entries.filter { it.isFard }
    }
}
