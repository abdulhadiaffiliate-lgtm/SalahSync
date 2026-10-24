package com.salahsync.core.hadith

import java.time.LocalDate

/** A language edition of a hadith book in the dataset. */
enum class HadithEdition(val prefix: String) { ARABIC("ara"), ENGLISH("eng"), URDU("urd") }

/**
 * Where the hadith text comes from: fawazahmed0/hadith-api, released under the Unlicense,
 * pinned to one commit so the text never changes under the app. Every file is checked
 * against its SHA-256 below before it is used. See licenses/HADITH-CONTENT.md.
 */
object HadithSource {
    const val COMMIT = "df57907be35291c91ad6a6691180e22ca9920784"
    const val REPO = "https://github.com/fawazahmed0/hadith-api"

    private val sha256 = mapOf(
        "ara-bukhari" to "f887d8724f75fb6e1d5d29342a173af5346ad9b933173c85e6e3cb570063ea33",
        "eng-bukhari" to "60f8dfcf2cb8bcd2c8e281be68d912ee153e0fc4d02ad317d33c532d3814a04f",
        "urd-bukhari" to "e9edbb3119ce1fc8a7422132c263ee76ddf4de3d448152de5868e2ce72d2e449",
        "ara-muslim" to "2cb296f3455ff8a3da9f6a0ca3de75da675788b2fb446ad316df297838e48217",
        "eng-muslim" to "b5a04a66206b2cfaab18e2439ed2efeb824c6e179627e36a883dc060c03d4960",
        "urd-muslim" to "f4df8f12a57e0d14b93829aae8885072f7f04b5ca2703f8fa90b99cd7624bdcf",
        "ara-abudawud" to "30dd3792ea65abe7fd0be8d568809d4eac04e38cbbd588b9c6055218fe43675f",
        "eng-abudawud" to "81f711c6240f517b3fce6f97ecb019c1831775297e25f1797e5785c11f61c6d8",
        "urd-abudawud" to "da1923ddaf1a2b81f81cc500abf536ff3b061a754fed528400344221d859da47",
        "ara-tirmidhi" to "22b7ffbbca9beb5d4f108cc0389a3cd16917876b3ceb32993c46e1554afd871b",
        "eng-tirmidhi" to "cf16ae15f7f6f23faab1c348d3962b49ac96e0e25cac7930b3725773bb6887f3",
        "urd-tirmidhi" to "ddd6e3a8c53193252391a36ea1a7870eb3ce5356362cf39dc915cfd2508f4f24",
    )

    /** File sizes in bytes, for download progress (the transfer itself is compressed). */
    private val bytes = mapOf(
        "ara-bukhari" to 9_409_647L,
        "eng-bukhari" to 4_821_289L,
        "urd-bukhari" to 9_584_861L,
        "ara-muslim" to 8_329_204L,
        "eng-muslim" to 3_940_216L,
        "urd-muslim" to 7_301_659L,
        "ara-abudawud" to 6_623_115L,
        "eng-abudawud" to 3_589_001L,
        "urd-abudawud" to 5_346_674L,
        "ara-tirmidhi" to 6_583_667L,
        "eng-tirmidhi" to 2_587_658L,
        "urd-tirmidhi" to 5_214_642L,
    )

    fun name(book: HadithBook, edition: HadithEdition) = "${edition.prefix}-${book.slug}"

    fun sha256(book: HadithBook, edition: HadithEdition): String = sha256.getValue(name(book, edition))

    fun bytes(book: HadithBook, edition: HadithEdition): Long = bytes.getValue(name(book, edition))

    /** The whole download for a book: its Arabic, English and Urdu editions. */
    fun bytes(book: HadithBook): Long = HadithEdition.entries.sumOf { bytes(book, it) }

    /** The jsDelivr mirror first, then GitHub itself, as the dataset's README advises. */
    fun urls(book: HadithBook, edition: HadithEdition): List<String> {
        val path = "editions/${name(book, edition)}.min.json"
        return listOf(
            "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@$COMMIT/$path",
            "https://raw.githubusercontent.com/fawazahmed0/hadith-api/$COMMIT/$path",
        )
    }
}

/** One of the hand-picked daily hadith, with a short topic line of our own. */
data class HadithPick(val book: HadithBook, val ref: String, val topic: String)

/**
 * Twenty well-known hadith from Sahih al-Bukhari and Sahih Muslim, one a day in turn.
 * Their text is bundled (assets/hadith/daily.json, made by scripts/build-daily-hadith.py
 * from the pinned dataset), so the Home screen has one without any download.
 */
object DailyHadith {
    val picks: List<HadithPick> = listOf(
        HadithPick(HadithBook.BUKHARI, "1", "Deeds are judged by intentions"),
        HadithPick(HadithBook.MUSLIM, "2564c", "Allah looks at hearts and deeds"),
        HadithPick(HadithBook.BUKHARI, "13", "Wanting for your brother what you want for yourself"),
        HadithPick(HadithBook.MUSLIM, "223", "Purity is half of faith"),
        HadithPick(HadithBook.BUKHARI, "8", "The five pillars of Islam"),
        HadithPick(HadithBook.MUSLIM, "55a", "The religion is sincerity"),
        HadithPick(HadithBook.BUKHARI, "10", "Keeping others safe from your tongue and hand"),
        HadithPick(HadithBook.MUSLIM, "2588", "Charity does not decrease wealth"),
        HadithPick(HadithBook.BUKHARI, "528", "The five prayers wash away sins"),
        HadithPick(HadithBook.MUSLIM, "2699a", "Easing a believer's hardship"),
        HadithPick(HadithBook.BUKHARI, "5027", "Learning the Quran and teaching it"),
        HadithPick(HadithBook.MUSLIM, "2999", "Every affair of the believer is good"),
        HadithPick(HadithBook.BUKHARI, "6018", "Neighbours, guests and good words"),
        HadithPick(HadithBook.MUSLIM, "1631", "Deeds that continue after death"),
        HadithPick(HadithBook.BUKHARI, "6114", "True strength is controlling anger"),
        HadithPick(HadithBook.MUSLIM, "2553a", "Righteousness is good character"),
        HadithPick(HadithBook.BUKHARI, "6464", "The most beloved deeds are constant ones"),
        HadithPick(HadithBook.MUSLIM, "1015", "Allah accepts only what is good"),
        HadithPick(HadithBook.BUKHARI, "6406", "Two words heavy on the scale"),
        HadithPick(HadithBook.MUSLIM, "2674", "Calling others to guidance"),
    )

    fun indexFor(date: LocalDate): Int = Math.floorMod(date.toEpochDay(), picks.size.toLong()).toInt()

    fun pickFor(date: LocalDate): HadithPick = picks[indexFor(date)]
}
