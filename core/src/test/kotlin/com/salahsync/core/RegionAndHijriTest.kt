package com.salahsync.core

import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RegionAndHijriTest {

    @Test
    fun methodDefaultsByCountry() {
        assertEquals(Method.KARACHI, RegionDefaults.methodFor("pk"))
        assertEquals(Method.UMM_AL_QURA, RegionDefaults.methodFor("SA"))
        assertEquals(Method.NORTH_AMERICA, RegionDefaults.methodFor("US"))
        assertEquals(Method.MUSLIM_WORLD_LEAGUE, RegionDefaults.methodFor(null))
    }

    @Test
    fun asrSchoolDefaultsByCountry() {
        assertEquals(AsrSchool.HANAFI, RegionDefaults.asrSchoolFor("PK"))
        assertEquals(AsrSchool.HANAFI, RegionDefaults.asrSchoolFor("TR"))
        assertEquals(AsrSchool.SHAFII, RegionDefaults.asrSchoolFor("EG"))
    }

    @Test
    fun southAsiaHijriIsOneDayBehind() {
        assertEquals(-1, RegionDefaults.hijriAdjustmentFor("PK"))
        assertEquals(0, RegionDefaults.hijriAdjustmentFor("SA"))
        val date = LocalDate.of(2026, 10, 23)
        val makkah = Hijri.of(date)
        val lahore = Hijri.of(date, RegionDefaults.hijriAdjustmentFor("PK"))
        assertEquals(Hijri.of(date.minusDays(1)), lahore)
        assertTrue(makkah != lahore)
    }

    @Test
    fun hijriConversionKnownDate() {
        // 1 Ramadan 1447 in the Umm al-Qura calendar is 18 February 2026.
        assertEquals(HijriDate(1, 9, 1447), Hijri.of(LocalDate.of(2026, 2, 18)))
        assertEquals("Ramadan", HijriDate(1, 9, 1447).monthName)
    }

    @Test
    fun hijriSpanOfGregorianMonth() {
        // February 2026 runs from Sha'ban into Ramadan 1447.
        assertEquals("Sha'ban – Ramadan 1447", Hijri.spanOf(YearMonth.of(2026, 2)))
        val crossesYear = Hijri.spanOf(YearMonth.of(2026, 6))
        assertTrue(crossesYear.contains("1447") && crossesYear.contains("1448"), crossesYear)
    }

    @Test
    fun qiblaFromLahorePointsWestSouthWest() {
        val bearing = QiblaDirection.bearingFrom(GeoPoint(31.5204, 74.3587))
        assertTrue(bearing in 250.0..262.0, "bearing was $bearing")
        val km = QiblaDirection.distanceKmFrom(GeoPoint(31.5204, 74.3587))
        assertTrue(km in 3500.0..3800.0, "distance was $km")
    }

    @Test
    fun qiblaFromNewYorkMatchesPublishedValue() {
        val bearing = QiblaDirection.bearingFrom(GeoPoint(40.7128, -74.0059))
        assertEquals(58.48, bearing, 0.1)
    }
}
