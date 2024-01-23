package pl.summernote.summernote.adapters

import android.content.Context
import android.opengl.Visibility
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.w3c.dom.Text
import pl.summernote.summernote.R
import pl.summernote.summernote.dataclasses.Element
import pl.summernote.summernote.dataclasses.FlashCard
import java.io.File
import java.io.FileWriter

interface OnListChangedListener {
    fun onListChanged(newListSize: Int)
}

class AddAdapter(private var flashcardsList: ArrayList<FlashCard>, private val cacheDir: File, private val context: Context, private val filePath: String, private val type: String) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    private var onListChangedListener: OnListChangedListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.add_flashcard_layout, parent, false)
        if(type == "notes") {
            itemView.findViewById<TextView>(R.id.textView2).visibility = TextView.GONE
            itemView.findViewById<TextView>(R.id.textView3).visibility = TextView.GONE
            itemView.findViewById<TextView>(R.id.add_back).visibility = TextView.GONE
        }
        return ViewHolder(itemView)
    }

    fun setOnListChangedListener(listener: OnListChangedListener) {
        onListChangedListener = listener
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder.itemView.isLongClickable = true
        val currentItem = flashcardsList[position]

        val front: TextView = holder.itemView.findViewById(R.id.add_front)
        val back: TextView = holder.itemView.findViewById(R.id.add_back)

        front.text = currentItem.front
        back.text = currentItem.back

    }

    override fun getItemCount(): Int {
        return flashcardsList.size
    }

    fun addItem(flashCard: FlashCard) {
        flashcardsList.add(flashCard)
        notifyItemInserted(flashcardsList.size)
        onListChangedListener?.onListChanged(flashcardsList.size)
        saveFlashcards()
    }

    fun removeItem(position: Int) {
        flashcardsList.removeAt(position)
        notifyItemRemoved(position)
        onListChangedListener?.onListChanged(flashcardsList.size)
        saveFlashcards()
    }

    fun getItems(): ArrayList<FlashCard>{
        return flashcardsList
    }

    fun setItems(flashcards: ArrayList<FlashCard>){
        flashcardsList = flashcards
        notifyDataSetChanged()
        onListChangedListener?.onListChanged(flashcardsList.size)
        saveFlashcards()
    }

    inner class ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val front: TextView = itemView.findViewById(R.id.add_front)
        val back: TextView = itemView.findViewById(R.id.add_back)
        val icon: ImageView = itemView.findViewById(R.id.delete)

        init {
            // Set up the text change listeners here
            front.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Do nothing
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // Update the corresponding item in the list with the new text
                    flashcardsList[adapterPosition].front = s.toString()
                }

                override fun afterTextChanged(s: Editable?) {
                    // Do nothing
                }
            })

            back.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Do nothing
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // Update the corresponding item in the list with the new text
                    flashcardsList[adapterPosition].back = s.toString()
                }

                override fun afterTextChanged(s: Editable?) {
                    // Do nothing
                }
            })
            // Set up the click listener for the delete icon
            icon.setOnClickListener {
                removeItem(adapterPosition)
            }
        }
    }

    private fun saveFlashcards() {
        val gson: Gson = GsonBuilder().setPrettyPrinting().create()

        // convert ArrayList to JSON string
        val jsonString: String = gson.toJson(flashcardsList)
        Log.d("CURRENTITEM", jsonString)
        FileWriter(File(cacheDir, filePath)).use { writer ->
            writer.write(jsonString)
        }
    }
}

