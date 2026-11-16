package com.jarvis.assistant.ai

import android.content.Context
import android.os.BatteryManager
import com.jarvis.assistant.JarvisApplication
import com.jarvis.assistant.automation.JarvisDeviceAutomator
import com.jarvis.assistant.service.JarvisAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JarvisBrain {

    private val geminiClient = GeminiApiClient()

    suspend fun processCommand(context: Context, input: String): String = withContext(Dispatchers.Default) {
        val trimmed = input.trim()
        val lower = trimmed.lowercase(Locale.ROOT)
        val prefs = JarvisApplication.instance.prefs
        val userName = prefs.userName
        val automator = JarvisDeviceAutomator(context)

        // =====================================================================
        // 1. REEL & SHORT VIDEO GESTURES (INSTANT 0-LATENCY)
        // =====================================================================
        if (lower.contains("scroll reel") || lower.contains("next reel") || lower.contains("next video") || lower.contains("reel scroll")) {
            val direction = if (lower.contains("down") || lower.contains("previous")) "down" else "up"
            return@withContext automator.scrollReels(direction)
        }

        // =====================================================================
        // 2. UI BUTTON CLICKING & TEXT TYPING ("CLICK [BUTTON]", "TYPE [TEXT]")
        // =====================================================================
        if (lower.startsWith("click ") || lower.startsWith("tap ") || lower.startsWith("press ")) {
            val target = trimmed.replaceFirst("(?i)^(click|tap|press)\\s+".toRegex(), "").trim()
            if (target.isNotBlank()) {
                return@withContext automator.clickButtonOnScreen(target)
            }
        }

        if (lower.startsWith("type ") || lower.startsWith("enter ")) {
            val textToType = trimmed.replaceFirst("(?i)^(type|enter)\\s+".toRegex(), "").trim()
            if (textToType.isNotBlank()) {
                return@withContext automator.typeTextOnScreen(textToType)
            }
        }

        // =====================================================================
        // 3. GLOBAL PHONE NAVIGATION (HOME, BACK, RECENTS, NOTIFICATIONS)
        // =====================================================================
        if (lower == "go back" || lower == "back") {
            val ok = JarvisAccessibilityService.instance?.navigateBack() ?: false
            return@withContext if (ok) "Navigated back." else "Accessibility service unavailable."
        }
        if (lower == "go home" || lower == "home screen") {
            val ok = JarvisAccessibilityService.instance?.navigateHome() ?: false
            return@withContext if (ok) "Returned to home screen." else "Accessibility service unavailable."
        }
        if (lower.contains("recent apps") || lower == "recents") {
            val ok = JarvisAccessibilityService.instance?.openRecents() ?: false
            return@withContext if (ok) "Displaying recent applications." else "Accessibility service unavailable."
        }
        if (lower.contains("notification") && (lower.contains("show") || lower.contains("open"))) {
            val ok = JarvisAccessibilityService.instance?.openNotifications() ?: false
            return@withContext if (ok) "Opening notification panel." else "Accessibility service unavailable."
        }

        // =====================================================================
        // 4. HARDWARE CONTROLS (TORCH, VOLUME, MEDIA)
        // =====================================================================
        if (lower.contains("torch on") || lower.contains("flashlight on") || lower == "turn on torch") {
            return@withContext automator.setFlashlight(true)
        }
        if (lower.contains("torch off") || lower.contains("flashlight off") || lower == "turn off torch") {
            return@withContext automator.setFlashlight(false)
        }
        if (lower.contains("volume up") || lower.contains("increase volume") || lower.contains("raise volume")) {
            return@withContext automator.adjustVolume("up")
        }
        if (lower.contains("volume down") || lower.contains("decrease volume") || lower.contains("lower volume")) {
            return@withContext automator.adjustVolume("down")
        }
        if (lower.contains("mute")) {
            return@withContext automator.adjustVolume("mute")
        }
        if (lower.contains("play music") || lower.contains("pause music") || lower == "play" || lower == "pause") {
            return@withContext automator.controlMedia("play")
        }
        if (lower.contains("next song") || lower.contains("next track")) {
            return@withContext automator.controlMedia("next")
        }

        // =====================================================================
        // 5. APPLICATION LAUNCHING & BROWSING
        // =====================================================================
        if (lower.startsWith("open ") || lower.startsWith("launch ")) {
            val target = trimmed.replaceFirst("(?i)^(open|launch)\\s+".toRegex(), "").trim()
            if (target.contains(".com") || target.contains(".org") || target.contains(".net") || target.startsWith("http")) {
                return@withContext automator.openWebsite(target)
            }
            return@withContext automator.launchApp(target)
        }

        // =====================================================================
        // 6. WHATSAPP & PHONE CALLS
        // =====================================================================
        if (lower.startsWith("call ")) {
            val target = trimmed.replaceFirst("(?i)^call\\s+".toRegex(), "").trim()
            return@withContext automator.makePhoneCall(target)
        }

        if (lower.startsWith("whatsapp ") || lower.startsWith("message ")) {
            // format: "whatsapp [contact] [message]"
            val clean = trimmed.replaceFirst("(?i)^(whatsapp|message)\\s+".toRegex(), "").trim()
            val parts = clean.split(" ", limit = 2)
            val recipient = parts[0]
            val msg = if (parts.size > 1) parts[1] else "Hello"
            return@withContext automator.sendWhatsAppMessage(recipient, msg)
        }

        // =====================================================================
        // 7. SCREEN READING (AWARENESS)
        // =====================================================================
        if (lower.contains("what is on my screen") || lower.contains("read screen") || lower.contains("screen read")) {
            val service = JarvisAccessibilityService.instance
            if (service == null) return@withContext "Accessibility Service is not enabled, $userName."
            val elements = service.inspectScreenElements()
            if (elements.isEmpty()) return@withContext "I cannot detect any readable UI elements on the current window."
            val visibleText = elements.filter { it.text.isNotBlank() }.take(8).joinToString(", ") { it.text }
            return@withContext "Screen Telemetry: Currently visible elements include: $visibleText."
        }

        // =====================================================================
        // 8. MEMORIZATION DIRECTIVES
        // =====================================================================
        if (lower.startsWith("remember that ") || lower.startsWith("remember this: ")) {
            val fact = trimmed.replaceFirst("(?i)remember (that|this:?)\\s*".toRegex(), "").trim()
            JarvisApplication.instance.memoryStorage.addMemory(fact)
            return@withContext "Acknowledged, $userName. I have committed that to my long-term memory matrix."
        }

        if (lower.contains("what do you remember") || lower.contains("recall memory") || lower.contains("show memory")) {
            val memories = JarvisApplication.instance.memoryStorage.getAllMemories()
            if (memories.isEmpty()) {
                return@withContext "My memory banks are currently empty, $userName. Say 'remember that...' to log personal data."
            }
            val list = memories.mapIndexed { idx, m -> "${idx + 1}. ${m.fact}" }.joinToString("\n")
            return@withContext "Retained Records for $userName:\n$list"
        }

        // =====================================================================
        // 9. SYSTEM DIAGNOSTICS & BRIEFING
        // =====================================================================
        if (lower.contains("diagnostic") || lower.contains("system status") || lower == "status") {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val battLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
            val memCount = JarvisApplication.instance.memoryStorage.getAllMemories().size
            val accessibilityState = if (JarvisAccessibilityService.isServiceRunning) "ACTIVE (GOD MODE)" else "OFFLINE"
            val neuralState = if (prefs.isConfigured) "ONLINE (GEMINI LIVE)" else "LOCAL ONLY"

            return@withContext "Diagnostic Report, $userName:\n" +
                "• Power Cells: $battLevel%\n" +
                "• Autopilot Engine: $accessibilityState\n" +
                "• Neural Matrix: $neuralState\n" +
                "• Retained Memories: $memCount entries\n" +
                "• System Status: All protocols operational."
        }

        if (lower.contains("briefing") || lower.contains("morning briefing")) {
            val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            val date = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val batt = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
            return@withContext "Good day, $userName. It is $time on $date. Battery stands at $batt%. All background protocols are nominal. How may I assist you today?"
        }

        // =====================================================================
        // 10. ADVANCED REASONING VIA GOOGLE GEMINI AI
        // =====================================================================
        return@withContext geminiClient.queryJarvis(trimmed)
    }
}
