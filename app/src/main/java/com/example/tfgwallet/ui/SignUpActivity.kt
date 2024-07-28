package com.example.tfgwallet.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.MotionEvent
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tfgwallet.R
import com.example.tfgwallet.control.Control
import com.example.tfgwallet.databinding.ActivitySignupBinding
import com.example.tfgwallet.model.KeyManagement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

private const val SUCCESS_MESSAGE = "Success!"
private const val MNEMONICS_MESSAGE = "Mnemonic words"
private const val SC_MESSAGE = "Smart contract address"


class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    private lateinit var user: String
    private var isPasswordVisible = false
    private val passwordCriteria = BooleanArray(5) // Array to track password criteria
    private var isEmailValid = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.signUpButton.isEnabled = false

        binding.username.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val email = s.toString()
                checkEmail(email)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.password.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                checkPasswordCriteria(password)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

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
        setupSignUpButton()
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

    private fun isEmailValid(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun checkEmail(email: String) {
        isEmailValid = isEmailValid(binding.username.text.toString())
        binding.signUpButton.isEnabled = passwordCriteria.all { it } && isEmailValid

        if (!isEmailValid) {
            binding.username.error = "Invalid email address"
        }
    }

    private fun checkPasswordCriteria(password: String) {
        val uppercasePattern = Pattern.compile(".*[A-Z].*")
        val lowercasePattern = Pattern.compile(".*[a-z].*")
        val numberPattern = Pattern.compile(".*[0-9].*")
        val symbolPattern = Pattern.compile(".*[^a-zA-Z0-9].*")

        passwordCriteria[0] = uppercasePattern.matcher(password).matches()
        passwordCriteria[1] = lowercasePattern.matcher(password).matches()
        passwordCriteria[2] = numberPattern.matcher(password).matches()
        passwordCriteria[3] = symbolPattern.matcher(password).matches()
        passwordCriteria[4] = password.length >= 8

        updateCheck(binding.uppercaseCheck, passwordCriteria[0])
        updateCheck(binding.lowercaseCheck, passwordCriteria[1])
        updateCheck(binding.numberCheck, passwordCriteria[2])
        updateCheck(binding.symbolCheck, passwordCriteria[3])
        updateCheck(binding.lengthCheck, passwordCriteria[4])

        binding.signUpButton.isEnabled = passwordCriteria.all { it } && isEmailValid // Enable the button if all criteria are met
    }

    private fun updateCheck(imageView: ImageView, matches: Boolean) {
        if (matches) {
            imageView.setImageResource(R.drawable.ic_check)
            imageView.setColorFilter(Color.GREEN)
        } else {
            imageView.setImageResource(R.drawable.ic_cross)
            imageView.setColorFilter(Color.RED)
        }
    }

    private fun setupSignUpButton() {
        binding.signUpButton.setOnClickListener {
            if (isInputValid()) {
                showLoadingDialog("Creating account...") { loading ->
                    createUser { isSuccess, message ->
                        loading.closeAlertDialog()
                        if (isSuccess) {
                            user = extractUsername()
                            saveUserPreferences()
                            showAlert(SUCCESS_MESSAGE, message)
                        } else {
                            showAlert("Error", message)
                        }
                    }
                }
            }
        }
    }

    private fun createUser(callback: (Boolean, String) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val email = binding.username.text.toString()
                val emailAlreadyRegistered = isEmailAlreadyRegistered(email)

                if (emailAlreadyRegistered) {
                    withContext(Dispatchers.Main) {
                        callback(false, "Email is already registered.")
                    }
                    return@launch
                }

                val user = extractUsername()
                val password = binding.password.text.toString()
                val hashedPassword = KeyManagement.sha512(password)
                val sharedPreferences = getSharedPreferences("credentials", Context.MODE_PRIVATE)
                with(sharedPreferences.edit()) {
                    putString("email_$user", email)
                    putString("password_$user", hashedPassword)
                    apply()
                }
                withContext(Dispatchers.Main) {
                    callback(true, "User $email has been registered successfully.")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {callback(false, "An error has occurred while trying to sign up.")}
            }
        }
    }

    private fun isEmailAlreadyRegistered(email: String): Boolean {
        val sharedPreferences = getSharedPreferences("credentials", Context.MODE_PRIVATE)
        return sharedPreferences.all.values.contains(email)
    }

    private fun isInputValid(): Boolean {
        return binding.username.text.isNotEmpty() && binding.password.text.isNotEmpty()
    }

    private fun extractUsername(): String {
        return binding.username.text.toString().substringBefore("@")
    }

    private fun saveUserPreferences() {
        val userPreferences = getSharedPreferences("user_$user", Context.MODE_PRIVATE)
        userPreferences.edit().putBoolean("isTwoFactorAuthEnabled", binding.checkBox.isChecked).apply()
    }

    private fun showLoadingDialog(message: String, action: (LoadingAlert) -> Unit) {
        val loading = LoadingAlert(this, message)
        loading.startAlertDialog()
        action(loading)
    }

    private fun showAlert(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Accept", null)
            .apply {
                when (title) {
                    SUCCESS_MESSAGE -> setOnDismissListener { lifecycleScope.launch { setUpKeys() } }
                    MNEMONICS_MESSAGE -> setOnDismissListener { lifecycleScope.launch { deploySmartContract() } }
                    SC_MESSAGE -> setOnDismissListener { finish() }
                }
            }

        builder.create().show()
    }

    private suspend fun setUpKeys() {
        showLoadingDialog("Generating keys, wait for your mnemonics... This process may take up some time, please be patient.") { loading ->
            lifecycleScope.launch {
                val mnemonic = withContext(Dispatchers.IO) {
                    Control.setUp(extractUsername(), binding.password.text.toString(), this@SignUpActivity)
                }
                withContext(Dispatchers.Main) {
                    loading.closeAlertDialog()
                    showAlert(MNEMONICS_MESSAGE, "These are your mnemonic words, please store them safely.\n\n$mnemonic")
                }
            }
        }
    }

    private suspend fun deploySmartContract() {
        showLoadingDialog("Generating Smart Contract... This may take a few minutes, please be patient.") { loading ->
            lifecycleScope.launch {
                val address = withContext(Dispatchers.IO) {
                    Control.deploySKMSC(this@SignUpActivity, user)
                }
                withContext(Dispatchers.Main) {
                    loading.closeAlertDialog()
                    showAlert(SC_MESSAGE, address)
                }
            }
        }
    }

}