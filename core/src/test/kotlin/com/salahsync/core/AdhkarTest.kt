package com.salahsync.core

import com.salahsync.core.adhkar.Adhkar
import com.salahsync.core.adhkar.AdhkarSession
import com.salahsync.core.quran.Quran
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdhkarTest {
    private val all = AdhkarSession.entries.flatMap { Adhkar.forSession(it) }

    @Test
    fun everyDhikrIsReferenced() {
        all.forEach { d ->
            assertTrue(d.references.isNotEmpty(), "${d.id} has no reference")
            d.references.forEach { r ->
                assertTrue(r.grade.isNotBlank() && r.narrator.isNotBlank(), "${d.id} reference is incomplete")
                assertTrue(r.url.startsWith("https://sunnah.com/"), "${d.id} reference has no sunnah.com link")
            }
            assertTrue(d.count > 0)
            assertTrue(d.isQuran || d.arabic.isNotBlank(), "${d.id} has no Arabic text")
        }
    }

    @Test
    fun quranPassagesAreValid() {
        all.flatMap { it.quran }.flatMap { it.refs }.forEach { assertTrue(Quran.isValid(it), "$it") }
    }

    @Test
    fun idsAreUniqueWithinASession() {
        AdhkarSession.entries.forEach { s ->
            val ids = Adhkar.forSession(s).map { it.id }
            assertEquals(ids.size, ids.toSet().size)
        }
    }

    @Test
    fun sessionFollowsTheTimeOfDay() {
        assertEquals(AdhkarSession.EVENING, Adhkar.sessionAt(LocalTime.of(2, 59)))
        assertEquals(AdhkarSession.MORNING, Adhkar.sessionAt(LocalTime.of(3, 0)))
        assertEquals(AdhkarSession.MORNING, Adhkar.sessionAt(LocalTime.of(14, 59)))
        assertEquals(AdhkarSession.EVENING, Adhkar.sessionAt(LocalTime.of(15, 0)))
        assertEquals(AdhkarSession.EVENING, Adhkar.sessionAt(LocalTime.of(23, 30)))
    }
}
