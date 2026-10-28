package com.salahsync.core

import com.salahsync.core.duas.Duas
import com.salahsync.core.feelings.Feelings
import com.salahsync.core.hadith.HadithSource
import com.salahsync.core.quran.Quran
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FeelingsTest {
    @Test
    fun everyFeelingHasAyatDuasAndASahihHadith() {
        assertTrue(Feelings.all.size >= 8)
        assertEquals(Feelings.all.size, Feelings.all.map { it.id }.toSet().size)
        Feelings.all.forEach { f ->
            assertTrue(f.name.isNotBlank() && f.comfort.isNotBlank(), "${f.id} is incomplete")
            assertTrue(f.ayat.isNotEmpty(), "${f.id} has no ayah")
            f.ayat.flatMap { it.refs }.forEach { assertTrue(Quran.isValid(it), "${f.id}: $it") }
            assertTrue(f.duaIds.isNotEmpty(), "${f.id} has no dua")
            f.duaIds.forEach { id -> assertTrue(Duas.all.any { it.id == id }, "${f.id}: no dua $id") }
            assertTrue(f.hadith.book.isSahihCollection, "${f.id}: hadith is not from Bukhari or Muslim")
            assertEquals(f.duaIds.size, f.duas.size)
        }
    }

    @Test
    fun byIdFindsEachFeeling() {
        Feelings.all.forEach { assertEquals(it, Feelings.byId(it.id)) }
        assertEquals(null, Feelings.byId("missing"))
        assertEquals(null, Feelings.byId(null))
    }

    @Test
    fun bundledFeelingsTextMatchesThePicks() {
        val file = File("../app/src/main/assets/hadith/feelings.json")
        if (!file.exists()) return // core-only checkout
        val json = file.readText()
        assertTrue(HadithSource.COMMIT in json)
        val bundled = Regex(""""book": "([a-z]+)",\s*"ref": "([0-9a-z]+)"""").findAll(json)
            .map { it.groupValues[1] to it.groupValues[2] }.toList()
        assertEquals(Feelings.hadithPicks.map { it.book.slug to it.ref }, bundled)
        assertTrue(""""english": """"" !in json && """"urdu": """"" !in json && """"arabic": """"" !in json)
    }
}
