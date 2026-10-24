package com.salahsync.core

import com.salahsync.core.duas.DuaCategory
import com.salahsync.core.duas.Duas
import com.salahsync.core.quran.Quran
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DuasTest {
    @Test
    fun everyDuaIsReferenced() {
        Duas.all.forEach { d ->
            assertTrue(d.arabic.isNotBlank() && d.meaning.isNotBlank() && d.occasion.isNotBlank(), "${d.id} is incomplete")
            assertTrue(d.references.isNotEmpty(), "${d.id} has no reference")
            d.references.forEach { r ->
                assertTrue(r.grade.isNotBlank() && r.narrator.isNotBlank(), "${d.id} reference is incomplete")
                assertTrue(r.url.startsWith("https://sunnah.com/"), "${d.id} reference has no sunnah.com link")
            }
            d.ayah?.let { assertTrue(Quran.isValid(it), "${d.id}: $it") }
        }
    }

    @Test
    fun idsAreUnique() {
        assertEquals(Duas.all.size, Duas.all.map { it.id }.toSet().size)
    }

    @Test
    fun everyCategoryHasDuas() {
        DuaCategory.entries.forEach { assertTrue(Duas.inCategory(it).isNotEmpty(), "$it is empty") }
    }
}
