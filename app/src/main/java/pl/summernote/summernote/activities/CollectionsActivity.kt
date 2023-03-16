package pl.summernote.summernote.activities

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import pl.summernote.summernote.R
import pl.summernote.summernote.Utils
import pl.summernote.summernote.adapters.CollectionsAdapter
import pl.summernote.summernote.databinding.CollectionsCarouselLayoutBinding
import pl.summernote.summernote.databinding.PopupWindowCollectionsBinding
import pl.summernote.summernote.dataclasses.Collection
import java.io.File
import kotlin.collections.ArrayList


class CollectionsActivity : AppCompatActivity() {

    private lateinit var utils: Utils


    private lateinit var collectionsArrayList: ArrayList<Collection>
    private lateinit var adapter: CollectionsAdapter
    private lateinit var elementsIntent: Intent

    private lateinit var binding: CollectionsCarouselLayoutBinding
    private lateinit var bindingPopUp: PopupWindowCollectionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CollectionsCarouselLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        utils = Utils()

        elementsIntent = Intent(this, ElementsActivity::class.java)

        binding.recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerView.setHasFixedSize(false)
        collectionsArrayList = arrayListOf()

        val directoryName = "collections"
        val subDirectory = File(cacheDir, directoryName)
        subDirectory.mkdir()
        if (subDirectory.exists() && subDirectory.isDirectory) {
            val subDirectories = subDirectory.listFiles { file ->
                file.isDirectory
            }

            for (subDir in subDirectories!!) {
                collectionsArrayList.add(Collection(subDir.name))
            }
        }
        getCollections(collectionsArrayList)
        scrollToLastPosition()
    }

    private fun scrollToLastPosition() {
        val lastItemIndex = binding.recyclerView.adapter!!.itemCount - 1
        binding.recyclerView.scrollToPosition(lastItemIndex)
    }

    private fun getCollections(collections: ArrayList<Collection>){
        collectionsArrayList = collections
        adapter = CollectionsAdapter(collectionsArrayList, cacheDir, context = baseContext)
        adapter.setOnItemClickListener(object: CollectionsAdapter.onItemClickListener{
            override fun onItemClick(view: View, position: Int, x: Int, y: Int) {
                if(position == adapter.itemCount-1) {
                    val animation = AnimationUtils.loadAnimation(view.context, R.anim.pulse)
                    view.startAnimation(animation)
                    showPopup(x, y)
                } else {
                    elementsIntent.putExtra("collectionName", collectionsArrayList[position].name)
                    startActivity(elementsIntent)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
            }

        })
        binding.recyclerView.adapter = adapter
    }

    private fun showPopup(x: Int, y: Int) {
        bindingPopUp = PopupWindowCollectionsBinding.inflate(layoutInflater)

        val popupWindow = PopupWindow(
            bindingPopUp.root,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true

        bindingPopUp.addCollectionButton.setOnClickListener {
            val name = bindingPopUp.textView.text.toString()
            var isDuplicate = false

            for(i in collectionsArrayList.indices){
                if (collectionsArrayList[i].name == name) {
                    isDuplicate = true
                    break
                }
            }

             if (name == "") {
                Toast.makeText(this, "Name of the collection must not be empty!", Toast.LENGTH_SHORT).show()
            } else if (isDuplicate){
                 Toast.makeText(this, "Name of the collection must not duplicate!", Toast.LENGTH_SHORT).show()
            } else {
                 val collectionDir = File(cacheDir, "collections/$name")
                 collectionDir.mkdirs()
                 adapter.addItem(name)
                 popupWindow.dismiss()
             }
        }

        popupWindow.showAtLocation(
            findViewById(android.R.id.content),
            Gravity.CENTER,
            x,
            y
        )
    }
}