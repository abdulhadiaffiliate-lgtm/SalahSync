package com.salahsync.core.quiz

import com.salahsync.core.adhkar.HadithRef
import com.salahsync.core.quran.AyahRef
import com.salahsync.core.sources.Sunnah.abuDawud
import com.salahsync.core.sources.Sunnah.bukhari
import com.salahsync.core.sources.Sunnah.muslim
import com.salahsync.core.sources.Sunnah.tirmidhi
import kotlin.random.Random

enum class QuizTopic(val displayName: String, val arabicName: String) {
    FAITH("Faith", "الإيمان"),
    PRAYER("Prayer", "الصلاة"),
    QURAN("Quran", "القرآن"),
    PROPHETS("Prophets", "الأنبياء"),
    WORSHIP("Fasting, zakah, hajj", "العبادات"),
}

data class QuizQuestion(
    val id: String,
    val topic: QuizTopic,
    val question: String,
    /** The correct answer comes first; a round shuffles them. */
    val options: List<String>,
    /** Shown after answering: why the answer is right, close to the source's words. */
    val explanation: String,
    val ayahs: List<AyahRef> = emptyList(),
    val hadith: List<HadithRef> = emptyList(),
) {
    val answer: String get() = options.first()
}

/** A question as asked in one round, with its options in shuffled order. */
data class QuizItem(val question: QuizQuestion, val options: List<String>) {
    val answerIndex: Int get() = options.indexOf(question.answer)
}

private fun ayah(surah: Int, ayah: Int) = AyahRef(surah, ayah)

/**
 * Short questions on well-established basics, each answered from the Quran (Saheeh
 * International wording) or from hadith checked against the app's pinned hadith dataset,
 * with sunnah.com numbering: Bukhari and Muslim, or the Sunan with the grading shown.
 */
object Quiz {
    const val ROUND_SIZE = 5

    val all: List<QuizQuestion> = listOf(
        // Faith
        QuizQuestion(
            "pillars_islam", QuizTopic.FAITH,
            "How many pillars of Islam are there?",
            listOf("Five", "Three", "Six", "Seven"),
            "Islam is built on five: bearing witness that there is no god but Allah and that Muhammad is His Messenger, prayer, zakah, hajj and fasting in Ramadan.",
            hadith = listOf(bukhari("8", "'Abdullah ibn 'Umar")),
        ),
        QuizQuestion(
            "pillars_iman", QuizTopic.FAITH,
            "In the hadith of Jibril, how many things make up faith (iman)?",
            listOf("Six", "Five", "Four", "Seven"),
            "Belief in Allah, His angels, His books, His messengers, the Last Day, and the divine decree, its good and its bad.",
            hadith = listOf(muslim("8a", "'Umar ibn al-Khattab")),
        ),
        QuizQuestion(
            "ihsan", QuizTopic.FAITH,
            "In the hadith of Jibril, what is ihsan?",
            listOf("To worship Allah as though you see Him", "To give charity in secret", "To fast outside Ramadan", "To learn the whole Quran by heart"),
            "Ihsan is to worship Allah as though you see Him, for though you do not see Him, He sees you.",
            hadith = listOf(muslim("8a", "'Umar ibn al-Khattab")),
        ),
        QuizQuestion(
            "ikhlas_begets", QuizTopic.FAITH,
            "Which surah says of Allah, \"He neither begets nor is born\"?",
            listOf("Al-Ikhlas", "Al-Fatihah", "An-Nas", "Al-Kawthar"),
            "Surah Al-Ikhlas: \"Say, He is Allah, the One... He neither begets nor is born, nor is there to Him any equivalent.\"",
            ayahs = listOf(ayah(112, 3)),
        ),
        QuizQuestion(
            "jibril_revelation", QuizTopic.FAITH,
            "Which angel brought the Quran down to the Prophet ﷺ?",
            listOf("Jibril", "Mika'il", "Israfil", "Malik"),
            "Allah says Jibril (Gabriel) \"has brought it down upon your heart, by permission of Allah\".",
            ayahs = listOf(ayah(2, 97)),
        ),
        QuizQuestion(
            "zabur", QuizTopic.FAITH,
            "Which book did Allah give to Prophet Dawud?",
            listOf("The Zabur (Psalms)", "The Tawrah (Torah)", "The Injil (Gospel)", "The Quran"),
            "\"And to David We gave the book of Psalms (Zabur).\"",
            ayahs = listOf(ayah(4, 163)),
        ),
        QuizQuestion(
            "shirk", QuizTopic.FAITH,
            "Which sin does the Quran say Allah will not forgive, if a person meets Him upon it?",
            listOf("Associating partners with Allah (shirk)", "Missing a fast", "Breaking a promise", "Telling a lie"),
            "\"Indeed, Allah does not forgive association with Him, but He forgives what is less than that for whom He wills.\" Whoever repents in this life is forgiven, as Az-Zumar 39:53 says.",
            ayahs = listOf(ayah(4, 48), ayah(39, 53)),
        ),
        QuizQuestion(
            "names_99", QuizTopic.FAITH,
            "The Prophet ﷺ said Allah has how many names that whoever learns them will enter Paradise?",
            listOf("Ninety-nine", "Seventy", "Forty", "One hundred and one"),
            "\"Allah has ninety-nine names, one hundred less one, and whoever knows them will enter Paradise.\"",
            hadith = listOf(bukhari("2736", "Abu Hurayrah")),
        ),
        QuizQuestion(
            "taste_death", QuizTopic.FAITH,
            "The Quran says every soul will taste what?",
            listOf("Death", "Hunger", "Wealth", "Sorrow"),
            "\"Every soul will taste death, and you will only be given your full compensation on the Day of Resurrection.\"",
            ayahs = listOf(ayah(3, 185)),
        ),

        // Prayer
        QuizQuestion(
            "five_prayers", QuizTopic.PRAYER,
            "How many obligatory prayers are there in a day and night?",
            listOf("Five", "Three", "Six", "Seven"),
            "The Prophet ﷺ told a man who asked about Islam: five prayers in a day and night, and anything more is voluntary.",
            hadith = listOf(bukhari("46", "Talhah ibn 'Ubaydullah")),
        ),
        QuizQuestion(
            "miraj", QuizTopic.PRAYER,
            "When were the five daily prayers made obligatory?",
            listOf("On the Night Journey (al-Isra' and al-Mi'raj)", "At the Battle of Badr", "On the Farewell Hajj", "At the conquest of Makkah"),
            "On the ascension, fifty prayers were first enjoined, then reduced to five, which Allah said are equal to fifty in reward.",
            hadith = listOf(bukhari("349", "Abu Dharr")),
        ),
        QuizQuestion(
            "qiblah", QuizTopic.PRAYER,
            "Which direction do Muslims face in prayer?",
            listOf("The Ka'bah in Makkah", "Masjid al-Aqsa in Jerusalem", "The Prophet's Mosque in Madinah", "The rising sun"),
            "\"So turn your face toward al-Masjid al-Haram, and wherever you are, turn your faces toward it.\" The Muslims faced Jerusalem before this ayah was revealed.",
            ayahs = listOf(ayah(2, 144)),
        ),
        QuizQuestion(
            "fatihah_every_rakah", QuizTopic.PRAYER,
            "Which surah is recited in every rak'ah of prayer?",
            listOf("Al-Fatihah", "Al-Ikhlas", "Al-Baqarah", "Al-Kawthar"),
            "\"Whoever does not recite Al-Fatihah in his prayer, his prayer is not valid.\"",
            hadith = listOf(bukhari("756", "'Ubadah ibn as-Samit")),
        ),
        QuizQuestion(
            "wudu", QuizTopic.PRAYER,
            "What does the Quran tell believers to do when they rise to pray?",
            listOf("Make wudu (wash for prayer)", "Give charity", "Recite Surah Yasin", "Pray two rak'ahs first"),
            "\"When you rise to perform prayer, wash your faces and your forearms to the elbows and wipe over your heads and wash your feet to the ankles.\"",
            ayahs = listOf(ayah(5, 6)),
        ),
        QuizQuestion(
            "congregation", QuizTopic.PRAYER,
            "Prayer in congregation is how many times better than praying alone?",
            listOf("Twenty-seven times", "Ten times", "Twice", "Seventy times"),
            "\"Prayer in congregation is twenty-seven times better than prayer offered alone.\" Another narration says twenty-five (Bukhari 646).",
            hadith = listOf(bukhari("645", "'Abdullah ibn 'Umar")),
        ),
        QuizQuestion(
            "jumuah", QuizTopic.PRAYER,
            "What is the congregational prayer on Friday called?",
            listOf("Jumu'ah", "Duha", "Tahajjud", "Witr"),
            "\"When the adhan is called for the prayer on the day of Jumu'ah, then proceed to the remembrance of Allah and leave trade.\"",
            ayahs = listOf(ayah(62, 9)),
        ),
        QuizQuestion(
            "takbir_taslim", QuizTopic.PRAYER,
            "How does the prayer begin and end?",
            listOf("With takbir, and with taslim", "With Al-Fatihah, and with a du'a", "With the adhan, and with the iqamah", "With sujud, and with ruku'"),
            "\"The key to prayer is purification, its beginning is the takbir (Allahu akbar) and its end is the taslim (as-salamu 'alaykum).\"",
            hadith = listOf(
                abuDawud("61", "'Ali ibn Abi Talib", "Hasan Sahih (al-Albani)"),
                tirmidhi("3", "'Ali ibn Abi Talib", "Hasan Sahih (al-Albani)"),
            ),
        ),
        QuizQuestion(
            "first_account", QuizTopic.PRAYER,
            "What is the first deed a person will be called to account for on the Day of Resurrection?",
            listOf("Prayer", "Fasting", "Charity", "Hajj"),
            "\"The first of his deeds for which a servant will be called to account on the Day of Resurrection is his prayer.\"",
            hadith = listOf(
                tirmidhi("413", "Abu Hurayrah", "Sahih (al-Albani)"),
                abuDawud("864", "Abu Hurayrah", "Sahih (al-Albani)"),
            ),
        ),
        QuizQuestion(
            "night_prayer", QuizTopic.PRAYER,
            "Which prayer is the best after the obligatory prayers?",
            listOf("Prayer at night", "Prayer at Duha (mid-morning)", "Prayer on entering the mosque", "Prayer after wudu"),
            "\"The best prayer after the obligatory prayer is the prayer at night.\"",
            hadith = listOf(muslim("1163a", "Abu Hurayrah")),
        ),

        // Quran
        QuizQuestion(
            "surah_count", QuizTopic.QURAN,
            "How many surahs are in the Quran?",
            listOf("114", "99", "120", "110"),
            "The mushaf opens with Al-Fatihah and closes with An-Nas, the 114th surah.",
            ayahs = listOf(ayah(1, 1), ayah(114, 1)),
        ),
        QuizQuestion(
            "first_revealed", QuizTopic.QURAN,
            "What was the first word of the Quran to be revealed?",
            listOf("Iqra' (Read, or Recite)", "Bismillah", "Alhamdulillah", "Qul (Say)"),
            "In the cave of Hira the angel said, \"Read in the name of your Lord who created\", the start of Surah Al-'Alaq.",
            ayahs = listOf(ayah(96, 1)),
            hadith = listOf(bukhari("3", "'A'ishah")),
        ),
        QuizQuestion(
            "ramadan_revealed", QuizTopic.QURAN,
            "In which month was the Quran sent down?",
            listOf("Ramadan", "Muharram", "Sha'ban", "Dhul-Hijjah"),
            "\"The month of Ramadan is that in which was revealed the Quran, a guidance for the people.\"",
            ayahs = listOf(ayah(2, 185)),
        ),
        QuizQuestion(
            "laylat_qadr", QuizTopic.QURAN,
            "On which night was the Quran sent down?",
            listOf("Laylat al-Qadr (the Night of Decree)", "The night of 'Arafah", "The first night of Muharram", "The night of Eid"),
            "\"Indeed, We sent it down during the Night of Decree.\"",
            ayahs = listOf(ayah(97, 1)),
        ),
        QuizQuestion(
            "greatest_ayah", QuizTopic.QURAN,
            "Which ayah did the Prophet ﷺ call the greatest in the Book of Allah?",
            listOf("Ayat al-Kursi", "The last ayah of Al-Baqarah", "The first ayah of Al-Fatihah", "The first ayah of Al-Ikhlas"),
            "He asked Ubayy ibn Ka'b which ayah is the greatest; Ubayy said Ayat al-Kursi (Al-Baqarah 2:255), and the Prophet ﷺ praised his knowledge.",
            ayahs = listOf(ayah(2, 255)),
            hadith = listOf(muslim("810", "Ubayy ibn Ka'b")),
        ),
        QuizQuestion(
            "greatest_surah", QuizTopic.QURAN,
            "Which surah did the Prophet ﷺ call the greatest surah in the Quran?",
            listOf("Al-Fatihah", "Al-Baqarah", "Yasin", "Al-Mulk"),
            "\"Al-hamdu lillahi rabbil-'alamin: it is the seven oft-repeated ayahs and the great Quran given to me.\"",
            ayahs = listOf(ayah(1, 2)),
            hadith = listOf(bukhari("4474", "Abu Sa'id ibn al-Mu'alla")),
        ),
        QuizQuestion(
            "third_quran", QuizTopic.QURAN,
            "Which surah did the Prophet ﷺ say is equal to a third of the Quran?",
            listOf("Al-Ikhlas", "Al-Fatihah", "Al-Kawthar", "Al-'Asr"),
            "Of \"Say, He is Allah, the One\" he said: \"By Him in whose hand my life is, it is equal to a third of the Quran.\"",
            ayahs = listOf(ayah(112, 1)),
            hadith = listOf(bukhari("5013", "Abu Sa'id al-Khudri")),
        ),
        QuizQuestion(
            "longest_surah", QuizTopic.QURAN,
            "Which is the longest surah of the Quran?",
            listOf("Al-Baqarah", "Al 'Imran", "An-Nisa'", "Al-Kahf"),
            "Al-Baqarah has 286 ayahs, more than any other surah.",
            ayahs = listOf(ayah(2, 286)),
        ),
        QuizQuestion(
            "arabic", QuizTopic.QURAN,
            "In which language was the Quran revealed?",
            listOf("Arabic", "Hebrew", "Aramaic", "Persian"),
            "\"Indeed, We have sent it down as an Arabic Quran that you might understand.\"",
            ayahs = listOf(ayah(12, 2)),
        ),
        QuizQuestion(
            "day_of_recompense", QuizTopic.QURAN,
            "In Al-Fatihah, Allah is called Sovereign of which day?",
            listOf("The Day of Recompense", "The day of Jumu'ah", "The day of 'Arafah", "The day of Eid"),
            "\"Sovereign of the Day of Recompense\": the Day of Judgement, when every deed is repaid.",
            ayahs = listOf(ayah(1, 4)),
        ),

        // Prophets
        QuizQuestion(
            "seal_prophets", QuizTopic.PROPHETS,
            "Who is the last of the prophets?",
            listOf("Muhammad ﷺ", "'Isa", "Musa", "Ibrahim"),
            "\"Muhammad is not the father of any of your men, but he is the Messenger of Allah and the seal of the prophets.\"",
            ayahs = listOf(ayah(33, 40)),
        ),
        QuizQuestion(
            "adam", QuizTopic.PROPHETS,
            "Who was the first human being Allah created?",
            listOf("Adam", "Nuh", "Ibrahim", "Idris"),
            "Allah created Adam from dust (Al 'Imran 3:59), and all people come from that one soul (An-Nisa' 4:1).",
            ayahs = listOf(ayah(3, 59), ayah(4, 1)),
        ),
        QuizQuestion(
            "nuh_ship", QuizTopic.PROPHETS,
            "Which prophet built the Ark by Allah's command?",
            listOf("Nuh", "Ibrahim", "Musa", "Yunus"),
            "Allah told Nuh: \"Construct the ship under Our observation and Our inspiration.\"",
            ayahs = listOf(ayah(11, 37)),
        ),
        QuizQuestion(
            "kabah_foundations", QuizTopic.PROPHETS,
            "Which prophet raised the foundations of the Ka'bah with his son Isma'il?",
            listOf("Ibrahim", "Nuh", "Dawud", "Sulayman"),
            "\"When Abraham was raising the foundations of the House and with him Ishmael, saying, Our Lord, accept this from us.\"",
            ayahs = listOf(ayah(2, 127)),
        ),
        QuizQuestion(
            "yunus_fish", QuizTopic.PROPHETS,
            "Which prophet was swallowed by a fish?",
            listOf("Yunus", "Yusuf", "Ayyub", "Ilyas"),
            "\"And indeed, Jonah was among the messengers... Then the fish swallowed him.\"",
            ayahs = listOf(ayah(37, 139), ayah(37, 142)),
        ),
        QuizQuestion(
            "ibrahim_fire", QuizTopic.PROPHETS,
            "Which prophet was thrown into a fire that Allah made cool and safe?",
            listOf("Ibrahim", "Musa", "Yahya", "Hud"),
            "\"We said, O fire, be coolness and safety upon Abraham.\"",
            ayahs = listOf(ayah(21, 69)),
        ),
        QuizQuestion(
            "yusuf_well", QuizTopic.PROPHETS,
            "Which prophet was put into a well by his brothers?",
            listOf("Yusuf", "Yunus", "Isma'il", "Harun"),
            "Yusuf's brothers \"agreed to put him into the bottom of the well\", and Allah inspired him that he would one day tell them of it.",
            ayahs = listOf(ayah(12, 15)),
        ),
        QuizQuestion(
            "isa_cradle", QuizTopic.PROPHETS,
            "Which prophet spoke as a baby in the cradle?",
            listOf("'Isa", "Yahya", "Musa", "Sulayman"),
            "When Maryam pointed to her baby, 'Isa said: \"Indeed, I am the servant of Allah. He has given me the Scripture and made me a prophet.\"",
            ayahs = listOf(ayah(19, 29), ayah(19, 30)),
        ),
        QuizQuestion(
            "musa_staff", QuizTopic.PROPHETS,
            "Whose staff became a snake when he threw it down, by Allah's command?",
            listOf("Musa", "Harun", "Salih", "Shu'ayb"),
            "Allah told Musa to throw down his staff, \"and thereupon it was a snake, moving swiftly.\"",
            ayahs = listOf(ayah(20, 20)),
        ),
        QuizQuestion(
            "sulayman_birds", QuizTopic.PROPHETS,
            "Which prophet said, \"We have been taught the language of birds\"?",
            listOf("Sulayman", "Dawud", "Ibrahim", "Lut"),
            "\"And Solomon inherited David. He said, O people, we have been taught the language of birds.\"",
            ayahs = listOf(ayah(27, 16)),
        ),
        QuizQuestion(
            "ayyub_patience", QuizTopic.PROPHETS,
            "Which prophet is praised in the Quran for his patience through hardship?",
            listOf("Ayyub", "Yunus", "Nuh", "Ishaq"),
            "Ayyub called to his Lord, \"Adversity has touched me\", and Allah said, \"We found him patient, an excellent servant.\"",
            ayahs = listOf(ayah(21, 83), ayah(38, 44)),
        ),
        QuizQuestion(
            "khadijah", QuizTopic.PROPHETS,
            "To whom did the Prophet ﷺ go back after the first revelation in the cave of Hira?",
            listOf("Khadijah", "Abu Bakr", "'A'ishah", "Abu Talib"),
            "He returned with his heart beating severely to his wife Khadijah, who comforted him.",
            hadith = listOf(bukhari("3", "'A'ishah")),
        ),

        // Fasting, zakah and hajj
        QuizQuestion(
            "fasting_month", QuizTopic.WORSHIP,
            "In which month is fasting obligatory?",
            listOf("Ramadan", "Sha'ban", "Muharram", "Rajab"),
            "\"The month of Ramadan... whoever sights the month, let him fast it.\"",
            ayahs = listOf(ayah(2, 185)),
        ),
        QuizQuestion(
            "fasting_taqwa", QuizTopic.WORSHIP,
            "According to Al-Baqarah 2:183, fasting was prescribed so that you may become what?",
            listOf("Righteous, mindful of Allah (taqwa)", "Wealthy", "Healthy", "Well known"),
            "\"Decreed upon you is fasting as it was decreed upon those before you, that you may become righteous.\"",
            ayahs = listOf(ayah(2, 183)),
        ),
        QuizQuestion(
            "hajj_able", QuizTopic.WORSHIP,
            "Upon whom is hajj obligatory?",
            listOf("Whoever is able to make the journey", "Every Muslim, every year", "Only those who live in Makkah", "Only scholars"),
            "\"Due to Allah from the people is a pilgrimage to the House, for whoever is able to find thereto a way.\"",
            ayahs = listOf(ayah(3, 97)),
        ),
        QuizQuestion(
            "zakah_groups", QuizTopic.WORSHIP,
            "How many groups of people does the Quran list as receivers of zakah?",
            listOf("Eight", "Four", "Five", "Ten"),
            "The poor, the needy, those who collect it, those whose hearts are brought together, freeing captives, those in debt, the cause of Allah, and the stranded traveller.",
            ayahs = listOf(ayah(9, 60)),
        ),
        QuizQuestion(
            "thousand_months", QuizTopic.WORSHIP,
            "Which night does the Quran call better than a thousand months?",
            listOf("Laylat al-Qadr", "The night of 'Arafah", "The night before Jumu'ah", "The night of Eid"),
            "\"The Night of Decree is better than a thousand months.\"",
            ayahs = listOf(ayah(97, 3)),
        ),
        QuizQuestion(
            "iftar_dates", QuizTopic.WORSHIP,
            "What did the Prophet ﷺ break his fast with?",
            listOf("Fresh dates, or dry dates, or else water", "Milk and honey", "Bread and olive oil", "Only Zamzam water"),
            "He broke his fast before praying with fresh dates; if there were none, with dry dates; and if there were none, with a few sips of water.",
            hadith = listOf(abuDawud("2356", "Anas ibn Malik", "Hasan Sahih (al-Albani)")),
        ),
        QuizQuestion(
            "hasten_iftar", QuizTopic.WORSHIP,
            "The Prophet ﷺ said people will remain upon good as long as they do what?",
            listOf("Hasten to break the fast", "Delay breaking the fast", "Fast every day", "Skip the suhur meal"),
            "\"The people will remain upon good as long as they hasten to break the fast.\"",
            hadith = listOf(bukhari("1957", "Sahl ibn Sa'd")),
        ),
        QuizQuestion(
            "charity_wealth", QuizTopic.WORSHIP,
            "What did the Prophet ﷺ say charity does to wealth?",
            listOf("It does not decrease it", "It halves it", "It makes it unlawful", "It counts only in Ramadan"),
            "\"Charity does not decrease wealth, and Allah increases in honour the one who forgives, and raises the one who humbles himself for Allah.\"",
            hadith = listOf(muslim("2588", "Abu Hurayrah")),
        ),
    )

    fun inTopic(topic: QuizTopic?): List<QuizQuestion> = if (topic == null) all else all.filter { it.topic == topic }

    /** [ROUND_SIZE] different questions from [topic] (all topics when null), options shuffled. */
    fun round(topic: QuizTopic?, random: Random): List<QuizItem> =
        inTopic(topic).shuffled(random).take(ROUND_SIZE).map { QuizItem(it, it.options.shuffled(random)) }
}
