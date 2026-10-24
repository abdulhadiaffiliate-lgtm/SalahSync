package com.salahsync.core.hadith

/** The four hadith books in the app. */
enum class HadithBook(
    val slug: String,
    val title: String,
    val shortTitle: String,
    val arabicTitle: String,
    val compiler: String,
    private val aliases: List<String>,
) {
    BUKHARI(
        "bukhari", "Sahih al-Bukhari", "Bukhari", "صحيح البخاري", "Imam Muhammad al-Bukhari",
        listOf("bukhari", "sahihbukhari", "sahihalbukhari"),
    ),
    MUSLIM("muslim", "Sahih Muslim", "Muslim", "صحيح مسلم", "Imam Muslim ibn al-Hajjaj", listOf("muslim", "sahihmuslim")),
    ABU_DAWUD(
        "abudawud", "Sunan Abi Dawud", "Abu Dawud", "سنن أبي داود", "Imam Abu Dawud as-Sijistani",
        listOf("abudawud", "abudaud", "abudawood", "sunanabidawud", "dawud"),
    ),
    TIRMIDHI(
        "tirmidhi", "Jami' at-Tirmidhi", "Tirmidhi", "جامع الترمذي", "Imam Abu 'Isa at-Tirmidhi",
        listOf("tirmidhi", "tirmizi", "attirmidhi", "jamiattirmidhi"),
    ),
    ;

    /** Bukhari and Muslim are sahih throughout; the Sunan carry a grading per hadith. */
    val isSahihCollection: Boolean get() = this == BUKHARI || this == MUSLIM

    companion object {
        fun bySlug(slug: String?): HadithBook? = entries.firstOrNull { it.slug == slug }

        /** Matches a typed book name such as "bukhari", "abu dawud" or "tirmidhi". */
        fun byAlias(text: String): HadithBook? {
            val t = text.lowercase().filter { it in 'a'..'z' }
            if (t.length < 3) return null
            return entries.firstOrNull { b -> b.aliases.any { it.startsWith(t) } }
        }
    }
}

/**
 * How a hadith is cited. [ref] is the number used on sunnah.com, such as "13" or, for
 * Muslim, "2564c"; [base] is the same without its letter, so "2564" finds 2564a to 2564c.
 */
data class HadithNumber(val ref: String, val base: String) {
    companion object {
        private const val INTRO = "intro-"

        /**
         * [hadithNumber] and [arabicNumber] are the dataset's fields as written in its JSON.
         * Muslim is cited by Fuad Abdul Baqi's numbering, which the dataset keeps in
         * arabicnumber as "2564.03"; that is 2564c. Muslim's introduction has no such number,
         * so it is cited by its place in the introduction.
         */
        fun of(book: HadithBook, hadithNumber: String, arabicNumber: String?): HadithNumber {
            if (book != HadithBook.MUSLIM) return HadithNumber(clean(hadithNumber), clean(hadithNumber).substringBefore('.'))
            val a = arabicNumber?.trim().orEmpty()
            if (a.isEmpty() || a == "null") return HadithNumber(INTRO + clean(hadithNumber), INTRO + clean(hadithNumber))
            val whole = a.substringBefore('.')
            val part = a.substringAfter('.', "").toIntOrNull()
            return if (part == null || part == 0 || part > 26) HadithNumber(whole, whole)
            else HadithNumber(whole + ('a' + part - 1), whole)
        }

        /** "13.0" and "13" are the same hadith. */
        private fun clean(n: String): String = n.trim().removeSuffix(".0")

        fun isIntroduction(ref: String) = ref.startsWith(INTRO)

        /** The number as a reader would write it, e.g. "2564c" or "Introduction 5". */
        fun label(ref: String): String = if (isIntroduction(ref)) "Introduction ${ref.removePrefix(INTRO)}" else ref

        /** sunnah.com only has pages for whole or lettered numbers, not the dataset's "402.2". */
        fun sunnahUrl(book: HadithBook, ref: String): String? =
            if (isIntroduction(ref) || '.' in ref) null else "https://sunnah.com/${book.slug}:$ref"
    }
}

/** A grading given by one scholar, as the dataset records it. */
data class Grade(val scholar: String, val grade: String)

/**
 * The chapter (kitab) of each hadith, fed in the dataset's order. The dataset gives most
 * hadith a chapter number and a few none (0). Those take the chapter of the hadith before
 * them, except at the very start of a book, where they stay in chapter 0 (Muslim's
 * introduction).
 */
class ChapterAssigner {
    private var last = 0

    fun next(chapter: Int): Int {
        if (chapter != 0) last = chapter
        return last
    }
}

object HadithSections {
    fun assign(chapters: List<Int>): List<Int> = ChapterAssigner().let { a -> chapters.map(a::next) }
}

sealed interface HadithQuery {
    /** A hadith number, optionally in one book: "13", "2564c", "muslim 2564". */
    data class Number(val ref: String, val book: HadithBook?) : HadithQuery

    /** Words to find in the English and Urdu translations. */
    data class Words(val terms: List<String>) : HadithQuery {
        /** An SQLite FTS4 MATCH expression: every word, each as a prefix. */
        val match: String get() = terms.joinToString(" ") { "$it*" }
    }

    companion object {
        private val number = Regex("""^\d{1,5}(\.\d{1,2})?[a-z]?$""")

        fun parse(text: String): HadithQuery? {
            val words = text.trim().lowercase().split(Regex("""\s+""")).filter { it.isNotEmpty() }
            if (words.isEmpty()) return null
            if (words.size == 1 && number.matches(words[0])) return Number(words[0], null)
            // A book name only counts before a number ("muslim 2564c", "abu dawud 23"), so
            // a search for the words "muslim brother" stays a word search.
            if (words.size in 2..3 && number.matches(words.last())) {
                HadithBook.byAlias(words.dropLast(1).joinToString(""))?.let { return Number(words.last(), it) }
            }
            return words(words)
        }

        private fun words(words: List<String>): Words? {
            // Letters and digits only, so the text cannot break the FTS query syntax.
            val terms = words.map { w -> w.filter { it.isLetterOrDigit() } }.filter { it.length >= 2 }
            return if (terms.isEmpty()) null else Words(terms)
        }
    }
}
