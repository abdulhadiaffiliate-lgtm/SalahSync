package com.salahsync.core.goals

import com.salahsync.core.adhkar.Adhkar
import com.salahsync.core.adhkar.AdhkarSession
import com.salahsync.core.tracker.WeeklyReports
import java.time.LocalDate

/** The goals a user can set. Each is worked out from what the app already records. */
enum class GoalKind(val title: String, val weekly: Boolean = false) {
    /** All five fard prayers ticked in the prayer tracker. */
    PRAYERS("Pray all five"),

    /** All five ticked, none of them late. */
    ON_TIME("Pray all five on time"),

    /** Minutes in the Quran reader, plus any added by hand for reading from a mushaf. */
    QURAN("Read Quran"),

    /** Every morning dhikr counted to the end on the adhkar screen. */
    MORNING_ADHKAR("Morning adhkar"),
    EVENING_ADHKAR("Evening adhkar"),

    /** Days in the week on which every daily goal was met. */
    GOOD_DAYS("Meet your daily goals", weekly = true),

    /** Qada prayers logged as made up in the prayer tracker this week. */
    QADA("Make up Qada prayers", weekly = true),
}

data class GoalSettings(
    val enabled: Set<GoalKind> = DEFAULT_ENABLED,
    val quranMinutes: Int = 10,
    val goodDays: Int = 5,
    val qadaPerWeek: Int = 7,
) {
    fun isOn(kind: GoalKind): Boolean = kind in enabled

    companion object {
        val DEFAULT_ENABLED = setOf(GoalKind.PRAYERS, GoalKind.QURAN, GoalKind.MORNING_ADHKAR, GoalKind.GOOD_DAYS)
        val quranMinuteRange = 5..60
        const val QURAN_MINUTE_STEP = 5
        val goodDayRange = 1..7
        val qadaRange = 1..35
    }
}

/** What happened on one day, read from the tracker, the adhkar counts and the reader. */
data class DayRecord(
    val prayed: Int = 0,
    val onTime: Int = 0,
    val quranSeconds: Int = 0,
    val morningAdhkar: Boolean = false,
    val eveningAdhkar: Boolean = false,
)

data class GoalProgress(val kind: GoalKind, val done: Int, val target: Int) {
    val met: Boolean get() = done >= target
    val fraction: Float get() = if (target <= 0) 1f else (done.toFloat() / target).coerceIn(0f, 1f)
}

enum class DayMark {
    /** Every daily goal was met. */
    MET,

    /** A past day, after goals began, on which some goal was not met. */
    MISSED,

    /** Today, not finished yet: it never counts as missed. */
    OPEN,

    /** Before goals began, or still to come. */
    NONE,
}

data class GoalsDay(
    val today: LocalDate,
    val settings: GoalSettings,
    val daily: List<GoalProgress>,
    val weekly: List<GoalProgress>,
    /** Friday to Thursday, as in the weekly report. */
    val week: List<Pair<LocalDate, DayMark>>,
    val streak: Int,
) {
    val dailyMet: Int get() = daily.count { it.met }
    val allMet: Boolean get() = daily.isNotEmpty() && daily.all { it.met }

    /** How far through today's goals the user is, for the progress ring. Partial goals count in part. */
    val fraction: Float get() = if (daily.isEmpty()) 0f else daily.sumOf { it.fraction.toDouble() }.toFloat() / daily.size
}

object Goals {
    /** Daily goals, in the order they are shown. */
    val dailyKinds: List<GoalKind> = GoalKind.entries.filter { !it.weekly }
    val weeklyKinds: List<GoalKind> = GoalKind.entries.filter { it.weekly }

    /** True once every dhikr of [session] has been counted in full. */
    fun adhkarDone(session: AdhkarSession, counts: Map<String, Int>): Boolean =
        Adhkar.forSession(session).all { (counts[it.id] ?: 0) >= it.count }

    fun daily(settings: GoalSettings, day: DayRecord): List<GoalProgress> = dailyKinds.filter(settings::isOn).map { kind ->
        when (kind) {
            GoalKind.PRAYERS -> GoalProgress(kind, day.prayed, 5)
            GoalKind.ON_TIME -> GoalProgress(kind, day.onTime, 5)
            GoalKind.QURAN -> GoalProgress(kind, day.quranSeconds / 60, settings.quranMinutes)
            GoalKind.MORNING_ADHKAR -> GoalProgress(kind, if (day.morningAdhkar) 1 else 0, 1)
            GoalKind.EVENING_ADHKAR -> GoalProgress(kind, if (day.eveningAdhkar) 1 else 0, 1)
            else -> error("$kind is weekly")
        }
    }

    /** A day counts as met only when there was at least one daily goal and all were met. */
    fun dayMet(settings: GoalSettings, day: DayRecord): Boolean = daily(settings, day).let { it.isNotEmpty() && it.all(GoalProgress::met) }

    /**
     * Days in a row on which the daily goals were met. It is gentle: today never breaks
     * it, and one missed day in a Friday-to-Thursday week is let go. A second missed day
     * in the same week ends it. Only met days are counted.
     */
    fun streak(today: LocalDate, since: LocalDate, met: Set<LocalDate>): Int {
        var day = if (today in met) today else today.minusDays(1)
        var count = 0
        val missesByWeek = HashMap<LocalDate, Int>()
        while (!day.isBefore(since)) {
            if (day in met) {
                count++
            } else {
                val week = WeeklyReports.weekStart(day)
                val misses = (missesByWeek[week] ?: 0) + 1
                if (misses > 1) break
                missesByWeek[week] = misses
            }
            day = day.minusDays(1)
        }
        return count
    }

    /**
     * Today's goals, this week's and the streak. [met] holds the days already recorded as
     * met; today is judged from [todayRecord] instead, so it is always current.
     */
    fun build(
        today: LocalDate,
        since: LocalDate,
        settings: GoalSettings,
        todayRecord: DayRecord,
        met: Set<LocalDate>,
        qadaMadeUpThisWeek: Int,
    ): GoalsDay {
        val todayMet = dayMet(settings, todayRecord)
        val allMet = if (todayMet) met + today else met - today
        val start = WeeklyReports.weekStart(today)
        val week = (0L until 7L).map { start.plusDays(it) }.map { d ->
            d to when {
                d.isAfter(today) || d.isBefore(since) -> DayMark.NONE
                d in allMet -> DayMark.MET
                d == today -> DayMark.OPEN
                else -> DayMark.MISSED
            }
        }
        val weekly = weeklyKinds.filter(settings::isOn).map { kind ->
            when (kind) {
                GoalKind.GOOD_DAYS -> GoalProgress(kind, week.count { it.second == DayMark.MET }, settings.goodDays)
                GoalKind.QADA -> GoalProgress(kind, qadaMadeUpThisWeek.coerceAtLeast(0), settings.qadaPerWeek)
                else -> error("$kind is daily")
            }
        }
        return GoalsDay(
            today = today,
            settings = settings,
            daily = daily(settings, todayRecord),
            weekly = weekly,
            week = week,
            streak = streak(today, since, allMet),
        )
    }
}
