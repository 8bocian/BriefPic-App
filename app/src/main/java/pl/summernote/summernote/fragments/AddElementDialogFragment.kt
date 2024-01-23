package pl.summernote.summernote.fragments

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import pl.summernote.summernote.R
import pl.summernote.summernote.customs.FlowSender

interface AddElementDialogListener{
    fun onElementAdded(collectionName: String)
}

class AddElementDialogFragment : DialogFragment() {

     var listener: AddElementDialogListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as AddElementDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement AddCollectionDialogListener")
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
        dialog.setContentView(R.layout.add_element_layout)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent);

        val existingNames = arguments?.getStringArrayList("names")!!

        val editText = dialog.findViewById<EditText>(R.id.collection_name)
        val textView = dialog.findViewById<TextView>(R.id.element_name)
        val addButton = dialog.findViewById<MaterialButton>(R.id.add_collection)

        editText.addTextChangedListener {
            textView.text = it.toString()
        }

        addButton.setOnClickListener {
            val elementName = editText.text.toString()
            if(elementName != "") {
                if(!existingNames.contains(elementName)) {
                    listener?.onElementAdded(elementName)
                    dismiss()
                } else {
                    Toast.makeText(requireContext(), "Duplicated name", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Name is empty", Toast.LENGTH_SHORT).show()
            }
        }

        return dialog
    }
}