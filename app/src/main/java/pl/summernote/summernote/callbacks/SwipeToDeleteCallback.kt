package pl.summernote.summernote.callbacks

import android.content.Context
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import pl.summernote.summernote.adapters.ElementsAdapter

class SwipeToDeleteCallback(private val context: Context, private val adapter: ElementsAdapter) : ItemTouchHelper.Callback() {
    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        TODO("Not yet implemented")
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        Toast.makeText(context, "on Move", Toast.LENGTH_SHORT).show()
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
        Toast.makeText(context, "on Swiped ", Toast.LENGTH_SHORT).show()
        //Remove swiped item from list and notify the RecyclerView
        val position = viewHolder.adapterPosition
        adapter.removeItem(position)
    }

}