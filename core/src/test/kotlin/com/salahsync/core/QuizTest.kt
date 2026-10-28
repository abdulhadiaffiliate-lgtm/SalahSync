package com.salahsync.core

import com.salahsync.core.quiz.Quiz
import com.salahsync.core.quiz.QuizTopic
import com.salahsync.core.quran.AyahRef
import com.salahsync.core.quran.Quran
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuizTest {
    @Test
    fun everyQuestionIsCompleteAndSourced() {
        Quiz.all.forEach { q ->
            assertTrue(q.question.isNotBlank() && q.explanation.isNotBlank(), "${q.id} is incomplete")
            assertEquals(4, q.options.size, "${q.id} needs four options")
            assertEquals(q.options.size, q.options.toSet().size, "${q.id} repeats an option")
            assertTrue(q.options.none { it.isBlank() }, "${q.id} has a blank option")
            assertTrue(q.ayahs.isNotEmpty() || q.hadith.isNotEmpty(), "${q.id} has no source")
            q.ayahs.forEach { assertTrue(Quran.isValid(it), "${q.id}: $it") }
            q.hadith.forEach { r ->
                assertTrue(r.grade.isNotBlank() && r.narrator.isNotBlank(), "${q.id} reference is incomplete")
                assertTrue(r.url.startsWith("https://sunnah.com/"), "${q.id} reference has no sunnah.com link")
            }
        }
    }

    @Test
    fun idsAreUnique() {
        assertEquals(Quiz.all.size, Quiz.all.map { it.id }.toSet().size)
    }

    @Test
    fun everyTopicFillsARound() {
        QuizTopic.entries.forEach { assertTrue(Quiz.inTopic(it).size >= Quiz.ROUND_SIZE, "$it has too few questions") }
    }

    @Test
    fun roundsHaveDistinctQuestionsAndKeepTheAnswer() {
        val topics = listOf<QuizTopic?>(null) + QuizTopic.entries
        topics.forEach { topic ->
            repeat(50) { seed ->
                val round = Quiz.round(topic, Random(seed))
                assertEquals(Quiz.ROUND_SIZE, round.size)
                assertEquals(round.size, round.map { it.question.id }.toSet().size)
                round.forEach { item ->
                    assertEquals(item.question.options.toSet(), item.options.toSet())
                    assertEquals(item.question.answer, item.options[item.answerIndex])
                    topic?.let { assertEquals(it, item.question.topic) }
                }
            }
        }
    }

    @Test
    fun sameSeedGivesSameRound() {
        assertEquals(Quiz.round(null, Random(7)), Quiz.round(null, Random(7)))
    }

    @Test
    fun answersThatDependOnTheMushafHold() {
        assertEquals(114, Quran.surahs.size)
        val longest = Quran.surahs.maxBy { it.ayahCount }
        assertEquals(2, longest.number)
        assertEquals(286, longest.ayahCount)
        assertTrue(Quran.isValid(AyahRef(2, 286)) && !Quran.isValid(AyahRef(2, 287)))
    }
}
