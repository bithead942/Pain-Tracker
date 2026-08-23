package com.bithead942.paintracker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

class BodyMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bodyBitmap: Bitmap? = BitmapFactory.decodeResource(resources, R.drawable.body_map)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val hotspots = listOf(
        Hotspot("Left ankle", 0.40f, 0.93f),
        Hotspot("Right ankle", 0.60f, 0.93f),
        Hotspot("Left knee", 0.40f, 0.73f),
        Hotspot("Right knee", 0.60f, 0.73f),
        Hotspot("Left hip", 0.38f, 0.55f),
        Hotspot("Right hip", 0.62f, 0.55f),
        Hotspot("Left wrist", 0.15f, 0.45f),
        Hotspot("Right wrist", 0.85f, 0.45f),
        Hotspot("Left elbow", 0.17f, 0.33f),
        Hotspot("Right elbow", 0.83f, 0.33f),
        Hotspot("Left shoulder", 0.25f, 0.21f),
        Hotspot("Right shoulder", 0.75f, 0.21f),
        Hotspot("Neck", 0.50f, 0.15f)
    )

    data class Hotspot(val name: String, val relX: Float, val relY: Float, var state: Int = 0)

    fun getActiveEntries(): List<PainLogStore.PainEntry> =
        hotspots.filter { it.state > 0 }
            .map { PainLogStore.PainEntry(it.name, it.state) }

    fun setJoints(entries: List<PainLogStore.PainEntry>) {
        for (h in hotspots) h.state = 0
        for (e in entries) {
            hotspots.find { it.name == e.location }?.state = e.severity
        }
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val bmp = bodyBitmap
        if (bmp == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        val w = resolveSize(MeasureSpec.getSize(widthMeasureSpec), widthMeasureSpec)
        val desiredHeight = (w / (bmp.width.toFloat() / bmp.height)).toInt()
        val h = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = bodyBitmap ?: return
        val dst = computeDst(bmp)
        canvas.drawBitmap(bmp, null, dst, null)
        drawHotspots(canvas, dst)
    }

    private fun computeDst(bmp: Bitmap): RectF {
        val viewRatio = width.toFloat() / height
        val bmpRatio = bmp.width.toFloat() / bmp.height
        val dst = RectF()
        if (viewRatio > bmpRatio) {
            val h = height.toFloat()
            val w = h * bmpRatio
            dst.top = 0f
            dst.bottom = h
            dst.left = (width - w) / 2
            dst.right = dst.left + w
        } else {
            val w = width.toFloat()
            val h = w / bmpRatio
            dst.left = 0f
            dst.right = w
            dst.top = (height - h) / 2
            dst.bottom = dst.top + h
        }
        return dst
    }

    private fun drawHotspots(canvas: Canvas, dst: RectF) {
        for (h in hotspots) {
            val x = dst.left + h.relX * dst.width()
            val y = dst.top + h.relY * dst.height()
            val r = min(dst.width(), dst.height()) * 0.065f
            if (h.state > 0) {
                val (red, green, blue) = when (h.state) {
                    1 -> Triple(255, 255, 0)      // Mild - yellow
                    2 -> Triple(255, 165, 0)      // Moderate - orange
                    else -> Triple(255, 0, 0)     // Severe - red
                }
                paint.color = Color.argb(150, red, green, blue)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(x, y, r, paint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val bmp = bodyBitmap ?: return false
                val dst = computeDst(bmp)
                for (h in hotspots) {
                    val x = dst.left + h.relX * dst.width()
                    val y = dst.top + h.relY * dst.height()
                    val r = min(dst.width(), dst.height()) * 0.09f
                    val dx = event.x - x
                    val dy = event.y - y
                    if (dx * dx + dy * dy <= r * r) {
                        h.state = (h.state + 1) % 4
                        invalidate()
                        return true
                    }
                }
            }
            MotionEvent.ACTION_UP -> performClick()
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }
}
