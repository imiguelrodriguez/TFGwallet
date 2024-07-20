package com.example.tfgwallet.ui

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.tfgwallet.R
import com.example.tfgwallet.control.Control
import com.example.tfgwallet.databinding.ActivityQrscannerBinding
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QRCodeScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrscannerBinding
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var qrCodeLauncher: ActivityResultLauncher<ScanOptions>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrscannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        setupPermissionLauncher()
        setupQRCodeLauncher()
        checkCameraPermission()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.qr_code_scanner)
    }

    private fun setupPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) showCamera()
            else showToast("Camera permission required")
        }
    }

    private fun setupQRCodeLauncher() {
        qrCodeLauncher = registerForActivityResult(
            ScanContract()
        ) { result: ScanIntentResult ->
            if (result.contents == null) showToast("Cancelled")
            else handleQRCodeResult(result.contents)
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                showCamera()
            }
            shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA) -> {
                showToast("Camera permission required")
            }
            else -> {
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }
    }

    private fun showCamera() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("SCAN QR CODE")
            setCameraId(0)
            setBeepEnabled(false)
            setBarcodeImageEnabled(true)
            setOrientationLocked(false)
        }
        qrCodeLauncher.launch(options)
    }

    private fun handleQRCodeResult(contents: String) {
        lifecycleScope.launch {
            val loading = LoadingAlert(
                this@QRCodeScannerActivity,
                "Adding device to Smart Contract... This process can take up some minutes, be patient."
            ).apply { startAlertDialog() }

            val status = withContext(Dispatchers.IO) { processQRCodeResult(contents) }
            loading.closeAlertDialog()
            updateUIBasedOnStatus(status)
        }
    }

    private suspend fun processQRCodeResult(contents: String): String {
        val preferences: SharedPreferences = getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
        val user = preferences.getString("lastLoggedInUserId", null)
        Log.i("User", user.toString())

        return user?.substringBefore("@")?.let {
            Control.generateBrowserKeys(this, it, contents, "user_$it")
        } ?: ""
    }

    private fun updateUIBasedOnStatus(status: String) {
        binding.textResult.apply {
            setText(if (status == "0x1") {
                getString(R.string.plugin_success)
            } else {
                getString(R.string.plugin_failed, status)
            })
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
}