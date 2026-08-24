package com.example.data.sound

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.local.SecureStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SoundManager(
    private val context: Context,
    private val secureStorage: SecureStorage
) {
    private var toneGenerator: ToneGenerator? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 40)
        } catch (e: Exception) {
            toneGenerator = null
        }
    }

    private fun playTone(toneType: Int, durationMs: Int) {
        if (!secureStorage.isSoundEnabled()) return
        try {
            toneGenerator?.startTone(toneType, durationMs)
        } catch (e: Exception) {
            // Ignore audio tone exceptions
        }
    }

    private fun vibrate(durationMs: Long, amplitude: Int = 40) {
        if (!secureStorage.isHapticEnabled()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(durationMs)
                }
            }
        } catch (e: Exception) {
            // Ignore vibration exceptions
        }
    }

    fun playClick() {
        playTone(ToneGenerator.TONE_PROP_BEEP, 35)
        vibrate(15, 30)
    }

    fun playModeSwitch() {
        playTone(ToneGenerator.TONE_PROP_ACK, 50)
        vibrate(25, 45)
    }

    fun playSuccess() {
        scope.launch {
            playTone(ToneGenerator.TONE_PROP_BEEP, 40)
            delay(60)
            playTone(ToneGenerator.TONE_PROP_ACK, 70)
        }
        vibrate(35, 60)
    }

    fun playQuizAnswer(isCorrect: Boolean) {
        if (isCorrect) {
            playTone(ToneGenerator.TONE_PROP_ACK, 80)
            vibrate(30, 70)
        } else {
            playTone(ToneGenerator.TONE_PROP_NACK, 100)
            vibrate(50, 90)
        }
    }

    fun playQuizComplete() {
        scope.launch {
            playTone(ToneGenerator.TONE_PROP_BEEP, 50)
            delay(80)
            playTone(ToneGenerator.TONE_PROP_ACK, 60)
            delay(90)
            playTone(ToneGenerator.TONE_PROP_PROMPT, 100)
        }
        vibrate(60, 100)
    }

    fun playCardFlip() {
        playTone(ToneGenerator.TONE_PROP_BEEP2, 30)
        vibrate(20, 35)
    }
}
