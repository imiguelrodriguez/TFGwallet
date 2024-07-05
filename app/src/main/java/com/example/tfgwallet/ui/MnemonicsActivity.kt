package com.example.tfgwallet.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View.NOT_FOCUSABLE
import android.widget.Toast
import com.example.tfgwallet.R
import com.example.tfgwallet.databinding.ActivityHomeBinding
import com.example.tfgwallet.databinding.ActivityMnemonicsBinding
import com.leo.searchablespinner.SearchableSpinner
import org.bitcoinj.crypto.MnemonicCode
import com.leo.searchablespinner.interfaces.OnItemSelectListener


class MnemonicsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMnemonicsBinding
    private var isTextInputLayoutClicked: Boolean = false
    private var words : Int = 0

    // TODO: control maximum input number (24 words max)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMnemonicsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        updateWords(0)

        val searchableSpinner = SearchableSpinner(this)
        searchableSpinner.windowTitle = getString(R.string.pick_mnemonic)

        searchableSpinner.onItemSelectListener = object : OnItemSelectListener {
            override fun setOnItemSelectListener(position: Int, selectedString: String) {
                Toast.makeText(
                    applicationContext,
                    "${searchableSpinner.selectedItem}  ${searchableSpinner.selectedItemPosition}",
                    Toast.LENGTH_SHORT
                ).show()
                if (isTextInputLayoutClicked) {
                    binding.textInputSpinner.editText?.setText(selectedString)
                    binding.mnemonicList.setText(binding.mnemonicList.text.toString() + " " +  selectedString)
                    updateWords(++words)
                }
                else
                    binding.mnemonicList.setText(selectedString)
            }
        }

        //Setting up list items for spinner
        val mnemonicList = MnemonicCode.INSTANCE.wordList
        searchableSpinner.setSpinnerListItems(mnemonicList as ArrayList<String>)

        //Showing searchable spinner
        binding.textInputSpinner.editText?.setOnClickListener {
            isTextInputLayoutClicked = true
            searchableSpinner.show()
        }

        binding.mnemonicList.focusable = NOT_FOCUSABLE
        binding.mnemonicList.isClickable = false
        binding.mnemonicList.isCursorVisible = false

        binding.deleteAll.setOnClickListener {
            binding.mnemonicList.setText("")
            updateWords(0)
        }

        binding.deleteLast.setOnClickListener {
            val newText = binding.mnemonicList.text.toString().substringBeforeLast(' ')
            binding.mnemonicList.setText(newText)
            if (newText != "")
                updateWords(--words)
            else
                updateWords(0)
        }
    }

    private fun updateWords(i: Int) {
        words = i
        binding.wordsLabel.text = getString(R.string.number_mnemonic_words, words)
        if(words == 0) {
            binding.deleteLast.isEnabled = false
            binding.deleteAll.isEnabled = false
        }
        else {
            binding.deleteLast.isEnabled = true
            binding.deleteAll.isEnabled = true
        }
    }
}