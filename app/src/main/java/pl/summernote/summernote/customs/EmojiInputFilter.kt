package pl.summernote.summernote.customs

import android.text.InputFilter
import android.text.Spanned
import java.util.regex.Pattern


class EmptyInputFilter : InputFilter {
    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        // Only accept the input if the EditText is empty
        return if (dest.isEmpty()) {
            null // Return null to accept the input
        } else {
            "" // Return an empty string to block the input
        }
    }
}