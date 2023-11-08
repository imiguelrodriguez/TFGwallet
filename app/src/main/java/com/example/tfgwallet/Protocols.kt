package com.example.tfgwallet
import android.graphics.Point
import com.example.tfgwallet.Protocols.Companion.bip32
import com.example.tfgwallet.Protocols.Companion.bip39
import com.example.tfgwallet.Protocols.Companion.sha256
import java.io.File
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.experimental.and


class Protocols {

    companion object {
        fun sha256(input: String): String {
            val messageDigest = MessageDigest.getInstance("SHA-256")
            val bytes = messageDigest.digest(input.toByteArray())
            val stringBuilder = StringBuilder()

            for (byte in bytes) {
                stringBuilder.append(String.format("%02x", byte))
            }
            return stringBuilder.toString()
        }


        fun bip39(size: Int, password: String) : Pair<String, ByteArray> {

            // generate random sequence (entropy) between 128 to 256 bits
            // first check that size is within the boundaries
            val secureRandom = SecureRandom()
            var entropy = BigInteger(size, secureRandom).toString(16) // fix when the first char is 0
            println("Entropy length ${entropy.length}")
            println(entropy)
            var checksum: String = sha256(entropy)
            println(checksum)
            // only keep first size/32 bits and concatenate at the end of the initial entropy
            println("Char to drop ${checksum.length - (size / 32)}")
            println(checksum.length)
            var modChecksum: String = checksum.dropLast(checksum.length - (size / 32))
            println(modChecksum)
            entropy += modChecksum
            println(entropy)

            entropy = BigInteger(entropy, 16).toString(2)

            println(entropy)
            println("Entropy length ${entropy.length}")
            // split entropy into 11-bits groups
            var groups = mutableListOf<String>()

            for (i in 0 until entropy.length - 1 step 11) {
                println(i)
                groups.add(entropy.substring(i, i + 11))
            }

            // get decimal value of each group
            val t_groups = groups.map { Integer.parseInt(it, 2).toString() }
            println(t_groups)
            println(System.getProperty("user.dir"))
            // read file and match every code to its word to give the eventual passphrase
            var words = readFile("app/resources/words.txt")
            var mnemonic: String = ""
            for (group in t_groups) { // for every number get the i-th word
                mnemonic += words[Integer.parseInt(group)] + " "
            }
            println(mnemonic)
            // convert to mnemonic words based on https://github.com/bitcoin/bips/blob/master/bip-0039/english.txt file

            // create seed using PBKDF2

            val salt = mnemonic + password // Tu salt aquí
            val iterations = 2048
            val keyLength = 64 * 8 // 64 bytes, 512 bits

            val keySpec: KeySpec = PBEKeySpec(password.toCharArray(), salt.toByteArray(), iterations, keyLength)
            val factory: SecretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
            var key = factory.generateSecret(keySpec).encoded

            val hexKey = key.joinToString("") { "%02x".format(it) }

            println("PBKDF2 generated key: $hexKey")

            return Pair(mnemonic, key)


    }
        private fun readFile(fileName: String) : List<String> {
            val file = File(fileName)
            return file.readLines()
        }



    fun bip32(seed: String) {
        fun point(p: Point) {

        }

        fun getBytesFromInt(int: Int, size: Int) : ByteArray {
            return ByteBuffer.allocate(size).putInt(int).array()
        }

        fun ser32(i: Int) : ByteArray {
            return getBytesFromInt(i, 4)
        }

        fun ser256(p: Int): ByteArray {
            return getBytesFromInt(p, 32)
        }

        fun serP(P: Point) {

        }

        fun parse256(p: ByteArray): Int {
            return ByteBuffer.wrap(p).int
        }

        val bytes32 = ser32(384)
        val bytes256 = ser256(123109823)
        print("[")
        for (byte in bytes32) {
            print("$byte, ")
        }
        println("]")

        print("[")
        for (byte in bytes256) {
            print("$byte, ")
        }
        println("]")

        println("Original number is ${parse256(bytes256)}")
    }

    }

}

fun main(args: Array<String>) {
    val inputString = "Hello, this is a test string for hashing."
    val hashedString = sha256(inputString)
    println("SHA256 Hash: $hashedString")
    bip39(128, "whatever")
    bip32("123")
}