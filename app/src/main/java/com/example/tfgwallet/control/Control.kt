package com.example.tfgwallet.control

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

class Control {
/*
    val keyGenerator = KeyGenerator
        .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")

    val keyGenParameterSpec = KeyGenParameterSpec.Builder(
        "secret_key",
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .build()

    keyGenerator.init(keyGenParameterSpec)
    val secretKey = keyGenerator.generateKey()

    var cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, secretKey)

    val iv = cipher.iv
    val keyPair = Pair(output.first.toByteArray(), pubK)
    val outputStream = ByteArrayOutputStream()
    val objectOutputStream = ObjectOutputStream(CipherOutputStream(outputStream, cipher))
    objectOutputStream.writeObject(keyPair)
    objectOutputStream.close()

    val encryptedData = cipher.doFinal(outputStream.toByteArray())

    // decrypt
    var keyStore = KeyStore.getInstance("AndroidKeyStore")
    keyStore.load(null)

    val s = keyStore
        .getEntry("secret_key", null)
    val sKey = (s as KeyStore.SecretKeyEntry).secretKey
    cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val spec = GCMParameterSpec(128, iv)
    cipher.init(Cipher.DECRYPT_MODE, sKey, spec)

    val decodedData = cipher.doFinal(encryptedData)*/
}