package com.salahsync.notify

import com.salahsync.MainActivity
import com.salahsync.SalahSyncApp
import com.salahsync.core.daily.DailyContent
import com.salahsync.core.daily.DailyNoteKind
import com.salahsync.core.quran.QuranText
import com.salahsync.quran.Translation
import com.salahsync.quran.TranslationMode
import java.time.LocalDate

/**
 * Posts the day's ayah or good deed, the same ones the Home screen shows. The ayah is
 * read from the bundled Saheeh International text, so it matches the reader it opens.
 */
suspend fun postDailyNote(app: SalahSyncApp, kind: DailyNoteKind, date: LocalDate = LocalDate.now()) {
    if (kind.showsAyahOn(date)) {
        val ayah = DailyContent.ayahFor(date)
        val english = runCatching { app.quran.verse(ayah.ref, TranslationMode.ENGLISH).translations[Translation.ENGLISH] }
            .getOrNull()?.let(QuranText::plainTranslation)?.ifBlank { null } ?: ayah.english
        Notifications.showDaily(app, "Ayah of the day", "\u201C$english\u201D", ayah.reference) {
            it.putExtra(MainActivity.EXTRA_OPEN_AYAH, ayah.ref.toString())
        }
    } else {
        val deed = DailyContent.goodDeedFor(date)
        Notifications.showDaily(app, "Today's good deed", deed.text, deed.source) {
            it.putExtra(MainActivity.EXTRA_OPEN_HOME, true)
        }
    }
}
