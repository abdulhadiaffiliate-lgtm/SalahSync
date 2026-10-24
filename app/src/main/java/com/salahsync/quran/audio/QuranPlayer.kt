package com.salahsync.quran.audio

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.salahsync.core.quran.AyahRef
import com.salahsync.core.quran.Quran
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Repeat(val label: String, val mode: Int) {
    OFF("Continue to next surah", Player.REPEAT_MODE_OFF),
    AYAH("Repeat ayah", Player.REPEAT_MODE_ONE),
    SURAH("Repeat surah", Player.REPEAT_MODE_ALL);

    fun next(): Repeat = entries[(ordinal + 1) % entries.size]

    companion object {
        fun of(mode: Int) = entries.firstOrNull { it.mode == mode } ?: OFF
    }
}

data class PlayerState(
    val entry: QueueEntry? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val repeat: Repeat = Repeat.OFF,
    val speed: Float = 1f,
    val error: String? = null,
) {
    val active: Boolean get() = entry != null
    val ref: AyahRef? get() = entry?.ref
}

/**
 * The app's handle on [PlaybackService]: queues surahs, controls playback and exposes
 * which ayah is playing, so the reader can highlight it.
 */
class QuranPlayer(private val context: Context, private val queue: QueueBuilder) {
    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private val pending = mutableListOf<(MediaController) -> Unit>()

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish()

        override fun onPlayerError(error: PlaybackException) {
            _state.value = _state.value.copy(
                error = if (error.errorCode in networkErrors) "No connection. Download this surah to listen offline."
                else "This ayah could not be played.",
            )
        }
    }

    /** Binds to the playback service, picking up a recitation that is already playing. */
    fun connect() {
        if (future != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val f = MediaController.Builder(context, token).buildAsync()
        future = f
        f.addListener({
            val c = runCatching { f.get() }.getOrNull()
            if (c == null) {
                future = null
                return@addListener
            }
            controller = c
            c.addListener(listener)
            publish()
            pending.forEach { it(c) }
            pending.clear()
        }, ContextCompat.getMainExecutor(context))
    }

    private fun withController(action: (MediaController) -> Unit) {
        controller?.let(action) ?: run {
            pending += action
            connect()
        }
    }

    private fun publish() {
        val c = controller ?: return
        _state.value = PlayerState(
            entry = if (c.mediaItemCount > 0) QueueEntry.parse(c.currentMediaItem?.mediaId) else null,
            isPlaying = c.isPlaying,
            isBuffering = c.playbackState == Player.STATE_BUFFERING,
            repeat = Repeat.of(c.repeatMode),
            speed = c.playbackParameters.speed,
            error = if (c.playerError != null) _state.value.error else null,
        )
    }

    /** Plays [surah] from [fromAyah], replacing whatever was queued. */
    fun play(reciter: Reciter, surah: Int, fromAyah: Int = 1) = withController { c ->
        c.setMediaItems(queue.surahIds(reciter, surah), (fromAyah - 1).coerceIn(0, Quran.surah(surah).ayahCount - 1), 0L)
        c.prepare()
        c.play()
    }

    /** Jumps to [ayah] when its surah is already queued, otherwise starts the surah there. */
    fun playAyah(reciter: Reciter, ref: AyahRef) {
        val current = _state.value.entry
        if (current != null && current.reciter == reciter && current.ref.surah == ref.surah) {
            withController { c ->
                c.seekTo(ref.ayah - 1, 0L)
                c.play()
            }
        } else {
            play(reciter, ref.surah, ref.ayah)
        }
    }

    fun togglePlay() = withController { c ->
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            if (c.playbackState == Player.STATE_ENDED) c.seekToDefaultPosition(0)
            c.play()
        }
    }

    fun next() = withController { c ->
        if (c.hasNextMediaItem()) {
            c.seekToNextMediaItem()
        } else {
            val e = _state.value.entry ?: return@withController
            if (e.ref.surah < Quran.surahs.size) play(e.reciter, e.ref.surah + 1)
        }
    }

    fun previous() = withController { c ->
        if (c.currentPosition > 3_000 || !c.hasPreviousMediaItem()) c.seekTo(0L) else c.seekToPreviousMediaItem()
    }

    fun seekToAyah(ayah: Int) = withController { c ->
        if (ayah - 1 in 0 until c.mediaItemCount) c.seekTo(ayah - 1, 0L)
    }

    fun cycleRepeat() = withController { c -> c.repeatMode = Repeat.of(c.repeatMode).next().mode }

    fun setSpeed(speed: Float) = withController { c -> c.setPlaybackSpeed(speed) }

    /** Switches reciter mid-surah, carrying on from the same ayah. */
    fun switchReciter(reciter: Reciter) {
        val e = _state.value.entry ?: return
        if (e.reciter == reciter) return
        val wasPlaying = _state.value.isPlaying
        withController { c ->
            c.setMediaItems(queue.surahIds(reciter, e.ref.surah), e.ref.ayah - 1, 0L)
            c.prepare()
            if (wasPlaying) c.play()
        }
    }

    fun stop() = withController { c ->
        c.stop()
        c.clearMediaItems()
    }

    /** Position within the current ayah, in milliseconds, for the progress bar. */
    fun progress(): Pair<Long, Long> {
        val c = controller ?: return 0L to 0L
        return c.currentPosition.coerceAtLeast(0L) to c.duration.coerceAtLeast(0L)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private companion object {
        val networkErrors = setOf(
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        )
    }
}
