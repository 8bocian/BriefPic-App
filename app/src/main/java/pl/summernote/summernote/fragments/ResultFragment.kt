package pl.summernote.summernote.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.button.MaterialButton
import pl.summernote.summernote.R

class ResultFragment: DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.change_element_layout)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent);



        val elementNameOld = arguments?.getString("name").toString()
        val index = arguments?.getInt("index")

        val editText = dialog.findViewById<EditText>(R.id.element_name_modify)
        val textView = dialog.findViewById<TextView>(R.id.element_name)


        return dialog
    }
}