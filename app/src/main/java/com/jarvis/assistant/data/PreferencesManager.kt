package com.jarvis.assistant.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("jarvis_config", Context.MODE_PRIVATE)

    var geminiApiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_KEY, value.trim()).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "Sir") ?: "Sir"
        set(value) = prefs.edit().putString(KEY_USER_NAME, value.trim().ifEmpty { "Sir" }).apply()

    var assistantName: String
        get() = prefs.getString(KEY_ASST_NAME, "JARVIS") ?: "JARVIS"
        set(value) = prefs.edit().putString(KEY_ASST_NAME, value.trim().ifEmpty { "JARVIS" }).apply()

    var voiceRate: Float
        get() = prefs.getFloat(KEY_VOICE_RATE, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_VOICE_RATE, value).apply()

    var voicePitch: Float
        get() = prefs.getFloat(KEY_VOICE_PITCH, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_VOICE_PITCH, value).apply()

    val isConfigured: Boolean
        get() = geminiApiKey.length > 15

    companion object {
        private const val KEY_API_KEY = "gemini_api_key"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_ASST_NAME = "assistant_name"
        private const val KEY_VOICE_RATE = "voice_rate"
        private const val KEY_VOICE_PITCH = "voice_pitch"
    }
}
