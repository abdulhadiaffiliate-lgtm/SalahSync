package com.salahsync.quran

import android.content.Context
import com.salahsync.core.quran.AyahRef
import com.salahsync.core.quran.Quran
import com.salahsync.core.quran.QuranText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** A bundled translation. Each is republished verbatim from QuranEnc.com, with its version. */
enum class Translation(
    val label: String,
    val file: String,
    val rtl: Boolean,
    val translator: String,
    val credit: String,
) {
    ENGLISH(
        "English", "en-saheeh", false, "Saheeh International",
        "Saheeh International, published by Noor International Center. QuranEnc.com, version 1.1.2.",
    ),
    URDU(
        "اردو", "ur-junagarhi", true, "Muhammad Junagarhi",
        "Maulana Muhammad Junagarhi, with notes by Hafiz Salahuddin Yusuf. QuranEnc.com, version 1.1.3.",
    ),
}

/** Which translations the reader shows under the Arabic. */
enum class TranslationMode(val label: String, val translations: List<Translation>) {
    ENGLISH("English", listOf(Translation.ENGLISH)),
    URDU("Urdu", listOf(Translation.URDU)),
    BOTH("Both", listOf(Translation.ENGLISH, Translation.URDU)),
    ARABIC("Arabic only", emptyList()),
}

data class Verse(val ref: AyahRef, val arabic: String, val translations: Map<Translation, String>)

/**
 * Reads the bundled Quran text and translations from assets. Each file is loaded once,
 * on first use, and kept in memory: about 6,236 short strings per file.
 */
class QuranRepository(private val context: Context) {
    private val lock = Mutex()
    private var arabic: Array<String>? = null
    private val translations = mutableMapOf<Translation, Array<String>>()
    private val notes = mutableMapOf<Translation, Map<Int, String>>()

    private fun readAyat(file: String): Array<String> {
        val out = arrayOfNulls<String>(Quran.AYAH_COUNT)
        context.assets.open("quran/$file.txt").bufferedReader().useLines { lines ->
            lines.forEach { line -> QuranText.parseLine(line)?.let { (ref, text) -> out[Quran.indexOf(ref)] = text } }
        }
        return Array(Quran.AYAH_COUNT) { out[it].orEmpty() }
    }

    private suspend fun arabic(): Array<String> = lock.withLock {
        arabic ?: withContext(Dispatchers.IO) { readAyat("ar-uthmani") }.also { arabic = it }
    }

    private suspend fun translation(t: Translation): Array<String> = lock.withLock {
        translations[t] ?: withContext(Dispatchers.IO) { readAyat(t.file) }.also { translations[t] = it }
    }

    suspend fun surah(number: Int, mode: TranslationMode): List<Verse> {
        val ar = arabic()
        val tr = mode.translations.associateWith { translation(it) }
        val first = Quran.indexOf(AyahRef(number, 1))
        return (1..Quran.surah(number).ayahCount).map { ayah ->
            val ref = AyahRef(number, ayah)
            val i = first + ayah - 1
            Verse(ref, QuranText.displayArabic(ref, ar[i]), tr.mapValues { (_, texts) -> QuranText.displayTranslation(texts[i]) })
        }
    }

    suspend fun verse(ref: AyahRef, mode: TranslationMode): Verse {
        val i = Quran.indexOf(ref)
        val ar = arabic()
        val tr = mode.translations.associateWith { QuranText.displayTranslation(translation(it)[i]) }
        return Verse(ref, QuranText.displayArabic(ref, ar[i]), tr)
    }

    /** The basmala exactly as the text gives it in Al-Fatihah 1:1, shown above each surah. */
    suspend fun basmala(): String = arabic()[0]

    /** The translator's footnotes for an ayah, if it has any. */
    suspend fun notes(t: Translation, ref: AyahRef): String? {
        val all = lock.withLock {
            notes[t] ?: withContext(Dispatchers.IO) {
                buildMap {
                    context.assets.open("quran/${t.file}-notes.txt").bufferedReader().useLines { lines ->
                        lines.forEach { line ->
                            QuranText.parseLine(line)?.let { (r, text) -> put(Quran.indexOf(r), QuranText.unescapeNote(text)) }
                        }
                    }
                }
            }.also { notes[t] = it }
        }
        return all[Quran.indexOf(ref)]
    }
}
