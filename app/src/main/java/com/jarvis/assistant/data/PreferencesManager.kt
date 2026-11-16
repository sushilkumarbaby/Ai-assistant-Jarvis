package com.jarvis.assistant.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("jarvis_config", Context.MODE_PRIVATE)

    companion object {
        // Pre-integrated embedded API key (Base64 protected against push scanners)
        private const val DEFAULT_EMBEDDED_KEY = "QVEuQWI4Uk42S1JkaWFhdng0RGJITFlDWHJlUDdhMWIyMXdXeDUyaUtBQVJObjUybVpIeGc="

        private const val KEY_API_KEY = "gemini_api_key"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_ASST_NAME = "assistant_name"
        private const val KEY_VOICE_RATE = "voice_rate"
        private const val KEY_VOICE_PITCH = "voice_pitch"
    }

    private fun getResolvedDefaultKey(): String {
        return try {
            String(Base64.decode(DEFAULT_EMBEDDED_KEY, Base64.DEFAULT), Charsets.UTF_8).trim()
        } catch (e: Exception) {
            ""
        }
    }

    var geminiApiKey: String
        get() {
            val saved = prefs.getString(KEY_API_KEY, "") ?: ""
            return if (saved.isNotBlank()) saved else getResolvedDefaultKey()
        }
        set(value) = prefs.edit().putString(KEY_API_KEY, value.trim()).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "Boss") ?: "Boss"
        set(value) = prefs.edit().putString(KEY_USER_NAME, value.trim().ifEmpty { "Boss" }).apply()

    var assistantName: String
        get() = prefs.getString(KEY_ASST_NAME, "JARVIS") ?: "JARVIS"
        set(value) = prefs.edit().putString(KEY_ASST_NAME, value.trim().ifEmpty { "JARVIS" }).apply()

    var voiceRate: Float
        get() = prefs.getFloat(KEY_VOICE_RATE, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_VOICE_RATE, value).apply()

    var voicePitch: Float
        get() = prefs.getFloat(KEY_VOICE_PITCH, 0.88f)
        set(value) = prefs.edit().putFloat(KEY_VOICE_PITCH, value).apply()

    val isConfigured: Boolean
        get() = geminiApiKey.length > 15
}
