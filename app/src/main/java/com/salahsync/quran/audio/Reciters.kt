package com.salahsync.quran.audio

import java.util.Locale

/** A recitation published as one MP3 per ayah on EveryAyah.com. */
data class Reciter(
    val id: String,
    val name: String,
    val arabicName: String,
    val style: String,
    /** The EveryAyah folder, such as "Alafasy_64kbps". */
    val folder: String,
) {
    fun url(surah: Int, ayah: Int): String = "https://everyayah.com/data/$folder/${fileName(surah, ayah)}"

    companion object {
        /** EveryAyah's naming: "002255.mp3" for 2:255. */
        fun fileName(surah: Int, ayah: Int): String = String.format(Locale.US, "%03d%03d.mp3", surah, ayah)
    }
}

object Reciters {
    val all = listOf(
        Reciter("alafasy", "Mishary Rashid Alafasy", "مشاري راشد العفاسي", "Murattal", "Alafasy_64kbps"),
        Reciter("abdulbasit", "Abdul Basit Abdus Samad", "عبد الباسط عبد الصمد", "Murattal", "Abdul_Basit_Murattal_64kbps"),
        Reciter("sudais", "Abdur-Rahman as-Sudais", "عبد الرحمن السديس", "Murattal", "Abdurrahmaan_As-Sudais_64kbps"),
        Reciter("shuraym", "Saud ash-Shuraym", "سعود الشريم", "Murattal", "Saood_ash-Shuraym_64kbps"),
        Reciter("maher", "Maher al-Muaiqly", "ماهر المعيقلي", "Murattal", "Maher_AlMuaiqly_64kbps"),
        Reciter("husary", "Mahmoud Khalil al-Husary", "محمود خليل الحصري", "Murattal", "Husary_64kbps"),
        Reciter("minshawi", "Mohamed Siddiq al-Minshawi", "محمد صديق المنشاوي", "Murattal", "Minshawy_Murattal_128kbps"),
        Reciter("ghamdi", "Saad al-Ghamdi", "سعد الغامدي", "Murattal", "Ghamadi_40kbps"),
    )

    /** The reciter whose short surahs ship inside the app. */
    val default: Reciter = all.first()

    fun byId(id: String?): Reciter? = all.firstOrNull { it.id == id }

    fun orDefault(id: String?): Reciter = byId(id) ?: default
}
