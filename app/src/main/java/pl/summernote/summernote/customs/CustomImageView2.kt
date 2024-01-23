package pl.summernote.summernote.customs

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.imageview.ShapeableImageView
import org.opencv.core.Rect

class CustomImageView2(context: Context, attrs: AttributeSet?) : ShapeableImageView(context, attrs) {

    interface OnRectangleUpdateListener {
        fun onRectanglesUpdated(rectangles: Rect, action: String)
    }

    val rectangles = mutableListOf<Rect>()
    private var currentRect: Rect? = null

    private var listener: OnRectangleUpdateListener? = null



    fun setOnRectangleUpdateListener(listener: OnRectangleUpdateListener) {
        this.listener = listener
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.let {
            // Draw the existing rectangles
            rectangles.forEach { rect ->
                val rectF = RectF(rect.x.toFloat(), rect.y.toFloat(), rect.x.toFloat() + rect.width.toFloat(), rect.y.toFloat() + rect.height.toFloat())
                it.drawRect(rectF, Paint().apply {
                    color = Color.BLUE
                    style = Paint.Style.STROKE
                    strokeWidth = 5f
                })
            }
            // Draw the current rectangle being drawn
            currentRect?.let { rect ->
                val rectF = RectF(rect.x.toFloat(), rect.y.toFloat(), rect.x.toFloat() + rect.width.toFloat(), rect.y.toFloat() + rect.height.toFloat())
                it.drawRect(rectF, Paint().apply {
                    color = Color.parseColor("#7EABE2")
                    style = Paint.Style.STROKE
                    strokeWidth = 5f
                })
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            when (it.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Check if the touch event is inside an existing rectangle
                    val touchX = event.x.toInt()
                    val touchY = event.y.toInt()
                    Log.d("RECTANGLESARRAY", rectangles.toString())
                    Log.d("RECTANGLESARRAY", rectangles.toString())
                    for (existingRect in rectangles) {
                        Log.d("POSITIONCONTAINS", (event.x in(existingRect.x.toFloat().rangeTo((existingRect.x + existingRect.width).toFloat()))).toString())
                        Log.d("POSITIONCONTAINS", "${event.x}, ${existingRect.x}, ${existingRect.x + existingRect.width}")
                        Log.d("POSITIONCONTAINS", "${event.y}, ${existingRect.y}, ${existingRect.y + existingRect.height}")

                        val x_1 = existingRect.x.toFloat()
                        val x_2 = (existingRect.x + existingRect.width).toFloat()

                        val y_1 = existingRect.y.toFloat()
                        val y_2 = (existingRect.y + existingRect.height).toFloat()

                        if (
                            (event.x in(x_1.rangeTo(x_2)) || event.x in(x_2.rangeTo(x_1))) &&
                            (event.y in(y_1.rangeTo(y_2)) || event.y in(y_2.rangeTo(y_1)))
                        )
                        {
                            // Remove the existing rectangle and redraw
                            rectangles.remove(existingRect)
                            listener?.onRectanglesUpdated(existingRect, "delete")
                            invalidate()
                            return true
                        }
                    }
                    // If the touch event is not inside an existing rectangle, start drawing a new rectangle
                    currentRect = Rect(it.x.toInt(), it.y.toInt(), it.x.toInt(), it.y.toInt())
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    currentRect?.width = it.x.toInt() - currentRect!!.x
                    currentRect?.height = it.y.toInt() - currentRect!!.y
                    invalidate()
                }
                MotionEvent.ACTION_UP -> {
                    currentRect?.let { rect ->
                        // Add the new rectangle and notify the listener
                        rectangles.add(rect)
                        listener?.onRectanglesUpdated(rect, "add")
                    }
                    currentRect = null
                    invalidate()
                }
            }
        }
        return true
    }
    fun getBitmapWithDrawings(): Bitmap {
        // Create a bitmap with the same dimensions as the image
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        // Create a canvas using the bitmap
        val canvas = Canvas(bitmap)
        // Draw the image on the canvas
        drawable?.draw(canvas)
        // Draw the rectangles on the canvas
        rectangles.forEach { rect ->
            val rectF = RectF(
                rect.x.toFloat(),
                rect.y.toFloat(),
                rect.x.toFloat() + rect.width.toFloat(),
                rect.y.toFloat() + rect.height.toFloat()
            )
            canvas.drawRect(rectF, Paint().apply {
                color = Color.BLUE
                style = Paint.Style.STROKE
                strokeWidth = 5f
            })
        }
        return bitmap
    }
}