package com.salahsync.core

import java.time.Instant
import java.time.ZonedDateTime

/** How the phone is quietened during prayer. */
enum class SilenceMode(val displayName: String, val description: String, val needsPolicyAccess: Boolean) {
    DO_NOT_DISTURB("Do Not Disturb", "Priority only: alarms and your starred contacts still come through", true),
    SILENT("Silent", "Ringer and notifications muted, no vibration", true),
    VIBRATE("Vibrate", "Ringer muted, the phone still vibrates", false),
}

/** One stretch of time during which the phone should be quiet for [prayer]. */
data class SilenceWindow(val prayer: Prayer, val label: String, val start: ZonedDateTime, val end: ZonedDateTime) {
    fun contains(now: Instant): Boolean = !now.isBefore(start.toInstant()) && now.isBefore(end.toInstant())
}

object SilenceWindows {

    /**
     * The quiet windows for one day. Each starts at the prayer time and lasts [minutes],
     * or [jumuahMinutes] for Dhuhr on Fridays. A window never runs into the next prayer's
     * window: it is cut short where the next one begins.
     */
    fun forDay(times: DayTimes, prayers: Set<Prayer>, minutes: Int, jumuahMinutes: Int): List<SilenceWindow> {
        val chosen = Prayer.fard.filter { it in prayers }
        return chosen.mapIndexedNotNull { i, p ->
            val length = if (p == Prayer.DHUHR && times.isFriday) jumuahMinutes else minutes
            if (length <= 0) return@mapIndexedNotNull null
            val start = times[p]
            var end = start.plusMinutes(length.toLong())
            chosen.getOrNull(i + 1)?.let { next -> if (times[next].isBefore(end)) end = times[next] }
            SilenceWindow(p, p.labelOn(times.isFriday), start, end)
        }
    }
}

/** What the ringer and Do Not Disturb were set to, so they can be put back. */
data class SoundState(val ringerMode: Int, val interruptionFilter: Int)

/**
 * Decides whether to put the sound back when a quiet window ends. Only the setting the
 * mode changed is compared, and if the user changed it themselves during prayer their
 * choice is left alone.
 */
object SoundRestore {
    fun shouldRestore(mode: SilenceMode, applied: SoundState, current: SoundState): Boolean =
        if (mode == SilenceMode.DO_NOT_DISTURB) current.interruptionFilter == applied.interruptionFilter
        else current.ringerMode == applied.ringerMode
}
