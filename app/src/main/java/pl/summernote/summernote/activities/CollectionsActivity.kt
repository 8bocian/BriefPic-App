package pl.summernote.summernote.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.facebook.appevents.AppEventsLogger
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.summernote.summernote.R
import pl.summernote.summernote.adapters.CollectionsAdapter
import pl.summernote.summernote.customs.FlowSender
import pl.summernote.summernote.databinding.CollectionCarouselLayoutBinding
import pl.summernote.summernote.dataclasses.Collection
import pl.summernote.summernote.fragments.AddCollectionDialogFragment
import pl.summernote.summernote.fragments.AddCollectionDialogListener
import pl.summernote.summernote.fragments.ChangeCollectionDialogListener
import java.io.File
import java.util.*


class CollectionsActivity : AppCompatActivity(), AddCollectionDialogListener, ChangeCollectionDialogListener {

    private lateinit var collectionsArrayList: ArrayList<Collection>
    private lateinit var adapter: CollectionsAdapter

    private lateinit var subjectsIntent: Intent

    private lateinit var binding: CollectionCarouselLayoutBinding
    private var uuidString: String? = null

    override fun onStop() {
        super.onStop()
        Log.d("DUPAJAJ", "STOPCIOR")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CollectionCarouselLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        val sharedPrefs: SharedPreferences = this.baseContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        uuidString = sharedPrefs.getString("UUID", null)

        if (uuidString == null) {
            val uuid = UUID.randomUUID().toString()
            with(sharedPrefs.edit()) {
                putString("UUID", uuid)
                apply()
            }
        }
        uuidString = sharedPrefs.getString("UUID", null)


        subjectsIntent = Intent(this, ElementsActivity::class.java)
        val myDirectory = File(cacheDir, "collections")

        val logger = AppEventsLogger.newLogger(this)
        logger.logEvent("sentFriendRequest");

        if (!myDirectory.exists()) {
            myDirectory.mkdir()
        }
        val bottomBarBackground = binding.appBar.background as MaterialShapeDrawable
        bottomBarBackground.shapeAppearanceModel = bottomBarBackground.shapeAppearanceModel
            .toBuilder()
            .setTopLeftCorner(CornerFamily.ROUNDED, 80f)
            .setTopRightCorner(CornerFamily.ROUNDED, 80f)
            .build()

        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.setHasFixedSize(false)
        collectionsArrayList = arrayListOf()

        val directoryName = "collections"
        val subDirectory = File(cacheDir, directoryName)
        if (subDirectory.exists() && subDirectory.isDirectory) {
            val subDirectories = subDirectory.listFiles()?.filter { it.isDirectory }

            val subDirsSorted = subDirectories?.sortedByDescending { it.name }
            subDirsSorted?.forEach { subDir ->
                    Log.d("SUBDIRNAME", subDir.name)

                    val lastIndex = subDir.name.split('_').lastIndex
                    val icon: String = subDir.name.split('_')[lastIndex]
                    val name: String = subDir.name.split('_')[lastIndex-1]
                    val position: Int = subDir.name.substringBefore('_').toInt()
                    collectionsArrayList.add(0, Collection(name, icon, position))
            }
        }

        if (collectionsArrayList.isNotEmpty()){
            binding.greeting1.visibility = View.GONE
        }

        getCollections(collectionsArrayList)
        scrollToLastPosition()

        binding.addCollection.setOnClickListener{
            val popupDialogFragment = AddCollectionDialogFragment()
            popupDialogFragment.listener = this
            val args = Bundle()
            args.putStringArrayList("names", adapter.getNames())
            popupDialogFragment.arguments = args
            popupDialogFragment.show(supportFragmentManager, "popup_dialog_fragment")
        }

        binding.menuButton.setOnClickListener { menuItem ->

            val popupMenu = PopupMenu(this@CollectionsActivity, binding.menuButton)
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
                        val yesno = PopupMenu(this@CollectionsActivity, binding.menuButton)
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

    override fun onCollectionChanged(collectionNameNew: String, collectionIconNew: String, collectionNameOld: String, collectionIconOld: String, index: Int) {
        val realPosition = adapter.getItem(index)
        adapter.changeItem(collectionNameNew, collectionIconNew, index)
        changeCollection("${realPosition}_${collectionNameNew}_${collectionIconNew}", "${index}_${collectionNameOld}_${collectionIconOld}")
    }

    private fun changeCollection(collectionNameNew: String, collectionNameOld: String){
        val path = File(cacheDir, "collections/$collectionNameNew")
        val pathNew = File(cacheDir, "collections/$collectionNameOld")
        Log.d("RENAMEPATH", path.path)
        Log.d("RENAMEPATH", pathNew.path)
        pathNew.renameTo(path)

        val directory = File(cacheDir, "collections")
        val directories = directory.listFiles { file -> file.isDirectory() }
        directories.forEach {
            Log.d("RENAMEPATH", it.name)
        }
    }

    override fun onCollectionRemoved(collectionNameOld: String, collectionIconOld: String, index: Int) {
        val realPosition = adapter.getItem(index)
        adapter.removeItem(index)
        removeCollection(collectionNameOld, collectionIconOld, realPosition)
        if (adapter.itemCount == 0) binding.greeting1.visibility = View.VISIBLE
    }

    private fun removeCollection(collectionNameOld: String, collectionIconOld: String, collectionPosition: Int){
        val path = File(cacheDir, "collections/${collectionPosition}_${collectionNameOld}_${collectionIconOld}")
        path.deleteRecursively()
    }

    override fun onCollectionAdded(collectionName: String, collectionIcon: String) {
        val nextPosition = adapter.getItem(adapter.itemCount-1)+1
        adapter.addItem(collectionName, collectionIcon, nextPosition)
        saveCollection(collectionName, collectionIcon, nextPosition)
        binding.greeting1.visibility = View.GONE
    }

    private fun saveCollection(name: String, icon: String, position: Int){
        val path = File(cacheDir, "collections/${position}_${name}_${icon}")
        path.mkdir()
    }

    private fun scrollToLastPosition() {
        val lastItemIndex = binding.recyclerView.adapter!!.itemCount - 1
        binding.recyclerView.scrollToPosition(lastItemIndex)
    }

    private fun getCollections(collections: ArrayList<Collection>){
        collectionsArrayList = collections
        collectionsArrayList.forEach {
            Log.d("RESULT", it.name)
        }

        adapter = CollectionsAdapter(collectionsArrayList, supportFragmentManager, baseContext)
        adapter.setOnItemClickListener(object: CollectionsAdapter.onItemClickListener{
            override fun onItemClick(view: View, position: Int, x: Int, y: Int) {
                view.setClickable(false)
                subjectsIntent.putExtra(
                    "collectionName",
                    "${position}_${collectionsArrayList[position].name}_${collectionsArrayList[position].icon}"
                )
                startActivity(subjectsIntent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        })
        binding.recyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        val flowSender = FlowSender()
        val sharedPrefs: SharedPreferences = this@CollectionsActivity.baseContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val uuidString = sharedPrefs.getString("UUID", null)
        flowSender.sendFlowInformation(this.javaClass.simpleName, uuidString!!, "ENTER")
    }
}