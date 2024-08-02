package com.example.tfgwallet.ui


import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tfgwallet.R
import com.example.tfgwallet.control.Control
import com.example.tfgwallet.databinding.ActivityDappBrowserKeysBinding
import com.example.tfgwallet.databinding.FragmentKeysBinding
import com.example.tfgwallet.model.KeyItem
import com.example.tfgwallet.model.KeyManagement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DAppBrowserKeys : AppCompatActivity() {

    private lateinit var binding: ActivityDappBrowserKeysBinding
    private var pluginAddress: String = ""
    private var user: String = ""
    private lateinit var dAdapter: KeyItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDappBrowserKeysBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (intent.hasExtra("address")) {
            pluginAddress = intent.getStringExtra("address").toString()
        }
        if (intent.hasExtra("user")) {
            user = intent.getStringExtra("user").toString()
        }
        setupToolbar()
        setupDappKeys()
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun setupDappKeys() {
        val keysList = ArrayList<KeyItem>()
        val dappTitles = ArrayList<String>()
        if (pluginAddress.isNotEmpty()) {
            try {
                lifecycleScope.launch(Dispatchers.IO) {
                    val ref = Control.control(applicationContext, user, pluginAddress, "user_$user")
                    val hashes = Control.getDataFromIPFS(ref, applicationContext).split("\n")
                    hashes.forEach { hash ->
                        val data = Control.getDataFromIPFS(hash, applicationContext)
                        val decryptedData =
                            KeyManagement.decryptIPFSData(data, "user_$user", applicationContext)
                        if (decryptedData != null) {
                            Log.i("Decrypted data", decryptedData.toString())
                            keysList.add(decryptedDataToKeyItem(decryptedData.toTriple()))
                            dappTitles.add("ID: 0x" + decryptedData.fourth.toHexString(HexFormat.Default))
                        }
                    }

                    withContext(Dispatchers.Main) {
                        setupDappRecyclerView(keysList, dappTitles)
                    }
                }
            }catch (e: Exception) {
                Log.e("Error", e.message.toString())
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun decryptedDataToKeyItem(decryptedData: Triple<ByteArray, ByteArray, ByteArray>): KeyItem {
        return KeyItem(
            "0x" + decryptedData.first.toHexString(HexFormat.Default),
            "0x" + decryptedData.second.toHexString(HexFormat.Default),
            "0x" + decryptedData.third.toHexString(HexFormat.Default)
        )
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.browser_keys)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupDappRecyclerView(keysList: List<KeyItem>, dappTitles: List<String>) {
        dAdapter = KeyItemAdapter(keysList)
        val dAppRecyclerView = binding.dAppRecycler
        dAppRecyclerView.layoutManager = LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, false)
        dAppRecyclerView.adapter = dAdapter
    }

}

