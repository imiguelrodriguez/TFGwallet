package com.example.tfgwallet.control

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.core.content.ContextCompat.getString
import com.example.tfgwallet.R
import com.example.tfgwallet.databinding.ActivitySignupBinding
import com.example.tfgwallet.model.Blockchain
import com.example.tfgwallet.model.KeyManagement
import com.example.tfgwallet.model.Protocols
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.Credentials
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.math.BigInteger
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

/**
 * MVC: Controller for UI
 * @author Ignacio Miguel Rodríguez
 */
class Control {
    companion object {
        /**
         * Method that calls the Blockchain.connect() method in an asynchronous way.
         * @param context app context
         * @param user String indicating the user logged in
         */
        suspend fun deploySKM_SC(context: Context, user: String): String {
            val contractAddress: String
            val res = GlobalScope.async(Dispatchers.IO) {
                return@async Blockchain.deploySKM_SC(context, "user_$user")
            }
            contractAddress = res.await()
            return contractAddress
        }

        fun executeBIP32(seed: UByteArray): Protocols.Companion.Bip32 {
            return Protocols.Companion.Bip32(seed)
        }

        fun executeBIP39(size: Int, password: String, context: Context): Pair<String, UByteArray> {
            val bip39 = Protocols.Companion.Bip39(size, password, context)
            return bip39.getSeed()
        }

        fun storeKeys(bip32: Protocols.Companion.Bip32) {

            // Initialize the cipher with the new IV
            val keyGenerator = KeyGenerator
                .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")

            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                "secret_key",
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(false) // Disable randomized encryption to use custom IV
                .build()

            keyGenerator.init(keyGenParameterSpec)
            val secretKey = keyGenerator.generateKey()

            // Initialize the cipher with the new IV
            var cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            // Encrypt the data
            val keyPair = Pair(bip32.privateKey, bip32.publicKey)
            val outputStream = ByteArrayOutputStream()
            val objectOutputStream = ObjectOutputStream(outputStream)
            objectOutputStream.writeObject(keyPair)
            objectOutputStream.close()
            val iv = cipher.iv

            val encryptedData = cipher.doFinal(outputStream.toByteArray())

            // decrypt
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)

            val s = keyStore
                .getEntry("secret_key", null)
            val sKey = (s as KeyStore.SecretKeyEntry).secretKey
            cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, sKey, spec)

            val decodedData = cipher.doFinal(encryptedData)
            val inputStream = ByteArrayInputStream(decodedData)
            val objectInputStream = ObjectInputStream(inputStream)
            val keyPairDecoded = objectInputStream.readObject() as Pair<PrivateKey, PublicKey>
            objectInputStream.close()

        }


        suspend fun setUp(binding: ActivitySignupBinding, context: Context): String {
            val mnemonicList = KeyManagement.generateMnemonic()

            val mnemonic = mnemonicList.joinToString(separator = " ")
            val masterKeyPair = KeyManagement.generateKeyPair(mnemonicList, binding.password.text.toString())

            val user = binding.username.text.toString().substringBefore("@")
            KeyManagement.encryptRSA(masterKeyPair, user, context)

            val acc = Credentials.create(masterKeyPair)
            Log.i("Address", acc.address)

            val res = GlobalScope.async(Dispatchers.IO) {
                Blockchain.send(context, acc.address)
            }
            res.await()
            return mnemonic
        }

        @OptIn(ExperimentalStdlibApi::class)
        suspend fun generateBrowserKeys(context: Context, user: String, contents: String, prefs_name: String): String {
            val keyPair: Triple<BigInteger, BigInteger, ByteArray>? = KeyManagement.decryptRsa("$user/login$user.bin", user, context)
            if (keyPair != null) {
                var id = contents.takeLast(64)
                Log.i("Key", "Your private key is ${keyPair.first} and public key is ${keyPair.second}")
                Log.i("Chain code", "Your chain code is ${BigInteger(keyPair.third)}")
                val path = intArrayOf(id.substring(0, 8).hexToInt(HexFormat.Default) or Bip32ECKeyPair.HARDENED_BIT)
                val sessionKey = contents.take(contents.length - 64).hexToByteArray()
                try {
                    val master = Bip32ECKeyPair.create(keyPair.first, keyPair.third)
                    val brKeyPair = KeyManagement.generateBrowserKeyPair(master, path)
                    Log.i("Browser Key", "Private key is ${brKeyPair.privateKey}")
                    Log.i("Key size", "Private ${brKeyPair.privateKey.toByteArray().size}  Public ${brKeyPair.publicKey.toByteArray().size}")
                    val from = Credentials.create(master)
                    Log.i("Address", from.address)
                    var status = ""
                    val res = GlobalScope.async(Dispatchers.IO) {
                        status = Blockchain.addDevice(from, brKeyPair, context, prefs_name).toString()
                        //  removed +id.hexToByteArray() + ByteArray(64) to the output of encryption
                        val encrypted = KeyManagement.encryptWithSessionKey(brKeyPair, sessionKey)
                        Blockchain.modTemp(from, encrypted, context, prefs_name)
                    }
                    res.await()
                    return status
                } catch (e: Exception) {
                    Log.e("Error", e.toString())
                    return "Error"
                }
            }
            else{
                Log.i("Problem", "problem with key")
                return "Error"
            }
        }


    }
}