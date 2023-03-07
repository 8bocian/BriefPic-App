package pl.summernote.summernote.activities

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
import pl.summernote.summernote.BuildConfig
import pl.summernote.summernote.R
import pl.summernote.summernote.databinding.MainActivityBinding
import pl.summernote.summernote.dataclasses.FlaskResponse
import pl.summernote.summernote.dataclasses.Point
import pl.summernote.summernote.interfaces.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_IMAGE_SELECT = 2
        const val PERMISSION_REQUEST_CODE = 3
    }

    private lateinit var photoURI: Uri
    private lateinit var imageBitmap: Bitmap

    private lateinit var binding: MainActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide();

        val addButton = binding.addButton
        addButton.setOnClickListener {
            val x = binding.imageView.width.toFloat()/2 // Get x coordinate from user input or other source
            val y = binding.imageView.height.toFloat()/2 // Get y coordinate from user input or other source
                binding.imageView.setPoint(x, y)
                binding.imageView.invalidate()
            Log.d("POINT", binding.imageView.points.toString())
        }

        binding.captureButton.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 123)
            } else {
                var intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                val photoFile = createImageFile()
                photoURI = FileProvider.getUriForFile(
                    this,
                    "${BuildConfig.APPLICATION_ID}.fileprovider",
                    photoFile
                )

                Log.w("URI", photoURI.toString())
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
            }
        }

        binding.selectButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_IMAGE_SELECT)
        }


        binding.sendButton.setOnClickListener {
            lifecycleScope.launch {
                sendRequest(imageBitmap, binding.imageView.points)
            }
        }


//        binding.imageView.setOnTouchListener {_, motionEvent ->
//            if (motionEvent.action == MotionEvent.ACTION_UP) {
//                binding.imageView.performClick()
//                val x = motionEvent.x
//                val y = motionEvent.y
//
//                binding.imageView.setPoint(x, y)
//                Log.d("CLICK", "$x, $y")
//            }
//            true
//        }
    }

    private fun createImageFile(): File {
        val timeStamp = System.currentTimeMillis()
        val imageFileName = "NAME_$timeStamp"
        val storageDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                binding.imageView.setImageURI(photoURI)
                val inputStream: InputStream? = contentResolver.openInputStream(photoURI)
                imageBitmap = BitmapFactory.decodeStream(inputStream)
            } else if (requestCode == REQUEST_IMAGE_SELECT) {
                binding.imageView.setImageURI(data?.data)
                imageBitmap = (binding.imageView.drawable as BitmapDrawable).bitmap
            }
            binding.imageView.clearPoints()
        }
    }

    private fun showPopup(text: String) {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popup_window, null)

        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val closeButton = popupView.findViewById<Button>(R.id.close_button)
        closeButton.setOnClickListener {
            popupWindow.dismiss()
        }

        val responseText = popupView.findViewById<TextView>(R.id.textView)
        responseText.text = text

        popupWindow.showAtLocation(
            findViewById<View>(R.id.main_layout),
            Gravity.CENTER,
            0,
            0
        )
    }



    suspend fun sendRequest(image: Bitmap, json: MutableList<Point>) {
        val gson = GsonBuilder()
            .setLenient()
            .create()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(6000, TimeUnit.SECONDS) // set connection timeout to 30 seconds
            .readTimeout(6000, TimeUnit.SECONDS) // set read timeout to 30 seconds
            .writeTimeout(6000, TimeUnit.SECONDS) // set write timeout to 30 seconds
            .build()

        val retrofit = Retrofit.Builder()
//            .baseUrl("http://3.71.98.173:80/")
            .baseUrl("http://192.168.0.178:80/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(okHttpClient)
            .build()

        val api = retrofit.create(ApiService::class.java)

        val byteArrayOutputStream = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val imageRequestBody = byteArrayOutputStream.toByteArray().toRequestBody("image/jpeg".toMediaTypeOrNull())

        val jsonRequestBodyPoints = Gson().toJson(json).toRequestBody("application/json".toMediaTypeOrNull())

        val ratioV: Float = imageBitmap.width.toFloat()/binding.imageView.width.toFloat()
        val ratioH: Float = imageBitmap.height.toFloat()/binding.imageView.height.toFloat()
        Log.d("SIZE", imageBitmap.height.toString())
        Log.d("SIZE", binding.imageView.width.toString())
        Log.i("REQUEST", ratioV.toString())
        Log.i("REQUEST", ratioH.toString())

        val jsonObject = JSONObject()
        jsonObject.put("ratioV", ratioV)
        jsonObject.put("ratioH", ratioV)
        val jsonRequestBodyRatio = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())


        Log.d("REQUEST", json.toString())
        val response = withContext(Dispatchers.IO) {
            api.uploadImage(
                MultipartBody.Part.createFormData("image", "image.jpg", imageRequestBody),
                jsonRequestBodyPoints,
                jsonRequestBodyRatio
            )
        }
//        val responseBody = response.parseAs<ResponseBody>(gson)
//        Log.d("RESPONSE", responseBody.toString())
        showPopup(response)

//        if (response.isSuccessful) {
//            Log.d("RESPONSE", response.toString())
//            val responseBody = response.body()
//            Log.d("RESPONSE", responseBody.toString())
//        } else {
//            Log.d("RESPONSE", response.errorBody().toString())
//        }
    }
}