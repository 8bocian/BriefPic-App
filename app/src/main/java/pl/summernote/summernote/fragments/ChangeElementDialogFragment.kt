package pl.summernote.summernote.fragments

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.button.MaterialButton
import org.w3c.dom.Text
import pl.summernote.summernote.R
import pl.summernote.summernote.customs.FlowSender

interface ChangeElementDialogListener {
    fun onElementChanged(
        elementNameNew: String,
        elementNameOld: String,
        index: Int
    )

    fun onElementRemoved(
        elementNameOld: String,
        index: Int
    )
}

class ChangeElementDialogFragment: DialogFragment() {
    var listener: ChangeElementDialogListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as ChangeElementDialogListener
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
        dialog.setContentView(R.layout.change_element_layout)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val existingNames = arguments?.getStringArrayList("names")!!

        val elementNameOld = arguments?.getString("name").toString()
        val index = arguments?.getInt("index")
        val count = arguments?.getString("count")

        val editText = dialog.findViewById<EditText>(R.id.element_name_modify)
        val textView = dialog.findViewById<TextView>(R.id.element_name)
        dialog.findViewById<TextView>(R.id.flashcards_count).text = "flashcards: $count"
        Log.d("COUNTTEXT", count.toString())

        editText.addTextChangedListener {
            textView.text = it.toString()
        }

        editText.text = Editable.Factory.getInstance().newEditable(elementNameOld)
        textView.text = elementNameOld

        dialog.findViewById<MaterialButton>(R.id.change_element).setOnClickListener {
            val elementNameNew = dialog.findViewById<EditText>(R.id.element_name_modify).text.toString()

            if (elementNameNew != "") {
                if(!existingNames.contains(elementNameNew)) {
                    listener?.onElementChanged(
                        elementNameNew,
                        elementNameOld,
                        index!!
                    )
                    dismiss()
                } else {
                    Toast.makeText(requireContext(), "Duplicated name", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Name is empty", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.findViewById<MaterialButton>(R.id.delete_element).setOnClickListener {
            listener?.onElementRemoved(
                elementNameOld,
                index!!
            )
            dismiss()
        }
        return dialog
    }
}