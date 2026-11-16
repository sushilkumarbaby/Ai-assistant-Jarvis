package com.jarvis.assistant.ai

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.jarvis.assistant.JarvisApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class GeminiApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun queryJarvis(userPrompt: String): String = withContext(Dispatchers.IO) {
        val prefs = JarvisApplication.instance.prefs
        val apiKey = prefs.geminiApiKey

        if (apiKey.isBlank()) {
            return@withContext "My apologies, ${prefs.userName}. To access my neural core, please enter your Gemini API key in Settings."
        }

        // Build context with stored memories
        val memories = JarvisApplication.instance.memoryStorage.getAllMemories()
        val memoryBlock = if (memories.isNotEmpty()) {
            "\nLONG-TERM FACTS YOU REMEMBER ABOUT THE USER:\n" +
                memories.joinToString("\n") { "• ${it.fact}" }
        } else ""

        val dateStr = SimpleDateFormat("EEEE, MMMM d, yyyy h:mm a", Locale.getDefault()).format(Date())

        val systemPrompt = """
            You are J.A.R.V.I.S. (Just A Rather Very Intelligent System), the iconic personal AI assistant originally designed by Tony Stark.
            You speak with a calm, refined, ultra-polite, British-inspired cadence.
            Address the user respectfully as '${prefs.userName}'.
            Be analytical, concise, witty when appropriate, and highly competent.
            Keep responses crisp and natural (under 3 to 4 sentences whenever possible) because your answer will be synthesized as spoken voice output.
            Never include markdown formatting like asterisks or hashtags that sound strange when read aloud.
            Current date and time: $dateStr.$memoryBlock
        """.trimIndent()

        // JSON payload
        val requestJson = JsonObject().apply {
            val contentsArray = com.google.gson.JsonArray().apply {
                val turn = JsonObject().apply {
                    addProperty("role", "user")
                    val parts = com.google.gson.JsonArray().apply {
                        val part = JsonObject().apply {
                            addProperty("text", "$systemPrompt\n\nUSER DIRECTIVE: $userPrompt")
                        }
                        add(part)
                    }
                    add("parts", parts)
                }
                add(turn)
            }
            add("contents", contentsArray)

            val genConfig = JsonObject().apply {
                addProperty("temperature", 0.7)
                addProperty("maxOutputTokens", 250)
            }
            add("generationConfig", genConfig)
        }

        val models = listOf("gemini-2.0-flash", "gemini-1.5-flash", "gemini-1.5-flash-8b")
        var lastException: Exception? = null

        for (model in models) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val body = requestJson.toString().toRequestBody(jsonMediaType)
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string().orEmpty()

                if (response.isSuccessful) {
                    val root = gson.fromJson(responseBody, JsonObject::class.java)
                    val candidates = root.getAsJsonArray("candidates")
                    if (candidates != null && candidates.size() > 0) {
                        val firstCandidate = candidates.get(0).asJsonObject
                        val content = firstCandidate.getAsJsonObject("content")
                        val parts = content.getAsJsonArray("parts")
                        if (parts != null && parts.size() > 0) {
                            val replyText = parts.get(0).asJsonObject.get("text").asString
                            return@withContext replyText.trim()
                        }
                    }
                } else {
                    lastException = Exception("Gemini API Error (HTTP ${response.code}): $responseBody")
                }
            } catch (e: Exception) {
                lastException = e
            }
        }

        return@withContext "I encountered a communication error with the neural network, ${prefs.userName}: ${lastException?.message ?: "Request failed"}."
    }
}
