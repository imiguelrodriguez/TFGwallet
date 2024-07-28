package com.example.tfgwallet.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import com.example.tfgwallet.R
import com.example.tfgwallet.databinding.ActivityLoginBinding
import com.example.tfgwallet.model.KeyManagement
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

/**
 * Main activity for the wallet app. It represents the log-in page.
 * @author Ignacio Miguel Rodríguez
 */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private var isPasswordVisible = false
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        if (intent.hasExtra("session_expired")) {
            intent.getStringExtra("session_expired")?.let { showAlert("Session expired", it) }
        }
        setContentView(binding.root)
        binding.password.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (binding.password.right - binding.password.compoundDrawables[2].bounds.width())) {
                    isPasswordVisible = !isPasswordVisible
                    updatePasswordVisibility()
                    binding.password.performClick() // Call performClick for accessibility
                }
            }
            false
        }
        setup()
    }

    /**
     * Method that sets the last user logged in into the Shared Preferences
     * @param context app context
     * @param userId String indicating the username (e.g. if email is example@gmail.com, username would be example)
     */
    private fun setLastLoggedInUserId(context: Context, userId: String) {
        context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE).edit().putString("lastLoggedInUserId", userId).apply()
    }

    /**
     * Method that retrieves the last user logged from the Shared Preferences
     * @param context app context
     * @return String indicating the username, null if the field does not exist
     */
    private fun getLastLoggedInUserId(context: Context): String? {
        return context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE).getString("lastLoggedInUserId", null)
    }


    /**
     * Method that retrieves if the 2AF is enabled for given user from the Shared Preferences
     * @param context app context
     * @param userId String indicating the username
     * @return true if the 2AF is enabled, false otherwise (note that if that entry does not exist, false will also be returned)
     */
    private fun isTwoFactorAuthEnabled(context: Context, userId: String): Boolean {
        return context.getSharedPreferences("user_$userId", Context.MODE_PRIVATE).getBoolean("isTwoFactorAuthEnabled", false)
    }

    private fun updatePasswordVisibility() {
        if (isPasswordVisible) {
            binding.password.transformationMethod = HideReturnsTransformationMethod.getInstance()
            binding.password.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_lock_24, 0, R.drawable.ic_eye_open, 0)
        } else {
            binding.password.transformationMethod = PasswordTransformationMethod.getInstance()
            binding.password.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_lock_24, 0, R.drawable.ic_eye_closed, 0)
        }
        binding.password.setSelection(binding.password.text.length)
    }

    private fun setup() {
        val lastLoggedInUserId = getLastLoggedInUserId(this)

        // autocomplete if last user is still logged in
        if (lastLoggedInUserId != null) {
            binding.username.setText(lastLoggedInUserId)
        }

        binding.logInButton.setOnClickListener {
            val email = binding.username.text.toString()
            val password = binding.password.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                val user = email.substringBefore("@")
                val sharedPrefs = getSharedPreferences("credentials", Context.MODE_PRIVATE)
                val savedEmail = sharedPrefs.getString("email_$user", null)
                val savedPassword = sharedPrefs.getString("password_$user", null)
                val hashedPassword = KeyManagement.sha512(password)
                if (savedEmail == email && savedPassword == hashedPassword) {
                    saveLoginTimestamp()
                    val homeIntent = Intent(this, HomeActivity::class.java).apply {
                        putExtra("email", email)
                    }
                    homeIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    if (isTwoFactorAuthEnabled(this, user)) {
                        askForTwoFactorAuth { isAuthenticated ->
                            if (isAuthenticated) {
                                setLastLoggedInUserId(this, email)
                                startActivity(homeIntent)
                                finish()
                            }
                        }
                    } else {
                        setLastLoggedInUserId(this, email)
                        startActivity(homeIntent)
                        finish()
                    }
                } else {
                    showAlert("Error", "Invalid email or password")
                }
            } else {
                showAlert("Error", "Please fill in all the fields!")
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

    /**
     * Method that asks for the 2AF (e.g. fingerprint or device PIN).
     * @return true if the authentication has been completed successfully, false otherwise
     */
    private fun askForTwoFactorAuth(callback: (Boolean) -> Unit) {
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
                    callback(false)
                }

                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(
                        applicationContext,
                        "Authentication succeeded!", Toast.LENGTH_SHORT
                    )
                        .show()
                    callback(true)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(
                        applicationContext, "Authentication failed",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    callback(false)
                }
            })

        val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login")
            .setSubtitle("Log in using your biometric credential")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

         biometricPrompt.authenticate(promptInfo)

    }

    private fun showAlert(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("Accept", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }


    private fun saveLoginTimestamp() {
        val sharedPrefs = getSharedPreferences("user_${binding.username.text.toString().substringBefore("@")}", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.putLong("loginTimestamp", System.currentTimeMillis())
        editor.apply()
    }

}