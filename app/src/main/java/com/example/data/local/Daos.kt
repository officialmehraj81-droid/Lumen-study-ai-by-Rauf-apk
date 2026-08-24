package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Query("UPDATE conversations SET updatedAt = :timestamp, lastMessage = :lastMessage WHERE id = :id")
    suspend fun updateConversation(id: Long, timestamp: Long, lastMessage: String)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: Long)

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: Long)

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessagesList(conversationId: Long): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()

    @Query("DELETE FROM conversations")
    suspend fun clearAllConversations()
}

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards ORDER BY lastReviewed ASC")
    fun getAllFlashcards(): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE deckName = :deckName ORDER BY id ASC")
    fun getFlashcardsByDeck(deckName: String): Flow<List<FlashcardEntity>>

    @Query("SELECT DISTINCT deckName FROM flashcards")
    fun getAllDeckNames(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: FlashcardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(flashcards: List<FlashcardEntity>)

    @Update
    suspend fun updateFlashcard(flashcard: FlashcardEntity)

    @Query("UPDATE flashcards SET isMastered = :isMastered, reviewCount = reviewCount + 1, lastReviewed = :timestamp WHERE id = :id")
    suspend fun markReviewStatus(id: Long, isMastered: Boolean, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteFlashcard(flashcard: FlashcardEntity)

    @Query("DELETE FROM flashcards WHERE deckName = :deckName")
    suspend fun deleteDeck(deckName: String)

    @Query("DELETE FROM flashcards")
    suspend fun clearAllFlashcards()
}

@Dao
interface QuizDao {
    @Query("SELECT * FROM quiz_results ORDER BY timestamp DESC")
    fun getAllQuizResults(): Flow<List<QuizResultEntity>>

    @Query("SELECT * FROM quiz_results ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentQuizResults(limit: Int): Flow<List<QuizResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizResult(result: QuizResultEntity): Long

    @Query("DELETE FROM quiz_results")
    suspend fun clearAllQuizResults()
}

@Dao
interface StudyPlanDao {
    @Query("SELECT * FROM study_plans ORDER BY createdAt DESC")
    fun getAllPlans(): Flow<List<StudyPlanEntity>>

    @Query("SELECT * FROM study_plans WHERE isActive = 1 LIMIT 1")
    fun getActivePlan(): Flow<StudyPlanEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: StudyPlanEntity): Long

    @Query("UPDATE study_plans SET isActive = 0")
    suspend fun deactivateAllPlans()

    @Query("UPDATE study_plans SET isActive = 1 WHERE id = :id")
    suspend fun setActivePlan(id: Long)

    @Query("DELETE FROM study_plans WHERE id = :id")
    suspend fun deletePlan(id: Long)

    @Query("SELECT * FROM study_tasks WHERE planId = :planId ORDER BY dayNumber ASC, id ASC")
    fun getTasksForPlan(planId: Long): Flow<List<StudyTaskEntity>>

    @Query("SELECT * FROM study_tasks ORDER BY dayNumber ASC, id ASC")
    fun getAllTasks(): Flow<List<StudyTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<StudyTaskEntity>)

    @Query("UPDATE study_tasks SET isCompleted = :completed WHERE id = :id")
    suspend fun setTaskCompleted(id: Long, completed: Boolean)

    @Query("DELETE FROM study_tasks WHERE planId = :planId")
    suspend fun deleteTasksForPlan(planId: Long)

    @Query("DELETE FROM study_plans")
    suspend fun clearAllPlans()

    @Query("DELETE FROM study_tasks")
    suspend fun clearAllTasks()
}

@Dao
interface StudyNoteDao {
    @Query("SELECT * FROM study_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<StudyNoteEntity>>

    @Query("SELECT * FROM study_notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Long): StudyNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: StudyNoteEntity): Long

    @Delete
    suspend fun deleteNote(note: StudyNoteEntity)

    @Query("DELETE FROM study_notes")
    suspend fun clearAllNotes()
}

@Dao
interface StudyPreferenceDao {
    @Query("SELECT * FROM study_preferences WHERE id = 1 LIMIT 1")
    fun getPreferences(): Flow<StudyPreferenceEntity?>

    @Query("SELECT * FROM study_preferences WHERE id = 1 LIMIT 1")
    suspend fun getPreferencesSync(): StudyPreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreferences(preferences: StudyPreferenceEntity)
}
