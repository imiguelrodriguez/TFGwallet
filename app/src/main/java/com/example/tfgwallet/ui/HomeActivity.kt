package com.example.tfgwallet.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.tfgwallet.R
import com.example.tfgwallet.databinding.ActivityHomeBinding
import com.example.tfgwallet.model.IPFSManager
import com.example.tfgwallet.model.KeyManagement
import com.example.tfgwallet.ui.fragments.HomeFragment
import com.example.tfgwallet.ui.fragments.InfoFragment
import com.example.tfgwallet.ui.fragments.KeysFragment
import com.example.tfgwallet.ui.fragments.SettingsFragment
import io.ipfs.kotlin.model.NamedHash
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.math.BigInteger

private const val EXPIRATION_TIME = 15

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var user: String
    @OptIn(ExperimentalStdlibApi::class, DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val intent = intent
        var keyPair : Triple<BigInteger, BigInteger, ByteArray> = Triple(BigInteger.ZERO,
            BigInteger.ZERO, byteArrayOf(0)
        )
        if (intent.hasExtra("email")) {
            val email = intent.getStringExtra("email")
            if (email != null) {
                user = email.substringBefore("@")
                Log.i("Email", user)
                keyPair = KeyManagement.decryptRSA("$user/login$user.bin", user,this)!!
                Log.i("Key", "Your private key is ${keyPair.first} and public key is ${keyPair.second}")
                Log.i("Chain code", "Your chain code is ${BigInteger(keyPair.third)}")

            }
        }

            replaceFragment(HomeFragment())
        val context = this

        binding.bottomNavigationView.setOnItemSelectedListener {
            when(it.itemId) {
                R.id.home -> replaceFragment(HomeFragment())
                R.id.settings -> replaceFragment(SettingsFragment.newInstance(user))
                R.id.keys -> replaceFragment(KeysFragment.newInstance(user, keyPair.first.toString(16), keyPair.second.toString(16), keyPair.third.toHexString(
                    HexFormat.Default)))
                R.id.info -> replaceFragment(InfoFragment())
                else -> {
                    true
                }
            }
        }
    }
    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun connectToIPFS(context: Context) {
        val res = GlobalScope.async(Dispatchers.IO) {
            val file: NamedHash
            try {

                //val file = ipfsManager.addFile(File(assets.("english.txt")))
                val string = IPFSManager.addString("test", "this is a test", context)
                val recovered = IPFSManager.getString(string.Hash, context)
                val recovered2 = IPFSManager.getString("bafkreife2klsil6kaxqhvmhgldpsvk5yutzm4i5bgjoq6fydefwtihnesa", context)
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
      moveTaskToBack(true)
    }

    override fun onResume() {
        super.onResume()
        if (isSessionExpired()) {
            logoutUser()
        }
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
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Accept", null)
            .setCancelable(false)  // Prevents closing the dialog by clicking outside
        builder.create().show()
    }

    private fun isSessionExpired(): Boolean {
        val sharedPrefs = getSharedPreferences("user_${user}", Context.MODE_PRIVATE)
        val loginTimestamp = sharedPrefs.getLong("loginTimestamp", 0)
        val currentTime = System.currentTimeMillis()
        val fifteenMinutesInMillis = EXPIRATION_TIME * 60 * 1000
        return (currentTime - loginTimestamp) > fifteenMinutesInMillis
    }

    private fun logoutUser() {
        val loginIntent = Intent(this, MainActivity::class.java)
        loginIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        loginIntent.putExtra("session_expired", "The session has expired due to inactivity, please log in again.")
        startActivity(loginIntent)
        finish()
    }
}