package pl.summernote.summernote.customs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

class MagnifierView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var magnifiedBitmap: Bitmap? = null
    private var touchX: Float = 0f
    private var touchY: Float = 0f
    private val magnificationFactor = 2f

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        magnifiedBitmap?.let {
            canvas?.drawBitmap(it, touchX - it.width / 2f, touchY - it.height / 2f, null)
        }
    }

    fun setMagnifiedBitmap(bitmap: Bitmap?, x: Float, y: Float) {
        magnifiedBitmap = bitmap?.let {
            Bitmap.createScaledBitmap(it, (it.width * magnificationFactor).toInt(), (it.height * magnificationFactor).toInt(), true)
        }
        touchX = x
        touchY = y
        invalidate()
    }
}
