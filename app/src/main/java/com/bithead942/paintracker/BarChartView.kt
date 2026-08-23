package com.bithead942.paintracker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val data = mutableListOf<Pair<String, Int>>()
    private val pressureData = mutableListOf<Pair<String, Int>>()

    private val pressureMin = 998.225f
    private val pressureMid = 1013.25f
    private val pressureMax = 1028.275f

    fun setData(values: List<Pair<String, Int>>) {
        data.clear()
        data.addAll(values)
        invalidate()
    }

    fun setPressureData(values: List<Pair<String, Int>>) {
        pressureData.clear()
        pressureData.addAll(values)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val padding = 60f
        val chartW = width - 2 * padding
        val chartH = height - 2 * padding
        val bottom = height - padding

        paint.color = Color.GRAY
        paint.strokeWidth = 3f
        canvas.drawLine(padding, padding, padding, bottom, paint)
        canvas.drawLine(padding, bottom, width - padding, bottom, paint)
        canvas.drawLine(width - padding, padding, width - padding, bottom, paint)

        textPaint.color = Color.BLACK
        textPaint.textSize = 24f

        textPaint.textAlign = Paint.Align.RIGHT
        val levels = listOf("Severe", "Moderate", "Mild", "None")
        val step = chartH / 3f
        for (i in 0..3) {
            val y = padding + i * step
            canvas.drawText(levels[i], padding - 10, y + 8, textPaint)
        }

        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 18f
        canvas.drawText("%.3f".format(pressureMax), width - padding + 10, padding + 8, textPaint)
        canvas.drawText("%.2f".format(pressureMid), width - padding + 10, padding + chartH / 2 + 8, textPaint)
        canvas.drawText("%.3f".format(pressureMin), width - padding + 10, bottom + 8, textPaint)
        textPaint.textSize = 24f

        if (data.isEmpty()) return
        val gap = chartW / data.size
        val barW = gap * 0.6f
        for ((i, pair) in data.withIndex()) {
            val severity = pair.second.coerceIn(0, 3)
            val barH = (severity / 3f) * chartH
            val x = padding + i * gap + gap / 2 - barW / 2
            val y = bottom - barH
            paint.color = when (severity) {
                1 -> Color.YELLOW
                2 -> Color.parseColor("#FFA500")
                3 -> Color.RED
                else -> Color.LTGRAY
            }
            canvas.drawRect(x, y, x + barW, bottom, paint)

            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(formatDate(pair.first), x + barW / 2, bottom + 30, textPaint)
        }

        linePaint.color = Color.BLUE
        linePaint.strokeWidth = 4f
        dotPaint.color = Color.BLUE

        val range = pressureMax - pressureMin
        val points = mutableListOf<Pair<Float, Float>>()
        for ((i, pair) in pressureData.withIndex()) {
            val pressure = pair.second
            if (pressure == -1) continue
            val t = ((pressure - pressureMin) / range).coerceIn(0f, 1f)
            val y = bottom - t * chartH
            val x = padding + i * gap + gap / 2
            points.add(x to y)
        }

        for (i in 0 until points.size - 1) {
            val (x1, y1) = points[i]
            val (x2, y2) = points[i + 1]
            canvas.drawLine(x1, y1, x2, y2, linePaint)
        }

        for ((x, y) in points) {
            canvas.drawCircle(x, y, 8f, dotPaint)
        }
    }

    private fun formatDate(ymd: String): String {
        val parts = ymd.split("-")
        return if (parts.size == 3) "${parts[1]}/${parts[2]}" else ymd
    }
}
