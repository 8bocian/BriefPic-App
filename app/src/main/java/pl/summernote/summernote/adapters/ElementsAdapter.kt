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
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pl.summernote.summernote.R
import pl.summernote.summernote.Utils
import pl.summernote.summernote.dataclasses.Collection
import pl.summernote.summernote.dataclasses.Element
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


class ElementsAdapter(private val elementsList: ArrayList<Element>,
                         private val cacheDir: File, private val context: Context) :
    RecyclerView.Adapter<ElementsAdapter.ViewHolder>() {

    private lateinit var nListener: onItemClickListener
    private var lastPosition = elementsList.size - 1

    init {
        var exists = false
        for(collection in elementsList){
            if(collection.imagePath == "predef/blank.jpeg"){
                exists = true
                break
            }
        }
        if (!exists) {
            elementsList.add(Element("predef/blank.jpeg"))
            lastPosition = elementsList.size - 1
        }
    }



    interface onItemClickListener {
        fun onItemClick(view: View, position: Int, x: Int, y: Int)
    }

    fun setOnItemClickListener(listener: onItemClickListener){
        nListener = listener
    }

    class ViewHolder(itemView: View, listener: onItemClickListener, lastPosition: Int) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.image_view)

        init {
            itemView.setOnClickListener{view ->
                listener.onItemClick(view, adapterPosition, 0, 0)
            }
        }
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.element_layout, parent, false)
        return ViewHolder(itemView, nListener, lastPosition)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if(position != elementsList.size-1) {

            val currentItem = elementsList[position]

            val file1 = File(cacheDir, currentItem.imagePath)
            val file = File(file1.path)
            val fis = FileInputStream(file)
            val bitmap = BitmapFactory.decodeStream(fis)

            holder.imageView.setImageBitmap(Utils().rotateImage(bitmap))
        } else {
            holder.imageView.setBackgroundResource(R.drawable.add_blank)
        }
    }

    fun addItem(path: String) {
        elementsList.add(elementsList.size - 1, Element(path))
        notifyItemInserted(elementsList.size - 2)
    }

    override fun getItemCount(): Int {
        return elementsList.size
    }
}
