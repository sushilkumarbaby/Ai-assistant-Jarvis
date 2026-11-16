package com.jarvis.assistant.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import com.jarvis.assistant.ui.AssistOverlayActivity

class JarvisVoiceSession(context: Context) : VoiceInteractionSession(context) {

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)

        // Launch the floating assistant overlay over current screen
        val intent = Intent(context, AssistOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("from_voice_session", true)
        }
        context.startActivity(intent)

        // Hide system session view since AssistOverlayActivity handles the HUD
        hide()
    }
}
