package com.salahsync.data

import java.time.LocalDate

data class Ayah(val arabic: String, val english: String, val reference: String)

/**
 * Rotating daily content for the Home screen. This is a starter set: the curated
 * 20 ayat (with Urdu) replace it once the content list is final.
 */
object DailyContent {

    val ayat = listOf(
        Ayah("لَا يُكَلِّفُ ٱللَّهُ نَفْسًا إِلَّا وُسْعَهَا", "Allah does not burden a soul beyond what it can bear.", "Al-Baqarah 2:286"),
        Ayah("فَإِنَّ مَعَ ٱلْعُسْرِ يُسْرًا", "So surely with hardship comes ease.", "Ash-Sharh 94:5"),
        Ayah("أَلَا بِذِكْرِ ٱللَّهِ تَطْمَئِنُّ ٱلْقُلُوبُ", "Surely in the remembrance of Allah do hearts find rest.", "Ar-Ra'd 13:28"),
        Ayah("فَٱذْكُرُونِىٓ أَذْكُرْكُمْ", "So remember Me; I will remember you.", "Al-Baqarah 2:152"),
        Ayah("وَمَن يَتَوَكَّلْ عَلَى ٱللَّهِ فَهُوَ حَسْبُهُۥٓ", "Whoever puts their trust in Allah, He is enough for them.", "At-Talaq 65:3"),
    )

    val goodDeeds = listOf(
        "Recite Surah Al-Kahf on Friday",
        "Call a relative you have not spoken to in a while",
        "Give something in charity, however small",
        "Pray two rak'ahs of Duha in the morning",
        "Smile at someone and greet them with salaam",
        "Say SubhanAllah, Alhamdulillah and Allahu Akbar 33 times after each salah",
        "Feed someone, or share a meal",
        "Make dua for a friend without telling them",
        "Read one page of the Quran with its meaning",
        "Forgive someone who wronged you",
    )

    private fun index(date: LocalDate, size: Int) = Math.floorMod(date.toEpochDay(), size.toLong()).toInt()

    fun ayahFor(date: LocalDate): Ayah = ayat[index(date, ayat.size)]

    fun goodDeedFor(date: LocalDate): String =
        if (date.dayOfWeek == java.time.DayOfWeek.FRIDAY) goodDeeds[0]
        else goodDeeds[1 + index(date, goodDeeds.size - 1)]
}
