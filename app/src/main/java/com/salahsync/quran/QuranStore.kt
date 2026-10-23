package com.salahsync.quran

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.salahsync.core.quran.AyahRef
import com.salahsync.quran.audio.Reciters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.quranStore: DataStore<Preferences> by preferencesDataStore(name = "quran")

data class Bookmark(val ref: AyahRef, val savedAt: Long)

data class QuranPrefs(
    val bookmarks: List<Bookmark> = emptyList(),
    val lastRead: AyahRef? = null,
    val mode: TranslationMode = TranslationMode.ENGLISH,
    val reciterId: String = Reciters.default.id,
    val arabicSize: Int = DEFAULT_ARABIC_SIZE,
) {
    fun isBookmarked(ref: AyahRef): Boolean = bookmarks.any { it.ref == ref }

    companion object {
        const val DEFAULT_ARABIC_SIZE = 28
        val arabicSizes = 22..40
    }
}

/** Reading progress and Quran preferences, kept on the device. */
class QuranStore(private val context: Context) {
    private object K {
        val bookmarks = stringSetPreferencesKey("bookmarks")
        val lastRead = stringPreferencesKey("last_read")
        val mode = stringPreferencesKey("translation_mode")
        val reciter = stringPreferencesKey("reciter")
        val arabicSize = intPreferencesKey("arabic_size")
    }

    val prefs: Flow<QuranPrefs> = context.quranStore.data.map { p ->
        QuranPrefs(
            bookmarks = p[K.bookmarks].orEmpty().mapNotNull(::parseBookmark).sortedByDescending { it.savedAt },
            lastRead = AyahRef.parse(p[K.lastRead]),
            mode = p[K.mode]?.let { runCatching { TranslationMode.valueOf(it) }.getOrNull() } ?: TranslationMode.ENGLISH,
            reciterId = p[K.reciter]?.takeIf { Reciters.byId(it) != null } ?: Reciters.default.id,
            arabicSize = (p[K.arabicSize] ?: QuranPrefs.DEFAULT_ARABIC_SIZE).coerceIn(QuranPrefs.arabicSizes),
        )
    }

    /** Stored as "2:255@1729700000000" so the list can show the newest first. */
    private fun parseBookmark(s: String): Bookmark? {
        val ref = AyahRef.parse(s.substringBefore('@')) ?: return null
        return Bookmark(ref, s.substringAfter('@', "0").toLongOrNull() ?: 0L)
    }

    suspend fun toggleBookmark(ref: AyahRef) = context.quranStore.edit { p ->
        val current = p[K.bookmarks].orEmpty()
        val existing = current.filter { it.substringBefore('@') == ref.toString() }
        p[K.bookmarks] = if (existing.isNotEmpty()) current - existing.toSet() else current + "$ref@${System.currentTimeMillis()}"
    }

    suspend fun setLastRead(ref: AyahRef) = context.quranStore.edit { it[K.lastRead] = ref.toString() }
    suspend fun setMode(mode: TranslationMode) = context.quranStore.edit { it[K.mode] = mode.name }
    suspend fun setReciter(id: String) = context.quranStore.edit { it[K.reciter] = id }
    suspend fun setArabicSize(sp: Int) = context.quranStore.edit { it[K.arabicSize] = sp.coerceIn(QuranPrefs.arabicSizes) }
}
