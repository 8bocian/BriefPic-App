package pl.summernote.summernote.activities

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import pl.summernote.summernote.R
import pl.summernote.summernote.customs.FlowSender
import pl.summernote.summernote.databinding.FlashcardsCarouselLayoutBinding
import pl.summernote.summernote.dataclasses.FlashCard

class FlashCardsActivity : AppCompatActivity() {
    private lateinit var binding: FlashcardsCarouselLayoutBinding

    private var position = 0

    private var flashcards = listOf<FlashCard>()
    private var type: Int = 0

    private lateinit var collectionType: String
    private lateinit var ansButton: MaterialButton

    private var hiddenAns: String? = null

    private var goods = arrayListOf<FlashCard>()
    private var bads = arrayListOf<FlashCard>()

    override fun onResume() {
        super.onResume()
        val flowSender = FlowSender()
        val sharedPrefs: SharedPreferences = this@FlashCardsActivity.baseContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val uuidString = sharedPrefs.getString("UUID", null)
        flowSender.sendFlowInformation(this.javaClass.simpleName, uuidString!!, "ENTER")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FlashcardsCarouselLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.navigationBarColor = resources.getColor(R.color.appBar)
        supportActionBar?.hide()

        flashcards = intent.getParcelableArrayListExtra<FlashCard>("flash_cards") as List<FlashCard>
        type = intent.getIntExtra("type", 0)
        collectionType = intent.getStringExtra("collectionType" )!!
        binding.progressBar1.max = 100
        binding.progressBar2.max = 100


        when (type) {
            R.id.review -> {
                binding.multiple.visibility = View.GONE
                binding.no.visibility = View.GONE
                binding.yes.visibility = View.GONE
                binding.scrollButton.visibility = View.VISIBLE
                binding.answer.visibility = View.GONE
            }
            R.id.write -> {
                binding.multiple.visibility = View.GONE
                binding.no.visibility = View.GONE
                binding.yes.visibility = View.GONE
                binding.scrollButton.visibility = View.VISIBLE
                binding.scrollButton.text = "Check"
                binding.answer.visibility = View.VISIBLE
            }
            R.id.multiple_choice -> {
                binding.multiple.visibility = View.VISIBLE
                binding.no.visibility = View.GONE
                binding.yes.visibility = View.GONE
                binding.scrollButton.visibility = View.VISIBLE
                binding.answer.visibility = View.GONE
            }
            R.id.yes_no -> {
                binding.multiple.visibility = View.GONE
                binding.no.visibility = View.VISIBLE
                binding.yes.visibility = View.VISIBLE
                binding.scrollButton.visibility = View.GONE
                binding.answer.visibility = View.GONE
            }
        }

        showNewFlashcard(flashcards[position])

        binding.scrollButton.setOnClickListener {
            if (binding.answer.visibility == View.VISIBLE) {
                if (binding.answer.text.toString() == ""){
                    Toast.makeText(this@FlashCardsActivity, "Provide an answer", Toast.LENGTH_SHORT).show()
                } else {
                    if (binding.scrollButton.text.toString().uppercase() == "NEXT") {
                        binding.scrollButton.text = "SUBMIT"
                        binding.realAnswer.visibility = View.GONE
                        binding.answer.setTextColor(resources.getColor(R.color.betterWhite))
                        binding.answer.isEnabled = true
                        binding.answer.text = Editable.Factory().newEditable("")
                        move()
                    } else {
                        if (check(binding.answer.text.toString())) {
                            binding.answer.setTextColor(resources.getColor(R.color.color3))
                        } else {
                            binding.answer.setTextColor(resources.getColor(R.color.color1))
                        }
                        binding.realAnswer.visibility = View.VISIBLE
                        binding.realAnswer.text = hiddenAns
                        binding.scrollButton.text = "NEXT"
                        binding.answer.isEnabled = false
                    }
                }
            } else if (binding.multiple.visibility == View.VISIBLE){
                if ((goods.size + bads.size) == (position+1)) {
                    move()
                } else {
                    Toast.makeText(this@FlashCardsActivity, "Select your answer", Toast.LENGTH_SHORT).show()
                }
            } else if(type == R.id.review) {
                if (!goods.contains(flashcards[position])) {
                    goods.add(flashcards[position])
                    binding.progressBar1.progress = ((goods.size.toFloat() / flashcards.size.toFloat()) * 100).toInt()
                }
                move()
            } else {
                move()
            }
        }



        binding.yes.setOnClickListener {
            if (!goods.contains(flashcards[position])) {
                goods.add(flashcards[position])
                binding.progressBar1.progress = ((goods.size.toFloat() / flashcards.size.toFloat()) * 100).toInt()
            }
            move()
        }

        binding.no.setOnClickListener {
            if (!bads.contains(flashcards[position])) {
                bads.add(flashcards[position])
                binding.progressBar2.progress = ((bads.size.toFloat() / flashcards.size.toFloat()) * 100).toInt()
            }
            move()
        }
    }

    private fun move(){
        Log.d("MOVEFLASH", "MOVE")
        if (position != flashcards.lastIndex) {
            position += 1
            showNewFlashcard(flashcards[position])
            binding.front.visibility = View.VISIBLE
            binding.back.visibility = View.GONE
        } else {
            saveResult()
            finish()
        }
    }

    private fun saveResult(){

    }

    private fun showNewFlashcard(flashcard: FlashCard){
        binding.front.text = flashcard.front
        binding.back.text = flashcard.back

        hiddenAns = binding.back.text.toString()

        for (materialButton in arrayListOf(
            binding.ans1,
            binding.ans2,
            binding.ans3,
            binding.ans4
        )) {
            materialButton.visibility = View.GONE
            materialButton.setTextColor(resources.getColor(R.color.betterWhite))
        }

        if (type == R.id.multiple_choice){
            val toShuffle = flashcards.filter{it.back != hiddenAns}
            val answers = toShuffle.shuffled().take(3).map { it.back } as ArrayList
            answers.add(hiddenAns!!)
            answers.shuffle()
            answers.zip(arrayListOf(binding.ans1, binding.ans2, binding.ans3, binding.ans4)){ answer, button ->
                button.text = answer

                if (answer == hiddenAns){
                    ansButton = button
                }

                button.visibility = View.VISIBLE
                button.setOnClickListener {
                    check(answer)
                    if(answer == hiddenAns) {
                        button.setTextColor(resources.getColor(R.color.color3))
                    } else {
                        button.setTextColor(resources.getColor(R.color.color1))
                    }
                    ansButton.setTextColor(resources.getColor(R.color.color3))
                    binding.scrollButton.visibility = View.VISIBLE
                }
            }
        }

        binding.flashcard.setOnClickListener {
            binding.flipper.inAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
            binding.flipper.outAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)

//            val random = (0..1).random()
//            if (random == 1){
//                binding.flipper.showNext()
//                hiddenAns = binding.front.text.toString()
//            } else {
//                hiddenAns = binding.back.text.toString()
//            }
            binding.flipper.setOnClickListener {
                binding.flipper.showNext()
            }
        }
    }

    private fun check(answer: String): Boolean{
        return if(answer == hiddenAns){
            goods.add(flashcards[position])
            binding.progressBar1.progress = ((goods.size.toFloat() / flashcards.size.toFloat()) * 100).toInt()
            Log.d("ANSWERCHECK", binding.progressBar1.secondaryProgress.toString())
            true
        } else {
            bads.add(flashcards[position])
            binding.progressBar2.progress = ((bads.size.toFloat() / flashcards.size.toFloat()) * 100).toInt()
            Log.d("ANSWERCHECK", binding.progressBar2.progress.toString())
            false
        }
    }
}