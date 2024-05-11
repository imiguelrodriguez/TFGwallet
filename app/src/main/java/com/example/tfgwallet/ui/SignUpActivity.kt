package com.example.tfgwallet.ui

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.tfgwallet.control.Control
import com.example.tfgwallet.databinding.ActivitySignupBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async


class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    private lateinit var user: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        FirebaseApp.initializeApp(this)
        setup(binding)
    }

    private fun setup(binding: ActivitySignupBinding) {

        binding.signUpButton.setOnClickListener {
            if (binding.username.text.isNotEmpty() && binding.password.text.isNotEmpty()) {
                val loading = LoadingAlert(this, "Creating account...")
                loading.startAlertDialog()
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(binding.username.text.toString(),
                    binding.password.text.toString()).addOnCompleteListener {
                    if (it.isSuccessful) {
                        user = binding.username.text.toString().substringBefore("@")
                        val userPreferences = getSharedPreferences("user_$user", Context.MODE_PRIVATE)
                        userPreferences.edit().putBoolean("isTwoFactorAuthEnabled", binding.checkBox.isChecked).apply()
                        loading.closeAlertDialog()
                        showAlert("Success!","User ${it.result.user?.email} has been registered successfully.")

                    } else {
                        loading.closeAlertDialog()
                        showAlert("Error","An error has occurred while trying to sign up.")
                    }
                }
            }
        }

    }

    private suspend fun deploySC(user: String) {
        val context = this
        val loading = LoadingAlert(this, "Generating Smart Contract...")
        runOnUiThread { loading.startAlertDialog() }
        val res = GlobalScope.async(Dispatchers.IO) {
            return@async Control.deploySKM_SC(context, user)
        }
        val address = res.await()

        runOnUiThread { loading.closeAlertDialog()
            showAlert("address", address) }
    }
    private fun showAlert(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("Accept", null)
        if (title == "Success!") builder.setOnDismissListener{ GlobalScope.async(Dispatchers.IO) {
            setUpKeysLib()
        }}
        val dialog: AlertDialog = builder.create()
        dialog.show()
        if (title == "Mnemonic words") dialog.setOnDismissListener { GlobalScope.async(Dispatchers.IO) {
            deploySC(user)
        } }
        if(title == "address") dialog.setOnDismissListener{finish()}
    }

    private suspend fun setUpKeysLib() {
        val loading = LoadingAlert(this, "Generating keys, wait for your mnemonics...")
        runOnUiThread {
             loading.startAlertDialog()
        }
        val mnemonic = Control.setUp(binding, this)
        runOnUiThread {

            loading.closeAlertDialog()
            showAlert("Mnemonic words", "These are your mnemonic words, please store them safely.\n" +
                "\n $mnemonic")}
    }
}