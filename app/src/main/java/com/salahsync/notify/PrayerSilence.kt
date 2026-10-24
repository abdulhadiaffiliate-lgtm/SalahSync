package com.salahsync.notify

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.AlarmManagerCompat
import com.salahsync.SalahSyncApp
import com.salahsync.core.SilenceMode
import com.salahsync.core.SilenceWindow
import com.salahsync.core.SilenceWindows
import com.salahsync.core.SoundRestore
import com.salahsync.core.SoundState
import com.salahsync.data.PrayerRepository
import com.salahsync.data.UserPrefs
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Quietens the phone for prayer and puts the sound back afterwards.
 *
 * What the sound was before is saved to disk with the time the quiet window ends, so a
 * reboot, an app update or the process being killed never leaves the phone stuck on
 * silent: [recover] runs on boot and on every reschedule and restores or re-arms it.
 */
class PrayerSilence(private val context: Context) {

    private val audio = context.getSystemService(AudioManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val alarms = context.getSystemService(AlarmManager::class.java)

    // Plain SharedPreferences: readable synchronously from receivers, kept out of backups.
    private val store = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)

    /** True when the user has let SalahSync change Do Not Disturb and the silent ringer. */
    fun hasPolicyAccess(): Boolean = notifications.isNotificationPolicyAccessGranted

    /** The mode that can actually be used now: without access, the phone is set to vibrate. */
    fun effectiveMode(mode: SilenceMode): SilenceMode =
        if (mode.needsPolicyAccess && !hasPolicyAccess()) SilenceMode.VIBRATE else mode

    val isActive: Boolean get() = store.contains(K_ENDS_AT)

    private fun readState() = SoundState(audio.ringerMode, notifications.currentInterruptionFilter)

    /** Quietens the phone from now until [window] ends, unless it is already quiet. */
    fun begin(prefs: UserPrefs, window: SilenceWindow) {
        val end = window.end.toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        if (!prefs.silentEnabled || now >= end) return
        if (isActive) {
            // Already quiet for an earlier prayer: just carry on to the later end.
            val ends = maxOf(store.getLong(K_ENDS_AT, 0), end)
            store.edit().putLong(K_ENDS_AT, ends).putString(K_LABEL, window.label).apply()
            armEnd(ends)
            showOngoing(window.label, ends)
            return
        }
        val mode = effectiveMode(prefs.silentMode)
        val before = readState()
        val alreadyQuiet = when (mode) {
            SilenceMode.DO_NOT_DISTURB -> before.interruptionFilter > NotificationManager.INTERRUPTION_FILTER_ALL
            SilenceMode.SILENT -> before.ringerMode == AudioManager.RINGER_MODE_SILENT
            SilenceMode.VIBRATE -> before.ringerMode != AudioManager.RINGER_MODE_NORMAL
        }
        markHandled(window)
        if (alreadyQuiet) return
        val applied = try {
            when (mode) {
                SilenceMode.DO_NOT_DISTURB -> notifications.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                SilenceMode.SILENT -> audio.ringerMode = AudioManager.RINGER_MODE_SILENT
                SilenceMode.VIBRATE -> audio.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            }
            mode
        } catch (e: SecurityException) {
            // Access was withdrawn after the check; vibrate needs no special access.
            Log.w(TAG, "Could not apply $mode, falling back to vibrate", e)
            runCatching { audio.ringerMode = AudioManager.RINGER_MODE_VIBRATE }.getOrNull() ?: return
            SilenceMode.VIBRATE
        }
        val after = readState()
        store.edit()
            .putString(K_MODE, applied.name)
            .putInt(K_PREV_RINGER, before.ringerMode)
            .putInt(K_PREV_FILTER, before.interruptionFilter)
            .putInt(K_SET_RINGER, after.ringerMode)
            .putInt(K_SET_FILTER, after.interruptionFilter)
            .putLong(K_ENDS_AT, end)
            .putString(K_LABEL, window.label)
            .apply()
        armEnd(end)
        showOngoing(window.label, end)
    }

    /**
     * Puts the sound back as it was. If the user changed it during prayer, their choice
     * stands. Safe to call when nothing is active.
     */
    fun end() {
        if (!isActive) return
        val mode = store.getString(K_MODE, null)?.let { runCatching { SilenceMode.valueOf(it) }.getOrNull() } ?: SilenceMode.VIBRATE
        val previous = SoundState(
            store.getInt(K_PREV_RINGER, AudioManager.RINGER_MODE_NORMAL),
            store.getInt(K_PREV_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL)
                .takeIf { it != NotificationManager.INTERRUPTION_FILTER_UNKNOWN } ?: NotificationManager.INTERRUPTION_FILTER_ALL,
        )
        val applied = SoundState(store.getInt(K_SET_RINGER, -1), store.getInt(K_SET_FILTER, -1))
        val current = readState()
        if (SoundRestore.shouldRestore(mode, applied, current)) {
            try {
                when (mode) {
                    SilenceMode.DO_NOT_DISTURB -> notifications.setInterruptionFilter(previous.interruptionFilter)
                    SilenceMode.SILENT, SilenceMode.VIBRATE -> {
                        audio.ringerMode = previous.ringerMode
                        // Silencing the ringer turns on Do Not Disturb on some phones; undo that too.
                        if (current.interruptionFilter != previous.interruptionFilter && current.interruptionFilter == applied.interruptionFilter &&
                            hasPolicyAccess()
                        ) notifications.setInterruptionFilter(previous.interruptionFilter)
                    }
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "Could not restore sound", e)
            }
        }
        store.edit().remove(K_MODE).remove(K_PREV_RINGER).remove(K_PREV_FILTER).remove(K_SET_RINGER)
            .remove(K_SET_FILTER).remove(K_ENDS_AT).remove(K_LABEL).apply()
        alarms.cancel(endIntent())
        Notifications.cancel(context, Notifications.ID_QUIET)
    }

    /**
     * Brings things back in line after a reboot, an update, a clock change or a settings
     * change: restores the sound if its window is over (or the feature was turned off),
     * re-arms the end alarm otherwise, and quietens the phone if a prayer's window began
     * while the phone was off.
     */
    fun recover(prefs: UserPrefs, windowsNow: List<SilenceWindow>) {
        val now = System.currentTimeMillis()
        if (isActive) {
            val ends = store.getLong(K_ENDS_AT, 0)
            if (!prefs.silentEnabled || now >= ends) end() else {
                armEnd(ends)
                showOngoing(store.getString(K_LABEL, null).orEmpty(), ends)
            }
            return
        }
        if (!prefs.silentEnabled) return
        val instant = Instant.ofEpochMilli(now)
        val current = windowsNow.firstOrNull { it.contains(instant) && !instant.isBefore(it.start.plusSeconds(START_DELAY_SECONDS).toInstant()) }
            ?: return
        if (store.getLong(K_HANDLED, Long.MIN_VALUE) != current.start.toEpochSecond()) begin(prefs, current)
    }

    /** Today's windows, for [recover]. */
    suspend fun windowsNow(prayers: PrayerRepository, prefs: UserPrefs): List<SilenceWindow> {
        if (!prefs.silentEnabled) return emptyList()
        val zone = ZoneId.systemDefault()
        val today = java.time.LocalDate.now(zone)
        return listOf(today.minusDays(1), today).flatMap { d ->
            prayers.timesFor(prefs, d, zone)
                ?.let { SilenceWindows.forDay(it, prefs.silentPrayers, prefs.silentMinutes, prefs.silentJumuahMinutes) }
                .orEmpty()
        }
    }

    /** Remembers that [window] was dealt with, so turning the sound back on by hand sticks. */
    private fun markHandled(window: SilenceWindow) {
        store.edit().putLong(K_HANDLED, window.start.toEpochSecond()).apply()
    }

    private fun endIntent(): PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST_END,
        Intent(context, SilenceReceiver::class.java).setAction(ACTION_END),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun restoreIntent(): PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST_RESTORE,
        Intent(context, SilenceReceiver::class.java).setAction(ACTION_RESTORE),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun armEnd(atMillis: Long) {
        val pi = endIntent()
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        if (exact) AlarmManagerCompat.setExactAndAllowWhileIdle(alarms, AlarmManager.RTC_WAKEUP, atMillis, pi)
        else AlarmManagerCompat.setAndAllowWhileIdle(alarms, AlarmManager.RTC_WAKEUP, atMillis, pi)
    }

    private fun showOngoing(label: String, endsAt: Long) {
        val until = Instant.ofEpochMilli(endsAt).atZone(ZoneId.systemDefault()).format(TIME)
        val mode = store.getString(K_MODE, null)?.let { runCatching { SilenceMode.valueOf(it) }.getOrNull() }
        val how = when (mode) {
            SilenceMode.DO_NOT_DISTURB -> "Do Not Disturb is on"
            SilenceMode.SILENT -> "Your phone is on silent"
            SilenceMode.VIBRATE, null -> "Your phone is on vibrate"
        }
        Notifications.showQuiet(context, "Quiet for $label", "$how until $until.", restoreIntent())
    }

    companion object {
        private const val TAG = "PrayerSilence"
        private const val STORE = "prayer_silence"
        private const val K_MODE = "mode"
        private const val K_PREV_RINGER = "prev_ringer"
        private const val K_PREV_FILTER = "prev_filter"
        private const val K_SET_RINGER = "set_ringer"
        private const val K_SET_FILTER = "set_filter"
        private const val K_ENDS_AT = "ends_at"
        private const val K_LABEL = "label"
        private const val K_HANDLED = "handled_window"

        /** Quiet starts this long after the prayer time, so the adhan or alert sound is not cut off. */
        const val START_DELAY_SECONDS = 30L

        private const val REQUEST_END = 900
        private const val REQUEST_RESTORE = 901
        const val ACTION_END = "com.salahsync.SILENCE_END"
        const val ACTION_RESTORE = "com.salahsync.SILENCE_RESTORE"

        private val TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

        /** The system screen where the user grants Do Not Disturb access. */
        fun policyAccessIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
    }
}

/** Ends a quiet window on time, or straight away from the notification's button. */
class SilenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != PrayerSilence.ACTION_END && intent.action != PrayerSilence.ACTION_RESTORE) return
        (context.applicationContext as SalahSyncApp).silence.end()
    }
}
