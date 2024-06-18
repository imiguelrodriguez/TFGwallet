package com.example.tfgwallet.model

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import org.bitcoinj.crypto.MnemonicCode
import org.web3j.crypto.Bip32ECKeyPair
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


object KeyManagement {

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
        } catch (e: Exception) {
            Log.e("File error", e.toString())
        }
        return false
    }
    fun encryptRSA(data: Bip32ECKeyPair, userId: String, context: Context) {
        // Generate RSA key pair
        val keyPairGenerator =
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
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

        val result =
            ByteArray(data.privateKey.toByteArray().size + data.publicKey.toByteArray().size + data.chainCode.size + 2)
        result[0] = data.privateKey.toByteArray().size.toByte()
        result[1] = data.publicKey.toByteArray().size.toByte()
        val concatenate =
            data.privateKey.toByteArray() + data.publicKey.toByteArray() + data.chainCode
        System.arraycopy(concatenate, 0, result, 2, concatenate.size)

        val encryptedData = cipher.doFinal(result)
        Log.i(
            "Key length",
            "Private key size is ${data.privateKey.toByteArray().size} and public key size is ${data.publicKey.toByteArray().size}"
        )
        Log.i(
            "Encrypt",
            "Encrypting private key ${data.privateKey} and public key ${data.publicKey}"
        )
        Log.i("Chain code", "Chain code is ${BigInteger(data.chainCode)}")
        val directory = context.getDir("users", Context.MODE_PRIVATE)
        createDirectory("${directory.path}/$userId")
        val outputStream = FileOutputStream("${directory.path}/$userId/login$userId.bin")
        val objectOutputStream = ObjectOutputStream(outputStream)
        objectOutputStream.writeObject(encryptedData)
        objectOutputStream.close()
    }
    fun decryptRsa(
        inputFile: String,
        userId: String,
        context: Context
    ): Triple<BigInteger, BigInteger, ByteArray>? {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        // Retrieve RSA private key
        val privateKeyEntry =
            keyStore.getEntry("user_rsa_key_$userId", null) as? KeyStore.PrivateKeyEntry
                ?: return null
        val privateKey = privateKeyEntry.privateKey

        // Decrypt data using RSA private key
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        val directory = context.getDir("users", Context.MODE_PRIVATE)
        val inputStream = FileInputStream("${directory.path}/$inputFile")
        val objectInputStream = ObjectInputStream(inputStream)
        val encryptedData = objectInputStream.readObject() as ByteArray
        objectInputStream.close()
        val decryptedData = cipher.doFinal(encryptedData)
        val privateKeyLength = decryptedData[0].toInt()
        val publicKeyLength = decryptedData[1].toInt()
        val privateKeyBytes = decryptedData.copyOfRange(2, privateKeyLength + 2)
        val publicKeyBytes =
            decryptedData.copyOfRange(privateKeyLength + 2, privateKeyLength + publicKeyLength + 2)
        val chainCode =
            decryptedData.copyOfRange(privateKeyLength + publicKeyLength + 2, decryptedData.size)

        // Convert byte arrays back to BigIntegers
        val privateKeyBigInt = BigInteger(privateKeyBytes)
        val publicKeyBigInt = BigInteger(publicKeyBytes)

        return Triple(privateKeyBigInt, publicKeyBigInt, chainCode)
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun encryptWithSessionKey(keyPair: Bip32ECKeyPair, sessionKey: ByteArray): ByteArray {
        val secureRandom = SecureRandom()
        var iv = ByteArray(12)
        secureRandom.nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(sessionKey, "AES")
        val spec = GCMParameterSpec(128, iv)
        //val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec)
        val beforeEnc = (keyPair.privateKey.toByteArray()+keyPair.publicKey.toByteArray()+keyPair.chainCode).toHexString(
            HexFormat.Default)
        Log.i("Before enc", beforeEnc)
        // FOR THE MOMENT: CONCATENATE IV SO THAT THE PLUGIN CAN DECRYPT
        val encryptedBytes = cipher.doFinal(keyPair.privateKey.toByteArray()+keyPair.publicKey.toByteArray()+keyPair.chainCode)
        Log.i("EncryptedData", encryptedBytes.toString())
        return iv+encryptedBytes
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

    fun generateBrowserKeyPair(keyPair: Bip32ECKeyPair, path: IntArray): Bip32ECKeyPair {
        return Bip32ECKeyPair.deriveKeyPair(keyPair, path)
    }

}
