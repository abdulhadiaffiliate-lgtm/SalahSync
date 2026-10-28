package com.salahsync.core.quran

/** A single ayah, as surah and ayah number (both from 1). */
data class AyahRef(val surah: Int, val ayah: Int) : Comparable<AyahRef> {
    override fun compareTo(other: AyahRef): Int = compareValuesBy(this, other, AyahRef::surah, AyahRef::ayah)

    /** "2:255", the form used for storage and display. */
    override fun toString(): String = "$surah:$ayah"

    companion object {
        fun parse(s: String?): AyahRef? {
            val parts = s?.split(':') ?: return null
            if (parts.size != 2) return null
            val surah = parts[0].toIntOrNull() ?: return null
            val ayah = parts[1].toIntOrNull() ?: return null
            return AyahRef(surah, ayah).takeIf { Quran.isValid(it) }
        }
    }
}

/** Surah and juz metadata, and ayah index arithmetic, for the Hafs numbering of 6,236 ayat. */
object Quran {
    const val AYAH_COUNT = 6236

    val surahs: List<Surah> get() = SurahData.surahs
    val juzStarts: List<AyahRef> get() = SurahData.juzStarts

    /** Al-Fatihah and the last 32 surahs (Al-Mutaffifin to An-Nas), whose audio ships with the app. */
    val shortSurahs: Set<Int> = setOf(1) + (83..114)

    private val surahOffsets: IntArray = IntArray(115).also { o ->
        for (i in 1..114) o[i] = o[i - 1] + SurahData.surahs[i - 1].ayahCount
    }

    fun surah(number: Int): Surah = SurahData.surahs[number - 1]

    fun isValid(ref: AyahRef): Boolean = ref.surah in 1..114 && ref.ayah in 1..surah(ref.surah).ayahCount

    /** Position of [ref] in the whole Quran, from 0 to 6235. */
    fun indexOf(ref: AyahRef): Int = surahOffsets[ref.surah - 1] + ref.ayah - 1

    fun refAt(index: Int): AyahRef {
        require(index in 0 until AYAH_COUNT) { "No ayah at index $index" }
        var surah = 1
        while (surahOffsets[surah] <= index) surah++
        return AyahRef(surah, index - surahOffsets[surah - 1] + 1)
    }

    /** The juz (1 to 30) that [ref] falls in. */
    fun juzOf(ref: AyahRef): Int = juzStarts.indexOfLast { it <= ref } + 1

    fun isSajdah(ref: AyahRef): Boolean = ref in SurahData.sajdas

    /** Every surah except Al-Fatihah (where it is ayah 1) and At-Tawbah opens with the basmala. */
    fun opensWithBasmala(surah: Int): Boolean = surah != 1 && surah != 9

    /** Surahs that begin inside [juz], in order. */
    fun surahsStartingIn(juz: Int): List<Surah> = surahs.filter { juzOf(AyahRef(it.number, 1)) == juz }
}

/** Helpers for showing the bundled texts. The files themselves stay verbatim, as their licences require. */
object QuranText {
    private val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    fun arabicNumber(n: Int): String = n.toString().map { arabicDigits[it - '0'] }.joinToString("")

    /**
     * Tanzil's text prefixes ayah 1 of most surahs with the basmala. The reader shows the
     * basmala once as a heading, so it is left off the first ayah when displayed.
     */
    fun displayArabic(ref: AyahRef, text: String): String {
        if (ref.ayah != 1 || !Quran.opensWithBasmala(ref.surah)) return text
        val words = text.split(' ')
        return if (words.size > 4 && words[0].startsWith('ب')) words.drop(4).joinToString(" ") else text
    }

    /** The ayah-end sign with its number, which the Mushaf font draws as one ornament. */
    fun ayahEnd(ayah: Int): String = "۝" + arabicNumber(ayah)

    private val leadingNumber = Regex("""^\(\d+\)\s*""")
    private val footnoteRef = Regex("""\[(\d+)]""")

    /** QuranEnc translations start with "(n) "; the reader already shows the ayah number. */
    fun displayTranslation(text: String): String = text.replace(leadingNumber, "")

    /** A translation with its verse number and footnote markers taken out, for a notification. */
    fun plainTranslation(text: String): String =
        displayTranslation(text).replace(footnoteRef, "").replace(Regex("""\s{2,}"""), " ").trim()

    /** The footnote numbers referenced in a translation, such as [1] and [2]. */
    fun footnoteRefs(text: String): List<String> = footnoteRef.findAll(text).map { it.groupValues[1] }.toList()

    /** Splits a translation into plain runs and footnote markers, for styling the markers. */
    fun segments(text: String): List<Pair<String, Boolean>> {
        val out = mutableListOf<Pair<String, Boolean>>()
        var last = 0
        footnoteRef.findAll(text).forEach { m ->
            if (m.range.first > last) out += text.substring(last, m.range.first) to false
            out += m.groupValues[1] to true
            last = m.range.last + 1
        }
        if (last < text.length) out += text.substring(last) to false
        return out
    }

    /** Parses one "surah|ayah|text" line of the bundled files; comments and blanks give null. */
    fun parseLine(line: String): Pair<AyahRef, String>? {
        if (line.isBlank() || line.startsWith("#")) return null
        val first = line.indexOf('|')
        val second = line.indexOf('|', first + 1)
        if (first < 0 || second < 0) return null
        val surah = line.substring(0, first).toIntOrNull() ?: return null
        val ayah = line.substring(first + 1, second).toIntOrNull() ?: return null
        return AyahRef(surah, ayah) to line.substring(second + 1)
    }

    /** Notes files escape newlines so each note fits on one line. */
    fun unescapeNote(s: String): String {
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (s[i + 1]) {
                    'n' -> sb.append('\n')
                    '\\' -> sb.append('\\')
                    else -> sb.append(c).append(s[i + 1])
                }
                i += 2
            } else {
                sb.append(c); i++
            }
        }
        return sb.toString()
    }
}

/** An Arabic word of an ayah with its English gloss and, where known, its transliteration. */
data class QuranWord(val arabic: String, val gloss: String, val transliteration: String = "")

/**
 * Pairs the words of the bundled Tanzil text with the Quranic Arabic Corpus glosses.
 * The corpus counts words as the Tanzil text split at spaces, but in four ayat it
 * treats two of our words as one: there the two are shown together under one gloss.
 */
object WordByWord {
    /** For each such ayah, the index (from 0) of the first of the two words the corpus joins. */
    val joined: Map<AyahRef, Int> = mapOf(
        AyahRef(2, 181) to 2, // baʿda mā
        AyahRef(8, 6) to 3, // baʿda mā
        AyahRef(13, 37) to 7, // baʿda mā
        AyahRef(37, 130) to 2, // il yāsīn
    )

    /** Splits one line's text of the word-by-word or transliteration file into its words. */
    fun glosses(text: String): List<String> = text.split('\t')

    /**
     * The words of [arabic] (as the reader shows it, without a leading basmala) with
     * their [glosses] and [transliterations] (in the same order; leave it empty to skip),
     * or null if they do not line up, so the ayah is shown as plain text.
     */
    fun pair(ref: AyahRef, arabic: String, glosses: List<String>, transliterations: List<String> = emptyList()): List<QuranWord>? {
        val words = arabic.split(' ').filter { it.isNotEmpty() }.toMutableList()
        joined[ref]?.let { i ->
            if (i + 1 < words.size) {
                words[i] = words[i] + " " + words[i + 1]
                words.removeAt(i + 1)
            }
        }
        if (words.size != glosses.size) return null
        if (transliterations.isNotEmpty() && transliterations.size != words.size) return null
        return words.indices.map { i -> QuranWord(words[i], glosses[i], transliterations.getOrElse(i) { "" }) }
    }
}
