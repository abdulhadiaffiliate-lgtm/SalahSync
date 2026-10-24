package com.salahsync.core

import com.salahsync.core.guides.Guides
import com.salahsync.core.guides.Madhhab
import com.salahsync.core.guides.RakatKind
import com.salahsync.core.guides.Rakats
import com.salahsync.core.guides.Ruling
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GuidesTest {
    @Test
    fun everyStepHasARulingForEachSchool() {
        Guides.all.flatMap { it.steps }.forEach { s ->
            assertEquals(Madhhab.entries.toSet(), s.ruling.keys, s.title)
            s.references.forEach { assertTrue(it.url.startsWith("https://sunnah.com/"), s.title) }
        }
    }

    @Test
    fun wuduHasTheFourFaraidOfTheQuran() {
        // Face, arms, head and feet are fard in both schools (Al-Ma'idah 5:6).
        Madhhab.entries.forEach { m ->
            val fard = Guides.wudu.steps.filter { it.ruling[m] == Ruling.FARD }.map { it.title }
            listOf("Wash the face", "Wash the arms", "Wipe the head", "Wash the feet").forEach {
                assertTrue(it in fard, "$m: $it")
            }
        }
        assertEquals(Ruling.FARD, Guides.wudu.steps.first().ruling[Madhhab.SHAFII])
        assertEquals(Ruling.SUNNAH, Guides.wudu.steps.first().ruling[Madhhab.HANAFI])
    }

    @Test
    fun fardRakatsAreTheSameInEverySchool() {
        val expected = mapOf("fajr" to 2, "dhuhr" to 4, "asr" to 4, "maghrib" to 3, "isha" to 4, "jumuah" to 2)
        Madhhab.entries.forEach { m ->
            val prayers = Rakats.forMadhhab(m)
            assertEquals(expected.keys.toList(), prayers.map { it.id }, "$m")
            prayers.forEach { assertEquals(expected[it.id], it.fard, "$m ${it.id}") }
        }
    }

    @Test
    fun witrIsWajibOnlyForHanafis() {
        val witr = { m: Madhhab -> Rakats.forMadhhab(m).single { it.id == "isha" }.sets.single { it.note == "Witr" } }
        assertEquals(RakatKind.WAJIB, witr(Madhhab.HANAFI).kind)
        assertEquals(RakatKind.SUNNAH_MUAKKADAH, witr(Madhhab.SHAFII).kind)
    }
}
