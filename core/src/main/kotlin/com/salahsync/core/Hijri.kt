package com.salahsync.core

import java.time.LocalDate
import java.time.YearMonth
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

data class HijriDate(val day: Int, val month: Int, val year: Int) {
    val monthName: String get() = MONTHS[month - 1]
    val monthNameArabic: String get() = MONTHS_AR[month - 1]

    override fun toString(): String = "$day $monthName $year"

    companion object {
        val MONTHS = listOf(
            "Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani",
            "Jumada al-Ula", "Jumada al-Akhirah", "Rajab", "Sha'ban",
            "Ramadan", "Shawwal", "Dhu al-Qa'dah", "Dhu al-Hijjah",
        )
        val MONTHS_AR = listOf(
            "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
            "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
            "رمضان", "شوال", "ذو القعدة", "ذو الحجة",
        )
    }
}

object Hijri {
    /**
     * Converts with the Umm al-Qura calendar built into java.time, shifted by
     * [adjustmentDays] to follow local moon sighting.
     */
    fun of(date: LocalDate, adjustmentDays: Int = 0): HijriDate {
        val h = HijrahDate.from(date.plusDays(adjustmentDays.toLong()))
        return HijriDate(
            day = h.get(ChronoField.DAY_OF_MONTH),
            month = h.get(ChronoField.MONTH_OF_YEAR),
            year = h.get(ChronoField.YEAR),
        )
    }

    /**
     * The Hijri months a Gregorian month runs across, for a calendar header:
     * "Rabi al-Thani – Jumada al-Ula 1448", or with both years when it crosses one.
     */
    fun spanOf(month: YearMonth, adjustmentDays: Int = 0): String =
        span(of(month.atDay(1), adjustmentDays), of(month.atEndOfMonth(), adjustmentDays))

    fun span(first: HijriDate, last: HijriDate): String =
        when {
            first.month == last.month && first.year == last.year -> "${first.monthName} ${first.year}"
            first.year == last.year -> "${first.monthName} – ${last.monthName} ${last.year}"
            else -> "${first.monthName} ${first.year} – ${last.monthName} ${last.year}"
        }
}
