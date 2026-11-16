package com.jarvis.assistant.automation

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import com.jarvis.assistant.service.JarvisAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.URLEncoder

/**
 * High-Level Android Automation Controller.
 * Executes full device actions requested by user or by Gemini AI.
 */
class JarvisDeviceAutomator(private val context: Context) {

    companion object {
        private const val TAG = "JarvisDeviceAutomator"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    // =========================================================================
    // 1. APP LAUNCHING & BROWSER NAVIGATION
    // =========================================================================

    /**
     * Finds and launches any app by common name (e.g. "Instagram", "WhatsApp", "Chrome").
     */
    fun launchApp(targetName: String): String {
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        // Known direct shortcuts
        val alias = when (targetName.lowercase().trim()) {
            "instagram", "insta" -> "com.instagram.android"
            "whatsapp", "wa" -> "com.whatsapp"
            "youtube", "yt" -> "com.google.android.youtube"
            "chrome", "browser" -> "com.android.chrome"
            "camera" -> "com.google.android.GoogleCamera"
            "settings" -> "com.android.settings"
            "spotify" -> "com.spotify.music"
            "telegram" -> "org.telegram.messenger"
            else -> null
        }

        if (alias != null) {
            val intent = pm.getLaunchIntentForPackage(alias)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return "Launched $targetName successfully."
            }
        }

        // Fuzzy search by application label
        for (app in installedApps) {
            val label = pm.getApplicationLabel(app).toString()
            if (label.contains(targetName, ignoreCase = true)) {
                val intent = pm.getLaunchIntentForPackage(app.packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return "Opened $label, Sir."
                }
            }
        }
        return "I could not locate an installed app named '$targetName' on this device."
    }

    /**
     * Opens Chrome or default browser to a designated URL or search query.
     */
    fun openWebsite(queryOrUrl: String): String {
        var url = queryOrUrl.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = if (url.contains(".") && !url.contains(" ")) {
                "https://$url"
            } else {
                "https://www.google.com/search?q=" + URLEncoder.encode(url, "UTF-8")
            }
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "Navigating to $url."
    }

    // =========================================================================
    // 2. WHATSAPP & CALLING AUTOMATION
    // =========================================================================

    /**
     * Autonomously sends a WhatsApp message to a phone number or prepared contact.
     */
    suspend fun sendWhatsAppMessage(recipient: String, message: String): String = withContext(Dispatchers.Main) {
        val cleanPhone = recipient.replace("[^0-9+]".toRegex(), "")
        val encodedMsg = URLEncoder.encode(message, "UTF-8")

        val intent = if (cleanPhone.length >= 10) {
            val formatted = if (cleanPhone.startsWith("+")) cleanPhone.substring(1) else cleanPhone
            Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$formatted&text=$encodedMsg"))
        } else {
            // General share intent to WhatsApp
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_TEXT, message)
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)

        // Give WhatsApp 1.5 seconds to open, then trigger auto-send click via Accessibility
        delay(1500)
        val accessibility = JarvisAccessibilityService.instance
        if (accessibility != null) {
            // WhatsApp send button ID or text
            val sent = accessibility.clickById("com.whatsapp:id/send") || accessibility.clickByText("Send", exact = true)
            if (sent) return@withContext "WhatsApp message sent to $recipient."
        }
        return@withContext "WhatsApp opened with message for $recipient. Awaiting final send tap if accessibility is pending."
    }

    /**
     * Dials a contact or number.
     */
    fun makePhoneCall(number: String): String {
        val clean = number.replace("[^0-9+]".toRegex(), "")
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "Opening dialer for $number."
    }

    // =========================================================================
    // 3. REELS & LIVE SCREEN GESTURES
    // =========================================================================

    fun scrollReels(direction: String = "up"): String {
        val service = JarvisAccessibilityService.instance
            ?: return "Accessibility Service is not active. Please enable JARVIS under Settings → Accessibility."

        service.scrollReel(direction)
        return "Scrolled reel $direction."
    }

    fun clickButtonOnScreen(buttonText: String): String {
        val service = JarvisAccessibilityService.instance
            ?: return "Accessibility Service is not active. Please enable JARVIS under Settings → Accessibility."

        val clicked = service.clickByText(buttonText)
        return if (clicked) "Clicked '$buttonText' on screen." else "Could not locate '$buttonText' on active screen."
    }

    fun typeTextOnScreen(text: String, targetFieldHint: String? = null): String {
        val service = JarvisAccessibilityService.instance
            ?: return "Accessibility Service is not active. Please enable JARVIS under Settings → Accessibility."

        val typed = service.enterText(text, targetFieldHint)
        return if (typed) "Entered text successfully." else "Failed to find editable field on screen."
    }

    // =========================================================================
    // 4. HARDWARE & SYSTEM CONTROLS
    // =========================================================================

    /**
     * Toggles flashlight / torch on or off.
     */
    fun setFlashlight(enable: Boolean): String {
        return try {
            val cm = cameraManager ?: return "Camera hardware unavailable."
            val cameraId = cm.cameraIdList.firstOrNull() ?: return "No camera available for flashlight."
            cm.setTorchMode(cameraId, enable)
            if (enable) "Flashlight illuminated." else "Flashlight extinguished."
        } catch (e: Exception) {
            "Unable to modify flashlight state: ${e.message}"
        }
    }

    /**
     * Controls audio volume (raise, lower, set level 0-100).
     */
    fun adjustVolume(action: String, percentage: Int? = null): String {
        val am = audioManager ?: return "Audio system unavailable."
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        return when (action.lowercase()) {
            "up", "raise", "increase" -> {
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                "Volume increased."
            }
            "down", "lower", "decrease" -> {
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                "Volume decreased."
            }
            "set" -> {
                val pct = percentage?.coerceIn(0, 100) ?: 50
                val targetVol = (max * (pct / 100f)).toInt()
                am.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, AudioManager.FLAG_SHOW_UI)
                "Volume calibrated to $pct%."
            }
            "mute" -> {
                am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
                "System media muted."
            }
            else -> "Unrecognized volume action."
        }
    }

    /**
     * Media play, pause, next track, previous track.
     */
    fun controlMedia(command: String): String {
        val am = audioManager ?: return "Media system unavailable."
        val keyCode = when (command.lowercase()) {
            "play", "pause", "toggle" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            "next", "skip" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "prev", "previous", "back" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            "stop" -> KeyEvent.KEYCODE_MEDIA_STOP
            else -> null
        } ?: return "Unsupported media command '$command'."

        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val upEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        am.dispatchMediaKeyEvent(downEvent)
        am.dispatchMediaKeyEvent(upEvent)
        return "Media command '$command' dispatched."
    }
}
