package pl.summernote.summernote.fragments

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.button.MaterialButton
import pl.summernote.summernote.R
import pl.summernote.summernote.customs.EmptyInputFilter
import pl.summernote.summernote.customs.FlowSender

interface ChangeCollectionDialogListener {
    fun onCollectionChanged(
        collectionNameNew: String,
        collectionIconNew: String,
        collectionNameOld: String,
        collectionIconOld: String,
        index: Int
    )

    fun onCollectionRemoved(
        collectionNameOld: String,
        collectionIconOld: String,
        index: Int
    )
}

class ChangeCollectionDialogFragment : DialogFragment(), DialogListener {

    private var listener: ChangeCollectionDialogListener? = null
    private lateinit var collectionIcon: String
    private lateinit var icon: ImageView

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as ChangeCollectionDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement AddCollectionDialogListener")
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

    override fun onResume() {
        super.onResume()
        val flowSender = FlowSender()
        val sharedPrefs: SharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val uuidString = sharedPrefs.getString("UUID", null)
        flowSender.sendFlowInformation(this.javaClass.simpleName, uuidString!!, "ENTER")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.change_subject_layout)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val collectionNameOld = arguments?.getString("name").toString()
        var collectionIconOld = arguments?.getString("icon")!!
        Log.d("EMOJIFIRST", "collectionIconOld")

        val index = arguments?.getInt("index")
        val existingNames = arguments?.getStringArrayList("names")!!
        if (getFirstEmoji(collectionIconOld) == null){
            collectionIconOld = "\uD83C\uDDEC\uD83C\uDDE7"
        }
        collectionIcon = collectionIconOld
        Log.d("DUPSKOEMO", collectionIconOld)
        dialog.findViewById<EditText>(R.id.collection_name).text = Editable.Factory.getInstance().newEditable(collectionNameOld)

        val emojiEditText = dialog.findViewById<EditText>(R.id.emojiEditText)
        emojiEditText.text = Editable.Factory.getInstance().newEditable(collectionIcon)
        emojiEditText.filters = arrayOf(EmptyInputFilter())
        emojiEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not used
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Not used
            }

            override fun afterTextChanged(editable: Editable?) {
                val input = editable.toString()
                val emoji = getFirstEmoji(input)
                emojiEditText.removeTextChangedListener(this)

                if (emoji != null) {
                    emojiEditText.setText(emoji)
                    emojiEditText.setSelection(emoji.length)
                    collectionIcon = emoji
                } else {
                    emojiEditText.text = null
                    collectionIcon = ""
                }
                Log.d("DUPSKOEMO", collectionIcon)

                emojiEditText.addTextChangedListener(this)
            }
        })

        dialog.findViewById<MaterialButton>(R.id.change_collection).setOnClickListener {
            val collectionNameNew = dialog.findViewById<EditText>(R.id.collection_name).text.toString()

            Log.d("CHANGE ITEM", "CLICKED")
            if (collectionNameNew != "") {
                if(collectionIcon != "") {
                    if (!existingNames.contains(collectionNameNew)) {
                        listener?.onCollectionChanged(
                            collectionNameNew,
                            collectionIcon,
                            collectionNameOld,
                            collectionIconOld,
                            index!!
                        )
                        dismiss()
                    } else {
                        Toast.makeText(requireContext(), "Duplicated name", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Icon is empty", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Name is empty", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.findViewById<MaterialButton>(R.id.delete_collection).setOnClickListener {
            listener?.onCollectionRemoved(
                collectionNameOld,
                collectionIconOld,
                index!!
            )
            dismiss()
        }
        return dialog
    }
    override fun onValueSelected(value: Int) {
        collectionIcon = resources.getResourceEntryName(value)

        icon.setBackgroundResource(value)
    }
}
