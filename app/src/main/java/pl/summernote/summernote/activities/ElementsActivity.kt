package pl.summernote.summernote.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import pl.summernote.summernote.BuildConfig
import pl.summernote.summernote.R
import pl.summernote.summernote.Utils
import pl.summernote.summernote.adapters.ElementsAdapter
import pl.summernote.summernote.databinding.ElementsCarouselLayoutBinding
import pl.summernote.summernote.dataclasses.Element
import pl.summernote.summernote.fragments.BottomSheetFragment
import pl.summernote.summernote.interfaces.ApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*
import java.util.concurrent.TimeUnit

class ElementsActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_IMAGE_SELECT = 2
    }

    private lateinit var utils: Utils


    private lateinit var elementsArrayList: ArrayList<Element>
    private lateinit var photoURI: Uri
    private lateinit var imageBitmap: Bitmap
    private lateinit var adapter: ElementsAdapter
    private lateinit var elementsIntent: Intent
    private lateinit var bottomSheetFragment: BottomSheetFragment

    private lateinit var collectionName: String
    private var lengthValue: Float = 0f

    private lateinit var binding: ElementsCarouselLayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ElementsCarouselLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        collectionName = intent.getStringExtra("collectionName").toString()

        supportActionBar?.hide()

        utils = Utils()

        elementsIntent = Intent(this, MainActivity::class.java)

        binding.recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerView.setHasFixedSize(false)
        binding.recyclerView.isNestedScrollingEnabled = false
        val bundle = Bundle().apply {
            putString("textPath", "$cacheDir/collections/$collectionName/texts/main.txt")
        }

        bottomSheetFragment = BottomSheetFragment()
        bottomSheetFragment.arguments = bundle

        binding.showNoteButton.setOnClickListener {
            if(File("$cacheDir/collections/$collectionName/texts/main.txt").exists()) {
                bottomSheetFragment.show(supportFragmentManager, "noteTag")
            }
        }

        binding.createNoteButton.setOnClickListener {
            var fullNotesText = ""
            val path = File("$cacheDir/collections/$collectionName/texts")
            val txtFiles = path.listFiles { file ->
                file.isFile && file.extension == "txt"
            }

            for (file in txtFiles) {
                fullNotesText += file.readText()
                Log.d("TXT", file.name)
                Log.d("TXT", file.readText())
            }
            lifecycleScope.launch(Dispatchers.IO) {
                sendRequest(fullNotesText)
            }

        }

        binding.lengthSlider.addOnChangeListener{ slider, value, fromUser ->
            lengthValue = value
        }

        elementsArrayList = arrayListOf()



        val directoryName = "collections/$collectionName/images"
        val subDirectory = File(cacheDir, directoryName)
        subDirectory.mkdirs()

        if (subDirectory.exists() && subDirectory.isDirectory) {
            val subFiles = subDirectory.listFiles { file ->
                file.isFile
            }

            for (subFile in subFiles) {
                elementsArrayList.add(Element("collections/$collectionName/images/${subFile.name}"))
            }
        }
        getElements(elementsArrayList)
        scrollToLastPosition()
    }

    private suspend fun sendRequest(text: String) {
        val gson = GsonBuilder()
            .setLenient()
            .create()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(6000, TimeUnit.SECONDS) // set connection timeout to 30 seconds
            .readTimeout(6000, TimeUnit.SECONDS) // set read timeout to 30 seconds
            .writeTimeout(6000, TimeUnit.SECONDS) // set write timeout to 30 seconds
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://3.71.98.173:80/")
//            .baseUrl("http://192.168.0.178:80/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(okHttpClient)
            .build()

        val api = retrofit.create(ApiService::class.java)

        val textJson = JSONObject()
        textJson.put("text", text)
        val jsonRequestBodyText = textJson.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val length = JSONObject()
        length.put("length", lengthValue)
        val jsonRequestBodyLength = length.toString().toRequestBody("application/json".toMediaTypeOrNull())


        val response = withContext(Dispatchers.IO) {
            api.uploadImage(
                jsonRequestBodyText,
                jsonRequestBodyLength
            )
        }
        withContext(Dispatchers.Main) {
            saveText(response)
            bottomSheetFragment.show(supportFragmentManager, "noteTag")
        }
    }

    private fun saveText(text: String) {
        val path = "$cacheDir/collections/$collectionName/texts"
        File(path).mkdirs()
        val filename = "main.txt"
        val file = File(path, filename)
        file.writeText(text)
        Log.d("SAVE", file.path)
    }

    override fun onPause() {
        super.onPause()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun scrollToLastPosition() {
        val lastItemIndex = binding.recyclerView.adapter!!.itemCount - 1
        binding.recyclerView.scrollToPosition(lastItemIndex)
    }

    private fun getElements(elements: ArrayList<Element>){
        elementsArrayList = elements
        adapter = ElementsAdapter(elementsArrayList, cacheDir, context = baseContext)
        adapter.setOnItemClickListener(object: ElementsAdapter.onItemClickListener{

            override fun onItemClick(view: View, position: Int, x: Int, y: Int) {
                if (position == adapter.itemCount - 1) {
                    if (ActivityCompat.checkSelfPermission(this@ElementsActivity, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this@ElementsActivity, arrayOf(android.Manifest.permission.CAMERA), 123)
                    } else {
                        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        val photoFile = createImageFile()
                        photoURI = FileProvider.getUriForFile(
                            this@ElementsActivity,
                            "${BuildConfig.APPLICATION_ID}.fileprovider",
                            photoFile
                        )
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)

                        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            this@ElementsActivity,
                            androidx.core.util.Pair(
                                view.findViewById(R.id.image_view),
                                ViewCompat.getTransitionName(view.findViewById(R.id.image_view))
                            )
                        )

                        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE, options.toBundle())
                    }
                } else {
                    val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        this@ElementsActivity,
                        androidx.core.util.Pair(
                            view.findViewById(R.id.image_view),
                            ViewCompat.getTransitionName(view.findViewById(R.id.image_view))
                        )
                    )

                    elementsIntent.putExtra("imagePath", elementsArrayList[position].imagePath)
                    elementsIntent.putExtra("collectionName", collectionName)
                    elementsIntent.putExtra("position", position)
                    startActivity(elementsIntent, options.toBundle())
                }
            }

        })
        binding.recyclerView.adapter = adapter
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

                val inputStream: InputStream? = contentResolver.openInputStream(photoURI)
                imageBitmap = BitmapFactory.decodeStream(inputStream)
                val collectionDir = File(cacheDir, "collections/$collectionName/images")
                val collectionDirFiles = collectionDir.listFiles()
                val numberOfFile = (collectionDirFiles?.size ?: 0) + 1

                utils.saveImage("collections/$collectionName/images/$numberOfFile.png", resizeBitmap(imageBitmap, 0.6f), cacheDir)
                val dir = File(cacheDir, "collections/$collectionName/images")
                val files = dir.listFiles()
                if (files != null) {
                    for (file in files) {
                        Log.d("DIR FILE SAVED", file.name)
                    }
                } else {
                    Log.d("DIR FILE SAVED", "EMPTY")
                }
                adapter.addItem("collections/$collectionName/images/$numberOfFile.png")
            } else if (requestCode == REQUEST_IMAGE_SELECT) {
                val inputStream: InputStream? =
                    data?.data?.let { contentResolver.openInputStream(it) }
                imageBitmap = BitmapFactory.decodeStream(inputStream)
            }
        }
    }
    fun resizeBitmap(bitmap: Bitmap, scale: Float): Bitmap {
        val width = (scale * bitmap.width).toInt()
        val height = (scale * bitmap.height).toInt()

        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}