package pl.summernote.summernote.fragments

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.media.Image
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.emoji.widget.EmojiButton
import androidx.core.content.ContextCompat
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import androidx.emoji.widget.EmojiEditText
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.button.MaterialButton
import pl.summernote.summernote.R
import pl.summernote.summernote.customs.EmptyInputFilter
import pl.summernote.summernote.customs.FlowSender


interface AddCollectionDialogListener{
    fun onCollectionAdded(collectionName: String, collectionIcon: String)
}

class AddCollectionDialogFragment : DialogFragment(), DialogListener {
    var listener: AddCollectionDialogListener? = null
    private lateinit var collectionIcon: String
    private lateinit var icon: ImageView

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as AddCollectionDialogListener
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
        dialog.setContentView(R.layout.add_collection_layout)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val existingNames = arguments?.getStringArrayList("names")!!

        val emojiEditText = dialog.findViewById<EditText>(R.id.emojiEditText)
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

                emojiEditText.addTextChangedListener(this)
            }
        })


        collectionIcon = "\uD83C\uDDEC\uD83C\uDDE7"
        emojiEditText.text = Editable.Factory.getInstance().newEditable("🇬🇧")
        val addButton = dialog.findViewById<MaterialButton>(R.id.add_collection)
        addButton.setOnClickListener {
            val collectionName = dialog.findViewById<EditText>(R.id.collection_name).text.toString()
            if(collectionName != "") {
                if(collectionIcon != ""){
                    if (!existingNames.contains(collectionName)) {
                        listener?.onCollectionAdded(
                            collectionName,
                            collectionIcon,
                        )
                        dismiss()
                    } else {
                        Toast.makeText(requireContext(), "Duplicated name", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Icon is empty", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Name is empty", Toast.LENGTH_SHORT).show()
            }
        }

        return dialog
    }

    private fun validateInput() {
        val input = dialog?.findViewById<EditText>(R.id.emojiEditText)?.text.toString()
        val isOnlyEmojis = EmojiCompat.get().process(input).toString() == input

        if (isOnlyEmojis) {
            // Handle valid input containing only emojis
        } else {
            Toast.makeText(requireContext(), "Please enter only emojis", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onValueSelected(value: Int) {
        collectionIcon = resources.getResourceEntryName(value)

        icon.setBackgroundResource(value)
    }
}