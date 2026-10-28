package com.salahsync.core

import com.salahsync.core.quran.AyahRef
import com.salahsync.core.quran.Quran
import com.salahsync.core.quran.QuranText
import com.salahsync.core.quran.WordByWord
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuranTest {
    private val assets = listOf(File("../app/src/main/assets/quran"), File("app/src/main/assets/quran")).first { it.exists() }

    @Test
    fun metadataCoversTheWholeQuran() {
        assertEquals(114, Quran.surahs.size)
        assertEquals(Quran.AYAH_COUNT, Quran.surahs.sumOf { it.ayahCount })
        assertEquals(30, Quran.juzStarts.size)
        assertEquals(33, Quran.shortSurahs.size)
    }

    @Test
    fun indexRoundTrips() {
        for (i in listOf(0, 6, 7, 293, 3000, Quran.AYAH_COUNT - 1)) {
            assertEquals(i, Quran.indexOf(Quran.refAt(i)))
        }
        assertEquals(AyahRef(2, 1), Quran.refAt(7))
        assertEquals(AyahRef(114, 6), Quran.refAt(6235))
    }

    @Test
    fun juzBoundaries() {
        assertEquals(1, Quran.juzOf(AyahRef(2, 141)))
        assertEquals(2, Quran.juzOf(AyahRef(2, 142)))
        assertEquals(30, Quran.juzOf(AyahRef(114, 6)))
        assertEquals(listOf(78), Quran.surahsStartingIn(30).take(1).map { it.number })
    }

    @Test
    fun parsesRefs() {
        assertEquals(AyahRef(2, 255), AyahRef.parse("2:255"))
        assertNull(AyahRef.parse("2:287"))
        assertNull(AyahRef.parse("115:1"))
        assertNull(AyahRef.parse("junk"))
    }

    @Test
    fun arabicFileHasEveryAyahInOrder() {
        val lines = File(assets, "ar-uthmani.txt").readLines().mapNotNull(QuranText::parseLine)
        assertEquals(Quran.AYAH_COUNT, lines.size)
        lines.forEachIndexed { i, (ref, _) -> assertEquals(Quran.refAt(i), ref) }
    }

    @Test
    fun basmalaIsShownOnceAsAHeading() {
        val text = File(assets, "ar-uthmani.txt").readLines().mapNotNull(QuranText::parseLine).toMap()
        for (s in 2..114) {
            val ref = AyahRef(s, 1)
            val shown = QuranText.displayArabic(ref, text.getValue(ref))
            val original = text.getValue(ref)
            if (s == 9) {
                assertEquals(original, shown)
            } else {
                assertTrue(shown.isNotBlank() && shown.length < original.length && original.endsWith(shown), "Surah $s")
                assertFalse(shown.startsWith("بِ"), "Surah $s still opens with the basmala")
            }
        }
        assertEquals(text.getValue(AyahRef(1, 1)), QuranText.displayArabic(AyahRef(1, 1), text.getValue(AyahRef(1, 1))))
    }

    @Test
    fun translationsHaveEveryAyah() {
        for (name in listOf("en-saheeh.txt", "ur-junagarhi.txt", "en-mukhtasar.txt", "ar-muyassar.txt", "en-wbw.txt", "wbw-phonetic.txt")) {
            val lines = File(assets, name).readLines().mapNotNull(QuranText::parseLine)
            assertEquals(Quran.AYAH_COUNT, lines.size, name)
            lines.forEachIndexed { i, (ref, text) ->
                assertEquals(Quran.refAt(i), ref, name)
                assertTrue(text.isNotBlank(), "$name $ref is empty")
            }
        }
    }

    @Test
    fun translationDisplay() {
        val t = "(1) In the name of Allāh,[2] the Entirely Merciful."
        assertEquals("In the name of Allāh,[2] the Entirely Merciful.", QuranText.displayTranslation(t))
        assertEquals(listOf("2"), QuranText.footnoteRefs(t))
        val segs = QuranText.segments("a[1]b")
        assertEquals(listOf("a" to false, "1" to true, "b" to false), segs)
        assertEquals("line1\nline2\\x", QuranText.unescapeNote("line1\\nline2\\\\x"))
    }

    @Test
    fun arabicNumbers() {
        assertEquals("٢٥٥", QuranText.arabicNumber(255))
        assertTrue(QuranText.ayahEnd(7).startsWith("۝"))
    }

    @Test
    fun everyAyahHasAGlossForEachWord() {
        val arabic = File(assets, "ar-uthmani.txt").readLines().mapNotNull(QuranText::parseLine).toMap()
        val glosses = File(assets, "en-wbw.txt").readLines().mapNotNull(QuranText::parseLine)
        val sounds = File(assets, "wbw-phonetic.txt").readLines().mapNotNull(QuranText::parseLine).toMap()
        var words = 0
        glosses.forEach { (ref, text) ->
            val paired = WordByWord.pair(
                ref, QuranText.displayArabic(ref, arabic.getValue(ref)), WordByWord.glosses(text), WordByWord.glosses(sounds.getValue(ref)),
            )
            assertTrue(paired != null, "Words, glosses and transliterations differ in $ref")
            assertTrue(paired.all { it.arabic.isNotBlank() && it.gloss.isNotBlank() && it.transliteration.isNotBlank() }, "$ref")
            words += paired.size
        }
        assertEquals(77429, words)
    }

    @Test
    fun joinedWordsShareOneGloss() {
        val ref = AyahRef(37, 130)
        val paired = WordByWord.pair(ref, "سَلَٰمٌ عَلَىٰٓ إِلْ يَاسِينَ", listOf("\"Peace be", "upon", "Elijah.\""))!!
        assertEquals("إِلْ يَاسِينَ", paired[2].arabic)
        assertEquals("Elijah.\"", paired[2].gloss)
        assertEquals("", paired[2].transliteration)
        assertNull(WordByWord.pair(AyahRef(1, 2), "a b c", listOf("x", "y")))
    }
}
