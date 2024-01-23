package pl.summernote.summernote.adapters

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import org.w3c.dom.Text
import pl.summernote.summernote.R
import pl.summernote.summernote.dataclasses.Collection
import pl.summernote.summernote.fragments.AddCollectionDialogFragment
import pl.summernote.summernote.fragments.ChangeCollectionDialogFragment


class CollectionsAdapter(private var collectionsList: ArrayList<Collection>,
                         private val fragmentManager: FragmentManager,
                         private val context: Context,
) :
    RecyclerView.Adapter<CollectionsAdapter.ViewHolder>() {

    private lateinit var nListener: onItemClickListener

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_ADD = 1
    }

    interface onItemClickListener {
        fun onItemClick(view: View, position: Int, x: Int, y: Int)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == collectionsList.size - 1) {
            VIEW_TYPE_ADD
        } else {
            VIEW_TYPE_ITEM
        }
    }

    fun getNames(): ArrayList<String>{
        val names = arrayListOf<String>()
        collectionsList.forEach {
            names.add(it.name)
        }
        return names
    }

    fun setOnItemClickListener(listener: onItemClickListener){
        nListener = listener
    }

    class ViewHolder(itemView: View, listener: onItemClickListener,
                     private val fragmentManager: FragmentManager,
                     private val collectionsList: ArrayList<Collection>
    ) : RecyclerView.ViewHolder(itemView), View.OnLongClickListener {
        val imageView: ImageView = itemView.findViewById(R.id.image_view)
        val name: TextView = itemView.findViewById(R.id.name)
        val icon: TextView = itemView.findViewById(R.id.icon)
        val background: ImageView = itemView.findViewById(R.id.background)


        init {
            imageView.setOnClickListener{view ->
                listener.onItemClick(view, adapterPosition, 0, 0)
            }
            imageView.setOnLongClickListener {
                Log.d("LONG", "Item long clicked at position: $adapterPosition")
                val position = adapterPosition
                val dialogFragment = ChangeCollectionDialogFragment()
                val args = Bundle()
                args.putString("name", name.text.toString())
                args.putString("icon", collectionsList[adapterPosition].icon!!)
                args.putStringArrayList("names", getNames())
                args.putInt("index", position)
                dialogFragment.arguments = args
                dialogFragment.show(fragmentManager, "MyDialogFragment")
                true
            }
        }
        fun getNames(): ArrayList<String>{
            val names = arrayListOf<String>()
            collectionsList.forEach {
                names.add(it.name)
            }
            names.remove(name.text.toString())
            return names
        }

        override fun onLongClick(v: View?): Boolean {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val dialogFragment = AddCollectionDialogFragment()
                dialogFragment.show(fragmentManager, "MyDialogFragment")
                return true
            }
            return false
        }
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = when (viewType) {
            VIEW_TYPE_ADD -> LayoutInflater.from(parent.context)
                .inflate(R.layout.collection_layout, parent, false)
            else -> LayoutInflater.from(parent.context)
                .inflate(R.layout.collection_layout, parent, false)
        }
        return ViewHolder(itemView, nListener, fragmentManager, collectionsList)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = collectionsList[position]
        holder.name.text = currentItem.name
        holder.itemView.isLongClickable = true
//        holder.icon.setBackgroundResource(context.resources.getIdentifier(currentItem.icon!!, "drawable", "pl.summernote.summernote"))
        if (getFirstEmoji(currentItem.icon!!) == null){
            holder.icon.text = "\uD83C\uDDEC\uD83C\uDDE7"
        } else {
            holder.icon.text = currentItem.icon!!
        }
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

    fun addItem(name: String, icon: String, position: Int) {
        collectionsList.add(collectionsList.size, Collection(name, icon, position))
        notifyItemInserted(collectionsList.size - 1)
    }

    fun removeItem(position: Int) {
        collectionsList.removeAt(position)
        notifyItemRemoved(position)
    }

    fun getItem(position: Int): Int {
        return if (collectionsList.isEmpty()) {
            -1
        } else {
            collectionsList[position].position
        }
    }

    fun changeItem(collectionNameNew: String, collectionIconNew: String, index: Int){
        Log.d("CHANGEITEM", collectionsList[index].toString())
        collectionsList[index].name = collectionNameNew
        collectionsList[index].icon = collectionIconNew
        notifyItemChanged(index)
        notifyDataSetChanged()
        Log.d("CHANGEITEM", collectionsList[index].toString())
    }

    override fun getItemCount(): Int {
        return collectionsList.size
    }
}
