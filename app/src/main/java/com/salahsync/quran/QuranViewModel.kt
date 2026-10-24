package com.salahsync.quran

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.salahsync.SalahSyncApp
import com.salahsync.core.quran.AyahRef
import com.salahsync.quran.audio.DownloadProgress
import com.salahsync.quran.audio.Offline
import com.salahsync.quran.audio.PlayerState
import com.salahsync.quran.audio.Reciter
import com.salahsync.quran.audio.Reciters
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Shared by the Quran tab, the reader and the players. */
class QuranViewModel(app: Application) : AndroidViewModel(app) {
    private val salah = app as SalahSyncApp
    private val store = salah.quranStore
    private val repo = salah.quran
    private val library = salah.audioLibrary
    private val player = salah.quranPlayer

    val prefs: StateFlow<QuranPrefs> = store.prefs.stateIn(viewModelScope, SharingStarted.Eagerly, QuranPrefs())
    val playback: StateFlow<PlayerState> = player.state
    val downloads: StateFlow<Map<String, DownloadProgress>> = library.downloads
    val downloaded: StateFlow<Set<String>> = library.downloaded

    init {
        player.connect()
    }

    val reciter: Reciter get() = Reciters.orDefault(prefs.value.reciterId)

    suspend fun surah(number: Int, mode: TranslationMode): List<Verse> = repo.surah(number, mode)
    suspend fun verse(ref: AyahRef, mode: TranslationMode): Verse = repo.verse(ref, mode)
    suspend fun basmala(): String = repo.basmala()
    suspend fun notes(t: Translation, ref: AyahRef): String? = repo.notes(t, ref)

    private fun io(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    fun toggleBookmark(ref: AyahRef) = io { store.toggleBookmark(ref) }
    fun setLastRead(ref: AyahRef) = io { store.setLastRead(ref) }
    fun setMode(mode: TranslationMode) = io { store.setMode(mode) }
    fun setArabicSize(sp: Int) = io { store.setArabicSize(sp) }

    fun setReciter(r: Reciter) {
        io { store.setReciter(r.id) }
        player.switchReciter(r)
    }

    fun play(surah: Int, fromAyah: Int = 1) = player.play(reciter, surah, fromAyah)
    fun playAyah(ref: AyahRef) = player.playAyah(playback.value.entry?.reciter ?: reciter, ref)
    fun togglePlay() = player.togglePlay()
    fun next() = player.next()
    fun previous() = player.previous()
    fun seekToAyah(ayah: Int) = player.seekToAyah(ayah)
    fun cycleRepeat() = player.cycleRepeat()
    fun setSpeed(speed: Float) = player.setSpeed(speed)
    fun stop() = player.stop()
    fun progress(): Pair<Long, Long> = player.progress()
    fun clearError() = player.clearError()

    fun offline(surah: Int, r: Reciter = reciter): Offline = library.offline(r, surah)
    fun downloadKey(surah: Int, r: Reciter = reciter): String = library.key(r, surah)
    fun download(surah: Int) = library.download(reciter, surah)
    fun cancelDownload(surah: Int) = library.cancel(reciter, surah)
    fun deleteDownload(surah: Int) = library.delete(reciter, surah)
}
