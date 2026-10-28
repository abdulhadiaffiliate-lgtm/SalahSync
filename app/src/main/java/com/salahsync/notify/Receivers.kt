package com.salahsync.notify

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.salahsync.SalahSyncApp
import com.salahsync.core.Prayer
import com.salahsync.core.SilenceWindow
import com.salahsync.core.tracker.QadaLedger
import com.salahsync.core.tracker.WeeklyReports
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

private fun BroadcastReceiver.runAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    receiverScope.launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}

/** Posts the alert for a prayer or reminder, then schedules the next ones. */
class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != PrayerScheduler.ACTION_ALARM) return
        val app = context.applicationContext as SalahSyncApp
        runAsync {
            val prefs = app.settings.current()
            val kind = intent.getStringExtra(PrayerScheduler.EXTRA_KIND)
                ?.let { runCatching { PrayerScheduler.Kind.valueOf(it) }.getOrNull() }
            val prayer = intent.getStringExtra(PrayerScheduler.EXTRA_PRAYER)
                ?.let { runCatching { Prayer.valueOf(it) }.getOrNull() }
            val label = intent.getStringExtra(PrayerScheduler.EXTRA_LABEL) ?: prayer?.displayName ?: ""
            val firstName = prefs.name.substringBefore(' ').ifBlank { null }
            when (kind) {
                PrayerScheduler.Kind.PRAYER -> if (prayer != null) {
                    val channel = Notifications.channelFor(prefs.alertFor(prayer))
                    if (channel != null) {
                        val text = if (prayer == Prayer.SUNRISE) "The sun has risen. Fajr time has ended."
                        else listOfNotNull(firstName, "it is time for $label.").joinToString(", ")
                            .replaceFirstChar { it.uppercase() }
                        Notifications.show(context, 10 + prayer.ordinal, channel, label, text)
                    }
                }
                PrayerScheduler.Kind.PRE_REMINDER -> if (prayer != null) {
                    Notifications.show(
                        context, 20 + prayer.ordinal, Notifications.CHANNEL_REMINDERS,
                        "$label in ${prefs.preReminderMinutes} minutes", "Time to prepare and make wudu.",
                    )
                }
                PrayerScheduler.Kind.QURAN -> Notifications.show(
                    context, 30, Notifications.CHANNEL_REMINDERS, "Your daily Quran", "A few verses today keep the heart close.",
                )
                PrayerScheduler.Kind.DUA -> Notifications.show(
                    context, 31, Notifications.CHANNEL_REMINDERS, "Evening dua", "Take a quiet moment for your evening supplications.",
                )
                PrayerScheduler.Kind.SILENCE -> if (prayer != null) {
                    val zone = ZoneId.systemDefault()
                    val start = Instant.ofEpochMilli(intent.getLongExtra(PrayerScheduler.EXTRA_AT, 0)).atZone(zone)
                        .minusSeconds(PrayerSilence.START_DELAY_SECONDS)
                    val end = Instant.ofEpochMilli(intent.getLongExtra(PrayerScheduler.EXTRA_END, 0)).atZone(zone)
                    app.silence.begin(prefs, SilenceWindow(prayer, label, start, end))
                }
                PrayerScheduler.Kind.WEEKLY_REPORT -> if (prefs.weeklyReport) postWeeklyReport(app)
                PrayerScheduler.Kind.DAILY_NOTE -> if (prefs.dailyNote) postDailyNote(app, prefs.dailyNoteKind)
                null -> Unit
            }
            app.scheduler.reschedule(prefs)
        }
    }
}

/** Sums up last Friday-to-Thursday week from the local tracker. Nothing leaves the phone. */
private suspend fun postWeeklyReport(app: SalahSyncApp) {
    val snap = app.tracker.snapshot() ?: return
    val today = LocalDate.now()
    val owed = QadaLedger.counts(snap.since, today, snap.marks.prayed, snap.qada).values.sumOf { it.owed }
    val r = WeeklyReports.build(WeeklyReports.lastCompleteWeekStart(today), today, snap.since, snap.marks.prayed, snap.marks.late, owed)
    if (r.totals.tracked == 0) return
    val t = r.totals
    val lines = buildList {
        add("${t.onTime} on time, ${t.late} late, ${t.missed} missed out of ${t.tracked}.")
        if (r.currentStreak > 1) add("${r.currentStreak}-day streak of all five prayers.")
        add(if (owed == 0) "No Qada owed." else "Qada owed: $owed.")
    }
    Notifications.showReport(app, "Your week of prayer", lines.joinToString(" "))
}

/** Rebuilds the alarm chain after reboot, app update, clock or time-zone changes. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Exported for the system broadcasts, so other apps can reach it too: ignore anything else.
        if (intent.action !in ACTIONS) return
        val app = context.applicationContext as SalahSyncApp
        runAsync { app.scheduler.reschedule(app.settings.current()) }
    }

    private companion object {
        val ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }
}
