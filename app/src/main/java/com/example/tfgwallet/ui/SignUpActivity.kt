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
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(binding.username.text.toString(),
                    binding.password.text.toString()).addOnCompleteListener {
                    if (it.isSuccessful) {
                        val userPreferences = getSharedPreferences("user_${binding.username.text.toString().substringBefore("@")}", Context.MODE_PRIVATE)
                        userPreferences.edit().putBoolean("isTwoFactorAuthEnabled", binding.checkBox.isChecked).apply()

                        showAlert("Success!","User ${it.result.user?.email} has been registered successfully.")
                        GlobalScope.async(Dispatchers.IO) {
                            deploySC()
                        }
                    } else {
                        showAlert("Error","An error has occurred while trying to sign up.")
                    }
                }
            }
        }

    }

    private suspend fun deploySC() {
        val context = this
        val res = GlobalScope.async(Dispatchers.IO) {
            return@async Control.deploySKM_SC(context)
        }
        val address = res.await()
        runOnUiThread { showAlert("address", address) }
    }
    private fun showAlert(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("Accept", null)
        if (title == "Success!") builder.setOnDismissListener{setUpKeysLib()}
        val dialog: AlertDialog = builder.create()
        dialog.show()
        if (title == "Mnemonic words") dialog.setOnDismissListener { GlobalScope.async(Dispatchers.IO) {
            deploySC()
        } }
        if(title == "address") dialog.setOnDismissListener{finish()}
    }

    private fun setUpKeys() {
        val bip39 = Control.executeBIP39(128, binding.password.text.toString(), this)
        showAlert("Mnemonic words", "These are your mnemonic words, please store them safely.\n\n" +
                bip39.first)

        // You can continue your sequential logic here
        val bip32 = Control.executeBIP32(bip39.second)
        Control.storeKeys(bip32)
    }

    fun setUpKeysLib() {
        val mnemonic = Control.setUp(binding, this)
        showAlert("Mnemonic words", "These are your mnemonic words, please store them safely.\n" +
                "\n $mnemonic")
    }
}