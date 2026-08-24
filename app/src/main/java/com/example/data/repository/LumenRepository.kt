package com.example.data.repository

import android.content.Context
import com.example.data.ai.ApiResult
import com.example.data.ai.GeminiClient
import com.example.data.ai.GeneratedDailyTask
import com.example.data.ai.GeneratedFlashcard
import com.example.data.ai.GeneratedNotesSummary
import com.example.data.ai.GeneratedQuizQuestion
import com.example.data.ai.GeneratedStudyPlan
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ConversationEntity
import com.example.data.local.FlashcardEntity
import com.example.data.local.LumenDatabase
import com.example.data.local.QuizResultEntity
import com.example.data.local.SecureStorage
import com.example.data.local.StudyNoteEntity
import com.example.data.local.StudyPlanEntity
import com.example.data.local.StudyPreferenceEntity
import com.example.data.local.StudyTaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONArray
import kotlin.math.roundToInt

data class StudentProgressOverview(
    val levelName: String,
    val levelProgressPercent: Float, // 0.0 to 1.0
    val totalQuizzes: Int,
    val averageAccuracy: Float,
    val totalFlashcardsMastered: Int,
    val totalFlashcards: Int,
    val completedTasksCount: Int,
    val totalTasksCount: Int,
    val strongSubjects: List<String>,
    val weakSubjects: List<String>,
    val personalizedInsights: List<String>
)

class LumenRepository(
    private val context: Context,
    private val database: LumenDatabase,
    val secureStorage: SecureStorage,
    val geminiClient: GeminiClient
) {
    private val chatDao = database.chatDao()
    private val flashcardDao = database.flashcardDao()
    private val quizDao = database.quizDao()
    private val studyPlanDao = database.studyPlanDao()
    private val studyNoteDao = database.studyNoteDao()
    private val preferenceDao = database.studyPreferenceDao()

    // Preferences
    val studyPreferences: Flow<StudyPreferenceEntity?> = preferenceDao.getPreferences()

    suspend fun getPreferencesSync(): StudyPreferenceEntity {
        return preferenceDao.getPreferencesSync() ?: StudyPreferenceEntity()
    }

    suspend fun updatePreferences(preferences: StudyPreferenceEntity) {
        preferenceDao.savePreferences(preferences)
    }

    // Chat
    val conversations: Flow<List<ConversationEntity>> = chatDao.getAllConversations()

    fun getMessages(conversationId: Long): Flow<List<ChatMessageEntity>> {
        return chatDao.getMessagesForConversation(conversationId)
    }

    suspend fun createConversation(title: String): Long {
        val entity = ConversationEntity(title = title)
        return chatDao.insertConversation(entity)
    }

    suspend fun saveMessage(conversationId: Long, role: String, content: String, subjectTag: String = ""): Long {
        val msgId = chatDao.insertMessage(
            ChatMessageEntity(
                conversationId = conversationId,
                role = role,
                content = content,
                subjectTag = subjectTag
            )
        )
        val snippet = if (content.length > 60) content.take(60) + "..." else content
        chatDao.updateConversation(conversationId, System.currentTimeMillis(), snippet)
        return msgId
    }

    suspend fun deleteConversation(id: Long) {
        chatDao.deleteMessagesForConversation(id)
        chatDao.deleteConversation(id)
    }

    suspend fun clearAllConversations() {
        chatDao.clearAllMessages()
        chatDao.clearAllConversations()
    }

    // Study explainer
    suspend fun askStudyQuestion(
        question: String,
        subject: String,
        chapter: String,
        educationLevel: String,
        classYear: String,
        board: String
    ): ApiResult<String> {
        val prompt = """
Student Context:
- Education Level: $educationLevel
- Class/Year: $classYear
- Board/University: $board
- Current Subject: $subject
- Current Chapter/Topic: $chapter

Question/Doubt:
$question

Provide an insightful, crystal-clear explanation structured specifically for exam success and conceptual clarity.
"""
        return geminiClient.generateContent(prompt)
    }

    // Quizzes
    val allQuizResults: Flow<List<QuizResultEntity>> = quizDao.getAllQuizResults()
    val recentQuizResults: Flow<List<QuizResultEntity>> = quizDao.getRecentQuizResults(5)

    suspend fun generateQuiz(
        subject: String,
        chapter: String,
        difficulty: String,
        questionCount: Int
    ): ApiResult<List<GeneratedQuizQuestion>> {
        val prefs = getPreferencesSync()
        return geminiClient.generateQuiz(
            subject = subject,
            chapter = chapter,
            educationLevel = prefs.educationLevel,
            classYear = prefs.classYear,
            board = prefs.board,
            difficulty = difficulty,
            questionCount = questionCount
        )
    }

    suspend fun saveQuizResult(
        title: String,
        subject: String,
        chapter: String,
        score: Int,
        totalQuestions: Int,
        correctCount: Int,
        incorrectCount: Int,
        weakTopics: List<String>,
        strongTopics: List<String>
    ): Long {
        val prefs = getPreferencesSync()
        val accuracy = if (totalQuestions > 0) (correctCount.toFloat() / totalQuestions.toFloat()) * 100f else 0f
        val weakJson = JSONArray(weakTopics).toString()
        val strongJson = JSONArray(strongTopics).toString()

        val entity = QuizResultEntity(
            title = title,
            subject = subject,
            chapter = chapter,
            educationLevel = prefs.educationLevel,
            classYear = prefs.classYear,
            board = prefs.board,
            score = score,
            totalQuestions = totalQuestions,
            accuracy = accuracy,
            correctCount = correctCount,
            incorrectCount = incorrectCount,
            weakTopicsJson = weakJson,
            strongTopicsJson = strongJson
        )
        return quizDao.insertQuizResult(entity)
    }

    // Flashcards
    val allFlashcards: Flow<List<FlashcardEntity>> = flashcardDao.getAllFlashcards()
    val deckNames: Flow<List<String>> = flashcardDao.getAllDeckNames()

    fun getFlashcardsForDeck(deck: String): Flow<List<FlashcardEntity>> = flashcardDao.getFlashcardsByDeck(deck)

    suspend fun generateFlashcardsWithAi(
        deckName: String,
        subject: String,
        chapter: String,
        notes: String,
        count: Int = 6
    ): ApiResult<List<FlashcardEntity>> {
        val result = geminiClient.generateFlashcards(subject, chapter, notes, count)
        return when (result) {
            is ApiResult.Success -> {
                val entities = result.data.map { card ->
                    FlashcardEntity(
                        deckName = deckName,
                        question = card.question,
                        answer = card.answer,
                        difficulty = card.difficulty,
                        subject = subject,
                        chapter = chapter
                    )
                }
                flashcardDao.insertFlashcards(entities)
                ApiResult.Success(entities)
            }
            is ApiResult.Error -> result
        }
    }

    suspend fun saveCustomFlashcard(
        deckName: String,
        question: String,
        answer: String,
        difficulty: String = "Medium",
        subject: String = "General"
    ): Long {
        val entity = FlashcardEntity(
            deckName = deckName.ifBlank { "General Deck" },
            question = question,
            answer = answer,
            difficulty = difficulty,
            subject = subject
        )
        return flashcardDao.insertFlashcard(entity)
    }

    suspend fun markFlashcardReviewed(id: Long, mastered: Boolean) {
        flashcardDao.markReviewStatus(id, mastered)
    }

    suspend fun deleteDeck(deckName: String) {
        flashcardDao.deleteDeck(deckName)
    }

    suspend fun deleteFlashcard(card: FlashcardEntity) {
        flashcardDao.deleteFlashcard(card)
    }

    // Notes Summarizer
    val allNotes: Flow<List<StudyNoteEntity>> = studyNoteDao.getAllNotes()

    suspend fun summarizeAndSaveNotes(
        title: String,
        rawNotes: String,
        subject: String,
        chapter: String
    ): ApiResult<StudyNoteEntity> {
        val result = geminiClient.summarizeNotes(rawNotes, subject)
        return when (result) {
            is ApiResult.Success -> {
                val data = result.data
                val entity = StudyNoteEntity(
                    title = if (title.isNotBlank()) title else data.title,
                    rawContent = rawNotes,
                    summary = data.detailedSummary.ifBlank { data.shortSummary },
                    keyPointsJson = JSONArray(data.keyPoints).toString(),
                    definitionsJson = JSONArray(data.definitions.map { "${it.first} : ${it.second}" }).toString(),
                    revisionNotes = data.revisionTips,
                    subject = subject,
                    chapter = chapter
                )
                val id = studyNoteDao.insertNote(entity)
                ApiResult.Success(entity.copy(id = id))
            }
            is ApiResult.Error -> result
        }
    }

    suspend fun deleteNote(note: StudyNoteEntity) {
        studyNoteDao.deleteNote(note)
    }

    // Study Planner
    val activeStudyPlan: Flow<StudyPlanEntity?> = studyPlanDao.getActivePlan()
    val allStudyTasks: Flow<List<StudyTaskEntity>> = studyPlanDao.getAllTasks()

    fun getTasksForPlan(planId: Long): Flow<List<StudyTaskEntity>> = studyPlanDao.getTasksForPlan(planId)

    suspend fun generateAndSavePlan(
        subjects: List<String>,
        targetExamDate: String,
        dailyHours: Float,
        difficultSubjects: List<String>,
        sessionDuration: Int
    ): ApiResult<StudyPlanEntity> {
        val result = geminiClient.generateStudyPlan(subjects, targetExamDate, dailyHours, difficultSubjects, sessionDuration)
        return when (result) {
            is ApiResult.Success -> {
                val data = result.data
                studyPlanDao.deactivateAllPlans()
                val planEntity = StudyPlanEntity(
                    title = data.planTitle,
                    targetExamDate = targetExamDate,
                    dailyHours = dailyHours,
                    subjectsJson = JSONArray(subjects).toString(),
                    planDetailsJson = data.overview,
                    isActive = true
                )
                val planId = studyPlanDao.insertPlan(planEntity)
                val taskEntities = data.tasks.map { task ->
                    StudyTaskEntity(
                        planId = planId,
                        dayNumber = task.dayNumber,
                        taskTitle = task.taskTitle,
                        subject = task.subject,
                        durationMinutes = task.durationMinutes,
                        isRevision = task.isRevision
                    )
                }
                studyPlanDao.insertTasks(taskEntities)
                ApiResult.Success(planEntity.copy(id = planId))
            }
            is ApiResult.Error -> result
        }
    }

    suspend fun toggleTaskCompleted(taskId: Long, completed: Boolean) {
        studyPlanDao.setTaskCompleted(taskId, completed)
    }

    suspend fun deletePlan(planId: Long) {
        studyPlanDao.deleteTasksForPlan(planId)
        studyPlanDao.deletePlan(planId)
    }

    // Student Progress Analytics
    suspend fun calculateProgressOverview(): StudentProgressOverview {
        val quizList = quizDao.getAllQuizResults().firstOrNull() ?: emptyList()
        val flashcardsList = flashcardDao.getAllFlashcards().firstOrNull() ?: emptyList()
        val taskList = studyPlanDao.getAllTasks().firstOrNull() ?: emptyList()

        val totalQuizzes = quizList.size
        val avgAccuracy = if (totalQuizzes > 0) {
            quizList.map { it.accuracy }.average().toFloat()
        } else {
            0f
        }

        val totalFlashcards = flashcardsList.size
        val masteredFlashcards = flashcardsList.count { it.isMastered }

        val totalTasks = taskList.size
        val completedTasks = taskList.count { it.isCompleted }

        // Determine student level based on real activities
        val activityScore = (totalQuizzes * 15) + (avgAccuracy * 0.4f) + (masteredFlashcards * 10) + (completedTasks * 5)
        val normalizedProgress = (activityScore / 250f).coerceIn(0f, 1f)

        val levelName = when {
            normalizedProgress >= 0.90f -> "Master"
            normalizedProgress >= 0.75f -> "Excellent"
            normalizedProgress >= 0.55f -> "Advanced"
            normalizedProgress >= 0.35f -> "Good"
            normalizedProgress >= 0.15f -> "Improving"
            else -> "Beginner"
        }

        // Calculate subject strengths & weaknesses
        val subjectScores = mutableMapOf<String, MutableList<Float>>()
        for (quiz in quizList) {
            subjectScores.getOrPut(quiz.subject) { mutableListOf() }.add(quiz.accuracy)
        }

        val strongSubjects = mutableListOf<String>()
        val weakSubjects = mutableListOf<String>()

        for ((subj, accList) in subjectScores) {
            val avg = accList.average()
            if (avg >= 70.0) {
                strongSubjects.add(subj)
            } else {
                weakSubjects.add(subj)
            }
        }

        // Generate personalized real insights
        val insights = mutableListOf<String>()
        if (totalQuizzes == 0) {
            insights.add("Start by taking your first chapter quiz to calibrate your baseline proficiency.")
        } else {
            if (avgAccuracy >= 80f) {
                insights.add("Outstanding retention! Your average quiz accuracy is ${avgAccuracy.roundToInt()}%.")
            } else if (avgAccuracy >= 50f) {
                insights.add("Solid progress! Your overall quiz accuracy is at ${avgAccuracy.roundToInt()}%.")
            } else {
                insights.add("Targeted revision can boost your quiz scores. Try reviewing flashcards before tests.")
            }

            if (strongSubjects.isNotEmpty()) {
                insights.add("You are performing well in ${strongSubjects.joinToString(", ")}.")
            }
            if (weakSubjects.isNotEmpty()) {
                insights.add("You may need more revision in ${weakSubjects.joinToString(", ")}.")
            }
        }

        if (totalFlashcards > 0) {
            insights.add("You have mastered $masteredFlashcards out of $totalFlashcards flashcards.")
        }

        if (completedTasks > 0) {
            insights.add("You have completed $completedTasks study tasks from your planner.")
        }

        return StudentProgressOverview(
            levelName = levelName,
            levelProgressPercent = normalizedProgress,
            totalQuizzes = totalQuizzes,
            averageAccuracy = avgAccuracy,
            totalFlashcardsMastered = masteredFlashcards,
            totalFlashcards = totalFlashcards,
            completedTasksCount = completedTasks,
            totalTasksCount = totalTasks,
            strongSubjects = strongSubjects,
            weakSubjects = weakSubjects,
            personalizedInsights = insights
        )
    }

    suspend fun clearAllLocalData() {
        chatDao.clearAllMessages()
        chatDao.clearAllConversations()
        flashcardDao.clearAllFlashcards()
        quizDao.clearAllQuizResults()
        studyPlanDao.clearAllPlans()
        studyPlanDao.clearAllTasks()
        studyNoteDao.clearAllNotes()
        secureStorage.clearAllData()
    }
}
