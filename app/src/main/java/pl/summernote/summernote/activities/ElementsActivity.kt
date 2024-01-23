package pl.summernote.summernote.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.appcompat.widget.PopupMenu
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import pl.summernote.summernote.R
import pl.summernote.summernote.adapters.ElementsAdapter
import pl.summernote.summernote.adapters.OnListChangedListener
import pl.summernote.summernote.customs.FlowSender
import pl.summernote.summernote.databinding.ElementsCarouselLayoutBinding
import pl.summernote.summernote.dataclasses.Element
import pl.summernote.summernote.fragments.AddElementDialogFragment
import pl.summernote.summernote.fragments.AddElementDialogListener
import pl.summernote.summernote.fragments.ChangeElementDialogListener
import java.io.File
import java.util.concurrent.TimeUnit


class ElementsActivity : AppCompatActivity(), AddElementDialogListener, ChangeElementDialogListener {

    private lateinit var elementsArrayList: ArrayList<Element>
    private lateinit var adapter: ElementsAdapter
    private lateinit var elementsIntent: Intent

    private lateinit var collectionName: String

    private lateinit var binding: ElementsCarouselLayoutBinding

    override fun onResume() {
        super.onResume()
        val flowSender = FlowSender()
        val sharedPrefs: SharedPreferences = this@ElementsActivity.baseContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val uuidString = sharedPrefs.getString("UUID", null)
        flowSender.sendFlowInformation(this.javaClass.simpleName, uuidString!!, "ENTER")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ElementsCarouselLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.navigationBarColor = resources.getColor(R.color.appBar)
        supportActionBar?.hide()

        collectionName = intent.getStringExtra("collectionName").toString()
        Log.d("NAMECHECK", collectionName)
        val lastIndex = collectionName.split('_').lastIndex
        val icon: String = collectionName.split('_')[lastIndex]
        val name: String = collectionName.split('_')[lastIndex-1]
        val collectionPosition: Int = collectionName.substringBefore("_").toInt()

//        binding.icon.setImageResource(resources.getIdentifier(icon, "drawable", "pl.summernote.summernote"))
        if (getFirstEmoji(icon) == null){
            binding.icon.text = "\uD83C\uDDEC\uD83C\uDDE7"
        } else {
            binding.icon.text = icon
        }
        binding.title.text = name
//        binding.title.setTextColor(color)
//        binding.study.backgroundTintList = ColorStateList.valueOf(color)

        val bottomBarBackground = binding.appBar.background as MaterialShapeDrawable
        bottomBarBackground.shapeAppearanceModel = bottomBarBackground.shapeAppearanceModel
            .toBuilder()
            .setTopLeftCorner(CornerFamily.ROUNDED, 80f)
            .setTopRightCorner(CornerFamily.ROUNDED, 80f)
            .build()

        elementsIntent = Intent(this, AddActivity::class.java)

        binding.recyclerView.layoutManager = LinearLayoutManager(this@ElementsActivity, LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.isNestedScrollingEnabled = false

        binding.menuButton.setOnClickListener { menuItem ->

            val popupMenu = PopupMenu(this@ElementsActivity, binding.menuButton)
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
                        val yesno = PopupMenu(this@ElementsActivity, binding.menuButton)
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

        elementsArrayList = arrayListOf()

        val directoryName = "collections/$collectionName/elements"
        val subDirectory = File(cacheDir, directoryName)

        subDirectory.mkdir()

        if (subDirectory.exists() && subDirectory.isDirectory) {
            val subDirs = subDirectory.listFiles { file ->
                file.isDirectory
            }
            val subDirsSorted = subDirs.sortedByDescending { it.name }
            val subDirsReversed = subDirsSorted.reversed()
            for (subDir in subDirsReversed) {
                elementsArrayList.add(Element(subDir.name.substringAfter("_"), subDir.name.substringBefore("_").toInt()))
                Log.d("TESTY", subDir.name)
            }
        }

        if (elementsArrayList.isNotEmpty()){
            binding.greeting2.visibility = View.GONE
        }

        getElements(elementsArrayList)

        binding.recyclerView.adapter = adapter
        binding.addElement.setOnClickListener {
            val popupDialogFragment = AddElementDialogFragment()
            val args = Bundle()
            args.putStringArrayList("names", adapter.getNames())
            popupDialogFragment.arguments = args
            popupDialogFragment.listener = this@ElementsActivity
            popupDialogFragment.show(supportFragmentManager, "popup_dialog_fragment")
        }
        adapter.setOnListChangedListener(MyListChangedListener(binding))
        binding.elementsCount.text = "sets: ${adapter.itemCount}"
        binding.recyclerView.scrollToPosition(adapter.itemCount - 1)
    }

    private fun getFirstEmoji(input: String): String? {
        val emojiRegex = "[\\p{So}\\uD83C\\uDDE6-\\uD83C\\uDDFF]{2}"
        val matchResult = emojiRegex.toRegex().find(input)

        val emojiRegex2 = "[\\p{So}]"
        val matchResult2 = emojiRegex2.toRegex().find(input)
        Log.d("DUPAEMOJI", matchResult?.value.toString())
        Log.d("DUPAEMOJI", matchResult2?.value.toString())
        if (matchResult?.value != null){
            return matchResult.value
        } else {
            return matchResult2?.value
        }
    }

    class MyListChangedListener(private val binding: ElementsCarouselLayoutBinding) : OnListChangedListener {
        override fun onListChanged(newListSize: Int) {
            binding.elementsCount.text = "sets: $newListSize"
        }
    }
    override fun onElementAdded(elementName: String) {
        val nextPosition = adapter.getItem(adapter.itemCount-1)+1
        adapter.addItem(Element(elementName, nextPosition))
        saveElement(nextPosition, elementName)
        binding.greeting2.visibility = View.GONE
    }

    private fun saveElement(position: Int, elementName: String){

        val f = File(cacheDir, "collections/$collectionName/elements/${position}_${elementName}")
        f.mkdir()
    }

    override fun onPause() {
        super.onPause()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun getElements(elements: ArrayList<Element>){
        elementsArrayList = elements
        adapter = ElementsAdapter(elementsArrayList, cacheDir, baseContext, collectionName, this.supportFragmentManager)
        adapter.setOnItemClickListener(object: ElementsAdapter.onItemClickListener{

            override fun onItemClick(view: View, position: Int, x: Int, y: Int) {
                elementsIntent.putExtra("collectionName", collectionName)
                elementsIntent.putExtra("elementName", elementsArrayList[position].name)
                elementsIntent.putExtra("position", elementsArrayList[position].position)

                startActivity(elementsIntent)
            }

        })
        binding.recyclerView.adapter = adapter
    }

    override fun onElementChanged(elementNameNew: String, elementNameOld: String, index: Int) {
        val realPosition = adapter.getItem(index)
        adapter.changeItem(elementNameNew, index)
        changeElement(elementNameNew, elementNameOld, realPosition)
    }

    private fun changeElement(elementNameNew: String, elementNameOld: String, position: Int){
        val path = File(cacheDir, "collections/$collectionName/elements/${position}_${elementNameOld}")
        val pathNew = File(cacheDir, "collections/$collectionName/elements/${position}_${elementNameNew}")
        Log.d("RENAMEPATH", path.path)
        Log.d("RENAMEPATH", pathNew.path)
        path.renameTo(pathNew)

        val directory = File(cacheDir, "collections")
        val directories = directory.listFiles { file -> file.isDirectory() }
        directories.forEach {
            Log.d("RENAMEPATH", it.name)
        }
    }

    override fun onElementRemoved(elementNameOld: String, index: Int) {
        val realPosition = adapter.getItem(index)
        adapter.removeItem(index)
        removeElement(elementNameOld, realPosition)
        if (adapter.itemCount == 0) binding.greeting2.visibility = View.VISIBLE
    }

    private fun removeElement(elementNameOld: String, position: Int){
        val path = File(cacheDir, "collections/$collectionName/elements/${position}_${elementNameOld}")
        Log.d("DELETETEST", path.path)
        path.deleteRecursively()
    }
}