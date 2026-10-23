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
        nm.createNotificationChannels(listOf(adhan, prayer, silent, reminders))
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

    fun show(context: Context, id: Int, channel: String, title: String, text: String) {
        if (!canPost(context)) return
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
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
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post.
        }
    }
}
