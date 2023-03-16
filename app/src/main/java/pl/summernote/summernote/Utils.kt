package pl.summernote.summernote

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class Utils() {
    fun saveImage(filename: String, bitmap: Bitmap, cacheDir: File): File? {
        val file = File(cacheDir, filename)
        val dirs = filename.split("/") // split the string by "/"
        var dirName = ""
        for (i in dirs.dropLast(1)) {
            dirName += "/$i"
            val dir = File(cacheDir, dirName)
            Log.e("SAVE DIRS", dir.path)
            dir.mkdir()
        }
        return try {
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
            file
        } catch (e: IOException) {
            Log.e("SAVE DIRS", "Error saving bitmap to cache directory", e)
            null
        }
    }

    fun rotateImage(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()

        matrix.postRotate(90f)

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width, bitmap.height, true)

        return Bitmap.createBitmap(
            scaledBitmap,
            0,
            0,
            scaledBitmap.width,
            scaledBitmap.height,
            matrix,
            true
        )
    }
}