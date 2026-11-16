package com.jarvis.assistant.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.jarvis.assistant.JarvisApplication
import java.util.Locale

class SpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isTtsReady = false
    private var recognizer: SpeechRecognizer? = null

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            // Prefer UK English for authentic JARVIS accent, fallback to US
            val localeResult = tts?.setLanguage(Locale.UK)
            if (localeResult == TextToSpeech.LANG_MISSING_DATA || localeResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
            configureMaleVoice()
            updateTtsParameters()
        }
    }

    private fun configureMaleVoice() {
        try {
            val voices = tts?.voices ?: return
            // 1. Search for UK Male voice first (canonical British JARVIS)
            val ukMale = voices.firstOrNull { v ->
                val name = v.name.lowercase(Locale.ROOT)
                v.locale.language == "en" &&
                    (name.contains("rjs") || name.contains("male") || name.contains("-m-") || name.contains("en-gb-x-gbc")) &&
                    !name.contains("female")
            }

            // 2. Fallback to any English male voice
            val anyMale = ukMale ?: voices.firstOrNull { v ->
                val name = v.name.lowercase(Locale.ROOT)
                v.locale.language == "en" &&
                    (name.contains("male") || name.contains("-m-")) &&
                    !name.contains("female")
            }

            // 3. Fallback to any non-female voice
            val finalVoice = anyMale ?: voices.firstOrNull { v ->
                val name = v.name.lowercase(Locale.ROOT)
                v.locale.language == "en" && !name.contains("female") && !name.contains("f-")
            }

            if (finalVoice != null) {
                tts?.voice = finalVoice
            }
        } catch (e: Exception) {
            // Graceful fallback
        }
    }

    fun updateTtsParameters() {
        val prefs = JarvisApplication.instance.prefs
        tts?.setSpeechRate(prefs.voiceRate)
        // Deep, refined British male baritone pitch (0.88f)
        val defaultMalePitch = 0.88f
        tts?.setPitch(if (prefs.voicePitch == 1.0f) defaultMalePitch else prefs.voicePitch)
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!isTtsReady) {
            onDone?.invoke()
            return
        }

        updateTtsParameters()
        val utteranceId = "jarvis_${System.currentTimeMillis()}"

        if (onDone != null) {
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}
                override fun onDone(id: String?) {
                    if (id == utteranceId) onDone()
                }
                override fun onError(id: String?) {
                    if (id == utteranceId) onDone()
                }
            })
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onReady: (() -> Unit)? = null
    ) {
        stopSpeaking()

        if (recognizer == null) {
            initRecognizer()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { onReady?.invoke() }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out"
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    else -> "Speech recognition error code: $error"
                }
                onError(msg)
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onResult(matches[0])
                } else {
                    onError("No matches found")
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer?.startListening(intent)
    }

    fun stopListening() {
        try {
            recognizer?.stopListening()
        } catch (_: Exception) {}
    }
}
