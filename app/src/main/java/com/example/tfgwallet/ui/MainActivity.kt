package com.example.tfgwallet.ui

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import com.example.tfgwallet.databinding.ActivityLoginBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

/**
 * Main activity for the wallet app. It represents the log-in page.
 * @author Ignacio Miguel Rodríguez
 */
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
     * Method that retrieves if the given user is logged-in from the Shared Preferences
     * @param context app context
     * @param userId String indicating the username
     * @return true if logged-in, false otherwise (note that if that entry does not exist, false will also be returned)
     */
    private fun isLoggedIn(context: Context, userId: String): Boolean {
        return context.getSharedPreferences("user_$userId", Context.MODE_PRIVATE).getBoolean("isLoggedIn", false)
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
                            askForTwoFactorAuth { isAuthenticated ->
                               val auth = isAuthenticated as Boolean
                               if (auth) {
                                   setLastLoggedInUserId(this, binding.username.text.toString())
                                   startActivity(homeIntent)
                                   finish()
                               }
                           }
                        }
                        else {
                            setLastLoggedInUserId(this, binding.username.text.toString())
                            startActivity(homeIntent)
                            finish()
                        }

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
}