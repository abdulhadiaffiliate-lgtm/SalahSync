package com.salahsync.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.salahsync.SalahSyncApp
import com.salahsync.core.Prayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
                null -> Unit
            }
            app.scheduler.reschedule(prefs)
        }
    }
}

/** Rebuilds the alarm chain after reboot, app update, clock or time-zone changes. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as SalahSyncApp
        runAsync { app.scheduler.reschedule(app.settings.current()) }
    }
}
