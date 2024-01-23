package pl.summernote.summernote.fragments

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.GridLayout
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import pl.summernote.summernote.R
import pl.summernote.summernote.customs.FlowSender

interface DialogListener {
    fun onValueSelected(value: Int)
}

class AddIconDialogFragment : DialogFragment() {

    private var listener: DialogListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as? DialogListener
        if (listener == null) {
            throw ClassCastException("$context must implement DialogListener")
        }
    }

    override fun onResume() {
        super.onResume()
        val flowSender = FlowSender()
        val sharedPrefs: SharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val uuidString = sharedPrefs.getString("UUID", null)
        flowSender.sendFlowInformation(this.javaClass.simpleName, uuidString!!, "ENTER")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.add_icon_layout)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val icons = listOf(R.drawable.earthbg, R.drawable.biologybg, R.drawable.mathbg, R.drawable.physicsbg, R.drawable.poetrybg, R.drawable.englandbg, R.drawable.francebg, R.drawable.germanybg, R.drawable.italybg, R.drawable.spainbg)
//        val icons = listOf(R.drawable.englandbg, R.drawable.francebg, R.drawable.germanybg, R.drawable.italybg, R.drawable.spainbg)
        val grid: GridLayout = dialog.findViewById(R.id.icons_grid)
        val columnCount = 2

        val screenWidth = resources.displayMetrics.widthPixels
        val imageSize = (screenWidth-400) / columnCount

        icons.forEach { icon ->
            val iconView = ImageView(this.context)
            iconView.layoutParams = GridLayout.LayoutParams().apply {
                width = imageSize
                height = imageSize
                setMargins(20, 20, 20, 20)
            }
            iconView.setBackgroundResource(icon)
            grid.addView(iconView)
            iconView.setOnClickListener {
                listener?.onValueSelected(icon)
                dismiss()
            }
        }
        return dialog
    }
}