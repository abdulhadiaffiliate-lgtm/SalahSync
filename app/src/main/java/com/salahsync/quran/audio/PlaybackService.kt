package com.salahsync.quran.audio

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.salahsync.MainActivity
import com.salahsync.R
import com.salahsync.SalahSyncApp
import com.salahsync.core.quran.Quran

/**
 * Plays recitations in the background, with lock-screen and notification controls.
 * When a surah ends (and repeat is off), playback carries on into the next surah.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val queue = (application as SalahSyncApp).quranQueue
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state != Player.STATE_ENDED || player.repeatMode != Player.REPEAT_MODE_OFF) return
                val last = QueueEntry.parse(player.currentMediaItem?.mediaId) ?: return
                val next = last.ref.surah + 1
                if (next > Quran.surahs.size) return
                val items = queue.surahIds(last.reciter, next).mapNotNull(queue::resolve)
                player.setMediaItems(items, 0, 0L)
                player.prepare()
                player.play()
            }
        })

        // Its own request code: the prayer notifications use 0 for MainActivity too, and a
        // shared code would make Android merge the two, so one tap would open the other's screen.
        val open = PendingIntent.getActivity(
            this, REQUEST_OPEN_PLAYER,
            Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_PLAYER, true)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, player)
            .setSessionActivity(open)
            .setCallback(object : MediaSession.Callback {
                // Controllers send ids only; turn them into playable items here.
                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: MutableList<MediaItem>,
                ): ListenableFuture<MutableList<MediaItem>> =
                    Futures.immediateFuture(mediaItems.mapNotNull(queue::resolve).toMutableList())
            })
            .build()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).build().apply { setSmallIcon(R.drawable.ic_notification) },
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    private companion object {
        const val REQUEST_OPEN_PLAYER = 1
    }
}
