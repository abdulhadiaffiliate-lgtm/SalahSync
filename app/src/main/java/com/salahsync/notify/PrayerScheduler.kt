package com.salahsync.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.AlarmManagerCompat
import com.salahsync.core.Prayer
import com.salahsync.core.SilenceWindows
import com.salahsync.data.AlertStyle
import com.salahsync.data.PrayerRepository
import com.salahsync.data.UserPrefs
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Schedules today's and tomorrow's alarms. Every alarm that fires reschedules the
 * set, and so do boot, time-zone changes and app start, so the chain never breaks.
 */
class PrayerScheduler(
    private val context: Context,
    private val repository: PrayerRepository,
    private val silence: PrayerSilence,
) {

    enum class Kind { PRAYER, PRE_REMINDER, QURAN, DUA, SILENCE, WEEKLY_REPORT }

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    suspend fun reschedule(prefs: UserPrefs, now: ZonedDateTime = ZonedDateTime.now()) {
        cancelAll()
        if (!prefs.onboarded || prefs.location == null) {
            silence.recover(prefs, emptyList())
            return
        }
        val zone: ZoneId = now.zone
        for (dayIndex in 0..1) {
            val date = now.toLocalDate().plusDays(dayIndex.toLong())
            val times = repository.timesFor(prefs, date, zone) ?: continue
            for (prayer in Prayer.entries) {
                val style = prefs.alertFor(prayer)
                if (style == AlertStyle.OFF) continue
                val at = times[prayer]
                val label = prayer.labelOn(times.isFriday)
                schedule(Kind.PRAYER, dayIndex, prayer.ordinal, at, now, label)
                if (prefs.preReminderMinutes > 0 && prayer != Prayer.SUNRISE) {
                    schedule(Kind.PRE_REMINDER, dayIndex, prayer.ordinal, at.minusMinutes(prefs.preReminderMinutes.toLong()), now, label)
                }
            }
            if (prefs.quranReminder) schedule(Kind.QURAN, dayIndex, 0, date.at(8, 0, zone), now, null)
            if (prefs.duaReminder) schedule(Kind.DUA, dayIndex, 0, date.at(19, 30, zone), now, null)
            if (prefs.silentEnabled) {
                for (w in SilenceWindows.forDay(times, prefs.silentPrayers, prefs.silentMinutes, prefs.silentJumuahMinutes)) {
                    val at = w.start.plusSeconds(PrayerSilence.START_DELAY_SECONDS)
                    schedule(Kind.SILENCE, dayIndex, w.prayer.ordinal, at, now, w.label, w.end)
                }
            }
            if (prefs.weeklyReport && date.dayOfWeek == DayOfWeek.FRIDAY) {
                schedule(Kind.WEEKLY_REPORT, dayIndex, 0, date.at(9, 0, zone), now, null)
            }
        }
        silence.recover(prefs, silence.windowsNow(repository, prefs))
    }

    private fun LocalDate.at(h: Int, m: Int, zone: ZoneId) = atTime(LocalTime.of(h, m)).atZone(zone)

    private fun requestCode(kind: Kind, dayIndex: Int, slot: Int) = 1000 + kind.ordinal * 100 + dayIndex * 10 + slot

    private fun intent(kind: Kind, dayIndex: Int, slot: Int, label: String?, atMillis: Long, endMillis: Long?): PendingIntent {
        val i = Intent(context, PrayerAlarmReceiver::class.java)
            .setAction(ACTION_ALARM)
            .putExtra(EXTRA_KIND, kind.name)
            .putExtra(EXTRA_PRAYER, Prayer.entries.getOrNull(slot)?.name)
            .putExtra(EXTRA_LABEL, label)
            .putExtra(EXTRA_AT, atMillis)
        if (endMillis != null) i.putExtra(EXTRA_END, endMillis)
        return PendingIntent.getBroadcast(
            context, requestCode(kind, dayIndex, slot), i,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun schedule(
        kind: Kind, dayIndex: Int, slot: Int, at: ZonedDateTime, now: ZonedDateTime, label: String?, end: ZonedDateTime? = null,
    ) {
        if (!at.isAfter(now)) return
        val millis = at.toInstant().toEpochMilli()
        val pi = intent(kind, dayIndex, slot, label, millis, end?.toInstant()?.toEpochMilli())
        if (canScheduleExact()) {
            AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, millis, pi)
        } else {
            // Without the exact-alarm permission Android may delay this by a few minutes.
            AlarmManagerCompat.setAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, millis, pi)
        }
    }

    private fun cancelAll() {
        for (kind in Kind.entries) for (day in 0..1) for (slot in Prayer.entries.indices) {
            val i = Intent(context, PrayerAlarmReceiver::class.java).setAction(ACTION_ALARM)
            PendingIntent.getBroadcast(
                context, requestCode(kind, day, slot), i,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
            )?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
    }

    companion object {
        const val ACTION_ALARM = "com.salahsync.ALARM"
        const val EXTRA_KIND = "kind"
        const val EXTRA_PRAYER = "prayer"
        const val EXTRA_LABEL = "label"
        const val EXTRA_AT = "at"
        const val EXTRA_END = "end"
    }
}
