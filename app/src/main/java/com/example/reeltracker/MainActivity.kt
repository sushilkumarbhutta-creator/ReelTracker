package com.example.reeltracker

import android.app.usage.UsageStatsManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var statsText: TextView
    private val handler = Handler(Looper.getMainLooper())

    private val ticker = object : Runnable {
        override fun run() {
            refreshStats()
            handler.postDelayed(this, 2000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        statsText = TextView(this).apply {
            textSize = 17f
            setLineSpacing(0f, 1.2f)
        }

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(48, 120, 48, 48)
        root.addView(statsText)

        root.addView(button("1. Enable accessibility service") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        root.addView(button("2. Grant usage access") {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        })
        root.addView(button("3. Allow display over other apps") {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        })
        root.addView(button("4. Show bubble") {
            if (Settings.canDrawOverlays(this)) {
                ContextCompat.startForegroundService(
                    this,
                    Intent(this, BubbleService::class.java)
                )
            } else {
                Toast.makeText(this, "Allow overlay permission first", Toast.LENGTH_SHORT).show()
            }
        })

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        handler.post(ticker)
    }

    override fun onPause() {
        handler.removeCallbacks(ticker)
        super.onPause()
    }

    private fun button(label: String, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label
            setOnClickListener { onClick() }
        }

    private fun refreshStats() {
        val reels = Repository.getCount(this, Repository.Type.REEL)
        val shorts = Repository.getCount(this, Repository.Type.SHORT)
        val usage = todayUsageMs()

        val igMs = usage["com.instagram.android"] ?: 0L
        val ytMs = usage["com.google.android.youtube"] ?: 0L

        statsText.text = buildString {
            appendLine("Today")
            appendLine("Reels watched: $reels")
            appendLine("Shorts watched: $shorts")
            appendLine("Total: ${reels + shorts}")
            appendLine()
            appendLine("Time in Instagram: ${formatMs(igMs)}")
            appendLine("Time in YouTube: ${formatMs(ytMs)}")
            appendLine("Time in all apps: ${formatMs(usage.values.sum())}")
        }
    }

    /** Foreground time per app since midnight. Returns empty if usage access isn't granted. */
    private fun todayUsageMs(): Map<String, Long> {
        val usm = getSystemService(UsageStatsManager::class.java)
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return usm.queryAndAggregateUsageStats(start, System.currentTimeMillis())
            .mapValues { it.value.totalTimeInForeground }
    }

    private fun formatMs(ms: Long): String {
        val mins = ms / 60_000
        return "${mins / 60}h ${mins % 60}m"
    }
}
