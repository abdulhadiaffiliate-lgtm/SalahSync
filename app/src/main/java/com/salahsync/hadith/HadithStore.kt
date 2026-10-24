package com.salahsync.hadith

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.hadithStore: DataStore<Preferences> by preferencesDataStore(name = "hadith")

enum class HadithLanguage(val label: String) { ENGLISH("English"), URDU("اردو") }

data class HadithPrefs(val language: HadithLanguage = HadithLanguage.ENGLISH, val showArabic: Boolean = true)

/** Hadith reading preferences, kept on the device. */
class HadithStore(private val context: Context) {
    private object K {
        val language = stringPreferencesKey("language")
        val showArabic = booleanPreferencesKey("show_arabic")
    }

    val prefs: Flow<HadithPrefs> = context.hadithStore.data.map { p ->
        HadithPrefs(
            language = p[K.language]?.let { runCatching { HadithLanguage.valueOf(it) }.getOrNull() } ?: HadithLanguage.ENGLISH,
            showArabic = p[K.showArabic] ?: true,
        )
    }

    suspend fun setLanguage(language: HadithLanguage) = context.hadithStore.edit { it[K.language] = language.name }
    suspend fun setShowArabic(show: Boolean) = context.hadithStore.edit { it[K.showArabic] = show }
}
