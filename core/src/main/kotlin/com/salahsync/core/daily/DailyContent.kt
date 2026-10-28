package com.salahsync.core.daily

import com.salahsync.core.quran.AyahRef
import java.time.DayOfWeek
import java.time.LocalDate

data class Ayah(val ref: AyahRef, val arabic: String, val english: String, val reference: String)

/** A small act of good, with the ayah or hadith that encourages it. */
data class GoodDeed(val text: String, val source: String)

/** What the optional daily notification shows. */
enum class DailyNoteKind(val displayName: String) {
    AYAH("Ayah of the day"),
    GOOD_DEED("Today's good deed"),
    ALTERNATE("Both, on alternate days");

    /** Which of the two to show on [date]; alternating keeps the same pick for the whole day. */
    fun showsAyahOn(date: LocalDate): Boolean = when (this) {
        AYAH -> true
        GOOD_DEED -> false
        ALTERNATE -> Math.floorMod(date.toEpochDay(), 2L) == 0L
    }
}

/**
 * Rotating daily content for the Home screen and the daily notification. This is a
 * starter set: the curated 20 ayat (with Urdu) replace it once the content list is final.
 */
object DailyContent {

    val ayat = listOf(
        Ayah(AyahRef(2, 286), "لَا يُكَلِّفُ ٱللَّهُ نَفْسًا إِلَّا وُسْعَهَا", "Allah does not burden a soul beyond what it can bear.", "Al-Baqarah 2:286"),
        Ayah(AyahRef(94, 5), "فَإِنَّ مَعَ ٱلْعُسْرِ يُسْرًا", "So surely with hardship comes ease.", "Ash-Sharh 94:5"),
        Ayah(AyahRef(13, 28), "أَلَا بِذِكْرِ ٱللَّهِ تَطْمَئِنُّ ٱلْقُلُوبُ", "Surely in the remembrance of Allah do hearts find rest.", "Ar-Ra'd 13:28"),
        Ayah(AyahRef(2, 152), "فَٱذْكُرُونِىٓ أَذْكُرْكُمْ", "So remember Me; I will remember you.", "Al-Baqarah 2:152"),
        Ayah(AyahRef(65, 3), "وَمَن يَتَوَكَّلْ عَلَى ٱللَّهِ فَهُوَ حَسْبُهُۥٓ", "Whoever puts their trust in Allah, He is enough for them.", "At-Talaq 65:3"),
    )

    /** The first is kept for Fridays; the rest rotate through the week. */
    val goodDeeds = listOf(
        GoodDeed("Recite Surah Al-Kahf on Friday", "Al-Hakim, graded sahih by al-Albani (Sahih al-Jami' 6470)"),
        GoodDeed("Call a relative you have not spoken to in a while", "Sahih al-Bukhari 5986"),
        GoodDeed("Give something in charity, however small", "Sahih al-Bukhari 1417"),
        GoodDeed("Pray two rak'ahs of Duha in the morning", "Sahih Muslim 720"),
        GoodDeed("Smile at someone and greet them with salaam", "Jami' at-Tirmidhi 1956"),
        GoodDeed("Say SubhanAllah, Alhamdulillah and Allahu Akbar 33 times after each salah", "Sahih Muslim 597"),
        GoodDeed("Feed someone, or share a meal", "Sahih al-Bukhari 12"),
        GoodDeed("Make dua for a friend without telling them", "Sahih Muslim 2733"),
        GoodDeed("Read one page of the Quran with its meaning", "Quran, Sad 38:29"),
        GoodDeed("Forgive someone who wronged you", "Quran, An-Nur 24:22"),
    )

    private fun index(date: LocalDate, size: Int) = Math.floorMod(date.toEpochDay(), size.toLong()).toInt()

    fun ayahFor(date: LocalDate): Ayah = ayat[index(date, ayat.size)]

    fun goodDeedFor(date: LocalDate): GoodDeed =
        if (date.dayOfWeek == DayOfWeek.FRIDAY) goodDeeds[0]
        else goodDeeds[1 + index(date, goodDeeds.size - 1)]
}
