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
    private val data = mutableListOf<Pair<String, Int>>()

    fun setData(values: List<Pair<String, Int>>) {
        data.clear()
        data.addAll(values)
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

        textPaint.color = Color.BLACK
        textPaint.textSize = 24f
        textPaint.textAlign = Paint.Align.RIGHT
        val levels = listOf("Severe", "Moderate", "Mild", "None")
        val step = chartH / 3f
        for (i in 0..3) {
            val y = padding + i * step
            canvas.drawText(levels[i], padding - 10, y + 8, textPaint)
        }

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
    }

    private fun formatDate(ymd: String): String {
        val parts = ymd.split("-")
        return if (parts.size == 3) "${parts[1]}/${parts[2]}" else ymd
    }
}
