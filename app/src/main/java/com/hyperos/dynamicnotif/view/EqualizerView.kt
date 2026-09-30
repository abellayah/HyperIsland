package com.hyperos.dynamicnotif.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

class EqualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF6900")
        style = Paint.Style.FILL
    }

    private val barRect = RectF()
    private val barHeights = floatArrayOf(0.3f, 0.7f, 0.4f)
    private var animator: ValueAnimator? = null
    private var isPlaying = false

    fun start() {
        if (isPlaying) return
        isPlaying = true
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 450
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
            addUpdateListener {
                val fraction = it.animatedFraction
                barHeights[0] = 0.2f + 0.7f * fraction
                barHeights[1] = 0.9f - 0.6f * fraction
                barHeights[2] = 0.3f + 0.5f * fraction
                invalidate()
            }
            start()
        }
    }

    fun stop() {
        isPlaying = false
        animator?.cancel()
        barHeights[0] = 0.3f
        barHeights[1] = 0.5f
        barHeights[2] = 0.3f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val barCount = 3
        val barWidth = w / (barCount * 2 - 1)
        val corner = barWidth / 2f

        for (i in 0 until barCount) {
            val left = i * barWidth * 2f
            val right = left + barWidth
            val barHeight = h * barHeights[i]
            val top = h - barHeight
            val bottom = h

            barRect.set(left, top, right, bottom)
            canvas.drawRoundRect(barRect, corner, corner, paint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }
}
