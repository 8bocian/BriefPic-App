package pl.summernote.summernote.activities

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
//import com.google.android.gms.auth.api.signin.GoogleSignIn
//import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.RelativeCornerSize
import com.google.android.material.shape.RoundedCornerTreatment
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import pl.summernote.summernote.R
import pl.summernote.summernote.adapters.AddAdapter
import pl.summernote.summernote.adapters.OnListChangedListener
import pl.summernote.summernote.customs.FlowSender
import pl.summernote.summernote.databinding.AddActivityBinding
import pl.summernote.summernote.dataclasses.FlashCard
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


class AddActivity : AppCompatActivity() {

    companion object {
        const val CAMERA_REQUEST = 1
        const val SELECT_REQUEST = 2
        const val JSON_RECEIVE = 3
    }

    private lateinit var adapter: AddAdapter
    private lateinit var collectionName: String
    private lateinit var elementName: String
    private lateinit var collectionType: String
    private lateinit var flashcardsIntent: Intent
    private lateinit var photoURI: Uri

    private var flashcardsArrayList: ArrayList<FlashCard> = arrayListOf()
    private var position = 0

    private lateinit var binding: AddActivityBinding

    override fun onResume() {
        super.onResume()
        val flowSender = FlowSender()
        val sharedPrefs: SharedPreferences = this@AddActivity.baseContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val uuidString = sharedPrefs.getString("UUID", null)
        flowSender.sendFlowInformation(this.javaClass.simpleName, uuidString!!, "ENTER")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AddActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        collectionName = intent.getStringExtra("collectionName").toString()
        position = intent.getIntExtra("position", 0)
        elementName = intent.getStringExtra("elementName").toString()
        collectionType = intent.getStringExtra("type").toString()

        flashcardsIntent = Intent(this, FlashCardsActivity::class.java)
        flashcardsIntent.putExtra("collectionType", collectionType)

        val bottomBarBackground = binding.appBar.background as MaterialShapeDrawable
        bottomBarBackground.shapeAppearanceModel = bottomBarBackground.shapeAppearanceModel
            .toBuilder()
            .setTopLeftCorner(CornerFamily.ROUNDED, 80f)
            .setTopRightCorner(CornerFamily.ROUNDED, 80f)
            .build()
        binding.title.text = elementName

        binding.addFlashcard.setOnClickListener {
            val popupMenu = PopupMenu(this@AddActivity, binding.addFlashcard)
            popupMenu.menuInflater.inflate(R.menu.menu_add_flashcards, popupMenu.menu)
            popupMenu.menu.findItem(R.id.add_flashcards_via_photo).setIcon(R.drawable.ic_baseline_image_24)
            popupMenu.menu.findItem(R.id.add_flashcards_via_element).setIcon(R.drawable.ic_baseline_create_24)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.add_flashcards_via_photo -> {
                        val popupMenu2 = PopupMenu(this@AddActivity, binding.addFlashcard)
                        popupMenu2.menuInflater.inflate(R.menu.menu_chose_action, popupMenu2.menu)
                        popupMenu2.setOnMenuItemClickListener { item2 ->
                            when (item2.itemId){
                                R.id.select -> {
                                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                                    intent.type = "image/*"
                                    val flowSender = FlowSender()
                                    val sharedPrefs: SharedPreferences = this@AddActivity.baseContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                                    val uuidString = sharedPrefs.getString("UUID", null)
                                    flowSender.sendFlowInformation("SelectPhotoActivity", uuidString!!, "ENTER")
                                    startActivityForResult(intent, SELECT_REQUEST)
                                    true
                                }
                                R.id.take -> {
                                    getFromPhoto()
                                    true
                                }
                                else -> false
                            }
                        }
                        popupMenu2.show()
                        true
                    }
                    R.id.add_flashcards_via_element -> {
                        adapter.addItem(FlashCard("", ""))
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }

        binding.study.setOnClickListener{
            saveFlashcards()
            flashcardsIntent.putParcelableArrayListExtra("flash_cards", flashcardsArrayList)
            val popupMenu = PopupMenu(this@AddActivity, binding.study)
            Log.d("LERTYP", if(collectionType=="notes") "R.menu.menu_learning_types_reduced" else "R.menu.menu_learning_types")
            Log.d("LERTYP", collectionType)
            if(adapter.itemCount != 0) {
                popupMenu.menuInflater.inflate(
                    if (collectionType.toLowerCase() == "notes") R.menu.menu_learning_types_reduced else R.menu.menu_learning_types,
                    popupMenu.menu
                )
                popupMenu.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.review -> {
                            flashcardsIntent.putExtra("type", R.id.review)
                            startActivity(flashcardsIntent)
                            true
                        }
                        R.id.write -> {
                            flashcardsIntent.putExtra("type", R.id.write)
                            startActivity(flashcardsIntent)
                            true
                        }
                        R.id.multiple_choice -> {
                            flashcardsIntent.putExtra("type", R.id.multiple_choice)
                            startActivity(flashcardsIntent)
                            true
                        }
                        R.id.yes_no -> {
                            flashcardsIntent.putExtra("type", R.id.yes_no)
                            startActivity(flashcardsIntent)
                            true
                        }
                        else -> false
                    }
                }
                popupMenu.show()
            } else {
                Toast.makeText(this@AddActivity, "No flashcards to learn from", Toast.LENGTH_SHORT).show()
            }
        }
        flashcardsArrayList = readJsonFromFile(File(cacheDir, "collections/$collectionName/elements/${position}_$elementName/main.json"))
        Log.d("CURRENTITEM", flashcardsArrayList.toString())

        binding.recyclerView.layoutManager = LinearLayoutManager(this@AddActivity, LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.isNestedScrollingEnabled = false
        getFlashCards(flashcardsArrayList)
        adapter.setOnListChangedListener(MyListChangedListener(binding))
        binding.elementsCount.text = "flashcards: ${adapter.itemCount}"
        if (adapter.itemCount == 0) {
            binding.greeting3.visibility = View.VISIBLE
        } else {
            binding.greeting3.visibility = View.GONE
        }

        binding.menuButton.setOnClickListener { menuItem ->

            val popupMenu = PopupMenu(this@AddActivity, binding.menuButton)
            popupMenu.menuInflater.inflate(R.menu.menu_drawer, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_item_logout -> {
                        finish()
                        true
                    }
                    R.id.menu_item_licenses -> {
                        //                    val intent = Intent(this, LicensesActivity::class.java)
                        //                    startActivity(intent)
                        startActivity(Intent(this, OssLicensesMenuActivity::class.java))
                        true
                    }
                    R.id.menu_item_delete -> {
                        val yesno = PopupMenu(this@AddActivity, binding.menuButton)
                        yesno.menuInflater.inflate(R.menu.menu_yes_no, yesno.menu)
                        yesno.setOnMenuItemClickListener { item ->
                            when (item.itemId) {
                                R.id.confirm -> {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                    }
                                    true
                                }
                                R.id.decline -> {
                                    true
                                }
                                else -> {false}
                            }
                        }
                        yesno.show()
                        true
                    }
                    else -> {false}
                }
            }
            popupMenu.show()
        }
        binding.recyclerView.scrollToPosition(adapter.itemCount - 1)
    }

    class MyListChangedListener(private val binding: AddActivityBinding) : OnListChangedListener {
        override fun onListChanged(newListSize: Int) {
            binding.elementsCount.text = "flashcards: $newListSize"
            if (newListSize == 0) {
                binding.greeting3.visibility = View.VISIBLE
            } else {
                binding.greeting3.visibility = View.GONE
            }

        }
    }

    private fun getFromPhoto(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val imageFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "JPEG_${System.currentTimeMillis()}.jpg")
            photoURI = FileProvider.getUriForFile(this, "pl.summernote.summernote.fileprovider", imageFile)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            val flowSender = FlowSender()
            val sharedPrefs: SharedPreferences = this@AddActivity.baseContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            val uuidString = sharedPrefs.getString("UUID", null)
            flowSender.sendFlowInformation("TakePhotoActivity", uuidString!!, "ENTER")
            startActivityForResult(cameraIntent, CAMERA_REQUEST)
        } else {
            val flowSender = FlowSender()
            val sharedPrefs: SharedPreferences = this@AddActivity.baseContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            val uuidString = sharedPrefs.getString("UUID", null)
            flowSender.sendFlowInformation("RequestTakePhotoActivity", uuidString!!, "ENTER")
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 321)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                CAMERA_REQUEST -> {
                    val imageBitmap = MediaStore.Images.Media.getBitmap(contentResolver, photoURI)
                    saveBitmap(imageBitmap, "temp.jpg")

                    val photoIntent = Intent(this@AddActivity, AddPhotoFlashCardActivity::class.java)
                    photoIntent.putExtra("bitmapPath", File(cacheDir, "temp/temp.jpg").absolutePath)
                    photoIntent.putExtra("collectionName", collectionName)
                    photoIntent.putExtra("elementName", elementName)
                    photoIntent.putExtra("position", position)
                    photoIntent.putExtra("type", collectionType)
                    photoIntent.putParcelableArrayListExtra("array", adapter.getItems())
                    startActivityForResult(photoIntent, JSON_RECEIVE)
                }
                JSON_RECEIVE -> {
                    val flashcardsArrayListFromActivity = (data?.getSerializableExtra("customList") as? ArrayList<FlashCard>)!!
                    flashcardsArrayList.addAll(flashcardsArrayListFromActivity)
                    adapter.setItems(flashcardsArrayList)
                    Log.d("CUSTOMLIST", flashcardsArrayList.toString())
                    saveFlashcards()
                }
                SELECT_REQUEST -> {
                    val selectedImageUri: Uri? = data?.data
                    val imageBitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedImageUri)
                    saveBitmap(imageBitmap, "temp.jpg")

                    val photoIntent = Intent(this@AddActivity, AddPhotoFlashCardActivity::class.java)
                    photoIntent.putExtra("bitmapPath", File(cacheDir, "temp/temp.jpg").absolutePath)
                    photoIntent.putExtra("collectionName", collectionName)
                    photoIntent.putExtra("elementName", elementName)
                    photoIntent.putExtra("position", position)
                    photoIntent.putExtra("type", collectionType)
                    photoIntent.putParcelableArrayListExtra("array", adapter.getItems())
                    startActivityForResult(photoIntent, JSON_RECEIVE)
                }
            }
        }
    }

    fun saveBitmap(bitmap: Bitmap, fileName: String) {
        val width = bitmap.width
        val height = bitmap.height

        val newWidth = (width * 0.5).toInt()
        val newHeight = (height * 0.5).toInt()

        val bitmapN = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false)

        val folder = File(cacheDir, "temp")
        if (!folder.exists()) {
            folder.mkdirs()
        }

        val file = File(folder, fileName)
        try {
            val stream = FileOutputStream(file)
            bitmapN.compress(Bitmap.CompressFormat.JPEG, 60, stream)
            stream.flush()
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun getFlashCards(elements: ArrayList<FlashCard>){
        flashcardsArrayList = elements
        adapter = AddAdapter(flashcardsArrayList, cacheDir, baseContext, "collections/$collectionName/elements/${position}_$elementName/main.json", collectionType)
        binding.recyclerView.adapter = adapter
    }

    fun readJsonFromFile(file: File): ArrayList<FlashCard> {
        Log.d("ARRAYLIST", file.path)
        if (!file.exists()) {
            file.createNewFile()
            return arrayListOf()
        }
        Log.d("CURRENTITEM", file.readText())
        if (file.readText().isEmpty()){
            return arrayListOf()
        }
        val gson = Gson()
        val type = object : TypeToken<ArrayList<FlashCard>>() {}.type
        return try {
            gson.fromJson(file.readText(), type)
        } catch (e: java.lang.Exception){
            Log.d("ERROR", file.readText())
            gson.fromJson(file.readText(), type)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        saveFlashcards()
    }

    private fun saveFlashcards(){
        val gson: Gson = GsonBuilder().setPrettyPrinting().create()

        // convert ArrayList to JSON string
        val jsonString: String = gson.toJson(adapter.getItems())
        Log.d("CURRENTITEM", jsonString)
        // write JSON string to file
        FileWriter(File(cacheDir, "collections/$collectionName/elements/${position}_$elementName/main.json")).use { writer ->
            writer.write(jsonString)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}