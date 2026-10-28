package com.salahsync.adhkar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.salahsync.SalahSyncApp
import com.salahsync.core.adhkar.Adhkar
import com.salahsync.core.adhkar.AdhkarSession
import com.salahsync.core.adhkar.Dhikr
import com.salahsync.core.quran.Quran
import com.salahsync.quran.Translation
import com.salahsync.quran.TranslationMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalTime

/** One surah or passage of a Quranic dhikr, ready to show in full. */
data class PassageText(
    val surahName: String,
    val surahArabicName: String,
    val reference: String,
    val withBasmala: Boolean,
    val ayat: List<Pair<Int, String>>,
    val english: List<String>,
)

class AdhkarViewModel(app: Application) : AndroidViewModel(app) {
    private val salah = app as SalahSyncApp
    private val repo = salah.tracker
    private val day = LocalDate.now()

    private val _session = MutableStateFlow(Adhkar.sessionAt(LocalTime.now()))
    val session: StateFlow<AdhkarSession> = _session.asStateFlow()

    /** Counts for the current session. Held here so fast taps never race the database. */
    private val _progress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val progress: StateFlow<Map<String, Int>> = _progress.asStateFlow()
    private var loadJob: Job? = null
    private val writeLock = Mutex()

    private val _passages = MutableStateFlow<Map<String, List<PassageText>>>(emptyMap())

    /** Full text of the Quranic adhkar, keyed by dhikr id, read from the bundled mushaf. */
    val passages: StateFlow<Map<String, List<PassageText>>> = _passages.asStateFlow()

    private val _basmala = MutableStateFlow("")
    val basmala: StateFlow<String> = _basmala.asStateFlow()

    init {
        load(_session.value)
        viewModelScope.launch {
            _basmala.value = salah.quran.basmala()
            val quranic = AdhkarSession.entries.flatMap { Adhkar.forSession(it) }.filter { it.isQuran }.distinctBy { it.id }
            _passages.value = quranic.associate { d ->
                d.id to d.quran.map { p ->
                    val surah = Quran.surah(p.surah)
                    val verses = p.refs.map { salah.quran.verse(it, TranslationMode.ENGLISH) }
                    PassageText(
                        surahName = surah.name,
                        surahArabicName = surah.arabicName,
                        reference = if (p.ayahs.first == p.ayahs.last) "${surah.name} ${p.surah}:${p.ayahs.first}" else "${surah.name}, surah ${p.surah}",
                        withBasmala = p.ayahs.first == 1 && Quran.opensWithBasmala(p.surah),
                        ayat = verses.map { it.ref.ayah to it.arabic },
                        english = verses.map { it.translations[Translation.ENGLISH].orEmpty() },
                    )
                }
            }
        }
    }

    private fun load(s: AdhkarSession) {
        loadJob?.cancel()
        _progress.value = emptyMap()
        loadJob = viewModelScope.launch { _progress.value = repo.adhkarProgress(day, s).first() }
    }

    fun setSession(s: AdhkarSession) {
        if (s == _session.value) return
        _session.value = s
        load(s)
    }

    /** Counts one recitation. Returns true when this tap completes the dhikr. */
    fun tap(d: Dhikr): Boolean {
        val current = _progress.value[d.id] ?: 0
        if (current >= d.count) return false
        set(d, current + 1)
        return current + 1 == d.count
    }

    fun reset(d: Dhikr) = set(d, 0)

    private fun set(d: Dhikr, count: Int) {
        val s = _session.value
        _progress.value = _progress.value + (d.id to count)
        viewModelScope.launch {
            // Always write the newest count, whatever order these writes run in.
            writeLock.withLock { repo.setAdhkarProgress(day, s, d.id, if (s == _session.value) _progress.value[d.id] ?: count else count) }
        }
    }
}
