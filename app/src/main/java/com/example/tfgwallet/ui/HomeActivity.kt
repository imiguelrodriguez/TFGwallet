package com.example.tfgwallet.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.tfgwallet.R
import com.example.tfgwallet.control.Control
import com.example.tfgwallet.databinding.ActivityHomeBinding
import com.example.tfgwallet.model.Blockchain
import com.example.tfgwallet.model.IPFSManager
import com.example.tfgwallet.ui.fragments.HomeFragment
import com.example.tfgwallet.ui.fragments.KeysFragment
import com.example.tfgwallet.ui.fragments.SettingsFragment
import com.google.firebase.annotations.concurrent.Background
import java.math.BigInteger



class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        val intent = intent

        if (intent.hasExtra("email")) {
            val email = intent.getStringExtra("email")
            if (email != null) {
                val user = email.substringBefore("@")
                Log.i("Email", user)
                val keyPair: Triple<BigInteger, BigInteger, ByteArray>? = Blockchain.decryptRsa("$user/login$user.bin", user,this)
                if (keyPair != null) {
                    Log.i("Key", "Your private key is ${keyPair.first} and public key is ${keyPair.second}")
                    Log.i("Chain code", "Your chain code is ${BigInteger(keyPair.third)}")
                    Control.deploySKM_SC()

                }
            }
        }

            replaceFragment(HomeFragment())

        //val policy = ThreadPolicy.Builder().permitAll().build()

        //StrictMode.setThreadPolicy(policy)
        //connectToIPFS()

        binding.bottomNavigationView.setOnItemSelectedListener {
            when(it.itemId) {
                R.id.home -> replaceFragment(HomeFragment())
                R.id.settings -> replaceFragment(SettingsFragment())
                R.id.keys -> replaceFragment(KeysFragment())
                else -> {
                    true
                }
            }
        }
    }

    @Background
    private fun connectToIPFS() {
        try {
            val ipfsManager = IPFSManager()
            val file = ipfsManager.addFile("app/src/main/assets/english.txt")
            showAlert("SUCCESS", "${file.Name} with hash ${file.Hash}")
        } catch (e: java.lang.Exception) {
            showAlert("ERROR", e.toString())
        }
    }

    override fun onBackPressed() {
      finish()
    }
    private fun replaceFragment(fragment: Fragment): Boolean {
        val fragmentManager: FragmentManager  = supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frameLayout, fragment)
        fragmentTransaction.commit()
        return true
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