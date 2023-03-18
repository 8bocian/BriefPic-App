package pl.summernote.summernote.adapters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import pl.summernote.summernote.R
import pl.summernote.summernote.Utils
import pl.summernote.summernote.dataclasses.Collection
import pl.summernote.summernote.dataclasses.Element
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


class ElementsAdapter(
    private val elementsList: ArrayList<Element>,
    private val cacheDir: File,
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var nListener: onItemClickListener

    init {
        var exists = false
        for (collection in elementsList) {
            if (collection.imagePath == "predef/blank.jpeg") {
                exists = true
                break
            }
        }
        if (!exists) {
            elementsList.add(Element("predef/blank.jpeg"))
        }
    }

    interface onItemClickListener {
        fun onItemClick(view: View, position: Int, x: Int, y: Int)
    }

    fun setOnItemClickListener(listener: onItemClickListener) {
        nListener = listener
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == elementsList.size - 1) {
            VIEW_TYPE_ADD
        } else {
            VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val itemView = when (viewType) {
            VIEW_TYPE_ADD -> LayoutInflater.from(parent.context)
                .inflate(R.layout.element_layout, parent, false)
            else -> LayoutInflater.from(parent.context)
                .inflate(R.layout.element_layout, parent, false)
        }
        return ViewHolder(itemView, nListener)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder.itemView.isLongClickable = true;
        if (position != elementsList.size - 1) {
            val currentItem = elementsList[position]
            val file1 = File(cacheDir, currentItem.imagePath)
            val file = File(file1.path)
            val fis = FileInputStream(file)
            val bitmap = BitmapFactory.decodeStream(fis)
            (holder as ViewHolder).imageView.setImageBitmap(Utils().rotateImage(bitmap))
        }
    }

    override fun getItemCount(): Int {
        return elementsList.size
    }

    fun addItem(path: String) {
        elementsList.add(elementsList.size - 1, Element(path))
        notifyItemInserted(elementsList.size - 2)
    }

    fun removeItem(position: Int) {
        elementsList.removeAt(position)
        notifyItemRemoved(position)
    }

    inner class ViewHolder(itemView: View, listener: onItemClickListener) :
        RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.image_view)

        init {
            itemView.setOnClickListener { view ->
                listener.onItemClick(view, adapterPosition, 0, 0)
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_ADD = 1
    }
}

