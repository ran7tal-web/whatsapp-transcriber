package com.example.whatsapptranscriber

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import android.widget.TextView
import java.util.concurrent.TimeUnit

class WhatsAppAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private val activeOverlays = mutableListOf<LinearLayout>()
    private val processedMessageIds = mutableSetOf<String>()
    private val TIME_THRESHOLD_MS = TimeUnit.HOURS.toMillis(24)

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.packageName != "com.whatsapp") return

        val rootNode = rootInActiveWindow ?: return
        findVoiceNotesAndAttachOverlay(rootNode)
    }

    private fun findVoiceNotesAndAttachOverlay(node: AccessibilityNodeInfo) {
        if (isVoiceNote(node)) {
            val nodeId = generateNodeUniqueId(node)
            
            if (!processedMessageIds.contains(nodeId) && isWithinTimeThreshold(node)) {
                processedMessageIds.add(nodeId)
                
                val rect = Rect()
                node.getBoundsInScreen(rect)
                showTranscriptionOverlay(rect.left, rect.bottom, "תמלול מקומי: [תוכן ההודעה]")
            }
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { findVoiceNotesAndAttachOverlay(it) }
        }
    }

    private fun isVoiceNote(node: AccessibilityNodeInfo): Boolean {
        return node.viewIdResourceName?.contains("voice_note") == true || 
               node.contentDescription?.contains("Voice message") == true
    }

    private fun isWithinTimeThreshold(node: AccessibilityNodeInfo): Boolean {
        val currentTime = System.currentTimeMillis()
        val messageTime = extractMessageTimestamp(node) ?: currentTime
        return (currentTime - messageTime) <= TIME_THRESHOLD_MS
    }

    private fun extractMessageTimestamp(node: AccessibilityNodeInfo): Long? {
        return null 
    }

    private fun generateNodeUniqueId(node: AccessibilityNodeInfo): String {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return "${node.contentDescription}_${rect.top}_${rect.left}"
    }

    private fun showTranscriptionOverlay(x: Int, y: Int, text: String) {
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }

        val overlayView = LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#E0000000"))
            setPadding(16, 8, 16, 8)
            val textView = TextView(context).apply {
                this.text = text
                setTextColor(Color.WHITE)
                textSize = 12f
            }
            addView(textView)
        }

        windowManager?.addView(overlayView, layoutParams)
        activeOverlays.add(overlayView)
    }

    override fun onInterrupt() {
        clearOverlays()
    }

    private fun clearOverlays() {
        activeOverlays.forEach { windowManager?.removeView(it) }
        activeOverlays.clear()
    }
}
