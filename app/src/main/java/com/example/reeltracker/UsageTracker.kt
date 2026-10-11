package com.example.reeltracker

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar

/**
 * Uses usage events instead of aggregated usage stats. Aggregated stats update late,
 * which is why the earlier version looked stale. Events include the app open right now.
 */
object UsageTracker {

    const val IG = "com.instagram.android"
    const val YT = "com.google.android.youtube"

    /** Foreground time in milliseconds per package since midnight, including the app open now. */
    @Suppress("DEPRECATION")
    fun todayForegroundMs(context: Context): Map<String, Long> {
        val usm = context.getSystemService(UsageStatsManager::class.java)
        val now = System.currentTimeMillis()
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val events = usm.queryEvents(start, now)
        val event = UsageEvents.Event()
        val openSince = HashMap<String, Long>()
        val totals = HashMap<String, Long>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> openSince[pkg] = event.timeStamp
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val from = openSince.remove(pkg) ?: continue
                    totals[pkg] = (totals[pkg] ?: 0L) + (event.timeStamp - from)
                }
            }
        }

        // Apps still in the foreground right now
        for ((pkg, from) in openSince) {
            totals[pkg] = (totals[pkg] ?: 0L) + (now - from)
        }
        return totals
    }

    fun formatMs(ms: Long): String {
        val mins = ms / 60_000
        return "${mins / 60}h ${mins % 60}m"
    }
}
