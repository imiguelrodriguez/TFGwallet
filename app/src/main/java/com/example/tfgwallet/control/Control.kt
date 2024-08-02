package com.example.tfgwallet.control

import android.content.Context
import android.util.Log
import com.example.tfgwallet.model.Blockchain
import com.example.tfgwallet.model.IPFSManager
import com.example.tfgwallet.model.KeyManagement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.Credentials
import java.math.BigInteger

/**
 * MVC: Controller for UI
 * @author Ignacio Miguel Rodríguez
 */
class Control {
    companion object {
        private const val TAG = "Control"

        /**
         * Method that asynchronously deploys the SKM Smart Contract.
         * @param context app context
         * @param user String indicating the user logged in
         */
        suspend fun deploySKMSC(context: Context, user: String): String {
            return withContext(Dispatchers.IO) {
                Blockchain.deploySKMSC(context, "user_$user").toString()
            }

        }

        /**
         * Method that performs the set up of the user's account initializing and funding
         * his account in the blockchain. It also stores the encrypted master keys in the secure storage
         * of the device.
         * @param user username.
         * @param password user password.
         * @param context app context.
         * @return 24 mnemonic words.
         */
        suspend fun setUp(user: String, password: String, context: Context): String {
            val mnemonicList = KeyManagement.generateMnemonic()
            val mnemonic = mnemonicList.joinToString(separator = " ")
            val masterKeyPair = KeyManagement.generateKeyPair(mnemonicList, password)

            KeyManagement.encryptRSA(masterKeyPair, user, context)

            val acc = Credentials.create(masterKeyPair)
            Log.i(TAG, "Address ${acc.address}")

            withContext(Dispatchers.IO) {
                Blockchain.send(context, acc.address)
            }
            return mnemonic
        }

        suspend fun generateBrowserKeys(
            context: Context,
            user: String,
            contents: String,
            prefsName: String
        ): String {
            val keyPair = KeyManagement.decryptRSA("$user/login$user.bin", user, context)
            return keyPair?.let {
                handleBrowserKeyGeneration(context, user, contents, prefsName, it)
            } ?: run {
                Log.i(TAG, "Problem with key")
                "Error in key"
            }
        }

        @OptIn(ExperimentalStdlibApi::class)
        private suspend fun handleBrowserKeyGeneration(
            context: Context, user: String, contents: String, prefsName: String,
            keyPair: Triple<BigInteger, BigInteger, ByteArray>
        ): String {
            val id = contents.takeLast(64)
            Log.i(TAG, "Private key: ${keyPair.first}, Public key: ${keyPair.second}")
            Log.i(TAG, "Chain code: ${BigInteger(keyPair.third)}")
            val path = intArrayOf(
                id.substring(0, 8).hexToInt(HexFormat.Default) or Bip32ECKeyPair.HARDENED_BIT
            )
            val sessionKey = contents.take(contents.length - 64).hexToByteArray()

            return try {
                val master = Bip32ECKeyPair.create(keyPair.first, keyPair.third)
                val brKeyPair = KeyManagement.generateBrowserKeyPair(master, path)
                Log.i(TAG, "Browser Private Key: ${brKeyPair.privateKey}")
                KeyManagement.encryptRSA(brKeyPair, user, context, browser = true, id)
                val from = Credentials.create(master)
                Log.i(TAG, "Account Address: ${from.address}")
                handleBlockchainAddDevice(context, from, brKeyPair, prefsName, sessionKey)
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}", e)
                e.message ?: "Unknown error"
            }
        }

        private suspend fun handleBlockchainAddDevice(
            context: Context,
            from: Credentials,
            plugin: Bip32ECKeyPair,
            prefsName: String,
            sessionKey: ByteArray
        ): String {
            var status: String
            val pluginAddress = Credentials.create(plugin).address
            withContext(Dispatchers.IO) {
                status = Blockchain.addDevice(from, pluginAddress, context, prefsName).toString()
                if (status != "0x1") {
                    status = "Plugin already configured"
                } else {
                    Blockchain.send(context, pluginAddress) // fund plugin account
                    updatePluginAddresses(context, prefsName, pluginAddress)
                    val encrypted = KeyManagement.encryptWithSessionKey(plugin, sessionKey)
                    Blockchain.modTemp(from, encrypted, context, prefsName)
                }
            }
            return status
        }

        private fun updatePluginAddresses(
            context: Context,
            prefsName: String,
            pluginAddress: String
        ) {
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val prevPlugins = prefs.getString("pluginAddresses", "")
            val updatedPlugins = if (prevPlugins.isNullOrEmpty()) {
                pluginAddress
            } else {
                "$prevPlugins;$pluginAddress"
            }
            prefs.edit().putString("pluginAddresses", updatedPlugins).apply()
        }

        suspend fun control(
            context: Context,
            user: String,
            pluginAddress: String,
            prefsName: String
        ): String {
            val keyPair = KeyManagement.decryptRSA("$user/login$user.bin", user, context)
            return keyPair?.let {
                val master = Bip32ECKeyPair.create(it.first, it.third)
                val from = Credentials.create(master)
                withContext(Dispatchers.IO) {
                    Blockchain.getRef(from, pluginAddress, context, prefsName).toString()
                }
            } ?: ""
        }

        // TODO add browser key storage...
        fun recoverKeys(email: String, mnemonic: String, password: String, context: Context) {
            // Root Secret Key (SK0) and Root Chain code C0.
            val masterKeyPair =
                KeyManagement.generateKeyPair(mnemonic.split(" ") as MutableList<String>, password)
            val user = email.substringBefore("@")
            // Store SK0 in the smartphone’s secure storage
            KeyManagement.encryptRSA(masterKeyPair, user, context)
            val acc = Credentials.create(masterKeyPair)

            // Look for the SKM SC deployed using the key pair (SK0, P K0) int the BC, and store its hash, hSC , in the app storage
            val contractAddress = Blockchain.lookForSChashInBC(acc.address)
            val userPreferences = context.getSharedPreferences("user_$user", Context.MODE_PRIVATE)
            userPreferences.edit().putString("contractAddress", contractAddress).apply()
            val sharedPreferences = context.getSharedPreferences("credentials", Context.MODE_PRIVATE)
            val hashedPassword = KeyManagement.sha512(password)
            with(sharedPreferences.edit()) {
                putString("email_$user", email)
                putString("password_$user", hashedPassword)
                apply()
            }
        }

        fun getDataFromIPFS(ref: String, context: Context): String {
            return IPFSManager.getString(ref, context)
        }

    }
}
