package com.salahsync.quran.audio

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.salahsync.core.quran.AyahRef
import com.salahsync.core.quran.Quran
import com.salahsync.quran.share.VerseArt
import java.io.File

/** What a queued MediaItem plays, encoded in its media id as "alafasy|2|255". */
data class QueueEntry(val reciter: Reciter, val ref: AyahRef) {
    val mediaId: String get() = "${reciter.id}|${ref.surah}|${ref.ayah}"

    companion object {
        fun parse(mediaId: String?): QueueEntry? {
            val parts = mediaId?.split('|') ?: return null
            if (parts.size != 3) return null
            val reciter = Reciters.byId(parts[0]) ?: return null
            val ref = AyahRef(parts[1].toIntOrNull() ?: return null, parts[2].toIntOrNull() ?: return null)
            return if (Quran.isValid(ref)) QueueEntry(reciter, ref) else null
        }
    }
}

/** Builds playable MediaItems, one per ayah, so the player always knows which ayah is sounding. */
class QueueBuilder(private val context: Context, private val library: AudioLibrary) {

    /** Light items with only an id; the session resolves them with [resolve]. */
    fun surahIds(reciter: Reciter, surah: Int): List<MediaItem> =
        (1..Quran.surah(surah).ayahCount).map { MediaItem.Builder().setMediaId(QueueEntry(reciter, AyahRef(surah, it)).mediaId).build() }

    fun resolve(item: MediaItem): MediaItem? {
        val entry = QueueEntry.parse(item.mediaId) ?: return null
        val surah = Quran.surah(entry.ref.surah)
        val metadata = MediaMetadata.Builder()
            .setTitle("${surah.name} · Ayah ${entry.ref.ayah}")
            .setArtist(entry.reciter.name)
            .setAlbumTitle("Surah ${surah.name}")
            .setAlbumArtist(entry.reciter.name)
            .setTrackNumber(entry.ref.ayah)
            .setTotalTrackCount(surah.ayahCount)
            .setArtworkUri(artwork(entry.ref.surah))
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .build()
        return MediaItem.Builder()
            .setMediaId(entry.mediaId)
            .setUri(library.uri(entry.reciter, entry.ref))
            .setMediaMetadata(metadata)
            .build()
    }

    /** Lock-screen and notification artwork, drawn once per surah and kept in the cache. */
    private fun artwork(surah: Int): Uri {
        val file = File(context.cacheDir, "artwork/surah_$surah.png")
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            runCatching {
                val bitmap = VerseArt.surahArtwork(context, Quran.surah(surah), 512)
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
            }
        }
        return Uri.fromFile(file)
    }
}
