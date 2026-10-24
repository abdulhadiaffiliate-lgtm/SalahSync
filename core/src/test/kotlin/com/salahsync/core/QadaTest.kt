package com.salahsync.core

import com.salahsync.core.tracker.QadaEntry
import com.salahsync.core.tracker.QadaKind
import com.salahsync.core.tracker.QadaLedger
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class QadaTest {
    private val start = LocalDate.of(2026, 10, 20)

    @Test
    fun nothingIsMissedOnTheFirstDay() {
        val missed = QadaLedger.missed(start, start, emptySet())
        assertEquals(Prayer.fard.associateWith { 0 }, missed)
    }

    @Test
    fun unmarkedPrayersOfEndedDaysAreMissed() {
        val today = start.plusDays(3)
        val prayed = Prayer.fard.map { start to it }.toSet() + (start.plusDays(1) to Prayer.FAJR) + (today to Prayer.ASR)
        val missed = QadaLedger.missed(start, today, prayed)
        assertEquals(1, missed[Prayer.FAJR]) // day 3 only
        assertEquals(2, missed[Prayer.ASR]) // today's tick does not count against ended days
        assertEquals(2, missed[Prayer.ISHA])
        assertEquals(null, missed[Prayer.SUNRISE])
    }

    @Test
    fun ticksBeforeTrackingBeganAreIgnored() {
        val missed = QadaLedger.missed(start, start.plusDays(1), setOf(start.minusDays(1) to Prayer.DHUHR))
        assertEquals(1, missed[Prayer.DHUHR])
    }

    @Test
    fun owedAddsEarlierMissedAndSubtractsMadeUp() {
        val today = start.plusDays(2)
        val entries = listOf(
            QadaEntry(Prayer.FAJR, QadaKind.ADDED, 10),
            QadaEntry(Prayer.FAJR, QadaKind.MADE_UP, 1),
            QadaEntry(Prayer.FAJR, QadaKind.MADE_UP, 1),
            QadaEntry(Prayer.MAGHRIB, QadaKind.MADE_UP, 5),
        )
        val counts = QadaLedger.counts(start, today, emptySet(), entries)
        assertEquals(10, counts.getValue(Prayer.FAJR).owed) // 2 missed + 10 earlier - 2 made up
        assertEquals(0, counts.getValue(Prayer.MAGHRIB).owed) // never below zero
        assertEquals(2, counts.getValue(Prayer.ISHA).owed)
    }
}
