package com.salahsync.core.guides

import com.salahsync.core.adhkar.HadithRef
import com.salahsync.core.sources.Sunnah.abuDawud
import com.salahsync.core.sources.Sunnah.bukhari
import com.salahsync.core.sources.Sunnah.muslim
import com.salahsync.core.sources.Sunnah.tirmidhi

enum class RakatKind(val displayName: String) {
    FARD("Fard"),
    WAJIB("Wajib"),

    /** Sunnah mu'akkadah: the Prophet ﷺ kept to it regularly. */
    SUNNAH_MUAKKADAH("Sunnah mu'akkadah"),

    /** Sunnah ghayr mu'akkadah: done at times, rewarded but not stressed. */
    SUNNAH("Sunnah ghayr mu'akkadah"),
}

enum class RakatTiming { BEFORE, MAIN, AFTER }

data class RakatSet(val rakats: Int, val kind: RakatKind, val timing: RakatTiming, val note: String? = null)

data class PrayerRakats(
    val id: String,
    val name: String,
    val arabicName: String,
    val sets: List<RakatSet>,
    val note: String? = null,
) {
    val fard: Int get() = sets.filter { it.kind == RakatKind.FARD }.sumOf { it.rakats }
    val total: Int get() = sets.sumOf { it.rakats }
}

/**
 * Rak'ahs of each daily prayer and Jumu'ah, by school. The counts follow the standard manual of
 * each school ([Madhhab.manual]); [references] are the hadith those counts rest on.
 */
object Rakats {
    private fun before(n: Int, k: RakatKind, note: String? = null) = RakatSet(n, k, RakatTiming.BEFORE, note)
    private fun fard(n: Int) = RakatSet(n, RakatKind.FARD, RakatTiming.MAIN)
    private fun after(n: Int, k: RakatKind, note: String? = null) = RakatSet(n, k, RakatTiming.AFTER, note)

    private val M = RakatKind.SUNNAH_MUAKKADAH
    private val G = RakatKind.SUNNAH

    private val hanafi = listOf(
        PrayerRakats("fajr", "Fajr", "الفجر", listOf(before(2, M), fard(2)),
            note = "The two before Fajr are the most stressed of all the sunnah prayers."),
        PrayerRakats("dhuhr", "Dhuhr", "الظهر", listOf(before(4, M), fard(4), after(2, M))),
        PrayerRakats("asr", "Asr", "العصر", listOf(before(4, G), fard(4))),
        PrayerRakats("maghrib", "Maghrib", "المغرب", listOf(fard(3), after(2, M))),
        PrayerRakats(
            "isha", "Isha", "العشاء",
            listOf(before(4, G), fard(4), after(2, M), after(3, RakatKind.WAJIB, "Witr")),
            note = "Witr is wajib in the Hanafi school: three rak'ahs with one salam, and Du'a al-Qunut before the ruku' of the third.",
        ),
        PrayerRakats(
            "jumuah", "Jumu'ah", "الجمعة",
            listOf(before(4, M), fard(2), after(4, M)),
            note = "Abu Yusuf held six after Jumu'ah, four then two, and many Hanafis pray that.",
        ),
    )

    private val shafii = listOf(
        PrayerRakats("fajr", "Fajr", "الفجر", listOf(before(2, M), fard(2)),
            note = "The two before Fajr are among the most stressed of the sunnah prayers."),
        PrayerRakats("dhuhr", "Dhuhr", "الظهر", listOf(before(2, M), before(2, G), fard(4), after(2, M), after(2, G))),
        PrayerRakats("asr", "Asr", "العصر", listOf(before(4, G), fard(4))),
        PrayerRakats("maghrib", "Maghrib", "المغرب", listOf(before(2, G, "Short, between the adhan and iqamah"), fard(3), after(2, M))),
        PrayerRakats(
            "isha", "Isha", "العشاء",
            listOf(before(2, G), fard(4), after(2, M), after(3, M, "Witr")),
            note = "Witr is sunnah mu'akkadah in the Shafi'i school: at least one rak'ah, three is the common minimum of excellence, and up to eleven.",
        ),
        PrayerRakats(
            "jumuah", "Jumu'ah", "الجمعة",
            listOf(before(2, M), before(2, G), fard(2), after(2, M), after(2, G)),
            note = "The sunnahs of Jumu'ah are as those of Dhuhr.",
        ),
    )

    fun forMadhhab(m: Madhhab): List<PrayerRakats> = when (m) {
        Madhhab.HANAFI -> hanafi
        Madhhab.SHAFII -> shafii
    }

    val references: List<Pair<String, HadithRef>> = listOf(
        "Twelve rak'ahs a day: four before Dhuhr, two after, two after Maghrib, two after Isha and two before Fajr, for which a house is built in Paradise" to
            tirmidhi("415", "Umm Habibah", "Sahih (Darussalam); at-Tirmidhi called it hasan sahih"),
        "Ten rak'ahs Ibn 'Umar memorised: two before Dhuhr and two after, two after Maghrib, two after Isha, and two before Fajr" to
            bukhari("1180", "'Abdullah ibn 'Umar"),
        "\"May Allah have mercy on a man who prays four before Asr\"" to
            abuDawud("1271", "'Abdullah ibn 'Umar", "Hasan (al-Albani)"),
        "\"Pray before Maghrib\", said three times, adding \"for whoever wishes\"" to
            bukhari("1183", "'Abdullah ibn Mughaffal al-Muzani"),
        "\"When one of you prays Jumu'ah, let him pray four after it\"" to
            muslim("881a", "Abu Hurayrah"),
    )
}
