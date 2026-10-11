package com.example.reeltracker

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Detects when the reels/shorts screen is visible and counts each vertical swipe as one item.
 *
 * It only reads view IDs and scroll events. It never stores screen text.
 */
class ReelTrackerService : AccessibilityService() {

    // Marker strings are substrings of view resource IDs that appear only on the reels/shorts
    // screen. These are PLACEHOLDERS. Find the real IDs on your phone (see README) and update them.
    private val targets: Map<String, Pair<Repository.Type, List<String>>> = mapOf(
        "com.instagram.android" to (Repository.Type.REEL to listOf("clips_viewer")),
        "com.google.android.youtube" to (Repository.Type.SHORT to listOf("reel_player_page", "shorts_container"))
    )

    private var inShortsScreen = false
    private var lastCountedAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return

        val (type, markers) = targets[pkg] ?: run {
            inShortsScreen = false
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Re-check whether the visible screen is the reels/shorts viewer.
                val root = rootInActiveWindow
                inShortsScreen = hasMarker(root, markers, 0)
                root?.recycle()
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                if (inShortsScreen) countOneSwipe(type)
            }
        }
    }

    private fun countOneSwipe(type: Repository.Type) {
        val now = System.currentTimeMillis()
        // One swipe produces a burst of scroll events. Ignore anything within 1 second of the last count.
        if (now - lastCountedAt < 1000) return
        lastCountedAt = now
        Repository.increment(applicationContext, type)
    }

    private fun hasMarker(node: AccessibilityNodeInfo?, markers: List<String>, depth: Int): Boolean {
        if (node == null || depth > 25) return false
        val id = node.viewIdResourceName
        if (id != null && markers.any { id.contains(it) }) return true

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = hasMarker(child, markers, depth + 1)
            child.recycle()
            if (found) return true
        }
        return false
    }

    override fun onInterrupt() {
        // Required override. Nothing to clean up.
    }
}
