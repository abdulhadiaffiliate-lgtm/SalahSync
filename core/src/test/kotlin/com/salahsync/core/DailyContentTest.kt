package com.salahsync.core

import com.salahsync.core.daily.DailyContent
import com.salahsync.core.daily.DailyNoteKind
import com.salahsync.core.quran.Quran
import com.salahsync.core.quran.QuranText
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DailyContentTest {
    private val friday = LocalDate.of(2026, 10, 30)

    @Test
    fun everyAyahPointsAtARealVerseAndNamesIt() {
        for (a in DailyContent.ayat) {
            assertTrue(a.ref.ayah in 1..Quran.surah(a.ref.surah).ayahCount, "${a.ref}")
            assertTrue(a.reference.endsWith(" ${a.ref}"), a.reference)
        }
    }

    @Test
    fun everyGoodDeedHasASource() {
        assertTrue(DailyContent.goodDeeds.all { it.text.isNotBlank() && it.source.isNotBlank() })
    }

    @Test
    fun fridayIsAlwaysAlKahfAndOtherDaysNeverAre() {
        assertEquals(DayOfWeek.FRIDAY, friday.dayOfWeek)
        assertEquals(DailyContent.goodDeeds[0], DailyContent.goodDeedFor(friday))
        for (d in 1L..6L) assertNotEquals(DailyContent.goodDeeds[0], DailyContent.goodDeedFor(friday.plusDays(d)))
    }

    @Test
    fun alternateSwitchesEachDay() {
        val kind = DailyNoteKind.ALTERNATE
        assertNotEquals(kind.showsAyahOn(friday), kind.showsAyahOn(friday.plusDays(1)))
        assertEquals(kind.showsAyahOn(friday), kind.showsAyahOn(friday.plusDays(2)))
        assertTrue(DailyNoteKind.AYAH.showsAyahOn(friday))
        assertTrue(!DailyNoteKind.GOOD_DEED.showsAyahOn(friday))
    }

    @Test
    fun plainTranslationDropsNumberAndFootnotes() {
        assertEquals(
            "In the name of Allāh, the Entirely Merciful, the Especially Merciful.",
            QuranText.plainTranslation("(1) In the name of Allāh,[2] the Entirely Merciful, the Especially Merciful.[3]"),
        )
    }
}
