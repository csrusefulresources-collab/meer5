package com.baseerat.vision.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Accessibility Service for Baseerat.
 *
 * Responsibilities:
 * - Allows the app to be enabled as an accessibility service (required for deep screen access).
 * - Can later be extended to extract screen text / node tree for richer descriptions.
 * - Currently kept lightweight; the floating overlay is handled by OverlayService.
 */
class BaseeratAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Reserved for future: live auto-describe of unlabeled images,
        // or extracting focused node text for richer context.
    }

    override fun onInterrupt() {
        // Required override
    }

    /**
     * Helper: get a simple text summary of the current window content.
     * Useful when we want a text-based description without a screenshot.
     */
    fun getCurrentWindowTextSummary(maxLength: Int = 800): String {
        val root = rootInActiveWindow ?: return ""
        val sb = StringBuilder()
        collectText(root, sb, maxLength)
        return sb.toString().trim()
    }

    private fun collectText(node: AccessibilityNodeInfo?, out: StringBuilder, max: Int) {
        if (node == null || out.length >= max) return
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        if (!text.isNullOrBlank()) {
            if (out.isNotEmpty()) out.append(" ")
            out.append(text)
        } else if (!desc.isNullOrBlank()) {
            if (out.isNotEmpty()) out.append(" ")
            out.append(desc)
        }
        for (i in 0 until node.childCount) {
            if (out.length >= max) break
            collectText(node.getChild(i), out, max)
        }
    }

    companion object {
        @Volatile
        var instance: BaseeratAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}
