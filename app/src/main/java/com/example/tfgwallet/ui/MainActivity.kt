package com.example.tfgwallet.ui

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.example.tfgwallet.databinding.ActivityLoginBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.hbb20.CountryCodePicker


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        val phoneCodePicker = binding.phoneLayout
        binding.checkBox.setOnCheckedChangeListener { _, isChecked ->
            phoneCodePicker.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        FirebaseApp.initializeApp(this)
        setup(binding)

    }

    private fun setup(binding: ActivityLoginBinding) {

        binding.logInButton.setOnClickListener {
            FirebaseAuth.getInstance().signInWithEmailAndPassword(
                binding.username.text.toString(),
                binding.password.text.toString()
            )
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        val homeIntent = Intent(this, HomeActivity::class.java).apply {
                            putExtra("email", it.result.user.toString())
                        }
                        // to save the log in status
                        val sharedPrefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                        sharedPrefs.edit().putBoolean("isLoggedIn", true).apply()

                        homeIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(homeIntent)
                        finish()
                    } else {
                        showAlert("Error", "Problem signing in for user ${binding.username.text}")
                    }
                }
        }

        binding.signUp.setOnClickListener {
            val signUpIntent = Intent(this@MainActivity, SignUpActivity::class.java)
            startActivity(signUpIntent)

        }

        binding.forgottenPass.setOnClickListener {
            val forgottenIntent = Intent(this@MainActivity, ForgotActivity::class.java)
            startActivity(forgottenIntent)
        }
    }
    private fun showAlert(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("Accept", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }
}