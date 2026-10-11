package com.example.reeltracker

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Detects when the reels/shorts screen is visible and counts each vertical swipe as one item.
 * It reads only view IDs, class names and scroll events. It never stores screen text.
 */
class ReelTrackerService : AccessibilityService() {

    // Marker strings are substrings of view resource IDs that appear only on the reels/shorts
    // screen. These are PLACEHOLDERS. The "Screen IDs seen" section in the app shows the real
    // IDs. Copy them there and update these markers.
    private val targets: Map<String, Pair<Repository.Type, List<String>>> = mapOf(
        UsageTracker.IG to (Repository.Type.REEL to listOf("clips_viewer")),
        UsageTracker.YT to (Repository.Type.SHORT to listOf("reel_player_page", "shorts_container"))
    )

    private var inShortsScreen = false
    private var lastCountedAt = 0L
    private var lastDumpAt = 0L

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
                val root = rootInActiveWindow
                inShortsScreen = hasMarker(root, markers, 0)
                captureIds(pkg, root)
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

    /** Saves the IDs of the visible screen (throttled) so they can be inspected in the app. */
    private fun captureIds(pkg: String, root: AccessibilityNodeInfo?) {
        val now = System.currentTimeMillis()
        if (root == null || now - lastDumpAt < 3000) return
        lastDumpAt = now

        val lines = LinkedHashSet<String>()
        collectIds(root, lines, 0)
        Repository.saveDebugIds(applicationContext, "$pkg\n" + lines.joinToString("\n"))
    }

    private fun collectIds(node: AccessibilityNodeInfo?, out: MutableSet<String>, depth: Int) {
        if (node == null || depth > 25 || out.size > 200) return
        node.viewIdResourceName?.let { out.add("id: $it") }
        node.className?.toString()?.let { cls ->
            if (cls.contains("ViewPager") || cls.contains("RecyclerView")) out.add("class: $cls")
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectIds(child, out, depth + 1)
            child.recycle()
        }
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
