package com.salahsync.core

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.HighLatitudeRule
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** One day's prayer times, in the zone they were calculated for. */
data class DayTimes(
    val date: LocalDate,
    val zone: ZoneId,
    val times: Map<Prayer, ZonedDateTime>,
) {
    operator fun get(prayer: Prayer): ZonedDateTime = times.getValue(prayer)

    val isFriday: Boolean get() = date.dayOfWeek == java.time.DayOfWeek.FRIDAY

    /** The first prayer (including sunrise) strictly after [now], or null after Isha. */
    fun next(now: Instant): Prayer? =
        Prayer.entries.firstOrNull { times.getValue(it).toInstant().isAfter(now) }

    /** The prayer whose time has most recently begun, or null before Fajr. */
    fun current(now: Instant): Prayer? =
        Prayer.entries.lastOrNull { !times.getValue(it).toInstant().isAfter(now) }
}

/** Offline prayer-time calculator built on the Adhan astronomical library. */
object PrayerCalculator {

    fun calculate(
        location: GeoPoint,
        date: LocalDate,
        zone: ZoneId,
        settings: PrayerSettings,
    ): DayTimes {
        val params = settings.method.toAdhan().parameters.apply {
            madhab = when (settings.asrSchool) {
                AsrSchool.SHAFII -> Madhab.SHAFI
                AsrSchool.HANAFI -> Madhab.HANAFI
            }
            highLatitudeRule = when (settings.highLatitude) {
                HighLatitude.MIDDLE_OF_THE_NIGHT -> HighLatitudeRule.MIDDLE_OF_THE_NIGHT
                HighLatitude.SEVENTH_OF_THE_NIGHT -> HighLatitudeRule.SEVENTH_OF_THE_NIGHT
                HighLatitude.TWILIGHT_ANGLE -> HighLatitudeRule.TWILIGHT_ANGLE
            }
            adjustments.fajr = settings.offsetsMinutes[Prayer.FAJR] ?: 0
            adjustments.sunrise = settings.offsetsMinutes[Prayer.SUNRISE] ?: 0
            adjustments.dhuhr = settings.offsetsMinutes[Prayer.DHUHR] ?: 0
            adjustments.asr = settings.offsetsMinutes[Prayer.ASR] ?: 0
            adjustments.maghrib = settings.offsetsMinutes[Prayer.MAGHRIB] ?: 0
            adjustments.isha = settings.offsetsMinutes[Prayer.ISHA] ?: 0
        }
        val adhan = PrayerTimes(
            Coordinates(location.latitude, location.longitude),
            DateComponents(date.year, date.monthValue, date.dayOfMonth),
            params,
        )
        fun z(d: java.util.Date) = d.toInstant().atZone(zone)
        return DayTimes(
            date = date,
            zone = zone,
            times = linkedMapOf(
                Prayer.FAJR to z(adhan.fajr),
                Prayer.SUNRISE to z(adhan.sunrise),
                Prayer.DHUHR to z(adhan.dhuhr),
                Prayer.ASR to z(adhan.asr),
                Prayer.MAGHRIB to z(adhan.maghrib),
                Prayer.ISHA to z(adhan.isha),
            ),
        )
    }

    private fun Method.toAdhan(): CalculationMethod = when (this) {
        Method.MUSLIM_WORLD_LEAGUE -> CalculationMethod.MUSLIM_WORLD_LEAGUE
        Method.KARACHI -> CalculationMethod.KARACHI
        Method.NORTH_AMERICA -> CalculationMethod.NORTH_AMERICA
        Method.EGYPTIAN -> CalculationMethod.EGYPTIAN
        Method.UMM_AL_QURA -> CalculationMethod.UMM_AL_QURA
        Method.DUBAI -> CalculationMethod.DUBAI
        Method.KUWAIT -> CalculationMethod.KUWAIT
        Method.QATAR -> CalculationMethod.QATAR
        Method.SINGAPORE -> CalculationMethod.SINGAPORE
        Method.MOON_SIGHTING_COMMITTEE -> CalculationMethod.MOON_SIGHTING_COMMITTEE
    }
}
