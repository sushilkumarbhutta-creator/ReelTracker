package com.example.reeltracker

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

class BubbleService : Service() {

    companion object {
        const val NOTIF_ID = 1

        /** Read by the main screen to show whether the bubble is running. */
        @Volatile
        var isRunning = false
    }

    private lateinit var wm: WindowManager
    private lateinit var bubble: LinearLayout
    private lateinit var bubbleBrain: BrainView
    private lateinit var bubbleCount: TextView
    private lateinit var bubbleParams: WindowManager.LayoutParams

    private var panel: LinearLayout? = null
    private var panelBrain: BrainView? = null
    private var panelTitle: TextView? = null
    private var panelDetails: TextView? = null

    private var animator: ObjectAnimator? = null
    private var lastMoodIndex = -1

    private val handler = Handler(Looper.getMainLooper())
    private val density by lazy { resources.displayMetrics.density }
    private fun dp(value: Int): Int = (value * density).toInt()

    private val overlayType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    private val overlayFlags =
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

    private val refresher = object : Runnable {
        override fun run() {
            refreshAll()
            handler.postDelayed(this, 3000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(NOTIF_ID, buildNotification())
        addBubble()
        handler.post(refresher)
    }

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacks(refresher)
        animator?.cancel()
        hidePanel()
        wm.removeView(bubble)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun addBubble() {
        bubbleBrain = BrainView(this)
        bubbleCount = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(6), 0, 0, 0)
        }

        bubble = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(4), dp(14), dp(4))
            background = roundedBg("#E6101010", dp(26).toFloat())
            addView(bubbleBrain, LinearLayout.LayoutParams(dp(36), dp(36)))
            addView(bubbleCount)
        }

        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            overlayFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(16) // distance from the right edge
            y = dp(40) // below the status bar and camera cutout
        }

        attachDragAndTap(bubble, bubbleParams)
        wm.addView(bubble, bubbleParams)
    }

    /** Drag to move the bubble anywhere. A short tap opens or closes the details banner. */
    private fun attachDragAndTap(view: View, params: WindowManager.LayoutParams) {
        var startX = 0
        var startY = 0
        var downRawX = 0f
        var downRawY = 0f
        var moved = false

        view.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    downRawX = e.rawX
                    downRawY = e.rawY
                    moved = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downRawX
                    val dy = e.rawY - downRawY
                    if (abs(dx) > 8 || abs(dy) > 8) moved = true
                    params.x = startX - dx.toInt() // anchored to the right edge
                    params.y = (startY + dy.toInt()).coerceAtLeast(0)
                    wm.updateViewLayout(view, params)
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) togglePanel()
                }
            }
            true
        }
    }

    private fun togglePanel() {
        if (panel == null) showPanel() else hidePanel()
    }

    private fun showPanel() {
        if (panel != null) return

        val brain = BrainView(this)
        val title = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
        }
        val details = TextView(this).apply {
            setTextColor(Color.parseColor("#DDE1F0"))
            textSize = 13f
            setLineSpacing(0f, 1.2f)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(brain, LinearLayout.LayoutParams(dp(64), dp(64)))
            addView(
                title,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = dp(10)
                }
            )
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundedBg("#F2121212", dp(20).toFloat())
            addView(header)
            addView(
                details,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(10) }
            )
            setOnClickListener { hidePanel() }
        }

        val params = WindowManager.LayoutParams(
            dp(290),
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            overlayFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = bubbleParams.x
            y = bubbleParams.y + dp(52)
        }

        wm.addView(container, params)
        panel = container
        panelBrain = brain
        panelTitle = title
        panelDetails = details
        refreshAll()
    }

    private fun hidePanel() {
        panel?.let { wm.removeView(it) }
        panel = null
        panelBrain = null
        panelTitle = null
        panelDetails = null
    }

    private fun refreshAll() {
        val usage = UsageTracker.todayForegroundMs(this)
        val igMs = usage[UsageTracker.IG] ?: 0L
        val ytMs = usage[UsageTracker.YT] ?: 0L
        val allMs = usage.values.sum()
        val reels = Repository.getCount(this, Repository.Type.REEL)
        val shorts = Repository.getCount(this, Repository.Type.SHORT)

        val index = Moods.indexFor((igMs + ytMs) / 60_000)
        val state = Moods.states[index]

        bubbleBrain.mood = index
        bubbleCount.text = (reels + shorts).toString()
        if (index != lastMoodIndex) {
            lastMoodIndex = index
            startAnimation(index)
        }

        panelBrain?.mood = index
        panelTitle?.text = state.label
        panelDetails?.text = buildString {
            appendLine(state.message)
            appendLine()
            appendLine("Instagram + YouTube today: ${UsageTracker.formatMs(igMs + ytMs)}")
            appendLine()
            appendLine("Reels watched: $reels")
            appendLine("Shorts watched: $shorts")
            appendLine("Total: ${reels + shorts}")
            appendLine()
            appendLine("Instagram time: ${UsageTracker.formatMs(igMs)}")
            appendLine("YouTube time: ${UsageTracker.formatMs(ytMs)}")
            appendLine("All apps time: ${UsageTracker.formatMs(allMs)}")
            appendLine()
            append("Tap to close")
        }
    }

    /** Happier states pulse gently. Sad and crying states wobble. */
    private fun startAnimation(index: Int) {
        animator?.cancel()
        val wobble = index >= 5
        val anim = if (wobble) {
            ObjectAnimator.ofFloat(bubble, View.ROTATION, -4f, 4f)
        } else {
            ObjectAnimator.ofPropertyValuesHolder(
                bubble,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.08f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.08f)
            )
        }
        anim.apply {
            duration = if (wobble) 350L else 900L
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            start()
        }
        animator = anim
    }

    private fun roundedBg(color: String, radius: Float) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = radius
        setColor(Color.parseColor(color))
    }

    private fun buildNotification(): Notification {
        val channelId = "tracker"
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(channelId, "Tracker", NotificationManager.IMPORTANCE_MIN)
        )
        return Notification.Builder(this, channelId)
            .setContentTitle("Reel tracker is running")
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setOngoing(true)
            .build()
    }
}
