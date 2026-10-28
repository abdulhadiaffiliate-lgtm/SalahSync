package com.salahsync.core.adhkar

import com.salahsync.core.quran.AyahRef
import java.time.LocalTime

enum class AdhkarSession(val displayName: String, val arabicName: String) {
    MORNING("Morning", "أذكار الصباح"),
    EVENING("Evening", "أذكار المساء"),
}

/**
 * Where a dhikr comes from. [number] is the hadith number on sunnah.com, and [grade]
 * names who graded it. Every reference here was checked against [url].
 */
data class HadithRef(
    val book: String,
    val number: String,
    val narrator: String,
    val grade: String,
    val url: String,
)

/** A passage of the Quran, read from the bundled Tanzil text. */
data class QuranPassage(val surah: Int, val ayahs: IntRange) {
    val refs: List<AyahRef> get() = ayahs.map { AyahRef(surah, it) }
}

data class Dhikr(
    val id: String,
    val title: String,
    /** The words as said in this session; empty for Quran passages, whose text comes from the mushaf. */
    val arabic: String,
    val meaning: String,
    val count: Int,
    val virtue: String,
    val references: List<HadithRef>,
    val quran: List<QuranPassage> = emptyList(),
    /** Anything the reader should know about the reference, such as a grading difference. */
    val note: String? = null,
) {
    val isQuran: Boolean get() = quran.isNotEmpty()
}

/**
 * Morning and evening adhkar. Only narrations from Sahih al-Bukhari, Sahih Muslim, or
 * the Sunan with a sahih or hasan grading are included, each with its source.
 */
object Adhkar {
    private const val SUNNAH = "https://sunnah.com/"

    private fun bukhari(n: String, narrator: String) =
        HadithRef("Sahih al-Bukhari", n, narrator, "Sahih (in Sahih al-Bukhari)", SUNNAH + "bukhari:$n")

    private fun muslim(n: String, narrator: String) =
        HadithRef("Sahih Muslim", n, narrator, "Sahih (in Sahih Muslim)", SUNNAH + "muslim:$n")

    private fun abuDawud(n: String, narrator: String, grade: String) =
        HadithRef("Sunan Abi Dawud", n, narrator, grade, SUNNAH + "abudawud:$n")

    private fun tirmidhi(n: String, narrator: String, grade: String) =
        HadithRef("Jami' at-Tirmidhi", n, narrator, grade, SUNNAH + "tirmidhi:$n")

    private val ayatAlKursi = Dhikr(
        id = "ayat_al_kursi",
        title = "Ayat al-Kursi",
        arabic = "",
        meaning = "Al-Baqarah 2:255. Allah: there is no god but Him, the Ever-Living, the Sustainer of all existence.",
        count = 1,
        virtue = "Whoever recites it in the morning is protected from the jinn until evening, and whoever recites it in the evening is protected until morning.",
        references = listOf(
            HadithRef(
                "Al-Mustadrak of al-Hakim", "2064", "Ubayy ibn Ka'b",
                "Sahih: al-Hakim graded its chain sound; al-Albani graded it sahih in Sahih at-Targhib wat-Tarhib (1/273)",
                SUNNAH + "hisn:75",
            ),
        ),
        quran = listOf(QuranPassage(2, 255..255)),
        note = "This narration is not in Bukhari or Muslim. Hisn al-Muslim (no. 75) cites it from al-Hakim, and also an-Nasa'i and at-Tabarani.",
    )

    private val threeQuls = Dhikr(
        id = "three_quls",
        title = "Al-Ikhlas, Al-Falaq and An-Nas",
        arabic = "",
        meaning = "The last three surahs of the Quran, each recited three times.",
        count = 3,
        virtue = "The Prophet ﷺ said: recite Qul huwa Allahu ahad and the two Mu'awwidhat three times in the evening and the morning; they will suffice you against everything.",
        references = listOf(
            abuDawud("5082", "'Abdullah ibn Khubayb", "Hasan (al-Albani)"),
            tirmidhi("3575", "'Abdullah ibn Khubayb", "Hasan (Darussalam); at-Tirmidhi called it hasan sahih gharib"),
        ),
        quran = listOf(QuranPassage(112, 1..4), QuranPassage(113, 1..5), QuranPassage(114, 1..6)),
    )

    private fun mulk(session: AdhkarSession) = Dhikr(
        id = "asbahna_wa_asbaha_al_mulk",
        title = if (session == AdhkarSession.MORNING) "Asbahna wa asbahal-mulku lillah" else "Amsayna wa amsal-mulku lillah",
        arabic = if (session == AdhkarSession.MORNING) {
            "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، " +
                "لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ، رَبِّ أَسْأَلُكَ خَيْرَ مَا فِي هَذَا الْيَوْمِ وَخَيْرَ مَا بَعْدَهُ، " +
                "وَأَعُوذُ بِكَ مِنْ شَرِّ مَا فِي هَذَا الْيَوْمِ وَشَرِّ مَا بَعْدَهُ، رَبِّ أَعُوذُ بِكَ مِنَ الْكَسَلِ وَسُوءِ الْكِبَرِ، " +
                "رَبِّ أَعُوذُ بِكَ مِنْ عَذَابٍ فِي النَّارِ وَعَذَابٍ فِي الْقَبْرِ"
        } else {
            "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، " +
                "لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ، رَبِّ أَسْأَلُكَ خَيْرَ مَا فِي هَذِهِ اللَّيْلَةِ وَخَيْرَ مَا بَعْدَهَا، " +
                "وَأَعُوذُ بِكَ مِنْ شَرِّ مَا فِي هَذِهِ اللَّيْلَةِ وَشَرِّ مَا بَعْدَهَا، رَبِّ أَعُوذُ بِكَ مِنَ الْكَسَلِ وَسُوءِ الْكِبَرِ، " +
                "رَبِّ أَعُوذُ بِكَ مِنْ عَذَابٍ فِي النَّارِ وَعَذَابٍ فِي الْقَبْرِ"
        },
        meaning = if (session == AdhkarSession.MORNING) {
            "We have reached the morning and the dominion belongs to Allah. Praise is for Allah; none has the right to be worshipped but Allah alone, without partner. His is the dominion and His the praise, and He has power over all things. My Lord, I ask You for the good of this day and what follows it, and seek refuge in You from its evil and the evil of what follows it. My Lord, I seek refuge in You from laziness and the misery of old age; from punishment in the Fire and punishment in the grave."
        } else {
            "We have reached the evening and the dominion belongs to Allah. Praise is for Allah; none has the right to be worshipped but Allah alone, without partner. His is the dominion and His the praise, and He has power over all things. My Lord, I ask You for the good of this night and what follows it, and seek refuge in You from its evil and the evil of what follows it. My Lord, I seek refuge in You from laziness and the misery of old age; from punishment in the Fire and punishment in the grave."
        },
        count = 1,
        virtue = "Ibn Mas'ud reported that the Prophet ﷺ said this when evening came, and in the morning said the same with \"asbahna wa asbaha al-mulku lillah\".",
        references = listOf(muslim("2723a", "'Abdullah ibn Mas'ud")),
    )

    private fun bika(session: AdhkarSession) = Dhikr(
        id = "allahumma_bika",
        title = if (session == AdhkarSession.MORNING) "Allahumma bika asbahna" else "Allahumma bika amsayna",
        arabic = if (session == AdhkarSession.MORNING) {
            "اللَّهُمَّ بِكَ أَصْبَحْنَا، وَبِكَ أَمْسَيْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ النُّشُورُ"
        } else {
            "اللَّهُمَّ بِكَ أَمْسَيْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ النُّشُورُ"
        },
        meaning = if (session == AdhkarSession.MORNING) {
            "O Allah, by You we enter the morning and by You we enter the evening, by You we live and by You we die, and to You is the resurrection."
        } else {
            "O Allah, by You we enter the evening, by You we live and by You we die, and to You is the resurrection."
        },
        count = 1,
        virtue = "Abu Hurayrah reported that the Prophet ﷺ taught his companions to say this in the morning and in the evening.",
        references = listOf(
            abuDawud("5068", "Abu Hurayrah", "Sahih (al-Albani)"),
            tirmidhi("3391", "Abu Hurayrah", "Sahih (Darussalam)"),
        ),
        note = "The evening wording follows Sunan Abi Dawud 5068.",
    )

    private val sayyidAlIstighfar = Dhikr(
        id = "sayyid_al_istighfar",
        title = "Sayyid al-Istighfar",
        arabic = "اللَّهُمَّ أَنْتَ رَبِّي، لَا إِلَهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، " +
            "أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ لَكَ بِذَنْبِي، فَاغْفِرْ لِي، فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ",
        meaning = "O Allah, You are my Lord; none has the right to be worshipped but You. You created me and I am Your servant, and I keep Your covenant and promise as best I can. I seek refuge in You from the evil of what I have done. I acknowledge Your favour upon me and I acknowledge my sin, so forgive me, for none forgives sins but You.",
        count = 1,
        virtue = "Whoever says it during the day with firm faith and dies that day before evening is among the people of Paradise; and whoever says it at night with firm faith and dies before morning is among the people of Paradise.",
        references = listOf(bukhari("6306", "Shaddad ibn Aws")),
    )

    private val fatir = Dhikr(
        id = "fatir_as_samawat",
        title = "Allahumma fatir as-samawati wal-ard",
        arabic = "اللَّهُمَّ فَاطِرَ السَّمَاوَاتِ وَالْأَرْضِ، عَالِمَ الْغَيْبِ وَالشَّهَادَةِ، رَبَّ كُلِّ شَيْءٍ وَمَلِيكَهُ، " +
            "أَشْهَدُ أَنْ لَا إِلَهَ إِلَّا أَنْتَ، أَعُوذُ بِكَ مِنْ شَرِّ نَفْسِي، وَمِنْ شَرِّ الشَّيْطَانِ وَشِرْكِهِ",
        meaning = "O Allah, Originator of the heavens and the earth, Knower of the unseen and the seen, Lord and Sovereign of all things: I bear witness that none has the right to be worshipped but You. I seek refuge in You from the evil of my soul, and from the evil of Shaytan and his call to associate partners with You.",
        count = 1,
        virtue = "Abu Bakr asked the Prophet ﷺ for words to say in the morning and evening, and he taught him this, to be said in the morning, the evening and on going to bed.",
        references = listOf(abuDawud("5067", "Abu Hurayrah, from Abu Bakr as-Siddiq", "Sahih (al-Albani)")),
    )

    private val bismillah = Dhikr(
        id = "bismillah_la_yadurru",
        title = "Bismillahil-ladhi la yadurru",
        arabic = "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ، وَهُوَ السَّمِيعُ الْعَلِيمُ",
        meaning = "In the name of Allah, with whose name nothing on earth or in the heavens can cause harm, and He is the All-Hearing, the All-Knowing.",
        count = 3,
        virtue = "Whoever says it three times will not be struck by a sudden affliction until morning, and whoever says it three times in the morning will not be struck by one until evening.",
        references = listOf(abuDawud("5088", "'Uthman ibn 'Affan", "Sahih (al-Albani)")),
    )

    private val afiyah = Dhikr(
        id = "al_afw_wal_afiyah",
        title = "Allahumma inni as'alukal-'afiyah",
        arabic = "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَافِيَةَ فِي الدُّنْيَا وَالْآخِرَةِ، اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي دِينِي وَدُنْيَايَ وَأَهْلِي وَمَالِي، " +
            "اللَّهُمَّ اسْتُرْ عَوْرَاتِي، وَآمِنْ رَوْعَاتِي، اللَّهُمَّ احْفَظْنِي مِنْ بَيْنِ يَدَيَّ، وَمِنْ خَلْفِي، وَعَنْ يَمِينِي، وَعَنْ شِمَالِي، وَمِنْ فَوْقِي، " +
            "وَأَعُوذُ بِعَظَمَتِكَ أَنْ أُغْتَالَ مِنْ تَحْتِي",
        meaning = "O Allah, I ask You for well-being in this world and the Hereafter. O Allah, I ask You for pardon and well-being in my religion, my worldly life, my family and my wealth. O Allah, cover my faults and calm my fears. O Allah, guard me from in front of me and behind me, from my right and my left, and from above me; and I seek refuge in Your greatness from being taken unawares from beneath me.",
        count = 1,
        virtue = "Ibn 'Umar reported that the Messenger of Allah ﷺ never left these supplications in the morning and the evening.",
        references = listOf(abuDawud("5074", "'Abdullah ibn 'Umar", "Sahih (al-Albani)")),
    )

    private val afini = Dhikr(
        id = "allahumma_afini",
        title = "Allahumma 'afini fi badani",
        arabic = "اللَّهُمَّ عَافِنِي فِي بَدَنِي، اللَّهُمَّ عَافِنِي فِي سَمْعِي، اللَّهُمَّ عَافِنِي فِي بَصَرِي، لَا إِلَهَ إِلَّا أَنْتَ",
        meaning = "O Allah, grant me well-being in my body. O Allah, grant me well-being in my hearing. O Allah, grant me well-being in my sight. None has the right to be worshipped but You.",
        count = 3,
        virtue = "Abu Bakrah's son heard him say this three times every morning and evening, and he said he had heard the Prophet ﷺ supplicate with these words.",
        references = listOf(abuDawud("5090", "Abu Bakrah", "Hasan in its chain (al-Albani)")),
    )

    private val adadaKhalqih = Dhikr(
        id = "adada_khalqih",
        title = "SubhanAllahi wa bihamdihi 'adada khalqih",
        arabic = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، عَدَدَ خَلْقِهِ، وَرِضَا نَفْسِهِ، وَزِنَةَ عَرْشِهِ، وَمِدَادَ كَلِمَاتِهِ",
        meaning = "Glory and praise be to Allah, as many times as the number of His creation, as much as pleases Him, as much as the weight of His Throne, and as much as the ink of His words.",
        count = 3,
        virtue = "The Prophet ﷺ left Juwayriyah at her place of prayer after Fajr and returned in the forenoon; he said these words, said three times, would outweigh all she had said since morning.",
        references = listOf(muslim("2726a", "Juwayriyah bint al-Harith")),
    )

    private val kalimatillah = Dhikr(
        id = "kalimatillah_at_tammat",
        title = "A'udhu bikalimatillahit-tammat",
        arabic = "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ",
        meaning = "I seek refuge in the perfect words of Allah from the evil of what He has created.",
        count = 3,
        virtue = "A man stung by a scorpion was told by the Prophet ﷺ that had he said this when evening came, it would not have harmed him. At-Tirmidhi's narration: whoever says it three times in the evening will not be harmed by a sting that night.",
        references = listOf(
            muslim("2709a", "Abu Hurayrah"),
            tirmidhi("3604b", "Abu Hurayrah", "Sahih (Darussalam); at-Tirmidhi called it hasan"),
        ),
        note = "Muslim's narration gives no number; the three times is from at-Tirmidhi's.",
    )

    private val tasbih100 = Dhikr(
        id = "subhanallahi_wa_bihamdih",
        title = "SubhanAllahi wa bihamdih",
        arabic = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
        meaning = "Glory and praise be to Allah.",
        count = 100,
        virtue = "Whoever says it a hundred times in the morning and in the evening, no one will come on the Day of Resurrection with anything better, except someone who said the same or more.",
        references = listOf(muslim("2692", "Abu Hurayrah")),
    )

    private val tahlil100 = Dhikr(
        id = "la_ilaha_illallah_wahdah",
        title = "La ilaha illallahu wahdahu",
        arabic = "لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ، وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
        meaning = "None has the right to be worshipped but Allah alone, without partner. His is the dominion and His is the praise, and He has power over all things.",
        count = 100,
        virtue = "Whoever says it a hundred times in a day has the reward of freeing ten slaves, a hundred good deeds are written for him and a hundred sins erased, and it is a protection from Shaytan that day until evening.",
        references = listOf(bukhari("3293", "Abu Hurayrah")),
        note = "The hadith says \"in a day\"; it is placed with the morning adhkar so it protects for the whole day.",
    )

    /** Morning adhkar from Fajr until Asr; evening adhkar from Asr through the night. */
    fun sessionAt(time: LocalTime): AdhkarSession =
        if (time.hour in 3 until 15) AdhkarSession.MORNING else AdhkarSession.EVENING

    fun forSession(session: AdhkarSession): List<Dhikr> = when (session) {
        AdhkarSession.MORNING -> listOf(
            ayatAlKursi, threeQuls, mulk(session), bika(session), sayyidAlIstighfar, fatir,
            bismillah, afiyah, afini, adadaKhalqih, tasbih100, tahlil100,
        )
        AdhkarSession.EVENING -> listOf(
            ayatAlKursi, threeQuls, mulk(session), bika(session), sayyidAlIstighfar, fatir,
            bismillah, afiyah, afini, kalimatillah, tasbih100,
        )
    }
}
