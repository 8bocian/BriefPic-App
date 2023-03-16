package pl.summernote.summernote.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import pl.summernote.summernote.databinding.BottomSheetBarLayoutBinding
import java.io.File

class BottomSheetFragment: BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetBarLayoutBinding
    private lateinit var textPath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        textPath = arguments?.getString("textPath").toString()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetBarLayoutBinding.inflate(inflater, container, false)

        val text = File(textPath).readText()
        binding.bottomSheetNote.text = text
        return binding.root
    }
}