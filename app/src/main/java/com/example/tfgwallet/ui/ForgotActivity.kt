package com.example.tfgwallet.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.tfgwallet.R


class ForgotActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_passforgotten)
        val mnemonicsIntent = Intent(this, MnemonicsActivity::class.java)
        startActivity(mnemonicsIntent)
        finish()
    }
}