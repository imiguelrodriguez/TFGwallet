package com.example.tfgwallet

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.tfgwallet.Protocols as protocols

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val inputString = "Hello, this is a test string for hashing."
        val hashedString = protocols.sha256(inputString)
        println("SHA256 Hash: $hashedString")
    }
}