package com.example.reeltracker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import kotlin.math.min

/**
 * One brain character drawn in code, with a face inside.
 * The face changes with [mood]: 0 is happiest, 6 is crying.
 * It is drawn with Canvas, so no image files are needed.
 */
class BrainView(context: Context) : View(context) {

    var mood: Int = 0
        set(value) {
            field = value.coerceIn(0, 6)
            invalidate()
        }

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9C2A5B")
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F9B8CC")
    }
    private val fissurePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9C2A5B")
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3B1F2B")
    }
    private val mouthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3B1F2B")
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val tearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4FC3F7")
    }
    private val cheekPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF8FAB")
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(110, 255, 255, 255)
    }

    // Mouth curve per mood: +1 is a big smile, -1 is a frown
    private val smile = floatArrayOf(0.9f, 0.5f, 0.1f, -0.2f, -0.5f, -0.8f, -1f)

    // Brain lobes as (x, y, radius) in units where 1 is half the view size
    private val lobes = arrayOf(
        floatArrayOf(-0.40f, -0.10f, 0.36f),
        floatArrayOf(0.00f, -0.36f, 0.40f),
        floatArrayOf(0.40f, -0.10f, 0.36f),
        floatArrayOf(-0.40f, 0.28f, 0.30f),
        floatArrayOf(0.40f, 0.28f, 0.30f),
        floatArrayOf(0.00f, 0.16f, 0.46f)
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val u = min(width, height) * 0.46f
        val cx = width / 2f
        val cy = height / 2f
        fun px(v: Float) = cx + v * u
        fun py(v: Float) = cy + (v + 0.09f) * u // shift so the brain sits centered

        // Silhouette: dark outline pass first, then the pink fill pass
        for (l in lobes) canvas.drawCircle(px(l[0]), py(l[1]), (l[2] + 0.045f) * u, outlinePaint)
        for (l in lobes) canvas.drawCircle(px(l[0]), py(l[1]), l[2] * u, fillPaint)

        // Line between the two hemispheres
        fissurePaint.strokeWidth = 0.04f * u
        val fissure = Path().apply {
            moveTo(px(0f), py(-0.70f))
            quadTo(px(0.07f), py(-0.10f), px(0f), py(0.50f))
        }
        canvas.drawPath(fissure, fissurePaint)

        // Shine
        canvas.drawCircle(px(-0.26f), py(-0.42f), 0.07f * u, highlightPaint)

        // Cheeks
        canvas.drawCircle(px(-0.30f), py(0.14f), 0.08f * u, cheekPaint)
        canvas.drawCircle(px(0.30f), py(0.14f), 0.08f * u, cheekPaint)

        // Eyes
        canvas.drawCircle(px(-0.19f), py(-0.03f), 0.065f * u, inkPaint)
        canvas.drawCircle(px(0.19f), py(-0.03f), 0.065f * u, inkPaint)

        // Worried and sad states get slanted eyebrows
        if (mood >= 3) {
            mouthPaint.strokeWidth = 0.035f * u
            canvas.drawLine(px(-0.31f), py(-0.16f), px(-0.11f), py(-0.23f), mouthPaint)
            canvas.drawLine(px(0.31f), py(-0.16f), px(0.11f), py(-0.23f), mouthPaint)
        }

        // Tears for sad and crying
        if (mood >= 5) {
            canvas.drawCircle(px(-0.19f), py(0.08f), 0.035f * u, tearPaint)
            canvas.drawCircle(px(0.19f), py(0.08f), 0.035f * u, tearPaint)
        }
        if (mood == 6) {
            canvas.drawCircle(px(-0.19f), py(0.20f), 0.03f * u, tearPaint)
            canvas.drawCircle(px(0.19f), py(0.20f), 0.03f * u, tearPaint)
        }

        // Mouth: curves up when happy, down when sad
        mouthPaint.strokeWidth = 0.035f * u
        val s = smile[mood]
        val mouth = Path().apply {
            moveTo(px(-0.16f), py(0.22f))
            quadTo(px(0f), py(0.22f + s * 0.22f), px(0.16f), py(0.22f))
        }
        canvas.drawPath(mouth, mouthPaint)
    }
}
