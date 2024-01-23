package pl.summernote.summernote.adapters

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import pl.summernote.summernote.R
import pl.summernote.summernote.dataclasses.Element
import pl.summernote.summernote.dataclasses.FlashCard
import pl.summernote.summernote.fragments.ChangeElementDialogFragment
import java.io.File

interface OnElementsListChangedListener {
    fun onListChanged(newListSize: Int)
}

class ElementsAdapter(
    private var elementsList: ArrayList<Element>,
    private val cacheDir: File,
    private val context: Context,
    private val collectionName: String,
    private val fragmentManager: FragmentManager,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var nListener: onItemClickListener
    private var onListChangedListener: OnListChangedListener? = null

    interface onItemClickListener {
        fun onItemClick(view: View, position: Int, x: Int, y: Int)
    }
    fun setOnListChangedListener(listener: OnListChangedListener) {
        onListChangedListener = listener
    }
    fun setOnItemClickListener(listener: onItemClickListener) {
        nListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.element_layout, parent, false)
        return ViewHolder(itemView, nListener)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder.itemView.isLongClickable = true
        val currentItem = elementsList[position]
        val name: TextView = holder.itemView.findViewById(R.id.element_name)
        val count: TextView = holder.itemView.findViewById(R.id.flashcards_count)

        name.text = currentItem.name
        val flashcardsArrayList = readJsonFromFile(File(cacheDir, "collections/$collectionName/elements/${currentItem.position}_${currentItem.name}/main.json"))
        Log.d("LICZNIK", flashcardsArrayList.toString())
        count.text = "flashcards: ${flashcardsArrayList.size}"
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
            arrayListOf<FlashCard>()
        }
    }

    fun getNames(): ArrayList<String>{
        val names = arrayListOf<String>()
        elementsList.forEach {
            names.add(it.name)
        }
        return names
    }

    override fun getItemCount(): Int {
        return elementsList.size
    }

    fun getItem(position: Int): Int {
        return if (elementsList.isEmpty()) {
            -1
        } else {
            elementsList[position].position
        }
    }

    fun addItem(element: Element) {
        elementsList.add(element)
        notifyItemInserted(elementsList.size)
        onListChangedListener?.onListChanged(elementsList.size)
    }

    fun removeItem(position: Int) {
        elementsList.removeAt(position)
        notifyItemRemoved(position)
        onListChangedListener?.onListChanged(elementsList.size)
    }

    fun changeItem(elementNameNew: String, index: Int){
        elementsList[index].name = elementNameNew
        notifyItemChanged(index)
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View, listener: onItemClickListener) :
        RecyclerView.ViewHolder(itemView), View.OnLongClickListener {
        val imageView: ImageView = itemView.findViewById(R.id.image_view)
        val elementName: TextView = itemView.findViewById(R.id.element_name)
        init {
            itemView.setOnClickListener { view ->
                listener.onItemClick(view, adapterPosition, 0, 0)
            }
            itemView.setOnLongClickListener {
                val position = adapterPosition
                val dialogFragment = ChangeElementDialogFragment()
                val args = Bundle()
                val flashcardsCount = readJsonFromFile(File(cacheDir, "collections/$collectionName/elements/${elementsList[adapterPosition].position}_${elementsList[adapterPosition].name}/main.json")).size.toString()

                Log.d("COUNTTEXT", flashcardsCount)
                args.putString("count", flashcardsCount)
                args.putString("name", elementName.text.toString())
                args.putStringArrayList("names", getNames())
                args.putInt("index", position)
                dialogFragment.arguments = args
                dialogFragment.show(fragmentManager, "MyDialogFragment")
                true
            }
        }
        fun getNames(): ArrayList<String>{
            val names = arrayListOf<String>()
            elementsList.forEach {
                names.add(it.name)
            }
            names.remove(elementName.text.toString())
            return names
        }

        override fun onLongClick(v: View?): Boolean {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val dialogFragment = ChangeElementDialogFragment()
                dialogFragment.show(fragmentManager, "MyDialogFragment")
                return true
            }
            return false
        }
    }
}

