package com.salahsync.core

import com.salahsync.core.names.AllahNames
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AllahNamesTest {
    @Test
    fun thereAreNinetyNineNumberedInOrder() {
        assertEquals(99, AllahNames.all.size)
        assertEquals((1..99).toList(), AllahNames.all.map { it.number })
        assertEquals(99, AllahNames.all.map { it.arabic }.toSet().size, "an Arabic name is repeated")
        assertEquals(99, AllahNames.all.map { it.transliteration }.toSet().size, "a transliteration is repeated")
    }

    @Test
    fun everyNameIsComplete() {
        AllahNames.all.forEach {
            assertTrue(it.arabic.isNotBlank() && it.transliteration.isNotBlank() && it.meaning.isNotBlank(), "${it.number} is incomplete")
            assertTrue(it.arabic.all { c -> c == ' ' || c in '؀'..'ۿ' }, "${it.number} has non-Arabic letters")
        }
        assertEquals("Ar-Raḥmān", AllahNames.all.first().transliteration)
        assertEquals("Aṣ-Ṣabūr", AllahNames.all.last().transliteration)
        assertEquals("Al-'Alim", AllahNames.all[18].plain)
        assertTrue(AllahNames.all.all { n -> n.plain.all { it.code < 128 } }, "a plain form is not plain")
    }

    @Test
    fun nameOfTheDayGoesRoundAllNinetyNine() {
        val start = LocalDate.of(2026, 1, 1)
        val days = (0L until 99L).map { AllahNames.forDate(start.plusDays(it)) }
        assertEquals(99, days.toSet().size)
        assertEquals(AllahNames.forDate(start), AllahNames.forDate(start.plusDays(99)))
    }

    @Test
    fun searchIgnoresAccentsAndFindsByNumberOrMeaning() {
        assertEquals(listOf(1), AllahNames.search("rahman").map { it.number })
        assertEquals(listOf(19), AllahNames.search("al-alim").map { it.number })
        assertEquals(listOf(47), AllahNames.search("47").map { it.number })
        assertTrue(AllahNames.search("forgiv").map { it.number }.containsAll(listOf(14, 34)))
        assertEquals(listOf(1), AllahNames.search("الرحمن").map { it.number })
        assertEquals(99, AllahNames.search("  ").size)
    }
}
