package com.example

import com.example.data.ai.GeneratedFlashcard
import com.example.data.ai.GeneratedNotesSummary
import com.example.data.ai.GeneratedQuizQuestion
import com.example.data.local.ChatMessageEntity
import com.example.data.local.FlashcardEntity
import com.example.data.local.QuizResultEntity
import com.example.data.local.StudyPreferenceEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testQuizQuestionModel() {
        val question = GeneratedQuizQuestion(
            question = "What is Coulomb's Law?",
            options = listOf("F = k*q1*q2/r^2", "F = m*a", "E = m*c^2", "V = I*R"),
            correctIndex = 0,
            explanation = "Coulomb's Law quantifies the electrostatic force between two charges.",
            topicTag = "Electrostatics"
        )
        assertEquals(0, question.correctIndex)
        assertEquals("Electrostatics", question.topicTag)
        assertEquals(4, question.options.size)
    }

    @Test
    fun testFlashcardMastery() {
        val card = FlashcardEntity(
            deckName = "Physics 12th",
            question = "Define Electric Flux",
            answer = "The total number of electric lines of force passing normally through a given area.",
            isMastered = false,
            reviewCount = 0
        )
        assertFalse(card.isMastered)
        assertEquals(0, card.reviewCount)
    }

    @Test
    fun testStudyPreferencesDefaults() {
        val pref = StudyPreferenceEntity()
        assertEquals("JKBOSE", pref.board)
        assertEquals("Higher Secondary", pref.educationLevel)
        assertEquals("Physics", pref.currentSubject)
        assertEquals("06:00 AM - 08:30 AM", pref.morningRevisionTime)
        assertEquals("04:30 PM - 09:00 PM", pref.eveningRevisionTime)
    }

    @Test
    fun testQuizResultAccuracyCalculation() {
        val correct = 4
        val total = 5
        val accuracy = (correct.toFloat() / total.toFloat()) * 100f
        assertEquals(80f, accuracy, 0.01f)
    }

    @Test
    fun testNotesSummaryModel() {
        val summary = GeneratedNotesSummary(
            title = "Thermodynamics",
            shortSummary = "Study of heat and work.",
            detailedSummary = "First and Second Laws explained.",
            keyPoints = listOf("Energy is conserved", "Entropy increases"),
            definitions = listOf(Pair("Entropy", "Measure of disorder")),
            revisionTips = "Remember Q = delta U + W"
        )
        assertEquals("Thermodynamics", summary.title)
        assertEquals(2, summary.keyPoints.size)
        assertEquals(1, summary.definitions.size)
    }
}
