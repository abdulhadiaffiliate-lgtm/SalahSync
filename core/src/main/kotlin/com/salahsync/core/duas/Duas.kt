package com.salahsync.core.duas

import com.salahsync.core.adhkar.HadithRef
import com.salahsync.core.quran.AyahRef
import com.salahsync.core.sources.Sunnah.abuDawud
import com.salahsync.core.sources.Sunnah.bukhari
import com.salahsync.core.sources.Sunnah.ibnMajah
import com.salahsync.core.sources.Sunnah.muslim
import com.salahsync.core.sources.Sunnah.tirmidhi

enum class DuaCategory(val displayName: String, val arabicName: String) {
    SLEEP("Sleep and waking", "النوم والاستيقاظ"),
    PURITY("Purification", "الطهارة"),
    HOME("Home and mosque", "البيت والمسجد"),
    FOOD("Eating and fasting", "الطعام والصيام"),
    PRAYER("After prayer", "بعد الصلاة"),
    TRAVEL("Travel", "السفر"),
    HARDSHIP("Worry and hardship", "الهم والكرب"),
    EVERYDAY("Everyday moments", "أحوال اليوم"),
    ASKING("Asking of Allah", "جوامع الدعاء"),
}

data class Dua(
    val id: String,
    val category: DuaCategory,
    val title: String,
    /** When to say it, in a few words. */
    val occasion: String,
    val arabic: String,
    val meaning: String,
    val references: List<HadithRef>,
    /** Set when the same words are also an ayah of the Quran. */
    val ayah: AyahRef? = null,
    val count: Int = 1,
    /** Anything the reader should know about the wording or the reference. */
    val note: String? = null,
)

/**
 * Daily supplications. Each one is quoted in the wording of its first reference, and every
 * reference was checked on sunnah.com: Sahih al-Bukhari and Sahih Muslim, or the Sunan with
 * the sahih or hasan grading shown there.
 */
object Duas {
    val all: List<Dua> = listOf(
        Dua(
            id = "before_sleep",
            category = DuaCategory.SLEEP,
            title = "Before sleeping",
            occasion = "On lying down to sleep",
            arabic = "بِاسْمِكَ أَمُوتُ وَأَحْيَا",
            meaning = "In Your name I die and I live.",
            references = listOf(bukhari("6312", "Hudhayfah ibn al-Yaman")),
        ),
        Dua(
            id = "on_waking",
            category = DuaCategory.SLEEP,
            title = "On waking up",
            occasion = "On waking from sleep",
            arabic = "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ",
            meaning = "Praise be to Allah, who gave us life after He had caused us to die, and to Him is the return.",
            references = listOf(bukhari("6312", "Hudhayfah ibn al-Yaman")),
        ),
        Dua(
            id = "entering_toilet",
            category = DuaCategory.PURITY,
            title = "Entering the toilet",
            occasion = "Before entering the toilet",
            arabic = "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْخُبُثِ وَالْخَبَائِثِ",
            meaning = "O Allah, I seek refuge in You from the male and female devils (from all evil and evil ones).",
            references = listOf(bukhari("142", "Anas ibn Malik")),
        ),
        Dua(
            id = "leaving_toilet",
            category = DuaCategory.PURITY,
            title = "Leaving the toilet",
            occasion = "On coming out of the toilet",
            arabic = "غُفْرَانَكَ",
            meaning = "I ask You for Your forgiveness.",
            references = listOf(abuDawud("30", "'A'ishah", "Sahih (al-Albani)")),
        ),
        Dua(
            id = "before_wudu",
            category = DuaCategory.PURITY,
            title = "Before wudu",
            occasion = "At the start of wudu",
            arabic = "بِسْمِ اللَّهِ",
            meaning = "In the name of Allah.",
            references = listOf(abuDawud("101", "Abu Hurayrah", "Sahih (al-Albani)")),
            note = "The hadith says there is no wudu for one who does not mention Allah's name over it. The Hanafi and Shafi'i schools hold saying it is sunnah, not a condition of wudu.",
        ),
        Dua(
            id = "after_wudu",
            category = DuaCategory.PURITY,
            title = "After wudu",
            occasion = "On completing wudu",
            arabic = "أَشْهَدُ أَنْ لَا إِلَهَ إِلَّا اللَّهُ وَأَنَّ مُحَمَّدًا عَبْدُ اللَّهِ وَرَسُولُهُ",
            meaning = "I bear witness that none has the right to be worshipped but Allah, and that Muhammad is the servant of Allah and His Messenger.",
            references = listOf(muslim("234a", "'Uqbah ibn 'Amir, from 'Umar ibn al-Khattab")),
            note = "Whoever completes wudu well and says this, the eight gates of Paradise are opened for him to enter by whichever he wishes.",
        ),
        Dua(
            id = "leaving_home",
            category = DuaCategory.HOME,
            title = "Leaving the home",
            occasion = "On going out of the house",
            arabic = "بِسْمِ اللَّهِ، تَوَكَّلْتُ عَلَى اللَّهِ، لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
            meaning = "In the name of Allah, I put my trust in Allah; there is no might and no power except with Allah.",
            references = listOf(
                abuDawud("5095", "Anas ibn Malik", "Sahih (al-Albani)"),
                tirmidhi("3426", "Anas ibn Malik", "Sahih (Darussalam); at-Tirmidhi called it hasan sahih gharib"),
            ),
        ),
        Dua(
            id = "entering_mosque",
            category = DuaCategory.HOME,
            title = "Entering the mosque",
            occasion = "On entering the mosque",
            arabic = "اللَّهُمَّ افْتَحْ لِي أَبْوَابَ رَحْمَتِكَ",
            meaning = "O Allah, open for me the doors of Your mercy.",
            references = listOf(muslim("713a", "Abu Usayd, or Abu Humayd as-Sa'idi")),
        ),
        Dua(
            id = "leaving_mosque",
            category = DuaCategory.HOME,
            title = "Leaving the mosque",
            occasion = "On leaving the mosque",
            arabic = "اللَّهُمَّ إِنِّي أَسْأَلُكَ مِنْ فَضْلِكَ",
            meaning = "O Allah, I ask You of Your bounty.",
            references = listOf(muslim("713a", "Abu Usayd, or Abu Humayd as-Sa'idi")),
        ),
        Dua(
            id = "before_eating",
            category = DuaCategory.FOOD,
            title = "Before eating",
            occasion = "Before you start to eat",
            arabic = "بِسْمِ اللَّهِ",
            meaning = "In the name of Allah.",
            references = listOf(bukhari("5376", "'Umar ibn Abi Salamah")),
            note = "The Prophet ﷺ told him: \"O boy, mention the name of Allah, eat with your right hand, and eat from what is in front of you.\"",
        ),
        Dua(
            id = "forgot_bismillah",
            category = DuaCategory.FOOD,
            title = "If you forgot Bismillah",
            occasion = "On remembering partway through a meal",
            arabic = "بِسْمِ اللَّهِ أَوَّلَهُ وَآخِرَهُ",
            meaning = "In the name of Allah, at its beginning and at its end.",
            references = listOf(abuDawud("3767", "'A'ishah", "Sahih (al-Albani)")),
        ),
        Dua(
            id = "after_eating",
            category = DuaCategory.FOOD,
            title = "After eating",
            occasion = "On finishing a meal",
            arabic = "الْحَمْدُ لِلَّهِ الَّذِي أَطْعَمَنِي هَذَا الطَّعَامَ وَرَزَقَنِيهِ مِنْ غَيْرِ حَوْلٍ مِنِّي وَلَا قُوَّةٍ",
            meaning = "Praise be to Allah, who fed me this food and provided it for me without any might or power on my part.",
            references = listOf(abuDawud("4023", "Mu'adh ibn Anas", "Hasan (al-Albani), without the addition \"and what comes after\"")),
            note = "The hadith says whoever says this has his past sins forgiven.",
        ),
        Dua(
            id = "breaking_fast",
            category = DuaCategory.FOOD,
            title = "On breaking the fast",
            occasion = "At iftar",
            arabic = "ذَهَبَ الظَّمَأُ وَابْتَلَّتِ الْعُرُوقُ وَثَبَتَ الْأَجْرُ إِنْ شَاءَ اللَّهُ",
            meaning = "The thirst is gone, the veins are moistened, and the reward is certain, if Allah wills.",
            references = listOf(abuDawud("2357", "'Abdullah ibn 'Umar", "Hasan (al-Albani)")),
        ),
        Dua(
            id = "after_salam",
            category = DuaCategory.PRAYER,
            title = "On finishing the prayer",
            occasion = "Right after the salam of each prayer",
            arabic = "أَسْتَغْفِرُ اللَّهَ، أَسْتَغْفِرُ اللَّهَ، أَسْتَغْفِرُ اللَّهَ، اللَّهُمَّ أَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ، تَبَارَكْتَ ذَا الْجَلَالِ وَالْإِكْرَامِ",
            meaning = "I ask Allah's forgiveness (three times). O Allah, You are Peace and from You is peace. Blessed are You, Possessor of majesty and honour.",
            references = listOf(muslim("591", "Thawban")),
            note = "Thawban says he sought forgiveness three times; al-Awza'i, a narrator, explained it as saying \"astaghfirullah\".",
        ),
        Dua(
            id = "tasbih_after_prayer",
            category = DuaCategory.PRAYER,
            title = "Tasbih after the prayer",
            occasion = "After each obligatory prayer",
            arabic = "سُبْحَانَ اللَّهِ (٣٣)، الْحَمْدُ لِلَّهِ (٣٣)، اللَّهُ أَكْبَرُ (٣٣)، " +
                "لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ، وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
            meaning = "Glory be to Allah (33 times), praise be to Allah (33 times), Allah is the Greatest (33 times), and to complete the hundred: none has the right to be worshipped but Allah alone, without partner; His is the dominion and His is the praise, and He has power over all things.",
            references = listOf(muslim("597a", "Abu Hurayrah")),
            note = "Whoever says this after every prayer has his sins forgiven, even if they are like the foam of the sea. The Tasbih counter can count it for you.",
        ),
        Dua(
            id = "travel",
            category = DuaCategory.TRAVEL,
            title = "Setting out on a journey",
            occasion = "On mounting your ride for a journey",
            arabic = "اللَّهُ أَكْبَرُ، اللَّهُ أَكْبَرُ، اللَّهُ أَكْبَرُ، سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ، وَإِنَّا إِلَى رَبِّنَا لَمُنْقَلِبُونَ، " +
                "اللَّهُمَّ إِنَّا نَسْأَلُكَ فِي سَفَرِنَا هَذَا الْبِرَّ وَالتَّقْوَى، وَمِنَ الْعَمَلِ مَا تَرْضَى، اللَّهُمَّ هَوِّنْ عَلَيْنَا سَفَرَنَا هَذَا وَاطْوِ عَنَّا بُعْدَهُ، " +
                "اللَّهُمَّ أَنْتَ الصَّاحِبُ فِي السَّفَرِ، وَالْخَلِيفَةُ فِي الْأَهْلِ، اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنْ وَعْثَاءِ السَّفَرِ، وَكَآبَةِ الْمَنْظَرِ، وَسُوءِ الْمُنْقَلَبِ فِي الْمَالِ وَالْأَهْلِ",
            meaning = "Allah is the Greatest (three times). Glory be to Him who has subjected this to us, for we could never have done it ourselves, and to our Lord we will surely return. O Allah, we ask You on this journey for righteousness and piety, and for deeds that please You. O Allah, make this journey easy for us and shorten its distance. O Allah, You are the Companion on the journey and the One who looks after the family. O Allah, I seek refuge in You from the hardship of travel, from distressing sights, and from an evil return to wealth and family.",
            references = listOf(muslim("1342", "'Abdullah ibn 'Umar")),
            note = "On returning, say the same and add: آيِبُونَ تَائِبُونَ عَابِدُونَ لِرَبِّنَا حَامِدُونَ (returning, repenting, worshipping, and praising our Lord).",
        ),
        Dua(
            id = "distress",
            category = DuaCategory.HARDSHIP,
            title = "In distress",
            occasion = "At a time of great distress",
            arabic = "لَا إِلَهَ إِلَّا اللَّهُ الْعَظِيمُ الْحَلِيمُ، لَا إِلَهَ إِلَّا اللَّهُ رَبُّ الْعَرْشِ الْعَظِيمِ، " +
                "لَا إِلَهَ إِلَّا اللَّهُ رَبُّ السَّمَاوَاتِ، وَرَبُّ الْأَرْضِ، وَرَبُّ الْعَرْشِ الْكَرِيمِ",
            meaning = "None has the right to be worshipped but Allah, the Mighty, the Forbearing. None has the right to be worshipped but Allah, Lord of the Mighty Throne. None has the right to be worshipped but Allah, Lord of the heavens, Lord of the earth, and Lord of the Noble Throne.",
            references = listOf(bukhari("6346", "'Abdullah ibn 'Abbas")),
        ),
        Dua(
            id = "worry_and_debt",
            category = DuaCategory.HARDSHIP,
            title = "For worry, grief and debt",
            occasion = "When anxious, low or in debt",
            arabic = "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ، وَالْعَجْزِ وَالْكَسَلِ، وَالْجُبْنِ وَالْبُخْلِ، وَضَلَعِ الدَّيْنِ، وَغَلَبَةِ الرِّجَالِ",
            meaning = "O Allah, I seek refuge in You from worry and grief, from helplessness and laziness, from cowardice and miserliness, from the burden of debt and from being overpowered by men.",
            references = listOf(bukhari("6369", "Anas ibn Malik")),
            note = "Anas said he often heard the Prophet ﷺ make this supplication.",
        ),
        Dua(
            id = "dua_of_yunus",
            category = DuaCategory.HARDSHIP,
            title = "The supplication of Yunus",
            occasion = "In any difficulty",
            arabic = "لَا إِلَهَ إِلَّا أَنْتَ سُبْحَانَكَ إِنِّي كُنْتُ مِنَ الظَّالِمِينَ",
            meaning = "None has the right to be worshipped but You. Glory be to You; indeed I have been among the wrongdoers.",
            references = listOf(tirmidhi("3505", "Sa'd ibn Abi Waqqas", "Sahih (al-Albani)")),
            ayah = AyahRef(21, 87),
            note = "The Prophet ﷺ said no Muslim calls upon his Lord with it for anything except that He answers him.",
        ),
        Dua(
            id = "sneezing",
            category = DuaCategory.EVERYDAY,
            title = "Sneezing",
            occasion = "When you sneeze, or hear someone sneeze",
            arabic = "الْحَمْدُ لِلَّهِ\nيَرْحَمُكَ اللَّهُ\nيَهْدِيكُمُ اللَّهُ وَيُصْلِحُ بَالَكُمْ",
            meaning = "The one who sneezes says: Praise be to Allah. The one who hears replies: May Allah have mercy on you. The one who sneezed answers: May Allah guide you and set your affairs right.",
            references = listOf(bukhari("6224", "Abu Hurayrah")),
        ),
        Dua(
            id = "anger",
            category = DuaCategory.EVERYDAY,
            title = "When angry",
            occasion = "When anger rises",
            arabic = "أَعُوذُ بِاللَّهِ مِنَ الشَّيْطَانِ",
            meaning = "I seek refuge in Allah from Shaytan.",
            references = listOf(bukhari("3282", "Sulayman ibn Surad")),
            note = "Two men were quarrelling; the Prophet ﷺ said if the angry one said this, his anger would leave him.",
        ),
        Dua(
            id = "rain",
            category = DuaCategory.EVERYDAY,
            title = "When it rains",
            occasion = "On seeing rain",
            arabic = "اللَّهُمَّ صَيِّبًا نَافِعًا",
            meaning = "O Allah, let it be a beneficial rain.",
            references = listOf(bukhari("1032", "'A'ishah")),
        ),
        Dua(
            id = "visiting_sick",
            category = DuaCategory.EVERYDAY,
            title = "Visiting the sick",
            occasion = "To someone who is unwell",
            arabic = "لَا بَأْسَ، طَهُورٌ إِنْ شَاءَ اللَّهُ",
            meaning = "No harm; it is a purification, if Allah wills.",
            references = listOf(bukhari("5656", "'Abdullah ibn 'Abbas")),
        ),
        Dua(
            id = "good_in_both_worlds",
            category = DuaCategory.ASKING,
            title = "Good in this world and the next",
            occasion = "At any time; the Prophet's most frequent supplication",
            arabic = "اللَّهُمَّ رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
            meaning = "O Allah, our Lord, give us good in this world and good in the Hereafter, and protect us from the punishment of the Fire.",
            references = listOf(bukhari("6389", "Anas ibn Malik")),
            ayah = AyahRef(2, 201),
            note = "Anas said this was the supplication the Prophet ﷺ made most often.",
        ),
        Dua(
            id = "guidance_and_contentment",
            category = DuaCategory.ASKING,
            title = "Guidance, piety and contentment",
            occasion = "At any time",
            arabic = "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْهُدَى وَالتُّقَى وَالْعَفَافَ وَالْغِنَى",
            meaning = "O Allah, I ask You for guidance, piety, chastity and freedom from need.",
            references = listOf(muslim("2721a", "'Abdullah ibn Mas'ud")),
        ),
        Dua(
            id = "steadfast_heart",
            category = DuaCategory.ASKING,
            title = "A steadfast heart",
            occasion = "At any time",
            arabic = "يَا مُقَلِّبَ الْقُلُوبِ ثَبِّتْ قَلْبِي عَلَى دِينِكَ",
            meaning = "O Turner of hearts, keep my heart firm upon Your religion.",
            references = listOf(tirmidhi("2140", "Anas ibn Malik", "Hasan (at-Tirmidhi)")),
            note = "Anas said the Prophet ﷺ used to say this often.",
        ),
        Dua(
            id = "laylat_al_qadr",
            category = DuaCategory.ASKING,
            title = "For Laylat al-Qadr",
            occasion = "In the last ten nights of Ramadan",
            arabic = "اللَّهُمَّ إِنَّكَ عَفُوٌّ تُحِبُّ الْعَفْوَ فَاعْفُ عَنِّي",
            meaning = "O Allah, You are Pardoning and love to pardon, so pardon me.",
            references = listOf(ibnMajah("3850", "'A'ishah", "Sahih (Darussalam)")),
            note = "'A'ishah asked what to say if she knew which night was Laylat al-Qadr, and was taught this.",
        ),
        Dua(
            id = "istikhara",
            category = DuaCategory.ASKING,
            title = "Istikharah, seeking Allah's choice",
            occasion = "After two voluntary rak'ahs, when deciding on a matter",
            arabic = "اللَّهُمَّ إِنِّي أَسْتَخِيرُكَ بِعِلْمِكَ، وَأَسْتَقْدِرُكَ بِقُدْرَتِكَ، وَأَسْأَلُكَ مِنْ فَضْلِكَ الْعَظِيمِ، فَإِنَّكَ تَقْدِرُ وَلَا أَقْدِرُ، وَتَعْلَمُ وَلَا أَعْلَمُ، وَأَنْتَ عَلَّامُ الْغُيُوبِ، " +
                "اللَّهُمَّ إِنْ كُنْتَ تَعْلَمُ أَنَّ هَذَا الْأَمْرَ خَيْرٌ لِي فِي دِينِي وَمَعَاشِي وَعَاقِبَةِ أَمْرِي، فَاقْدُرْهُ لِي وَيَسِّرْهُ لِي، ثُمَّ بَارِكْ لِي فِيهِ، " +
                "وَإِنْ كُنْتَ تَعْلَمُ أَنَّ هَذَا الْأَمْرَ شَرٌّ لِي فِي دِينِي وَمَعَاشِي وَعَاقِبَةِ أَمْرِي، فَاصْرِفْهُ عَنِّي وَاصْرِفْنِي عَنْهُ، وَاقْدُرْ لِيَ الْخَيْرَ حَيْثُ كَانَ، ثُمَّ أَرْضِنِي بِهِ",
            meaning = "O Allah, I seek Your choice by Your knowledge, and seek ability by Your power, and ask of Your immense bounty, for You are able and I am not, You know and I do not, and You are the Knower of the unseen. O Allah, if You know this matter to be good for me in my religion, my livelihood and the outcome of my affairs, then decree it for me, make it easy for me and bless me in it. And if You know this matter to be bad for me in my religion, my livelihood and the outcome of my affairs, then turn it away from me and turn me away from it, decree for me what is good wherever it may be, and make me content with it.",
            references = listOf(bukhari("1166", "Jabir ibn 'Abdullah")),
            note = "Name your need where it says \"this matter\". The narrator was unsure whether the Prophet ﷺ said \"the outcome of my affairs\" or \"my affairs now and later\"; both are in the hadith.",
        ),
    )

    fun inCategory(category: DuaCategory): List<Dua> = all.filter { it.category == category }
}
