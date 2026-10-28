package com.salahsync.ui.util

import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

fun formatTime(t: ZonedDateTime): String = t.format(timeFormat)

fun formatTime(t: LocalTime): String = t.format(timeFormat)

/** "1:12" for hours and minutes, or "12:05" for minutes and seconds under an hour. */
fun formatCountdown(d: Duration): String {
    val s = d.seconds.coerceAtLeast(0)
    return if (s >= 3600) "%d:%02d".format(Locale.US, s / 3600, (s % 3600) / 60)
    else "%d:%02d".format(Locale.US, s / 60, s % 60)
}
