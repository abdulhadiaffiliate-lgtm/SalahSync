package com.salahsync.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.salahsync.MainActivity
import com.salahsync.R
import com.salahsync.data.AlertStyle

object Notifications {
    // Channel sound cannot change after creation, so the id carries a version.
    const val CHANNEL_ADHAN = "adhan_v1"
    const val CHANNEL_PRAYER = "prayer_v1"
    const val CHANNEL_SILENT = "prayer_silent_v1"
    const val CHANNEL_REMINDERS = "reminders_v1"
    const val CHANNEL_QUIET = "prayer_quiet_v1"
    const val CHANNEL_REPORT = "weekly_report_v1"
    const val CHANNEL_DAILY = "daily_note_v1"

    const val ID_QUIET = 40
    const val ID_REPORT = 41
    const val ID_DAILY = 42

    /**
     * The adhan recording. Drop a licensed `adhan.mp3` into res/raw to use it;
     * until then the phone's alarm sound plays at alarm volume.
     */
    private fun adhanSound(context: Context): Uri {
        val id = context.resources.getIdentifier("adhan", "raw", context.packageName)
        return if (id != 0) {
            Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/$id")
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val alarmAudio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val adhan = NotificationChannel(
            CHANNEL_ADHAN, context.getString(R.string.channel_adhan), NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_adhan_desc)
            setSound(adhanSound(context), alarmAudio)
            enableVibration(true)
        }
        val prayer = NotificationChannel(
            CHANNEL_PRAYER, context.getString(R.string.channel_prayer), NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = context.getString(R.string.channel_prayer_desc) }
        val silent = NotificationChannel(
            CHANNEL_SILENT, context.getString(R.string.channel_silent), NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.channel_silent_desc)
            setSound(null, null)
            enableVibration(false)
        }
        val reminders = NotificationChannel(
            CHANNEL_REMINDERS, context.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.channel_reminders_desc) }
        val quiet = NotificationChannel(
            CHANNEL_QUIET, context.getString(R.string.channel_quiet), NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.channel_quiet_desc)
            setShowBadge(false)
        }
        val report = NotificationChannel(
            CHANNEL_REPORT, context.getString(R.string.channel_report), NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.channel_report_desc) }
        // Gentle by design: it shows in the shade and status bar but makes no sound.
        val daily = NotificationChannel(
            CHANNEL_DAILY, context.getString(R.string.channel_daily), NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.channel_daily_desc)
            setSound(null, null)
            enableVibration(false)
        }
        nm.createNotificationChannels(listOf(adhan, prayer, silent, reminders, quiet, report, daily))
    }

    fun channelFor(style: AlertStyle): String? = when (style) {
        AlertStyle.ADHAN -> CHANNEL_ADHAN
        AlertStyle.NOTIFICATION -> CHANNEL_PRAYER
        AlertStyle.SILENT -> CHANNEL_SILENT
        AlertStyle.OFF -> null
    }

    fun canPost(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < 33

    private fun openApp(context: Context, requestCode: Int, extras: (Intent) -> Unit = {}): PendingIntent =
        PendingIntent.getActivity(
            context, requestCode,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .also(extras),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun post(context: Context, id: Int, n: android.app.Notification) {
        if (!canPost(context)) return
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post.
        }
    }

    /** The ongoing note shown while the phone is quiet for prayer, with a way to undo it. */
    fun showQuiet(context: Context, title: String, text: String, restore: PendingIntent) {
        val n = NotificationCompat.Builder(context, CHANNEL_QUIET)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFD9BC84.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openApp(context, 2))
            .addAction(0, context.getString(R.string.quiet_restore_now), restore)
            .build()
        post(context, ID_QUIET, n)
    }

    fun cancel(context: Context, id: Int) = NotificationManagerCompat.from(context).cancel(id)

    /** The Friday report notification; tapping it opens the report screen. */
    fun showReport(context: Context, title: String, text: String) {
        val n = NotificationCompat.Builder(context, CHANNEL_REPORT)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFD9BC84.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setAutoCancel(true)
            .setContentIntent(openApp(context, 3) { it.putExtra(MainActivity.EXTRA_OPEN_REPORT, true) })
            .build()
        post(context, ID_REPORT, n)
    }

    /**
     * The daily ayah or good deed. [extras] says where a tap goes; [subText] carries
     * the source so it is always visible, even when the text is collapsed.
     */
    fun showDaily(context: Context, title: String, text: String, subText: String, extras: (Intent) -> Unit) {
        val n = NotificationCompat.Builder(context, CHANNEL_DAILY)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFD9BC84.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setSubText(subText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setSilent(true)
            .setAutoCancel(true)
            .setContentIntent(openApp(context, 4, extras))
            .build()
        post(context, ID_DAILY, n)
    }

    fun show(context: Context, id: Int, channel: String, title: String, text: String) {
        if (!canPost(context)) return
        val open = openApp(context, 0)
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFD9BC84.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(if (channel == CHANNEL_REMINDERS) NotificationCompat.CATEGORY_REMINDER else NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        post(context, id, n)
    }
}
