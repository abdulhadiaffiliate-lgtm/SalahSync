package com.salahsync.core.names

import com.salahsync.core.adhkar.HadithRef
import com.salahsync.core.sources.Sunnah.bukhari
import java.text.Normalizer
import java.time.LocalDate

/** One of the Beautiful Names, numbered in the order of the well-known list. */
data class AllahName(val number: Int, val arabic: String, val transliteration: String, val meaning: String) {
    /** The transliteration in plain letters ("Ar-Rahman"), for fonts without the dotted and long vowels. */
    val plain: String
        get() = Normalizer.normalize(transliteration, Normalizer.Form.NFD)
            .filter { Character.getType(it) != Character.NON_SPACING_MARK.toInt() }
            .replace('ʿ', '\'').replace('ʾ', '\'')
}

/**
 * Al-Asma' al-Husna: the 99 Names of Allah. The Arabic and order follow the listing narrated in
 * Jami' at-Tirmidhi 3507, with Al-Ahad after Al-Wahid as in most lists, so the names number 99
 * without counting "Allah". That the names number 99 is in Sahih al-Bukhari ([source]).
 */
object AllahNames {
    val source: HadithRef = bukhari("2736", "Abu Hurayrah")

    val all: List<AllahName> = listOf(
        AllahName(1, "الرَّحْمَنُ", "Ar-Raḥmān", "The Most Gracious"),
        AllahName(2, "الرَّحِيمُ", "Ar-Raḥīm", "The Most Merciful"),
        AllahName(3, "الْمَلِكُ", "Al-Malik", "The King"),
        AllahName(4, "الْقُدُّوسُ", "Al-Quddūs", "The Most Holy"),
        AllahName(5, "السَّلاَمُ", "As-Salām", "The Source of Peace"),
        AllahName(6, "الْمُؤْمِنُ", "Al-Muʾmin", "The Giver of Security"),
        AllahName(7, "الْمُهَيْمِنُ", "Al-Muhaymin", "The Guardian over all"),
        AllahName(8, "الْعَزِيزُ", "Al-ʿAzīz", "The Almighty"),
        AllahName(9, "الْجَبَّارُ", "Al-Jabbār", "The Compeller"),
        AllahName(10, "الْمُتَكَبِّرُ", "Al-Mutakabbir", "The Supreme in Greatness"),
        AllahName(11, "الْخَالِقُ", "Al-Khāliq", "The Creator"),
        AllahName(12, "الْبَارِئُ", "Al-Bāriʾ", "The Maker"),
        AllahName(13, "الْمُصَوِّرُ", "Al-Muṣawwir", "The Fashioner of Forms"),
        AllahName(14, "الْغَفَّارُ", "Al-Ghaffār", "The Ever-Forgiving"),
        AllahName(15, "الْقَهَّارُ", "Al-Qahhār", "The All-Subduing"),
        AllahName(16, "الْوَهَّابُ", "Al-Wahhāb", "The Bestower"),
        AllahName(17, "الرَّزَّاقُ", "Ar-Razzāq", "The Provider"),
        AllahName(18, "الْفَتَّاحُ", "Al-Fattāḥ", "The Opener, the Judge"),
        AllahName(19, "الْعَلِيمُ", "Al-ʿAlīm", "The All-Knowing"),
        AllahName(20, "الْقَابِضُ", "Al-Qābiḍ", "The Withholder"),
        AllahName(21, "الْبَاسِطُ", "Al-Bāsiṭ", "The Extender"),
        AllahName(22, "الْخَافِضُ", "Al-Khāfiḍ", "The One Who Lowers"),
        AllahName(23, "الرَّافِعُ", "Ar-Rāfiʿ", "The One Who Raises"),
        AllahName(24, "الْمُعِزُّ", "Al-Muʿizz", "The Giver of Honour"),
        AllahName(25, "الْمُذِلُّ", "Al-Mudhill", "The Giver of Disgrace"),
        AllahName(26, "السَّمِيعُ", "As-Samīʿ", "The All-Hearing"),
        AllahName(27, "الْبَصِيرُ", "Al-Baṣīr", "The All-Seeing"),
        AllahName(28, "الْحَكَمُ", "Al-Ḥakam", "The Judge"),
        AllahName(29, "الْعَدْلُ", "Al-ʿAdl", "The Utterly Just"),
        AllahName(30, "اللَّطِيفُ", "Al-Laṭīf", "The Subtle, the Kind"),
        AllahName(31, "الْخَبِيرُ", "Al-Khabīr", "The All-Aware"),
        AllahName(32, "الْحَلِيمُ", "Al-Ḥalīm", "The Forbearing"),
        AllahName(33, "الْعَظِيمُ", "Al-ʿAẓīm", "The Magnificent"),
        AllahName(34, "الْغَفُورُ", "Al-Ghafūr", "The All-Forgiving"),
        AllahName(35, "الشَّكُورُ", "Ash-Shakūr", "The Most Appreciative"),
        AllahName(36, "الْعَلِيُّ", "Al-ʿAliyy", "The Most High"),
        AllahName(37, "الْكَبِيرُ", "Al-Kabīr", "The Most Great"),
        AllahName(38, "الْحَفِيظُ", "Al-Ḥafīẓ", "The Preserver"),
        AllahName(39, "الْمُقِيتُ", "Al-Muqīt", "The Nourisher"),
        AllahName(40, "الْحَسِيبُ", "Al-Ḥasīb", "The Reckoner"),
        AllahName(41, "الْجَلِيلُ", "Al-Jalīl", "The Majestic"),
        AllahName(42, "الْكَرِيمُ", "Al-Karīm", "The Most Generous"),
        AllahName(43, "الرَّقِيبُ", "Ar-Raqīb", "The Watchful"),
        AllahName(44, "الْمُجِيبُ", "Al-Mujīb", "The Answerer of Prayers"),
        AllahName(45, "الْوَاسِعُ", "Al-Wāsiʿ", "The All-Encompassing"),
        AllahName(46, "الْحَكِيمُ", "Al-Ḥakīm", "The All-Wise"),
        AllahName(47, "الْوَدُودُ", "Al-Wadūd", "The Most Loving"),
        AllahName(48, "الْمَجِيدُ", "Al-Majīd", "The Most Glorious"),
        AllahName(49, "الْبَاعِثُ", "Al-Bāʿith", "The Resurrector"),
        AllahName(50, "الشَّهِيدُ", "Ash-Shahīd", "The Witness"),
        AllahName(51, "الْحَقُّ", "Al-Ḥaqq", "The Truth"),
        AllahName(52, "الْوَكِيلُ", "Al-Wakīl", "The Trustee"),
        AllahName(53, "الْقَوِيُّ", "Al-Qawiyy", "The All-Strong"),
        AllahName(54, "الْمَتِينُ", "Al-Matīn", "The Firm"),
        AllahName(55, "الْوَلِيُّ", "Al-Waliyy", "The Protecting Friend"),
        AllahName(56, "الْحَمِيدُ", "Al-Ḥamīd", "The Praiseworthy"),
        AllahName(57, "الْمُحْصِي", "Al-Muḥṣī", "The One Who Counts All"),
        AllahName(58, "الْمُبْدِئُ", "Al-Mubdiʾ", "The Originator"),
        AllahName(59, "الْمُعِيدُ", "Al-Muʿīd", "The Restorer"),
        AllahName(60, "الْمُحْيِي", "Al-Muḥyī", "The Giver of Life"),
        AllahName(61, "الْمُمِيتُ", "Al-Mumīt", "The Bringer of Death"),
        AllahName(62, "الْحَيُّ", "Al-Ḥayy", "The Ever-Living"),
        AllahName(63, "الْقَيُّومُ", "Al-Qayyūm", "The Sustainer of All"),
        AllahName(64, "الْوَاجِدُ", "Al-Wājid", "The Finder, Who Lacks Nothing"),
        AllahName(65, "الْمَاجِدُ", "Al-Mājid", "The Noble"),
        AllahName(66, "الْوَاحِدُ", "Al-Wāḥid", "The One"),
        AllahName(67, "الْأَحَدُ", "Al-Aḥad", "The Unique"),
        AllahName(68, "الصَّمَدُ", "Aṣ-Ṣamad", "The Eternal Refuge"),
        AllahName(69, "الْقَادِرُ", "Al-Qādir", "The All-Able"),
        AllahName(70, "الْمُقْتَدِرُ", "Al-Muqtadir", "The All-Powerful"),
        AllahName(71, "الْمُقَدِّمُ", "Al-Muqaddim", "The One Who Brings Forward"),
        AllahName(72, "الْمُؤَخِّرُ", "Al-Muʾakhkhir", "The One Who Delays"),
        AllahName(73, "الأَوَّلُ", "Al-Awwal", "The First"),
        AllahName(74, "الآخِرُ", "Al-Ākhir", "The Last"),
        AllahName(75, "الظَّاهِرُ", "Aẓ-Ẓāhir", "The Most High Above All"),
        AllahName(76, "الْبَاطِنُ", "Al-Bāṭin", "The Most Near"),
        AllahName(77, "الْوَالِي", "Al-Wālī", "The Governor"),
        AllahName(78, "الْمُتَعَالِي", "Al-Mutaʿālī", "The Most Exalted"),
        AllahName(79, "الْبَرُّ", "Al-Barr", "The Source of Goodness"),
        AllahName(80, "التَّوَّابُ", "At-Tawwāb", "The Accepter of Repentance"),
        AllahName(81, "الْمُنْتَقِمُ", "Al-Muntaqim", "The Avenger"),
        AllahName(82, "الْعَفُوُّ", "Al-ʿAfuww", "The Pardoner"),
        AllahName(83, "الرَّءُوفُ", "Ar-Raʾūf", "The Most Kind"),
        AllahName(84, "مَالِكُ الْمُلْكِ", "Mālik al-Mulk", "The Owner of All Sovereignty"),
        AllahName(85, "ذُو الْجَلاَلِ وَالإِكْرَامِ", "Dhul-Jalāli wal-Ikrām", "The Lord of Majesty and Honour"),
        AllahName(86, "الْمُقْسِطُ", "Al-Muqsiṭ", "The Equitable"),
        AllahName(87, "الْجَامِعُ", "Al-Jāmiʿ", "The Gatherer"),
        AllahName(88, "الْغَنِيُّ", "Al-Ghaniyy", "The Self-Sufficient"),
        AllahName(89, "الْمُغْنِي", "Al-Mughnī", "The Enricher"),
        AllahName(90, "الْمَانِعُ", "Al-Māniʿ", "The Preventer"),
        AllahName(91, "الضَّارُّ", "Aḍ-Ḍārr", "The One Who Allows Harm"),
        AllahName(92, "النَّافِعُ", "An-Nāfiʿ", "The One Who Brings Benefit"),
        AllahName(93, "النُّورُ", "An-Nūr", "The Light"),
        AllahName(94, "الْهَادِي", "Al-Hādī", "The Guide"),
        AllahName(95, "الْبَدِيعُ", "Al-Badīʿ", "The Incomparable Originator"),
        AllahName(96, "الْبَاقِي", "Al-Bāqī", "The Everlasting"),
        AllahName(97, "الْوَارِثُ", "Al-Wārith", "The Inheritor"),
        AllahName(98, "الرَّشِيدُ", "Ar-Rashīd", "The Guide to the Right Path"),
        AllahName(99, "الصَّبُورُ", "Aṣ-Ṣabūr", "The Most Patient"),
    )

    /** A different name each day, in order, so all 99 come round in turn. */
    fun forDate(date: LocalDate): AllahName = all[Math.floorMod(date.toEpochDay(), all.size.toLong()).toInt()]

    /** Names matching [query] by number, transliteration (accents and apostrophes ignored), meaning or Arabic. */
    fun search(query: String): List<AllahName> {
        val raw = query.trim()
        if (raw.isEmpty()) return all
        raw.toIntOrNull()?.let { n -> return all.filter { it.number == n } }
        val q = fold(raw)
        val arabic = stripHarakat(raw)
        return all.filter {
            (q.isNotEmpty() && (q in fold(it.transliteration) || q in fold(it.meaning))) ||
                (q.isEmpty() && arabic in stripHarakat(it.arabic))
        }
    }

    private fun fold(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).lowercase().filter { it in 'a'..'z' || it.isDigit() || it == ' ' }.trim()

    private fun stripHarakat(s: String): String = s.filter { it !in 'ً'..'ٟ' && it != 'ٰ' }
}
