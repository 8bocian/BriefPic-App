package pl.summernote.summernote.customs

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import pl.summernote.summernote.dataclasses.Point
import kotlin.math.pow

class CustomImageView(context: Context, attrs: AttributeSet) : AppCompatImageView(context, attrs) {
    var points: MutableList<Point> = mutableListOf()
    private val paint = Paint()
    private val paintNum = Paint()
    private var touchedPoint: Point? = null
    var touchX = 0f
    var touchY = 0f

    init {
        paint.color = Color.RED
        paint.style = Paint.Style.FILL
        paintNum.color = Color.BLUE
        paintNum.style = Paint.Style.FILL
        paintNum.textSize = 30f
        paintNum.textAlign = Paint.Align.CENTER
        paintNum.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        setOnTouchListener { _, event ->
            try {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        touchedPoint = findClosestPoint(event.x, event.y)
                        touchedPoint?.let {
                            touchX = event.x - it.x
                            touchY = event.y - it.y
                            Log.d("TOUCH", "DOWN")
                        }
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        touchedPoint?.let {
                            it.x = event.x - touchX
                            it.y = event.y - touchY
                            if (!isInsideBoundaries(it)) {
                                points.remove(it)
                                touchedPoint = null
                            }
                            Log.d("TOUCH", "MOVE")
                            invalidate()
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        touchedPoint?.let {
                            if (isInsideBoundaries(it)) {
                                performClick()
                            }
                            touchedPoint = null
                            invalidate()
                        }
                        true
                    }
                    else -> false
                }
            } catch (e: java.lang.Exception){
                Log.d("ERROR", e.stackTrace.toString())
                Log.d("ERROR", e.localizedMessage.toString())
                Log.d("ERROR", e.message.toString())
                Log.d("ERROR", e.cause.toString())
                false
            }
        }
    }

    private fun isInsideBoundaries(point: Point): Boolean {
        return point.x >= 0 && point.x <= width && point.y >= 0 && point.y <= height
    }

    private fun findClosestPoint(x: Float, y: Float): Point? {
        var closestPoint: Point? = null
        var minDistance = Float.MAX_VALUE
        for (point in points) {
            val distance = ((x - point.x).pow(2) + (y - point.y).pow(2)).pow(0.5f)
            if (distance < minDistance) {
                closestPoint = point
                minDistance = distance
            }
        }
        return closestPoint
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    fun setPoint(x: Float, y: Float) {
        val point = Point(x, y)
        points.add(point)
        // Request a redraw of the view
        invalidate()
    }

    fun clearPoints() {
        points.clear()
        // Request a redraw of the view
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        try {
            for ((index, point) in points.withIndex()) {
                Log.d("DRAWPOINT", "$index, $point")
                canvas?.drawCircle(point.x, point.y, 30f, paint)
                canvas?.drawText("${index + 1}", point.x, point.y, paintNum)
            }
        } catch (e: java.lang.Exception){
            Log.d("ERROR", e.stackTrace.toString())
            Log.d("ERROR", e.localizedMessage.toString())
            Log.d("ERROR", e.message.toString())
            Log.d("ERROR", e.cause.toString())
        }
    }
}