package com.example.tfgwallet.control

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.core.content.ContextCompat.getString
import com.example.tfgwallet.R
import com.example.tfgwallet.databinding.ActivitySignupBinding
import com.example.tfgwallet.model.Blockchain
import com.example.tfgwallet.model.Protocols
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
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


class Control {
    companion object {
        suspend fun deploySKM_SC(context: Context): String {
            var contractAddress = ""
            val IPaddress = getString(context, R.string.IP)
            val bc = Blockchain
            val res = GlobalScope.async(Dispatchers.IO) {
                bc.connect(IPaddress)
                return@async bc.deploySKM_SC()
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


        fun setUp(binding: ActivitySignupBinding, context: Context): String {
            val mnemonicList = Blockchain.generateMnemonic()

            val mnemonic = mnemonicList.joinToString(separator = " ")
            val masterKeyPair = Blockchain.generateKeyPair(mnemonicList, binding.password.text.toString())

            val user = binding.username.text.toString().substringBefore("@")
            Blockchain.encryptRSA(masterKeyPair, user, context)

            val acc = Credentials.create(masterKeyPair)
            val ganacheAcc = Credentials.create("0x6bb31d0829a07ada4a0198536266c45228bbbdac0789314a64faf12fc6dd4965")
            GlobalScope.launch(Dispatchers.IO) {
                val bc = Blockchain
                bc.connect("http://192.168.0.105:7545")
                bc.send(ganacheAcc, acc.address, 1)
            }


            return mnemonic
        }

        @OptIn(ExperimentalStdlibApi::class)
        fun generateBrowserKeys(context: Context, user: String, contents: String) {
            val keyPair: Triple<BigInteger, BigInteger, ByteArray>? = Blockchain.decryptRsa("$user/login$user.bin", user, context)
            if (keyPair != null) {
                Log.i("Key", "Your private key is ${keyPair.first} and public key is ${keyPair.second}")
                Log.i("Chain code", "Your chain code is ${BigInteger(keyPair.third)}")
            }
            var id = contents.takeLast(64)
            /*if (keyPair != null) {
                val path: IntArray
                try {
                    val path = id.hexToInt(HexFormat.Default)
                } catch (exception: Exception) {
                    val path = IntArray(1)
                }

                try {
                    val brKeyPair = Blockchain.generateBrowserKeyPair(Bip32ECKeyPair.create(keyPair.first, keyPair.third), path)
                    Log.i("Browser Key", "Private key is ${brKeyPair.privateKey}")
                   // Blockchain.addDevice(brKeyPair.publicKey.toString())
                } catch (e: Exception) {
                    Log.e("Error", e.toString())
                }
            }*/

        }
    }
}