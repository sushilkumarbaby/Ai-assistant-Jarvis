package com.jarvis.assistant

import android.app.Application
import com.jarvis.assistant.ai.SpeechManager
import com.jarvis.assistant.data.MemoryStorage
import com.jarvis.assistant.data.PreferencesManager

class JarvisApplication : Application() {

    lateinit var prefs: PreferencesManager
        private set

    lateinit var memoryStorage: MemoryStorage
        private set

    lateinit var speechManager: SpeechManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = PreferencesManager(this)
        memoryStorage = MemoryStorage(this)
        speechManager = SpeechManager(this)
    }

    companion object {
        lateinit var instance: JarvisApplication
            private set
    }
}
