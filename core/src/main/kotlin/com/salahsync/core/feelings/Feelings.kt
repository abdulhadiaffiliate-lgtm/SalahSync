package com.salahsync.core.feelings

import com.salahsync.core.adhkar.QuranPassage
import com.salahsync.core.duas.Dua
import com.salahsync.core.duas.Duas
import com.salahsync.core.hadith.HadithBook
import com.salahsync.core.hadith.HadithPick

/**
 * A feeling, and what to turn to when you have it: ayat of the Quran (their text comes from
 * the bundled mushaf), duas from [Duas], and one hadith from Sahih al-Bukhari or Sahih Muslim
 * whose text is bundled in assets/hadith/feelings.json by scripts/build-daily-hadith.py.
 */
data class Feeling(
    val id: String,
    val name: String,
    /** One gentle line shown under the name. */
    val comfort: String,
    val ayat: List<QuranPassage>,
    val duaIds: List<String>,
    val hadith: HadithPick,
) {
    val duas: List<Dua> get() = duaIds.map { id -> Duas.all.first { it.id == id } }
}

object Feelings {
    val all: List<Feeling> = listOf(
        Feeling(
            id = "sad",
            name = "Sad",
            comfort = "Allah sees what you carry, and ease comes with hardship.",
            ayat = listOf(QuranPassage(94, 5..6), QuranPassage(3, 139..139)),
            duaIds = listOf("worry_and_debt", "dua_of_yunus"),
            hadith = HadithPick(HadithBook.MUSLIM, "2573", "Even grief and worry wipe away sins"),
        ),
        Feeling(
            id = "anxious",
            name = "Anxious",
            comfort = "Hearts find rest in the remembrance of Allah.",
            ayat = listOf(QuranPassage(13, 28..28), QuranPassage(9, 51..51)),
            duaIds = listOf("worry_and_debt", "distress"),
            hadith = HadithPick(HadithBook.BUKHARI, "7405", "Allah is as His servant thinks of Him"),
        ),
        Feeling(
            id = "angry",
            name = "Angry",
            comfort = "Restraining anger is a strength Allah loves.",
            ayat = listOf(QuranPassage(3, 134..134), QuranPassage(41, 34..34)),
            duaIds = listOf("anger"),
            hadith = HadithPick(HadithBook.BUKHARI, "6114", "True strength is controlling anger"),
        ),
        Feeling(
            id = "lonely",
            name = "Lonely",
            comfort = "Allah is near, and answers the one who calls on Him.",
            ayat = listOf(QuranPassage(2, 186..186), QuranPassage(50, 16..16)),
            duaIds = listOf("dua_of_yunus", "steadfast_heart"),
            hadith = HadithPick(HadithBook.BUKHARI, "1145", "Allah answers whoever calls on Him at night"),
        ),
        Feeling(
            id = "afraid",
            name = "Afraid",
            comfort = "Allah is enough for you, and the best to rely on.",
            ayat = listOf(QuranPassage(3, 173..173), QuranPassage(20, 46..46)),
            duaIds = listOf("distress", "dua_of_yunus"),
            hadith = HadithPick(HadithBook.BUKHARI, "4563", "Allah is sufficient for us"),
        ),
        Feeling(
            id = "guilty",
            name = "Guilty",
            comfort = "Never despair of Allah's mercy; He loves to forgive.",
            ayat = listOf(QuranPassage(39, 53..53), QuranPassage(4, 110..110)),
            duaIds = listOf("laylat_al_qadr", "dua_of_yunus"),
            hadith = HadithPick(HadithBook.BUKHARI, "6309", "Allah's joy at His servant's repentance"),
        ),
        Feeling(
            id = "grateful",
            name = "Grateful",
            comfort = "Thank Allah, and He gives you more.",
            ayat = listOf(QuranPassage(14, 7..7), QuranPassage(2, 152..152)),
            duaIds = listOf("help_to_remember_and_thank", "good_in_both_worlds"),
            hadith = HadithPick(HadithBook.MUSLIM, "2999", "Every affair of the believer is good"),
        ),
        Feeling(
            id = "unmotivated",
            name = "Unmotivated",
            comfort = "Small, steady deeds are the most beloved to Allah.",
            ayat = listOf(QuranPassage(29, 69..69), QuranPassage(94, 7..8)),
            duaIds = listOf("worry_and_debt", "help_to_remember_and_thank"),
            hadith = HadithPick(HadithBook.BUKHARI, "6464", "The most beloved deeds are constant ones"),
        ),
    )

    fun byId(id: String?): Feeling? = all.firstOrNull { it.id == id }

    /** The hadith whose text is bundled for this screen, each once, in order. */
    val hadithPicks: List<HadithPick> get() = all.map { it.hadith }.distinctBy { it.book to it.ref }
}
