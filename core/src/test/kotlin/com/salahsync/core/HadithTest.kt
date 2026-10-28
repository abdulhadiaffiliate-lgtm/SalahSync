package com.salahsync.core

import com.salahsync.core.hadith.DailyHadith
import com.salahsync.core.hadith.HadithBook
import com.salahsync.core.hadith.HadithEdition
import com.salahsync.core.hadith.HadithNumber
import com.salahsync.core.hadith.HadithPick
import com.salahsync.core.hadith.HadithQuery
import com.salahsync.core.hadith.HadithSections
import com.salahsync.core.hadith.HadithSource
import java.io.File
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HadithTest {
    @Test
    fun muslimIsCitedByAbdulBaqiNumberWithLetters() {
        assertEquals(HadithNumber("2564c", "2564"), HadithNumber.of(HadithBook.MUSLIM, "6543", "2564.03"))
        assertEquals(HadithNumber("864", "864"), HadithNumber.of(HadithBook.MUSLIM, "2001", "864"))
        assertEquals(HadithNumber("intro-5", "intro-5"), HadithNumber.of(HadithBook.MUSLIM, "5", null))
        assertEquals("Introduction 5", HadithNumber.label("intro-5"))
        assertNull(HadithNumber.sunnahUrl(HadithBook.MUSLIM, "intro-5"))
        assertEquals("https://sunnah.com/muslim:2564c", HadithNumber.sunnahUrl(HadithBook.MUSLIM, "2564c"))
    }

    @Test
    fun otherBooksUseTheirOwnNumber() {
        assertEquals(HadithNumber("13", "13"), HadithNumber.of(HadithBook.BUKHARI, "13", "13"))
        assertEquals(HadithNumber("402.2", "402"), HadithNumber.of(HadithBook.BUKHARI, "402.2", "402.2"))
        assertNull(HadithNumber.sunnahUrl(HadithBook.BUKHARI, "402.2"))
        assertEquals(HadithNumber("3604", "3604"), HadithNumber.of(HadithBook.TIRMIDHI, "3604", "3604"))
    }

    @Test
    fun unassignedHadithTakeTheChapterBeforeThem() {
        assertEquals(listOf(0, 0, 1, 1, 1, 2, 2), HadithSections.assign(listOf(0, 0, 1, 0, 1, 2, 0)))
    }

    @Test
    fun queriesAreNumbersOrWords() {
        assertEquals(HadithQuery.Number("13", null), HadithQuery.parse(" 13 "))
        assertEquals(HadithQuery.Number("2564c", HadithBook.MUSLIM), HadithQuery.parse("Muslim 2564c"))
        assertEquals(HadithQuery.Number("23", HadithBook.ABU_DAWUD), HadithQuery.parse("abu dawud 23"))
        assertEquals(HadithQuery.Number("100", HadithBook.TIRMIDHI), HadithQuery.parse("tirmidhi 100"))
        val words = HadithQuery.parse("Muslim brother") as HadithQuery.Words
        assertEquals("muslim* brother*", words.match)
        // Quotes and operators are dropped so the FTS query stays valid.
        assertEquals("patience*", (HadithQuery.parse("\"patience\" OR-") as HadithQuery.Words).match.substringBefore(' '))
        assertEquals("صبر*", (HadithQuery.parse("صبر") as HadithQuery.Words).match)
        assertNull(HadithQuery.parse("  "))
        assertNull(HadithQuery.parse("a"))
    }

    @Test
    fun everyEditionIsPinned() {
        assertEquals(40, HadithSource.COMMIT.length)
        HadithBook.entries.forEach { b ->
            HadithEdition.entries.forEach { e ->
                assertEquals(64, HadithSource.sha256(b, e).length)
                assertTrue(HadithSource.bytes(b, e) > 1_000_000)
                assertTrue(HadithSource.urls(b, e).all { HadithSource.COMMIT in it })
            }
        }
    }

    @Test
    fun twentyDailyHadithFromBukhariAndMuslim() {
        val picks = DailyHadith.picks
        assertEquals(20, picks.size)
        assertEquals(picks.size, picks.map { it.book to it.ref }.toSet().size)
        assertTrue(picks.all { it.book.isSahihCollection })
        assertTrue(picks.all(DailyHadith::isSahih))
        val start = LocalDate.of(2026, 1, 1)
        assertEquals(20, (0L until 20).map { DailyHadith.indexFor(start.plusDays(it)) }.toSet().size)
    }

    @Test
    fun onlySahihHadithCountForTheDay() {
        assertTrue(DailyHadith.isSahih(HadithPick(HadithBook.MUSLIM, "2564c", "")))
        assertFalse(DailyHadith.isSahih(HadithPick(HadithBook.MUSLIM, "intro-5", "")))
        assertFalse(DailyHadith.isSahih(HadithPick(HadithBook.ABU_DAWUD, "23", "")))
        assertFalse(DailyHadith.isSahih(HadithPick(HadithBook.TIRMIDHI, "1987", "")))
    }

    @Test
    fun bundledDailyTextMatchesThePicks() {
        val file = File("../app/src/main/assets/hadith/daily.json")
        if (!file.exists()) return // core-only checkout
        val json = file.readText()
        assertTrue(HadithSource.COMMIT in json)
        val bundled = Regex(""""book": "([a-z]+)",\s*"ref": "([0-9a-z]+)"""").findAll(json)
            .map { HadithBook.bySlug(it.groupValues[1]) to it.groupValues[2] }.toList()
        assertEquals(DailyHadith.picks.map { it.book to it.ref }, bundled)
        assertTrue(""""english": """"" !in json && """"urdu": """"" !in json && """"arabic": """"" !in json)
    }
}
