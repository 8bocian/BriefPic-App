package pl.summernote.summernote.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils.bitmapToMat
import org.opencv.android.Utils.matToBitmap
import org.opencv.core.*
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.cvtColor
import pl.summernote.summernote.R
import pl.summernote.summernote.customs.CustomImageView2
import pl.summernote.summernote.customs.FlowSender
import pl.summernote.summernote.databinding.FlashcardPhotoLayoutBinding
import pl.summernote.summernote.dataclasses.FlashCard
import java.io.*
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


class AddPhotoFlashCardActivity : AppCompatActivity(), CustomImageView2.OnRectangleUpdateListener {

    private lateinit var imageBitmap: Bitmap

    private var allBoxes: ArrayList<Rect> = arrayListOf()
    private var boxes: ArrayList<Rect> = arrayListOf()
    private var texts: ArrayList<String> = arrayListOf()
    private lateinit var imageInv: Mat
    private var type: String = ""

    private lateinit var collectionName: String
    private lateinit var elementName: String
    private lateinit var bitmapPath: String
    private lateinit var jsonString: String
    private lateinit var array: ArrayList<FlashCard>
    private var position = 0

    private val kernelSize: Double = 12.5

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())

    private lateinit var binding: FlashcardPhotoLayoutBinding

    override fun onRectanglesUpdated(rectangle: Rect, action: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val widthScalingFactor =
                (imageBitmap.width.toDouble() / binding.imageView.width.toDouble())
            val heightScalingFactor =
                (imageBitmap.height.toDouble() / binding.imageView.height.toDouble())

            Log.d("RECTTEXT", widthScalingFactor.toString())

            Log.d("RECTTEXT", heightScalingFactor.toString())

            val newX = (rectangle.x.toDouble() * widthScalingFactor)
            val newY = (rectangle.y.toDouble() * heightScalingFactor)

            val newWidth = (rectangle.width.toDouble() * widthScalingFactor)
            val newHeight = (rectangle.height.toDouble() * heightScalingFactor)

            val scaledRect =
                Rect(newX.toInt(), newY.toInt(), newWidth.toInt(), newHeight.toInt())

            if(action=="add") {
                boxes.add(scaledRect)
//                texts.add(text)
            } else {
//                texts.removeAt(boxes.indexOf(rectangle))
                boxes.remove(scaledRect)
            }

            Log.d("TEXTSARRAY", boxes.toString())
            Log.d("TEXTSARRAY", texts.toString())
        }
    }

    override fun onResume() {
        super.onResume()
        val flowSender = FlowSender()
        val sharedPrefs: SharedPreferences = this@AddPhotoFlashCardActivity.baseContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val uuidString = sharedPrefs.getString("UUID", null)
        flowSender.sendFlowInformation(this.javaClass.simpleName, uuidString!!, "ENTER")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FlashcardPhotoLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        if (!OpenCVLoader.initDebug()) {
            Log.d("OPENCV", "OpenCV not loaded")
        } else {
            Log.d("OPENCV", "OpenCV loaded")
        }

        collectionName = intent.getStringExtra("collectionName").toString()
        elementName = intent.getStringExtra("elementName").toString()
        position = intent.getIntExtra("position", 0)
        bitmapPath = intent.getStringExtra("bitmapPath").toString()
        array = intent.getParcelableArrayListExtra<FlashCard>("array") as ArrayList<FlashCard>

        imageBitmap = BitmapFactory.decodeFile(bitmapPath)
        imageBitmap = rotateBitmap(imageBitmap, 90F)
        initializeImage(imageBitmap)

        binding.help.setOnClickListener {
            val helpText = "Draw rectangles to select the text and click on them to delete"
            val dialog = AlertDialog.Builder(this)
                .setMessage(helpText)
                .setPositiveButton("OK", null)
                .create()
            dialog.show()
        }

        binding.rotate.setOnClickListener {
            // Get a reference to the image view

            // Get the current rotation of the image view
            val matrix = Matrix()
            matrix.postRotate(90f)
            imageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.width, imageBitmap.height, matrix, true)
            initializeImage(imageBitmap)
            binding.imageView.rectangles.clear()
        }

        binding.imageView.setOnRectangleUpdateListener(this)

        binding.submit.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            lifecycleScope.launch(Dispatchers.IO) {
                if (boxes.isNotEmpty()) {
                    boxes.forEach { scaledRect ->
                        val images = cropImageFromBoxes(arrayListOf(scaledRect))
                        val countDownLatch = CountDownLatch(images.size)
                        val text = getTextFromImages(images, countDownLatch)
                        texts.add(text)
                    }
                    runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                    }
                    texts = texts.filter { it.isNotEmpty() } as ArrayList<String>
                    if (texts.isNotEmpty()) {
                        type = "vocabulary"
                        lifecycleScope.launch(Dispatchers.IO) {
                            sendTestRequest()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(
                                this@AddPhotoFlashCardActivity,
                                "No text detected",
                                Toast.LENGTH_SHORT
                            ).show()
                            binding.progressBar.visibility = View.GONE
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@AddPhotoFlashCardActivity,
                            "No boxes drawn",
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
        }
    }

    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun cropImageFromBoxes(boxesSelected: MutableList<Rect>): List<Mat>{
        val masks = mutableListOf<Mat>()

        for (box in boxesSelected) {
            val mask = Mat.zeros(imageInv.size(), imageInv.type())
            Imgproc.rectangle(mask,
                Point(box.x.toDouble(), box.y.toDouble()),
                Point(box.x + box.width.toDouble(), box.y + box.height.toDouble()), Scalar(255.0), -1)

            val imageMasked = Mat()
            Core.bitwise_and(imageInv, mask, imageMasked)

            masks.add(imageMasked)
        }

        return masks
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val storageDir = cacheDir
        return File.createTempFile(
            "image",
            ".jpg",
            storageDir
        )
    }

    // Function to save a Bitmap to a file
    @Throws(IOException::class)
    private fun saveBitmapToFile(bitmap: Bitmap, file: File) {
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
    }

    private suspend fun sendTestRequest() {
        runOnUiThread {
            binding.progressBar.visibility = View.VISIBLE
        }
        val delimiter = UUID.randomUUID().toString().replace("-", "")

        val bitmap = binding.imageView.getBitmapWithDrawings()

// Create a file to store the Bitmap
        val file = createImageFile()
        saveBitmapToFile(bitmap, file)


        val mediaType = "image/jpeg".toMediaTypeOrNull()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", file.name, RequestBody.create(mediaType, file))
            .addFormDataPart("text", texts.joinToString(delimiter))
            .addFormDataPart("delimiter", delimiter)
            .addFormDataPart("type", type)
            .build()

        val request = Request.Builder()
            .url("https://briefpic.me/php/1_6_openai_request_flash.php")
            .post(requestBody)
            .addHeader(
                "Authentication",
                getSharedPreferences("MyPrefs", Context.MODE_PRIVATE).getString("accessToken", "0").toString()
            )
            .build()

        val client = OkHttpClient.Builder()
            .callTimeout(20, TimeUnit.MINUTES)
            .readTimeout(20, TimeUnit.MINUTES)
            .writeTimeout(20, TimeUnit.MINUTES)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@AddPhotoFlashCardActivity,
                        "Failed to create note",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("KURWADUPA", e.message.toString())
                    binding.progressBar.visibility = View.GONE
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(
                            this@AddPhotoFlashCardActivity,
                            "Failed to create note",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d("KURWADUPA", response.body.toString())
                        binding.progressBar.visibility = View.GONE
                    }
                } else {
                    runOnUiThread {
                        binding.progress.visibility = View.VISIBLE
                    }
                    var fullResponse = ""
                    val inputStream = response.body?.byteStream()
                    inputStream?.bufferedReader()?.useLines { lines ->
                        lines.forEach { line ->
                            // Process each line of the response
                            Log.d("PHPLINE", "Received line: $line")
                            if (line.trim() != "" && line.trim() != "data: [DONE]"){
                                try {
                                    val jsonLine = line.substring(6)
                                    val text = JSONObject(jsonLine)
                                        .getJSONArray("choices")
                                        .getJSONObject(0)
                                        .getJSONObject("delta")
                                        .getString("content")
                                    fullResponse += text
                                    Log.d("PHPLINE", "Remade text: $text")
                                    runOnUiThread {
                                        binding.result.text = fullResponse
                                    }
                                } catch (e: Exception){ }
                            }
                        }
                    }
                    fullResponse = fullResponse.trimIndent()
                    Log.d("FULLRESPONSE", fullResponse)
                    runOnUiThread {
                        if (fullResponse != "") {
                            val flashcards = arrayListOf<FlashCard>()
                            Log.d("ADDTYPE", type)
                            if(type=="vocabulary"){
                                for (line in fullResponse.split("\n")) {
                                    Log.d("READLINE", line)
                                    val front = line.substringBeforeLast(":").trim('-').trim()
                                    val back = line.substringAfterLast(":").trim('-').trim()
                                    if (front.trim() != "" && back.trim() != "") {
                                        flashcards.add(FlashCard(front, back))
                                    }
                                }
                            } else {
//                                for (line in fullResponse.split("\n")) {
//                                    line.trim('-').trim()
//                                    if (line.trim() != "") {
//                                        flashcards.add(FlashCard(line, ""))
//                                    }
//                                }
                                for(line in fullResponse.split("\n\n")){
                                    val front = line.substringBefore("\n").substringAfter(":").trim()
                                    val back = line.substringAfter("\n").substringAfter(":").trim()
                                    if (front.trim() != "" && back.trim() != "") {
                                        flashcards.add(FlashCard(front, back))
                                    }
                                    Log.d("LINE", line)
                                    Log.d("LINE", front)
                                    Log.d("LINE", back)
                                }

                            }

                            val uniqueList = flashcards.distinct()
                            flashcards.clear()
                            flashcards.addAll(uniqueList)

                            Log.d("FLASH", flashcards.toString())
                            val gson: Gson = GsonBuilder().setPrettyPrinting().create()

                            // convert ArrayList to JSON string
                            jsonString = gson.toJson(flashcards)
                            Log.d("CURRENTITEM", jsonString)
                            finishAndSave(flashcards)
                        } else {
                            Toast.makeText(this@AddPhotoFlashCardActivity, "Failed to create note", Toast.LENGTH_SHORT).show()
                            Log.d("KURWADUPA", "DUPWADAWDAWD")

                        }
                    }
                }
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                }
            }
        })
    }

    private fun getTextFromImages(images: List<Mat>, countDownLatch: CountDownLatch): String {
        var text = ""
        for(image in images){
            val bitmap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888)
            matToBitmap(image, bitmap)
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener {
                    text += it.text
                    Log.d("EXTRACT PART", it.text)
                }
                .addOnCompleteListener {
                    countDownLatch.countDown()
                }
                .addOnFailureListener {
                    Log.d("EXTRACT PART", "FAILED")
                }
        }
        countDownLatch.await()
        return text
    }

    private fun getTextBoxes(image: Mat): ArrayList<Rect> {
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(kernelSize, kernelSize))

        val imageDilated = Mat()
        Imgproc.dilate(image, imageDilated, kernel, Point(-1.0, -1.0), 2)

        val bitmapP = Bitmap.createBitmap(imageDilated.width(), imageDilated.height(), Bitmap.Config.ARGB_8888)

        matToBitmap(imageDilated, bitmapP)
//        binding.imageView.setImageBitmap(bitmapP)
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(imageDilated, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        val boxes = arrayListOf<Rect>()
        for (contour in contours) {
            val boundingRect = Imgproc.boundingRect(contour)
            boxes.add(Rect(boundingRect.x, boundingRect.y, boundingRect.width, boundingRect.height))
        }

        return boxes
    }

    fun initializeImage(imageBitmap: Bitmap){
        binding.imageView.setImageBitmap(imageBitmap)

        val mat = Mat(imageBitmap.height, imageBitmap.width, CvType.CV_8UC4)
        bitmapToMat(imageBitmap, mat)
        val imageGray = Mat()
        cvtColor(mat, imageGray, Imgproc.COLOR_RGB2GRAY)

        val imageThresh = Mat()
        Imgproc.adaptiveThreshold(imageGray, imageThresh, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 101, 30.0)

        imageInv = Mat()
        Core.bitwise_not(imageThresh, imageInv)

        allBoxes = getTextBoxes(imageInv)


//        matToBitmap(mat, imageBitmap)
//        binding.imageView.setImageBitmap(imageBitmap)
    }

    private fun finishAndSave(flashcards: ArrayList<FlashCard>){
        // write JSON string to file
        FileWriter(File(cacheDir, "collections/$collectionName/elements/${position}_$elementName/main.json")).use { writer ->
            writer.write(jsonString)
        }
        val resultIntent = Intent()
        resultIntent.putExtra("customList", flashcards)

        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.progress.visibility = View.GONE
    }
}