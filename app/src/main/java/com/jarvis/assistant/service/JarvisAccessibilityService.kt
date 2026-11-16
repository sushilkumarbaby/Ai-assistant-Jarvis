package com.jarvis.assistant.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Data class representing an interactive element extracted from the active screen.
 */
data class ScreenElement(
    val text: String,
    val viewId: String?,
    val contentDescription: String?,
    val className: String,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val bounds: Rect
)

/**
 * JARVIS God-Mode Accessibility Service.
 * Grants autonomous screen interaction, including gesture-based reel scrolling,
 * clicking buttons by text or ID, typing into input boxes, and reading UI hierarchy.
 */
class JarvisAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "JarvisAccessibility"

        @Volatile
        var instance: JarvisAccessibilityService? = null
            private set

        val isServiceRunning: Boolean
            get() = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "⚡ JARVIS God-Mode Accessibility Service connected successfully.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Active event stream available for real-time window tracking
    }

    override fun onInterrupt() {
        Log.w(TAG, "⚠️ JARVIS Accessibility Service interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        Log.i(TAG, "JARVIS Accessibility Service destroyed.")
    }

    // =========================================================================
    // 1. REEL & SHORT VIDEO SCROLLING (INSTAGRAM / YOUTUBE SHORTS / TIKTOK)
    // =========================================================================

    /**
     * Dispatches a flick gesture to scroll to the next or previous reel.
     * @param direction "up" (next reel) or "down" (previous reel).
     */
    fun scrollReel(direction: String = "up", onComplete: ((Boolean) -> Unit)? = null) {
        val dm = resources.displayMetrics
        val width = dm.widthPixels.toFloat()
        val height = dm.heightPixels.toFloat()

        val centerX = width / 2f
        val startY: Float
        val endY: Float

        if (direction.lowercase() == "down" || direction.lowercase() == "previous") {
            // Scroll down: flick from top to bottom (shows previous video)
            startY = height * 0.22f
            endY = height * 0.78f
        } else {
            // Scroll up: flick from bottom to top (shows next video)
            startY = height * 0.80f
            endY = height * 0.20f
        }

        val path = Path().apply {
            moveTo(centerX, startY)
            lineTo(centerX, endY)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 220))
            .build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.d(TAG, "Reel scroll gesture ($direction) executed.")
                onComplete?.invoke(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.w(TAG, "Reel scroll gesture cancelled.")
                onComplete?.invoke(false)
            }
        }, null)
    }

    // =========================================================================
    // 2. COORDINATE TAP & CUSTOM SWIPES
    // =========================================================================

    /**
     * Taps a precise coordinate on screen (e.g., predicted by Gemini Vision).
     */
    fun tapCoordinates(x: Float, y: Float, onComplete: ((Boolean) -> Unit)? = null) {
        val path = Path().apply {
            moveTo(x, y)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 60))
            .build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.d(TAG, "Tapped coordinates ($x, $y).")
                onComplete?.invoke(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                onComplete?.invoke(false)
            }
        }, null)
    }

    /**
     * Smooth swipe gesture between two points.
     */
    fun swipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 300, onComplete: ((Boolean) -> Unit)? = null) {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                onComplete?.invoke(true)
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                onComplete?.invoke(false)
            }
        }, null)
    }

    // =========================================================================
    // 3. SMART UI CLICKING (BY TEXT, RESOURCE ID, OR DESCRIPTION)
    // =========================================================================

    /**
     * Finds and clicks any interactive UI button matching target text.
     */
    fun clickByText(targetText: String, exact: Boolean = false): Boolean {
        val root = rootInActiveWindow ?: return false
        val found = searchNode(root) { node ->
            val text = node.text?.toString() ?: ""
            val desc = node.contentDescription?.toString() ?: ""
            if (exact) {
                text.equals(targetText, ignoreCase = true) || desc.equals(targetText, ignoreCase = true)
            } else {
                text.contains(targetText, ignoreCase = true) || desc.contains(targetText, ignoreCase = true)
            }
        }

        if (found != null) {
            val success = performClickActionOnNodeOrParent(found)
            found.recycle()
            return success
        }
        return false
    }

    /**
     * Finds and clicks an element by its Android viewId (e.g., "com.whatsapp:id/send_button").
     */
    fun clickById(viewId: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
        if (!nodes.isNullOrEmpty()) {
            val node = nodes[0]
            val success = performClickActionOnNodeOrParent(node)
            for (n in nodes) n.recycle()
            return success
        }
        return false
    }

    private fun performClickActionOnNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
        }
        // Fallback: click coordinates of bounds
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (!bounds.isEmpty) {
            tapCoordinates(bounds.centerX().toFloat(), bounds.centerY().toFloat())
            return true
        }
        return false
    }

    // =========================================================================
    // 4. AUTONOMOUS FORM FILLING & TYPING
    // =========================================================================

    /**
     * Enters text into an active or targeted text box on screen.
     */
    fun enterText(text: String, targetHintOrId: String? = null): Boolean {
        val root = rootInActiveWindow ?: return false

        val targetNode: AccessibilityNodeInfo? = if (!targetHintOrId.isNullOrBlank()) {
            searchNode(root) { node ->
                node.isEditable && (
                    node.text?.toString()?.contains(targetHintOrId, true) == true ||
                    node.hintText?.toString()?.contains(targetHintOrId, true) == true ||
                    node.viewIdResourceName?.contains(targetHintOrId, true) == true
                )
            }
        } else {
            // Find currently focused editable or first available editable field
            searchNode(root) { it.isEditable && it.isFocused } ?: searchNode(root) { it.isEditable }
        }

        if (targetNode != null) {
            targetNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            val result = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            targetNode.recycle()
            return result
        }
        return false
    }

    // =========================================================================
    // 5. GLOBAL NAVIGATION ACTIONS
    // =========================================================================

    fun navigateBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun navigateHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun openRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun openNotifications(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    fun openQuickSettings(): Boolean = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)

    // =========================================================================
    // 6. SCREEN TREE INSPECTION (FOR GEMINI AI SCREEN AWARENESS)
    // =========================================================================

    /**
     * Inspects the entire active screen tree and returns a compact list of visible text & clickable buttons.
     */
    fun inspectScreenElements(): List<ScreenElement> {
        val root = rootInActiveWindow ?: return emptyList()
        val elements = mutableListOf<ScreenElement>()
        collectElements(root, elements)
        return elements
    }

    private fun collectElements(node: AccessibilityNodeInfo?, list: MutableList<ScreenElement>) {
        if (node == null) return

        val text = node.text?.toString().orEmpty()
        val desc = node.contentDescription?.toString().orEmpty()
        val viewId = node.viewIdResourceName
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        if ((text.isNotBlank() || desc.isNotBlank() || node.isClickable || node.isEditable) && bounds.width() > 0 && bounds.height() > 0) {
            list.add(
                ScreenElement(
                    text = text,
                    viewId = viewId,
                    contentDescription = desc,
                    className = node.className?.toString().orEmpty(),
                    isClickable = node.isClickable,
                    isEditable = node.isEditable,
                    bounds = bounds
                )
            )
        }

        for (i in 0 until node.childCount) {
            collectElements(node.getChild(i), list)
        }
    }

    private fun searchNode(root: AccessibilityNodeInfo, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (predicate(root)) return root
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = searchNode(child, predicate)
            if (found != null) return found
        }
        return null
    }
}
