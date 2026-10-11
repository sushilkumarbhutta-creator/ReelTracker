package com.example.reeltracker

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val bgColor = "#0F1020"
    private val cardColor = "#1B1D33"
    private val accentColor = "#FF6F91"
    private val mutedColor = "#9AA0B4"
    private val goodColor = "#66BB6A"
    private val badColor = "#FF8A65"

    private val density by lazy { resources.displayMetrics.density }
    private fun dp(value: Int): Int = (value * density).toInt()

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    private lateinit var brain: BrainView
    private lateinit var moodLabel: TextView
    private lateinit var moodMessage: TextView
    private lateinit var moodTotal: TextView
    private lateinit var reelsValue: TextView
    private lateinit var shortsValue: TextView
    private lateinit var igValue: TextView
    private lateinit var ytValue: TextView
    private lateinit var allValue: TextView
    private lateinit var bubbleSwitch: SwitchCompat
    private lateinit var accStatus: TextView
    private lateinit var usageStatus: TextView
    private lateinit var overlayStatus: TextView
    private lateinit var debugText: TextView

    private var suppressSwitch = false
    private val handler = Handler(Looper.getMainLooper())

    private val ticker = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, 3000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(48), dp(20), dp(32))
        }
        build(page)

        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.parseColor(bgColor))
            addView(page)
        })
    }

    override fun onResume() {
        super.onResume()
        handler.post(ticker)
    }

    override fun onPause() {
        handler.removeCallbacks(ticker)
        super.onPause()
    }

    private fun build(page: LinearLayout) {
        // Header
        page.addView(text("Reel Tracker", 28f, "#FFFFFF", bold = true))
        page.addView(text(todayLabel(), 14f, mutedColor))

        // Mood hero: one brain character whose face changes with your mood
        val hero = card().apply { gravity = Gravity.CENTER_HORIZONTAL }
        brain = BrainView(this)
        moodLabel = text("", 24f, "#FFFFFF", bold = true, center = true)
        moodMessage = text("", 15f, mutedColor, center = true)
        moodTotal = text("", 13f, mutedColor, center = true)
        hero.addView(brain, lp(dp(160), dp(160)))
        hero.addView(moodLabel)
        hero.addView(moodMessage)
        hero.addView(moodTotal)
        page.addView(hero, lp(MATCH, WRAP, top = dp(18)))

        // Stat grid
        val (reelsCard, reels) = statCard("Reels today")
        val (shortsCard, shorts) = statCard("Shorts today")
        val (igCard, ig) = statCard("Instagram time")
        val (ytCard, yt) = statCard("YouTube time")
        val (allCard, all) = statCard("All apps screen time")
        reelsValue = reels
        shortsValue = shorts
        igValue = ig
        ytValue = yt
        allValue = all

        page.addView(row(reelsCard, shortsCard), lp(MATCH, WRAP, top = dp(14)))
        page.addView(row(igCard, ytCard), lp(MATCH, WRAP, top = dp(12)))
        page.addView(allCard, lp(MATCH, WRAP, top = dp(12)))

        // Bubble on/off
        bubbleSwitch = SwitchCompat(this).apply {
            setOnCheckedChangeListener { _, checked ->
                if (!suppressSwitch) toggleBubble(checked)
            }
        }
        val bubbleLabels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(text("Floating bubble", 16f, "#FFFFFF", bold = true))
            addView(text("Shows your mood and count over other apps", 12f, mutedColor))
        }
        val bubbleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(bubbleLabels, lp(0, WRAP, weight = 1f))
            addView(bubbleSwitch)
        }
        page.addView(card().apply { addView(bubbleRow) }, lp(MATCH, WRAP, top = dp(14)))

        // Setup status
        val setupCard = card()
        setupCard.addView(text("Setup", 16f, "#FFFFFF", bold = true))
        accStatus = setupRow(setupCard, "Accessibility", "Needed to count reels and Shorts") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        usageStatus = setupRow(setupCard, "Usage access", "Needed for screen time") {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        overlayStatus = setupRow(setupCard, "Display over apps", "Needed to show the bubble") {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }
        page.addView(setupCard, lp(MATCH, WRAP, top = dp(14)))

        // Diagnostics, hidden until needed
        val debugCard = card()
        debugCard.addView(text("Detection debug", 16f, "#FFFFFF", bold = true))
        debugCard.addView(
            text(
                "If reel or Shorts counts stay at 0, open the reels screen, come back here, and send me the text below.",
                12f,
                mutedColor
            )
        )
        debugText = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            textSize = 11f
            setTextColor(Color.parseColor("#C5CAE9"))
            visibility = View.GONE
        }
        val toggle = Button(this).apply {
            text = "Show screen IDs"
            isAllCaps = false
            setOnClickListener {
                val show = debugText.visibility != View.VISIBLE
                debugText.visibility = if (show) View.VISIBLE else View.GONE
                text = if (show) "Hide screen IDs" else "Show screen IDs"
            }
        }
        debugCard.addView(toggle, lp(WRAP, WRAP, top = dp(10)))
        debugCard.addView(debugText, lp(MATCH, WRAP, top = dp(10)))
        page.addView(debugCard, lp(MATCH, WRAP, top = dp(14)))

        page.addView(
            text("All data stays on this phone.", 12f, mutedColor, center = true),
            lp(MATCH, WRAP, top = dp(18))
        )
    }

    private fun refresh() {
        val usage = UsageTracker.todayForegroundMs(this)
        val igMs = usage[UsageTracker.IG] ?: 0L
        val ytMs = usage[UsageTracker.YT] ?: 0L
        val allMs = usage.values.sum()
        val reels = Repository.getCount(this, Repository.Type.REEL)
        val shorts = Repository.getCount(this, Repository.Type.SHORT)
        val index = Moods.indexFor((igMs + ytMs) / 60_000)
        val state = Moods.states[index]

        brain.mood = index
        moodLabel.text = state.label
        moodMessage.text = state.message
        moodTotal.text = "Instagram + YouTube today: ${UsageTracker.formatMs(igMs + ytMs)}"

        reelsValue.text = reels.toString()
        shortsValue.text = shorts.toString()
        igValue.text = UsageTracker.formatMs(igMs)
        ytValue.text = UsageTracker.formatMs(ytMs)
        allValue.text = UsageTracker.formatMs(allMs)

        setStatus(accStatus, isAccessibilityOn(), "Enabled", "Not enabled")
        setStatus(usageStatus, hasUsageAccess(), "Granted", "Not granted")
        setStatus(overlayStatus, Settings.canDrawOverlays(this), "Granted", "Not granted")

        suppressSwitch = true
        bubbleSwitch.isChecked = BubbleService.isRunning
        suppressSwitch = false

        debugText.text = Repository.getDebugIds(this).ifEmpty {
            "Nothing captured yet. Open Instagram Reels or YouTube Shorts first."
        }
    }

    private fun toggleBubble(on: Boolean) {
        val intent = Intent(this, BubbleService::class.java)
        if (on) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Allow display over other apps first", Toast.LENGTH_SHORT).show()
                refresh()
                return
            }
            ContextCompat.startForegroundService(this, intent)
        } else {
            stopService(intent)
        }
    }

    private fun setStatus(view: TextView, ok: Boolean, okText: String, badText: String) {
        view.text = if (ok) "✓ $okText" else badText
        view.setTextColor(Color.parseColor(if (ok) goodColor else badColor))
    }

    private fun isAccessibilityOn(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.contains(ComponentName(this, ReelTrackerService::class.java).flattenToString())
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = getSystemService(AppOpsManager::class.java)
        @Suppress("DEPRECATION")
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun todayLabel(): String =
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())

    // ---- small UI helpers ----

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(16), dp(18), dp(16))
        background = roundedBg(cardColor, dp(20).toFloat())
    }

    private fun statCard(label: String): Pair<LinearLayout, TextView> {
        val value = text("0", 24f, "#FFFFFF", bold = true)
        val c = card()
        c.addView(text(label, 12f, mutedColor))
        c.addView(value)
        return Pair(c, value)
    }

    private fun row(left: View, right: View): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(left, lp(0, WRAP, weight = 1f))
        addView(right, lp(0, WRAP, weight = 1f, left = dp(12)))
    }

    private fun setupRow(parent: LinearLayout, title: String, subtitle: String, onOpen: () -> Unit): TextView {
        val status = text("", 12f, mutedColor)
        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(text(title, 15f, "#FFFFFF"))
            addView(text(subtitle, 12f, mutedColor))
            addView(status)
        }
        val button = Button(this).apply {
            text = "Open"
            isAllCaps = false
            setOnClickListener { onOpen() }
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, 0)
            addView(labels, lp(0, WRAP, weight = 1f))
            addView(button)
        }
        parent.addView(row)
        return status
    }

    private fun text(
        value: String,
        size: Float,
        color: String,
        bold: Boolean = false,
        center: Boolean = false
    ): TextView = TextView(this).apply {
        this.text = value
        textSize = size
        setTextColor(Color.parseColor(color))
        if (bold) typeface = Typeface.DEFAULT_BOLD
        if (center) gravity = Gravity.CENTER_HORIZONTAL
        setLineSpacing(0f, 1.15f)
    }

    private fun lp(
        w: Int,
        h: Int,
        weight: Float = 0f,
        top: Int = 0,
        left: Int = 0
    ): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(w, h, weight).apply {
            topMargin = top
            leftMargin = left
        }

    private fun roundedBg(color: String, radius: Float) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = radius
        setColor(Color.parseColor(color))
    }
}
