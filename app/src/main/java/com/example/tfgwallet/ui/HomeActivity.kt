package com.example.tfgwallet.ui

import android.content.Context
import android.content.res.Configuration
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
import com.example.tfgwallet.model.KeyManagement
import com.example.tfgwallet.ui.fragments.HomeFragment
import com.example.tfgwallet.ui.fragments.KeysFragment
import com.example.tfgwallet.ui.fragments.SettingsFragment
import com.google.firebase.annotations.concurrent.Background
import io.ipfs.kotlin.model.NamedHash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.io.File
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
                val keyPair: Triple<BigInteger, BigInteger, ByteArray>? = KeyManagement.decryptRsa("$user/login$user.bin", user,this)
                if (keyPair != null) {
                    Log.i("Key", "Your private key is ${keyPair.first} and public key is ${keyPair.second}")
                    Log.i("Chain code", "Your chain code is ${BigInteger(keyPair.third)}")

                }
            }
        }

            replaceFragment(HomeFragment())
        val context = this
        GlobalScope.async(Dispatchers.IO) {
            connectToIPFS(context)
        }

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
    private suspend fun connectToIPFS(context: Context) {
        val res = GlobalScope.async(Dispatchers.IO) {
            val file: NamedHash
            try {
                IPFSManager.connect(context)
                //val file = ipfsManager.addFile(File(assets.("english.txt")))
                val string = IPFSManager.addString("test", "this is a test")
                val recovered = IPFSManager.getString(string.Hash)
                val recovered2 = IPFSManager.getString("bafkreife2klsil6kaxqhvmhgldpsvk5yutzm4i5bgjoq6fydefwtihnesa")
                return@async Pair(string, recovered + recovered2)
                showAlert("SUCCESS", "${file.Name} with hash ${file.Hash}")
            } catch (e: java.lang.Exception) {
                return@async e
                showAlert("ERROR", e.toString())
            }

        }
        val address = res.await()
        if (address is Pair<*, *>){
            val address2 = address as Pair<NamedHash, String>
            runOnUiThread { showAlert("address", address2.first.Name + " " + address2.second)}
        }
        else {
            runOnUiThread { showAlert("address", address.toString())}

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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }
}