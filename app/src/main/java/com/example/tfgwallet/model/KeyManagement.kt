package com.example.tfgwallet.model

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import org.bitcoinj.crypto.MnemonicCode
import org.bouncycastle.jce.ECNamedCurveTable
import org.web3j.crypto.Bip32ECKeyPair
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.math.BigInteger
import java.security.Key
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
) {
    fun toTriple(): Triple<A, B, C> {
        return Triple(first, second, third)
    }
}

object KeyManagement {

    /**
     * Method that creates a directory for the current user using the specified path.
     * @param directoryPath  String indicating the path.
     * @return true if created, false otherwise
     */
    private fun createDirectory(directoryPath: String): Boolean {
        return try {
            val directory = File(directoryPath)
            if (!directory.exists()) {
                val created = directory.mkdirs()
                if (created) {
                    Log.i("DIR creation", "Directory created successfully: $directoryPath")
                } else {
                    Log.e("DIR creation", "Failed to create directory: $directoryPath")
                }
                created
            } else {
                Log.i("DIR creation", "Directory already exists: $directoryPath")
                false
            }
        } catch (e: Exception) {
            Log.e("File error", "Error creating directory: ${e.message}")
            false
        }
    }

    private fun generateRSAkey(userId: String): KeyPair {
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
       return keyPairGenerator.generateKeyPair()
    }

    /**
     * Method that encrypts a BIP32 KeyPair using the RSA algorithm. Then, it stores the encrypted
     * data in a binary file for the current user.
     * The format followed for encryption is:
     * size of private key in bytes + size of public key in bytes + private key + public key + chain code
     * @param data the BIP32 KeyPair
     * @param userId String indicating the user name
     * @param context app context
     */
    fun encryptRSA(data: Bip32ECKeyPair, userId: String, context: Context, browser: Boolean = false, browserID: String = "") {
        try {
            // Generate RSA key pair
            val keyPair: KeyPair = if (browser)
                retrieveRSAkey(userId)!!
            else
                generateRSAkey(userId)

            // Encrypt data with RSA public key
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, keyPair.public)

            val privateKeyBytes = data.privateKey.toByteArray()
            val publicKeyBytes = data.publicKey.toByteArray()
            val result = ByteArray(2 + privateKeyBytes.size + publicKeyBytes.size + data.chainCode.size)
            result[0] = privateKeyBytes.size.toByte()
            result[1] = publicKeyBytes.size.toByte()

            System.arraycopy(privateKeyBytes, 0, result, 2, privateKeyBytes.size)
            System.arraycopy(publicKeyBytes, 0, result, 2 + privateKeyBytes.size, publicKeyBytes.size)
            System.arraycopy(data.chainCode, 0, result, 2 + privateKeyBytes.size + publicKeyBytes.size, data.chainCode.size)

            val encryptedData = cipher.doFinal(result)

            // get users app directory and create a new one for the new user
            val directory = context.getDir("users", Context.MODE_PRIVATE)
            if (browser) {
                if (createDirectory("${directory.path}/$userId/browsers/")) {
                    val filePath = "${directory.path}/$userId/browsers/$browserID.bin"
                    FileOutputStream(filePath).use { fileOut ->
                        ObjectOutputStream(fileOut).use { objOut ->
                            objOut.writeObject(encryptedData)
                        }
                    }
                }
            }
            else {
                if (createDirectory("${directory.path}/$userId")) {
                    val filePath = "${directory.path}/$userId/login$userId.bin"
                    FileOutputStream(filePath).use { fileOut ->
                        ObjectOutputStream(fileOut).use { objOut ->
                            objOut.writeObject(encryptedData)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Encrypt error", "Error during encryption: ${e.message}")
        }
    }

    private fun retrieveRSAkey(userId: String): KeyPair? {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        // Retrieve RSA private key
        val privateKeyEntry =
            keyStore.getEntry("user_rsa_key_$userId", null) as? KeyStore.PrivateKeyEntry
                ?: return null
        val privateKey: PrivateKey = privateKeyEntry.privateKey
        val publicKey: PublicKey = privateKeyEntry.certificate.publicKey
        // Return the KeyPair
        return KeyPair(publicKey, privateKey)
    }

    fun getBrowserKeysPaths(context: Context, userId: String): List<String> {
        val directory = context.getDir("users", Context.MODE_PRIVATE)
        val path = "${directory.path}/$userId/browsers/"
        val browserKeysDirectory = File(path)

        // Return a list of all file paths in the directory
        return browserKeysDirectory.listFiles()?.map { it.path } ?: emptyList()
    }

    /**
     * Method that gets the binary file where the keys are stored and it decrypts the data in a binary file for the current user.decrypts the master BIP32 KeyPair using the RSA algorithm.
     * using the RSA key securely stored in the device storage. The format followed for encryption is:
     * size of private key in bytes + size of public key in bytes + private key + public key + chain code
     * @param inputFile String with the path of the file where the encrypted keys are stored.
     * @param userId String indicating the user name
     * @param context app context
     * @return Triple containing <privateKey, publicKey, chainCode>
     */
    fun decryptRSA(path: String, userId: String, context: Context): Triple<BigInteger, BigInteger, ByteArray>? {
        return try {
            val keyPair = retrieveRSAkey(userId)
            // Decrypt data using RSA private key
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            if (keyPair != null) {
                cipher.init(Cipher.DECRYPT_MODE, keyPair.private)
            }

            val directory = context.getDir("users", Context.MODE_PRIVATE)
            val filePath = "${directory.path}/$path"
            val encryptedData: ByteArray = FileInputStream(filePath).use { fileIn ->
                ObjectInputStream(fileIn).use { objIn ->
                    objIn.readObject() as ByteArray
                }
            }

            val decryptedData = cipher.doFinal(encryptedData)
            val privateKeyLength = decryptedData[0].toInt()
            val publicKeyLength = decryptedData[1].toInt()
            val privateKeyBytes = decryptedData.copyOfRange(2, privateKeyLength + 2)
            val publicKeyBytes = decryptedData.copyOfRange(privateKeyLength + 2, privateKeyLength + publicKeyLength + 2)
            val chainCode = decryptedData.copyOfRange(privateKeyLength + publicKeyLength + 2, decryptedData.size)

            // Convert byte arrays back to BigIntegers
            val privateKeyBigInt = BigInteger(privateKeyBytes)
            val publicKeyBigInt = BigInteger(publicKeyBytes)
            Triple(privateKeyBigInt, publicKeyBigInt, chainCode)
        } catch (e: Exception) {
            Log.e("Decrypt error", "Error during decryption: ${e.message}")
            null
        }
    }


    /**
     * Method that encrypts the Bip32 key pair using the specified AES session key. It returns a ByteArray
     * following the format: IV + size in bytes of private key + size in bytes of public key + encrypted data.
     * @param keyPair Bip32 key pair to be encrypted.
     * @param sessionKey AES session key used to encrypt.
     * @return encrypted bytes.
     */
    fun encryptWithSessionKey(keyPair: Bip32ECKeyPair, sessionKey: ByteArray): ByteArray {
        return try {
            val encryptedBytes = encryptAES(sessionKey, keyPair.privateKey.toByteArray() + keyPair.publicKey.toByteArray() + keyPair.chainCode)
            // send iv + size in bytes of private key + size in bytes of public key + encrypted (size of chain code not needed)
            encryptedBytes.second + keyPair.privateKey.toByteArray().size.toByte() + keyPair.publicKey.toByteArray().size.toByte() + encryptedBytes.first
        } catch (e: Exception) {
            Log.e("Encryption error", "Error during AES encryption: ${e.message}")
            ByteArray(0)
        }
    }

    private fun encryptAES(key: ByteArray, data: ByteArray): Pair<ByteArray, ByteArray> {
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec)
        return Pair(cipher.doFinal(data), iv)

    }

    private fun decryptAES(iv: ByteArray, sessionKey: ByteArray, encryptedData: ByteArray): ByteArray {
        val secretKey: SecretKey = SecretKeySpec(sessionKey, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        return cipher.doFinal(encryptedData)
    }


    fun decryptIPFSData(encryptedData: String, prefsName: String, context: Context): Quadruple<ByteArray, ByteArray, ByteArray, ByteArray>? {
        // data in the IPFS is stored in this way ( EncKij (SKij, Kij, j), EncPK0 (Kij) )
        // first retrieve EncKij
        try {
            val encWithAES =
                encryptedData.substringBefore(";").removePrefix("((").removeSuffix(")").split(",")
                    .map { it.trim().toUByte().toByte() }.toByteArray()
            // second retrieve EncPK0
            val encWithPub =
                encryptedData.substringAfter(";").removePrefix("(").removeSuffix("))").split(",")
                    .map { it.trim().toUByte().toByte() }.toByteArray()
            val keyPair: Triple<BigInteger, BigInteger, ByteArray>? = decryptRSA(
                "${prefsName.substringAfter("_")}/login${prefsName.substringAfter("_")}.bin",
                prefsName.substringAfter("_"),
                context
            )
            if (keyPair != null) {
                val root = Bip32ECKeyPair.create(keyPair.first, keyPair.third)
                val decryptedSessionKey = decryptWithPubKey(root, encWithPub)
                val decryptedData : ByteArray =
                    decryptAES(encWithAES.take(12).toByteArray(), decryptedSessionKey, encWithAES.drop(12).toByteArray().dropLast(2).toByteArray())
                val privKeyLength = encWithAES[encWithAES.size - 2].toInt()
                val pubKeyLength = encWithAES.last().toInt()
                val privKey = decryptedData.slice(IntRange(0, privKeyLength - 1)).toByteArray()
                val pubKey = decryptedData.slice(IntRange(privKeyLength, privKeyLength + pubKeyLength - 1)).toByteArray()
                val chainCode = decryptedData.slice(IntRange(privKeyLength + pubKeyLength, privKeyLength + pubKeyLength + 32 - 1)).toByteArray()
                val j = decryptedData.slice(IntRange(privKeyLength + pubKeyLength + 32, decryptedData.size - 1)).toByteArray()
                return Quadruple(privKey, pubKey, chainCode, j)
            }
        } catch (e: Exception) {
            e.message?.let { Log.e("error", it) }
        }
        return null
    }


    private fun decryptWithPubKey(rootKeyPair: Bip32ECKeyPair, encryptedData: ByteArray): ByteArray {
        // Initialize EC
        val ecParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")

        // length of cryptograms is stored in the last positions
        // Extract c1, c2 and r from encryptedData
        val lengthC1 = encryptedData.last().toInt()
        val c1 = encryptedData.copyOfRange(0, lengthC1)
        val c2 = encryptedData.copyOfRange(lengthC1, encryptedData.size - 1)

        // Step 1: Derive the session key from c2
        val ecPoint = ecParameterSpec.curve.decodePoint(c2)
        val sessionKeyPointBytes = ecPoint.multiply(rootKeyPair.privateKey).normalize().getEncoded(false)

        val indexOfFour = sessionKeyPointBytes.indexOfFirst { it == 4.toByte() }

        // Check if 4 is found in the array
        val trimmedSessionKeyPointBytes = if (indexOfFour != -1) {
            sessionKeyPointBytes.copyOfRange(indexOfFour + 1, sessionKeyPointBytes.size)
        } else {
            sessionKeyPointBytes // If 4 is not found, return the original array
        }

        // Step 2: Perform SHA256(sessionKeyPoint.x) to get AES session key

        val sha256Digest = MessageDigest.getInstance("SHA-256")
        val aesKey = sha256Digest.digest(trimmedSessionKeyPointBytes)

        // Step 3: Decrypt c1 using AES-GCM
        return decryptAES(c1.take(12).toByteArray(), aesKey, c1.drop(12).toByteArray())
    }


    /**
     * Method to generate a mnemonic word list of length 24.
     * @return list of String with the words.
     */
    fun generateMnemonic(): MutableList<String> {
        val entropy = ByteArray(32).apply { SecureRandom().nextBytes(this) }
        return MnemonicCode.INSTANCE.toMnemonic(entropy)
    }

    /**
     * Method that generates a Bip32ECKeyPair based on a mnemonic word list and a password.
     * @param mnemonicList  list of String indicating the mnemonic words as part of the seed for BIP32 protocol.
     * @param password  String with the password as part of the seed for BIP32 protocol.
     * @return Bip32ECKeyPair with the root key pair.
     */
    fun generateKeyPair(mnemonicList: MutableList<String>, password: String): Bip32ECKeyPair {
        val seed = MnemonicCode.toSeed(mnemonicList, password)
        return Bip32ECKeyPair.generateKeyPair(seed)
    }

    /**
     * Method that generates a Bip32ECKeyPair for the plugin based on a root key and a derivation path.
     * @param keyPair  Bip32ECKeyPair indicating the root key pair.
     * @param path  IntArray with the derivation path.
     * @return Bip32ECKeyPair with the browser key pair.
     */
    fun generateBrowserKeyPair(keyPair: Bip32ECKeyPair, path: IntArray): Bip32ECKeyPair {
        return Bip32ECKeyPair.deriveKeyPair(keyPair, path)
    }


    fun sha512(input: String): String {
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-512")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}