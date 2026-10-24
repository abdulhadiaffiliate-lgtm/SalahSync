package com.salahsync.core

import com.salahsync.core.tracker.PrayerStatus
import com.salahsync.core.tracker.PrayerWindows
import com.salahsync.core.tracker.WeeklyReports
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WeeklyReportTest {
    private val zone = ZoneId.of("Asia/Karachi")

    // Friday 16 October to Thursday 22 October 2026.
    private val friday = LocalDate.of(2026, 10, 16)

    private fun all(date: LocalDate) = Prayer.fard.map { date to it }.toSet()

    private fun day(date: LocalDate) = DayTimes(
        date, zone,
        mapOf(
            Prayer.FAJR to LocalTime.of(5, 0), Prayer.SUNRISE to LocalTime.of(6, 20), Prayer.DHUHR to LocalTime.of(12, 5),
            Prayer.ASR to LocalTime.of(15, 30), Prayer.MAGHRIB to LocalTime.of(17, 40), Prayer.ISHA to LocalTime.of(19, 0),
        ).mapValues { it.value.atDate(date).atZone(zone) },
    )

    @Test
    fun weeksRunFridayToThursday() {
        assertEquals(friday, WeeklyReports.weekStart(friday))
        assertEquals(friday, WeeklyReports.weekStart(friday.plusDays(6)))
        assertEquals(friday.plusWeeks(1), WeeklyReports.weekStart(friday.plusDays(7)))
        assertEquals(DayOfWeek.FRIDAY, WeeklyReports.lastCompleteWeekStart(LocalDate.of(2026, 10, 28)).dayOfWeek)
        assertEquals(friday, WeeklyReports.lastCompleteWeekStart(friday.plusDays(7)))
    }

    @Test
    fun countsOnTimeLateAndMissed() {
        val today = friday.plusDays(7)
        val prayed = (0L..5L).flatMap { all(friday.plusDays(it)) }.toSet() - (friday.plusDays(2) to Prayer.FAJR)
        val late = setOf(friday to Prayer.ASR, friday.plusDays(1) to Prayer.ISHA)
        val r = WeeklyReports.build(friday, today, friday, prayed, late, qadaOwed = 6)
        assertEquals(27, r.totals.onTime)
        assertEquals(2, r.totals.late)
        assertEquals(6, r.totals.missed) // all of Thursday, plus one Fajr
        assertEquals(35, r.totals.tracked)
        assertEquals(2, r.byPrayer.getValue(Prayer.FAJR).missed)
        assertEquals(1, r.byPrayer.getValue(Prayer.ASR).late)
        assertEquals(6, r.qadaOwed)
        assertFalse(r.inProgress)
    }

    @Test
    fun daysBeforeTrackingAndTodayAreNotMissed() {
        val since = friday.plusDays(3)
        val today = friday.plusDays(4)
        val r = WeeklyReports.build(friday, today, since, all(since), emptySet(), 0)
        assertEquals(PrayerStatus.UNTRACKED, r.days[0].statuses[Prayer.FAJR])
        assertEquals(PrayerStatus.ON_TIME, r.days[3].statuses[Prayer.FAJR])
        assertEquals(PrayerStatus.PENDING, r.days[4].statuses[Prayer.FAJR])
        assertEquals(PrayerStatus.UNTRACKED, r.days[5].statuses[Prayer.FAJR])
        assertEquals(5, r.totals.tracked)
        assertTrue(r.inProgress)
    }

    @Test
    fun streaksIgnoreAnUnfinishedToday() {
        val since = friday
        val today = friday.plusDays(6)
        // Complete on days 0-1, a gap on day 2, complete on days 3-5, today unfinished.
        val prayed = listOf(0L, 1L, 3L, 4L, 5L).flatMap { all(friday.plusDays(it)) }.toSet() + (today to Prayer.FAJR)
        val late = setOf(friday.plusDays(4) to Prayer.DHUHR)
        val s = WeeklyReports.streaks(today, since, prayed, late)
        assertEquals(3, s.current)
        assertEquals(3, s.best)
        assertEquals(1, s.onTime) // day 5 only; day 4 had a late Dhuhr

        val finished = WeeklyReports.streaks(today, since, prayed + all(today), late)
        assertEquals(4, finished.current)
        assertEquals(4, finished.best)
    }

    @Test
    fun prayerTimeEndsAtTheNextPrayer() {
        val d = day(friday)
        val next = day(friday.plusDays(1))
        assertEquals(d[Prayer.SUNRISE], PrayerWindows.endOf(Prayer.FAJR, d, next))
        assertEquals(d[Prayer.ASR], PrayerWindows.endOf(Prayer.DHUHR, d, next))
        assertEquals(next[Prayer.FAJR], PrayerWindows.endOf(Prayer.ISHA, d, next))
        assertFalse(PrayerWindows.isLate(Prayer.ASR, d, next, d[Prayer.ASR].plusMinutes(30)))
        assertTrue(PrayerWindows.isLate(Prayer.ASR, d, next, d[Prayer.MAGHRIB].plusMinutes(1)))
        assertFalse(PrayerWindows.isLate(Prayer.ISHA, d, next, d[Prayer.ISHA].plusHours(6)))
    }

    @Test
    fun silenceWindowsUseJumuahLengthAndNeverOverlap() {
        val d = day(friday)
        val w = SilenceWindows.forDay(d, setOf(Prayer.DHUHR, Prayer.MAGHRIB, Prayer.ISHA), minutes = 90, jumuahMinutes = 60)
        assertEquals(listOf(Prayer.DHUHR, Prayer.MAGHRIB, Prayer.ISHA), w.map { it.prayer })
        assertEquals("Jumu'ah", w[0].label)
        assertEquals(d[Prayer.DHUHR].plusMinutes(60), w[0].end)
        assertEquals(d[Prayer.ISHA], w[1].end) // cut short where Isha begins
        assertEquals(d[Prayer.ISHA].plusMinutes(90), w[2].end)
        assertTrue(w[0].contains(d[Prayer.DHUHR].plusMinutes(5).toInstant()))
        assertFalse(w[0].contains(w[0].end.toInstant()))

        val saturday = SilenceWindows.forDay(day(friday.plusDays(1)), setOf(Prayer.DHUHR), 20, 60)
        assertEquals("Dhuhr", saturday.single().label)
        assertEquals(20, java.time.Duration.between(saturday.single().start, saturday.single().end).toMinutes())
    }

    @Test
    fun soundIsOnlyRestoredWhenTheUserLeftItAlone() {
        val previous = SoundState(ringerMode = 2, interruptionFilter = 1)
        val applied = SoundState(ringerMode = 0, interruptionFilter = 2)
        assertTrue(SoundRestore.shouldRestore(SilenceMode.DO_NOT_DISTURB, applied, applied))
        assertFalse(SoundRestore.shouldRestore(SilenceMode.DO_NOT_DISTURB, applied, applied.copy(interruptionFilter = 1)))
        assertTrue(SoundRestore.shouldRestore(SilenceMode.SILENT, applied, applied.copy(interruptionFilter = 1)))
        assertFalse(SoundRestore.shouldRestore(SilenceMode.VIBRATE, applied, previous))
    }
}
