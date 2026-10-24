package com.salahsync.hadith

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.salahsync.SalahSyncApp
import com.salahsync.core.hadith.DailyHadith
import com.salahsync.core.hadith.HadithBook
import com.salahsync.core.hadith.HadithPick
import com.salahsync.core.hadith.HadithQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** What a search found; [query] is null for text too short to search. */
data class HadithSearch(val text: String, val query: HadithQuery?, val results: List<HadithItem>)

/** Shared by the Hadith screens and the Home screen's Hadith of the Day. */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class HadithViewModel(app: Application) : AndroidViewModel(app) {
    private val salah = app as SalahSyncApp
    private val library = salah.hadith
    private val store = salah.hadithStore

    val prefs: StateFlow<HadithPrefs> = store.prefs.stateIn(viewModelScope, SharingStarted.Eagerly, HadithPrefs())
    val installed: StateFlow<Set<HadithBook>> = library.installed.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val downloads: StateFlow<Map<HadithBook, HadithDownload>> = library.downloads

    private val _daily = MutableStateFlow<List<HadithItem>>(emptyList())
    val daily: StateFlow<List<HadithItem>> = _daily.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Null while the box is empty. */
    val search: StateFlow<HadithSearch?> = combine(_query.debounce(250), installed) { text, books -> text to books }
        .mapLatest { (text, _) ->
            if (text.isBlank()) return@mapLatest null
            val q = HadithQuery.parse(text)
            HadithSearch(text, q, if (q == null) emptyList() else runCatching { library.search(q) }.getOrDefault(emptyList()))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { _daily.value = library.daily() }
    }

    fun dailyFor(date: LocalDate): HadithItem? = daily.value.getOrNull(DailyHadith.indexFor(date))

    fun topicOf(item: HadithItem): HadithPick? = library.topicOf(item)

    fun setQuery(text: String) {
        _query.value = text
    }

    suspend fun chapters(book: HadithBook): List<Chapter> = library.chapters(book)
    suspend fun chapterTitle(book: HadithBook, number: Int): String = library.chapterTitle(book, number)
    suspend fun inChapter(book: HadithBook, chapter: Int): List<HadithItem> = library.inChapter(book, chapter)
    suspend fun hadith(book: HadithBook, ref: String): HadithItem? = library.hadith(book, ref)

    fun download(book: HadithBook) = library.download(book)
    fun cancelDownload(book: HadithBook) = library.cancel(book)
    fun delete(book: HadithBook) = library.delete(book)

    fun setLanguage(language: HadithLanguage) = viewModelScope.launch { store.setLanguage(language) }
    fun setShowArabic(show: Boolean) = viewModelScope.launch { store.setShowArabic(show) }
}
