package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SecureStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("lumen_secure_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_API_KEY = "user_gemini_api_key"
        private const val KEY_IS_ONBOARDED = "is_onboarded"
        private const val KEY_THEME_MODE = "theme_mode" // "system", "dark", "light"
        private const val KEY_ACCENT_COLOR = "accent_color" // "gold", "indigo", "emerald", "rose"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_HAPTIC_ENABLED = "haptic_enabled"
        private const val KEY_STUDENT_NAME = "student_name"
    }

    private val _themeModeFlow = MutableStateFlow(getThemeMode())
    val themeModeFlow: StateFlow<String> = _themeModeFlow.asStateFlow()

    private val _accentColorFlow = MutableStateFlow(getAccentColor())
    val accentColorFlow: StateFlow<String> = _accentColorFlow.asStateFlow()

    private val _soundEnabledFlow = MutableStateFlow(isSoundEnabled())
    val soundEnabledFlow: StateFlow<Boolean> = _soundEnabledFlow.asStateFlow()

    private val _hasApiKeyFlow = MutableStateFlow(hasValidApiKey())
    val hasApiKeyFlow: StateFlow<Boolean> = _hasApiKeyFlow.asStateFlow()

    fun getApiKey(): String {
        val userKey = prefs.getString(KEY_USER_API_KEY, "") ?: ""
        if (userKey.isNotBlank()) return userKey.trim()
        val buildKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") return buildKey.trim()
        return ""
    }

    fun saveApiKey(apiKey: String) {
        prefs.edit().putString(KEY_USER_API_KEY, apiKey.trim()).apply()
        _hasApiKeyFlow.value = hasValidApiKey()
    }

    fun removeApiKey() {
        prefs.edit().remove(KEY_USER_API_KEY).apply()
        _hasApiKeyFlow.value = hasValidApiKey()
    }

    fun hasValidApiKey(): Boolean {
        return getApiKey().isNotBlank()
    }

    fun isOnboarded(): Boolean {
        return prefs.getBoolean(KEY_IS_ONBOARDED, false) && hasValidApiKey()
    }

    fun setOnboarded(onboarded: Boolean) {
        prefs.edit().putBoolean(KEY_IS_ONBOARDED, onboarded).apply()
    }

    fun getStudentName(): String {
        return prefs.getString(KEY_STUDENT_NAME, "Student") ?: "Student"
    }

    fun saveStudentName(name: String) {
        prefs.edit().putString(KEY_STUDENT_NAME, name.trim().ifBlank { "Student" }).apply()
    }

    fun getThemeMode(): String {
        return prefs.getString(KEY_THEME_MODE, "dark") ?: "dark"
    }

    fun saveThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        _themeModeFlow.value = mode
    }

    fun getAccentColor(): String {
        return prefs.getString(KEY_ACCENT_COLOR, "gold") ?: "gold"
    }

    fun saveAccentColor(color: String) {
        prefs.edit().putString(KEY_ACCENT_COLOR, color).apply()
        _accentColorFlow.value = color
    }

    fun isSoundEnabled(): Boolean {
        return prefs.getBoolean(KEY_SOUND_ENABLED, true)
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
        _soundEnabledFlow.value = enabled
    }

    fun isHapticEnabled(): Boolean {
        return prefs.getBoolean(KEY_HAPTIC_ENABLED, true)
    }

    fun setHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, enabled).apply()
    }

    fun resetAppearanceToDefault() {
        saveThemeMode("dark")
        saveAccentColor("gold")
        setSoundEnabled(true)
        setHapticEnabled(true)
    }

    fun clearAllData() {
        prefs.edit()
            .remove(KEY_IS_ONBOARDED)
            .remove(KEY_THEME_MODE)
            .remove(KEY_ACCENT_COLOR)
            .remove(KEY_STUDENT_NAME)
            .apply()
    }
}
