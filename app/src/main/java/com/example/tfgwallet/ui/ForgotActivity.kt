package com.example.tfgwallet.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.tfgwallet.R
import com.example.tfgwallet.databinding.ActivityLoginBinding
import com.example.tfgwallet.databinding.ActivityPassforgottenBinding


class ForgotActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPassforgottenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPassforgottenBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)


        binding.recoverButton.setOnClickListener {
            val mnemonicsIntent = Intent(this, MnemonicsActivity::class.java).apply {
                putExtra("email", binding.username.text.toString())
                putExtra("newpass", binding.newpass.text.toString())
            }
            startActivity(mnemonicsIntent)
        }
    }
}