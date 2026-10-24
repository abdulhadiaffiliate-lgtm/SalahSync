package com.salahsync.core.tracker

import com.salahsync.core.DayTimes
import com.salahsync.core.Prayer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime

/** When each fard prayer's time ends, which decides whether a tick counts as on time. */
object PrayerWindows {

    /**
     * The end of [prayer]'s time on [day]: Fajr ends at sunrise, Dhuhr at Asr, Asr at
     * Maghrib, Maghrib at Isha and Isha at the next day's Fajr. Without [nextDay] Isha
     * runs to the end of the day.
     */
    fun endOf(prayer: Prayer, day: DayTimes, nextDay: DayTimes?): ZonedDateTime = when (prayer) {
        Prayer.FAJR -> day[Prayer.SUNRISE]
        Prayer.SUNRISE -> day[Prayer.DHUHR]
        Prayer.DHUHR -> day[Prayer.ASR]
        Prayer.ASR -> day[Prayer.MAGHRIB]
        Prayer.MAGHRIB -> day[Prayer.ISHA]
        Prayer.ISHA -> nextDay?.get(Prayer.FAJR) ?: day.date.plusDays(1).atStartOfDay(day.zone)
    }

    /** True when a prayer ticked at [markedAt] was ticked after its time had ended. */
    fun isLate(prayer: Prayer, day: DayTimes, nextDay: DayTimes?, markedAt: ZonedDateTime): Boolean =
        markedAt.toInstant().isAfter(endOf(prayer, day, nextDay).toInstant())
}

enum class PrayerStatus {
    ON_TIME,
    LATE,
    MISSED,

    /** Today, not ticked yet: it can still be prayed, so it is not missed. */
    PENDING,

    /** Before tracking began, or in the future. */
    UNTRACKED,
}

data class StatusCounts(val onTime: Int = 0, val late: Int = 0, val missed: Int = 0) {
    val prayed: Int get() = onTime + late
    val tracked: Int get() = onTime + late + missed

    operator fun plus(status: PrayerStatus): StatusCounts = when (status) {
        PrayerStatus.ON_TIME -> copy(onTime = onTime + 1)
        PrayerStatus.LATE -> copy(late = late + 1)
        PrayerStatus.MISSED -> copy(missed = missed + 1)
        PrayerStatus.PENDING, PrayerStatus.UNTRACKED -> this
    }
}

data class ReportDay(val date: LocalDate, val statuses: Map<Prayer, PrayerStatus>)

data class WeeklyReport(
    /** The Friday the week starts on. */
    val start: LocalDate,
    /** The Thursday the week ends on. */
    val end: LocalDate,
    val days: List<ReportDay>,
    val totals: StatusCounts,
    val byPrayer: Map<Prayer, StatusCounts>,
    val qadaOwed: Int,
    /** Days in a row, up to today, with all five prayers ticked. */
    val currentStreak: Int,
    /** The longest such run since tracking began. */
    val bestStreak: Int,
    /** Days in a row, up to today, with all five prayers on time. */
    val onTimeStreak: Int,
    /** True while the week has not finished yet. */
    val inProgress: Boolean,
) {
    val hasData: Boolean get() = totals.tracked > 0 || days.any { d -> d.statuses.values.any { it == PrayerStatus.PENDING } }
}

/**
 * Builds the Friday-to-Thursday prayer report from the tracker's ticks. A week runs
 * Friday to Thursday so the report is ready on Friday morning, before Jumu'ah.
 */
object WeeklyReports {

    /** The Friday that starts the week containing [date]. */
    fun weekStart(date: LocalDate): LocalDate {
        val back = (date.dayOfWeek.value - DayOfWeek.FRIDAY.value + 7) % 7
        return date.minusDays(back.toLong())
    }

    /** The start of the most recent week that has fully ended by [today]. */
    fun lastCompleteWeekStart(today: LocalDate): LocalDate = weekStart(today).minusWeeks(1)

    fun status(
        date: LocalDate,
        prayer: Prayer,
        today: LocalDate,
        since: LocalDate,
        prayed: Set<Pair<LocalDate, Prayer>>,
        late: Set<Pair<LocalDate, Prayer>>,
    ): PrayerStatus = when {
        date.isBefore(since) || date.isAfter(today) -> PrayerStatus.UNTRACKED
        (date to prayer) in prayed -> if ((date to prayer) in late) PrayerStatus.LATE else PrayerStatus.ON_TIME
        date == today -> PrayerStatus.PENDING
        else -> PrayerStatus.MISSED
    }

    fun build(
        weekStart: LocalDate,
        today: LocalDate,
        since: LocalDate,
        prayed: Set<Pair<LocalDate, Prayer>>,
        late: Set<Pair<LocalDate, Prayer>>,
        qadaOwed: Int,
    ): WeeklyReport {
        val start = weekStart(weekStart)
        val days = (0L until 7L).map { offset ->
            val date = start.plusDays(offset)
            ReportDay(date, Prayer.fard.associateWith { status(date, it, today, since, prayed, late) })
        }
        var totals = StatusCounts()
        val byPrayer = Prayer.fard.associateWith { StatusCounts() }.toMutableMap()
        for (day in days) for ((p, s) in day.statuses) {
            totals += s
            byPrayer[p] = byPrayer.getValue(p) + s
        }
        val streaks = streaks(today, since, prayed, late)
        return WeeklyReport(
            start = start,
            end = start.plusDays(6),
            days = days,
            totals = totals,
            byPrayer = byPrayer,
            qadaOwed = qadaOwed,
            currentStreak = streaks.current,
            bestStreak = streaks.best,
            onTimeStreak = streaks.onTime,
            inProgress = !start.plusDays(6).isBefore(today),
        )
    }

    data class Streaks(val current: Int, val best: Int, val onTime: Int)

    /**
     * Counts runs of complete days. Today only adds to a run once all five are ticked,
     * and never breaks one, since its prayers can still be prayed.
     */
    fun streaks(
        today: LocalDate,
        since: LocalDate,
        prayed: Set<Pair<LocalDate, Prayer>>,
        late: Set<Pair<LocalDate, Prayer>>,
    ): Streaks {
        fun complete(d: LocalDate) = Prayer.fard.all { (d to it) in prayed }
        fun onTime(d: LocalDate) = Prayer.fard.all { (d to it) in prayed && (d to it) !in late }

        fun runEndingToday(test: (LocalDate) -> Boolean): Int {
            var day = if (test(today)) today else today.minusDays(1)
            var n = 0
            while (!day.isBefore(since) && test(day)) {
                n++
                day = day.minusDays(1)
            }
            return n
        }

        var best = 0
        var run = 0
        var day = since
        while (!day.isAfter(today)) {
            if (complete(day)) {
                run++
                best = maxOf(best, run)
            } else if (day != today) {
                run = 0
            }
            day = day.plusDays(1)
        }
        return Streaks(current = runEndingToday(::complete), best = best, onTime = runEndingToday(::onTime))
    }
}
