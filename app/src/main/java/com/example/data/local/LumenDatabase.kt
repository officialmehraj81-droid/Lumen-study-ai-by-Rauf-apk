package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChatMessageEntity::class,
        ConversationEntity::class,
        FlashcardEntity::class,
        QuizResultEntity::class,
        StudyPlanEntity::class,
        StudyTaskEntity::class,
        StudyNoteEntity::class,
        StudyPreferenceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LumenDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun quizDao(): QuizDao
    abstract fun studyPlanDao(): StudyPlanDao
    abstract fun studyNoteDao(): StudyNoteDao
    abstract fun studyPreferenceDao(): StudyPreferenceDao

    companion object {
        @Volatile
        private var INSTANCE: LumenDatabase? = null

        fun getInstance(context: Context): LumenDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LumenDatabase::class.java,
                    "lumen_study_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
