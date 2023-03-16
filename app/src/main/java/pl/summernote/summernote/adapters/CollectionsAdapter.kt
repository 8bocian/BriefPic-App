package pl.summernote.summernote.adapters

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import pl.summernote.summernote.R
import pl.summernote.summernote.dataclasses.Collection
import java.io.File


class CollectionsAdapter(private val collectionsList: ArrayList<Collection>,
                         private val cacheDir: File, private val context: Context) :
    RecyclerView.Adapter<CollectionsAdapter.ViewHolder>() {

    private lateinit var nListener: onItemClickListener
    var lastPosition = (collectionsList.size - 1).coerceAtLeast(0)

    init {
        var exists = false
        for(collection in collectionsList){
            Log.d("PATH", collection.name)
            if(collection.name == "blank"){
                exists = true
                break
            }
        }
        if (!exists) {
            collectionsList.add(Collection("blank"))
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
        val name: TextView = itemView.findViewById(R.id.name)

        init {
            itemView.setOnClickListener{view ->
                listener.onItemClick(view, adapterPosition, 0, 0)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.collection_layout, parent, false)
        return ViewHolder(itemView, nListener, lastPosition)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = collectionsList[position]
        if(position == collectionsList.size-1) {
            holder.imageView.setImageResource(R.drawable.add_blank)
            holder.imageView.scaleType = ImageView.ScaleType.CENTER
            holder.name.text = if (currentItem.name == "blank") "" else currentItem.name
        } else {
            holder.imageView.setImageResource(R.drawable.one)
            holder.name.text = currentItem.name
        }
    }

    fun addItem(name: String) {
        collectionsList.add(collectionsList.size - 1, Collection(name))
        notifyItemInserted(collectionsList.size - 2)
    }

    fun removeItem(position: Int) {
        collectionsList.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun getItemCount(): Int {
        return collectionsList.size
    }
}
