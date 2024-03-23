package com.example.tfgwallet.model

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import org.bitcoinj.crypto.MnemonicCode
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Convert.Unit
import org.web3j.tx.Transfer
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.math.BigDecimal
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher

object Blockchain {
    private lateinit var web3: Web3j
    private const val PRIVATE_KEY_LENGTH: Int = 32
    fun connect(url: String) {
        try {
            web3 = Web3j.build(HttpService(url))
            Log.i("BC connection", "Successful connection to the blockchain.")
            Log.i("Ganache accounts", web3.ethAccounts().send().accounts.toString())

        } catch (e: Exception) {
            Log.e("BC connection error", "Error connecting to the blockchain.${e.printStackTrace().toString()}")
        }
    }

    fun send(credentials: Credentials, recipientAddress: String, value: Long ) {
        if (!::web3.isInitialized) {
            throw IllegalStateException("Blockchain is not connected. Call connect() first.")
        }
        try {
            val receipt = Transfer.sendFunds(
                web3,
                credentials,
                recipientAddress,
                BigDecimal.valueOf(value),
                Unit.ETHER
            ).send()
            Log.i("Transaction status", receipt.status)
        } catch (e: Exception) {
            Log.e("Transaction error", "Error in transaction, try again.$e")
            Log.e("Transaction error", e.printStackTrace().toString())
        }

    }

    fun generateMnemonic(): MutableList<String> {
        val secureRandom = SecureRandom()
        val entropy = ByteArray(16)
        secureRandom.nextBytes(entropy)
        return MnemonicCode.INSTANCE.toMnemonic(entropy)
    }

    fun generateKeyPair(mnemonicList: MutableList<String>, password: String): Bip32ECKeyPair {
        val seed = MnemonicCode.toSeed(mnemonicList, password)
        return Bip32ECKeyPair.generateKeyPair(seed)
    }

    private fun createDirectory(directoryPath: String): Boolean {
        try {
            val f = File(directoryPath)
            // Check if the directory already exists
            return if (!f.exists()) {
                // Attempt to create the directory and its parent directories if they don't exist
                f.mkdirs()
                Log.i("DIR creation", "Directory created successfully: $directoryPath")
                true
            } else {
                Log.e("DIR creation", "Failed to create directory: $directoryPath")
                false
            }
        }catch(e: Exception) {
            Log.e("File error", e.toString())
        }
        return false
    }

    fun encryptRSA(data: Bip32ECKeyPair, userId: String, context: Context) {
        // Generate RSA key pair
        val keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
        keyPairGenerator.initialize(
            KeyGenParameterSpec.Builder(
                "user_rsa_key_$userId",
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(2048)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                .build()
        )
        val keyPair = keyPairGenerator.generateKeyPair()

        // Encrypt data with RSA public key
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, keyPair.public)

        val result = ByteArray(data.privateKey.toByteArray().size + data.publicKey.toByteArray().size + 1)
        result[0] = data.privateKey.toByteArray().size.toByte()
        val concatenate = data.privateKey.toByteArray() + data.publicKey.toByteArray()
        System.arraycopy(concatenate, 0, result, 1, concatenate.size)

        val encryptedData = cipher.doFinal(result)
        Log.i("Key length", "Private key size is ${data.privateKey.toByteArray().size} and public key size is ${data.publicKey.toByteArray().size}")
        Log.i("Encrypt", "Encrypting private key ${data.privateKey} and public key ${data.publicKey}")
        val directory = context.getDir("users", Context.MODE_PRIVATE)
        createDirectory("${directory.path}/$userId")
        val outputStream = FileOutputStream("${directory.path}/$userId/login$userId.bin")
        val objectOutputStream = ObjectOutputStream(outputStream)
        objectOutputStream.writeObject(encryptedData)
        objectOutputStream.close()

    }


    fun decryptRsa(inputFile: String, userId: String, context: Context): Pair<BigInteger, BigInteger>? {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        // Retrieve RSA private key
        val privateKeyEntry = keyStore.getEntry("user_rsa_key_$userId", null) as? KeyStore.PrivateKeyEntry ?: return null
        val privateKey = privateKeyEntry.privateKey

        // Decrypt data using RSA private key
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        val directory = context.getDir("users", Context.MODE_PRIVATE)
        val inputStream = FileInputStream( "${directory.path}/$inputFile")
        val objectInputStream = ObjectInputStream(inputStream)
        val encryptedData = objectInputStream.readObject() as ByteArray
        objectInputStream.close()
        val decryptedData = cipher.doFinal(encryptedData)
        val keyLength = decryptedData[0].toInt()
        val privateKeyBytes = decryptedData.copyOfRange(1, keyLength + 1)
        val publicKeyBytes = decryptedData.copyOfRange(keyLength + 1, decryptedData.size)

        // Convert byte arrays back to BigIntegers
        val privateKeyBigInt = BigInteger(privateKeyBytes)
        val publicKeyBigInt = BigInteger(publicKeyBytes)

        return Pair(privateKeyBigInt, publicKeyBigInt)

    }

}