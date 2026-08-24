package com.example.data.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.local.SecureStorage
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val userFriendlyMessage: String, val isNetworkError: Boolean = false) : ApiResult<Nothing>()
}

class GeminiClient(
    private val context: Context,
    private val secureStorage: SecureStorage
) {
    companion object {
        private const val MODEL = "gemini-3.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        const val SYSTEM_INSTRUCTION_LUMEN = """
You are Lumen, a modern, intelligent, friendly, and student-focused AI learning companion created by developer Rauf.
Your mission is to help students learn with clarity, confidence, depth, and genuine understanding.

Key Guidelines:
1. Student-Centric: Provide clear, structured, and easy-to-digest explanations. Break complex concepts into intuitive analogies, step-by-step logic, and high-yield exam insights.
2. Tone: Confident, encouraging, friendly, lightly witty when appropriate, never robotic or bureaucratic.
3. Concise & Direct: Answer the question immediately without generic filler ("How can I help you?", "I am ready to assist").
4. Multilingual: Naturally comprehend and answer in English, Hindi, or Hinglish depending on how the student asks.
5. Study Structure for Academic Queries:
   - Direct clear answer / concept summary
   - Simple intuitive explanation / real-world example
   - Key formulas, rules, or definitions (if applicable)
   - High-yield exam focus / common mistakes to avoid
6. Text-Only: Respond purely in rich, well-formatted plain text and markdown. Never mention audio, video, or external links.
"""
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun validateApiKey(keyToTest: String): ApiResult<Boolean> = withContext(Dispatchers.IO) {
        val cleanKey = keyToTest.trim()
        if (cleanKey.isBlank()) {
            return@withContext ApiResult.Error("Please enter a valid Gemini API key.")
        }
        if (!isOnline()) {
            return@withContext ApiResult.Error("Please check your internet connection and try again.", isNetworkError = true)
        }

        val requestUrl = "$BASE_URL/$MODEL:generateContent?key=$cleanKey"
        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", "Ping") })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("maxOutputTokens", 5)
            })
        }.toString()

        val request = Request.Builder()
            .url(requestUrl)
            .post(requestJson.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    ApiResult.Success(true)
                } else {
                    val code = response.code
                    if (code == 400 || code == 401 || code == 403) {
                        ApiResult.Error("This API key couldn't be verified. Please check it and try again.")
                    } else if (code == 429) {
                        ApiResult.Error("API quota exceeded or rate limit reached. Please try again in a few moments.")
                    } else {
                        ApiResult.Error("The AI service is temporarily unavailable. Please try again later.")
                    }
                }
            }
        } catch (e: IOException) {
            ApiResult.Error("Please check your internet connection and try again.", isNetworkError = true)
        } catch (e: Exception) {
            ApiResult.Error("The AI service is temporarily unavailable. Please try again later.")
        }
    }

    suspend fun generateContent(
        prompt: String,
        history: List<Pair<String, String>> = emptyList(),
        customSystemInstruction: String = SYSTEM_INSTRUCTION_LUMEN
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        val apiKey = secureStorage.getApiKey()
        if (apiKey.isBlank()) {
            return@withContext ApiResult.Error("No Gemini API key found. Please connect your API key in Settings.")
        }
        if (!isOnline()) {
            return@withContext ApiResult.Error("Please check your internet connection and try again.", isNetworkError = true)
        }

        val requestUrl = "$BASE_URL/$MODEL:generateContent?key=$apiKey"

        val contentsArray = JSONArray()
        // Add conversation history
        for ((role, text) in history) {
            val apiRole = if (role.lowercase() == "user") "user" else "model"
            contentsArray.put(JSONObject().apply {
                put("role", apiRole)
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", text) })
                })
            })
        }
        // Add current prompt
        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", prompt) })
            })
        })

        val requestPayload = JSONObject().apply {
            put("contents", contentsArray)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", customSystemInstruction) })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("topP", 0.95)
                put("maxOutputTokens", 4096)
            })
        }.toString()

        val request = Request.Builder()
            .url(requestUrl)
            .post(requestPayload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val rootJson = JSONObject(bodyString)
                    val candidates = rootJson.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val text = parts.getJSONObject(0).optString("text", "")
                            if (text.isNotBlank()) {
                                return@withContext ApiResult.Success(text.trim())
                            }
                        }
                    }
                    ApiResult.Error("No response received. Please try asking again.")
                } else {
                    handleHttpError(response.code)
                }
            }
        } catch (e: IOException) {
            ApiResult.Error("Please check your internet connection and try again.", isNetworkError = true)
        } catch (e: Exception) {
            ApiResult.Error("Something went wrong. Please try again.")
        }
    }

    fun streamGenerateContent(
        prompt: String,
        history: List<Pair<String, String>> = emptyList(),
        customSystemInstruction: String = SYSTEM_INSTRUCTION_LUMEN
    ): Flow<String> = flow {
        val apiKey = secureStorage.getApiKey()
        if (apiKey.isBlank()) {
            emit("Error: No Gemini API key found. Please connect your API key in Settings.")
            return@flow
        }
        if (!isOnline()) {
            emit("Error: Please check your internet connection and try again.")
            return@flow
        }

        val requestUrl = "$BASE_URL/$MODEL:streamGenerateContent?alt=sse&key=$apiKey"

        val contentsArray = JSONArray()
        for ((role, text) in history) {
            val apiRole = if (role.lowercase() == "user") "user" else "model"
            contentsArray.put(JSONObject().apply {
                put("role", apiRole)
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", text) })
                })
            })
        }
        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", prompt) })
            })
        })

        val requestPayload = JSONObject().apply {
            put("contents", contentsArray)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", customSystemInstruction) })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("topP", 0.95)
                put("maxOutputTokens", 4096)
            })
        }.toString()

        val request = Request.Builder()
            .url(requestUrl)
            .post(requestPayload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        var callSuccess = false
        try {
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorMsg = when (response.code) {
                    400, 401, 403 -> "This API key couldn't be verified. Please check it and try again."
                    429 -> "Rate limit reached. Please wait a moment."
                    else -> "The AI service is temporarily unavailable. Please try again later."
                }
                emit("Error: $errorMsg")
                response.close()
                return@flow
            }

            val source = response.body?.byteStream()
            if (source != null) {
                source.bufferedReader().use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val currentLine = line?.trim() ?: continue
                        if (currentLine.startsWith("data:")) {
                            val dataJsonStr = currentLine.removePrefix("data:").trim()
                            if (dataJsonStr.isNotBlank() && dataJsonStr != "[DONE]") {
                                try {
                                    val obj = JSONObject(dataJsonStr)
                                    val candidates = obj.optJSONArray("candidates")
                                    if (candidates != null && candidates.length() > 0) {
                                        val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
                                        if (parts != null && parts.length() > 0) {
                                            val chunk = parts.getJSONObject(0).optString("text", "")
                                            if (chunk.isNotEmpty()) {
                                                emit(chunk)
                                                callSuccess = true
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    // ignore partial parse errors in SSE
                                }
                            }
                        }
                    }
                }
            }
            response.close()
        } catch (e: IOException) {
            if (!callSuccess) {
                emit("Error: Please check your internet connection and try again.")
            }
        } catch (e: Exception) {
            if (!callSuccess) {
                emit("Error: Something went wrong. Please try again.")
            }
        }
    }.flowOn(Dispatchers.IO)

    // Dedicated Structured Generators
    suspend fun generateQuiz(
        subject: String,
        chapter: String,
        educationLevel: String,
        classYear: String,
        board: String,
        difficulty: String = "Medium",
        questionCount: Int = 5
    ): ApiResult<List<GeneratedQuizQuestion>> = withContext(Dispatchers.IO) {
        val prompt = """
Generate an academic quiz with exactly $questionCount multiple-choice questions for:
- Education Level: $educationLevel
- Class/Year: $classYear
- Board/University: $board
- Subject: $subject
- Chapter/Topic: $chapter
- Difficulty: $difficulty

Return ONLY a valid JSON array of objects. Do not include markdown code block quotes (like ```json). Each object must have this exact structure:
[
  {
    "question": "Question text here?",
    "options": ["Option A", "Option B", "Option C", "Option D"],
    "correctIndex": 0,
    "explanation": "Detailed step-by-step reasoning for why this answer is correct.",
    "topicTag": "Specific sub-topic"
  }
]
"""
        when (val result = generateContent(prompt, customSystemInstruction = "You are a professional exam setter for $board $classYear. Return pure JSON array only.")) {
            is ApiResult.Success -> {
                try {
                    val cleanJson = result.data.trim()
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()
                    val array = JSONArray(cleanJson)
                    val questions = mutableListOf<GeneratedQuizQuestion>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val optionsArray = obj.getJSONArray("options")
                        val options = mutableListOf<String>()
                        for (j in 0 until optionsArray.length()) {
                            options.add(optionsArray.getString(j))
                        }
                        questions.add(
                            GeneratedQuizQuestion(
                                question = obj.getString("question"),
                                options = options,
                                correctIndex = obj.getInt("correctIndex"),
                                explanation = obj.getString("explanation"),
                                topicTag = obj.optString("topicTag", subject)
                            )
                        )
                    }
                    if (questions.isNotEmpty()) {
                        ApiResult.Success(questions)
                    } else {
                        ApiResult.Error("Could not parse quiz questions. Please try again.")
                    }
                } catch (e: Exception) {
                    ApiResult.Error("Error parsing quiz. Please retry generating.")
                }
            }
            is ApiResult.Error -> result
        }
    }

    suspend fun generateFlashcards(
        subject: String,
        chapter: String,
        contentOrNotes: String,
        cardCount: Int = 6
    ): ApiResult<List<GeneratedFlashcard>> = withContext(Dispatchers.IO) {
        val prompt = """
Generate $cardCount high-yield study flashcards based on:
- Subject: $subject
- Chapter: $chapter
- Source Material / Topic Notes: $contentOrNotes

Return ONLY a valid JSON array of objects without markdown fences. Format:
[
  {
    "question": "Core question or definition term?",
    "answer": "Clear, memorable, high-yield answer or formula.",
    "difficulty": "Medium"
  }
]
"""
        when (val result = generateContent(prompt, customSystemInstruction = "You are an expert tutor creating memory-retention flashcards. Return pure JSON array only.")) {
            is ApiResult.Success -> {
                try {
                    val cleanJson = result.data.trim()
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()
                    val array = JSONArray(cleanJson)
                    val flashcards = mutableListOf<GeneratedFlashcard>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        flashcards.add(
                            GeneratedFlashcard(
                                question = obj.getString("question"),
                                answer = obj.getString("answer"),
                                difficulty = obj.optString("difficulty", "Medium")
                            )
                        )
                    }
                    if (flashcards.isNotEmpty()) {
                        ApiResult.Success(flashcards)
                    } else {
                        ApiResult.Error("No flashcards could be generated. Please try again.")
                    }
                } catch (e: Exception) {
                    ApiResult.Error("Could not parse flashcards. Please retry.")
                }
            }
            is ApiResult.Error -> result
        }
    }

    suspend fun summarizeNotes(
        notesText: String,
        subject: String = "General"
    ): ApiResult<GeneratedNotesSummary> = withContext(Dispatchers.IO) {
        val prompt = """
Analyze and summarize the following study notes for Subject: $subject.

Source Notes:
$notesText

Return ONLY a valid JSON object without markdown fences matching:
{
  "title": "Concise Descriptive Title",
  "shortSummary": "2-3 sentence high-level overview",
  "detailedSummary": "Comprehensive conceptual breakdown",
  "keyPoints": ["Key point 1", "Key point 2", "Key point 3", "Key point 4"],
  "definitions": [{"term": "Term", "meaning": "Definition"}],
  "revisionTips": "Crucial exam recall tips and mnemonics"
}
"""
        when (val result = generateContent(prompt, customSystemInstruction = "You are a master study notes summarizer. Return pure JSON object only.")) {
            is ApiResult.Success -> {
                try {
                    val cleanJson = result.data.trim()
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()
                    val obj = JSONObject(cleanJson)
                    val keyPoints = mutableListOf<String>()
                    val kpArray = obj.optJSONArray("keyPoints")
                    if (kpArray != null) {
                        for (i in 0 until kpArray.length()) {
                            keyPoints.add(kpArray.getString(i))
                        }
                    }
                    val definitions = mutableListOf<Pair<String, String>>()
                    val defArray = obj.optJSONArray("definitions")
                    if (defArray != null) {
                        for (i in 0 until defArray.length()) {
                            val defObj = defArray.getJSONObject(i)
                            definitions.add(Pair(defObj.optString("term", ""), defObj.optString("meaning", "")))
                        }
                    }
                    ApiResult.Success(
                        GeneratedNotesSummary(
                            title = obj.optString("title", "Study Summary"),
                            shortSummary = obj.optString("shortSummary", ""),
                            detailedSummary = obj.optString("detailedSummary", ""),
                            keyPoints = keyPoints,
                            definitions = definitions,
                            revisionTips = obj.optString("revisionTips", "")
                        )
                    )
                } catch (e: Exception) {
                    ApiResult.Error("Could not parse summarized notes. Please try again.")
                }
            }
            is ApiResult.Error -> result
        }
    }

    suspend fun generateStudyPlan(
        subjects: List<String>,
        targetExamDate: String,
        dailyHours: Float,
        difficultSubjects: List<String>,
        sessionDuration: Int
    ): ApiResult<GeneratedStudyPlan> = withContext(Dispatchers.IO) {
        val prompt = """
Create a realistic, balanced study plan for a student with:
- Subjects: ${subjects.joinToString(", ")}
- Target Exam Date: $targetExamDate
- Daily Study Time Available: $dailyHours hours
- Difficult/Weak Subjects (need higher priority): ${difficultSubjects.joinToString(", ")}
- Preferred Session Duration: $sessionDuration minutes

Generate a 7-day initial roadmap with daily priority tasks, planned revision slots, and break periods. Avoid unrealistic overloading.

Return ONLY a valid JSON object without markdown fences:
{
  "planTitle": "Strategic Exam Roadmap",
  "overview": "Realistic advice on how to pace these subjects.",
  "dailyTasks": [
    {
      "dayNumber": 1,
      "taskTitle": "Study Electrostatics Coulombs Law",
      "subject": "Physics",
      "durationMinutes": 45,
      "isRevision": false
    },
    {
      "dayNumber": 1,
      "taskTitle": "Revise Organic Chemistry Nomenclature",
      "subject": "Chemistry",
      "durationMinutes": 30,
      "isRevision": true
    }
  ]
}
"""
        when (val result = generateContent(prompt, customSystemInstruction = "You are an expert academic counselor and study planner. Return pure JSON object only.")) {
            is ApiResult.Success -> {
                try {
                    val cleanJson = result.data.trim()
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()
                    val obj = JSONObject(cleanJson)
                    val tasks = mutableListOf<GeneratedDailyTask>()
                    val tasksArray = obj.optJSONArray("dailyTasks")
                    if (tasksArray != null) {
                        for (i in 0 until tasksArray.length()) {
                            val taskObj = tasksArray.getJSONObject(i)
                            tasks.add(
                                GeneratedDailyTask(
                                    dayNumber = taskObj.optInt("dayNumber", 1),
                                    taskTitle = taskObj.optString("taskTitle", ""),
                                    subject = taskObj.optString("subject", "General"),
                                    durationMinutes = taskObj.optInt("durationMinutes", 45),
                                    isRevision = taskObj.optBoolean("isRevision", false)
                                )
                            )
                        }
                    }
                    ApiResult.Success(
                        GeneratedStudyPlan(
                            planTitle = obj.optString("planTitle", "Custom Study Plan"),
                            overview = obj.optString("overview", ""),
                            tasks = tasks
                        )
                    )
                } catch (e: Exception) {
                    ApiResult.Error("Could not parse study plan. Please retry.")
                }
            }
            is ApiResult.Error -> result
        }
    }

    private fun handleHttpError(code: Int): ApiResult<Nothing> {
        return when (code) {
            400, 401, 403 -> ApiResult.Error("This API key couldn't be verified. Please check it and try again.")
            429 -> ApiResult.Error("API quota exceeded or rate limit reached. Please try again in a few moments.")
            500, 502, 503, 504 -> ApiResult.Error("The AI service is temporarily unavailable. Please try again later.")
            else -> ApiResult.Error("Something went wrong. Please try again.")
        }
    }
}

data class GeneratedQuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val topicTag: String
)

data class GeneratedFlashcard(
    val question: String,
    val answer: String,
    val difficulty: String
)

data class GeneratedNotesSummary(
    val title: String,
    val shortSummary: String,
    val detailedSummary: String,
    val keyPoints: List<String>,
    val definitions: List<Pair<String, String>>,
    val revisionTips: String
)

data class GeneratedDailyTask(
    val dayNumber: Int,
    val taskTitle: String,
    val subject: String,
    val durationMinutes: Int,
    val isRevision: Boolean
)

data class GeneratedStudyPlan(
    val planTitle: String,
    val overview: String,
    val tasks: List<GeneratedDailyTask>
)
