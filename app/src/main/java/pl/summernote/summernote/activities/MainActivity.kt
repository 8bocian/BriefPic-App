package pl.summernote.summernote.activities

import androidx.camera.core.ImageProxy
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okio.Timeout
import org.json.JSONObject
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils.bitmapToMat
import org.opencv.android.Utils.matToBitmap
import org.opencv.core.*
import pl.summernote.summernote.BuildConfig
import pl.summernote.summernote.R
import pl.summernote.summernote.Utils
import pl.summernote.summernote.databinding.MainActivityBinding
import pl.summernote.summernote.dataclasses.Point
import pl.summernote.summernote.interfaces.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.*
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.cvtColor
import java.util.concurrent.CountDownLatch


class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_IMAGE_SELECT = 2
    }

    private lateinit var photoURI: Uri
    private lateinit var imageBitmap: Bitmap

    private lateinit var collectionName: String
    private lateinit var imagePath: String
    private var position = 0

    private val kernelSize: Double = 40.0

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private lateinit var binding: MainActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        if (!OpenCVLoader.initDebug()) {
            Log.d("OPENCV", "OpenCV not loaded");
        } else {
            Log.d("OPENCV", "OpenCV loaded");
        }



        imagePath = intent.getStringExtra("imagePath").toString()
        collectionName = intent.getStringExtra("collectionName").toString()
        position = intent.getIntExtra("position", 0)

        Log.d("VALS", imagePath)
        Log.d("VALS", collectionName)
        Log.d("VALS", position.toString())

        initializeImage(imagePath)

        binding.addButton.setOnClickListener {
            val x = binding.imageView.width.toFloat()/2
            val y = binding.imageView.height.toFloat()/2
            binding.imageView.setPoint(x, y)
            binding.imageView.invalidate()
            Log.d("POINT", binding.imageView.points.toString())
        }

        binding.sendButton.setOnClickListener {
            var points = binding.imageView.points
            if (points.isEmpty()){
                points = mutableListOf(Point((imageBitmap.width/2).toFloat(), (imageBitmap.height/2).toFloat()))
            }
            val images = preprocess(imageBitmap, points)
            val countDownLatch = CountDownLatch(images.size)
            lifecycleScope.launch(Dispatchers.IO) {
                val outputText = getTextFromImages(images, countDownLatch)
                val processedText = processResult(outputText)
                withContext(Dispatchers.Main) {
                    saveText(processedText)
                }
            }
        }
    }

    private fun saveText(text: String) {
        val path = "$cacheDir/collections/$collectionName/texts"
        File(path).mkdirs()
        val filename = "$position.txt"
        val file = File(path, filename)
        file.writeText(text)
        Log.d("SAVE", file.path)
    }

    private fun processResult(text: String): String {
        val pattern1 = Regex("(\\S)—\\s*")
        var result = pattern1.replace(text, "$1")

        val pattern2 = Regex("(\\S)-\\s*")
        result = pattern2.replace(result, "$1")

        val pattern3 = Regex("\\s+")
        result = pattern3.replace(result, " ")

        return result
    }

    private fun getTextFromImages(images: List<Mat>, countDownLatch: CountDownLatch): String {
        var text = ""
        for(image in images){
            val bitmap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888)
            matToBitmap(image, bitmap)
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener {
                    text += it.text
                }
                .addOnCompleteListener {
                    countDownLatch.countDown()
                }
        }
        countDownLatch.await()
        return text
    }

    private fun getTextBoxes(image: Mat): List<Rect> {
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(kernelSize, kernelSize))

        val imageDilated = Mat()
        Imgproc.dilate(image, imageDilated, kernel, Point(-1.0, -1.0), 1)

        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(imageDilated, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        val boxes = mutableListOf<Rect>()
        for (contour in contours) {
            val boundingRect = Imgproc.boundingRect(contour)
            boxes.add(Rect(boundingRect.x, boundingRect.y, boundingRect.width, boundingRect.height))
        }

        return boxes
    }

    private fun preprocess(bitmap: Bitmap, points: List<Point>): List<Mat> {

        val mat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC4)
        Log.d("OPENCV", "POINTS: $points")
        bitmapToMat(bitmap, mat)
        val imageGray = Mat()
        cvtColor(mat, imageGray, Imgproc.COLOR_RGB2GRAY)

        val imageThresh = Mat()
        Imgproc.adaptiveThreshold(imageGray, imageThresh, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 101, 30.0)

        val imageInv = Mat()
        Core.bitwise_not(imageThresh, imageInv)

        val boxes = getTextBoxes(imageInv)

        val boxesSelected = mutableListOf<Rect>()
        Rect()
        for (box in boxes) {
            val x1 = box.x
            val y1 = box.y
            val x2 = x1 + box.width
            val y2 = y1 + box.height

            for (point in points) {
                val px = point.x.toInt()
                val py = point.y.toInt()

                Imgproc.circle(mat, Point(px.toDouble(), py.toDouble()), 40, Scalar(0.0, 0.0, 255.0), -1)

                if (px in x1..x2 && y1 <= py && py <= y2) {
                    boxesSelected.add(box)
                    Log.d("OPENCV", "POINTS $x1, $y1, $x2, $y2")
                }
            }
        }

        val masks = mutableListOf<Mat>()

        for (box in boxesSelected) {
            val mask = Mat.zeros(imageInv.size(), imageInv.type())
            Imgproc.rectangle(mask, Point(box.x.toDouble(), box.y.toDouble()), Point(box.x + box.width.toDouble(), box.y + box.height.toDouble()), Scalar(255.0), -1)

            val imageMasked = Mat()
            Core.bitwise_and(imageInv, mask, imageMasked)

            masks.add(imageMasked)
        }

        return masks
    }

    fun initializeImage(imagePath: String){
        val file1 = File(cacheDir, imagePath)
        val file = File(file1.path)
        val fis = FileInputStream(file)
        val bitmap = BitmapFactory.decodeStream(fis)
        imageBitmap = Utils().rotateImage(bitmap)
        binding.imageView.setImageBitmap(imageBitmap)
    }
}