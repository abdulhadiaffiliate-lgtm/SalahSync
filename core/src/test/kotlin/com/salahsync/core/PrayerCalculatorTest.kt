package com.salahsync.core

import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PrayerCalculatorTest {
    private val lahore = GeoPoint(31.5204, 74.3587)
    private val karachiZone = ZoneId.of("Asia/Karachi")
    private val date = LocalDate.of(2026, 10, 23)

    private fun times(school: AsrSchool, offsets: Map<Prayer, Int> = emptyMap()) =
        PrayerCalculator.calculate(
            lahore, date, karachiZone,
            PrayerSettings(method = Method.KARACHI, asrSchool = school, offsetsMinutes = offsets),
        )

    @Test
    fun timesAreInDailyOrder() {
        val t = times(AsrSchool.HANAFI)
        Prayer.entries.zipWithNext().forEach { (a, b) ->
            assertTrue(t[a].isBefore(t[b]), "$a should be before $b")
        }
    }

    @Test
    fun timesFallOnTheRequestedLocalDate() {
        val t = times(AsrSchool.HANAFI)
        Prayer.entries.forEach { assertEquals(date, t[it].toLocalDate(), "$it") }
    }

    @Test
    fun hanafiAsrIsLaterThanShafii() {
        val gap = Duration.between(times(AsrSchool.SHAFII)[Prayer.ASR], times(AsrSchool.HANAFI)[Prayer.ASR])
        assertTrue(gap.toMinutes() in 30..90, "Hanafi Asr gap was ${gap.toMinutes()} min")
    }

    @Test
    fun lahoreTimesAreInTheExpectedWindows() {
        val t = times(AsrSchool.HANAFI)
        fun hm(p: Prayer) = t[p].hour * 60 + t[p].minute
        assertTrue(hm(Prayer.FAJR) in (4 * 60 + 30)..(5 * 60 + 15), "Fajr ${t[Prayer.FAJR]}")
        assertTrue(hm(Prayer.DHUHR) in (11 * 60 + 45)..(12 * 60 + 5), "Dhuhr ${t[Prayer.DHUHR]}")
        assertTrue(hm(Prayer.MAGHRIB) in (17 * 60 + 20)..(17 * 60 + 50), "Maghrib ${t[Prayer.MAGHRIB]}")
    }

    @Test
    fun offsetsShiftOnlyTheirPrayer() {
        val base = times(AsrSchool.HANAFI)
        val shifted = times(AsrSchool.HANAFI, mapOf(Prayer.ISHA to 10))
        assertEquals(10, Duration.between(base[Prayer.ISHA], shifted[Prayer.ISHA]).toMinutes())
        assertEquals(base[Prayer.FAJR], shifted[Prayer.FAJR])
    }

    @Test
    fun nextAndCurrentPrayer() {
        val t = times(AsrSchool.HANAFI)
        val justAfterDhuhr = t[Prayer.DHUHR].plusMinutes(1).toInstant()
        assertEquals(Prayer.ASR, t.next(justAfterDhuhr))
        assertEquals(Prayer.DHUHR, t.current(justAfterDhuhr))
        assertNull(t.next(t[Prayer.ISHA].plusMinutes(1).toInstant()))
        assertNull(t.current(t[Prayer.FAJR].minusMinutes(1).toInstant()))
    }

    @Test
    fun fridayDhuhrIsJumuah() {
        val t = times(AsrSchool.HANAFI)
        assertTrue(t.isFriday)
        assertEquals("Jumu'ah", Prayer.DHUHR.labelOn(t.isFriday))
        assertEquals("Dhuhr", Prayer.DHUHR.labelOn(false))
    }
}
