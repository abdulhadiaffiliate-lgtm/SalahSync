package com.salahsync.core.guides

import com.salahsync.core.adhkar.HadithRef
import com.salahsync.core.sources.Sunnah.abuDawud
import com.salahsync.core.sources.Sunnah.bukhari
import com.salahsync.core.sources.Sunnah.muslim
import com.salahsync.core.sources.Sunnah.tirmidhi

enum class Madhhab(val displayName: String, val manual: String) {
    HANAFI("Hanafi", "Nur al-Idah of ash-Shurunbulali, with Maraqi al-Falah"),
    SHAFII("Shafi'i", "Minhaj at-Talibin of an-Nawawi, as summarised in Reliance of the Traveller"),
}

/** How binding a step is: leaving a fard step out means the act is not valid. */
enum class Ruling(val displayName: String) {
    FARD("Fard"),
    SUNNAH("Sunnah"),
}

data class GuideStep(
    val title: String,
    val detail: String,
    val ruling: Map<Madhhab, Ruling>,
    /** Words to say at this step, if any. */
    val arabic: String? = null,
    /** Where a school's ruling needs a word of explanation. */
    val madhhabNote: Map<Madhhab, String> = emptyMap(),
    val references: List<HadithRef> = emptyList(),
    /** An ayah reference shown as text, such as "Al-Ma'idah 5:6". */
    val quran: String? = null,
)

data class Guide(
    val id: String,
    val title: String,
    val arabicName: String,
    val intro: String,
    val steps: List<GuideStep>,
    /** Points that differ by school and are not tied to one step. */
    val madhhabNote: Map<Madhhab, String> = emptyMap(),
)

private fun both(r: Ruling) = Madhhab.entries.associateWith { r }
private fun of(hanafi: Ruling, shafii: Ruling) = mapOf(Madhhab.HANAFI to hanafi, Madhhab.SHAFII to shafii)

/**
 * Step-by-step wudu and ghusl. The steps follow the Prophet's ﷺ wudu as described by 'Uthman and
 * 'Abdullah ibn Zayd, and his ghusl as described by 'A'ishah and Maymunah, all in Sahih al-Bukhari.
 * Which steps are fard follows each school's standard manual ([Madhhab.manual]).
 */
object Guides {
    private const val MAIDAH = "Al-Ma'idah 5:6"

    val wudu = Guide(
        id = "wudu",
        title = "Wudu",
        arabicName = "الوضوء",
        intro = "The ablution needed before prayer, touching the mushaf and tawaf. Each washing is done once as the minimum; three times is the sunnah.",
        madhhabNote = mapOf(
            Madhhab.HANAFI to "Doing the steps in this order, without long pauses, and right before left, is sunnah.",
            Madhhab.SHAFII to "Doing the fard steps in this order (tartib) is itself fard. Doing them without long pauses, and right before left, is sunnah.",
        ),
        steps = listOf(
            GuideStep(
                "Make the intention",
                "Intend in your heart to make wudu for prayer. It does not need to be said aloud.",
                of(Ruling.SUNNAH, Ruling.FARD),
                madhhabNote = mapOf(Madhhab.SHAFII to "The intention must be present when you begin washing the face."),
                references = listOf(bukhari("1", "'Umar ibn al-Khattab")),
            ),
            GuideStep(
                "Say Bismillah",
                "Begin with \"Bismillah\", in the name of Allah.",
                both(Ruling.SUNNAH),
                arabic = "بِسْمِ اللَّهِ",
                references = listOf(abuDawud("101", "Abu Hurayrah", "Sahih (al-Albani)")),
            ),
            GuideStep(
                "Wash the hands",
                "Wash both hands up to the wrists three times, between the fingers too.",
                both(Ruling.SUNNAH),
                references = listOf(bukhari("159", "Humran, from 'Uthman ibn 'Affan")),
            ),
            GuideStep(
                "Rinse the mouth",
                "Take water into the mouth with the right hand, swirl it and spit it out, three times. Using a miswak here is also sunnah.",
                both(Ruling.SUNNAH),
                references = listOf(bukhari("159", "Humran, from 'Uthman ibn 'Affan")),
            ),
            GuideStep(
                "Rinse the nose",
                "Sniff water into the nose with the right hand and blow it out with the left, three times.",
                both(Ruling.SUNNAH),
                references = listOf(bukhari("159", "Humran, from 'Uthman ibn 'Affan")),
            ),
            GuideStep(
                "Wash the face",
                "Wash the whole face three times: from the hairline to below the chin, and from one earlobe to the other.",
                both(Ruling.FARD),
                references = listOf(bukhari("159", "Humran, from 'Uthman ibn 'Affan")),
                quran = MAIDAH,
            ),
            GuideStep(
                "Wash the arms",
                "Wash the right arm from the fingertips up to and including the elbow three times, then the left.",
                both(Ruling.FARD),
                references = listOf(bukhari("159", "Humran, from 'Uthman ibn 'Affan")),
                quran = MAIDAH,
            ),
            GuideStep(
                "Wipe the head",
                "With wet hands, wipe from the front of the head to the nape and back again, once.",
                both(Ruling.FARD),
                madhhabNote = mapOf(
                    Madhhab.HANAFI to "Wiping at least a quarter of the head is fard; wiping all of it is sunnah.",
                    Madhhab.SHAFII to "Wiping any part of the head, even a few hairs, is fard; wiping all of it is sunnah.",
                ),
                references = listOf(bukhari("185", "'Abdullah ibn Zayd")),
                quran = MAIDAH,
            ),
            GuideStep(
                "Wipe the ears",
                "Wipe the inside of the ears with the index fingers and the back with the thumbs.",
                both(Ruling.SUNNAH),
                madhhabNote = mapOf(
                    Madhhab.HANAFI to "The water left from wiping the head may be used.",
                    Madhhab.SHAFII to "Take fresh water for the ears.",
                ),
                references = listOf(tirmidhi("36", "'Abdullah ibn 'Abbas", "Sahih (Darussalam); at-Tirmidhi called it hasan sahih")),
            ),
            GuideStep(
                "Wash the feet",
                "Wash the right foot up to and including the ankle three times, running a finger between the toes, then the left.",
                both(Ruling.FARD),
                references = listOf(bukhari("159", "Humran, from 'Uthman ibn 'Affan")),
                quran = MAIDAH,
            ),
            GuideStep(
                "Say the testimony",
                "I bear witness that none has the right to be worshipped but Allah, and that Muhammad is the servant of Allah and His Messenger.",
                both(Ruling.SUNNAH),
                arabic = "أَشْهَدُ أَنْ لَا إِلَهَ إِلَّا اللَّهُ وَأَنَّ مُحَمَّدًا عَبْدُ اللَّهِ وَرَسُولُهُ",
                references = listOf(muslim("234a", "'Uqbah ibn 'Amir, from 'Umar ibn al-Khattab")),
            ),
        ),
    )

    val ghusl = Guide(
        id = "ghusl",
        title = "Ghusl",
        arabicName = "الغسل",
        intro = "The full bath required after janabah (sexual intercourse or ejaculation) and at the end of menstruation or post-natal bleeding. It is valid only if water reaches every part of the body.",
        steps = listOf(
            GuideStep(
                "Make the intention",
                "Intend in your heart to lift the state of major impurity.",
                of(Ruling.SUNNAH, Ruling.FARD),
                references = listOf(bukhari("1", "'Umar ibn al-Khattab")),
            ),
            GuideStep(
                "Wash the hands",
                "Wash both hands before putting them in the water.",
                both(Ruling.SUNNAH),
                references = listOf(bukhari("248", "'A'ishah")),
            ),
            GuideStep(
                "Wash the private parts",
                "Wash the private parts with the left hand, and remove any impurity from the body.",
                both(Ruling.SUNNAH),
                madhhabNote = Madhhab.entries.associateWith { "Any impurity on the body must be gone before the water that lifts the state reaches it." },
                references = listOf(bukhari("249", "Maymunah bint al-Harith")),
            ),
            GuideStep(
                "Make wudu",
                "Make wudu as for prayer. You may leave the feet to wash at the end, as in Maymunah's narration.",
                both(Ruling.SUNNAH),
                references = listOf(bukhari("248", "'A'ishah"), bukhari("249", "Maymunah bint al-Harith")),
            ),
            GuideStep(
                "Rinse the mouth and nose",
                "Rinse the mouth and the nose thoroughly. If you did so in your wudu, that is enough.",
                of(Ruling.FARD, Ruling.SUNNAH),
                madhhabNote = mapOf(Madhhab.HANAFI to "In ghusl, unlike wudu, rinsing the mouth and nose is fard: the ghusl is not valid without them."),
            ),
            GuideStep(
                "Wash the head",
                "Work wet fingers into the roots of the hair, then pour water over the head three times.",
                both(Ruling.SUNNAH),
                madhhabNote = Madhhab.entries.associateWith { "Water reaching the scalp and all the hair is fard." },
                references = listOf(bukhari("248", "'A'ishah")),
            ),
            GuideStep(
                "Wash the whole body",
                "Pour water over the whole body, the right side first and then the left, making sure it reaches the navel, the armpits, behind the ears and between the toes.",
                both(Ruling.FARD),
                references = listOf(bukhari("248", "'A'ishah"), bukhari("168", "'A'ishah")),
            ),
            GuideStep(
                "Wash the feet",
                "If you left them in your wudu, move aside and wash the feet.",
                both(Ruling.SUNNAH),
                references = listOf(bukhari("249", "Maymunah bint al-Harith")),
            ),
        ),
    )

    val all: List<Guide> = listOf(wudu, ghusl)
}
