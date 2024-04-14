package com.example.tfgwallet.ui

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.tfgwallet.databinding.ActivityLoginBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        FirebaseApp.initializeApp(this)
        setup(binding)

    }

    fun setLastLoggedInUserId(context: Context, userId: String) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = sharedPreferences.edit()
        editor.putString("lastLoggedInUserId", userId)
        editor.apply()
    }

    // Retrieve the last logged-in user ID
    private fun getLastLoggedInUserId(context: Context): String? {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return sharedPreferences.getString("lastLoggedInUserId", null)
    }

    private fun isLoggedIn(context: Context, userId: String): Boolean {
        val sharedPreferences = context.getSharedPreferences("user_$userId", Context.MODE_PRIVATE)
        Log.i("reading pref", "$userId ${sharedPreferences.all.toString()}")
        return sharedPreferences.getBoolean("isLoggedIn", false)
    }
    private fun isTwoFactorAuthEnabled(context: Context, userId: String): Boolean {
        val sharedPreferences = context.getSharedPreferences("user_$userId", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("isTwoFactorAuthEnabled", false)
    }
    private fun setup(binding: ActivityLoginBinding) {
        val lastLoggedInUserId = getLastLoggedInUserId(this)

        // autocomplete if last user is still logged in
        if (lastLoggedInUserId != null) {
            Log.i("Last user", lastLoggedInUserId)
            val user = lastLoggedInUserId.substringBefore("@")
            val loggedIn = isLoggedIn(this, user)
            Log.i("Logged in?", loggedIn.toString())
            if (loggedIn) {
                binding.username.setText(lastLoggedInUserId)
            }
        }

        binding.logInButton.setOnClickListener {
            FirebaseAuth.getInstance().signInWithEmailAndPassword(
                binding.username.text.toString(),
                binding.password.text.toString()
            )
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        val homeIntent = Intent(this, HomeActivity::class.java).apply {
                            putExtra("email", binding.username.text.toString())
                        }
                        // save log in status and 2AF preferences
                        val user = binding.username.text.toString().substringBefore("@")
                        val sharedPrefs = getSharedPreferences("user_$user", Context.MODE_PRIVATE)

                        val editor = sharedPrefs.edit()
                        editor.putBoolean("isLoggedIn", true).apply()

                        Log.i("Pref", sharedPrefs.all.toString())
                        homeIntent.flags =
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK

                        if (isTwoFactorAuthEnabled(this, user)) {
                            askForTwoFactorAuth()
                        }
                        setLastLoggedInUserId(this, binding.username.text.toString())
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

    private fun askForTwoFactorAuth() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = androidx.biometric.BiometricPrompt(this, executor,
            object: androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(
                        applicationContext,
                        "Authentication error: $errString", Toast.LENGTH_SHORT
                    )
                        .show()
                }

                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(
                        applicationContext,
                        "Authentication succeeded!", Toast.LENGTH_SHORT
                    )
                        .show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(
                        applicationContext, "Authentication failed",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            })

        val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for my app")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Use account password")
            .build()

        // Prompt appears when user clicks "Log in".
        // Consider integrating with the keystore to unlock cryptographic operations,
        // if needed by your app.

        val res = biometricPrompt.authenticate(promptInfo)

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