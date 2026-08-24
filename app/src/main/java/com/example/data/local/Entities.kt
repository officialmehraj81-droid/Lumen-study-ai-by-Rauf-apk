package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val role: String, // "user" or "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val subjectTag: String = ""
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastMessage: String = ""
)

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckName: String,
    val question: String,
    val answer: String,
    val difficulty: String = "Medium", // Easy, Medium, Hard
    val reviewCount: Int = 0,
    val isMastered: Boolean = false,
    val subject: String = "General",
    val chapter: String = "",
    val lastReviewed: Long = System.currentTimeMillis()
)

@Entity(tableName = "quiz_results")
data class QuizResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subject: String,
    val chapter: String,
    val educationLevel: String,
    val classYear: String,
    val board: String,
    val score: Int,
    val totalQuestions: Int,
    val accuracy: Float,
    val correctCount: Int,
    val incorrectCount: Int,
    val weakTopicsJson: String = "[]",
    val strongTopicsJson: String = "[]",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_plans")
data class StudyPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetExamDate: String,
    val dailyHours: Float,
    val subjectsJson: String,
    val planDetailsJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

@Entity(tableName = "study_tasks")
data class StudyTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long = 0,
    val dayNumber: Int = 1,
    val taskTitle: String,
    val subject: String,
    val durationMinutes: Int = 45,
    val isCompleted: Boolean = false,
    val isRevision: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_notes")
data class StudyNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val rawContent: String,
    val summary: String,
    val keyPointsJson: String = "[]",
    val definitionsJson: String = "[]",
    val revisionNotes: String = "",
    val subject: String = "General",
    val chapter: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_preferences")
data class StudyPreferenceEntity(
    @PrimaryKey val id: Int = 1,
    val educationLevel: String = "Higher Secondary",
    val classYear: String = "Class 12th",
    val board: String = "JKBOSE",
    val currentSubject: String = "Physics",
    val currentChapter: String = "Electrostatics",
    val schoolStartTime: String = "09:00 AM",
    val schoolEndTime: String = "03:30 PM",
    val morningRevisionTime: String = "06:00 AM - 08:30 AM",
    val eveningRevisionTime: String = "04:30 PM - 09:00 PM",
    val dailyGoalMinutes: Int = 120,
    val reminderEnabled: Boolean = true,
    val reminderTime: String = "18:00"
)
