package pl.summernote.summernote.customs

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText


class EmojiTextWatcher(private val editText: EditText) : TextWatcher {

    private val emojiRegex = Regex("[\\p{So}]") // Matches any emoji character

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        val input = s.toString()
        val filteredInput = input.filter { emojiRegex.matches(it.toString()) }

        if (input != filteredInput) {
            editText.removeTextChangedListener(this)
            editText.setText(filteredInput)
            editText.setSelection(filteredInput.length)
            editText.addTextChangedListener(this)
        }
    }

    override fun afterTextChanged(s: Editable?) {}
}