package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.ApiResult
import com.example.data.ai.GeminiClient
import com.example.data.ai.GeneratedDailyTask
import com.example.data.ai.GeneratedFlashcard
import com.example.data.ai.GeneratedNotesSummary
import com.example.data.ai.GeneratedQuizQuestion
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
import com.example.data.notifications.StudyReminderManager
import com.example.data.repository.LumenRepository
import com.example.data.repository.StudentProgressOverview
import com.example.data.sound.SoundManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ScreenDestination {
    object Onboarding : ScreenDestination()
    object Home : ScreenDestination()
    object Chat : ScreenDestination()
    object Study : ScreenDestination()
    object Quiz : ScreenDestination()
    object Library : ScreenDestination()
    object Progress : ScreenDestination()
    object Settings : ScreenDestination()
}

class LumenViewModel(application: Application) : AndroidViewModel(application) {
    val secureStorage = SecureStorage(application)
    private val database = LumenDatabase.getInstance(application)
    val geminiClient = GeminiClient(application, secureStorage)
    val soundManager = SoundManager(application, secureStorage)
    val reminderManager = StudyReminderManager(application)
    val repository = LumenRepository(application, database, secureStorage, geminiClient)

    // Destination
    private val _currentScreen = MutableStateFlow<ScreenDestination>(
        if (secureStorage.isOnboarded()) ScreenDestination.Home else ScreenDestination.Onboarding
    )
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    fun navigateTo(screen: ScreenDestination) {
        soundManager.playModeSwitch()
        _currentScreen.value = screen
    }

    // Appearance State
    val themeMode: StateFlow<String> = secureStorage.themeModeFlow
    val accentColor: StateFlow<String> = secureStorage.accentColorFlow
    val soundEnabled: StateFlow<Boolean> = secureStorage.soundEnabledFlow

    fun updateThemeMode(mode: String) {
        soundManager.playClick()
        secureStorage.saveThemeMode(mode)
    }

    fun updateAccentColor(color: String) {
        soundManager.playClick()
        secureStorage.saveAccentColor(color)
    }

    fun toggleSound(enabled: Boolean) {
        secureStorage.setSoundEnabled(enabled)
        if (enabled) soundManager.playClick()
    }

    fun toggleHaptic(enabled: Boolean) {
        secureStorage.setHapticEnabled(enabled)
        soundManager.playClick()
    }

    fun resetAppearance() {
        soundManager.playSuccess()
        secureStorage.resetAppearanceToDefault()
    }

    // Onboarding & API Key Validation State
    private val _onboardingStep = MutableStateFlow(1)
    val onboardingStep: StateFlow<Int> = _onboardingStep.asStateFlow()

    private val _apiKeyInput = MutableStateFlow("")
    val apiKeyInput: StateFlow<String> = _apiKeyInput.asStateFlow()

    private val _isValidatingKey = MutableStateFlow(false)
    val isValidatingKey: StateFlow<Boolean> = _isValidatingKey.asStateFlow()

    private val _validationError = MutableStateFlow<String?>(null)
    val validationError: StateFlow<String?> = _validationError.asStateFlow()

    private val _validationSuccess = MutableStateFlow(false)
    val validationSuccess: StateFlow<Boolean> = _validationSuccess.asStateFlow()

    fun setOnboardingStep(step: Int) {
        soundManager.playClick()
        _onboardingStep.value = step
    }

    fun updateApiKeyInput(key: String) {
        _apiKeyInput.value = key
        _validationError.value = null
    }

    fun validateAndConnectApiKey(key: String, onComplete: () -> Unit = {}) {
        val cleanKey = key.trim()
        if (cleanKey.isBlank()) {
            _validationError.value = "Please enter a valid Gemini API key."
            return
        }
        _isValidatingKey.value = true
        _validationError.value = null
        _validationSuccess.value = false

        viewModelScope.launch {
            when (val result = geminiClient.validateApiKey(cleanKey)) {
                is ApiResult.Success -> {
                    secureStorage.saveApiKey(cleanKey)
                    secureStorage.setOnboarded(true)
                    _isValidatingKey.value = false
                    _validationSuccess.value = true
                    soundManager.playSuccess()
                    _currentScreen.value = ScreenDestination.Home
                    onComplete()
                }
                is ApiResult.Error -> {
                    _isValidatingKey.value = false
                    _validationError.value = result.userFriendlyMessage
                }
            }
        }
    }

    fun removeApiKey() {
        soundManager.playClick()
        secureStorage.removeApiKey()
        secureStorage.setOnboarded(false)
        _currentScreen.value = ScreenDestination.Onboarding
        _onboardingStep.value = 2
    }

    // Chat ("Ask Lumen") State
    val conversations = repository.conversations.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedConversationId = MutableStateFlow<Long?>(null)
    val selectedConversationId: StateFlow<Long?> = _selectedConversationId.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessageEntity>> = _chatMessages.asStateFlow()

    private val _chatInput = MutableStateFlow("")
    val chatInput: StateFlow<String> = _chatInput.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _streamingMessage = MutableStateFlow("")
    val streamingMessage: StateFlow<String> = _streamingMessage.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    private var messageObserveJob: Job? = null

    fun selectConversation(id: Long) {
        _selectedConversationId.value = id
        messageObserveJob?.cancel()
        messageObserveJob = viewModelScope.launch {
            repository.getMessages(id).collectLatest { msgs ->
                _chatMessages.value = msgs
            }
        }
    }

    fun startNewConversation() {
        soundManager.playClick()
        viewModelScope.launch {
            val count = (conversations.value.size + 1)
            val newId = repository.createConversation("Study Session $count")
            selectConversation(newId)
        }
    }

    fun deleteConversation(id: Long) {
        soundManager.playClick()
        viewModelScope.launch {
            repository.deleteConversation(id)
            if (_selectedConversationId.value == id) {
                _selectedConversationId.value = null
            }
        }
    }

    fun updateChatInput(text: String) {
        _chatInput.value = text
    }

    fun sendChatMessage(customPrompt: String? = null) {
        val messageText = (customPrompt ?: _chatInput.value).trim()
        if (messageText.isBlank() || _isAiThinking.value) return

        soundManager.playClick()
        _chatInput.value = ""
        _chatError.value = null

        viewModelScope.launch {
            var convId = _selectedConversationId.value
            if (convId == null) {
                val title = if (messageText.length > 25) messageText.take(25) + "..." else messageText
                convId = repository.createConversation(title)
                _selectedConversationId.value = convId
                selectConversation(convId)
            }

            // Save user message
            repository.saveMessage(convId, "user", messageText)

            _isAiThinking.value = true
            _streamingMessage.value = ""

            // Prepare history
            val currentMsgs = _chatMessages.value
            val history = currentMsgs.takeLast(10).map { Pair(it.role, it.content) }

            var fullText = ""
            try {
                geminiClient.streamGenerateContent(messageText, history).collect { chunk ->
                    if (chunk.startsWith("Error:")) {
                        _chatError.value = chunk.removePrefix("Error:").trim()
                    } else {
                        fullText += chunk
                        _streamingMessage.value = fullText
                    }
                }

                if (fullText.isNotBlank()) {
                    repository.saveMessage(convId, "model", fullText)
                    soundManager.playSuccess()
                } else if (_chatError.value == null) {
                    // Fallback to one-shot if stream was empty
                    when (val res = geminiClient.generateContent(messageText, history)) {
                        is ApiResult.Success -> {
                            repository.saveMessage(convId, "model", res.data)
                            soundManager.playSuccess()
                        }
                        is ApiResult.Error -> {
                            _chatError.value = res.userFriendlyMessage
                        }
                    }
                }
            } catch (e: Exception) {
                _chatError.value = "Failed to receive response. Please try again."
            } finally {
                _isAiThinking.value = false
                _streamingMessage.value = ""
            }
        }
    }

    fun regenerateLastResponse() {
        val msgs = _chatMessages.value
        val lastUserMsg = msgs.lastOrNull { it.role == "user" } ?: return
        sendChatMessage(lastUserMsg.content)
    }

    // Study Mode & Concept Explainer
    val studyPreferences = repository.studyPreferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _studyQuestionInput = MutableStateFlow("")
    val studyQuestionInput: StateFlow<String> = _studyQuestionInput.asStateFlow()

    private val _studyAnswer = MutableStateFlow<String?>(null)
    val studyAnswer: StateFlow<String?> = _studyAnswer.asStateFlow()

    private val _isStudyLoading = MutableStateFlow(false)
    val isStudyLoading: StateFlow<Boolean> = _isStudyLoading.asStateFlow()

    private val _studyError = MutableStateFlow<String?>(null)
    val studyError: StateFlow<String?> = _studyError.asStateFlow()

    fun updateStudyQuestion(q: String) {
        _studyQuestionInput.value = q
    }

    fun updateStudyPreferences(pref: StudyPreferenceEntity) {
        soundManager.playClick()
        viewModelScope.launch {
            repository.updatePreferences(pref)
        }
    }

    fun explainStudyConcept(question: String? = null) {
        val q = (question ?: _studyQuestionInput.value).trim()
        if (q.isBlank() || _isStudyLoading.value) return

        soundManager.playClick()
        _isStudyLoading.value = true
        _studyError.value = null
        _studyAnswer.value = null

        viewModelScope.launch {
            val prefs = repository.getPreferencesSync()
            when (val result = repository.askStudyQuestion(
                question = q,
                subject = prefs.currentSubject,
                chapter = prefs.currentChapter,
                educationLevel = prefs.educationLevel,
                classYear = prefs.classYear,
                board = prefs.board
            )) {
                is ApiResult.Success -> {
                    _studyAnswer.value = result.data
                    soundManager.playSuccess()
                }
                is ApiResult.Error -> {
                    _studyError.value = result.userFriendlyMessage
                }
            }
            _isStudyLoading.value = false
        }
    }

    // Quiz Mode State
    private val _isQuizGenerating = MutableStateFlow(false)
    val isQuizGenerating: StateFlow<Boolean> = _isQuizGenerating.asStateFlow()

    private val _activeQuizQuestions = MutableStateFlow<List<GeneratedQuizQuestion>>(emptyList())
    val activeQuizQuestions: StateFlow<List<GeneratedQuizQuestion>> = _activeQuizQuestions.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _selectedAnswers = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val selectedAnswers: StateFlow<Map<Int, Int>> = _selectedAnswers.asStateFlow()

    private val _isQuizCompleted = MutableStateFlow(false)
    val isQuizCompleted: StateFlow<Boolean> = _isQuizCompleted.asStateFlow()

    private val _lastQuizResult = MutableStateFlow<QuizResultEntity?>(null)
    val lastQuizResult: StateFlow<QuizResultEntity?> = _lastQuizResult.asStateFlow()

    private val _quizError = MutableStateFlow<String?>(null)
    val quizError: StateFlow<String?> = _quizError.asStateFlow()

    val quizHistory = repository.allQuizResults.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startQuiz(
        subject: String,
        chapter: String,
        difficulty: String = "Medium",
        questionCount: Int = 5
    ) {
        soundManager.playClick()
        _isQuizGenerating.value = true
        _quizError.value = null
        _activeQuizQuestions.value = emptyList()
        _currentQuestionIndex.value = 0
        _selectedAnswers.value = emptyMap()
        _isQuizCompleted.value = false
        _lastQuizResult.value = null

        viewModelScope.launch {
            when (val result = repository.generateQuiz(subject, chapter, difficulty, questionCount)) {
                is ApiResult.Success -> {
                    _activeQuizQuestions.value = result.data
                    _isQuizGenerating.value = false
                    soundManager.playSuccess()
                }
                is ApiResult.Error -> {
                    _quizError.value = result.userFriendlyMessage
                    _isQuizGenerating.value = false
                }
            }
        }
    }

    fun answerCurrentQuestion(optionIndex: Int) {
        val currentQIndex = _currentQuestionIndex.value
        val questions = _activeQuizQuestions.value
        if (currentQIndex >= questions.size) return

        val isCorrect = (optionIndex == questions[currentQIndex].correctIndex)
        soundManager.playQuizAnswer(isCorrect)

        val newAnswers = _selectedAnswers.value.toMutableMap()
        newAnswers[currentQIndex] = optionIndex
        _selectedAnswers.value = newAnswers
    }

    fun nextQuestion() {
        soundManager.playClick()
        val nextIdx = _currentQuestionIndex.value + 1
        if (nextIdx < _activeQuizQuestions.value.size) {
            _currentQuestionIndex.value = nextIdx
        } else {
            completeQuiz()
        }
    }

    fun previousQuestion() {
        soundManager.playClick()
        if (_currentQuestionIndex.value > 0) {
            _currentQuestionIndex.value -= 1
        }
    }

    private fun completeQuiz() {
        soundManager.playQuizComplete()
        _isQuizCompleted.value = true

        val questions = _activeQuizQuestions.value
        val answers = _selectedAnswers.value
        var correct = 0
        var incorrect = 0
        val weakTopics = mutableListOf<String>()
        val strongTopics = mutableListOf<String>()

        for (i in questions.indices) {
            val q = questions[i]
            val selected = answers[i]
            if (selected == q.correctIndex) {
                correct++
                if (!strongTopics.contains(q.topicTag)) strongTopics.add(q.topicTag)
            } else {
                incorrect++
                if (!weakTopics.contains(q.topicTag)) weakTopics.add(q.topicTag)
            }
        }

        val total = questions.size
        val score = correct * 10

        viewModelScope.launch {
            val prefs = repository.getPreferencesSync()
            val resultId = repository.saveQuizResult(
                title = "${prefs.currentSubject} Quiz",
                subject = prefs.currentSubject,
                chapter = prefs.currentChapter,
                score = score,
                totalQuestions = total,
                correctCount = correct,
                incorrectCount = incorrect,
                weakTopics = weakTopics,
                strongTopics = strongTopics
            )
            _lastQuizResult.value = QuizResultEntity(
                id = resultId,
                title = "${prefs.currentSubject} Quiz",
                subject = prefs.currentSubject,
                chapter = prefs.currentChapter,
                educationLevel = prefs.educationLevel,
                classYear = prefs.classYear,
                board = prefs.board,
                score = score,
                totalQuestions = total,
                accuracy = if (total > 0) (correct.toFloat() / total) * 100f else 0f,
                correctCount = correct,
                incorrectCount = incorrect
            )
            loadProgress()
        }
    }

    fun resetQuiz() {
        soundManager.playClick()
        _activeQuizQuestions.value = emptyList()
        _isQuizCompleted.value = false
        _lastQuizResult.value = null
        _selectedAnswers.value = emptyMap()
        _currentQuestionIndex.value = 0
    }

    // Flashcards State
    val allFlashcards = repository.allFlashcards.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val deckNames = repository.deckNames.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDeck = MutableStateFlow<String>("All Cards")
    val selectedDeck: StateFlow<String> = _selectedDeck.asStateFlow()

    private val _isFlashcardGenerating = MutableStateFlow(false)
    val isFlashcardGenerating: StateFlow<Boolean> = _isFlashcardGenerating.asStateFlow()

    private val _flashcardError = MutableStateFlow<String?>(null)
    val flashcardError: StateFlow<String?> = _flashcardError.asStateFlow()

    fun selectDeck(deck: String) {
        soundManager.playClick()
        _selectedDeck.value = deck
    }

    fun generateFlashcards(deckName: String, notesOrTopic: String) {
        soundManager.playClick()
        _isFlashcardGenerating.value = true
        _flashcardError.value = null

        viewModelScope.launch {
            val prefs = repository.getPreferencesSync()
            when (val result = repository.generateFlashcardsWithAi(
                deckName = deckName.ifBlank { "${prefs.currentSubject} - ${prefs.currentChapter}" },
                subject = prefs.currentSubject,
                chapter = prefs.currentChapter,
                notes = notesOrTopic
            )) {
                is ApiResult.Success -> {
                    _isFlashcardGenerating.value = false
                    soundManager.playSuccess()
                    loadProgress()
                }
                is ApiResult.Error -> {
                    _isFlashcardGenerating.value = false
                    _flashcardError.value = result.userFriendlyMessage
                }
            }
        }
    }

    fun addCustomFlashcard(deckName: String, question: String, answer: String, difficulty: String = "Medium") {
        soundManager.playClick()
        viewModelScope.launch {
            val prefs = repository.getPreferencesSync()
            repository.saveCustomFlashcard(deckName, question, answer, difficulty, prefs.currentSubject)
            soundManager.playSuccess()
            loadProgress()
        }
    }

    fun markCardReviewed(cardId: Long, mastered: Boolean) {
        soundManager.playCardFlip()
        viewModelScope.launch {
            repository.markFlashcardReviewed(cardId, mastered)
            loadProgress()
        }
    }

    fun deleteDeck(deck: String) {
        soundManager.playClick()
        viewModelScope.launch {
            repository.deleteDeck(deck)
            _selectedDeck.value = "All Cards"
            loadProgress()
        }
    }

    fun deleteCard(card: FlashcardEntity) {
        soundManager.playClick()
        viewModelScope.launch {
            repository.deleteFlashcard(card)
            loadProgress()
        }
    }

    // Notes Summarizer
    val allNotes = repository.allNotes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSummarizing = MutableStateFlow(false)
    val isSummarizing: StateFlow<Boolean> = _isSummarizing.asStateFlow()

    private val _notesSummaryError = MutableStateFlow<String?>(null)
    val notesSummaryError: StateFlow<String?> = _notesSummaryError.asStateFlow()

    private val _activeSummary = MutableStateFlow<StudyNoteEntity?>(null)
    val activeSummary: StateFlow<StudyNoteEntity?> = _activeSummary.asStateFlow()

    fun summarizeNotes(title: String, rawNotes: String) {
        if (rawNotes.isBlank()) return
        soundManager.playClick()
        _isSummarizing.value = true
        _notesSummaryError.value = null

        viewModelScope.launch {
            val prefs = repository.getPreferencesSync()
            when (val result = repository.summarizeAndSaveNotes(
                title = title,
                rawNotes = rawNotes,
                subject = prefs.currentSubject,
                chapter = prefs.currentChapter
            )) {
                is ApiResult.Success -> {
                    _activeSummary.value = result.data
                    _isSummarizing.value = false
                    soundManager.playSuccess()
                }
                is ApiResult.Error -> {
                    _notesSummaryError.value = result.userFriendlyMessage
                    _isSummarizing.value = false
                }
            }
        }
    }

    fun viewNoteDetail(note: StudyNoteEntity?) {
        soundManager.playClick()
        _activeSummary.value = note
    }

    fun deleteNote(note: StudyNoteEntity) {
        soundManager.playClick()
        viewModelScope.launch {
            repository.deleteNote(note)
            if (_activeSummary.value?.id == note.id) {
                _activeSummary.value = null
            }
        }
    }

    // Study Planner & Tasks
    val activePlan = repository.activeStudyPlan.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val studyTasks = repository.allStudyTasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isPlanGenerating = MutableStateFlow(false)
    val isPlanGenerating: StateFlow<Boolean> = _isPlanGenerating.asStateFlow()

    private val _planError = MutableStateFlow<String?>(null)
    val planError: StateFlow<String?> = _planError.asStateFlow()

    fun generateStudyPlan(
        subjects: List<String>,
        targetExamDate: String,
        dailyHours: Float,
        difficultSubjects: List<String>,
        sessionDuration: Int
    ) {
        soundManager.playClick()
        _isPlanGenerating.value = true
        _planError.value = null

        viewModelScope.launch {
            when (val result = repository.generateAndSavePlan(
                subjects,
                targetExamDate,
                dailyHours,
                difficultSubjects,
                sessionDuration
            )) {
                is ApiResult.Success -> {
                    _isPlanGenerating.value = false
                    soundManager.playSuccess()
                    loadProgress()
                }
                is ApiResult.Error -> {
                    _planError.value = result.userFriendlyMessage
                    _isPlanGenerating.value = false
                }
            }
        }
    }

    fun toggleTask(task: StudyTaskEntity) {
        soundManager.playClick()
        viewModelScope.launch {
            repository.toggleTaskCompleted(task.id, !task.isCompleted)
            loadProgress()
        }
    }

    fun deleteActivePlan(planId: Long) {
        soundManager.playClick()
        viewModelScope.launch {
            repository.deletePlan(planId)
            loadProgress()
        }
    }

    // Student Progress System
    private val _progressOverview = MutableStateFlow(
        StudentProgressOverview(
            levelName = "Beginner",
            levelProgressPercent = 0.05f,
            totalQuizzes = 0,
            averageAccuracy = 0f,
            totalFlashcardsMastered = 0,
            totalFlashcards = 0,
            completedTasksCount = 0,
            totalTasksCount = 0,
            strongSubjects = emptyList(),
            weakSubjects = emptyList(),
            personalizedInsights = listOf("Start your learning journey with Lumen today.")
        )
    )
    val progressOverview: StateFlow<StudentProgressOverview> = _progressOverview.asStateFlow()

    fun loadProgress() {
        viewModelScope.launch {
            _progressOverview.value = repository.calculateProgressOverview()
        }
    }

    // Reminders
    fun triggerTestReminder() {
        soundManager.playSuccess()
        reminderManager.showStudyReminder(
            title = "Lumen Study Reminder",
            message = "Your scheduled revision session is ready! Keep your learning streak going."
        )
    }

    // Clear All Data
    fun clearAllData() {
        soundManager.playClick()
        viewModelScope.launch {
            repository.clearAllLocalData()
            _chatMessages.value = emptyList()
            _selectedConversationId.value = null
            _activeQuizQuestions.value = emptyList()
            _activeSummary.value = null
            loadProgress()
            _currentScreen.value = ScreenDestination.Onboarding
            _onboardingStep.value = 1
        }
    }

    init {
        viewModelScope.launch {
            conversations.collectLatest { list ->
                if (_selectedConversationId.value == null && list.isNotEmpty()) {
                    selectConversation(list.first().id)
                } else if (list.isEmpty() && secureStorage.hasValidApiKey()) {
                    val newId = repository.createConversation("Study Chat 1")
                    selectConversation(newId)
                }
            }
        }
        loadProgress()
    }
}
