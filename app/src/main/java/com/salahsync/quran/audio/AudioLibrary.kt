package com.salahsync.quran.audio

import android.content.Context
import android.net.Uri
import com.salahsync.core.quran.AyahRef
import com.salahsync.core.quran.Quran
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Where a surah's audio can be played from without a connection. */
enum class Offline { NONE, BUNDLED, DOWNLOADED }

/** A surah download in progress: [done] of [total] ayat fetched. */
data class DownloadProgress(val done: Int, val total: Int, val failed: Boolean = false) {
    val fraction: Float get() = if (total == 0) 0f else done.toFloat() / total
}

/**
 * Finds each ayah's audio: bundled in the app, downloaded earlier, or streamed from EveryAyah.
 * Also downloads whole surahs for offline listening.
 */
class AudioLibrary(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = mutableMapOf<String, Job>()

    private val _downloads = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    /** Downloads in progress, keyed by [key]. */
    val downloads: StateFlow<Map<String, DownloadProgress>> = _downloads.asStateFlow()

    private val _downloaded = MutableStateFlow<Set<String>>(emptySet())
    /** Surahs fully downloaded, keyed by [key]. */
    val downloaded: StateFlow<Set<String>> = _downloaded.asStateFlow()

    /** File names bundled under assets/audio/<reciter id>/, read once. */
    private val bundled: Set<String> by lazy {
        runCatching { context.assets.list("audio/${Reciters.default.id}")?.toSet() }.getOrNull().orEmpty()
    }

    init {
        scope.launch { _downloaded.value = scanDownloaded() }
    }

    fun key(reciter: Reciter, surah: Int) = "${reciter.id}:$surah"

    private fun dir(reciter: Reciter) = File(context.filesDir, "recitations/${reciter.id}")

    private fun localFile(reciter: Reciter, ref: AyahRef) = File(dir(reciter), Reciter.fileName(ref.surah, ref.ayah))

    private fun isBundled(reciter: Reciter, ref: AyahRef) =
        reciter.id == Reciters.default.id && Reciter.fileName(ref.surah, ref.ayah) in bundled

    fun uri(reciter: Reciter, ref: AyahRef): Uri {
        if (isBundled(reciter, ref)) return Uri.parse("asset:///audio/${reciter.id}/${Reciter.fileName(ref.surah, ref.ayah)}")
        val file = localFile(reciter, ref)
        return if (file.exists()) Uri.fromFile(file) else Uri.parse(reciter.url(ref.surah, ref.ayah))
    }

    fun offline(reciter: Reciter, surah: Int): Offline {
        val count = Quran.surah(surah).ayahCount
        return when {
            (1..count).all { isBundled(reciter, AyahRef(surah, it)) } -> Offline.BUNDLED
            key(reciter, surah) in _downloaded.value -> Offline.DOWNLOADED
            else -> Offline.NONE
        }
    }

    private fun scanDownloaded(): Set<String> = Reciters.all.flatMap { r ->
        val names = dir(r).list()?.toSet().orEmpty()
        (1..114).filter { s -> (1..Quran.surah(s).ayahCount).all { Reciter.fileName(s, it) in names } }.map { key(r, it) }
    }.toSet()

    fun download(reciter: Reciter, surah: Int) {
        val k = key(reciter, surah)
        if (jobs[k]?.isActive == true) return
        val total = Quran.surah(surah).ayahCount
        _downloads.update { it + (k to DownloadProgress(0, total)) }
        jobs[k] = scope.launch {
            var done = 0
            try {
                dir(reciter).mkdirs()
                for (ayah in 1..total) {
                    if (!isActive) return@launch
                    val ref = AyahRef(surah, ayah)
                    val file = localFile(reciter, ref)
                    if (!file.exists() && !isBundled(reciter, ref)) fetch(reciter.url(surah, ayah), file)
                    done++
                    _downloads.update { it + (k to DownloadProgress(done, total)) }
                }
                _downloaded.update { it + k }
                _downloads.update { it - k }
            } catch (e: IOException) {
                _downloads.update { it + (k to DownloadProgress(done, total, failed = true)) }
            } finally {
                if (!isActive) _downloads.update { it - k }
            }
        }
    }

    fun cancel(reciter: Reciter, surah: Int) {
        val k = key(reciter, surah)
        jobs.remove(k)?.cancel()
        _downloads.update { it - k }
    }

    fun delete(reciter: Reciter, surah: Int) {
        cancel(reciter, surah)
        scope.launch {
            (1..Quran.surah(surah).ayahCount).forEach { localFile(reciter, AyahRef(surah, it)).delete() }
            _downloaded.update { it - key(reciter, surah) }
        }
    }

    /** Space taken by downloads, in bytes. */
    fun downloadedBytes(): Long = File(context.filesDir, "recitations").walkTopDown().filter { it.isFile }.sumOf { it.length() }

    private fun fetch(url: String, target: File) {
        val tmp = File(target.parentFile, target.name + ".part")
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            if (conn.responseCode != HttpURLConnection.HTTP_OK) throw IOException("HTTP ${conn.responseCode} for $url")
            conn.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
            if (!tmp.renameTo(target)) throw IOException("Could not save ${target.name}")
        } finally {
            conn.disconnect()
            tmp.delete()
        }
    }
}
