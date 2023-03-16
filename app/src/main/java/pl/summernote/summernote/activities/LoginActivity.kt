package pl.summernote.summernote.activities

import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import pl.summernote.summernote.R
import pl.summernote.summernote.databinding.LoginActivityBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import pl.summernote.summernote.Utils

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: LoginActivityBinding
    private lateinit var utils: Utils


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide();

        utils = Utils()

        val file = File(cacheDir, "collections")
        file.mkdirs()
        val directories = file.listFiles { file -> file.isDirectory}

        directories?.forEach { directory ->
            Log.d("DIRCOL",directory.name)
        }

        val directoryName = "collections"
        val myDirectory = File(cacheDir, directoryName)

        if (!myDirectory.exists()) {
            myDirectory.mkdir()

        }

        binding.loginButton.setOnClickListener {
            val intent = Intent(this, CollectionsActivity::class.java)
            startActivity(intent)
        }
    }
}