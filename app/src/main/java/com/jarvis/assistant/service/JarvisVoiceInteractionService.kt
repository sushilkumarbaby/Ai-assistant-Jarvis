package com.jarvis.assistant.service

import android.service.voice.VoiceInteractionService

class JarvisVoiceInteractionService : VoiceInteractionService() {

    override fun onReady() {
        super.onReady()
        // Called when the service is bound by the Android system as the active assistant
    }

    override fun onShutdown() {
        super.onShutdown()
    }
}
