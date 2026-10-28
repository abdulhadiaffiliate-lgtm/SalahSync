package com.salahsync.core

import com.salahsync.core.adhkar.Adhkar
import com.salahsync.core.adhkar.AdhkarSession
import com.salahsync.core.goals.DayMark
import com.salahsync.core.goals.DayRecord
import com.salahsync.core.goals.GoalKind
import com.salahsync.core.goals.GoalSettings
import com.salahsync.core.goals.Goals
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GoalsTest {
    // Friday 16 October to Thursday 22 October 2026, then the next week.
    private val friday = LocalDate.of(2026, 10, 16)

    private val defaults = GoalSettings()
    private val full = DayRecord(prayed = 5, onTime = 5, quranSeconds = 600, morningAdhkar = true, eveningAdhkar = true)

    private fun days(vararg offsets: Long) = offsets.map { friday.plusDays(it) }.toSet()

    @Test
    fun defaultsArePrayersQuranMorningAdhkarAndGoodDays() {
        assertEquals(listOf(GoalKind.PRAYERS, GoalKind.QURAN, GoalKind.MORNING_ADHKAR), Goals.daily(defaults, DayRecord()).map { it.kind })
        assertEquals(10, defaults.quranMinutes)
        assertEquals(5, defaults.goodDays)
    }

    @Test
    fun dailyProgressCountsPartWay() {
        val p = Goals.daily(defaults, DayRecord(prayed = 3, quranSeconds = 359)).associateBy { it.kind }
        assertEquals(3, p.getValue(GoalKind.PRAYERS).done)
        assertEquals(0.6f, p.getValue(GoalKind.PRAYERS).fraction)
        assertEquals(5, p.getValue(GoalKind.QURAN).done) // whole minutes only
        assertFalse(p.getValue(GoalKind.QURAN).met)
        assertFalse(p.getValue(GoalKind.MORNING_ADHKAR).met)
    }

    @Test
    fun aDayIsMetOnlyWhenEveryDailyGoalIs() {
        assertTrue(Goals.dayMet(defaults, full))
        assertFalse(Goals.dayMet(defaults, full.copy(prayed = 4)))
        assertFalse(Goals.dayMet(defaults, full.copy(quranSeconds = 599)))
        assertTrue(Goals.dayMet(defaults, full.copy(quranSeconds = 600, eveningAdhkar = false)))
        // On time is checked only when it is a goal.
        assertTrue(Goals.dayMet(defaults, full.copy(onTime = 2)))
        assertFalse(Goals.dayMet(defaults.copy(enabled = defaults.enabled + GoalKind.ON_TIME), full.copy(onTime = 4)))
        // With no daily goals there is nothing to meet.
        assertFalse(Goals.dayMet(GoalSettings(enabled = setOf(GoalKind.GOOD_DAYS)), full))
    }

    @Test
    fun quranTargetFollowsSettings() {
        val s = defaults.copy(quranMinutes = 20)
        assertFalse(Goals.dayMet(s, full))
        assertTrue(Goals.dayMet(s, full.copy(quranSeconds = 20 * 60)))
    }

    @Test
    fun adhkarAreDoneWhenEveryDhikrIsCountedInFull() {
        val morning = Adhkar.forSession(AdhkarSession.MORNING)
        val all = morning.associate { it.id to it.count }
        assertTrue(Goals.adhkarDone(AdhkarSession.MORNING, all))
        assertFalse(Goals.adhkarDone(AdhkarSession.MORNING, all - morning.last().id))
        assertFalse(Goals.adhkarDone(AdhkarSession.MORNING, all + (morning.first().id to morning.first().count - 1)))
        assertFalse(Goals.adhkarDone(AdhkarSession.MORNING, emptyMap()))
        // The evening set has its own dhikr, so a full morning is not a full evening.
        assertFalse(Goals.adhkarDone(AdhkarSession.EVENING, all))
    }

    @Test
    fun streakCountsMetDaysAndTodayNeverBreaksIt() {
        val today = friday.plusDays(4)
        assertEquals(4, Goals.streak(today, friday, days(0, 1, 2, 3)))
        assertEquals(5, Goals.streak(today, friday, days(0, 1, 2, 3, 4)))
        assertEquals(0, Goals.streak(today, today, emptySet()))
        assertEquals(1, Goals.streak(today, today, days(4)))
    }

    @Test
    fun oneMissedDayAWeekIsLetGo() {
        val today = friday.plusDays(6)
        // Missed Monday only: the run carries on through it.
        assertEquals(5, Goals.streak(today, friday, days(0, 1, 2, 4, 5)))
        // Missed yesterday: still alive.
        assertEquals(5, Goals.streak(today, friday, days(0, 1, 2, 3, 4)))
        // Two missed days in the same week end it at the second: Wednesday, Tuesday and Sunday count.
        assertEquals(3, Goals.streak(today, friday, days(0, 2, 4, 5)))
    }

    @Test
    fun theGraceIsPerWeek() {
        // Thursday of week one missed, Saturday of week two missed: both forgiven.
        val today = friday.plusDays(9) // Sunday of week two
        val met = days(0, 1, 2, 3, 4, 5, 7, 9)
        assertEquals(8, Goals.streak(today, friday, met))
    }

    @Test
    fun streakStopsWhereGoalsBegan() {
        val since = friday.plusDays(2)
        val today = friday.plusDays(5)
        assertEquals(3, Goals.streak(today, since, days(0, 1, 2, 3, 4)))
        // A missed day must not reach back before the start.
        assertEquals(2, Goals.streak(today, since, days(0, 1, 3, 4)))
    }

    @Test
    fun weekMarksAndGoodDays() {
        val today = friday.plusDays(3)
        val g = Goals.build(today, friday.plusDays(1), defaults, DayRecord(prayed = 5), days(0, 1, 2), qadaMadeUpThisWeek = 0)
        assertEquals(friday, g.week.first().first)
        assertEquals(
            listOf(DayMark.NONE, DayMark.MET, DayMark.MET, DayMark.OPEN, DayMark.NONE, DayMark.NONE, DayMark.NONE),
            g.week.map { it.second },
        )
        val good = g.weekly.single { it.kind == GoalKind.GOOD_DAYS }
        assertEquals(2, good.done)
        assertEquals(5, good.target)
        assertEquals(2, g.streak)
        assertFalse(g.allMet)
        assertEquals(1, g.dailyMet)
    }

    @Test
    fun todayIsJudgedLiveNotFromTheRecord() {
        val today = friday.plusDays(2)
        // Recorded as met earlier, but a prayer has since been unticked.
        val undone = Goals.build(today, friday, defaults, full.copy(prayed = 4), days(0, 1, 2), 0)
        assertEquals(DayMark.OPEN, undone.week[2].second)
        assertEquals(2, undone.streak)
        val done = Goals.build(today, friday, defaults, full, days(0, 1), 0)
        assertEquals(DayMark.MET, done.week[2].second)
        assertEquals(3, done.streak)
        assertTrue(done.allMet)
        assertEquals(1f, done.fraction)
    }

    @Test
    fun missedPastDaysAreMarked() {
        val today = friday.plusDays(3)
        val g = Goals.build(today, friday, defaults, DayRecord(), days(0, 2), 0)
        assertEquals(DayMark.MISSED, g.week[1].second)
    }

    @Test
    fun qadaGoalCountsThisWeeksMadeUpPrayers() {
        val s = defaults.copy(enabled = setOf(GoalKind.PRAYERS, GoalKind.QADA), qadaPerWeek = 7)
        val g = Goals.build(friday, friday, s, DayRecord(), emptySet(), qadaMadeUpThisWeek = 3)
        val qada = g.weekly.single()
        assertEquals(GoalKind.QADA, qada.kind)
        assertEquals(3, qada.done)
        assertFalse(qada.met)
        assertTrue(Goals.build(friday, friday, s, DayRecord(), emptySet(), 9).weekly.single().met)
    }

    @Test
    fun ringIsTheAverageOfTodaysGoals() {
        val g = Goals.build(friday, friday, defaults, DayRecord(prayed = 5, quranSeconds = 300), emptySet(), 0)
        // Prayers 1, Quran 0.5, adhkar 0.
        assertEquals(0.5f, g.fraction, 0.0001f)
        val none = Goals.build(friday, friday, GoalSettings(enabled = emptySet()), full, emptySet(), 0)
        assertEquals(0f, none.fraction)
        assertTrue(none.daily.isEmpty())
        assertTrue(none.weekly.isEmpty())
        assertEquals(0, none.streak)
    }
}
